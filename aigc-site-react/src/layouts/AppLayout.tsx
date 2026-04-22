import { useEffect, useState } from 'react'
import { Outlet, useLocation, useMatches, useNavigate } from 'react-router-dom'
import { PageToolbar } from '@/components/common/PageToolbar'
import { AppShellNav } from '@/components/layout/AppShellNav'
import type { RouteHandle } from '@/routes/types'
import { useAuthStore } from '@/stores/authStore'
import { useStyleTemplateStore } from '@/stores/styleTemplateStore'
import { WelcomePage } from '@/pages/WelcomePage'

const DEFAULT_META: RouteHandle = {
  title: '高校 AIGC 实训平台',
  eyebrow: '工作区',
  section: '工作台',
}

function roleText(role?: string | null) {
  switch (role) {
    case 'ADMIN':
      return '管理员'
    case 'TEACHER':
      return '教师'
    case 'STUDENT':
      return '学生'
    default:
      return '访客'
  }
}

export function AppLayout() {
  const location = useLocation()
  const matches = useMatches()
  const navigate = useNavigate()
  const [navOpen, setNavOpen] = useState(false)
  const last = matches[matches.length - 1]
  const handle = (last?.handle as RouteHandle | undefined) ?? DEFAULT_META
  const initAuth = useAuthStore((s) => s.init)
  const user = useAuthStore((s) => s.user)
  const signOut = useAuthStore((s) => s.signOut)
  const loadTemplates = useStyleTemplateStore((s) => s.loadTemplates)

  useEffect(() => {
    void initAuth()
  }, [initAuth])

  useEffect(() => {
    void loadTemplates()
  }, [loadTemplates])

  useEffect(() => {
    setNavOpen(false)
  }, [location.pathname])

  function handleSignOut() {
    void signOut().then(() => navigate('/login'))
  }

  if (!user) {
    return <WelcomePage />
  }

  return (
    <div className="app-shell">
      <button
        type="button"
        className={`app-shell__mask${navOpen ? ' is-visible' : ''}`}
        aria-label="关闭导航"
        onClick={() => setNavOpen(false)}
      />
      <AppShellNav open={navOpen} onClose={() => setNavOpen(false)} onSignOut={handleSignOut} />

      <div className="app-shell__main">
        <main className="app-shell__content">
          <PageToolbar
            eyebrow={handle.eyebrow}
            title={handle.title}
            meta={
              <>
                <span className="page-toolbar__chip">{handle.section || handle.eyebrow}</span>
                <span className="page-toolbar__chip">{roleText(user?.role)}</span>
              </>
            }
          />
          <div className="app-shell__outlet">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
