package site.muyin.domainthemerouter.menu;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.pf4j.PluginWrapper;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.HiddenPluginManagerFixture;
import run.halo.app.theme.ViewContextBasedVariablesAcquirer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HaloMenuModelBridgeTest {
    @Test
    void registersWithNonPublicHaloManagerAndIsolatesRequestMenus() throws Exception {
        try (var root = new GenericApplicationContext()) {
            var nativeFinder = new NativeMenuFinder();
            root.getBeanFactory().registerSingleton("menuFinder", nativeFinder);
            root.refresh();
            var manager = HiddenPluginManagerFixture.create(root);
            assertThat(Modifier.isPublic(manager.getClass().getModifiers())).isFalse();
            // Reproduce the old production-only reflection failure before asserting the fix.
            var oldMethod = manager.getClass().getMethod("getRootContext");
            assertThatThrownBy(() -> oldMethod.invoke(manager)).isInstanceOf(IllegalAccessException.class);
            var wrapper = mock(PluginWrapper.class);
            when(wrapper.getPluginManager()).thenReturn(manager);
            var bridge = new HaloMenuModelBridge(wrapper,
                    new DomainMenuModelProvider(new HaloMenuFinderAdapter()));

            bridge.register();
            try {
                assertThat(root.getBeansOfType(ViewContextBasedVariablesAcquirer.class)).hasSize(1);
                var provider = root.getBean(ViewContextBasedVariablesAcquirer.class);
                var a = MockServerWebExchange.from(MockServerHttpRequest.get("https://a.example.com/"));
                var b = MockServerWebExchange.from(MockServerHttpRequest.get("https://b.example.com/"));
                a.getAttributes().put(DomainMenuBinding.ATTRIBUTE, new DomainMenuBinding("menu-a"));
                b.getAttributes().put(DomainMenuBinding.ATTRIBUTE, new DomainMenuBinding("menu-b"));
                var finderA = (RequestMenuFinder) provider.acquire(a).block().get("menuFinder");
                var finderB = (RequestMenuFinder) provider.acquire(b).block().get("menuFinder");
                assertThat(finderA.getPrimary().block()).isEqualTo("menu-a");
                assertThat(finderB.getPrimary().block()).isEqualTo("menu-b");
                assertThat(finderA.getByName("footer").block()).isEqualTo("footer");
                assertThat(provider.acquire(MockServerWebExchange.from(MockServerHttpRequest.get("/")))
                        .block()).isEmpty();
                assertThat(root.getBean("menuFinder")).isSameAs(nativeFinder);
                bridge.unregister();
                assertThat(root.getBeansOfType(ViewContextBasedVariablesAcquirer.class)).isEmpty();
                assertThat(provider.acquire(a).block()).isEmpty();
                assertThat(root.getBean("menuFinder")).isSameAs(nativeFinder);
            } finally {
                bridge.unregister();
            }
        }
    }

    @Test
    void missingBoundMenuFallsBackToNativePrimary() {
        var finder = new RequestMenuFinder(new NativeMenuFinder(), "deleted", new HaloMenuFinderAdapter());
        assertThat(finder.getPrimary().block()).isEqualTo("global-primary");
    }

    public static class NativeMenuFinder {
        public Mono<String> getPrimary() {
            return Mono.just("global-primary");
        }

        public Mono<String> getByName(String name) {
            return "deleted".equals(name) ? Mono.error(new IllegalArgumentException("deleted menu"))
                    : Mono.just(name);
        }
    }
}
