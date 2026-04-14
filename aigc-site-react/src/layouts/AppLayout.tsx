import { useEffect, useState } from 'react'
import { Outlet, useMatches } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { TopNav } from '@/components/layout/TopNav'
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
      <TopNav navOpen={navOpen} setNavOpen={setNavOpen} />

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
