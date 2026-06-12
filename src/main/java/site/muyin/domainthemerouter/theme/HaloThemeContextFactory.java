package site.muyin.domainthemerouter.theme;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class HaloThemeContextFactory {

    private static final String THEME_CONTEXT_CLASS = "run.halo.app.theme.ThemeContext";

    public Optional<Object> create(String themeName, Path themePath) {
        try {
            var contextClass = Class.forName(THEME_CONTEXT_CLASS);
            var builder = contextClass.getMethod("builder").invoke(null);
            invoke(builder, "name", String.class, themeName);
            invoke(builder, "path", Path.class, themePath);
            invoke(builder, "active", boolean.class, true);
            return Optional.of(builder.getClass().getMethod("build").invoke(builder));
        } catch (ReflectiveOperationException | RuntimeException error) {
            log.debug("Failed to create Halo ThemeContext for domain theme route {}, fallback to "
                    + "preview-theme request parameter.", themeName, error);
            return Optional.empty();
        }
    }

    private void invoke(Object target, String methodName, Class<?> parameterType, Object value)
            throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName, parameterType);
        method.invoke(target, value);
    }
}
