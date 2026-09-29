package site.muyin.domainthemerouter.menu;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.reflect.Proxy;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Bridges Halo's application-only ViewContextBasedVariablesAcquirer SPI. Registers an additional
 * provider under a plugin-specific name, never replaces the native finder or any existing bean.
 * Halo looks up providers for each render, so enabling/disabling needs no theme-cache mutation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HaloMenuModelBridge {

    static final String BEAN_NAME = "domainThemeRouterMenuModelProvider";
    private static final String SPI = "run.halo.app.theme.ViewContextBasedVariablesAcquirer";
    private static final String PLUGIN_MANAGER_SPI = "run.halo.app.plugin.SpringPluginManager";

    private final PluginWrapper pluginWrapper;
    private final DomainMenuModelProvider provider;
    private DefaultListableBeanFactory registeredFactory;
    private Object registeredProvider;
    private volatile boolean active;

    @PostConstruct
    public void register() {
        try {
            var manager = pluginWrapper.getPluginManager();
            // HaloPluginManager is package-private. Its public method is not reflectively
            // accessible from this plugin; invoke the method declared on the public interface.
            var managerApi = Class.forName(PLUGIN_MANAGER_SPI, false, manager.getClass().getClassLoader());
            var root = (ConfigurableApplicationContext) managerApi
                    .getMethod("getRootContext").invoke(manager);
            var factory = (DefaultListableBeanFactory) root.getBeanFactory();
            var spi = Class.forName(SPI, false, root.getClassLoader());
            // Resolve Halo-owned types by name so the plugin stays compiled against the API only.
            var nativeFinder = root.getBean("menuFinder");
            nativeFinder.getClass().getMethod("getPrimary");
            nativeFinder.getClass().getMethod("getByName", String.class);
            var proxy = Proxy.newProxyInstance(spi.getClassLoader(), new Class<?>[] {spi},
                    (instance, method, args) -> switch (method.getName()) {
                        case "acquire" -> active
                                ? provider.acquire((ServerWebExchange) args[0], nativeFinder)
                                : Mono.just(Map.of());
                        case "toString" -> BEAN_NAME;
                        case "hashCode" -> System.identityHashCode(instance);
                        case "equals" -> instance == args[0];
                        default -> throw new UnsupportedOperationException(method.toString());
                    });
            factory.registerSingleton(BEAN_NAME, proxy);
            registeredFactory = factory;
            registeredProvider = proxy;
            active = true;
            log.info("Automatic domain menu binding registered successfully.");
        } catch (ReflectiveOperationException | RuntimeException error) {
            // An incompatible Halo version must not disable existing domain-to-theme routing.
            log.error("Automatic domain menu binding is unavailable; domain theme routing remains active.", error);
        }
    }

    @PreDestroy
    public void unregister() {
        active = false;
        if (registeredFactory != null
                && registeredFactory.getSingleton(BEAN_NAME) == registeredProvider) {
            registeredFactory.destroySingleton(BEAN_NAME);
        }
        registeredFactory = null;
        registeredProvider = null;
    }
}
