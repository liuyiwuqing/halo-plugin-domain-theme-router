package site.muyin.domainthemerouter.theme;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HaloThemeContextFactoryTest {

    @Test
    void returnsEmptyWhenHaloThemeContextClassIsNotOnClasspath() {
        var factory = new HaloThemeContextFactory();

        var themeContext = factory.create("theme-a", Path.of("/halo/themes/theme-a"));

        assertThat(themeContext).isEmpty();
    }
}
