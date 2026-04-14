import { useCallback, useEffect, useMemo, useState, type Dispatch, type SetStateAction } from 'react'
import { NavLink, useLocation } from 'react-router-dom'

type NavKey = 'home' | 'workflow' | 'canvas' | 'tools' | 'history' | 'settings'

function pathMatches(pathname: string, pattern: string): boolean {
  if (pattern.endsWith('/*')) {
    const base = pattern.slice(0, -2)
    return pathname === base || pathname.startsWith(`${base}/`)
  }
  return pathname === pattern || pathname.startsWith(`${pattern}/`)
}

function activeKeyForPath(pathname: string): NavKey {
  if (pathname === '/') return 'home'
  if (pathMatches(pathname, '/global-settings') || pathMatches(pathname, '/script-projects') || pathMatches(pathname, '/workflow')) {
    return 'workflow'
  }
  if (pathMatches(pathname, '/canvas')) return 'canvas'
  if (pathname === '/workspace' || pathMatches(pathname, '/tools')) return 'tools'
  if (pathMatches(pathname, '/history')) return 'history'
  if (pathMatches(pathname, '/settings') || pathMatches(pathname, '/models')) return 'settings'
  return 'home'
}

type Props = {
  navOpen: boolean
  setNavOpen: Dispatch<SetStateAction<boolean>>
}

export function TopNav({ navOpen, setNavOpen }: Props) {
  const { pathname } = useLocation()
  const activeKey = activeKeyForPath(pathname)
  const [hoverKey, setHoverKey] = useState<NavKey | null>(null)
  const [tapKey, setTapKey] = useState<NavKey | null>(null)
  const [canHover, setCanHover] = useState(true)

  useEffect(() => {
    const mq = window.matchMedia('(hover: hover)')
    const apply = () => setCanHover(mq.matches)
    apply()
    mq.addEventListener('change', apply)
    return () => mq.removeEventListener('change', apply)
  }, [])

  const openKey = useMemo(() => {
    if (canHover) return hoverKey
    return tapKey
  }, [canHover, hoverKey, tapKey])

  const onGroupEnter = useCallback(
    (key: NavKey) => {
      if (canHover) setHoverKey(key)
    },
    [canHover],
  )

  const onGroupLeave = useCallback(() => {
    if (canHover) setHoverKey(null)
  }, [canHover])

  const toggleTap = useCallback(
    (key: NavKey) => {
      if (canHover) return
      setTapKey((k) => (k === key ? null : key))
    },
    [canHover],
  )

  useEffect(() => {
    setHoverKey(null)
    setTapKey(null)
  }, [pathname])

  function dropdownOpen(key: NavKey) {
    return openKey === key
  }

  const onNavigate = () => setNavOpen(false)

  const clusterClass = `top-nav__cluster${navOpen ? ' top-nav__cluster--open' : ''}`

  return (
    <header className="top-nav panel glass top-nav--mega">
      <div className="brand">
        <span className="dot" aria-hidden />
        <span className="brand-text">
          <span className="brand-name">AIGC Studio</span>
          <span className="brand-tag">Image · Video · Script</span>
        </span>
      </div>

      <nav className={clusterClass} aria-label="主导航">
        <div
          className={`top-nav__item top-nav__item--accent-home${activeKey === 'home' ? ' is-active' : ''}`}
          onMouseEnter={() => onGroupEnter('home')}
          onMouseLeave={onGroupLeave}
        >
          <NavLink to="/" end className="top-nav__link" onClick={onNavigate}>
            首页
          </NavLink>
        </div>

        <div
          className={`top-nav__item top-nav__item--has-sub top-nav__item--accent-workflow${activeKey === 'workflow' ? ' is-active' : ''}`}
          onMouseEnter={() => onGroupEnter('workflow')}
          onMouseLeave={onGroupLeave}
        >
          <button
            type="button"
            className={`top-nav__trigger${dropdownOpen('workflow') ? ' is-open' : ''}`}
            aria-expanded={dropdownOpen('workflow')}
            onClick={() => toggleTap('workflow')}
          >
            工作流模式
          </button>
          <ul className={`top-nav__dropdown${dropdownOpen('workflow') ? ' is-visible' : ''}`} role="list">
            <li>
              <NavLink to="/global-settings" onClick={onNavigate}>
                全局设置
              </NavLink>
            </li>
            <li>
              <NavLink to="/script-projects" onClick={onNavigate}>
                剧本工程
              </NavLink>
            </li>
            <li>
              <NavLink to="/workflow/script-story" onClick={onNavigate}>
                剧本与故事
              </NavLink>
            </li>
            <li>
              <NavLink to="/workflow/scenes-props" onClick={onNavigate}>
                场景与道具
              </NavLink>
            </li>
            <li>
              <NavLink to="/workflow/director" onClick={onNavigate}>
                导演模式
              </NavLink>
            </li>
            <li>
              <NavLink to="/workflow/export" onClick={onNavigate}>
                成片与导出
              </NavLink>
            </li>
            <li>
              <NavLink to="/workflow/prompts" onClick={onNavigate}>
                提示词管理
              </NavLink>
            </li>
          </ul>
        </div>

        <div
          className={`top-nav__item top-nav__item--accent-canvas${activeKey === 'canvas' ? ' is-active' : ''}`}
          onMouseEnter={() => onGroupEnter('canvas')}
          onMouseLeave={onGroupLeave}
        >
          <NavLink to="/canvas" className="top-nav__link" onClick={onNavigate}>
            无限画布模式
            <span className="top-nav__badge-muted" aria-hidden>
              （后期开发）
            </span>
          </NavLink>
        </div>

        <div
          className={`top-nav__item top-nav__item--has-sub top-nav__item--accent-tools${activeKey === 'tools' ? ' is-active' : ''}`}
          onMouseEnter={() => onGroupEnter('tools')}
          onMouseLeave={onGroupLeave}
        >
          <button
            type="button"
            className={`top-nav__trigger${dropdownOpen('tools') ? ' is-open' : ''}`}
            aria-expanded={dropdownOpen('tools')}
            onClick={() => toggleTap('tools')}
          >
            小工具
          </button>
          <ul className={`top-nav__dropdown${dropdownOpen('tools') ? ' is-visible' : ''}`} role="list">
            <li>
              <NavLink to="/tools/image" onClick={onNavigate}>
                文生图
              </NavLink>
            </li>
            <li>
              <NavLink to="/tools/video" onClick={onNavigate}>
                文生视频
              </NavLink>
            </li>
            <li>
              <NavLink to="/tools/image-to-video" onClick={onNavigate}>
                图生视频
              </NavLink>
            </li>
            <li>
              <NavLink to="/tools/asset-visual" onClick={onNavigate}>
                三视图 / 九宫格
              </NavLink>
            </li>
          </ul>
        </div>

        <div
          className={`top-nav__item top-nav__item--accent-history${activeKey === 'history' ? ' is-active' : ''}`}
          onMouseEnter={() => onGroupEnter('history')}
          onMouseLeave={onGroupLeave}
        >
          <NavLink to="/history" className="top-nav__link" onClick={onNavigate}>
            历史记录
          </NavLink>
        </div>

        <div
          className={`top-nav__item top-nav__item--accent-settings${activeKey === 'settings' ? ' is-active' : ''}`}
          onMouseEnter={() => onGroupEnter('settings')}
          onMouseLeave={onGroupLeave}
        >
          <NavLink to="/settings" className="top-nav__link" onClick={onNavigate}>
            设置
          </NavLink>
        </div>
      </nav>

      <button
        className="hamburger"
        type="button"
        aria-expanded={navOpen}
        aria-label="菜单"
        onClick={() => setNavOpen((o) => !o)}
      >
        <span />
        <span />
        <span />
      </button>
    </header>
  )
}
