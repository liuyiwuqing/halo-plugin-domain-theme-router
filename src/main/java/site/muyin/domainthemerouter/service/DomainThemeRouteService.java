package site.muyin.domainthemerouter.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.muyin.domainthemerouter.model.DomainThemeRoute;

import java.util.List;

public interface DomainThemeRouteService {

    Flux<DomainThemeRoute> listEnabledRoutes();

    Mono<List<DomainThemeRoute>> listEnabledRoutesAsList();
}
