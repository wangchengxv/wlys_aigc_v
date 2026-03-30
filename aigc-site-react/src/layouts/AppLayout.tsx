import { useEffect, useState } from 'react'
import { NavLink, Outlet, useMatches } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { ThemeToggle } from '@/components/common/ThemeToggle'
import type { RouteHandle } from '@/routes/types'

const DEFAULT_META: RouteHandle = { title: 'AIGC 图文生成平台', eyebrow: 'Studio' }

export function AppLayout() {
  const matches = useMatches()
  const last = matches[matches.length - 1]
  const handle = (last?.handle as RouteHandle | undefined) ?? DEFAULT_META
  const [navOpen, setNavOpen] = useState(false)
  const [showBackTop, setShowBackTop] = useState(false)

  useEffect(() => {
    const onScroll = () => setShowBackTop(window.scrollY > 380)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <div className="app-layout">
      <header className="top-nav panel glass">
        <div className="brand">
          <span className="dot" aria-hidden />
          <span className="brand-text">
            <span className="brand-name">AIGC Studio</span>
            <span className="brand-tag">Image · Video · Text</span>
          </span>
        </div>

        <nav className={`nav-links${navOpen ? ' open' : ''}`}>
          <NavLink to="/" end onClick={() => setNavOpen(false)}>
            首页
          </NavLink>
          <NavLink to="/global-settings" onClick={() => setNavOpen(false)}>
            全局设定
          </NavLink>
          <NavLink to="/workspace" onClick={() => setNavOpen(false)}>
            生成工作台
          </NavLink>
          <NavLink to="/script-projects" onClick={() => setNavOpen(false)}>
            剧本项目
          </NavLink>
          <NavLink to="/history" onClick={() => setNavOpen(false)}>
            历史记录
          </NavLink>
          <NavLink to="/models" onClick={() => setNavOpen(false)}>
            模型配置
          </NavLink>
          <NavLink to="/settings" onClick={() => setNavOpen(false)}>
            设置
          </NavLink>
        </nav>

        <div className="theme-slot">
          <ThemeToggle />
        </div>

        <button className="hamburger" type="button" aria-expanded={navOpen} aria-label="菜单" onClick={() => setNavOpen((o) => !o)}>
          <span />
          <span />
          <span />
        </button>
      </header>

      <main className="page-container">
        <header className="page-head">
          <p className="page-eyebrow">{handle.eyebrow}</p>
          <h1>{handle.title}</h1>
        </header>

        <div className="page-outlet" key={last?.pathname}>
          <Outlet />
        </div>
      </main>

      {showBackTop ? (
        <AppButton className="back-top" size="sm" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
          返回顶部
        </AppButton>
      ) : null}
    </div>
  )
}
