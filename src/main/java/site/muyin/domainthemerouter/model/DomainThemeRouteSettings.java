package site.muyin.domainthemerouter.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DomainThemeRouteSettings {

    private List<DomainThemeRoute> bindings = new ArrayList<>();
}
