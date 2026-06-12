package site.muyin.domainthemerouter.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DomainThemeRoute {

    private String domain;

    private String themeName;

    private Boolean enabled = true;

    private String remark;
}
