import { type ChangeEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { LGraph, LGraphCanvas, LGraphNode, LiteGraph } from '@comfyorg/litegraph'
import '@comfyorg/litegraph/style.css'
import { useNavigate } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { useToast } from '@/context/ToastContext'
import {
  buildPromptFromGraph,
  fetchComfyObjectInfo,
  parseComfyPromptResult,
  pollComfyHistory,
  queueComfyPrompt,
  type ComfyPromptResult,
} from '@/lib/graph/comfyApi'
import { applyLitegraphFrontCanvasBlitFix } from '@/lib/graph/litegraphViewFix'
import { applyState, graphToState, parseImportedState } from '@/lib/graph/serialize'
import type { GraphState } from '@/lib/graph/schema'
import { useCanvasGraphStore } from '@/stores/canvasGraphStore'

applyLitegraphFrontCanvasBlitFix()

type ControlledNodeType = 'aigc/prompt' | 'aigc/render'
type GraphTemplateId = 'starter' | 'blank'

type DraftMeta = {
  title: string
  projectId: string | null
}

type Props = {
  draftTitle: string
  projectId: string | null
  projectName?: string
  onDraftMetaHydrated?: (meta: DraftMeta) => void
}

const CONTROLLED_NODE_LABELS: Array<{ type: ControlledNodeType; label: string; description: string }> = [
  { type: 'aigc/prompt', label: '提示词节点', description: '输入镜头或画面描述' },
  { type: 'aigc/render', label: '渲染节点', description: '连接提示词后提交到 Comfy' },
]

const GRAPH_TEMPLATE_OPTIONS: Array<{ id: GraphTemplateId; label: string; description: string }> = [
  { id: 'starter', label: '海报出图模板', description: '内置 Prompt -> Render 结构，适合快速体验' },
  { id: 'blank', label: '空白画布', description: '只保留无限画布，按需添加受控节点' },
]

let nodesRegistered = false

class PromptNode extends LGraphNode {
  static title = 'Prompt'
  static desc = '输入提示词'

  constructor() {
    super('Prompt')
    this.addOutput('prompt', 'string')
    this.addWidget('text', 'Prompt', 'AIGC 电影海报，电影感，高细节', 'prompt')
    this.size = [280, 110]
    this.color = '#4c6ef5'
  }
}

class RenderNode extends LGraphNode {
  static title = 'Render'
  static desc = '图像生成'

  constructor() {
    super('Render')
    this.addInput('prompt', 'string')
    this.addOutput('image', 'image')
    this.addWidget('combo', 'Engine', 'flux', () => void 0, { values: ['flux', 'sdxl', 'wan2.1'] })
    this.size = [260, 120]
    this.color = '#16a34a'
  }
}

function registerDemoNodes() {
  if (nodesRegistered) return
  LiteGraph.registerNodeType('aigc/prompt', PromptNode)
  LiteGraph.registerNodeType('aigc/render', RenderNode)
  nodesRegistered = true
}

function normalizeDraftMeta(meta: Partial<DraftMeta>): DraftMeta {
  const title = meta.title?.trim() || '未命名无限画布'
  return {
    title,
    projectId: meta.projectId?.trim() || null,
  }
}

function seedGraph(graph: LGraph, templateId: GraphTemplateId = 'starter') {
  graph.clear()
  if (templateId === 'blank') return

  const prompt = LiteGraph.createNode('aigc/prompt')
  const render = LiteGraph.createNode('aigc/render')
  if (!prompt || !render) return
  prompt.pos = [80, 120]
  render.pos = [420, 140]
  graph.add(prompt)
  graph.add(render)
  prompt.connect(0, render, 0)
}

function formatSavedAt(updatedAt: number | null) {
  return updatedAt ? new Date(updatedAt).toLocaleString() : '尚未保存'
}

function remoteStatusText(status: string, errorText: string | null) {
  switch (status) {
    case 'saving':
      return '正在同步本地缓存与远端草稿'
    case 'saved':
      return '本地与远端草稿已同步'
    case 'error':
      return errorText ? `远端保存失败，已保留本地缓存：${errorText}` : '远端保存失败，已保留本地缓存'
    default:
      return '尚未开始保存'
  }
}

function executionStatusText(result: ComfyPromptResult | null, promptId: string | null) {
  if (!promptId) return '尚未提交 Comfy 工作流'
  if (!result) return `已提交到 Comfy，正在等待回显（prompt_id=${promptId}）`
  if (result.errorText) return `Comfy 执行失败：${result.errorText}`
  if (!result.completed) return `Comfy 仍在排队或执行中（prompt_id=${promptId}）`
  if (result.images.length > 0) return `Comfy 执行完成，已返回 ${result.images.length} 张图片`
  return `Comfy 执行完成，返回 ${result.outputCount} 个输出节点`
}

export function ComfyLikeCanvas({ draftTitle, projectId, projectName, onDraftMetaHydrated }: Props) {
  const { showToast } = useToast()
  const navigate = useNavigate()
  const viewportRef = useRef<HTMLDivElement>(null)
  const domCanvasRef = useRef<HTMLCanvasElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const graphRef = useRef<LGraph | null>(null)
  const lgCanvasRef = useRef<LGraphCanvas | null>(null)
  const draftMetaRef = useRef<DraftMeta>(normalizeDraftMeta({ title: draftTitle, projectId }))
  const initializedRef = useRef(false)
  const suspendNextMetaSyncRef = useRef(false)
  const [updatedAt, setUpdatedAt] = useState<number | null>(null)
  const [bridgeStatus, setBridgeStatus] = useState('画布已就绪，可导入 JSON、添加节点并提交到 Comfy')
  const [bridgeLoading, setBridgeLoading] = useState(false)
  const [activeTemplate, setActiveTemplate] = useState<GraphTemplateId>('starter')
  const [resultPromptId, setResultPromptId] = useState<string | null>(null)
  const [resultPayload, setResultPayload] = useState<ComfyPromptResult | null>(null)
  const comfyPollTokenRef = useRef(0)
  const redrawRafRef = useRef<number | null>(null)
  const interactionActiveRef = useRef(false)
  const setState = useCanvasGraphStore((s) => s.setState)
  const loadState = useCanvasGraphStore((s) => s.load)
  const syncFromRemote = useCanvasGraphStore((s) => s.syncFromRemote)
  const clearState = useCanvasGraphStore((s) => s.clear)
  const remoteStatus = useCanvasGraphStore((s) => s.remoteStatus)
  const remoteError = useCanvasGraphStore((s) => s.remoteError)

  const buildSnapshot = useCallback((): GraphState | null => {
    const graph = graphRef.current
    const canvas = lgCanvasRef.current
    if (!graph || !canvas) return null
    const meta = draftMetaRef.current
    return {
      ...graphToState(graph, canvas),
      title: meta.title,
      projectId: meta.projectId,
    }
  }, [])

  const saveSnapshot = useCallback(() => {
    const snapshot = buildSnapshot()
    if (!snapshot) return null
    setState(snapshot)
    setUpdatedAt(snapshot.updatedAt)
    return snapshot
  }, [buildSnapshot, setState])

  const hydrateDraftMeta = useCallback(
    (meta: Partial<DraftMeta>) => {
      const nextMeta = normalizeDraftMeta(meta)
      draftMetaRef.current = nextMeta
      suspendNextMetaSyncRef.current = true
      onDraftMetaHydrated?.(nextMeta)
    },
    [onDraftMetaHydrated],
  )

  useEffect(() => {
    draftMetaRef.current = normalizeDraftMeta({ title: draftTitle, projectId })
    if (!initializedRef.current) return
    if (suspendNextMetaSyncRef.current) {
      suspendNextMetaSyncRef.current = false
      return
    }
    saveSnapshot()
  }, [draftTitle, projectId, saveSnapshot])

  useEffect(() => {
    const canvasEl = domCanvasRef.current
    if (!canvasEl) return

    registerDemoNodes()
    const graph = new LGraph()
    const lgCanvas = new LGraphCanvas(canvasEl, graph)
    graphRef.current = graph
    lgCanvasRef.current = lgCanvas

    lgCanvas.background_image = ''
    lgCanvas.allow_dragcanvas = true
    lgCanvas.allow_interaction = true
    lgCanvas.render_shadows = false
    lgCanvas.zoom_speed = 1.08
    ;(lgCanvas as unknown as { clear_background?: boolean; always_render_background?: boolean }).clear_background = true
    ;(lgCanvas as unknown as { clear_background?: boolean; always_render_background?: boolean }).always_render_background = true
    ;(lgCanvas as unknown as { dirty_canvas?: boolean; dirty_bgcanvas?: boolean }).dirty_canvas = true
    ;(lgCanvas as unknown as { dirty_canvas?: boolean; dirty_bgcanvas?: boolean }).dirty_bgcanvas = true
    canvasEl.style.background = '#1e1e1e'

    const lgCanvasWithDraw = lgCanvas as unknown as {
      draw?: (...args: unknown[]) => unknown
      canvas?: HTMLCanvasElement
      ctx?: CanvasRenderingContext2D | null
    }
    const originalDraw = lgCanvasWithDraw.draw?.bind(lgCanvasWithDraw)
    if (originalDraw) {
      lgCanvasWithDraw.draw = (...args: unknown[]) => {
        const ctx = lgCanvasWithDraw.ctx
        const drawCanvas = lgCanvasWithDraw.canvas
        if (ctx && drawCanvas) {
          ctx.save()
          ctx.setTransform(1, 0, 0, 1, 0, 0)
          ctx.fillStyle = '#1e1e1e'
          ctx.fillRect(0, 0, drawCanvas.width, drawCanvas.height)
          ctx.restore()
        }
        return originalDraw(...args)
      }
    }

    const viewport = viewportRef.current

    const syncSize = () => {
      if (!viewport) return
      const w = viewport.clientWidth
      const h = viewport.clientHeight
      if (w > 0 && h > 0) {
        canvasEl.style.width = `${w}px`
        canvasEl.style.height = `${h}px`
        lgCanvas.resize(w, h)
        lgCanvas.setDirty(true, true)
      }
    }

    const scheduleFullRedraw = () => {
      if (redrawRafRef.current != null) return
      redrawRafRef.current = window.requestAnimationFrame(() => {
        redrawRafRef.current = null
        lgCanvas.setDirty(true, true)
        if (interactionActiveRef.current) {
          scheduleFullRedraw()
        }
      })
    }

    const startInteractionRedraw = () => {
      interactionActiveRef.current = true
      scheduleFullRedraw()
    }

    const stopInteractionRedraw = () => {
      interactionActiveRef.current = false
    }

    lgCanvas.startRendering()

    const ro =
      viewport && typeof ResizeObserver !== 'undefined'
        ? new ResizeObserver(() => syncSize())
        : null
    if (viewport && ro) ro.observe(viewport)

    requestAnimationFrame(() => {
      requestAnimationFrame(async () => {
        syncSize()

        const localCached = loadState()
        const cached = localCached?.graph ? localCached : await syncFromRemote()
        if (cached?.graph) {
          applyState(graph, lgCanvas, cached)
          hydrateDraftMeta({ title: cached.title, projectId: cached.projectId ?? null })
          setUpdatedAt(cached.updatedAt)
          setActiveTemplate(Array.isArray((cached.graph as { nodes?: unknown }).nodes) && ((cached.graph as { nodes?: unknown[] }).nodes?.length ?? 0) > 0 ? 'starter' : 'blank')
          setBridgeStatus('已恢复最近一次草稿与视口位置')
        } else {
          seedGraph(graph, 'starter')
          lgCanvas.fitViewToSelectionAnimated({ duration: 0 })
          initializedRef.current = true
          saveSnapshot()
          setBridgeStatus('已载入海报出图模板，可继续编辑或切换为空白画布')
          return
        }
        initializedRef.current = true
        lgCanvas.setDirty(true, true)
      })
    })

    const onResize = () => syncSize()
    const onPointerUp = () => saveSnapshot()
    const onPointerDown = () => startInteractionRedraw()
    const onPointerMove = () => {
      if (!interactionActiveRef.current) return
      scheduleFullRedraw()
    }
    const onWheel = () => {
      startInteractionRedraw()
      window.setTimeout(() => stopInteractionRedraw(), 180)
      saveSnapshot()
    }
    const onKeyDown = (event: KeyboardEvent) => {
      const tag = (event.target as HTMLElement)?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return

      if (event.key === '.') {
        event.preventDefault()
        lgCanvas.fitViewToSelectionAnimated({ duration: 180 })
        lgCanvas.setDirty(true, true)
        saveSnapshot()
      }
      if (event.key === 'Delete' || event.key === 'Backspace') {
        lgCanvas.deleteSelected()
        saveSnapshot()
      }
      if (event.altKey && (event.key === '+' || event.key === '=')) {
        event.preventDefault()
        lgCanvas.ds.changeDeltaScale(1.08, [window.innerWidth * 0.5, window.innerHeight * 0.5])
        lgCanvas.setDirty(true, true)
        saveSnapshot()
      }
      if (event.altKey && event.key === '-') {
        event.preventDefault()
        lgCanvas.ds.changeDeltaScale(1 / 1.08, [window.innerWidth * 0.5, window.innerHeight * 0.5])
        lgCanvas.setDirty(true, true)
        saveSnapshot()
      }
    }

    graph.onAfterChange = () => saveSnapshot()
    window.addEventListener('resize', onResize)
    window.addEventListener('keydown', onKeyDown)
    window.addEventListener('pointerup', stopInteractionRedraw)
    canvasEl.addEventListener('pointerup', onPointerUp)
    canvasEl.addEventListener('pointerdown', onPointerDown)
    canvasEl.addEventListener('pointermove', onPointerMove)
    canvasEl.addEventListener('wheel', onWheel)

    return () => {
      comfyPollTokenRef.current += 1
      initializedRef.current = false
      interactionActiveRef.current = false
      if (redrawRafRef.current != null) {
        window.cancelAnimationFrame(redrawRafRef.current)
        redrawRafRef.current = null
      }
      ro?.disconnect()
      window.removeEventListener('resize', onResize)
      window.removeEventListener('keydown', onKeyDown)
      window.removeEventListener('pointerup', stopInteractionRedraw)
      canvasEl.removeEventListener('pointerup', onPointerUp)
      canvasEl.removeEventListener('pointerdown', onPointerDown)
      canvasEl.removeEventListener('pointermove', onPointerMove)
      canvasEl.removeEventListener('wheel', onWheel)
      graph.onAfterChange = undefined
      lgCanvas.stopRendering()
      graph.clear()
      graphRef.current = null
      lgCanvasRef.current = null
    }
  }, [hydrateDraftMeta, loadState, saveSnapshot, syncFromRemote])

  const handleExport = useCallback(() => {
    const data = buildSnapshot()
    if (!data) return
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `aigc-canvas-${Date.now()}.json`
    anchor.click()
    URL.revokeObjectURL(url)
    showToast('当前画布已导出为 JSON', 'success')
  }, [buildSnapshot, showToast])

  const handleImportClick = useCallback(() => inputRef.current?.click(), [])

  const handleImportFile = useCallback(
    async (event: ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0]
      event.target.value = ''
      if (!file) return
      try {
        const text = await file.text()
        const parsed = parseImportedState(text)
        const graph = graphRef.current
        const canvas = lgCanvasRef.current
        if (!graph || !canvas) return
        const nextMeta = normalizeDraftMeta({
          title: parsed.title || draftMetaRef.current.title,
          projectId: parsed.projectId ?? draftMetaRef.current.projectId,
        })
        applyState(graph, canvas, parsed)
        hydrateDraftMeta(nextMeta)
        setState({ ...parsed, ...nextMeta })
        setUpdatedAt(parsed.updatedAt)
        canvas.fitViewToSelectionAnimated({ duration: 220 })
        setBridgeStatus('JSON 导入成功，已恢复节点与视口')
        showToast('画布 JSON 导入成功', 'success')
      } catch (error) {
        const message = error instanceof Error ? error.message : '导入失败'
        setBridgeStatus(`导入失败：${message}`)
        showToast(message, 'error')
      }
    },
    [hydrateDraftMeta, setState, showToast],
  )

  const handleApplyTemplate = useCallback(
    (templateId: GraphTemplateId) => {
      const graph = graphRef.current
      const canvas = lgCanvasRef.current
      if (!graph || !canvas) return
      setActiveTemplate(templateId)
      seedGraph(graph, templateId)
      if (templateId === 'blank') {
        canvas.ds.reset()
      } else {
        canvas.fitViewToSelectionAnimated({ duration: 220 })
      }
      saveSnapshot()
      setBridgeStatus(templateId === 'blank' ? '已切换为空白画布' : '已重置为海报出图模板')
      showToast(templateId === 'blank' ? '已切换为空白画布' : '已恢复海报出图模板', 'info')
    },
    [saveSnapshot, showToast],
  )

  const handleAddNode = useCallback(
    (type: ControlledNodeType) => {
      const graph = graphRef.current
      const canvas = lgCanvasRef.current
      if (!graph || !canvas) return
      const node = LiteGraph.createNode(type)
      if (!node) {
        showToast('当前节点类型不可用', 'error')
        return
      }
      const viewportWidth = canvas.canvas?.width ?? 960
      const viewportHeight = canvas.canvas?.height ?? 640
      const scale = canvas.ds.scale || 1
      const centerX = viewportWidth / scale / 2 - canvas.ds.offset[0]
      const centerY = viewportHeight / scale / 2 - canvas.ds.offset[1]
      node.pos = [centerX - node.size[0] / 2, centerY - node.size[1] / 2]
      graph.add(node)
      canvas.setDirty(true, true)
      saveSnapshot()
      setBridgeStatus(`已添加${type === 'aigc/prompt' ? '提示词' : '渲染'}节点`)
    },
    [saveSnapshot, showToast],
  )

  const handleFitView = useCallback(() => {
    const canvas = lgCanvasRef.current
    if (!canvas) return
    canvas.fitViewToSelectionAnimated({ duration: 180 })
    canvas.setDirty(true, true)
    saveSnapshot()
  }, [saveSnapshot])

  const handleClearCache = useCallback(() => {
    clearState()
    setUpdatedAt(null)
    setBridgeStatus('已清理本地缓存，当前画布仍保留在页面中，刷新后将不再自动恢复')
    showToast('本地缓存已清理', 'info')
  }, [clearState, showToast])

  const handleManualSave = useCallback(() => {
    const snapshot = saveSnapshot()
    if (!snapshot) return
    setBridgeStatus(`草稿已保存：${snapshot.title || '未命名无限画布'}`)
    showToast('草稿已保存', 'success')
  }, [saveSnapshot, showToast])

  const handleSyncObjectInfo = useCallback(async () => {
    setBridgeLoading(true)
    try {
      const objectInfo = await fetchComfyObjectInfo()
      const count = Object.keys(objectInfo).length
      setBridgeStatus(`Comfy 节点同步成功，共 ${count} 个节点类型；MVP 仍只开放受控节点目录`)
      showToast(`Comfy 已连通，共检测到 ${count} 个节点类型`, 'success')
    } catch (error) {
      const message = error instanceof Error ? error.message : '同步失败'
      setBridgeStatus(`同步失败：${message}`)
      showToast(message, 'error')
    } finally {
      setBridgeLoading(false)
    }
  }, [showToast])

  const handleQueueComfy = useCallback(async () => {
    const snapshot = saveSnapshot()
    if (!snapshot) return
    setBridgeLoading(true)
    const pollToken = Date.now()
    comfyPollTokenRef.current = pollToken
    setResultPayload(null)
    try {
      const prompt = buildPromptFromGraph(snapshot)
      const queued = await queueComfyPrompt(prompt)
      setResultPromptId(queued.prompt_id)
      setBridgeStatus(`已提交到 Comfy 队列，prompt_id=${queued.prompt_id}`)
      const polled = await pollComfyHistory(queued.prompt_id, { intervalMs: 2000, maxAttempts: 30 })
      if (comfyPollTokenRef.current !== pollToken) return
      const parsed = parseComfyPromptResult(polled.history, queued.prompt_id)
      setResultPayload(parsed)
      setBridgeStatus(executionStatusText(parsed, queued.prompt_id))
      if (parsed.errorText) {
        showToast(parsed.errorText, 'error', 3000)
      } else if (parsed.images.length > 0) {
        showToast('Comfy 执行完成，结果已回显', 'success')
      } else {
        showToast(polled.completed ? 'Comfy 执行完成' : 'Comfy 仍在执行中', 'info')
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : '提交失败'
      setBridgeStatus(`提交失败：${message}`)
      setResultPayload({
        completed: true,
        statusText: 'execution_error',
        errorText: message,
        images: [],
        outputCount: 0,
      })
      showToast(message, 'error', 3000)
    } finally {
      if (comfyPollTokenRef.current === pollToken) {
        setBridgeLoading(false)
      }
    }
  }, [saveSnapshot, showToast])

  const resultSummary = useMemo(() => executionStatusText(resultPayload, resultPromptId), [resultPayload, resultPromptId])

  return (
    <div className="comfy-canvas panel glass">
      <div className="comfy-canvas__toolbar">
        <div className="comfy-canvas__actions">
          <AppButton size="sm" variant="primary" onClick={handleManualSave}>
            保存草稿
          </AppButton>
          <AppButton size="sm" variant="primary" loading={bridgeLoading} onClick={handleQueueComfy}>
            提交到 Comfy
          </AppButton>
          <AppButton size="sm" variant="ghost" onClick={handleExport}>
            导出 JSON
          </AppButton>
          <AppButton size="sm" variant="ghost" onClick={handleImportClick}>
            导入 JSON
          </AppButton>
          <AppButton size="sm" variant="ghost" onClick={handleFitView}>
            适配视图
          </AppButton>
          <AppButton size="sm" variant="ghost" onClick={handleClearCache}>
            清理本地缓存
          </AppButton>
          <AppButton size="sm" variant="ghost" loading={bridgeLoading} onClick={handleSyncObjectInfo}>
            检查 Comfy 连通性
          </AppButton>
          <AppButton size="sm" variant="ghost" onClick={() => navigate('/tools/reverse-prompt')}>
            反推提示词
          </AppButton>
        </div>

        <div className="comfy-canvas__status-grid">
          <div className="comfy-canvas__status-card">
            <span className="comfy-canvas__status-label">草稿状态</span>
            <strong>{formatSavedAt(updatedAt)}</strong>
            <p className="muted">{remoteStatusText(remoteStatus, remoteError)}</p>
          </div>
          <div className="comfy-canvas__status-card">
            <span className="comfy-canvas__status-label">项目绑定</span>
            <strong>{projectName || '未绑定工程'}</strong>
            <p className="muted">{projectId ? `已关联项目 ID：${projectId}` : '未绑定时仍可编辑、保存与执行'}</p>
          </div>
          <div className="comfy-canvas__status-card">
            <span className="comfy-canvas__status-label">Comfy 回显</span>
            <strong>{resultPromptId || '未提交'}</strong>
            <p className="muted">{resultSummary}</p>
          </div>
        </div>

        <div className="comfy-canvas__control-grid">
          <div className="comfy-canvas__control-card">
            <p className="comfy-canvas__control-title">受控模板</p>
            <div className="comfy-canvas__choice-list">
              {GRAPH_TEMPLATE_OPTIONS.map((option) => (
                <button
                  key={option.id}
                  type="button"
                  className={`comfy-canvas__choice${activeTemplate === option.id ? ' is-active' : ''}`}
                  onClick={() => handleApplyTemplate(option.id)}
                >
                  <strong>{option.label}</strong>
                  <span>{option.description}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="comfy-canvas__control-card">
            <p className="comfy-canvas__control-title">受控节点目录</p>
            <div className="comfy-canvas__choice-list">
              {CONTROLLED_NODE_LABELS.map((item) => (
                <button key={item.type} type="button" className="comfy-canvas__choice" onClick={() => handleAddNode(item.type)}>
                  <strong>{item.label}</strong>
                  <span>{item.description}</span>
                </button>
              ))}
            </div>
          </div>
        </div>

        <p className="comfy-canvas__tips muted">
          快捷键：`Space` 拖动画布、滚轮缩放、`Alt +/-` 缩放、`.` 适配视图、`Delete` 删除节点
        </p>
        <p className="comfy-canvas__tips">{bridgeStatus}</p>
      </div>

      <input ref={inputRef} type="file" accept="application/json" hidden onChange={handleImportFile} />

      <div ref={viewportRef} className="comfy-canvas__viewport">
        <canvas ref={domCanvasRef} className="comfy-canvas__surface" />
      </div>

      <div className="comfy-canvas__result panel glass">
        <div className="section-heading">
          <h3>执行结果</h3>
          <span>{resultPromptId ? `prompt_id=${resultPromptId}` : '提交后在此显示结果摘要与预览'}</span>
        </div>

        {resultPayload?.images.length ? (
          <div className="comfy-canvas__result-grid">
            {resultPayload.images.map((image) => (
              <figure key={`${image.nodeId}-${image.filename}`} className="comfy-canvas__image-card">
                <img src={image.url} alt={image.filename} />
                <figcaption>
                  <strong>{image.filename}</strong>
                  <span>输出节点：{image.nodeId}</span>
                </figcaption>
              </figure>
            ))}
          </div>
        ) : (
          <div className="comfy-canvas__result-empty">
            <strong>{resultPayload?.errorText ? '执行失败' : '暂未回显图片'}</strong>
            <p className="muted">
              {resultPayload?.errorText
                ? resultPayload.errorText
                : resultPromptId
                  ? '当前任务可能仍在排队，或仅返回文本/结构化输出摘要。'
                  : '先在工具栏点击“提交到 Comfy”，再查看状态与结果。'}
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
