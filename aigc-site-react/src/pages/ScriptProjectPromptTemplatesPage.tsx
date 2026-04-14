import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  getPromptTemplateCatalog,
  getPromptTemplateOverrides,
  updatePromptTemplateOverrides,
} from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { ScriptProjectWorkflowNav } from '@/components/script/ScriptProjectWorkflowNav'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { PromptTemplateCatalogItem } from '@/types'

const CAT_LABEL: Record<string, string> = {
  visual: '视觉 / 分镜',
  keyframe: '关键帧',
  script: '剧本',
  storyboard: '镜头视频',
}

export function ScriptProjectPromptTemplatesPage() {
  const { projectId = '' } = useParams()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const loadProject = useScriptProjectStore((s) => s.loadProject)

  const [catalog, setCatalog] = useState<PromptTemplateCatalogItem[]>([])
  const [overrides, setOverrides] = useState<Record<string, string>>({})
  const [drafts, setDrafts] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [savingPath, setSavingPath] = useState<string | null>(null)

  useEffect(() => {
    if (!projectId) return
    void loadProject(projectId)
  }, [projectId, loadProject])

  useEffect(() => {
    if (!projectId) return
    let cancelled = false
    void (async () => {
      setLoading(true)
      try {
        const [cat, ovr] = await Promise.all([getPromptTemplateCatalog(), getPromptTemplateOverrides(projectId)])
        if (cancelled) return
        setCatalog(cat)
        setOverrides(ovr || {})
        const nextDrafts: Record<string, string> = {}
        for (const item of cat) {
          nextDrafts[item.path] = ovr[item.path] != null ? ovr[item.path]! : item.defaultBody
        }
        setDrafts(nextDrafts)
      } catch (e) {
        if (!cancelled) showToast(e instanceof Error ? e.message : '加载失败', 'error')
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [projectId, showToast])

  const grouped = useMemo(() => {
    const m = new Map<string, PromptTemplateCatalogItem[]>()
    for (const item of catalog) {
      const k = item.category || 'other'
      if (!m.has(k)) m.set(k, [])
      m.get(k)!.push(item)
    }
    return m
  }, [catalog])

  async function saveOne(path: string) {
    if (!projectId) return
    const body = drafts[path] ?? ''
    const def = catalog.find((c) => c.path === path)?.defaultBody ?? ''
    setSavingPath(path)
    try {
      const next = { ...overrides, [path]: body }
      if (body.trim() === def.trim()) {
        delete next[path]
      }
      const saved = await updatePromptTemplateOverrides(projectId, next)
      setOverrides(saved)
      showToast('已保存（与默认相同的项会自动清除覆盖）', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
    } finally {
      setSavingPath(null)
    }
  }

  async function restoreDefault(path: string) {
    const def = catalog.find((c) => c.path === path)?.defaultBody ?? ''
    if (!projectId) return
    setSavingPath(path)
    try {
      const saved = await updatePromptTemplateOverrides(projectId, { [path]: '' })
      setOverrides(saved)
      setDrafts((d) => ({ ...d, [path]: def }))
      showToast('已恢复默认', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '恢复失败', 'error')
    } finally {
      setSavingPath(null)
    }
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请从剧本工程列表进入。" />
  }

  return (
    <div className="script-project-workflow-layout">
      <ScriptProjectWorkflowNav projectId={projectId} />
      <div className="script-project-workflow-layout__main">
        <section className="script-prompt-templates-page">
          <div className="toolbar panel glass">
            <div>
              <h2>提示词模板（项目覆盖）</h2>
              <p className="muted">修改后保存即可影响本项目内的 AI 调用；与资源文件默认一致时会自动清除覆盖项。</p>
            </div>
          </div>

          {loading ? (
            <LoadingSpinner />
          ) : (
            <div className="prompt-template-groups">
              {Array.from(grouped.entries()).map(([cat, items]) => (
                <div key={cat} className="panel glass prompt-template-group">
                  <h3 className="eyebrow">{CAT_LABEL[cat] ?? cat}</h3>
                  {items.map((item) => (
                    <div key={item.path} className="prompt-template-item">
                      <div className="prompt-template-item__head">
                        <div>
                          <h4>{item.title}</h4>
                          <p className="muted small">{item.description}</p>
                          <p className="muted mono small">{item.path}</p>
                        </div>
                        <div className="actions">
                          <AppButton
                            size="sm"
                            variant="ghost"
                            loading={savingPath === item.path}
                            onClick={() => void restoreDefault(item.path)}
                          >
                            恢复默认
                          </AppButton>
                          <AppButton
                            size="sm"
                            variant="primary"
                            loading={savingPath === item.path}
                            onClick={() => void saveOne(item.path)}
                          >
                            保存
                          </AppButton>
                        </div>
                      </div>
                      <textarea
                        className="ctrl prompt-template-item__body"
                        rows={14}
                        value={drafts[item.path] ?? ''}
                        onChange={(e) => setDrafts((d) => ({ ...d, [item.path]: e.target.value }))}
                      />
                    </div>
                  ))}
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
