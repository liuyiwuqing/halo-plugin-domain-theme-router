package site.muyin.domainthemerouter.scheme;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@GVK(group = "domain-theme-router.muyin.site", version = "v1alpha1",
        kind = "DomainThemeRoute", plural = "domainThemeRoutes", singular = "domainThemeRoute")
public class DomainThemeRoute extends AbstractExtension {

    public static final String EXTENSION_NAME_PREFIX = "domainThemeRoute-";

    @Schema(description = "精确匹配的访问域名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String domain;

    @Schema(description = "绑定的 Halo 主题名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String themeName;

    @Schema(description = "是否启用")
    private Boolean enabled = true;

    @Schema(description = "备注")
    private String remark;
}
