import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import {
  createConnection,
  createModel,
  deleteConnection,
  deleteModel,
  getConnections,
  getModels,
  getProviderCatalog,
  testConnection,
  updateConnection,
  updateModel,
} from '@/api'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { PageBackLink } from '@/components/common/PageBackLink'
import { ConnectionForm } from '@/components/model/ConnectionForm'
import { ModelForm } from '@/components/model/ModelForm'
import { QuickModelForm } from '@/components/model/QuickModelForm'
import { useToast } from '@/context/ToastContext'
import type {
  ConnectionConfig,
  ConnectionConfigCreateRequest,
  ModelConfig,
  ModelConfigCreateRequest,
  ProviderCatalogEntry,
} from '@/types'

export function ModelConfigPage() {
  const { showToast } = useToast()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [configMode, setConfigMode] = useState<'quick' | 'advanced'>('quick')
  const [activeTab, setActiveTab] = useState<'connections' | 'models'>('connections')
  const [connections, setConnections] = useState<ConnectionConfig[]>([])
  const [models, setModels] = useState<ModelConfig[]>([])
  const [catalog, setCatalog] = useState<ProviderCatalogEntry[]>([])
  const [loading, setLoading] = useState(false)

  const [showConnForm, setShowConnForm] = useState(false)
  const [editingConn, setEditingConn] = useState<ConnectionConfig | null>(null)

  const [showModelForm, setShowModelForm] = useState(false)
  const [editingModel, setEditingModel] = useState<ModelConfig | null>(null)

  const [showQuickForm, setShowQuickForm] = useState(false)

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [deletingId, setDeletingId] = useState('')
  const [deletingType, setDeletingType] = useState<'connection' | 'model'>('connection')

  async function loadData() {
    setLoading(true)
    try {
      const [conns, mods] = await Promise.all([getConnections(), getModels()])
      setConnections(conns)
      setModels(mods)
    } catch (e) {
      showToast(e instanceof Error ? e.message : '加载数据失败', 'error')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  useEffect(() => {
    void getProviderCatalog()
      .then((r) => setCatalog(r.providers))
      .catch(() => setCatalog([]))
  }, [])

  useEffect(() => {
    const mode = searchParams.get('mode')
    const tab = searchParams.get('tab')
    if (!mode && !tab) return
    if (mode === 'advanced' || tab === 'models' || tab === 'connections') {
      setConfigMode('advanced')
    }
    if (tab === 'models' || tab === 'connections') {
      setActiveTab(tab)
    }
    const next = new URLSearchParams(searchParams)
    if (next.has('mode')) next.delete('mode')
    if (next.has('tab')) next.delete('tab')
    setSearchParams(next, { replace: true })
  }, [searchParams, setSearchParams])

  async function handleConnSubmit(data: ConnectionConfigCreateRequest) {
    try {
      if (editingConn) {
        await updateConnection(editingConn.id, {
          name: data.name,
          provider: data.provider,
          baseUrl: data.baseUrl,
          apiKey: data.apiKey,
          enabled: data.enabled,
          metadata: data.metadata,
        })
        showToast('连接已更新')
      } else {
        await createConnection(data)
        showToast('连接已创建')
      }
      setShowConnForm(false)
      await loadData()
    } catch (e) {
      showToast(e instanceof Error ? e.message : '操作失败', 'error')
    }
  }

  async function handleDelete() {
    try {
      if (deletingType === 'connection') {
        await deleteConnection(deletingId)
        showToast('连接已删除')
      } else {
        await deleteModel(deletingId)
        showToast('模型已删除')
      }
      setShowDeleteConfirm(false)
      await loadData()
    } catch (e) {
      showToast(e instanceof Error ? e.message : '删除失败', 'error')
    }
  }

  async function handleModelSubmit(data: ModelConfigCreateRequest) {
    try {
      if (editingModel) {
        await updateModel(editingModel.id, data)
        showToast('模型已更新')
      } else {
        await createModel(data)
        showToast('模型已创建')
      }
      setShowModelForm(false)
      await loadData()
    } catch (e) {
      showToast(e instanceof Error ? e.message : '操作失败', 'error')
    }
  }

  async function handleQuickSuccess() {
    await loadData()
    setActiveTab('models')
    setConfigMode('advanced')
  }

  function getConnName(connId: string): string {
    return connections.find((c) => c.id === connId)?.name ?? connId
  }

  function getCapabilities(mod: ModelConfig): string {
    const caps = Array.isArray(mod.metadata?.capabilities) ? (mod.metadata.capabilities as string[]) : []
    return caps.length ? caps.join(', ') : '未配置'
  }

  function hasCapabilityIssue(mod: ModelConfig): boolean {
    const caps = Array.isArray(mod.metadata?.capabilities) ? (mod.metadata.capabilities as string[]) : []
    return caps.length === 0
  }

  async function handleTestConnection(conn: ConnectionConfig) {
    try {
      const result = await testConnection(conn.id)
      showToast(result.ok ? `${conn.name} 测试通过` : result.message, result.ok ? 'success' : 'error')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '连接测试失败', 'error')
    }
  }

  function renderModelsTable(list: ModelConfig[], onEdit: (m: ModelConfig) => void) {
    return (
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>提供商</th>
              <th>模型标识</th>
              <th>能力</th>
              <th>关联连接</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {list.map((mod) => (
              <tr key={mod.id} className={hasCapabilityIssue(mod) ? 'warning' : undefined}>
                <td>{mod.name}</td>
                <td>{mod.provider}</td>
                <td>{mod.modelName}</td>
                <td>
                  {getCapabilities(mod)}
                  {hasCapabilityIssue(mod) ? <span className="warn-text">（请编辑后补充能力标签）</span> : null}
                </td>
                <td>{getConnName(mod.connectionId)}</td>
                <td>
                  <span className={`status ${mod.enabled ? 'enabled' : 'disabled'}`}>{mod.enabled ? '启用' : '禁用'}</span>
                </td>
                <td className="actions">
                  <button
                    type="button"
                    className="btn-icon"
                    onClick={() => {
                      setConfigMode('advanced')
                      setActiveTab('models')
                      onEdit(mod)
                    }}
                  >
                    编辑
                  </button>
                  <button
                    type="button"
                    className="btn-icon danger"
                    onClick={() => {
                      setDeletingId(mod.id)
                      setDeletingType('model')
                      setShowDeleteConfirm(true)
                    }}
                  >
                    删除
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )
  }

  const orderedForQuickEdit = models

  return (
    <section className="model-config-page">
      <header className="page-header">
        <div className="model-config-page__nav">
          <PageBackLink />
        </div>
        <div className="mode-toggle">
          <button type="button" className={`mode-btn${configMode === 'quick' ? ' active' : ''}`} onClick={() => setConfigMode('quick')}>
            快捷模式
          </button>
          <button type="button" className={`mode-btn${configMode === 'advanced' ? ' active' : ''}`} onClick={() => setConfigMode('advanced')}>
            高级模式
          </button>
        </div>
        {configMode === 'quick' ? (
          <div className="quick-action-bar">
            <button type="button" className="btn-primary" onClick={() => setShowQuickForm(true)}>
              + 添加模型
            </button>
            <Link to="/models/hub" className="model-config-hub-link">
              服务商中心
            </Link>
          </div>
        ) : (
          <div className="tab-nav model-config-page__advanced-bar">
            <div className="tab-nav__tabs">
              <button type="button" className={`tab${activeTab === 'connections' ? ' active' : ''}`} onClick={() => setActiveTab('connections')}>
                连接配置
              </button>
              <button type="button" className={`tab${activeTab === 'models' ? ' active' : ''}`} onClick={() => setActiveTab('models')}>
                模型配置
              </button>
            </div>
            <Link to="/models/hub" className="model-config-hub-link model-config-hub-link--tab">
              服务商中心
            </Link>
          </div>
        )}
      </header>

      {loading ? (
        <div className="loading">加载中...</div>
      ) : configMode === 'quick' ? (
        models.length === 0 && connections.length === 0 ? (
          <div className="empty">
            <p>还没有配置任何模型</p>
            <p className="empty-hint">快捷模式使用预置模型；若你的 API 不在列表中，请用「服务商中心」自定义 Base URL。</p>
            <div className="empty-actions">
              <button type="button" className="btn-primary" onClick={() => setShowQuickForm(true)}>
                快捷添加第一个模型
              </button>
              <Link to="/models/hub?add=1" className="btn-ghost link-as-button">
                服务商中心（自定义 API）
              </Link>
            </div>
          </div>
        ) : (
          renderModelsTable(orderedForQuickEdit, (mod) => {
            setEditingModel(mod)
            setShowModelForm(true)
          })
        )
      ) : activeTab === 'connections' ? (
        <div className="content">
          <div className="action-bar">
            <button
              type="button"
              className="btn-primary"
              onClick={() => {
                setEditingConn(null)
                setShowConnForm(true)
              }}
            >
              新建连接
            </button>
          </div>
          {connections.length === 0 ? (
            <div className="empty">
              <p>暂无连接配置</p>
              <p className="empty-hint">可手动填写，或使用服务商向导预填地址。</p>
              <div className="empty-actions">
                <button
                  type="button"
                  className="btn-primary"
                  onClick={() => {
                    setEditingConn(null)
                    setShowConnForm(true)
                  }}
                >
                  手动新建连接
                </button>
                <Link to="/models/hub?add=1" className="btn-ghost link-as-button">
                  打开服务商中心
                </Link>
              </div>
            </div>
          ) : (
            <div className="table-wrap">
              <table className="data-table">
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
                  {connections.map((conn) => (
                    <tr key={conn.id}>
                      <td>{conn.name}</td>
                      <td>{conn.provider}</td>
                      <td className="url-cell">{conn.baseUrl}</td>
                      <td>{conn.apiKeyMasked || '-'}</td>
                      <td>
                        <span className={`status ${conn.enabled ? 'enabled' : 'disabled'}`}>{conn.enabled ? '启用' : '禁用'}</span>
                      </td>
                      <td className="actions">
                        <button type="button" className="btn-icon" onClick={() => void handleTestConnection(conn)}>
                          测试
                        </button>
                        <button
                          type="button"
                          className="btn-icon"
                          onClick={() => {
                            setEditingConn(conn)
                            setShowConnForm(true)
                          }}
                        >
                          编辑
                        </button>
                        <button
                          type="button"
                          className="btn-icon danger"
                          onClick={() => {
                            setDeletingId(conn.id)
                            setDeletingType('connection')
                            setShowDeleteConfirm(true)
                          }}
                        >
                          删除
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : (
        <div className="content">
          <div className="action-bar">
            <button
              type="button"
              className="btn-primary"
              disabled={connections.length === 0}
              onClick={() => {
                setEditingModel(null)
                setShowModelForm(true)
              }}
            >
              新建模型
            </button>
          </div>
          {connections.length === 0 ? (
            <div className="empty">
              <p>请先创建连接配置</p>
              <p className="empty-hint">
                在「连接配置」中新建，或前往 <Link to="/models/hub">服务商中心</Link>。
              </p>
            </div>
          ) : models.length === 0 ? (
            <div className="empty">
              <p>暂无模型配置</p>
              <button
                type="button"
                className="btn-ghost"
                onClick={() => {
                  setEditingModel(null)
                  setShowModelForm(true)
                }}
              >
                创建第一个模型
              </button>
            </div>
          ) : (
            renderModelsTable(models, (mod) => {
              setEditingModel(mod)
              setShowModelForm(true)
            })
          )}
        </div>
      )}

      <ConnectionForm
        visible={showConnForm}
        editing={editingConn ?? undefined}
        catalog={catalog}
        onClose={() => setShowConnForm(false)}
        onSubmit={handleConnSubmit}
      />

      <ModelForm
        visible={showModelForm}
        editing={editingModel ?? undefined}
        connections={connections}
        onClose={() => setShowModelForm(false)}
        onSubmit={handleModelSubmit}
      />

      <QuickModelForm
        visible={showQuickForm}
        onClose={() => setShowQuickForm(false)}
        onSuccess={() => void handleQuickSuccess()}
        onOtherProvider={() => {
          setShowQuickForm(false)
          navigate('/models/hub?add=1')
        }}
        onAdvancedMode={() => {
          setShowQuickForm(false)
          navigate('/models?mode=advanced&tab=connections')
        }}
      />

      <ConfirmDialog
        visible={showDeleteConfirm}
        title={deletingType === 'connection' ? '删除连接' : '删除模型'}
        message={deletingType === 'connection' ? '确定要删除此连接吗？' : '确定要删除此模型吗？'}
        confirmText="删除"
        onConfirm={() => void handleDelete()}
        onCancel={() => setShowDeleteConfirm(false)}
      />
    </section>
  )
}
