package site.muyin.domainthemerouter.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.halo.app.core.extension.Theme;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import site.muyin.domainthemerouter.scheme.DomainThemeRoute;
import site.muyin.domainthemerouter.service.DomainThemeRouteMatcher;
import site.muyin.domainthemerouter.service.DomainThemeRouteService;
import site.muyin.domainthemerouter.theme.HaloThemeContextOverride;

import java.util.List;

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

    @Mock
    HaloThemeContextOverride themeContextOverride;

    @Test
    void overridesThemeWhenForwardedHostMatchesEnabledRoute() {
        var route = route("demo.muyin.site", "theme-a", true);
        var theme = theme("theme-a");
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client,
                themeContextOverride
        );
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-Host", "Demo.Muyin.Site:443"));

        when(routeService.listEnabledRoutesAsList()).thenReturn(Mono.just(List.of(route)));
        when(client.fetch(Theme.class, "theme-a")).thenReturn(Mono.just(theme));
        when(themeContextOverride.override(exchange, "theme-a")).thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, completedChain()))
                .verifyComplete();

        verify(themeContextOverride).override(exchange, "theme-a");
    }

    @Test
    void fallsBackWhenHostDoesNotMatch() {
        var route = route("demo.muyin.site", "theme-a", true);
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client,
                themeContextOverride
        );
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("Host", "test.muyin.site"));

        when(routeService.listEnabledRoutesAsList()).thenReturn(Mono.just(List.of(route)));

        StepVerifier.create(filter.filter(exchange, completedChain()))
                .verifyComplete();

        verify(client, never()).fetch(eq(Theme.class), any());
        verify(themeContextOverride, never()).override(any(), any());
    }

    @Test
    void fallsBackWhenMatchedThemeDoesNotExist() {
        var route = route("demo.muyin.site", "missing-theme", true);
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client,
                themeContextOverride
        );
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("Host", "demo.muyin.site"));

        when(routeService.listEnabledRoutesAsList()).thenReturn(Mono.just(List.of(route)));
        when(client.fetch(Theme.class, "missing-theme")).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, completedChain()))
                .verifyComplete();

        verify(themeContextOverride, never()).override(any(), any());
    }

    @Test
    void skipsNonThemeRenderingPaths() {
        var filter = new DomainThemeRouteWebFilter(
                routeService,
                new DomainThemeRouteMatcher(),
                client,
                themeContextOverride
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
        return theme;
    }

    private static WebFilterChain completedChain() {
        return exchange -> Mono.empty();
    }
}
