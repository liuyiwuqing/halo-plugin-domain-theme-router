package site.muyin.domainthemerouter.service;

import org.junit.jupiter.api.Test;
import site.muyin.domainthemerouter.scheme.DomainThemeRoute;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DomainThemeRouteMatcherTest {

    private final DomainThemeRouteMatcher matcher = new DomainThemeRouteMatcher();

    @Test
    void normalizesRequestDomain() {
        assertThat(matcher.normalizeRequestDomain("Demo.Muyin.Site:443")).contains("demo.muyin.site");
        assertThat(matcher.normalizeRequestDomain(" https://Test.Muyin.Site:8443 ")).contains("test.muyin.site");
        assertThat(matcher.normalizeRequestDomain("www.example.cn")).contains("www.example.cn");
        assertThat(matcher.normalizeRequestDomain("")).isEmpty();
    }

    @Test
    void matchesOnlyEnabledExactDomainRoutes() {
        var routes = List.of(
                route("demo.muyin.site", "theme-a", true),
                route("test.muyin.site", "theme-b", false)
        );

        assertThat(matcher.match("DEMO.MUYIN.SITE:443", routes))
                .map(DomainThemeRoute::getThemeName)
                .contains("theme-a");
        assertThat(matcher.match("test.muyin.site", routes)).isEmpty();
        assertThat(matcher.match("blog.muyin.site", routes)).isEmpty();
    }

    @Test
    void ignoresMalformedRouteDomains() {
        var routes = List.of(
                route("", "empty-theme", true),
                route("demo.muyin.site", "theme-a", true)
        );

        Optional<DomainThemeRoute> matched = matcher.match("demo.muyin.site", routes);

        assertThat(matched).map(DomainThemeRoute::getThemeName).contains("theme-a");
    }

    @Test
    void ignoresNullRouteList() {
        assertThat(matcher.match("demo.muyin.site", null)).isEmpty();
    }

    private static DomainThemeRoute route(String domain, String themeName, boolean enabled) {
        var route = new DomainThemeRoute();
        route.setDomain(domain);
        route.setThemeName(themeName);
        route.setEnabled(enabled);
        return route;
    }
}
