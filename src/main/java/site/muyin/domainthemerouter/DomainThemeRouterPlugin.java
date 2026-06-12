package site.muyin.domainthemerouter;

import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import site.muyin.domainthemerouter.scheme.DomainThemeRoute;

/**
 * <p>Plugin main class to manage the lifecycle of the plugin.</p>
 * <p>This class must be public and have a public constructor.</p>
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 *
 * @author Muyin
 * @since 1.0.0
 */
@Component
public class DomainThemeRouterPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public DomainThemeRouterPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(DomainThemeRoute.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<DomainThemeRoute, String>single("domain", String.class)
                    .unique(true)
                    .indexFunc(DomainThemeRoute::getDomain));
            indexSpecs.add(IndexSpecs.<DomainThemeRoute, String>single("themeName", String.class)
                    .indexFunc(DomainThemeRoute::getThemeName));
            indexSpecs.add(IndexSpecs.<DomainThemeRoute, Boolean>single("enabled", Boolean.class)
                    .indexFunc(DomainThemeRoute::getEnabled));
        });
    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(DomainThemeRoute.class));
    }
}
