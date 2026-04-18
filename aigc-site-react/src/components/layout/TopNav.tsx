import { useEffect, useMemo, useState, type Dispatch, type SetStateAction } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import type { UserRole } from '@/types'

type NavSectionKey =
  | 'overview'
  | 'courses'
  | 'projects'
  | 'tools'
  | 'directory'
  | 'resources'
  | 'review'
  | 'settings'

type NavItem = {
  label: string
  to: string
  patterns?: string[]
  roles?: UserRole[]
  badge?: string
}

type NavSection = {
  id: NavSectionKey
  title: string
  items: NavItem[]
}

function pathMatches(pathname: string, pattern: string): boolean {
  if (pattern.endsWith('/*')) {
    const base = pattern.slice(0, -2)
    return pathname === base || pathname.startsWith(`${base}/`)
  }
  return pathname === pattern || pathname.startsWith(`${pattern}/`)
}

const NAV_SECTIONS: NavSection[] = [
  {
    id: 'overview',
    title: '概览',
    items: [
      { label: '平台首页', to: '/' },
      { label: '平台概览', to: '/operations-dashboard', roles: ['ADMIN', 'TEACHER'] },
    ],
  },
  {
    id: 'courses',
    title: '课程与实训',
    items: [{ label: '课程工作台', to: '/courses', patterns: ['/courses/*'] }],
  },
  {
    id: 'projects',
    title: '项目与作品',
    items: [
      { label: '剧本工程', to: '/script-projects', patterns: ['/script-projects/*'] },
      { label: '剧本与故事', to: '/workflow/script-story' },
      { label: '场景与道具', to: '/workflow/scenes-props' },
      { label: '导演模式', to: '/workflow/director' },
      { label: '配音与旁白', to: '/workflow/dubbing' },
      { label: '成片与导出', to: '/workflow/export' },
      { label: '提示词管理', to: '/workflow/prompts' },
    ],
  },
  {
    id: 'tools',
    title: '创作工具',
    items: [
      { label: '文生图', to: '/tools/image' },
      { label: '文生视频', to: '/tools/video' },
      { label: '图生视频', to: '/tools/image-to-video' },
      { label: '三视图 / 九宫格', to: '/tools/asset-visual', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
      { label: '无限画布', to: '/canvas' },
      { label: '历史记录', to: '/history' },
    ],
  },
  {
    id: 'directory',
    title: '组织与用户',
    items: [
      { label: '组织与用户', to: '/admin/directory', roles: ['ADMIN'] },
    ],
  },
  {
    id: 'resources',
    title: '资源与模型',
    items: [
      { label: '媒体资源中心', to: '/admin/media-resources', roles: ['ADMIN', 'TEACHER'] },
      { label: '模型配置', to: '/models', roles: ['ADMIN'] },
      { label: '服务商中心', to: '/models/hub', roles: ['ADMIN'] },
    ],
  },
  {
    id: 'review',
    title: '审核与审计',
    items: [{ label: '审计日志', to: '/audit-logs', roles: ['ADMIN'] }],
  },
  {
    id: 'settings',
    title: '系统设置',
    items: [
      { label: '设置中心', to: '/settings' },
      { label: '全局设定', to: '/global-settings', roles: ['ADMIN'] },
    ],
  },
]

function isItemActive(pathname: string, item: NavItem) {
  const patterns = item.patterns ?? [item.to]
  return patterns.some((pattern) => pathMatches(pathname, pattern))
}

function canViewItem(role: UserRole | undefined, item: NavItem) {
  if (!item.roles) return true
  return role ? item.roles.includes(role) : false
}

type Props = {
  navOpen: boolean
  setNavOpen: Dispatch<SetStateAction<boolean>>
}

export function TopNav({ navOpen, setNavOpen }: Props) {
  const { pathname } = useLocation()
  const user = useAuthStore((s) => s.user)
  const roleText = user?.role === 'ADMIN' ? '管理员' : user?.role === 'TEACHER' ? '教师' : user?.role === 'STUDENT' ? '学生' : ''
  const userRole = user?.role

  const visibleSections = useMemo(
    () =>
      NAV_SECTIONS.map((section) => ({
        ...section,
        items: section.items.filter((item) => canViewItem(userRole, item)),
      })).filter((section) => section.items.length > 0),
    [userRole],
  )

  const activeSectionId = useMemo(
    () => visibleSections.find((section) => section.items.some((item) => isItemActive(pathname, item)))?.id ?? visibleSections[0]?.id,
    [pathname, visibleSections],
  )

  const [expandedSections, setExpandedSections] = useState<Record<string, boolean>>({})

  useEffect(() => {
    setExpandedSections((current) => {
      const next: Record<string, boolean> = {}
      visibleSections.forEach((section) => {
        next[section.id] = current[section.id] ?? section.id === activeSectionId
      })
      if (activeSectionId) {
        next[activeSectionId] = true
      }
      return next
    })
  }, [activeSectionId, visibleSections])

  return (
    <aside className={`admin-sidebar panel glass${navOpen ? ' is-open' : ''}`} aria-label="后台导航">
      <div className="admin-sidebar__brand">
        <span className="admin-sidebar__brand-mark" aria-hidden />
        <div className="admin-sidebar__brand-text">
          <strong>高校 AIGC 实训平台</strong>
          <span>Campus AI Console</span>
        </div>
      </div>

      <div className="admin-sidebar__meta">
        <span>{roleText || '访客模式'}</span>
        <span>{visibleSections.length} 个导航分组</span>
      </div>

      <nav className="admin-sidebar__nav">
        {visibleSections.map((section) => {
          const expanded = expandedSections[section.id] ?? true
          const active = section.id === activeSectionId
          return (
            <section key={section.id} className={`admin-sidebar__group${active ? ' is-active' : ''}`}>
              <button
                type="button"
                className="admin-sidebar__group-toggle"
                aria-expanded={expanded}
                onClick={() =>
                  setExpandedSections((current) => ({
                    ...current,
                    [section.id]: !expanded,
                  }))
                }
              >
                <span>{section.title}</span>
                <span className="admin-sidebar__group-caret" aria-hidden>
                  {expanded ? '−' : '+'}
                </span>
              </button>

              {expanded ? (
                <div className="admin-sidebar__group-links">
                  {section.items.map((item) => (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      className={`admin-sidebar__link${isItemActive(pathname, item) ? ' is-active' : ''}`}
                      onClick={() => setNavOpen(false)}
                    >
                      <span>{item.label}</span>
                      {item.badge ? <span className="admin-sidebar__badge">{item.badge}</span> : null}
                    </NavLink>
                  ))}
                </div>
              ) : null}
            </section>
          )
        })}
      </nav>

      <div className="admin-sidebar__footer">
        <span className="admin-sidebar__footer-label">{user ? '当前账号' : '访客提示'}</span>
        <strong>{user?.displayName || '访客模式'}</strong>
        <p>{user ? `${roleText} · ${user.username}` : '可从顶部栏右侧登录按钮打开全局登录弹窗，解锁受限导航与后台能力。'}</p>
      </div>
    </aside>
  )
}
