# Use Request Theme Context With Preview Fallback

Halo renders theme links through `ThemeLinkBuilder`. When a request is rendered as a non-active preview theme, Halo appends `preview-theme=<themeName>` to internal links so preview state survives navigation. That behavior is useful for manual theme preview, but it exposes preview query parameters on domain-bound theme routes.

Domain Theme Router will therefore prefer a request-scoped Halo `ThemeContext` for matched domain routes. The filter builds a `ThemeContext` for the matched theme, marks it active for the current request, and stores it under `run.halo.app.theme.ThemeContext` on `ServerWebExchange`. Because the context is active, Halo's link builder keeps generated menu and navigation links clean.

The plugin does not replace Halo theme beans or call `ThemeResolver#getThemeContext`. `ThemeContext` is an application-internal class, so the plugin creates it through a small runtime adapter and fails closed.

**Consequences**

If request-scoped `ThemeContext` creation succeeds, domain-bound pages render with clean internal links and no visible `preview-theme` query parameter. If the internal class changes, the target theme lacks a reconciled status location, or the adapter cannot create the context, the filter falls back to Halo's native `preview-theme` request parameter. If no domain route matches, the route is disabled, the configured theme is missing, or route lookup fails, the request is left unchanged and Halo falls back to the activated theme.
