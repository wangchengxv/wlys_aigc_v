import { NavLink, useLocation } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { useAuthStore } from '@/stores/authStore'
import type { UserRole } from '@/types'

type ShellNavItem = {
  label: string
  to: string
  patterns?: string[]
  roles?: UserRole[]
}

type ShellNavGroup = {
  id: string
  title: string
  items: ShellNavItem[]
}

type Props = {
  open: boolean
  onClose: () => void
  onSignOut?: () => void
}

function matchesPath(pathname: string, patterns: string[]) {
  return patterns.some((pattern) => {
    if (pattern.endsWith('/*')) {
      const base = pattern.slice(0, -2)
      return pathname === base || pathname.startsWith(`${base}/`)
    }
    return pathname === pattern || pathname.startsWith(`${pattern}/`)
  })
}

function canView(role: UserRole | undefined, item: ShellNavItem) {
  if (!item.roles?.length) return true
  return role ? item.roles.includes(role) : false
}

function roleLabel(role?: UserRole) {
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

const SHELL_NAV: ShellNavGroup[] = [
  {
    id: 'workspace',
    title: '工作台',
    items: [
      { label: '首页', to: '/', patterns: ['/'] },
      { label: '创作工作台', to: '/workspace', patterns: ['/workspace', '/tools/image', '/tools/video', '/tools/image-to-video', '/canvas'] },
      { label: '无限画布', to: '/canvas' },
      { label: '历史记录', to: '/history' },
    ],
  },
  {
    id: 'teaching',
    title: '教学管理',
    items: [
      { label: '课程中心', to: '/courses', patterns: ['/courses/*'] },
    ],
  },
  {
    id: 'projects',
    title: '项目生产',
    items: [
      { label: '项目库', to: '/script-projects', patterns: ['/script-projects/*'] },
      { label: '全局设定', to: '/global-settings' },
      { label: '流程工作区', to: '/workflow/script-story', patterns: ['/workflow/*'] },
    ],
  },
  {
    id: 'resources',
    title: '资源中心',
    items: [
      { label: '媒体资源', to: '/admin/media-resources', roles: ['ADMIN', 'TEACHER'] },
      { label: '模型与服务商', to: '/models', patterns: ['/models', '/models/hub'], roles: ['ADMIN'] },
      { label: '三视图工具', to: '/tools/asset-visual', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
    ],
  },
  {
    id: 'admin',
    title: '平台管理',
    items: [
      { label: '组织用户', to: '/admin/directory', roles: ['ADMIN'] },
      { label: '审计日志', to: '/audit-logs', roles: ['ADMIN'] },
      { label: '设置', to: '/settings', roles: ['ADMIN', 'TEACHER'] },
    ],
  },
]

export function AppShellNav({ open, onClose, onSignOut }: Props) {
  const { pathname } = useLocation()
  const user = useAuthStore((s) => s.user)
  const groups = SHELL_NAV.map((group) => ({
    ...group,
    items: group.items.filter((item) => canView(user?.role, item)),
  })).filter((group) => group.items.length > 0)

  return (
    <aside className={`app-shell-nav${open ? ' is-open' : ''}`} aria-label="应用导航">
      <div className="app-shell-nav__brand">
        <span className="app-shell-nav__mark" aria-hidden>
          云
        </span>
        <div>
          <strong>AIGC实训台</strong>
          <span>{roleLabel(user?.role)}工作区</span>
        </div>
      </div>

      <div className="app-shell-nav__groups">
        {groups.map((group) => (
          <section key={group.id} className="app-shell-nav__group">
            <p className="app-shell-nav__group-title">{group.title}</p>
            <div className="app-shell-nav__links">
              {group.items.map((item) => (
                <NavLink
                  key={`${group.id}-${item.label}-${item.to}`}
                  to={item.to}
                  className={`app-shell-nav__link${matchesPath(pathname, item.patterns ?? [item.to]) ? ' is-active' : ''}`}
                  onClick={onClose}
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          </section>
        ))}
      </div>

      <div className="app-shell-nav__footer">
        <div className="app-shell-nav__footer-meta">
          <span>{user?.displayName || '访客模式'}</span>
          <strong>{user?.username || '打开登录后解锁完整后台能力'}</strong>
        </div>
        {user && onSignOut ? (
          <div className="app-shell-nav__footer-action">
            <AppButton size="sm" onClick={onSignOut}>
              退出
            </AppButton>
          </div>
        ) : null}
      </div>
    </aside>
  )
}
