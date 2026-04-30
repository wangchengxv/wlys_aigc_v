import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import './BasicLayout.css';

const navItems = [
  { key: '/', label: '首页' },
  { key: '/projects', label: '项目' },
  { key: '/creation', label: '创作' },
];

function BasicLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const handleNavClick = (path: string) => {
    navigate(path);
  };

  const isActive = (path: string) => {
    if (path === '/' && (location.pathname === '/' || location.pathname === '')) return true;
    if (path === '/projects' && location.pathname === '/projects') return true;
    if (path === '/creation' && location.pathname.startsWith('/creation')) return true;
    return false;
  };

  return (
    <div className="basic-layout">
      <header className="basic-layout__header">
        <div className="basic-layout__logo">
          <span className="basic-layout__logo-text">AIGC</span>
          <div className="basic-layout__logo-icon"></div>
        </div>

        <nav className="basic-layout__nav">
          {navItems.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`basic-layout__nav-item ${isActive(item.key) ? 'basic-layout__nav-item--active' : ''}`}
              onClick={() => handleNavClick(item.key)}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="basic-layout__user">
          <div className="basic-layout__user-avatar"></div>
        </div>
      </header>

      <main className="basic-layout__content">
        <Outlet />
      </main>
    </div>
  );
}

export default BasicLayout;