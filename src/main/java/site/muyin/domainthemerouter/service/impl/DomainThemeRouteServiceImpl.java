package site.muyin.domainthemerouter.service.impl;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Queries;
import site.muyin.domainthemerouter.scheme.DomainThemeRoute;
import site.muyin.domainthemerouter.service.DomainThemeRouteService;

import java.util.List;

@Service
public class DomainThemeRouteServiceImpl implements DomainThemeRouteService {

    private final ReactiveExtensionClient client;

    public DomainThemeRouteServiceImpl(ReactiveExtensionClient client) {
        this.client = client;
    }

    @Override
    public Flux<DomainThemeRoute> listEnabledRoutes() {
        var listOptions = ListOptions.builder()
                .fieldQuery(Queries.equal("enabled", Boolean.TRUE))
                .build();
        return client.listAll(DomainThemeRoute.class, listOptions, null);
    }

    @Override
    public Mono<List<DomainThemeRoute>> listEnabledRoutesAsList() {
        return listEnabledRoutes().collectList();
    }
}
