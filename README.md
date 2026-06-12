# Domain Theme Router

域名主题路由是一个 Halo 插件，用于把精确域名绑定到已安装主题，让同一个 Halo 内容空间可以根据访问域名展示不同的前台主题。

## 简介

第一版目标：

- 支持精确域名到已安装主题的绑定。
- 未命中绑定时回退 Halo 当前激活主题。
- 所有域名共享同一套文章、页面、菜单、附件、用户和后台数据。
- 不支持多站点隔离，不支持通配符域名。

## 实现方式

- 绑定数据存储为 `DomainThemeRoute` 扩展资源，接口路径为 `/apis/domain-theme-router.muyin.site/v1alpha1/domainThemeRoutes`。
- 控制台页面路径为 `/domain-theme-router`，可管理域名、主题、启用状态和备注。
- 运行时通过 `AdditionalWebFilter` 读取 `X-Forwarded-Host`，没有该请求头时读取 `Host`。
- 命中启用绑定后，插件会在请求级别覆盖 Halo 的主题上下文；未命中或覆盖失败时回退 Halo 当前激活主题。

## 架构记录

- [0001-domain-theme-routing-is-not-multi-site](./docs/adr/0001-domain-theme-routing-is-not-multi-site.md)
- [0002-use-request-theme-context-override](./docs/adr/0002-use-request-theme-context-override.md)

## 注意事项

- 域名匹配会忽略端口并统一转小写。
- 生产环境使用 Nginx、CDN 或网关时，需要正确传递 `X-Forwarded-Host`。
- 当前实现依赖 Halo 2.24 的请求级主题上下文契约；如果 Halo 后续调整主题渲染内部 API，需要复测适配器。

## 开发环境

- Java 21+
- Node.js 18+
- pnpm
- Halo 2.24.0+

## 开发

```bash
# 构建插件
./gradlew build

# 开发前端
cd ui
pnpm install
pnpm dev
```

## 构建

```bash
./gradlew build
```

构建完成后，可以在 `build/libs` 目录找到插件 jar 文件。

## 本地验证

```bash
./gradlew test
```

如果 macOS 本地默认没有配置 Java，可以临时指定 `JAVA_HOME` 为 Java 21 安装目录后再运行 Gradle。

## 许可证

[GPL-3.0](./LICENSE) © Muyin 
