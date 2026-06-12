# Use Request Theme Context Override

Halo 2.24 resolves the frontend theme through a `ThemeContext` stored in `ServerWebExchange` attributes before `HaloViewResolver` renders the view. Domain Theme Router will set that request-scoped theme context from an `AdditionalWebFilter` instead of replacing Halo's theme beans, and it will isolate direct access to Halo application-only theme classes behind a small adapter because those classes are not part of the public plugin API.

**Consequences**

The plugin depends on Halo 2.24's request attribute contract for theme resolution. The adapter must fail closed and let Halo use the activated theme when the internal theme context API changes or no domain route matches.
