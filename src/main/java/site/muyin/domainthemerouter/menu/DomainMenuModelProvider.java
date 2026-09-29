package site.muyin.domainthemerouter.menu;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Builds only the request-local variable override; all unbound requests keep Halo's model. */
@Component
@RequiredArgsConstructor
public class DomainMenuModelProvider {

    private final HaloMenuFinderAdapter adapter;

    public Mono<Map<String, Object>> acquire(ServerWebExchange exchange, Object nativeMenuFinder) {
        DomainMenuBinding binding = exchange.getAttribute(DomainMenuBinding.ATTRIBUTE);
        if (binding == null) {
            return Mono.just(Map.of());
        }
        return Mono.just(Map.of("menuFinder",
                new RequestMenuFinder(nativeMenuFinder, binding.menuName(), adapter)));
    }
}
