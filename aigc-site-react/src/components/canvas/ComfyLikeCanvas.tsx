import { type ChangeEvent, useCallback, useEffect, useRef, useState } from 'react'
import { LGraph, LGraphCanvas, LGraphNode, LiteGraph } from '@comfyorg/litegraph'
import '@comfyorg/litegraph/style.css'
import { AppButton } from '@/components/common/AppButton'
import { buildPromptFromGraph, fetchComfyObjectInfo, pollComfyHistory, queueComfyPrompt } from '@/lib/graph/comfyApi'
import { applyLitegraphFrontCanvasBlitFix } from '@/lib/graph/litegraphViewFix'
import { applyState, graphToState, parseImportedState } from '@/lib/graph/serialize'
import { useCanvasGraphStore } from '@/stores/canvasGraphStore'

applyLitegraphFrontCanvasBlitFix()

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

function seedGraph(graph: LGraph) {
  const prompt = LiteGraph.createNode('aigc/prompt')
  const render = LiteGraph.createNode('aigc/render')
  if (!prompt || !render) return
  prompt.pos = [80, 120]
  render.pos = [420, 140]
  graph.add(prompt)
  graph.add(render)
  prompt.connect(0, render, 0)
}

export function ComfyLikeCanvas() {
  const viewportRef = useRef<HTMLDivElement>(null)
  const domCanvasRef = useRef<HTMLCanvasElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const graphRef = useRef<LGraph | null>(null)
  const lgCanvasRef = useRef<LGraphCanvas | null>(null)
  const [updatedAt, setUpdatedAt] = useState<number | null>(null)
  const [bridgeStatus, setBridgeStatus] = useState('Comfy 桥接未连接')
  const [bridgeLoading, setBridgeLoading] = useState(false)
  const comfyPollTokenRef = useRef(0)
  const redrawRafRef = useRef<number | null>(null)
  const interactionActiveRef = useRef(false)
  const setState = useCanvasGraphStore((s) => s.setState)
  const loadState = useCanvasGraphStore((s) => s.load)
  const syncFromRemote = useCanvasGraphStore((s) => s.syncFromRemote)
  const clearState = useCanvasGraphStore((s) => s.clear)

  const saveSnapshot = useCallback(() => {
    const graph = graphRef.current
    const canvas = lgCanvasRef.current
    if (!graph || !canvas) return
    const snapshot = graphToState(graph, canvas)
    setState(snapshot)
    setUpdatedAt(snapshot.updatedAt)
  }, [setState])

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
    // Force full-canvas clear each frame to avoid dirty-rect artifacts.
    ;(lgCanvas as unknown as { clear_background?: boolean; always_render_background?: boolean }).clear_background = true
    ;(lgCanvas as unknown as { clear_background?: boolean; always_render_background?: boolean }).always_render_background = true
    // Disable partial redraw optimization to avoid edge artifacts on some GPUs/drivers.
    ;(lgCanvas as unknown as { dirty_canvas?: boolean; dirty_bgcanvas?: boolean }).dirty_canvas = true
    ;(lgCanvas as unknown as { dirty_canvas?: boolean; dirty_bgcanvas?: boolean }).dirty_bgcanvas = true
    canvasEl.style.background = '#1e1e1e'

    // Hard fallback: full clear before each draw to eliminate residual trails.
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
        // 只通过 LGraphCanvas.resize 改 buffer；若在外部先改 canvas.width，resize 会提前 return，bgcanvas 可能不同步。
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

    // Use double RAF so layout is stable before first graph render.
    requestAnimationFrame(() => {
      requestAnimationFrame(async () => {
        syncSize()

        const localCached = loadState()
        const cached = localCached?.graph ? localCached : await syncFromRemote()
        if (cached?.graph) {
          applyState(graph, lgCanvas, cached)
          setUpdatedAt(cached.updatedAt)
        } else {
          seedGraph(graph)
          lgCanvas.fitViewToSelectionAnimated({ duration: 0 })
          saveSnapshot()
        }
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
      // Ignore shortcuts when typing in inputs
      const tag = (event.target as HTMLElement)?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return

      if (event.key === '.') {
        event.preventDefault()
        lgCanvas.ds.reset()
        lgCanvas.setDirty(true, true)
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
  }, [loadState, saveSnapshot, syncFromRemote])

  const handleExport = useCallback(() => {
    const graph = graphRef.current
    const canvas = lgCanvasRef.current
    if (!graph || !canvas) return
    const data = graphToState(graph, canvas)
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `aigc-canvas-${Date.now()}.json`
    anchor.click()
    URL.revokeObjectURL(url)
  }, [])

  const handleImportClick = useCallback(() => inputRef.current?.click(), [])

  const handleImportFile = useCallback(
    async (event: ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0]
      event.target.value = ''
      if (!file) return
      const text = await file.text()
      const parsed = parseImportedState(text)
      const graph = graphRef.current
      const canvas = lgCanvasRef.current
      if (!graph || !canvas) return
      applyState(graph, canvas, parsed)
      setState(parsed)
      setUpdatedAt(parsed.updatedAt)
      canvas.fitViewToSelectionAnimated({ duration: 220 })
    },
    [setState],
  )

  const handleReset = useCallback(() => {
    const graph = graphRef.current
    const canvas = lgCanvasRef.current
    if (!graph || !canvas) return
    graph.clear()
    seedGraph(graph)
    canvas.fitViewToSelectionAnimated({ duration: 220 })
    saveSnapshot()
  }, [saveSnapshot])

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
  }, [clearState])

  const handleSyncObjectInfo = useCallback(async () => {
    setBridgeLoading(true)
    try {
      const objectInfo = await fetchComfyObjectInfo()
      const count = Object.keys(objectInfo).length
      setBridgeStatus(`Comfy 节点同步成功，共 ${count} 个节点类型`)
    } catch (error) {
      setBridgeStatus(error instanceof Error ? `同步失败：${error.message}` : '同步失败')
    } finally {
      setBridgeLoading(false)
    }
  }, [])

  const handleQueueComfy = useCallback(async () => {
    const graph = graphRef.current
    const canvas = lgCanvasRef.current
    if (!graph || !canvas) return
    setBridgeLoading(true)
    const pollToken = Date.now()
    comfyPollTokenRef.current = pollToken
    try {
      const snapshot = graphToState(graph, canvas)
      const prompt = buildPromptFromGraph(snapshot)
      const queued = await queueComfyPrompt(prompt)
      setBridgeStatus(`已提交队列，prompt_id=${queued.prompt_id}`)
      const polled = await pollComfyHistory(queued.prompt_id, { intervalMs: 2000, maxAttempts: 30 })
      if (comfyPollTokenRef.current !== pollToken) return
      setBridgeStatus(polled.completed ? `任务完成，prompt_id=${queued.prompt_id}` : `任务排队中，prompt_id=${queued.prompt_id}`)
    } catch (error) {
      setBridgeStatus(error instanceof Error ? `提交失败：${error.message}` : '提交失败')
    } finally {
      if (comfyPollTokenRef.current === pollToken) {
        setBridgeLoading(false)
      }
    }
  }, [])

  return (
    <div className="comfy-canvas panel glass">
      <div className="comfy-canvas__toolbar">
        <div className="comfy-canvas__actions">
          <AppButton size="sm" variant="ghost" onClick={handleExport}>
            导出 JSON
          </AppButton>
          <AppButton size="sm" variant="ghost" onClick={handleImportClick}>
            导入 JSON
          </AppButton>
          <AppButton size="sm" variant="ghost" onClick={handleReset}>
            重置示例图
          </AppButton>
          <AppButton size="sm" variant="ghost" onClick={handleFitView}>
            适配视图
          </AppButton>
          <AppButton size="sm" variant="ghost" onClick={handleClearCache}>
            清理本地缓存
          </AppButton>
          <AppButton size="sm" variant="ghost" loading={bridgeLoading} onClick={handleSyncObjectInfo}>
            同步 Comfy 节点
          </AppButton>
          <AppButton size="sm" variant="primary" loading={bridgeLoading} onClick={handleQueueComfy}>
            提交到 Comfy
          </AppButton>
        </div>
        <p className="comfy-canvas__tips muted">
          快捷键：`Space` 拖动、滚轮缩放、`Alt +/-` 缩放、`.` 适配视图、`Delete` 删除
          {updatedAt ? ` · 已保存 ${new Date(updatedAt).toLocaleTimeString()}` : ''}
        </p>
        <p className="comfy-canvas__tips">{bridgeStatus}</p>
      </div>
      <input ref={inputRef} type="file" accept="application/json" hidden onChange={handleImportFile} />
      <div ref={viewportRef} className="comfy-canvas__viewport">
        <canvas ref={domCanvasRef} className="comfy-canvas__surface" />
      </div>
    </div>
  )
}
