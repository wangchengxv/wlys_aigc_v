import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { ScriptRevisionPanel } from '@/components/script/ScriptRevisionPanel'
import { ScriptStructuredPreview } from '@/components/script/ScriptStructuredPreview'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'

export function ScriptProjectPreviewPage() {
  const { projectId = '' } = useParams()
  const [searchParams] = useSearchParams()
  const { showToast } = useToast()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const scriptPayload = useScriptProjectStore((s) => s.scriptPayload)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const refineLoading = useScriptProjectStore((s) => s.refineLoading)
  const refinePromptLoading = useScriptProjectStore((s) => s.refinePromptLoading)
  const saveScriptLoading = useScriptProjectStore((s) => s.saveScriptLoading)
  const revisions = useScriptProjectStore((s) => s.revisions)
  const revisionLoading = useScriptProjectStore((s) => s.revisionLoading)
  const optimizeLoading = useScriptProjectStore((s) => s.optimizeLoading)
  const importLoading = useScriptProjectStore((s) => s.importLoading)
  const restoringRevisionId = useScriptProjectStore((s) => s.restoringRevisionId)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadScript = useScriptProjectStore((s) => s.loadScript)
  const loadRevisions = useScriptProjectStore((s) => s.loadRevisions)
  const refine = useScriptProjectStore((s) => s.refine)
  const refineWithPrompt = useScriptProjectStore((s) => s.refineWithPrompt)
  const saveScript = useScriptProjectStore((s) => s.saveScript)
  const restoreRevision = useScriptProjectStore((s) => s.restoreRevision)
  const optimizeScenes = useScriptProjectStore((s) => s.optimizeScenes)
  const optimizeCharacters = useScriptProjectStore((s) => s.optimizeCharacters)
  const optimizeProps = useScriptProjectStore((s) => s.optimizeProps)
  const importScript = useScriptProjectStore((s) => s.importScript)

  const initialTab = (() => {
    const t = searchParams.get('tab')
    if (t === 'structured') return 'structured'
    if (t === 'refined') return 'refined'
    if (t === 'json') return 'json'
    return 'original'
  })()

  const [activeTab, setActiveTab] = useState<'original' | 'refined' | 'structured' | 'json'>(initialTab)
  const [structuredSub, setStructuredSub] = useState<'overview' | 'scenes' | 'characters' | 'props'>('overview')
  const [refinedText, setRefinedText] = useState('')
  const [briefPrompt, setBriefPrompt] = useState('')

  useEffect(() => {
    setRefinedText(scriptPayload?.refinedMarkdown || '')
  }, [scriptPayload?.refinedMarkdown])

  useEffect(() => {
    if (!projectId) return
    void Promise.all([loadProject(projectId), loadScript(projectId), loadRevisions(projectId)])
    setActiveTab(initialTab)
  }, [projectId, loadProject, loadScript, loadRevisions, initialTab])

  const structuredText = useMemo(() => JSON.stringify(scriptPayload?.structuredScript || {}, null, 2), [scriptPayload?.structuredScript])

  async function handleRefine() {
    try {
      await refine(projectId)
      showToast('剧本完善完成', 'success')
      setActiveTab('refined')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '完善剧本失败', 'error')
    }
  }

  async function handleRefineWithPrompt() {
    const prompt = briefPrompt.trim()
    if (!prompt) {
      showToast('请输入短提示词', 'error')
      return
    }
    try {
      await refineWithPrompt(projectId, prompt)
      showToast('剧本完善功能完成', 'success')
      setActiveTab('structured')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '完善剧本失败', 'error')
    }
  }

  async function handleSave() {
    try {
      await saveScript(projectId, {
        refinedMarkdown: refinedText,
        structuredScript: scriptPayload?.structuredScript || {},
      })
      showToast('剧本保存成功', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  function onPickImport() {
    fileInputRef.current?.click()
  }

  async function onImportFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file || !projectId) return
    try {
      await importScript(projectId, file, { autoRefine: false })
      showToast('剧本已导入', 'success')
      setActiveTab('original')
    } catch (err) {
      showToast(err instanceof Error ? err.message : '导入失败', 'error')
    }
  }

  async function handleRestore(revisionId: string) {
    try {
      await restoreRevision(projectId, revisionId)
      showToast('已恢复到所选版本', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '恢复失败', 'error')
    }
  }

  async function runOptimize(kind: 'scenes' | 'characters' | 'props') {
    try {
      if (kind === 'scenes') await optimizeScenes(projectId)
      if (kind === 'characters') await optimizeCharacters(projectId)
      if (kind === 'props') await optimizeProps(projectId)
      showToast('智能体优化完成', 'success')
      setActiveTab('structured')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '优化失败', 'error')
    }
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请返回列表重新选择项目。" />
  }

  return (
    <section className="script-preview-page">
      <input
        ref={fileInputRef}
        type="file"
        accept=".txt,.md,.docx"
        style={{ display: 'none' }}
        aria-hidden
        onChange={(e) => void onImportFile(e)}
      />
      <div className="toolbar panel glass">
        <div>
          <h2>{currentProject.project.name}</h2>
          <p className="muted">完善剧本后可使用三阶段智能体优化；导入会替换原始剧本文本。</p>
        </div>
        <div className="actions script-preview-toolbar-actions">
          <AppButton variant="ghost" loading={importLoading} onClick={() => onPickImport()}>
            导入剧本
          </AppButton>
          <AppButton variant="primary" loading={refineLoading} onClick={() => void handleRefine()}>
            完善剧本
          </AppButton>
          <div style={{ width: 'min(360px, 100%)' }}>
            <AppInput
              value={briefPrompt}
              onChange={(v) => setBriefPrompt(String(v))}
              label="短提示词"
              as="textarea"
              rows={3}
              placeholder="例如：让节奏更紧凑，突出主角情绪变化。"
            />
          </div>
          <AppButton variant="primary" loading={refinePromptLoading} onClick={() => void handleRefineWithPrompt()}>
            剧本完善功能
          </AppButton>
          <AppButton loading={saveScriptLoading} onClick={() => void handleSave()}>
            保存修改
          </AppButton>
          <Link className="nav-btn" to={`/script-projects/${projectId}/assets`}>
            进入资产页
          </Link>
        </div>
      </div>

      <div className="panel glass script-optimize-bar">
        <span className="eyebrow">三阶段优化</span>
        <div className="script-optimize-actions">
          <AppButton size="sm" loading={optimizeLoading} onClick={() => void runOptimize('scenes')}>
            场景 / 场次
          </AppButton>
          <AppButton size="sm" loading={optimizeLoading} onClick={() => void runOptimize('characters')}>
            角色人设
          </AppButton>
          <AppButton size="sm" loading={optimizeLoading} onClick={() => void runOptimize('props')}>
            道具巧思
          </AppButton>
        </div>
        <p className="muted small">需先完成「完善剧本」。顺序建议：场景 → 角色 → 道具。</p>
      </div>

      <div className="script-preview-layout">
        <div className="script-preview-main">
          <div className="tabs panel glass">
            <button type="button" className={activeTab === 'original' ? 'active' : ''} onClick={() => setActiveTab('original')}>
              原始剧本
            </button>
            <button type="button" className={activeTab === 'refined' ? 'active' : ''} onClick={() => setActiveTab('refined')}>
              完善剧本
            </button>
            <button type="button" className={activeTab === 'structured' ? 'active' : ''} onClick={() => setActiveTab('structured')}>
              结构化预览
            </button>
            <button type="button" className={activeTab === 'json' ? 'active' : ''} onClick={() => setActiveTab('json')}>
              JSON
            </button>
          </div>

          {detailLoading || !scriptPayload ? (
            <LoadingSpinner />
          ) : (
            <div className="content panel glass">
              {activeTab === 'original' ? <textarea className="editor" value={scriptPayload.originalText} readOnly /> : null}
              {activeTab === 'refined' ? (
                <textarea className="editor" value={refinedText} onChange={(e) => setRefinedText(e.target.value)} />
              ) : null}
              {activeTab === 'structured' ? (
                <div className="script-structured-wrap">
                  <div className="script-structured-subtabs">
                    {(
                      [
                        ['overview', '概览'],
                        ['scenes', '场次'],
                        ['characters', '角色'],
                        ['props', '道具'],
                      ] as const
                    ).map(([key, label]) => (
                      <button
                        key={key}
                        type="button"
                        className={structuredSub === key ? 'active' : ''}
                        onClick={() => setStructuredSub(key)}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                  <ScriptStructuredPreview structured={scriptPayload.structuredScript || {}} subView={structuredSub} />
                </div>
              ) : null}
              {activeTab === 'json' ? <pre className="json">{structuredText}</pre> : null}
            </div>
          )}
        </div>

        <aside className="panel glass script-revision-aside">
          <h3>历史版本</h3>
          <ScriptRevisionPanel
            revisions={revisions}
            loading={revisionLoading}
            restoringId={restoringRevisionId}
            onRestore={(id) => void handleRestore(id)}
          />
        </aside>
      </div>
    </section>
  )
}
