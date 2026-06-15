# 🎨 Domain Theme Router

> **一套内容，无限可能** —— 让你的 Halo 站点在不同域名下展现不同风采

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Halo Version](https://img.shields.io/badge/Halo-2.24.0+-green.svg)](https://halo.run)

---

## 🚀 这是什么？

Domain Theme Router 是一个 Halo CMS 插件，它可以让你：

- ✅ 为同一个 Halo 站点配置多个域名
- ✅ 每个域名绑定不同的前台主题
- ✅ 所有域名共享相同的文章、页面、菜单和后台管理

**简单来说**：你有两篇文章，但想在 `blog.example.com` 用简约风格，在 `news.example.com` 用新闻风格。这个插件就能帮你实现！

---

## 交流群

[点击链接加入群聊【halo博客-lywq插件】](https://qm.qq.com/q/wuC7NZr0sw)

<img src="https://github.com/user-attachments/assets/bf162401-07fd-49ec-b50f-5218c9510937" style="height: 400px !important; width: auto; object-fit: contain;" />

## ✨ 核心亮点

### 🎯 一石多鸟
一套内容，多个域名，不同主题。无需重复发布，无需多站点管理。

### ⚡ 极速体验
基于 WebFilter 实现，请求处理性能影响微乎其微。用户无感知，体验零延迟。

### 🔌 即插即用
无需修改代码，无需复杂配置。安装插件，配置域名，立即生效。

### 🛡️ 稳定可靠
智能主题验证，自动回退机制。主题不可用时，自动切换到默认主题，确保站点始终可用。

---

## 📖 使用场景

### 🏢 企业多品牌运营
```
brand-a.com → 商务风格主题
brand-b.com → 科技风格主题  
brand-c.com → 创意风格主题
```

### 📰 内容分发网络
```
news.example.com → 新闻资讯风格
blog.example.com → 个人博客风格
docs.example.com → 文档中心风格
```

### 🧪 A/B 测试
```
v1.example.com → 主题 A
v2.example.com → 主题 B
```

### 🌍 多地区站点
```
cn.example.com → 中国风主题
us.example.com → 欧美风主题
jp.example.com → 日系风格主题
```

---

## 🛠️ 快速开始

### 1. 安装插件

1. 下载最新版本的 `halo-plugin-domain-theme-router-x.x.x.jar`
2. 在 Halo 后台 → 插件 → 安装插件
3. 上传 jar 文件并启用

### 2. 配置域名绑定

1. 进入插件设置页面
2. 点击「添加绑定」
3. 填写配置信息：
   - **访问域名**：你的域名，如 `blog.example.com`
   - **绑定主题**：选择已安装的主题
   - **启用状态**：是否激活此绑定
   - **备注**：可选，方便管理

4. 点击保存

### 3. 配置 DNS/反向代理

确保你的域名正确指向 Halo 服务器，并且传递了正确的 `Host` 或 `X-Forwarded-Host` 请求头。

**Nginx 配置示例：**

```nginx
server {
    listen 80;
    server_name blog.example.com;

    location / {
        proxy_pass http://localhost:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Host $host;
    }
}
```

### 4. 验证配置

访问你配置的域名，检查是否显示了对应的主题。

---

## 💡 工作原理

```
用户访问域名
      ↓
  插件拦截请求
      ↓
读取 Host / X-Forwarded-Host
      ↓
  匹配域名绑定
      ↓
┌─────────────────────────────────────┐
│ 命中绑定？                           │
│  ├─ 是 → 使用绑定的主题              │
│  └─ 否 → 使用 Halo 默认激活主题      │
└─────────────────────────────────────┘
```

---

## ⚙️ 配置说明

### 域名格式

插件支持多种域名格式：

| 输入格式 | 实际匹配域名 |
|---------|-------------|
| `blog.example.com` | `blog.example.com` |
| `http://blog.example.com` | `blog.example.com` |
| `https://blog.example.com:443` | `blog.example.com` |
| `BLOG.EXAMPLE.COM` | `blog.example.com` |

### 主题选择

- 只能选择已安装的 Halo 主题
- 如果绑定的主题被卸载，运行时自动回退到默认主题
- 建议在卸载主题前先解除相关域名绑定

---

## 📋 技术规格

| 项目 | 说明 |
|-----|------|
| 最低 Halo 版本 | 2.24.0+ |
| Java 版本 | 21+ |
| 存储方式 | 插件设置表单 |
| 匹配方式 | 精确域名匹配 |
| 性能影响 | 极小（WebFilter 实现） |

---

## ❓ 常见问题

### Q: 为什么访问域名后还是显示默认主题？

**A: 检查以下几点：**
1. 域名绑定是否已启用
2. 域名是否填写正确（检查大小写和端口）
3. DNS 是否已生效
4. 反向代理是否正确传递了 `Host` 或 `X-Forwarded-Host` 请求头
5. 绑定的主题是否已安装且可用

### Q: 可以使用通配符域名吗？

**A: 不支持。** 当前版本只支持精确域名匹配。如果你需要 `*.example.com` 的形式，需要为每个子域名单独配置。

### Q: 多个域名可以绑定同一个主题吗？

**A: 可以。** 你可以让多个域名指向同一个主题。

### Q: 会影响后台管理吗？

**A: 不会。** 所有域名共享同一个 Halo 后台，管理操作完全一致。

### Q: 可以绑定未安装的主题吗？

**A: 不可以。** 设置页面只会显示已安装的主题列表。如果主题被卸载，相关绑定会自动失效并回退到默认主题。

---

## 🔧 开发环境

### 环境要求

- Java 21+
- Node.js 18+
- pnpm
- Halo 2.24.0+

### 开发命令

```bash
# 构建插件
./gradlew build

# 开发前端
cd ui
pnpm install
pnpm dev

# 运行测试
./gradlew test
```

### 本地调试

```bash
# 启动 Halo 开发服务器
./gradlew haloStart

# 访问 Halo 后台
# http://localhost:8181/console
```

---

## 📚 文档资源

- [📖 用户指南](./docs/USER_GUIDE.md) - 详细使用说明
- [🏗️ 技术架构](./docs/ARCHITECTURE.md) - 技术实现细节
- [📢 宣传文档](./docs/PROMOTION.md) - 面向用户的宣传材料
- [📝 ADR 文档](./docs/adr/) - 架构决策记录

---

## 🤝 参与贡献

我们欢迎所有形式的贡献！

- 🐛 报告 Bug
- 💡 提出新功能建议
- 📖 完善文档
- 🔧 提交代码

---

## 📄 许可证

[GPL-3.0](./LICENSE) © liuyiwuqing

---

## 📞 联系我们

- GitHub: [liuyiwuqing/plugin-domain-theme-router](https://github.com/liuyiwuqing/plugin-domain-theme-router)
- Issues: [问题反馈](https://github.com/liuyiwuqing/plugin-domain-theme-router/issues)

---

<div align="center">

**🚀 立即体验，释放 Halo 的无限可能！**

[下载最新版本](https://github.com/liuyiwuqing/plugin-domain-theme-router/releases/latest) | [查看文档](./docs/USER_GUIDE.md) | [反馈问题](https://github.com/liuyiwuqing/plugin-domain-theme-router/issues)

</div>
