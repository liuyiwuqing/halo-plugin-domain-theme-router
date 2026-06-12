import { definePlugin } from '@halo-dev/ui-shared'
import HomeView from './views/HomeView.vue'
import { markRaw } from 'vue'
import RiGlobalLine from '~icons/ri/global-line'

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'Root',
      route: {
        path: '/domain-theme-router',
        name: 'DomainThemeRouter',
        component: HomeView,
        meta: {
          title: '域名主题路由',
          permissions: ['plugin:domain-theme-router:manage'],
          searchable: true,
          menu: {
            name: '域名主题路由',
            group: '外观',
            icon: markRaw(RiGlobalLine),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
})
