# Halo Domain Theme Routing

This context describes a Halo plugin that lets one Halo site present different frontend themes for different request domains while sharing the same content and administration model.

## Language

**Domain Theme Route**:
A binding between one exact request domain and one installed Halo theme. It changes the frontend presentation for matching requests without creating a separate site.
_Avoid_: multi-site, tenant, domain site, wildcard route

**Fallback Theme**:
The currently activated Halo theme used when a request domain does not match any enabled Domain Theme Route.
_Avoid_: default site, backup theme

**Shared Content Space**:
The single set of Halo posts, pages, menus, attachments, users, and administrative data shared by all Domain Theme Routes.
_Avoid_: site content, tenant data, isolated content

**Request Domain**:
The normalized domain used to match a Domain Theme Route. Ports are ignored and case is normalized before matching.
_Avoid_: full URL, site URL, origin
