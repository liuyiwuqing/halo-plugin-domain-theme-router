package site.muyin.domainthemerouter.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DomainThemeRoute {

    private String domain;

    private String themeName;

    /** Optional primary menu for this domain; blank preserves the theme's original menu. */
    private String menuName;

    private Boolean enabled = true;

    private String remark;
}
