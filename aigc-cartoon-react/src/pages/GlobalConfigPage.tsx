import { UserOutlined, PlusOutlined } from '@ant-design/icons';
import '@/styles/global-config-page.css';

function GlobalConfigPage() {
  return (
    <div className="global-config-page">
      <header className="global-config-page__header">
        <div className="global-config-page__logo">Miioo</div>
        <div className="global-config-page__avatar">
          <UserOutlined />
        </div>
      </header>

      <aside className="global-config-page__nav">
        <div className="global-config-page__nav-item">
          <div className="global-config-page__nav-icon"></div>
          <span>全局设定</span>
        </div>
        <div className="global-config-page__nav-item global-config-page__nav-item--active">
          <div className="global-config-page__nav-icon"></div>
          <span>剧本</span>
        </div>
        <div className="global-config-page__nav-item">
          <div className="global-config-page__nav-icon"></div>
          <span>主体</span>
        </div>
        <div className="global-config-page__nav-item">
          <div className="global-config-page__nav-icon"></div>
          <span>分镜</span>
        </div>
        <div className="global-config-page__nav-item">
          <div className="global-config-page__nav-icon"></div>
          <span>剪辑成片</span>
        </div>
      </aside>

      <main className="global-config-page__content">
        <div className="global-config-page__title">
          <span>早上好，郝亮印</span>
        </div>

        <div className="global-config-page__options">
          <div className="global-config-page__option-card">
            <span className="global-config-page__option-text">Doubao-Seed-2.0-Pro</span>
            <span className="global-config-page__option-arrow">{'>'}</span>
          </div>
          <div className="global-config-page__option-card">
            <span className="global-config-page__option-text">集数：自动适应</span>
            <span className="global-config-page__option-arrow">{'>'}</span>
          </div>
        </div>

        <div className="global-config-page__input-area">
          <span className="global-config-page__input-placeholder">告诉导演你想拍什么或者直接上传剧本</span>
          <div className="global-config-page__upload-btn">
            <PlusOutlined />
          </div>
        </div>

        <button type="button" className="global-config-page__action-btn">
          <span>开始提取主体</span>
          <div className="global-config-page__action-icon"></div>
        </button>
      </main>
    </div>
  );
}

export default GlobalConfigPage;
