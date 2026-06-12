<script setup lang="ts">
import { axiosInstance } from '@halo-dev/api-client'
import { computed, onMounted, reactive, ref } from 'vue'
import Logo from '@/assets/logo.svg'

type Metadata = {
  name: string
}

type DomainThemeRoute = {
  apiVersion?: string
  kind?: string
  metadata: Metadata
  domain: string
  themeName: string
  enabled?: boolean
  remark?: string
}

type Theme = {
  metadata: Metadata
  spec?: {
    displayName?: string
  }
}

type ListResult<T> = {
  items: T[]
}

type RouteForm = {
  name?: string
  domain: string
  themeName: string
  enabled: boolean
  remark: string
}

const ROUTE_API = '/apis/domain-theme-router.muyin.site/v1alpha1/domainThemeRoutes'
const THEME_API = '/apis/theme.halo.run/v1alpha1/themes'

const routes = ref<DomainThemeRoute[]>([])
const themes = ref<Theme[]>([])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('all')

const form = reactive<RouteForm>({
  domain: '',
  themeName: '',
  enabled: true,
  remark: '',
})

const enabledCount = computed(() => routes.value.filter((route) => route.enabled !== false).length)
const filteredRoutes = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return routes.value.filter((route) => {
    const enabled = route.enabled !== false
    const statusMatched =
      statusFilter.value === 'all' ||
      (statusFilter.value === 'enabled' && enabled) ||
      (statusFilter.value === 'disabled' && !enabled)
    const keywordMatched =
      !normalizedKeyword ||
      route.domain.toLowerCase().includes(normalizedKeyword) ||
      route.themeName.toLowerCase().includes(normalizedKeyword)
    return statusMatched && keywordMatched
  })
})

onMounted(() => {
  void refresh()
})

async function refresh() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [routeResponse, themeResponse] = await Promise.all([
      axiosInstance.get<ListResult<DomainThemeRoute>>(ROUTE_API, {
        params: { page: 1, size: 100 },
      }),
      axiosInstance.get<ListResult<Theme>>(THEME_API, {
        params: { page: 1, size: 200 },
      }),
    ])
    routes.value = routeResponse.data.items ?? []
    themes.value = themeResponse.data.items ?? []
    if (!form.themeName && themes.value.length > 0) {
      form.themeName = themes.value[0].metadata.name
    }
  } catch (error) {
    console.error(error)
    errorMessage.value = '加载域名主题绑定失败'
  } finally {
    loading.value = false
  }
}

async function saveRoute() {
  const domain = normalizeDomain(form.domain)
  if (!domain) {
    errorMessage.value = '请输入有效域名'
    return
  }
  if (!form.themeName) {
    errorMessage.value = '请选择绑定主题'
    return
  }

  saving.value = true
  errorMessage.value = ''
  const payload: DomainThemeRoute = {
    apiVersion: 'domain-theme-router.muyin.site/v1alpha1',
    kind: 'DomainThemeRoute',
    metadata: {
      name: form.name || routeNameForDomain(domain),
    },
    domain,
    themeName: form.themeName,
    enabled: form.enabled,
    remark: form.remark.trim(),
  }

  try {
    if (form.name) {
      await axiosInstance.put(`${ROUTE_API}/${form.name}`, payload)
    } else {
      await axiosInstance.post(ROUTE_API, payload)
    }
    resetForm()
    await refresh()
  } catch (error) {
    console.error(error)
    errorMessage.value = '保存绑定失败，请检查域名是否重复'
  } finally {
    saving.value = false
  }
}

function editRoute(route: DomainThemeRoute) {
  form.name = route.metadata.name
  form.domain = route.domain
  form.themeName = route.themeName
  form.enabled = route.enabled !== false
  form.remark = route.remark || ''
}

async function deleteRoute(route: DomainThemeRoute) {
  if (!window.confirm(`确认删除 ${route.domain} 的主题绑定？`)) {
    return
  }
  errorMessage.value = ''
  try {
    await axiosInstance.delete(`${ROUTE_API}/${route.metadata.name}`)
    await refresh()
  } catch (error) {
    console.error(error)
    errorMessage.value = '删除绑定失败'
  }
}

function resetForm() {
  form.name = undefined
  form.domain = ''
  form.themeName = themes.value[0]?.metadata.name || ''
  form.enabled = true
  form.remark = ''
}

function themeLabel(themeName: string) {
  const theme = themes.value.find((item) => item.metadata.name === themeName)
  return theme?.spec?.displayName ? `${theme.spec.displayName} (${themeName})` : themeName
}

function normalizeDomain(value: string) {
  let candidate = value.trim()
  if (!candidate) {
    return ''
  }
  if (candidate.includes(',')) {
    candidate = candidate.split(',')[0].trim()
  }
  if (!candidate.includes('://')) {
    candidate = `https://${candidate}`
  }
  try {
    return new URL(candidate).hostname.toLowerCase()
  } catch {
    return ''
  }
}

function routeNameForDomain(domain: string) {
  const normalized = domain
    .toLowerCase()
    .replace(/[^a-z0-9.-]/g, '-')
    .replace(/\./g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
    .slice(0, 180)
    .replace(/-$/g, '')
  const fallback = crypto.randomUUID?.() || Date.now().toString(36)
  return `domain-theme-route-${normalized || fallback}`
}
</script>

<template>
  <section class="domain-theme-router-admin">
    <div class="domain-theme-router-admin__header">
      <div class="domain-theme-router-admin__identity">
        <img :src="Logo" alt="" class="domain-theme-router-admin__logo" />
        <div>
          <h1>域名主题路由</h1>
          <p>根据访问域名选择已安装主题，未命中时回退当前激活主题。</p>
        </div>
      </div>
      <button class="domain-theme-router-admin__primary-button" type="button" @click="resetForm">
        新增绑定
      </button>
    </div>

    <div class="domain-theme-router-admin__summary">
      <div>
        <span>精确域名</span>
        <strong>{{ routes.length }}</strong>
      </div>
      <div>
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </div>
      <div>
        <span>可选主题</span>
        <strong>{{ themes.length }}</strong>
      </div>
    </div>

    <div v-if="errorMessage" class="domain-theme-router-admin__notice">
      {{ errorMessage }}
    </div>

    <div class="domain-theme-router-admin__panel">
      <form class="domain-theme-router-admin__form" @submit.prevent="saveRoute">
        <label>
          <span>访问域名</span>
          <input v-model="form.domain" type="text" placeholder="demo.muyin.site" />
        </label>
        <label>
          <span>绑定主题</span>
          <select v-model="form.themeName">
            <option v-for="theme in themes" :key="theme.metadata.name" :value="theme.metadata.name">
              {{ themeLabel(theme.metadata.name) }}
            </option>
          </select>
        </label>
        <label>
          <span>备注</span>
          <input v-model="form.remark" type="text" placeholder="选填" />
        </label>
        <label class="domain-theme-router-admin__switch">
          <input v-model="form.enabled" type="checkbox" />
          <span>启用</span>
        </label>
        <div class="domain-theme-router-admin__form-actions">
          <button type="button" @click="resetForm">重置</button>
          <button type="submit" :disabled="saving || themes.length === 0">
            {{ form.name ? '保存修改' : '创建绑定' }}
          </button>
        </div>
      </form>

      <div class="domain-theme-router-admin__toolbar">
        <input v-model="keyword" type="search" placeholder="搜索域名或主题" />
        <select v-model="statusFilter">
          <option value="all">全部状态</option>
          <option value="enabled">启用</option>
          <option value="disabled">禁用</option>
        </select>
        <button type="button" :disabled="loading" @click="refresh">刷新</button>
      </div>

      <div class="domain-theme-router-admin__table">
        <div class="domain-theme-router-admin__table-head">
          <span>访问域名</span>
          <span>绑定主题</span>
          <span>状态</span>
          <span>操作</span>
        </div>
        <div
          v-for="route in filteredRoutes"
          :key="route.metadata.name"
          class="domain-theme-router-admin__table-row"
        >
          <span>{{ route.domain }}</span>
          <span>{{ themeLabel(route.themeName) }}</span>
          <span>
            <i :class="{ 'is-enabled': route.enabled !== false }"></i>
            {{ route.enabled !== false ? '启用' : '禁用' }}
          </span>
          <span class="domain-theme-router-admin__actions">
            <button type="button" @click="editRoute(route)">编辑</button>
            <button type="button" @click="deleteRoute(route)">删除</button>
          </span>
        </div>
        <div v-if="!loading && filteredRoutes.length === 0" class="domain-theme-router-admin__empty">
          暂无绑定
        </div>
        <div v-if="loading" class="domain-theme-router-admin__empty">加载中</div>
      </div>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.domain-theme-router-admin {
  min-height: 100vh;
  padding: 24px;
  background: #f8fafc;
  color: #0f172a;
}

.domain-theme-router-admin__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  max-width: 1120px;
  margin: 0 auto 20px;
}

.domain-theme-router-admin__identity {
  display: flex;
  align-items: center;
  gap: 14px;

  h1 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    line-height: 1.3;
  }

  p {
    margin: 4px 0 0;
    color: #64748b;
    font-size: 14px;
  }
}

.domain-theme-router-admin__logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
}

.domain-theme-router-admin__primary-button,
.domain-theme-router-admin__form-actions button,
.domain-theme-router-admin__toolbar button,
.domain-theme-router-admin__actions button {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  color: #0f172a;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
}

.domain-theme-router-admin__primary-button {
  padding: 10px 14px;
  background: #0f172a;
  color: #fff;
  border-color: #0f172a;
}

.domain-theme-router-admin__summary,
.domain-theme-router-admin__notice,
.domain-theme-router-admin__panel {
  max-width: 1120px;
  margin: 0 auto;
}

.domain-theme-router-admin__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;

  div {
    padding: 16px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    background: #fff;
  }

  span {
    display: block;
    color: #64748b;
    font-size: 13px;
  }

  strong {
    display: block;
    margin-top: 8px;
    font-size: 20px;
  }
}

.domain-theme-router-admin__notice {
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
  color: #991b1b;
  font-size: 14px;
}

.domain-theme-router-admin__panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.domain-theme-router-admin__form {
  display: grid;
  grid-template-columns: 1.2fr 1.2fr 1fr auto auto;
  gap: 12px;
  align-items: end;
  padding: 14px;
  border-bottom: 1px solid #e2e8f0;

  label {
    min-width: 0;
  }

  label > span {
    display: block;
    margin-bottom: 6px;
    color: #64748b;
    font-size: 13px;
  }
}

.domain-theme-router-admin__toolbar {
  display: flex;
  gap: 10px;
  padding: 14px;
  border-bottom: 1px solid #e2e8f0;
}

.domain-theme-router-admin__form input,
.domain-theme-router-admin__form select,
.domain-theme-router-admin__toolbar input,
.domain-theme-router-admin__toolbar select {
  width: 100%;
  height: 36px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  color: #0f172a;
  font-size: 14px;
}

.domain-theme-router-admin__form input,
.domain-theme-router-admin__toolbar input {
  padding: 0 12px;
}

.domain-theme-router-admin__form select,
.domain-theme-router-admin__toolbar select {
  padding: 0 10px;
}

.domain-theme-router-admin__switch {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;

  input {
    width: 16px;
    height: 16px;
  }
}

.domain-theme-router-admin__form-actions,
.domain-theme-router-admin__actions {
  display: flex;
  gap: 8px;
}

.domain-theme-router-admin__form-actions button,
.domain-theme-router-admin__toolbar button,
.domain-theme-router-admin__actions button {
  height: 36px;
  padding: 0 10px;
}

.domain-theme-router-admin__form-actions button[type='submit'] {
  background: #0f172a;
  color: #fff;
  border-color: #0f172a;
}

.domain-theme-router-admin__toolbar input {
  flex: 1;
  min-width: 0;
}

.domain-theme-router-admin__toolbar select {
  width: 140px;
}

.domain-theme-router-admin__table {
  overflow: hidden;
}

.domain-theme-router-admin__table-head,
.domain-theme-router-admin__table-row {
  display: grid;
  grid-template-columns: 1.4fr 1.4fr 0.7fr 0.8fr;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  font-size: 14px;
}

.domain-theme-router-admin__table-head {
  color: #64748b;
  background: #f8fafc;
  font-weight: 600;
}

.domain-theme-router-admin__table-row {
  border-top: 1px solid #e2e8f0;

  i {
    display: inline-block;
    width: 8px;
    height: 8px;
    margin-right: 6px;
    border-radius: 999px;
    background: #94a3b8;
  }

  i.is-enabled {
    background: #14b8a6;
  }
}

.domain-theme-router-admin__empty {
  padding: 28px 14px;
  color: #64748b;
  text-align: center;
  font-size: 14px;
}

@media (max-width: 900px) {
  .domain-theme-router-admin__form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .domain-theme-router-admin {
    padding: 16px;
  }

  .domain-theme-router-admin__header,
  .domain-theme-router-admin__toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .domain-theme-router-admin__summary {
    grid-template-columns: 1fr;
  }

  .domain-theme-router-admin__toolbar select {
    width: 100%;
  }

  .domain-theme-router-admin__table-head {
    display: none;
  }

  .domain-theme-router-admin__table-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
</style>
