package site.muyin.domainthemerouter.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.halo.app.core.extension.Theme;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import site.muyin.domainthemerouter.model.DomainThemeRoute;
import site.muyin.domainthemerouter.service.DomainThemeRouteMatcher;
import site.muyin.domainthemerouter.service.DomainThemeRouteService;
import site.muyin.domainthemerouter.theme.HaloThemeContextFactory;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainThemeRouteWebFilterTest {

    @Mock
    DomainThemeRouteService routeService;

    @Mock
    ReactiveExtensionClient client;

    @Test
    void publicDependencyConstructorIsMarkedForSpringAutowiring() throws NoSuchMethodException {
        Constructor<DomainThemeRouteWebFilter> constructor =
                DomainThemeRouteWebFilter.class.getConstructor(
                        DomainThemeRouteService.class,
                        DomainThemeRouteMatcher.class,
                        ReactiveExtensionClient.class
                );

        assertThat(constructor.isAnnotationPresent(Autowired.class)).isTrue();
    }

    @Test
    void canBeCreatedBySpringWhenRegisteredAsPluginComponent() {
        var context = new GenericApplicationContext();
        context.registerBean(DomainThemeRouteService.class, () -> routeService);
        context.registerBean(DomainThemeRouteMatcher.class);
        context.registerBean(ReactiveExtensionClient.class, () -> client);
        context.registerBean(DomainThemeRouteWebFilter.class);

        context.refresh();

        assertThat(context.getBean(DomainThemeRouteWebFilter.class)).isNotNull();
        context.close();
    }

    @Test
    void overridesThemeWhenForwardedHostMatchesEnabledRoute() {
        var route = route("demo.muyin.site", "theme-a", true);
        var theme = theme("theme-a");
        var themeContext = new TestThemeContext("theme-a", Path.of("/halo/themes/theme-a"), true);
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client,
                new TestHaloThemeContextFactory(themeContext)
        );
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-Host", "Demo.Muyin.Site:443"));
        var chain = new CapturingWebFilterChain();

        when(routeService.listEnabledRoutesAsList()).thenReturn(Mono.just(List.of(route)));
        when(client.fetch(Theme.class, "theme-a")).thenReturn(Mono.just(theme));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chain.exchange().getRequest().getQueryParams()).doesNotContainKey("preview-theme");
        Object appliedThemeContext = chain.exchange()
                .getAttribute("run.halo.app.theme.ThemeContext");
        assertThat(appliedThemeContext).isEqualTo(themeContext);
    }

    @Test
    void removesIncomingPreviewQueryParamWhenThemeContextIsApplied() {
        var route = route("demo.muyin.site", "theme-a", true);
        var theme = theme("theme-a");
        var themeContext = new TestThemeContext("theme-a", Path.of("/halo/themes/theme-a"), true);
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client,
                new TestHaloThemeContextFactory(themeContext)
        );
        var exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/archives/post?foo=bar&preview-theme=manual-theme")
                .header("Host", "demo.muyin.site"));
        var chain = new CapturingWebFilterChain();

        when(routeService.listEnabledRoutesAsList()).thenReturn(Mono.just(List.of(route)));
        when(client.fetch(Theme.class, "theme-a")).thenReturn(Mono.just(theme));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chain.exchange().getRequest().getQueryParams().getFirst("foo"))
                .isEqualTo("bar");
        assertThat(chain.exchange().getRequest().getQueryParams()).doesNotContainKey("preview-theme");
        Object appliedThemeContext = chain.exchange()
                .getAttribute("run.halo.app.theme.ThemeContext");
        assertThat(appliedThemeContext).isEqualTo(themeContext);
    }

    @Test
    void replacesCachedHaloThemeContextWhenDomainThemeIsApplied() {
        var route = route("demo.muyin.site", "theme-a", true);
        var theme = theme("theme-a");
        var themeContext = new TestThemeContext("theme-a", Path.of("/halo/themes/theme-a"), true);
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client,
                new TestHaloThemeContextFactory(themeContext)
        );
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("Host", "demo.muyin.site"));
        exchange.getAttributes()
                .put("run.halo.app.theme.ThemeContext", "activated-theme-context");
        var chain = new CapturingWebFilterChain();

        when(routeService.listEnabledRoutesAsList()).thenReturn(Mono.just(List.of(route)));
        when(client.fetch(Theme.class, "theme-a")).thenReturn(Mono.just(theme));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chain.exchange().getRequest().getQueryParams()).doesNotContainKey("preview-theme");
        Object appliedThemeContext = chain.exchange()
                .getAttribute("run.halo.app.theme.ThemeContext");
        assertThat(appliedThemeContext).isEqualTo(themeContext);
    }

    @Test
    void fallsBackToPreviewQueryParamWhenThemeContextCannotBeCreated() {
        var route = route("demo.muyin.site", "theme-a", true);
        var theme = theme("theme-a");
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client,
                new TestHaloThemeContextFactory(null)
        );
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/archives/post?foo=bar")
                .header("Host", "demo.muyin.site"));
        var chain = new CapturingWebFilterChain();

        when(routeService.listEnabledRoutesAsList()).thenReturn(Mono.just(List.of(route)));
        when(client.fetch(Theme.class, "theme-a")).thenReturn(Mono.just(theme));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chain.exchange().getRequest().getQueryParams().getFirst("foo"))
                .isEqualTo("bar");
        assertThat(chain.exchange().getRequest().getQueryParams().getFirst("preview-theme"))
                .isEqualTo("theme-a");
        Object appliedThemeContext = chain.exchange()
                .getAttribute("run.halo.app.theme.ThemeContext");
        assertThat(appliedThemeContext).isNull();
    }

    @Test
    void fallsBackWhenHostDoesNotMatch() {
        var route = route("demo.muyin.site", "theme-a", true);
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client
        );
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("Host", "test.muyin.site"));

        when(routeService.listEnabledRoutesAsList()).thenReturn(Mono.just(List.of(route)));

        StepVerifier.create(filter.filter(exchange, completedChain()))
                .verifyComplete();

        verify(client, never()).fetch(eq(Theme.class), any());
    }

    @Test
    void fallsBackWhenMatchedThemeDoesNotExist() {
        var route = route("demo.muyin.site", "missing-theme", true);
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client
        );
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("Host", "demo.muyin.site"));

        when(routeService.listEnabledRoutesAsList()).thenReturn(Mono.just(List.of(route)));
        when(client.fetch(Theme.class, "missing-theme")).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, completedChain()))
                .verifyComplete();

        verify(client).fetch(Theme.class, "missing-theme");
    }

    @Test
    void skipsNonThemeRenderingPaths() {
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client
        );
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/apis/foo"));

        StepVerifier.create(filter.filter(exchange, completedChain()))
                .verifyComplete();

        verify(routeService, never()).listEnabledRoutesAsList();
    }

    private static DomainThemeRoute route(String domain, String themeName, boolean enabled) {
        return new DomainThemeRoute()
                .setDomain(domain)
                .setThemeName(themeName)
                .setEnabled(enabled);
    }

    private static Theme theme(String name) {
        var theme = new Theme();
        var metadata = new Metadata();
        metadata.setName(name);
        theme.setMetadata(metadata);
        var status = new Theme.ThemeStatus();
        status.setLocation("/halo/themes/" + name);
        theme.setStatus(status);
        return theme;
    }

    private static WebFilterChain completedChain() {
        return exchange -> Mono.empty();
    }

    private static class CapturingWebFilterChain implements WebFilterChain {

        private final AtomicReference<ServerWebExchange> exchange = new AtomicReference<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.exchange.set(exchange);
            return Mono.empty();
        }

        ServerWebExchange exchange() {
            return exchange.get();
        }
    }

    private record TestThemeContext(String name, Path path, boolean active) {
    }

    private static class TestHaloThemeContextFactory extends HaloThemeContextFactory {

        private final Object themeContext;

        private TestHaloThemeContextFactory(Object themeContext) {
            this.themeContext = themeContext;
        }

        @Override
        public Optional<Object> create(String themeName, Path themePath) {
            return Optional.ofNullable(themeContext);
        }
    }
}
