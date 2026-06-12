# Domain Theme Router

域名主题路由是一个 Halo 插件，用于把精确域名绑定到已安装主题，让同一个 Halo 内容空间可以根据访问域名展示不同的前台主题。

## 简介

第一版目标：

- 支持精确域名到已安装主题的绑定。
- 未命中绑定时回退 Halo 当前激活主题。
- 所有域名共享同一套文章、页面、菜单、附件、用户和后台数据。
- 不支持多站点隔离，不支持通配符域名。

## 实现方式

- 绑定数据存储在插件设置表单中，配置分组为 `routes`，字段为 `bindings` 数组。
- 插件设置入口使用 Halo 插件详情页的配置表单，不再提供独立控制台管理页。
- 每条绑定包含访问域名、绑定主题、启用状态和备注。
- 运行时通过 `AdditionalWebFilter` 读取 `X-Forwarded-Host`，没有该请求头时读取 `Host`。
- 命中启用绑定且目标主题存在后，插件会写入请求级 `ThemeContext` 并将其标记为当前请求的活跃主题，避免菜单等站内链接暴露 `preview-theme`。
- 如果无法构造 Halo 请求级主题上下文，会降级为 Halo 原生主题预览参数 `preview-theme=<themeName>`；未命中或降级也失败时回退 Halo 当前激活主题。

## 架构记录

- [0001-domain-theme-routing-is-not-multi-site](./docs/adr/0001-domain-theme-routing-is-not-multi-site.md)
- [0002-use-request-theme-context-with-preview-fallback](./docs/adr/0002-use-request-theme-context-with-preview-fallback.md)

## 注意事项

- 域名匹配会忽略端口并统一转小写。
- 绑定里的域名可以填写裸域名或带协议的地址，运行时会统一规范化后做精确匹配。
- 绑定主题通过设置表单远程读取已安装 Halo 主题；如果主题被卸载或不可用，运行时自动回退当前激活主题。
- 生产环境使用 Nginx、CDN 或网关时，需要正确传递 `X-Forwarded-Host`。
- 当前实现优先使用请求级 `ThemeContext`，降级时才复用 Halo 的主题预览参数 `preview-theme`；如果系统设置禁用了匿名主题预览，降级路径下的未登录访问可能会回退当前激活主题。

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
