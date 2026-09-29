package site.muyin.domainthemerouter.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Theme;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.security.AdditionalWebFilter;
import site.muyin.domainthemerouter.menu.DomainMenuBinding;
import site.muyin.domainthemerouter.model.DomainThemeRoute;
import site.muyin.domainthemerouter.service.DomainThemeRouteMatcher;
import site.muyin.domainthemerouter.service.DomainThemeRouteService;
import site.muyin.domainthemerouter.theme.HaloThemeContextFactory;

import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Component
public class DomainThemeRouteWebFilter implements AdditionalWebFilter {

    private static final String FORWARDED_HOST = "X-Forwarded-Host";
    private static final String THEME_PREVIEW_PARAM = "preview-theme";
    private static final String HALO_THEME_CONTEXT_ATTRIBUTE = "run.halo.app.theme.ThemeContext";

    private final DomainThemeRouteService routeService;
    private final DomainThemeRouteMatcher routeMatcher;
    private final ReactiveExtensionClient client;
    private final HaloThemeContextFactory themeContextFactory;

    @Autowired
    public DomainThemeRouteWebFilter(DomainThemeRouteService routeService,
                                     DomainThemeRouteMatcher routeMatcher,
                                     ReactiveExtensionClient client) {
        this(routeService, routeMatcher, client, new HaloThemeContextFactory());
    }

    DomainThemeRouteWebFilter(DomainThemeRouteService routeService,
                              DomainThemeRouteMatcher routeMatcher,
                              ReactiveExtensionClient client,
                              HaloThemeContextFactory themeContextFactory) {
        this.routeService = routeService;
        this.routeMatcher = routeMatcher;
        this.client = client;
        this.themeContextFactory = themeContextFactory;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (shouldSkip(exchange)) {
            return chain.filter(exchange);
        }

        var requestDomain = resolveRequestDomain(exchange);
        log.debug("Domain theme request: path={}, host={}, forwardedHost={}, uriHost={}, resolvedDomain={}",
                exchange.getRequest().getPath().value(),
                exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST),
                exchange.getRequest().getHeaders().getFirst(FORWARDED_HOST),
                exchange.getRequest().getURI().getHost(), requestDomain);
        if (StringUtils.isBlank(requestDomain)) {
            return chain.filter(exchange);
        }

        return routeService.listEnabledRoutesAsList()
                .flatMap(routes -> {
                    var matched = routeMatcher.match(requestDomain, routes);
                    if (matched.isEmpty()) {
                        log.debug("No domain theme binding matched domain={}, enabledBindings={}",
                                requestDomain, routes.size());
                    }
                    return matched.map(route -> {
                        log.debug("Domain theme binding matched: domain={}, theme={}, menu={}",
                                requestDomain, route.getThemeName(), route.getMenuName());
                        return applyThemePreview(exchange, route);
                    }).orElseGet(() -> Mono.just(exchange));
                })
                .onErrorResume(error -> {
                    log.warn("Failed to resolve domain theme route, fallback to activated theme.", error);
                    return Mono.just(exchange);
                })
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    private Mono<ServerWebExchange> applyThemePreview(ServerWebExchange exchange, DomainThemeRoute route) {
        var themeName = route.getThemeName();
        if (StringUtils.isBlank(themeName)) {
            return Mono.just(exchange);
        }
        return client.fetch(Theme.class, themeName)
                .map(theme -> {
                    var resolvedExchange = withDomainTheme(exchange, theme);
                    var menuName = StringUtils.trimToNull(route.getMenuName());
                    if (menuName != null) {
                        resolvedExchange.getAttributes().put(DomainMenuBinding.ATTRIBUTE,
                                new DomainMenuBinding(menuName));
                    }
                    return resolvedExchange;
                })
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    log.warn("Domain theme route matched missing theme {}, fallback to activated theme.",
                            themeName);
                    return exchange;
                }));
    }

    private ServerWebExchange withDomainTheme(ServerWebExchange exchange, Theme theme) {
        var themeName = theme.getMetadata().getName();
        return themePath(theme)
                .flatMap(themePath -> themeContextFactory.create(themeName, themePath))
                .map(themeContext -> withThemeContext(exchange, themeContext))
                .orElseGet(() -> withPreviewTheme(exchange, themeName));
    }

    private Optional<Path> themePath(Theme theme) {
        return Optional.ofNullable(theme.getStatus())
                .map(Theme.ThemeStatus::getLocation)
                .filter(StringUtils::isNotBlank)
                .map(Path::of);
    }

    private ServerWebExchange withThemeContext(ServerWebExchange exchange, Object themeContext) {
        log.debug("Applying domain theme with request ThemeContext");
        var request = exchange.getRequest().mutate()
                .uri(UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                        .replaceQueryParam(THEME_PREVIEW_PARAM)
                        .build(true)
                        .toUri())
                .build();
        var mutatedExchange = exchange.mutate()
                .request(request)
                .build();
        mutatedExchange.getAttributes().put(HALO_THEME_CONTEXT_ATTRIBUTE, themeContext);
        return mutatedExchange;
    }

    private ServerWebExchange withPreviewTheme(ServerWebExchange exchange, String themeName) {
        log.debug("Applying domain theme {} using preview-theme fallback", themeName);
        var uri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                .replaceQueryParam(THEME_PREVIEW_PARAM, themeName)
                .build(true)
                .toUri();
        var request = exchange.getRequest().mutate()
                .uri(uri)
                .build();
        var mutatedExchange = exchange.mutate()
                .request(request)
                .build();
        mutatedExchange.getAttributes().remove(HALO_THEME_CONTEXT_ATTRIBUTE);
        return mutatedExchange;
    }

    private String resolveRequestDomain(ServerWebExchange exchange) {
        var headers = exchange.getRequest().getHeaders();
        var forwardedHost = headers.getFirst(FORWARDED_HOST);
        if (StringUtils.isNotBlank(forwardedHost)) {
            return forwardedHost;
        }
        return headers.getFirst(HttpHeaders.HOST);
    }

    private boolean shouldSkip(ServerWebExchange exchange) {
        var path = exchange.getRequest().getPath().pathWithinApplication().value();
        return path.startsWith("/apis/")
                || path.startsWith("/api/")
                || path.startsWith("/console")
                || path.startsWith("/uc")
                || path.startsWith("/plugins/")
                || path.startsWith("/themes/")
                || path.startsWith("/assets/")
                || path.startsWith("/actuator");
    }
}
