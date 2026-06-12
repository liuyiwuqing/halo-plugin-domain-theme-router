package site.muyin.domainthemerouter.theme;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
@Component
public class HaloThemeContextOverride {

    private static final String THEME_CONTEXT_CLASS = "run.halo.app.theme.ThemeContext";
    private static final String THEME_RESOLVER_BEAN_NAME = "themeResolver";

    private final ApplicationContext applicationContext;

    public HaloThemeContextOverride(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Mono<Boolean> override(ServerWebExchange exchange, String themeName) {
        return Mono.defer(() -> {
            try {
                Class<?> contextClass = Class.forName(THEME_CONTEXT_CLASS);
                Object resolver = applicationContext.getBean(THEME_RESOLVER_BEAN_NAME);
                Method getThemeContext = findGetThemeContextMethod(resolver);
                Object result = getThemeContext.invoke(resolver, themeName);

                if (!(result instanceof Mono<?> themeContextMono)) {
                    log.warn("Halo ThemeResolver#getThemeContext returned unexpected type: {}",
                            result == null ? "null" : result.getClass().getName());
                    return Mono.just(false);
                }

                return themeContextMono
                        .cast(contextClass)
                        .doOnNext(themeContext ->
                                exchange.getAttributes().put(contextClass.getName(), themeContext))
                        .thenReturn(true)
                        .onErrorResume(error -> {
                            log.warn("Failed to override Halo theme context for theme {}.", themeName, error);
                            return Mono.just(false);
                        });
            } catch (ReflectiveOperationException | RuntimeException error) {
                log.warn("Halo theme context API is unavailable, fallback to activated theme.", error);
                return Mono.just(false);
            }
        });
    }

    private Method findGetThemeContextMethod(Object resolver) throws NoSuchMethodException {
        return Arrays.stream(resolver.getClass().getMethods())
                .filter(method -> "getThemeContext".equals(method.getName()))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> String.class.equals(method.getParameterTypes()[0]))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException("getThemeContext(String)"));
    }
}
