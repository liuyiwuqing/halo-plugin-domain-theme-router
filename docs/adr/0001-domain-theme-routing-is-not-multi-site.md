# Domain Theme Routing Is Not Multi-Site

Domain Theme Router presents the same Halo content through different installed themes based on the exact request domain. It deliberately does not isolate posts, pages, menus, attachments, users, or administration per domain because that would turn the plugin into a multi-site system with a much larger ownership and compatibility surface.

**Consequences**

Domain mappings route presentation only. Requests that do not match an enabled mapping fall back to Halo's currently activated theme, and the first version only supports exact domains rather than wildcard domains.
