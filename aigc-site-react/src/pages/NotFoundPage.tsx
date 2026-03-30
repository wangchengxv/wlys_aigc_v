import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <section className="not-found-page">
      <p className="code">404</p>
      <h1>页面走丢了</h1>
      <p className="desc">你访问的页面不存在，可能已被移动或删除。</p>
      <Link className="back-home" to="/">
        返回首页
      </Link>
    </section>
  )
}
