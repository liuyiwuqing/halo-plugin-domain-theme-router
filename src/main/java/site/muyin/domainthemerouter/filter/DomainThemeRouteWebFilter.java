package site.muyin.domainthemerouter.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Theme;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.security.AdditionalWebFilter;
import site.muyin.domainthemerouter.service.DomainThemeRouteMatcher;
import site.muyin.domainthemerouter.service.DomainThemeRouteService;
import site.muyin.domainthemerouter.theme.HaloThemeContextOverride;

@Slf4j
@Component
public class DomainThemeRouteWebFilter implements AdditionalWebFilter {

    private static final String FORWARDED_HOST = "X-Forwarded-Host";

    private final DomainThemeRouteService routeService;
    private final DomainThemeRouteMatcher routeMatcher;
    private final ReactiveExtensionClient client;
    private final HaloThemeContextOverride themeContextOverride;

    public DomainThemeRouteWebFilter(DomainThemeRouteService routeService,
                                     DomainThemeRouteMatcher routeMatcher,
                                     ReactiveExtensionClient client,
                                     HaloThemeContextOverride themeContextOverride) {
        this.routeService = routeService;
        this.routeMatcher = routeMatcher;
        this.client = client;
        this.themeContextOverride = themeContextOverride;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (shouldSkip(exchange)) {
            return chain.filter(exchange);
        }

        var requestDomain = resolveRequestDomain(exchange);
        if (StringUtils.isBlank(requestDomain)) {
            return chain.filter(exchange);
        }

        return routeService.listEnabledRoutesAsList()
                .flatMap(routes -> routeMatcher.match(requestDomain, routes)
                        .map(route -> overrideTheme(exchange, route.getThemeName()))
                        .orElseGet(Mono::empty))
                .onErrorResume(error -> {
                    log.warn("Failed to resolve domain theme route, fallback to activated theme.", error);
                    return Mono.empty();
                })
                .then(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    private Mono<Boolean> overrideTheme(ServerWebExchange exchange, String themeName) {
        if (StringUtils.isBlank(themeName)) {
            return Mono.just(false);
        }
        return client.fetch(Theme.class, themeName)
                .flatMap(theme -> themeContextOverride.override(exchange, theme.getMetadata().getName()))
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    log.warn("Domain theme route matched missing theme {}, fallback to activated theme.",
                            themeName);
                    return false;
                }));
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
