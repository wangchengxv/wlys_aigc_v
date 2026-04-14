import { Link } from 'react-router-dom'

export function CanvasPage() {
  return (
    <section className="canvas-page">
      <div className="canvas-page__hero panel glass">
        <p className="eyebrow">Canvas</p>
        <h2>敬请期待</h2>
        <p className="muted">无限画布功能开发中，后续版本开放。</p>
        <p className="canvas-page__coming-soon-actions">
          <Link className="back-home" to="/">
            返回首页
          </Link>
        </p>
      </div>
    </section>
  )
}
