package site.muyin.domainthemerouter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.halo.app.plugin.ReactiveSettingFetcher;
import site.muyin.domainthemerouter.model.DomainThemeRoute;
import site.muyin.domainthemerouter.model.DomainThemeRouteSettings;
import site.muyin.domainthemerouter.service.impl.DomainThemeRouteServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainThemeRouteServiceImplTest {

    @Mock
    ReactiveSettingFetcher settingFetcher;

    @Test
    void listsEnabledRoutesFromPluginSettings() {
        var settings = new DomainThemeRouteSettings();
        settings.setBindings(List.of(
                route("demo.muyin.site", "theme-a", true),
                route("test.muyin.site", "theme-b", false)
        ));
        when(settingFetcher.fetch("routes", DomainThemeRouteSettings.class))
                .thenReturn(Mono.just(settings));

        var service = new DomainThemeRouteServiceImpl(settingFetcher);

        StepVerifier.create(service.listEnabledRoutesAsList())
                .assertNext(routes -> {
                    assertThat(routes).hasSize(1);
                    assertThat(routes.getFirst().getDomain()).isEqualTo("demo.muyin.site");
                    assertThat(routes.getFirst().getThemeName()).isEqualTo("theme-a");
                })
                .verifyComplete();
    }

    @Test
    void returnsEmptyListWhenPluginSettingsAreMissing() {
        when(settingFetcher.fetch("routes", DomainThemeRouteSettings.class))
                .thenReturn(Mono.empty());

        var service = new DomainThemeRouteServiceImpl(settingFetcher);

        StepVerifier.create(service.listEnabledRoutesAsList())
                .expectNext(List.of())
                .verifyComplete();
    }

    private static DomainThemeRoute route(String domain, String themeName, boolean enabled) {
        return new DomainThemeRoute()
                .setDomain(domain)
                .setThemeName(themeName)
                .setEnabled(enabled);
    }
}
