import { useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import landingBg from '@/assets/figma/cartoon-workflow-v2/v2-landing-bg.png';
import '@/styles/v2-landing-page.css';

type NavItem = {
  key: string;
  label: string;
  to: string;
};

export default function V2LandingPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = useMemo<NavItem[]>(
    () => [
      { key: 'home', label: '首页', to: '/v2' },
      { key: 'projects', label: '项目', to: '/v2/projects' },
      { key: 'creation', label: '创作', to: '/v2/projects' },
      { key: 'assets', label: '资产库', to: '/v2/projects' },
      { key: 'config', label: '配置中心', to: '/v2/projects' },
    ],
    [],
  );

  return (
    <div className="v2-landing" style={{ backgroundImage: `url(${landingBg})` }}>
      <div className="v2-landing__brand">Miioo</div>
      <div className="v2-landing__avatar" />

      <nav className="v2-landing__nav">
        {navItems.map((item) => {
          const active = item.to === '/v2' ? location.pathname === '/v2' : location.pathname.startsWith(item.to);
          return (
            <button
              key={item.key}
              className={`v2-landing__navItem ${active ? 'is-active' : ''}`}
              onClick={() => navigate(item.to)}
              type="button"
            >
              {item.label}
            </button>
          );
        })}
      </nav>

      <button className="v2-landing__cta" type="button" onClick={() => navigate('/v2/projects')}>
        开始创作
      </button>
    </div>
  );
}
