package site.muyin.domainthemerouter.menu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/** Native menu finder facade owned by a single render model, never cached across requests. */
@Slf4j
@RequiredArgsConstructor
public class RequestMenuFinder {

    private final Object delegate;
    private final String menuName;
    private final HaloMenuFinderAdapter adapter;

    public Mono<Object> getPrimary() {
        return adapter.getByName(delegate, menuName)
                .onErrorResume(error -> {
                    log.warn("Failed to read domain menu {}, using Halo primary menu.", menuName, error);
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(() -> adapter.getPrimary(delegate)));
    }

    /** Explicitly selected secondary menus retain the theme's original behavior. */
    public Mono<Object> getByName(String name) {
        return adapter.getByName(delegate, name);
    }
}
