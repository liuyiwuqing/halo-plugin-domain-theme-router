package site.muyin.domainthemerouter.menu;

import java.lang.reflect.InvocationTargetException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Delegates to the native finder supplied by the template. MenuFinder and its view objects are
 * Halo application classes, not part of the plugin API, so keep that boundary here. Returning the
 * native objects preserves Halo's menu hierarchy, ordering, annotations and resolved links.
 */
@Component
public class HaloMenuFinderAdapter {

    public Mono<Object> getPrimary(Object finder) {
        return invoke(finder, "getPrimary", new Class<?>[0]);
    }

    public Mono<Object> getByName(Object finder, String name) {
        return invoke(finder, "getByName", new Class<?>[] {String.class}, name);
    }

    private Mono<Object> invoke(Object finder, String method, Class<?>[] types, Object... args) {
        return Mono.defer(() -> {
            try {
                var result = finder.getClass().getMethod(method, types).invoke(finder, args);
                if (result instanceof Mono<?> mono) {
                    return mono.cast(Object.class);
                }
                return Mono.error(new IllegalStateException("Halo menu finder must return Mono"));
            } catch (InvocationTargetException error) {
                return Mono.error(error.getCause());
            } catch (ReflectiveOperationException | RuntimeException error) {
                return Mono.error(error);
            }
        });
    }
}
