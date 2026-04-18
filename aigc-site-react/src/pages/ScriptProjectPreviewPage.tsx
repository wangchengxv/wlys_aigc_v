import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { ScriptRevisionPanel } from '@/components/script/ScriptRevisionPanel'
import { ScriptAppendPreviewDialog } from '@/components/script/ScriptAppendPreviewDialog'
import { ScriptRewriteDialog } from '@/components/script/ScriptRewriteDialog'
import type { RewriteDiffMode } from '@/components/script/ScriptRewriteDiffPanel'
import { ProjectSubpageShell } from '@/components/script/ProjectSubpageShell'
import { ScriptStructuredPreview } from '@/components/script/ScriptStructuredPreview'
import { WorkflowModelPanel } from '@/components/script/WorkflowModelPanel'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import { getModels } from '@/api'
import type { ModelConfig } from '@/types'

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
  const appendLoading = useScriptProjectStore((s) => s.appendLoading)
  const rewriteLoading = useScriptProjectStore((s) => s.rewriteLoading)
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
  const appendPreview = useScriptProjectStore((s) => s.appendPreview)
  const rewritePreview = useScriptProjectStore((s) => s.rewritePreview)
  const applyRewrite = useScriptProjectStore((s) => s.applyRewrite)
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
  const [appendPreviewText, setAppendPreviewText] = useState('')
  const [appendPreviewMeta, setAppendPreviewMeta] = useState<{ existingLength: number; maxAppendChars: number; baseUsed: string } | null>(
    null,
  )
  const [showAppendDialog, setShowAppendDialog] = useState(false)
  const [showRewriteDialog, setShowRewriteDialog] = useState(false)
  const [rewriteInstruction, setRewriteInstruction] = useState('')
  const [rewriteTargetStyle, setRewriteTargetStyle] = useState('')
  const [rewriteMaxOutputChars, setRewriteMaxOutputChars] = useState('')
  const [rewritePreviewText, setRewritePreviewText] = useState('')
  const [rewriteDiffMode, setRewriteDiffMode] = useState<RewriteDiffMode>('split')
  const [rewritePreviewMeta, setRewritePreviewMeta] = useState<{ baseUsed: string; sourceLength: number; maxOutputChars?: number | null } | null>(
    null,
  )
  const [allModels, setAllModels] = useState<ModelConfig[]>([])

  useEffect(() => {
    setRefinedText(scriptPayload?.refinedMarkdown || '')
  }, [scriptPayload?.refinedMarkdown])

  useEffect(() => {
    void getModels().then(setAllModels).catch(() => {})
  }, [])

  useEffect(() => {
    if (!projectId) return
    void (async () => {
      try {
        await Promise.all([loadProject(projectId), loadScript(projectId), loadRevisions(projectId)])
      } catch (e) {
        showToast(e instanceof Error ? e.message : '页面初始化失败，请重试', 'error')
      }
    })()
    setActiveTab(initialTab)
  }, [projectId, loadProject, loadScript, loadRevisions, initialTab, showToast])

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

  async function handleAppendPreview() {
    try {
      const res = await appendPreview(projectId, {})
      setAppendPreviewText(res.appendText || '')
      setAppendPreviewMeta({ existingLength: res.existingLength, maxAppendChars: res.maxAppendChars, baseUsed: res.baseUsed })
      setShowAppendDialog(true)
    } catch (e) {
      showToast(e instanceof Error ? e.message : '续写预览失败', 'error')
    }
  }

  async function handleAppendConfirm() {
    try {
      const base = (scriptPayload?.refinedMarkdown || '').trim() ? scriptPayload?.refinedMarkdown || '' : scriptPayload?.originalText || ''
      const combined = `${base}${base.endsWith('\n') ? '' : '\n'}${appendPreviewText}`.trim()
      await saveScript(projectId, {
        refinedMarkdown: combined,
        structuredScript: scriptPayload?.structuredScript || {},
      })
      showToast('续写已追加并保存', 'success')
      setShowAppendDialog(false)
      setActiveTab('refined')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  async function handleRewritePreview() {
    const instruction = rewriteInstruction.trim()
    if (!instruction) {
      showToast('请先输入改写要求', 'error')
      return
    }
    const maxOutputChars = rewriteMaxOutputChars.trim() ? Number(rewriteMaxOutputChars.trim()) : undefined
    if (maxOutputChars != null && (!Number.isFinite(maxOutputChars) || maxOutputChars <= 0)) {
      showToast('字数上限必须为正整数', 'error')
      return
    }
    try {
      const result = await rewritePreview(projectId, {
        rewriteInstruction: instruction,
        targetStyle: rewriteTargetStyle.trim() || undefined,
        maxOutputChars,
        language: currentProject?.project.language || undefined,
      })
      setRewritePreviewText(result.rewrittenText || '')
      setRewritePreviewMeta({
        baseUsed: result.baseUsed,
        sourceLength: result.sourceLength,
        maxOutputChars: result.maxOutputChars,
      })
      showToast('改写预览生成成功', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '改写预览失败', 'error')
    }
  }

  async function handleApplyRewrite() {
    if (!rewritePreviewText.trim()) {
      showToast('请先生成改写预览', 'error')
      return
    }
    try {
      await applyRewrite(projectId, { rewrittenText: rewritePreviewText })
      setRefinedText(rewritePreviewText)
      setShowRewriteDialog(false)
      setRewriteDiffMode('split')
      setActiveTab('refined')
      showToast('改写结果已应用并保存', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '应用改写失败', 'error')
    }
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请返回列表重新选择项目。" />
  }

  const project = currentProject.project

  return (
    <ProjectSubpageShell
      projectId={projectId}
      title="剧本预览与优化"
      description="把导入、完善、续写、改写、结构化预览和版本回溯收进同一页，首屏只保留关键动作与当前状态。"
      meta={
        <>
          <span className="soft-badge">{project.name}</span>
          <span className="soft-badge">{scriptPayload?.structuredScript ? '结构化已生成' : '待结构化'}</span>
        </>
      }
      stats={[
        { key: 'revisions', label: '版本数', value: revisions.length },
        { key: 'documents', label: '文档版本', value: currentProject.documents.length },
        { key: 'assets', label: '已抽取资产', value: currentProject.assets.length },
        { key: 'shots', label: '镜头数', value: currentProject.shots.length },
      ]}
      helpTitle="查看剧本页说明"
      help={
        <>
          <p>导入、完善、结构化、续写和改写都集中在这页处理，减少剧本文本在多个入口间来回跳转。</p>
          <p>三阶段优化与版本回溯保留，但说明文案全部下沉到帮助提示，首屏只保留动作与结果。</p>
        </>
      }
    >
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
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
          <WorkflowModelPanel projectId={projectId} scope="preview" allModels={allModels} />
        </div>
        <div className="actions script-preview-toolbar-actions">
          <AppButton variant="ghost" loading={importLoading} onClick={() => onPickImport()}>
            导入剧本
          </AppButton>
          <AppButton variant="primary" loading={refineLoading} onClick={() => void handleRefine()}>
            完善剧本
          </AppButton>
          <AppButton variant="primary" loading={appendLoading} onClick={() => void handleAppendPreview()}>
            AI 续写剧本
          </AppButton>
          <AppButton variant="primary" loading={rewriteLoading} onClick={() => setShowRewriteDialog(true)}>
            AI 剧本改写
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

      <ScriptAppendPreviewDialog
        visible={showAppendDialog}
        appendText={appendPreviewText}
        loading={saveScriptLoading}
        subtitle={
          appendPreviewMeta
            ? `基于：${appendPreviewMeta.baseUsed}｜已有 ${appendPreviewMeta.existingLength} 字符｜本次上限 ${appendPreviewMeta.maxAppendChars} 字符`
            : undefined
        }
        onCancel={() => setShowAppendDialog(false)}
        onConfirmAppend={() => void handleAppendConfirm()}
      />

      <ScriptRewriteDialog
        visible={showRewriteDialog}
        instruction={rewriteInstruction}
        targetStyle={rewriteTargetStyle}
        maxOutputChars={rewriteMaxOutputChars}
        originalText={scriptPayload?.originalText || ''}
        previewText={rewritePreviewText}
        diffMode={rewriteDiffMode}
        loading={rewriteLoading}
        applying={rewriteLoading}
        previewSubtitle={
          rewritePreviewMeta
            ? `基于：${rewritePreviewMeta.baseUsed}｜原文长度 ${rewritePreviewMeta.sourceLength} 字符${
                rewritePreviewMeta.maxOutputChars ? `｜本次上限 ${rewritePreviewMeta.maxOutputChars} 字符` : ''
              }`
            : undefined
        }
        onChangeInstruction={setRewriteInstruction}
        onChangeTargetStyle={setRewriteTargetStyle}
        onChangeMaxOutputChars={setRewriteMaxOutputChars}
        onChangeDiffMode={setRewriteDiffMode}
        onCancel={() => setShowRewriteDialog(false)}
        onPreview={() => void handleRewritePreview()}
        onApply={() => void handleApplyRewrite()}
      />

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
    </ProjectSubpageShell>
  )
}
