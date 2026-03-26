<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useToast } from '@/composables/useToast'
import {
  createConnection,
  createModel,
  deleteConnection,
  deleteModel,
  getConnections,
  getModels,
  updateConnection,
  updateModel,
} from '@/services/api'
import type { ConnectionConfig, ConnectionConfigCreateRequest, ModelConfig, ModelConfigCreateRequest } from '@/types'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import ConnectionForm from '@/components/model/ConnectionForm.vue'
import ModelForm from '@/components/model/ModelForm.vue'

const { showToast } = useToast()

const activeTab = ref<'connections' | 'models'>('connections')
const connections = ref<ConnectionConfig[]>([])
const models = ref<ModelConfig[]>([])
const loading = ref(false)

const showConnForm = ref(false)
const editingConn = ref<ConnectionConfig | null>(null)

const showModelForm = ref(false)
const editingModel = ref<ModelConfig | null>(null)

const showDeleteConfirm = ref(false)
const deletingId = ref<string>('')
const deletingType = ref<'connection' | 'model'>('connection')

async function loadData() {
  loading.value = true
  try {
    const [conns, mods] = await Promise.all([getConnections(), getModels()])
    connections.value = conns
    models.value = mods
  } catch (e) {
    showToast((e as Error)?.message || '加载数据失败', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

function openNewConnForm() {
  editingConn.value = null
  showConnForm.value = true
}

function openEditConnForm(conn: ConnectionConfig) {
  editingConn.value = conn
  showConnForm.value = true
}

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
    showToast((e as Error)?.message || '操作失败', 'error')
  }
}

function confirmDeleteConn(id: string) {
  deletingId.value = id
  deletingType.value = 'connection'
  showDeleteConfirm.value = true
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
    showToast((e as Error)?.message || '删除失败', 'error')
  }
}

function openNewModelForm() {
  editingModel.value = null
  showModelForm.value = true
}

function openEditModelForm(mod: ModelConfig) {
  editingModel.value = mod
  showModelForm.value = true
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
    showToast((e as Error)?.message || '操作失败', 'error')
  }
}

function confirmDeleteModel(id: string) {
  deletingId.value = id
  deletingType.value = 'model'
  showDeleteConfirm.value = true
}

function getConnName(connId: string): string {
  const conn = connections.value.find((c) => c.id === connId)
  return conn ? conn.name : connId
}
</script>

<template>
  <section class="model-config-page">
    <header class="page-header">
      <h2>模型配置</h2>
      <div class="tab-nav">
        <button class="tab" :class="{ active: activeTab === 'connections' }" @click="activeTab = 'connections'">
          连接配置
        </button>
        <button class="tab" :class="{ active: activeTab === 'models' }" @click="activeTab = 'models'">
          模型配置
        </button>
      </div>
    </header>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="activeTab === 'connections'" class="content">
      <div class="action-bar">
        <button class="btn-primary" @click="openNewConnForm">新建连接</button>
      </div>

      <div v-if="connections.length === 0" class="empty">
        <p>暂无连接配置</p>
        <button class="btn-ghost" @click="openNewConnForm">创建第一个连接</button>
      </div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>提供商</th>
              <th>Base URL</th>
              <th>API Key</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="conn in connections" :key="conn.id">
              <td>{{ conn.name }}</td>
              <td>{{ conn.provider }}</td>
              <td class="url-cell">{{ conn.baseUrl }}</td>
              <td>{{ conn.apiKeyMasked || '-' }}</td>
              <td>
                <span class="status" :class="conn.enabled ? 'enabled' : 'disabled'">
                  {{ conn.enabled ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="actions">
                <button class="btn-icon" @click="openEditConnForm(conn)">编辑</button>
                <button class="btn-icon danger" @click="confirmDeleteConn(conn.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else class="content">
      <div class="action-bar">
        <button class="btn-primary" :disabled="connections.length === 0" @click="openNewModelForm">
          新建模型
        </button>
      </div>

      <div v-if="connections.length === 0" class="empty">
        <p>请先创建连接配置</p>
      </div>

      <div v-else-if="models.length === 0" class="empty">
        <p>暂无模型配置</p>
        <button class="btn-ghost" @click="openNewModelForm">创建第一个模型</button>
      </div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>提供商</th>
              <th>模型标识</th>
              <th>关联连接</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="mod in models" :key="mod.id">
              <td>{{ mod.name }}</td>
              <td>{{ mod.provider }}</td>
              <td>{{ mod.modelName }}</td>
              <td>{{ getConnName(mod.connectionId) }}</td>
              <td>
                <span class="status" :class="mod.enabled ? 'enabled' : 'disabled'">
                  {{ mod.enabled ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="actions">
                <button class="btn-icon" @click="openEditModelForm(mod)">编辑</button>
                <button class="btn-icon danger" @click="confirmDeleteModel(mod.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <ConnectionForm
      :visible="showConnForm"
      :editing="editingConn"
      @close="showConnForm = false"
      @submit="handleConnSubmit"
    />

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
      :message="deletingType === 'connection' ? '确定要删除此连接吗？' : '确定要删除此模型吗？'"
      confirm-text="删除"
      @confirm="handleDelete"
      @cancel="showDeleteConfirm = false"
    />
  </section>
</template>

<style scoped>
.model-config-page {
  padding: var(--space-xl);
  display: grid;
  gap: var(--space-lg);
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

h2 {
  margin: 0;
}

.tab-nav {
  display: flex;
  gap: var(--space-sm);
}

.tab {
  min-height: 40px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-muted);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--duration-fast);
}

.tab:hover {
  color: var(--text-main);
  border-color: color-mix(in srgb, var(--primary) 35%, var(--line));
}

.tab.active {
  color: var(--text-main);
  background: var(--tint-primary-14);
  border-color: color-mix(in srgb, var(--primary) 45%, transparent);
}

.loading,
.empty {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--text-muted);
}

.action-bar {
  display: flex;
  justify-content: flex-end;
}

.btn-primary {
  min-height: 40px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  border: none;
  background: var(--primary);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: opacity var(--duration-fast);
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-ghost {
  min-height: 40px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-main);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--duration-fast);
}

.btn-ghost:hover {
  border-color: var(--primary);
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
  text-align: left;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
}

.data-table th {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.data-table td {
  font-size: 14px;
}

.url-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.status.enabled {
  background: color-mix(in srgb, var(--success) 15%, transparent);
  color: var(--success);
}

.status.disabled {
  background: color-mix(in srgb, var(--text-muted) 15%, transparent);
  color: var(--text-muted);
}

.actions {
  display: flex;
  gap: var(--space-sm);
}

.btn-icon {
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast);
}

.btn-icon:hover {
  color: var(--text-main);
  background: var(--tint-primary-08);
}

.btn-icon.danger:hover {
  color: var(--danger);
  background: color-mix(in srgb, var(--danger) 10%, transparent);
}
</style>
