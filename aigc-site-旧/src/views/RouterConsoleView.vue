<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useToast } from '@/composables/useToast'
import {
  createRouterKey,
  deleteRouterKey,
  exportRouterConfig,
  getConnections,
  getRouterKeys,
  getRouterLogs,
  getRouterRouting,
  getRouterStats,
  importRouterConfig,
  toggleRouterKey,
  updateRouterRouting,
} from '@/services/api'
import type { ConnectionConfig, RouterApiKey, RouterRequestLog, RouterRoutingConfig, RouterStats } from '@/types'

const { showToast } = useToast()

const tab = ref<'keys' | 'routing' | 'logs'>('keys')
const loading = ref(false)

const keys = ref<RouterApiKey[]>([])
const createdKey = ref<string>('')
const newKeyName = ref('default-client')

const connections = ref<ConnectionConfig[]>([])
const routing = reactive<RouterRoutingConfig>({
  strategy: 'priority',
  priorityConnectionIds: [],
  failoverEnabled: false,
  failoverTimeoutSeconds: 10,
  timeSchedule: [],
})

const stats = ref<RouterStats>({
  requestsToday: 0,
  requestsWeek: 0,
  requestsMonth: 0,
  tokensToday: 0,
  tokensWeek: 0,
  tokensMonth: 0,
  totalRequests: 0,
  totalTokens: 0,
})
const logs = ref<RouterRequestLog[]>([])
const logTotal = ref(0)
const logPage = ref(1)
const logPageSize = 20
const filters = reactive({
  status: '',
  connectionId: '',
  days: 7,
})

const connectionMap = computed(() => new Map(connections.value.map((item) => [item.id, item])))
const orderedConnections = computed(() =>
  routing.priorityConnectionIds
    .map((id) => connectionMap.value.get(id))
    .filter((item): item is ConnectionConfig => Boolean(item)),
)

async function loadBaseData() {
  loading.value = true
  try {
    const [connList, keyList, routingRes, statsRes] = await Promise.all([
      getConnections(),
      getRouterKeys(),
      getRouterRouting(),
      getRouterStats(),
    ])
    connections.value = connList
    keys.value = keyList
    Object.assign(routing, routingRes)
    stats.value = statsRes
    await loadLogs()
  } catch (e) {
    showToast((e as Error)?.message || '加载路由控制台失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  const page = await getRouterLogs({
    page: logPage.value,
    pageSize: logPageSize,
    status: filters.status || undefined,
    connectionId: filters.connectionId || undefined,
    days: filters.days || undefined,
  })
  logs.value = page.list
  logTotal.value = page.total
}

onMounted(loadBaseData)

async function handleCreateKey() {
  if (!newKeyName.value.trim()) return
  try {
    const created = await createRouterKey(newKeyName.value.trim())
    createdKey.value = created.key || ''
    showToast('路由 API Key 已创建', 'success')
    keys.value = await getRouterKeys()
  } catch (e) {
    showToast((e as Error)?.message || '创建失败', 'error')
  }
}

async function handleToggleKey(key: RouterApiKey) {
  try {
    await toggleRouterKey(key.id, !key.active)
    keys.value = await getRouterKeys()
  } catch (e) {
    showToast((e as Error)?.message || '更新失败', 'error')
  }
}

async function handleDeleteKey(key: RouterApiKey) {
  try {
    await deleteRouterKey(key.id)
    keys.value = await getRouterKeys()
    showToast('路由 API Key 已删除', 'success')
  } catch (e) {
    showToast((e as Error)?.message || '删除失败', 'error')
  }
}

function moveConnection(index: number, step: number) {
  const target = index + step
  if (target < 0 || target >= routing.priorityConnectionIds.length) return
  const next = [...routing.priorityConnectionIds]
  const [item] = next.splice(index, 1)
  next.splice(target, 0, item)
  routing.priorityConnectionIds = next
}

function syncPriorityFromConnections() {
  const knownIds = new Set(routing.priorityConnectionIds)
  const missing = connections.value.filter((item) => !knownIds.has(item.id)).map((item) => item.id)
  if (missing.length) {
    routing.priorityConnectionIds = [...routing.priorityConnectionIds, ...missing]
  }
}

function addTimeSlot() {
  syncPriorityFromConnections()
  routing.timeSchedule.push({
    start: '09:00',
    end: '18:00',
    connectionId: routing.priorityConnectionIds[0] || connections.value[0]?.id || '',
  })
}

function removeTimeSlot(index: number) {
  routing.timeSchedule.splice(index, 1)
}

async function handleSaveRouting() {
  try {
    const payload: RouterRoutingConfig = {
      strategy: routing.strategy,
      priorityConnectionIds: routing.priorityConnectionIds,
      failoverEnabled: routing.failoverEnabled,
      failoverTimeoutSeconds: routing.failoverTimeoutSeconds,
      timeSchedule: routing.timeSchedule.filter((slot) => slot.connectionId),
    }
    const saved = await updateRouterRouting(payload)
    Object.assign(routing, saved)
    showToast('路由配置已保存', 'success')
  } catch (e) {
    showToast((e as Error)?.message || '保存失败', 'error')
  }
}

async function handleExport() {
  try {
    const data = await exportRouterConfig()
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'aigc-router-config.json'
    anchor.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    showToast((e as Error)?.message || '导出失败', 'error')
  }
}

async function handleImport(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const text = await file.text()
    const payload = JSON.parse(text) as Record<string, unknown>
    await importRouterConfig(payload)
    showToast('导入成功，已刷新配置', 'success')
    await loadBaseData()
  } catch (e) {
    showToast((e as Error)?.message || '导入失败', 'error')
  } finally {
    input.value = ''
  }
}

async function refreshLogs() {
  try {
    stats.value = await getRouterStats()
    await loadLogs()
  } catch (e) {
    showToast((e as Error)?.message || '刷新失败', 'error')
  }
}

function totalPages() {
  return Math.max(1, Math.ceil(logTotal.value / logPageSize))
}

async function changeLogPage(next: number) {
  logPage.value = Math.max(1, Math.min(totalPages(), next))
  await loadLogs()
}

function connectionName(id: string) {
  return connectionMap.value.get(id)?.name || id
}
</script>

<template>
  <section class="router-console">
    <header class="page-header">
      <div>
        <h2>路由控制台</h2>
        <p class="muted">这里对应迁移进来的 llm-router 能力：客户端 API Key、优先级路由、统计与日志。</p>
      </div>
      <div class="header-actions">
        <button class="ghost-btn" @click="handleExport">导出配置</button>
        <label class="ghost-btn file-btn">
          导入配置
          <input type="file" accept="application/json" @change="handleImport" />
        </label>
      </div>
    </header>

    <div class="stats-grid">
      <div class="stat-card glass">
        <span class="muted">今日请求</span>
        <strong>{{ stats.requestsToday }}</strong>
      </div>
      <div class="stat-card glass">
        <span class="muted">近 7 天请求</span>
        <strong>{{ stats.requestsWeek }}</strong>
      </div>
      <div class="stat-card glass">
        <span class="muted">总请求数</span>
        <strong>{{ stats.totalRequests }}</strong>
      </div>
      <div class="stat-card glass">
        <span class="muted">总 Token</span>
        <strong>{{ stats.totalTokens }}</strong>
      </div>
    </div>

    <div class="tab-nav">
      <button class="tab" :class="{ active: tab === 'keys' }" @click="tab = 'keys'">API Keys</button>
      <button class="tab" :class="{ active: tab === 'routing' }" @click="tab = 'routing'">路由策略</button>
      <button class="tab" :class="{ active: tab === 'logs' }" @click="tab = 'logs'">日志统计</button>
    </div>

    <section v-if="tab === 'keys'" class="panel glass section">
      <div class="row">
        <input v-model="newKeyName" class="ctrl" type="text" placeholder="请输入客户端名称，例如：my-app" />
        <button class="primary-btn" @click="handleCreateKey">生成 Key</button>
      </div>
      <div v-if="createdKey" class="created-key">
        <strong>新 Key：</strong>
        <code>{{ createdKey }}</code>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>Key</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="key in keys" :key="key.id">
            <td>{{ key.name }}</td>
            <td><code>{{ key.maskedKey }}</code></td>
            <td>{{ key.active ? '启用' : '禁用' }}</td>
            <td>{{ new Date(key.createdAt).toLocaleString() }}</td>
            <td class="actions">
              <button class="ghost-btn small" @click="handleToggleKey(key)">{{ key.active ? '禁用' : '启用' }}</button>
              <button class="ghost-btn small danger" @click="handleDeleteKey(key)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-else-if="tab === 'routing'" class="panel glass section">
      <div class="form-grid">
        <label class="field">
          <span class="label">策略模式</span>
          <select v-model="routing.strategy" class="ctrl">
            <option value="priority">固定优先级</option>
            <option value="time_schedule">时间计划路由</option>
          </select>
        </label>
        <label class="field">
          <span class="label">故障转移</span>
          <input v-model="routing.failoverEnabled" type="checkbox" class="toggle" />
        </label>
        <label class="field">
          <span class="label">超时秒数</span>
          <input v-model.number="routing.failoverTimeoutSeconds" class="ctrl" type="number" min="1" max="120" />
        </label>
      </div>

      <div class="sub-block">
        <div class="sub-header">
          <h3>优先级列表</h3>
          <button class="ghost-btn small" @click="syncPriorityFromConnections">同步连接</button>
        </div>
        <div v-if="orderedConnections.length === 0" class="muted">暂无可排序连接，请先去模型配置中创建连接。</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>顺序</th>
              <th>名称</th>
              <th>提供商</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(conn, index) in orderedConnections" :key="conn.id">
              <td>{{ index + 1 }}</td>
              <td>{{ conn.name }}</td>
              <td>{{ conn.provider }}</td>
              <td class="actions">
                <button class="ghost-btn small" @click="moveConnection(index, -1)">上移</button>
                <button class="ghost-btn small" @click="moveConnection(index, 1)">下移</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="sub-block" v-if="routing.strategy === 'time_schedule'">
        <div class="sub-header">
          <h3>时间计划</h3>
          <button class="ghost-btn small" @click="addTimeSlot">新增时段</button>
        </div>
        <div v-if="routing.timeSchedule.length === 0" class="muted">暂无时间计划，可添加多个时段。</div>
        <div v-for="(slot, index) in routing.timeSchedule" :key="`${slot.start}-${slot.end}-${index}`" class="slot-row">
          <input v-model="slot.start" class="ctrl" type="time" />
          <input v-model="slot.end" class="ctrl" type="time" />
          <select v-model="slot.connectionId" class="ctrl">
            <option value="" disabled>选择连接</option>
            <option v-for="conn in connections" :key="conn.id" :value="conn.id">{{ conn.name }}</option>
          </select>
          <button class="ghost-btn small danger" @click="removeTimeSlot(index)">删除</button>
        </div>
      </div>

      <div class="footer-actions">
        <button class="primary-btn" @click="handleSaveRouting">保存路由配置</button>
      </div>
    </section>

    <section v-else class="panel glass section">
      <div class="row wrap">
        <select v-model="filters.status" class="ctrl">
          <option value="">全部状态</option>
          <option value="success">success</option>
          <option value="failover">failover</option>
          <option value="error">error</option>
        </select>
        <select v-model="filters.connectionId" class="ctrl">
          <option value="">全部连接</option>
          <option v-for="conn in connections" :key="conn.id" :value="conn.id">{{ conn.name }}</option>
        </select>
        <select v-model.number="filters.days" class="ctrl">
          <option :value="1">近 1 天</option>
          <option :value="7">近 7 天</option>
          <option :value="30">近 30 天</option>
        </select>
        <button class="ghost-btn" @click="refreshLogs">刷新</button>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>连接</th>
            <th>模型</th>
            <th>状态</th>
            <th>耗时</th>
            <th>Token</th>
            <th>错误</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in logs" :key="item.id">
            <td>{{ new Date(item.timestamp).toLocaleString() }}</td>
            <td>{{ item.connectionName || connectionName(item.connectionId || '') }}</td>
            <td>{{ item.model || '-' }}</td>
            <td>{{ item.status }}</td>
            <td>{{ item.durationMs }}ms</td>
            <td>{{ item.totalTokens }}</td>
            <td class="error-cell">{{ item.errorMessage || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <div class="pager">
        <button class="ghost-btn small" :disabled="logPage <= 1" @click="changeLogPage(logPage - 1)">上一页</button>
        <span>{{ logPage }} / {{ totalPages() }}</span>
        <button class="ghost-btn small" :disabled="logPage >= totalPages()" @click="changeLogPage(logPage + 1)">下一页</button>
      </div>
    </section>
  </section>
</template>

<style scoped>
.router-console {
  display: grid;
  gap: var(--space-lg);
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  align-items: flex-start;
}

.page-header h2 {
  margin: 0 0 var(--space-xs);
}

.header-actions,
.row,
.actions,
.pager,
.tab-nav,
.footer-actions {
  display: flex;
  gap: var(--space-sm);
}

.header-actions,
.footer-actions {
  flex-wrap: wrap;
}

.file-btn {
  position: relative;
  overflow: hidden;
}

.file-btn input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-md);
}

.stat-card,
.section {
  padding: var(--space-lg);
}

.stat-card {
  display: grid;
  gap: var(--space-xs);
}

.stat-card strong {
  font-size: 1.5rem;
}

.tab {
  min-height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
}

.tab.active {
  color: var(--text-main);
  border-color: color-mix(in srgb, var(--primary) 45%, transparent);
  background: var(--tint-primary-14);
}

.ctrl {
  min-height: 40px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--field-bg);
  color: var(--text-main);
  padding: 0 12px;
}

.primary-btn,
.ghost-btn {
  min-height: 40px;
  border-radius: var(--radius-md);
  padding: 0 14px;
  cursor: pointer;
}

.primary-btn {
  border: none;
  background: var(--primary);
  color: #fff;
}

.ghost-btn {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-main);
}

.ghost-btn.small {
  min-height: 34px;
  padding: 0 10px;
}

.danger {
  color: var(--danger);
}

.created-key {
  margin-top: var(--space-md);
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--primary) 35%, transparent);
  border-radius: var(--radius-md);
  background: var(--tint-primary-14);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: var(--space-md);
}

.data-table th,
.data-table td {
  padding: 12px 10px;
  border-bottom: 1px solid var(--line);
  text-align: left;
  vertical-align: top;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-md);
}

.field {
  display: grid;
  gap: var(--space-xs);
}

.label {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.sub-block {
  margin-top: var(--space-lg);
}

.sub-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
}

.sub-header h3 {
  margin: 0;
}

.slot-row {
  margin-top: var(--space-sm);
  display: grid;
  grid-template-columns: 120px 120px minmax(0, 1fr) auto;
  gap: var(--space-sm);
}

.wrap {
  flex-wrap: wrap;
}

.error-cell {
  max-width: 240px;
  word-break: break-word;
}

.pager {
  margin-top: var(--space-md);
  align-items: center;
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .stats-grid,
  .form-grid {
    grid-template-columns: 1fr 1fr;
  }

  .slot-row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .page-header,
  .stats-grid,
  .form-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
