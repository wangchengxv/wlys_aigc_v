import { LGraphCanvas } from '@comfyorg/litegraph'

/**
 * litegraph 在 drawFrontCanvas 里用 5 参数 drawImage 把离屏背景缩到
 * (bg/dpr)×(bg/dpr)。当主画布位图已是高分辨率（width≈css×dpr）时，合成结果只占左上角约 1/dpr²。
 * 这里改为 9 参数把 bg 完整贴满主画布，与节点层一致。
 */
export function applyLitegraphFrontCanvasBlitFix() {
  const proto = LGraphCanvas.prototype as unknown as {
    drawFrontCanvas?: () => void
    __aigcFrontCanvasBlitFix?: boolean
  }
  if (proto.__aigcFrontCanvasBlitFix) return
  proto.__aigcFrontCanvasBlitFix = true

  const original = proto.drawFrontCanvas
  if (!original) return

  proto.drawFrontCanvas = function (this: LGraphCanvas) {
    const self = this
    const ctx = this.ctx
    if (!ctx) return original.call(this)

    const savedDrawImage = ctx.drawImage.bind(ctx)
    let bgBlitFixed = false

    const patchedDrawImage = function (this: CanvasRenderingContext2D, ...args: unknown[]) {
      if (!bgBlitFixed && args.length === 5 && args[0] === self.bgcanvas) {
        bgBlitFixed = true
        const bg = self.bgcanvas as HTMLCanvasElement
        return (savedDrawImage as (typeof ctx)['drawImage'])(
          bg,
          0,
          0,
          bg.width,
          bg.height,
          0,
          0,
          self.canvas.width,
          self.canvas.height,
        )
      }
      return (savedDrawImage as (...a: unknown[]) => unknown).apply(ctx, args)
    }
    ctx.drawImage = patchedDrawImage as CanvasRenderingContext2D['drawImage']
    try {
      return original.call(self)
    } finally {
      ctx.drawImage = savedDrawImage as CanvasRenderingContext2D['drawImage']
    }
  }
}
