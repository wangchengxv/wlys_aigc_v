<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useToast } from '@/composables/useToast'
import {
  batchImportModels,
  createConnection,
  createModel,
  deleteConnection,
  deleteModel,
  getConnections,
  getModels,
  getProviderCatalog,
  probeModel,
  testConnection,
  updateConnection,
  updateModel,
} from '@/services/api'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import ConnectionForm from '@/components/model/ConnectionForm.vue'
import ModelForm from '@/components/model/ModelForm.vue'
import type { ConnectionConfig, ConnectionConfigCreateRequest, ModelConfig, ModelConfigCreateRequest, ProviderCatalogEntry } from '@/types'

const { showToast } = useToast()
const loading = ref(false)
const catalogLoading = ref(false)
const connections = ref<ConnectionConfig[]>([])
const models = ref<ModelConfig[]>([])
const catalog = ref<ProviderCatalogEntry[]>([])
const selectedId = ref('')
const modelListFilter = ref('')
const listFilter = ref('')
const showConnForm = ref(false)
const showModelForm = ref(false)
const editingConn = ref<ConnectionConfig | null>(null)
const editingModel = ref<ModelConfig | null>(null)
const showDeleteConfirm = ref(false)
const deletingId = ref('')
const deletingType = ref<'connection' | 'model'>('connection')
const testingId = ref('')
const probingId = ref('')
const importLoading = ref(false)
const importPick = ref<Record<string, boolean>>({})
const lastTestModels = ref<{ connectionId: string; names: string[] } | null>(null)

const filteredConnections = computed(() => {
  const q = listFilter.value.trim().toLowerCase()
  if (!q) return connections.value
  return connections.value.filter(
    (c) =>
      c.name.toLowerCase().includes(q) ||
      c.provider.toLowerCase().includes(q) ||
      c.baseUrl.toLowerCase().includes(q),
  )
})

const selectedConn = computed(() => connections.value.find((c) => c.id === selectedId.value) || null)

const modelsForConn = computed(() => {
  if (!selectedConn.value) return []
  const list = models.value.filter((m) => m.connectionId === selectedConn.value?.id)
  const q = modelListFilter.value.trim().toLowerCase()
  if (!q) return list
  return list.filter((m) => m.name.toLowerCase().includes(q) || m.modelName.toLowerCase().includes(q))
})

function getCapabilities(mod: ModelConfig): string {
  const caps = Array.isArray(mod.metadata?.capabilities) ? (mod.metadata.capabilities as string[]) : []
  return caps.length ? caps.join(', ') : '未配置'
}

async function loadData() {
  loading.value = true
  try {
    const [conns, mods] = await Promise.all([getConnections(), getModels()])
    connections.value = conns
    models.value = mods
    if (!selectedId.value && conns.length > 0) selectedId.value = conns[0].id
  } catch (e) {
    showToast(e instanceof Error ? e.message : '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadCatalog() {
  catalogLoading.value = true
  try {
    const res = await getProviderCatalog()
    catalog.value = res.providers
  } catch (e) {
    showToast(e instanceof Error ? e.message : '加载服务商目录失败', 'error')
  } finally {
    catalogLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadData(), loadCatalog()])
})

async function handleConnSubmit(data: ConnectionConfigCreateRequest) {
  try {
    if (editingConn.value) {
      await updateConnection(editingConn.value.id, data)
      showToast('连接已更新')
    } else {
      await createConnection(data)
      showToast('连接已创建')
    }
    showConnForm.value = false
    await loadData()
  } catch (e) {
    showToast(e instanceof Error ? e.message : '操作失败', 'error')
  }
}

async function handleModelSubmit(data: ModelConfigCreateRequest) {
  try {
    if (editingModel.value) {
      await updateModel(editingModel.value.id, data)
      showToast('模型已更新')
    } else {
      await createModel(data)
      showToast('模型已创建')
    }
    showModelForm.value = false
    await loadData()
  } catch (e) {
    showToast(e instanceof Error ? e.message : '操作失败', 'error')
  }
}

async function handleDelete() {
  try {
    if (deletingType.value === 'connection') {
      await deleteConnection(deletingId.value)
      showToast('连接已删除')
    } else {
      await deleteModel(deletingId.value)
      showToast('模型已删除')
    }
    showDeleteConfirm.value = false
    await loadData()
  } catch (e) {
    showToast(e instanceof Error ? e.message : '删除失败', 'error')
  }
}

async function handleTestConnection(conn: ConnectionConfig) {
  testingId.value = conn.id
  try {
    const result = await testConnection(conn.id)
    showToast(result.ok ? `${conn.name} 测试通过` : result.message, result.ok ? 'success' : 'error')
    if (result.ok && result.models?.length) {
      lastTestModels.value = { connectionId: conn.id, names: result.models }
      importPick.value = Object.fromEntries(result.models.map((name) => [name, true]))
    } else {
      lastTestModels.value = null
      importPick.value = {}
    }
  } catch (e) {
    showToast(e instanceof Error ? e.message : '连接测试失败', 'error')
  } finally {
    testingId.value = ''
  }
}

async function handleProbeModel(mod: ModelConfig) {
  probingId.value = mod.id
  try {
    const result = await probeModel(mod.id)
    showToast(result.message, result.ok ? 'success' : 'error')
  } catch (e) {
    showToast(e instanceof Error ? e.message : '探测失败', 'error')
  } finally {
    probingId.value = ''
  }
}

async function handleBatchImportFromTest() {
  if (!lastTestModels.value) return
  const modelNames = Object.entries(importPick.value)
    .filter(([, checked]) => checked)
    .map(([name]) => name)
  if (modelNames.length === 0) {
    showToast('请至少勾选一个模型 ID', 'error')
    return
  }
  importLoading.value = true
  try {
    await batchImportModels({
      connectionId: lastTestModels.value.connectionId,
      modelNames,
      capabilities: ['text'],
    })
    showToast('已批量导入模型', 'success')
    lastTestModels.value = null
    importPick.value = {}
    await loadData()
  } catch (e) {
    showToast(e instanceof Error ? e.message : '导入失败', 'error')
  } finally {
    importLoading.value = false
  }
}
</script>

<template>
  <section class="provider-hub model-config-page">
    <header class="page-header provider-hub__header">
      <div class="model-config-page__nav">
        <RouterLink to="/models" class="btn-ghost">返回模型配置</RouterLink>
      </div>
      <div class="provider-hub__title-row">
        <h2 class="provider-hub__title">服务商中心</h2>
        <p class="muted provider-hub__subtitle">先添加连接，再绑定模型 ID 与能力标签。</p>
      </div>
    </header>

    <p v-if="catalogLoading" class="muted">正在加载服务商目录...</p>

    <div v-if="loading" class="loading">加载连接与模型...</div>
    <div v-else-if="connections.length === 0" class="provider-hub__onboarding panel glass">
      <h3>还没有任何连接</h3>
      <div class="actions">
        <button class="btn-primary" @click="editingConn = null; showConnForm = true">添加服务商</button>
        <RouterLink to="/models" class="btn-ghost">返回快捷配置</RouterLink>
      </div>
    </div>
    <div v-else class="provider-hub__layout">
      <aside class="provider-hub__sidebar panel glass">
        <div class="provider-hub__sidebar-head">
          <input v-model="listFilter" type="search" class="ctrl provider-hub__search" placeholder="搜索连接..." />
          <button type="button" class="btn-primary provider-hub__add" @click="editingConn = null; showConnForm = true">+ 添加服务商</button>
        </div>
        <ul class="provider-hub__list">
          <li v-for="c in filteredConnections" :key="c.id">
            <button type="button" class="provider-hub__list-item" :class="{ active: selectedId === c.id }" @click="selectedId = c.id">
              <span>{{ c.name }}</span>
              <span class="muted">{{ c.provider }}</span>
            </button>
          </li>
        </ul>
      </aside>

      <div class="provider-hub__main content" v-if="selectedConn">
        <div class="provider-hub__detail-head">
          <div>
            <h3>{{ selectedConn.name }}</h3>
            <p class="muted url-cell">{{ selectedConn.baseUrl }}</p>
          </div>
          <div class="actions">
            <button type="button" class="btn-ghost" :disabled="testingId === selectedConn.id" @click="handleTestConnection(selectedConn)">
              {{ testingId === selectedConn.id ? '测试中...' : '测试连接' }}
            </button>
            <button type="button" class="btn-primary" @click="editingConn = selectedConn; showConnForm = true">编辑连接</button>
            <button
              type="button"
              class="btn-icon danger"
              @click="deletingId = selectedConn.id; deletingType = 'connection'; showDeleteConfirm = true"
            >
              删除
            </button>
          </div>
        </div>

        <div class="provider-hub__models">
          <div class="action-bar">
            <input v-model="modelListFilter" type="search" class="ctrl provider-hub__model-search" placeholder="筛选模型..." />
            <button type="button" class="btn-primary" @click="editingModel = null; showModelForm = true">+ 新建模型</button>
          </div>
          <div v-if="lastTestModels?.connectionId === selectedConn.id && lastTestModels.names.length" class="provider-hub__batch-import panel glass">
            <p class="muted">连接测试返回 {{ lastTestModels.names.length }} 个模型 ID，可勾选导入。</p>
            <ul class="provider-hub__import-pick">
              <li v-for="name in lastTestModels.names" :key="name">
                <label><input v-model="importPick[name]" type="checkbox" /> <code>{{ name }}</code></label>
              </li>
            </ul>
            <button class="btn-primary" :disabled="importLoading" @click="handleBatchImportFromTest">
              {{ importLoading ? '导入中...' : '导入选中项' }}
            </button>
          </div>

          <div v-if="modelsForConn.length === 0" class="empty"><p>暂无模型</p></div>
          <div v-else class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>名称</th>
                  <th>模型标识</th>
                  <th>能力</th>
                  <th>状态</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="mod in modelsForConn" :key="mod.id">
                  <td>{{ mod.name }}</td>
                  <td>{{ mod.modelName }}</td>
                  <td>{{ getCapabilities(mod) }}</td>
                  <td>
                    <span class="status" :class="mod.enabled ? 'enabled' : 'disabled'">{{ mod.enabled ? '启用' : '禁用' }}</span>
                  </td>
                  <td class="actions">
                    <button type="button" class="btn-icon" :disabled="probingId === mod.id" @click="handleProbeModel(mod)">
                      {{ probingId === mod.id ? '探测...' : '探测' }}
                    </button>
                    <button type="button" class="btn-icon" @click="editingModel = mod; showModelForm = true">编辑</button>
                    <button type="button" class="btn-icon danger" @click="deletingId = mod.id; deletingType = 'model'; showDeleteConfirm = true">
                      删除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <ConnectionForm :visible="showConnForm" :editing="editingConn" @close="showConnForm = false" @submit="handleConnSubmit" />

    <ModelForm
      :visible="showModelForm"
      :editing="editingModel"
      :connections="connections"
      @close="showModelForm = false"
      @submit="handleModelSubmit"
    />

    <ConfirmDialog
      :visible="showDeleteConfirm"
      :title="deletingType === 'connection' ? '删除连接' : '删除模型'"
      :message="deletingType === 'connection' ? '确定删除此连接及其关联模型配置？' : '确定删除此模型？'"
      confirm-text="删除"
      @confirm="handleDelete"
      @cancel="showDeleteConfirm = false"
    />
  </section>
</template>

<style scoped>
.provider-hub__layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: var(--space-md);
}
.provider-hub__sidebar,
.provider-hub__main,
.provider-hub__onboarding {
  padding: var(--space-lg);
}
.provider-hub__sidebar-head,
.provider-hub__detail-head,
.action-bar,
.actions {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
}
.provider-hub__list {
  list-style: none;
  margin: var(--space-md) 0 0;
  padding: 0;
  display: grid;
  gap: var(--space-xs);
}
.provider-hub__list-item {
  width: 100%;
  text-align: left;
  border: 1px solid var(--line);
  background: transparent;
  border-radius: var(--radius-md);
  padding: 10px;
  display: grid;
}
.provider-hub__list-item.active {
  background: var(--tint-primary-14);
  border-color: color-mix(in srgb, var(--primary) 45%, transparent);
}
.table-wrap {
  overflow-x: auto;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
}
.data-table th,
.data-table td {
  border-bottom: 1px solid var(--line);
  padding: 10px;
  text-align: left;
}
.btn-primary,
.btn-ghost {
  min-height: 36px;
  padding: 0 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
}
.btn-primary {
  background: var(--primary);
  border: none;
  color: #fff;
}
.btn-ghost {
  background: transparent;
  color: var(--text-main);
}
.status.enabled {
  color: var(--success);
}
.status.disabled {
  color: var(--text-muted);
}
.btn-icon {
  border: none;
  background: transparent;
}
.btn-icon.danger {
  color: var(--danger);
}
@media (max-width: 980px) {
  .provider-hub__layout {
    grid-template-columns: 1fr;
  }
}
</style>
