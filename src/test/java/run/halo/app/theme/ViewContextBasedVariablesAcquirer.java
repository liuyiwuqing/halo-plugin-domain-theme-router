package run.halo.app.theme;

import java.util.Map;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Halo application SPI fixture, matching the public render-model contract. */
public interface ViewContextBasedVariablesAcquirer {
    Mono<Map<String, Object>> acquire(ServerWebExchange exchange);
}
