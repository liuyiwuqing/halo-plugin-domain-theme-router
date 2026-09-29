package run.halo.app.plugin;

import org.pf4j.PluginManager;
import org.springframework.context.ApplicationContext;

/** Halo application SPI fixture: public interface implemented by a package-private class. */
public interface SpringPluginManager extends PluginManager {
    ApplicationContext getRootContext();
}
