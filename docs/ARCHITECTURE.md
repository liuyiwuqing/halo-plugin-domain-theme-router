# Domain Theme Router 技术架构文档

## 概述

Domain Theme Router 是一个 Halo CMS 插件，通过拦截 HTTP 请求并根据访问域名动态切换前台主题。本文档描述了插件的技术架构、核心组件和实现细节。

---

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      HTTP Request                           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                  DomainThemeRouteWebFilter                   │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  1. shouldSkip() - 跳过 API/Console/静态资源请求       │  │
│  │  2. resolveRequestDomain() - 提取请求域名              │  │
│  │  3. routeService.listEnabledRoutesAsList() - 获取路由  │  │
│  │  4. routeMatcher.match() - 匹配域名                    │  │
│  │  5. applyThemePreview() - 应用主题                     │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    主题应用策略                               │
│  ┌─────────────────┐    ┌─────────────────┐                 │
│  │ ThemeContext     │    │ preview-theme   │                 │
│  │ (优先方案)       │    │ (降级方案)       │                 │
│  └─────────────────┘    └─────────────────┘                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Halo 主题渲染                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 核心组件

### 1. DomainThemeRouterPlugin

**位置**: `src/main/java/site/muyin/domainthemerouter/DomainThemeRouterPlugin.java`

插件主类，继承 `BasePlugin`，负责插件生命周期管理。

```java
@Component
public class DomainThemeRouterPlugin extends BasePlugin {
    public DomainThemeRouterPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }
}
```

### 2. DomainThemeRouteWebFilter

**位置**: `src/main/java/site/muyin/domainthemerouter/filter/DomainThemeRouteWebFilter.java`

核心过滤器，实现 `AdditionalWebFilter` 接口，在请求处理链中拦截并应用主题。

**关键方法**:

| 方法 | 说明 |
|------|------|
| `filter()` | 主过滤逻辑 |
| `shouldSkip()` | 判断是否跳过请求（API/Console/静态资源） |
| `resolveRequestDomain()` | 从请求头提取域名 |
| `applyThemePreview()` | 应用主题预览 |
| `withDomainTheme()` | 构建主题上下文 |

**请求处理流程**:

```java
public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // 1. 跳过非页面请求
    if (shouldSkip(exchange)) {
        return chain.filter(exchange);
    }
    
    // 2. 提取请求域名
    var requestDomain = resolveRequestDomain(exchange);
    if (StringUtils.isBlank(requestDomain)) {
        return chain.filter(exchange);
    }
    
    // 3. 匹配并应用主题
    return routeService.listEnabledRoutesAsList()
            .flatMap(routes -> routeMatcher.match(requestDomain, routes)
                    .map(route -> applyThemePreview(exchange, route.getThemeName()))
                    .orElseGet(() -> Mono.just(exchange)))
            .onErrorResume(error -> {
                log.warn("Failed to resolve domain theme route, fallback to activated theme.", error);
                return Mono.just(exchange);
            })
            .flatMap(chain::filter);
}
```

**请求跳过规则**:

```java
private boolean shouldSkip(ServerWebExchange exchange) {
    var path = exchange.getRequest().getPath().pathWithinApplication().value();
    return path.startsWith("/apis/")      // Halo API
            || path.startsWith("/api/")    // 自定义 API
            || path.startsWith("/console") // 后台管理
            || path.startsWith("/uc")      // 用户中心
            || path.startsWith("/plugins/") // 插件资源
            || path.startsWith("/themes/")  // 主题资源
            || path.startsWith("/assets/")  // 静态资源
            || path.startsWith("/actuator"); // 监控端点
}
```

### 3. DomainThemeRouteMatcher

**位置**: `src/main/java/site/muyin/domainthemerouter/service/DomainThemeRouteMatcher.java`

域名匹配器，负责规范化域名并执行精确匹配。

**核心逻辑**:

```java
public Optional<DomainThemeRoute> match(String requestDomain, List<DomainThemeRoute> routes) {
    var normalizedRequestDomain = normalizeRequestDomain(requestDomain);
    if (normalizedRequestDomain.isEmpty()) {
        return Optional.empty();
    }
    
    return Optional.ofNullable(routes).orElse(Collections.emptyList()).stream()
            .filter(route -> Boolean.TRUE.equals(route.getEnabled()))
            .filter(route -> normalizeRequestDomain(route.getDomain())
                    .filter(domain -> domain.equals(normalizedRequestDomain.get()))
                    .isPresent())
            .findFirst();
}
```

**域名规范化规则**:

1. **去除空白**: `trim()` 处理
2. **处理逗号分隔**: 取第一个域名（处理 `X-Forwarded-Host` 多值情况）
3. **添加协议**: 无协议时添加 `http://`
4. **提取主机名**: 使用 `URI.create()` 解析
5. **统一小写**: `toLowerCase(Locale.ROOT)`

```java
public Optional<String> normalizeRequestDomain(String value) {
    if (StringUtils.isBlank(value)) {
        return Optional.empty();
    }
    
    var candidate = StringUtils.trim(value);
    if (candidate.contains(",")) {
        candidate = StringUtils.substringBefore(candidate, ",");
    }
    candidate = StringUtils.trim(candidate);
    if (StringUtils.isBlank(candidate)) {
        return Optional.empty();
    }
    
    if (!candidate.contains("://")) {
        candidate = "http://" + candidate;
    }
    
    try {
        var host = URI.create(candidate).getHost();
        return Optional.ofNullable(StringUtils.trimToNull(host))
                .map(domain -> domain.toLowerCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
        return Optional.empty();
    }
}
```

### 4. DomainThemeRouteService

**位置**: `src/main/java/site/muyin/domainthemerouter/service/DomainThemeRouteService.java`

路由服务接口，定义路由数据访问契约。

```java
public interface DomainThemeRouteService {
    Mono<List<DomainThemeRoute>> listEnabledRoutesAsList();
}
```

### 5. DomainThemeRouteServiceImpl

**位置**: `src/main/java/site/muyin/domainthemerouter/service/impl/DomainThemeRouteServiceImpl.java`

路由服务实现，从 Halo 插件设置中读取配置。

### 6. HaloThemeContextFactory

**位置**: `src/main/java/site/muyin/domainthemerouter/theme/HaloThemeContextFactory.java`

主题上下文工厂，负责创建 Halo 请求级主题上下文。

### 7. 数据模型

#### DomainThemeRoute

```java
@Data
@Accessors(chain = true)
public class DomainThemeRoute {
    private String domain;      // 访问域名
    private String themeName;   // 绑定主题名称
    private Boolean enabled = true;  // 启用状态
    private String remark;      // 备注
}
```

#### DomainThemeRouteSettings

```java
@Data
public class DomainThemeRouteSettings {
    private List<DomainThemeRoute> bindings = new ArrayList<>();
}
```

---

## 主题应用策略

### 策略优先级

```
1. 请求级 ThemeContext (优先)
      ↓ 失败
2. preview-theme 查询参数 (降级)
      ↓ 失败
3. Halo 默认激活主题 (兜底)
```

### 策略 1: 请求级 ThemeContext (优先)

**实现位置**: `withThemeContext()`

```java
private ServerWebExchange withThemeContext(ServerWebExchange exchange, Object themeContext) {
    var request = exchange.getRequest().mutate()
            .uri(UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                    .replaceQueryParam(THEME_PREVIEW_PARAM)
                    .build(true)
                    .toUri())
            .build();
    var mutatedExchange = exchange.mutate()
            .request(request)
            .build();
    mutatedExchange.getAttributes().put(HALO_THEME_CONTEXT_ATTRIBUTE, themeContext);
    return mutatedExchange;
}
```

**优点**:
- 不暴露 `preview-theme` 参数
- 菜单等站内链接不会泄露主题切换信息
- 更安全、更专业

**实现细节**:
- 移除 `preview-theme` 查询参数
- 在请求属性中设置 `run.halo.app.theme.ThemeContext`
- 使用 `HaloThemeContextFactory` 创建上下文

### 策略 2: preview-theme 查询参数 (降级)

**实现位置**: `withPreviewTheme()`

```java
private ServerWebExchange withPreviewTheme(ServerWebExchange exchange, String themeName) {
    var uri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
            .replaceQueryParam(THEME_PREVIEW_PARAM, themeName)
            .build(true)
            .toUri();
    var request = exchange.getRequest().mutate()
            .uri(uri)
            .build();
    var mutatedExchange = exchange.mutate()
            .request(request)
            .build();
    mutatedExchange.getAttributes().remove(HALO_THEME_CONTEXT_ATTRIBUTE);
    return mutatedExchange;
}
```

**适用场景**:
- 无法创建请求级 ThemeContext 时
- 主题路径无法解析时

**注意事项**:
- 会在 URL 中暴露 `preview-theme` 参数
- 菜单等站内链接可能包含该参数
- 如果系统禁用了匿名主题预览，未登录用户可能无法看到切换后的主题

### 策略 3: Halo 默认激活主题 (兜底)

当以上两种策略都失败时，请求会继续传递到 Halo 默认的主题处理逻辑。

---

## 请求域名解析

### 解析优先级

```java
private String resolveRequestDomain(ServerWebExchange exchange) {
    var headers = exchange.getRequest().getHeaders();
    var forwardedHost = headers.getFirst(FORWARDED_HOST);
    if (StringUtils.isNotBlank(forwardedHost)) {
        return forwardedHost;
    }
    return headers.getFirst(HttpHeaders.HOST);
}
```

1. **X-Forwarded-Host**: 优先使用（反向代理场景）
2. **Host**: 回退使用（直连场景）

### 反向代理配置

**Nginx 配置示例**:

```nginx
server {
    listen 80;
    server_name example.com;
    
    location / {
        proxy_pass http://localhost:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Host $host;
    }
}
```

---

## 性能优化

### 1. 请求跳过策略

跳过所有非页面请求，避免不必要的域名匹配：
- API 请求 (`/apis/`, `/api/`)
- 后台管理 (`/console`)
- 用户中心 (`/uc`)
- 静态资源 (`/plugins/`, `/themes/`, `/assets/`)
- 监控端点 (`/actuator`)

### 2. 响应式编程

使用 Project Reactor 实现非阻塞处理：
- `Mono<List<DomainThemeRoute>>` 异步获取路由配置
- `flatMap` 链式处理
- `onErrorResume` 错误降级

### 3. 过滤器优先级

```java
@Override
public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 100;
}
```

高优先级执行，确保在其他过滤器之前完成主题切换。

---

## 错误处理

### 降级策略

```java
.onErrorResume(error -> {
    log.warn("Failed to resolve domain theme route, fallback to activated theme.", error);
    return Mono.just(exchange);
})
```

任何错误都会：
1. 记录警告日志
2. 回退到 Halo 默认激活主题
3. 确保站点始终可用

### 主题验证

```java
private Mono<ServerWebExchange> applyThemePreview(ServerWebExchange exchange, String themeName) {
    if (StringUtils.isBlank(themeName)) {
        return Mono.just(exchange);
    }
    return client.fetch(Theme.class, themeName)
            .map(theme -> withDomainTheme(exchange, theme))
            .switchIfEmpty(Mono.fromSupplier(() -> {
                log.warn("Domain theme route matched missing theme {}, fallback to activated theme.",
                        themeName);
                return exchange;
            }));
}
```

- 检查主题名称是否为空
- 验证主题是否存在于 Halo 系统
- 不存在时自动回退并记录日志

---

## 扩展点

### 1. 自定义域名匹配规则

可以通过实现自定义的 `DomainThemeRouteMatcher` 来扩展匹配逻辑，例如：
- 正则表达式匹配
- 通配符匹配
- 子域名匹配

### 2. 主题切换事件

可以监听主题切换事件，实现：
- 切换日志记录
- 统计分析
- 自定义处理逻辑

### 3. 配置来源扩展

当前配置存储在 Halo 插件设置中，可以扩展为：
- 数据库存储
- 外部配置中心
- 文件配置

---

## 测试策略

### 单元测试

- `DomainThemeRouteMatcherTest`: 域名匹配逻辑
- `DomainThemeRouteServiceImplTest`: 路由服务实现
- `HaloThemeContextFactoryTest`: 主题上下文创建
- `DomainThemeRouteWebFilterTest`: WebFilter 集成测试

### 测试覆盖

- ✅ 正常域名匹配
- ✅ 域名规范化处理
- ✅ 主题不存在降级
- ✅ 配置为空处理
- ✅ 错误场景降级
- ✅ 请求跳过逻辑

---

## 依赖关系

### 核心依赖

```gradle
dependencies {
    implementation platform("run.halo.tools.platform:plugin:$haloPlatformVersion")
    compileOnly 'run.halo.app:api'
}
```

### 运行时依赖

- **Halo 2.24.0+**: 插件运行平台
- **Spring WebFlux**: 响应式 Web 框架
- **Project Reactor**: 响应式编程库
- **Apache Commons Lang**: 字符串工具
- **Lombok**: 代码简化

---

## 部署架构

### 典型部署

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   用户      │────▶│   Nginx     │────▶│    Halo     │
│  (浏览器)   │     │  (反向代理)  │     │  (应用服务器) │
└─────────────┘     └─────────────┘     └─────────────┘
                           │                   │
                           │                   │
                    ┌──────┴──────┐     ┌──────┴──────┐
                    │  Domain A   │     │  Domain B   │
                    │  Theme A    │     │  Theme B    │
                    └─────────────┘     └─────────────┘
```

### CDN 部署

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   用户      │────▶│    CDN      │────▶│   Origin    │
│  (浏览器)   │     │  (缓存层)   │     │  (源站)     │
└─────────────┘     └─────────────┘     └─────────────┘
                           │                   │
                           ▼                   ▼
                    ┌─────────────┐     ┌─────────────┐
                    │  缓存策略   │     │    Halo     │
                    │  (按域名)   │     │  + 插件     │
                    └─────────────┘     └─────────────┘
```

**注意事项**:
- CDN 缓存需要按域名区分
- 确保传递正确的 `X-Forwarded-Host` 头
- 静态资源可以共享缓存

---

## 安全考虑

### 1. 主题验证

- 验证主题是否存在
- 防止恶意主题注入
- 自动回退机制

### 2. 请求过滤

- 跳过 API 请求，避免影响后端逻辑
- 跳过后台管理，确保管理功能正常
- 跳过静态资源，提高性能

### 3. 错误处理

- 所有错误都有降级方案
- 不暴露内部实现细节
- 记录日志用于排查

---

## 未来规划

### 短期目标

- [ ] 支持通配符域名匹配
- [ ] 支持正则表达式匹配
- [ ] 批量导入/导出配置

### 中期目标

- [ ] 域名分组管理
- [ ] 主题切换统计
- [ ] 配置热更新

### 长期目标

- [ ] 多站点隔离支持
- [ ] 基于路径的主题切换
- [ ] 主题切换 API

---

## 参考文档

- [Halo 插件开发文档](https://docs.halo.run/developer-guide/plugin/)
- [Spring WebFlux 文档](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Project Reactor 文档](https://projectreactor.io/docs)

---

*Domain Theme Router - 让主题切换更智能！*
