import { Fragment, useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  batchImportModels,
  createModel,
  deleteConnection,
  deleteModel,
  getConnections,
  getModels,
  getProviderCatalog,
  getProviderOAuthNotes,
  probeModel,
  testConnection,
  updateConnection,
  updateModel,
} from '@/api'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { PageBackLink } from '@/components/common/PageBackLink'
import { ConnectionForm } from '@/components/model/ConnectionForm'
import { ModelForm } from '@/components/model/ModelForm'
import { AddProviderWizard } from '@/components/provider/AddProviderWizard'
import { useToast } from '@/context/ToastContext'
import type {
  ConnectionConfig,
  ConnectionConfigCreateRequest,
  ModelConfig,
  ModelConfigCreateRequest,
  ProviderCatalogEntry,
} from '@/types'

function getCapabilities(mod: ModelConfig): string {
  const caps = Array.isArray(mod.metadata?.capabilities) ? (mod.metadata.capabilities as string[]) : []
  return caps.length ? caps.join(', ') : '未配置'
}

function hasCapabilityIssue(mod: ModelConfig): boolean {
  const caps = Array.isArray(mod.metadata?.capabilities) ? (mod.metadata.capabilities as string[]) : []
  return caps.length === 0
}

export function ProviderHubPage() {
  const { showToast } = useToast()
  const [searchParams, setSearchParams] = useSearchParams()

  const [connections, setConnections] = useState<ConnectionConfig[]>([])
  const [models, setModels] = useState<ModelConfig[]>([])
  const [catalog, setCatalog] = useState<ProviderCatalogEntry[]>([])
  const [catalogLoading, setCatalogLoading] = useState(true)
  const [coreLoading, setCoreLoading] = useState(true)
  const [listFilter, setListFilter] = useState('')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [testingId, setTestingId] = useState<string | null>(null)

  const [wizardOpen, setWizardOpen] = useState(false)
  const [showConnForm, setShowConnForm] = useState(false)
  const [editingConn, setEditingConn] = useState<ConnectionConfig | null>(null)
  const [showModelForm, setShowModelForm] = useState(false)
  const [editingModel, setEditingModel] = useState<ModelConfig | null>(null)
  const [openModelAfterWizard, setOpenModelAfterWizard] = useState(false)

  const [modelListFilter, setModelListFilter] = useState('')
  const [lastTestModels, setLastTestModels] = useState<{ connectionId: string; names: string[] } | null>(null)
  const [importLoading, setImportLoading] = useState(false)
  const [probingId, setProbingId] = useState<string | null>(null)
  const [oauthNotes, setOauthNotes] = useState<Record<string, string> | null>(null)
  const [importPick, setImportPick] = useState<Record<string, boolean>>({})
  const [healthModalOpen, setHealthModalOpen] = useState(false)
  const [healthRows, setHealthRows] = useState<{ name: string; modelName: string; ok: boolean; message: string }[]>([])
  const [healthRunning, setHealthRunning] = useState(false)

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [deletingId, setDeletingId] = useState('')
  const [deletingType, setDeletingType] = useState<'connection' | 'model'>('connection')

  const loadCore = useCallback(async () => {
    setCoreLoading(true)
    try {
      const [conns, mods] = await Promise.all([getConnections(), getModels()])
      setConnections(conns)
      setModels(mods)
    } catch (e) {
      showToast(e instanceof Error ? e.message : '加载连接失败', 'error')
    } finally {
      setCoreLoading(false)
    }
  }, [showToast])

  const loadCatalog = useCallback(async () => {
    setCatalogLoading(true)
    try {
      const cat = await getProviderCatalog()
      setCatalog(cat.providers)
    } catch (e) {
      showToast(e instanceof Error ? e.message : '加载服务商目录失败', 'error')
    } finally {
      setCatalogLoading(false)
    }
  }, [showToast])

  useEffect(() => {
    void loadCore()
    void loadCatalog()
  }, [loadCore, loadCatalog])

  useEffect(() => {
    void getProviderOAuthNotes()
      .then(setOauthNotes)
      .catch(() => setOauthNotes(null))
  }, [])

  useEffect(() => {
    if (!lastTestModels?.names.length) {
      setImportPick({})
      return
    }
    const next: Record<string, boolean> = {}
    for (const n of lastTestModels.names) {
      next[n] = true
    }
    setImportPick(next)
  }, [lastTestModels])

  useEffect(() => {
    const add = searchParams.get('add')
    if (add === '1') {
      setWizardOpen(true)
      const next = new URLSearchParams(searchParams)
      next.delete('add')
      setSearchParams(next, { replace: true })
    }
  }, [searchParams, setSearchParams])

  useEffect(() => {
    if (!selectedId && connections.length > 0) {
      setSelectedId(connections[0].id)
    }
    if (selectedId && connections.length && !connections.some((c) => c.id === selectedId)) {
      setSelectedId(connections[0]?.id ?? null)
    }
  }, [connections, selectedId])

  useEffect(() => {
    if (!openModelAfterWizard || !selectedId || coreLoading) return
    setShowModelForm(true)
    setEditingModel(null)
    setOpenModelAfterWizard(false)
  }, [openModelAfterWizard, selectedId, coreLoading])

  const filteredConnections = useMemo(() => {
    const q = listFilter.trim().toLowerCase()
    if (!q) return connections
    return connections.filter(
      (c) => c.name.toLowerCase().includes(q) || c.provider.toLowerCase().includes(q) || c.baseUrl.toLowerCase().includes(q),
    )
  }, [connections, listFilter])

  const selectedConn = connections.find((c) => c.id === selectedId) ?? null
  const modelsForConn = selectedConn ? models.filter((m) => m.connectionId === selectedConn.id) : []
  const filteredModelsForConn = useMemo(() => {
    const q = modelListFilter.trim().toLowerCase()
    if (!q) return modelsForConn
    return modelsForConn.filter(
      (m) =>
        m.name.toLowerCase().includes(q) ||
        m.modelName.toLowerCase().includes(q) ||
        m.provider.toLowerCase().includes(q),
    )
  }, [modelsForConn, modelListFilter])

  const groupedFilteredModels = useMemo(() => {
    const map = new Map<string, ModelConfig[]>()
    for (const m of filteredModelsForConn) {
      const raw =
        m.metadata && typeof m.metadata === 'object' ? (m.metadata as Record<string, unknown>).group : undefined
      const g = typeof raw === 'string' ? raw.trim() : ''
      const key = g || '__ungrouped__'
      if (!map.has(key)) map.set(key, [])
      map.get(key)!.push(m)
    }
    const keys = [...map.keys()].sort((a, b) => {
      if (a === '__ungrouped__') return 1
      if (b === '__ungrouped__') return -1
      return a.localeCompare(b)
    })
    return keys.map((k) => ({ group: k, models: map.get(k)! }))
  }, [filteredModelsForConn])

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
      }
      setShowConnForm(false)
      await loadCore()
    } catch (e) {
      showToast(e instanceof Error ? e.message : '操作失败', 'error')
    }
  }

  async function handleTestConnection(conn: ConnectionConfig) {
    setTestingId(conn.id)
    try {
      const result = await testConnection(conn.id)
      showToast(result.ok ? `${conn.name} 测试通过` : result.message, result.ok ? 'success' : 'error')
      if (result.ok && result.models?.length) {
        setLastTestModels({ connectionId: conn.id, names: result.models })
      } else {
        setLastTestModels(null)
      }
    } catch (e) {
      showToast(e instanceof Error ? e.message : '连接测试失败', 'error')
      setLastTestModels(null)
    } finally {
      setTestingId(null)
    }
  }

  async function handleBatchHealthCheck() {
    if (!selectedConn || filteredModelsForConn.length === 0) return
    setHealthModalOpen(true)
    setHealthRows([])
    setHealthRunning(true)
    const acc: { name: string; modelName: string; ok: boolean; message: string }[] = []
    for (const mod of filteredModelsForConn) {
      try {
        const r = await probeModel(mod.id)
        acc.push({ name: mod.name, modelName: mod.modelName, ok: r.ok, message: r.message })
      } catch (e) {
        acc.push({
          name: mod.name,
          modelName: mod.modelName,
          ok: false,
          message: e instanceof Error ? e.message : '探测失败',
        })
      }
      setHealthRows([...acc])
    }
    setHealthRunning(false)
  }

  async function handleBatchImportFromTest() {
    if (!lastTestModels || !selectedConn || lastTestModels.connectionId !== selectedConn.id) return
    const modelNames = Object.entries(importPick)
      .filter(([, v]) => v)
      .map(([k]) => k)
    if (modelNames.length === 0) {
      showToast('请至少勾选一个模型 ID', 'error')
      return
    }
    setImportLoading(true)
    try {
      await batchImportModels({
        connectionId: lastTestModels.connectionId,
        modelNames,
        capabilities: ['text'],
      })
      showToast('已批量导入模型', 'success')
      setLastTestModels(null)
      await loadCore()
    } catch (e) {
      showToast(e instanceof Error ? e.message : '导入失败', 'error')
    } finally {
      setImportLoading(false)
    }
  }

  async function handleProbeModel(mod: ModelConfig) {
    setProbingId(mod.id)
    try {
      const r = await probeModel(mod.id)
      showToast(r.message, r.ok ? 'success' : 'error')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '探测失败', 'error')
    } finally {
      setProbingId(null)
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
      await loadCore()
    } catch (e) {
      showToast(e instanceof Error ? e.message : '操作失败', 'error')
    }
  }

  async function handleDelete() {
    try {
      if (deletingType === 'connection') {
        await deleteConnection(deletingId)
        showToast('连接已删除')
        if (selectedId === deletingId) setSelectedId(null)
      } else {
        await deleteModel(deletingId)
        showToast('模型已删除')
      }
      setShowDeleteConfirm(false)
      await loadCore()
    } catch (e) {
      showToast(e instanceof Error ? e.message : '删除失败', 'error')
    }
  }

  return (
    <section className="provider-hub model-config-page">
      <header className="page-header provider-hub__header">
        <div className="model-config-page__nav">
          <PageBackLink to="/models">返回模型配置</PageBackLink>
        </div>
        <div className="provider-hub__title-row">
          <h2 className="provider-hub__title">服务商中心</h2>
          <p className="muted provider-hub__subtitle">
            适合自定义 Base URL、代理或预置未覆盖的服务。先添加连接，再绑定模型 ID 与能力标签。
          </p>
        </div>
      </header>

      {oauthNotes?.message ? (
        <div className="provider-hub__oauth-banner panel glass" role="note">
          <strong>桌面专属能力说明：</strong>
          <span>{oauthNotes.message}</span>
          {oauthNotes.ovms ? (
            <span className="provider-hub__oauth-extra">
              {' '}
              OVMS：{oauthNotes.ovms}
            </span>
          ) : null}
        </div>
      ) : null}

      {coreLoading ? (
        <div className="loading">加载连接与模型…</div>
      ) : connections.length === 0 ? (
        <div className="provider-hub__onboarding panel glass">
          <h3 className="provider-hub__onboarding-title">还没有任何连接</h3>
          <ol className="provider-hub__steps">
            <li>点击下方按钮，从目录选择服务商类型并填写 API 地址与密钥。</li>
            <li>保存后为此连接添加模型标识（与供应商控制台一致），并勾选 text / image / video 能力。</li>
            <li>在文生图/文生视频中选择对应模型即可调用。</li>
          </ol>
          <div className="provider-hub__onboarding-actions">
            <button type="button" className="btn-primary" onClick={() => setWizardOpen(true)}>
              添加服务商
            </button>
            <Link to="/models" className="btn-ghost link-as-button">
              返回快捷配置（预置模型）
            </Link>
          </div>
        </div>
      ) : (
        <div className="provider-hub__layout">
          <aside className="provider-hub__sidebar panel glass">
            <div className="provider-hub__sidebar-head">
              <input
                type="search"
                className="ctrl provider-hub__search"
                placeholder="搜索连接…"
                value={listFilter}
                onChange={(e) => setListFilter(e.target.value)}
                aria-label="搜索连接"
              />
              <button type="button" className="btn-primary provider-hub__add" onClick={() => setWizardOpen(true)}>
                + 添加服务商
              </button>
            </div>
            <ul className="provider-hub__list" role="listbox" aria-label="连接列表">
              {filteredConnections.length === 0 ? (
                <li className="provider-hub__empty muted">无匹配连接，请调整搜索或新建</li>
              ) : (
                filteredConnections.map((c) => (
                  <li key={c.id}>
                    <button
                      type="button"
                      className={`provider-hub__list-item${selectedId === c.id ? ' active' : ''}`}
                      onClick={() => setSelectedId(c.id)}
                    >
                      <span className="provider-hub__list-name">{c.name}</span>
                      <span className="provider-hub__list-meta">{c.provider}</span>
                    </button>
                  </li>
                ))
              )}
            </ul>
            <p className="provider-hub__footer-hint muted">
              预置模型见 <Link to="/models">模型配置 · 快捷模式</Link>
            </p>
          </aside>

          <div className="provider-hub__main content">
            {!selectedConn ? (
              <div className="empty">
                <p>请选择左侧连接。</p>
              </div>
            ) : (
              <>
                <div className="provider-hub__detail-head">
                  <div>
                    <h3>{selectedConn.name}</h3>
                    <p className="muted url-cell">{selectedConn.baseUrl}</p>
                  </div>
                  <div className="provider-hub__detail-actions">
                    <button
                      type="button"
                      className="btn-ghost"
                      disabled={testingId === selectedConn.id}
                      onClick={() => void handleTestConnection(selectedConn)}
                    >
                      {testingId === selectedConn.id ? '测试中…' : '测试连接'}
                    </button>
                    <button
                      type="button"
                      className="btn-primary"
                      onClick={() => {
                        setEditingConn(selectedConn)
                        setShowConnForm(true)
                      }}
                    >
                      编辑连接
                    </button>
                    <button
                      type="button"
                      className="btn-icon danger"
                      onClick={() => {
                        setDeletingId(selectedConn.id)
                        setDeletingType('connection')
                        setShowDeleteConfirm(true)
                      }}
                    >
                      删除
                    </button>
                  </div>
                </div>
                <h4 className="form-section-title provider-hub__section-title">连接信息</h4>
                <dl className="provider-hub__meta">
                  <div>
                    <dt>提供商</dt>
                    <dd>{selectedConn.provider}</dd>
                  </div>
                  <div>
                    <dt>API Key</dt>
                    <dd>{selectedConn.apiKeyMasked || '—'}</dd>
                  </div>
                  <div>
                    <dt>状态</dt>
                    <dd>
                      <span className={`status ${selectedConn.enabled ? 'enabled' : 'disabled'}`}>
                        {selectedConn.enabled ? '启用' : '禁用'}
                      </span>
                    </dd>
                  </div>
                </dl>

                {modelsForConn.length === 0 ? (
                  <p className="provider-hub__next-hint muted">
                    下一步：点击「新建模型」，填写供应商控制台中的 <strong>模型 ID</strong>，并勾选能力标签。
                  </p>
                ) : null}

                <div className="provider-hub__models" id="provider-hub-models">
                  <h4 className="form-section-title provider-hub__section-title">模型列表</h4>
                  <div className="action-bar">
                    <span className="provider-hub__models-heading">此连接下的模型</span>
                    <input
                      type="search"
                      className="ctrl provider-hub__model-search"
                      placeholder="筛选模型…"
                      value={modelListFilter}
                      onChange={(e) => setModelListFilter(e.target.value)}
                      aria-label="筛选模型"
                    />
                    <button
                      type="button"
                      className="btn-ghost"
                      disabled={filteredModelsForConn.length === 0 || healthRunning}
                      onClick={() => void handleBatchHealthCheck()}
                    >
                      {healthRunning ? '健康检查中…' : '批量健康检查'}
                    </button>
                    <button
                      type="button"
                      className="btn-primary"
                      onClick={() => {
                        setEditingModel(null)
                        setShowModelForm(true)
                      }}
                    >
                      + 新建模型
                    </button>
                  </div>
                  {lastTestModels && selectedConn && lastTestModels.connectionId === selectedConn.id && lastTestModels.names.length > 0 ? (
                    <div className="provider-hub__batch-import panel glass">
                      <p className="muted">
                        连接测试返回 {lastTestModels.names.length} 个模型 ID，勾选后导入为本地模型配置（默认 text 能力）。
                      </p>
                      <ul className="provider-hub__import-pick">
                        {lastTestModels.names.map((name) => (
                          <li key={name}>
                            <label className="provider-hub__import-label">
                              <input
                                type="checkbox"
                                checked={importPick[name] !== false}
                                onChange={() =>
                                  setImportPick((p) => ({
                                    ...p,
                                    [name]: p[name] === false ? true : false,
                                  }))
                                }
                              />
                              <code>{name}</code>
                            </label>
                          </li>
                        ))}
                      </ul>
                      <button
                        type="button"
                        className="btn-primary"
                        disabled={importLoading}
                        onClick={() => void handleBatchImportFromTest()}
                      >
                        {importLoading ? '导入中…' : '导入选中项'}
                      </button>
                    </div>
                  ) : null}
                  {modelsForConn.length === 0 ? (
                    <div className="empty">
                      <p>暂无模型</p>
                      <button
                        type="button"
                        className="btn-ghost"
                        onClick={() => {
                          setEditingModel(null)
                          setShowModelForm(true)
                        }}
                      >
                        添加第一个模型
                      </button>
                    </div>
                  ) : (
                    <div className="table-wrap">
                      <table className="data-table">
                        <thead>
                          <tr>
                            <th>名称</th>
                            <th>模型标识</th>
                            <th>分组</th>
                            <th>能力</th>
                            <th>状态</th>
                            <th />
                          </tr>
                        </thead>
                        <tbody>
                          {groupedFilteredModels.map(({ group, models }) => (
                            <Fragment key={group}>
                              <tr className="provider-hub__group-row">
                                <td colSpan={6}>
                                  <strong>{group === '__ungrouped__' ? '未分组' : group}</strong>
                                </td>
                              </tr>
                              {models.map((mod) => (
                                <tr key={mod.id} className={hasCapabilityIssue(mod) ? 'warning' : undefined}>
                                  <td>{mod.name}</td>
                                  <td>{mod.modelName}</td>
                                  <td className="muted">
                                    {String((mod.metadata as Record<string, unknown> | undefined)?.group ?? '') || '—'}
                                  </td>
                                  <td>
                                    {getCapabilities(mod)}
                                    {hasCapabilityIssue(mod) ? <span className="warn-text">（请编辑补充能力）</span> : null}
                                  </td>
                                  <td>
                                    <span className={`status ${mod.enabled ? 'enabled' : 'disabled'}`}>
                                      {mod.enabled ? '启用' : '禁用'}
                                    </span>
                                  </td>
                                  <td className="actions">
                                    <button
                                      type="button"
                                      className="btn-icon"
                                      disabled={probingId === mod.id}
                                      onClick={() => void handleProbeModel(mod)}
                                    >
                                      {probingId === mod.id ? '探测…' : '探测'}
                                    </button>
                                    <button
                                      type="button"
                                      className="btn-icon"
                                      onClick={() => {
                                        setEditingModel(mod)
                                        setShowModelForm(true)
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
                            </Fragment>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      )}

      <AddProviderWizard
        visible={wizardOpen}
        catalog={catalog}
        catalogLoading={catalogLoading}
        onClose={() => setWizardOpen(false)}
        onCreated={async (created) => {
          await loadCore()
          if (created?.id) {
            setSelectedId(created.id)
            setOpenModelAfterWizard(true)
            showToast('连接已创建，请填写模型 ID 与能力标签', 'success')
          }
        }}
      />

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
        presetConnectionId={!editingModel ? selectedId ?? undefined : undefined}
        onClose={() => setShowModelForm(false)}
        onSubmit={handleModelSubmit}
      />

      {healthModalOpen ? (
        <div
          className="dialog-overlay"
          role="dialog"
          aria-modal
          aria-labelledby="health-modal-title"
          onClick={(ev) => ev.target === ev.currentTarget && setHealthModalOpen(false)}
        >
          <div className="dialog glass modal-form-dialog provider-hub__health-modal">
            <h3 id="health-modal-title">健康检查结果</h3>
            {healthRunning ? <p className="muted">正在逐个探测模型…</p> : null}
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>名称</th>
                    <th>模型 ID</th>
                    <th>结果</th>
                  </tr>
                </thead>
                <tbody>
                  {healthRows.map((row, i) => (
                    <tr key={`${row.modelName}-${i}`}>
                      <td>{row.name}</td>
                      <td>
                        <code>{row.modelName}</code>
                      </td>
                      <td>
                        <span className={row.ok ? 'status enabled' : 'status disabled'}>{row.ok ? '通过' : row.message}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-submit" onClick={() => setHealthModalOpen(false)}>
                关闭
              </button>
            </div>
          </div>
        </div>
      ) : null}

      <ConfirmDialog
        visible={showDeleteConfirm}
        title={deletingType === 'connection' ? '删除连接' : '删除模型'}
        message={deletingType === 'connection' ? '确定删除此连接及其关联模型配置？' : '确定删除此模型？'}
        confirmText="删除"
        onConfirm={() => void handleDelete()}
        onCancel={() => setShowDeleteConfirm(false)}
      />
    </section>
  )
}
