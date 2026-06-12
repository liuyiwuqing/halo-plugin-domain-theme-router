package site.muyin.domainthemerouter.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import site.muyin.domainthemerouter.model.DomainThemeRoute;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class DomainThemeRouteMatcher {

    public Optional<DomainThemeRoute> match(String requestDomain, List<DomainThemeRoute> routes) {
        var normalizedRequestDomain = normalizeRequestDomain(requestDomain);
        if (normalizedRequestDomain.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(routes).orElse(Collections.emptyList()).stream()
                .filter(route -> Boolean.TRUE.equals(route.getEnabled()))
                .filter(route -> normalizeRequestDomain(route.getDomain())
                        .filter(domain -> domain.equals(normalizedRequestDomain.get()))
                        .isPresent())
                .findFirst();
    }

    public Optional<String> normalizeRequestDomain(String value) {
        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }

        var candidate = StringUtils.trim(value);
        if (candidate.contains(",")) {
            candidate = StringUtils.substringBefore(candidate, ",");
        }
        candidate = StringUtils.trim(candidate);
        if (StringUtils.isBlank(candidate)) {
            return Optional.empty();
        }

        if (!candidate.contains("://")) {
            candidate = "http://" + candidate;
        }

        try {
            var host = URI.create(candidate).getHost();
            return Optional.ofNullable(StringUtils.trimToNull(host))
                    .map(domain -> domain.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
