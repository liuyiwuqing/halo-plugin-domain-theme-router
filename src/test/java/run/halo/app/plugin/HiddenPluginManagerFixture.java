package run.halo.app.plugin;

import org.pf4j.DefaultPluginManager;
import org.springframework.context.ApplicationContext;

public final class HiddenPluginManagerFixture {
    private HiddenPluginManagerFixture() {
    }

    public static SpringPluginManager create(ApplicationContext context) {
        return new HiddenPluginManager(context);
    }
}

// Deliberately outside the bridge's package and NOT public, just like HaloPluginManager.
class HiddenPluginManager extends DefaultPluginManager implements SpringPluginManager {
    private final ApplicationContext context;

    HiddenPluginManager(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public ApplicationContext getRootContext() {
        return context;
    }
}
