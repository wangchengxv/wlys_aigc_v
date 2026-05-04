import { useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

export type NavKey = 'home' | 'projects' | 'creation' | 'assets' | 'config';

type NavItem = {
  key: NavKey;
  label: string;
  to: string;
};

const NAV_ITEMS: NavItem[] = [
  { key: 'home', label: '首页', to: '/v2' },
  { key: 'projects', label: '项目', to: '/v2/projects' },
  { key: 'creation', label: '创作', to: '/v2/projects' },
  { key: 'assets', label: '资产库', to: '/v2/assets' },
  { key: 'config', label: '配置中心', to: '/v2/config' },
];

function isActive(pathname: string, item: NavItem) {
  if (item.key === 'home') return pathname === '/v2';
  return pathname.startsWith(item.to);
}

export default function V2Nav() {
  const navigate = useNavigate();
  const location = useLocation();

  const activeKey = useMemo<NavKey | null>(() => {
    for (const item of NAV_ITEMS) {
      if (isActive(location.pathname, item)) return item.key;
    }
    return null;
  }, [location.pathname]);

  return (
    <nav className="v2-nav">
      {NAV_ITEMS.map((item) => {
        const active = activeKey === item.key;
        return (
          <button
            key={item.key}
            className={`v2-nav__item ${active ? 'is-active' : ''}`}
            onClick={() => navigate(item.to)}
            type="button"
          >
            {item.label}
          </button>
        );
      })}
    </nav>
  );
}
