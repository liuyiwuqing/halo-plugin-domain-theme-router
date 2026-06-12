package site.muyin.domainthemerouter.service.impl;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import site.muyin.domainthemerouter.model.DomainThemeRoute;
import site.muyin.domainthemerouter.model.DomainThemeRouteSettings;
import site.muyin.domainthemerouter.service.DomainThemeRouteService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class DomainThemeRouteServiceImpl implements DomainThemeRouteService {

    private static final String ROUTES_GROUP = "routes";

    private final ReactiveSettingFetcher settingFetcher;

    public DomainThemeRouteServiceImpl(ReactiveSettingFetcher settingFetcher) {
        this.settingFetcher = settingFetcher;
    }

    @Override
    public Flux<DomainThemeRoute> listEnabledRoutes() {
        return listEnabledRoutesAsList()
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<List<DomainThemeRoute>> listEnabledRoutesAsList() {
        return settingFetcher.fetch(ROUTES_GROUP, DomainThemeRouteSettings.class)
                .map(settings -> Optional.ofNullable(settings.getBindings())
                        .orElse(Collections.emptyList()))
                .defaultIfEmpty(List.of())
                .map(routes -> routes.stream()
                        .filter(route -> Boolean.TRUE.equals(route.getEnabled()))
                        .toList());
    }
}
