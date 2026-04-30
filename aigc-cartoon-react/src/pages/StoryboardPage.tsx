import { useState } from 'react';
import { RightOutlined } from '@ant-design/icons';
import '@/styles/storyboard-page.css';

interface StoryboardItem {
  id: number;
  sceneDescription: string;
  lighting: string;
  ambientSound: string;
  narration: string;
  mainReference: string;
  storyboardImage: string;
  storyboardVideo: string;
}

const storyboardData: StoryboardItem[] = [
  {
    id: 2,
    sceneDescription: '清晨的山林空地，淡蓝色的晨雾浮在草尖之上。镜头从树林边缘缓缓向前推进，穿过薄雾，视觉中心逐渐锁定在空地中央那块泛着油亮红光的大块鲜肉上。两侧树影在微风中轻轻摇晃，营造出静谧而紧张的氛围。',
    lighting: '全局柔光，阳光穿过树叶缝隙',
    ambientSound: '微微的风声',
    narration: '无',
    mainReference: '',
    storyboardImage: '',
    storyboardVideo: '',
  },
  {
    id: 3,
    sceneDescription: '清晨的山林空地，淡蓝色的晨雾浮在草尖之上。镜头从树林边缘缓缓向前推进，穿过薄雾，视觉中心逐渐锁定在空地中央那块泛着油亮红光的大块鲜肉上。两侧树影在微风中轻轻摇晃，营造出静谧而紧张的氛围。',
    lighting: '全局柔光，阳光穿过树叶缝隙',
    ambientSound: '微微的风声',
    narration: '无',
    mainReference: '',
    storyboardImage: '',
    storyboardVideo: '',
  },
  {
    id: 1,
    sceneDescription: '清晨的山林空地，淡蓝色的晨雾浮在草尖之上。镜头从树林边缘缓缓向前推进，穿过薄雾，视觉中心逐渐锁定在空地中央那块泛着油亮红光的大块鲜肉上。两侧树影在微风中轻轻摇晃，营造出静谧而紧张的氛围。',
    lighting: '全局柔光，阳光穿过树叶缝隙',
    ambientSound: '微微的风声',
    narration: '无',
    mainReference: '',
    storyboardImage: '',
    storyboardVideo: '',
  },
];

const stepItems = [
  { id: 1, label: '全局设定' },
  { id: 2, label: '剧本' },
  { id: 3, label: '主体' },
  { id: 4, label: '分镜' },
  { id: 5, label: '剪辑成片' },
];

function StoryboardPage() {
  const [activeStep, setActiveStep] = useState(4);
  const sortedStoryboard = [...storyboardData].sort((a, b) => a.id - b.id);

  return (
    <div className="storyboard-page">
      <header className="storyboard-page__header">
        <div className="storyboard-page__logo">logo</div>
        <div className="storyboard-page__avatar" />
      </header>

      <div className="storyboard-page__layout">
        <aside className="storyboard-page__steps">
          {stepItems.map((item) => (
            <div
              key={item.id}
              className={`storyboard-page__step-item ${
                activeStep === item.id ? 'storyboard-page__step-item--active' : ''
              }`}
              onClick={() => setActiveStep(item.id)}
            >
              <div className="storyboard-page__step-bar" />
              <span>{item.label}</span>
            </div>
          ))}
        </aside>

        <main className="storyboard-page__content">
          <div className="storyboard-page__top-bar">
            <div className="storyboard-page__episode-count">
              <span className="storyboard-page__episode-label">分镜</span>
              <span className="storyboard-page__episode-number">30</span>
            </div>

            <div className="storyboard-page__actions">
              <button type="button" className="storyboard-page__action-btn storyboard-page__action-btn--secondary">
                批量生成分镜图
              </button>
              <button type="button" className="storyboard-page__action-btn storyboard-page__action-btn--secondary">
                批量生成分镜视频
              </button>
              <button type="button" className="storyboard-page__action-btn storyboard-page__action-btn--primary">
                <span>开始剪辑</span>
                <svg className="storyboard-page__action-icon" viewBox="0 0 24 24" fill="none">
                  <path d="M5 12h14M12 5l7 7-7 7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </button>
            </div>
          </div>

          <div className="storyboard-page__breadcrumb">
            <div className="storyboard-page__project-icon">
              <svg viewBox="0 0 24 24" fill="none">
                <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="2" />
                <path d="M9 9h6M9 12h6M9 15h4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </div>
            <span className="storyboard-page__project-name">两只老虎的青枫奇遇</span>
            <RightOutlined className="storyboard-page__breadcrumb-arrow" />
            <div className="storyboard-page__episode-tag">
              <span>第一集</span>
            </div>
          </div>

          <div className="storyboard-page__cards">
            {sortedStoryboard.map((item) => (
              <div key={item.id} className="storyboard-page__card">
                <div className="storyboard-page__card-header">
                  <div className="storyboard-page__card-number">{item.id}</div>
                  <div className="storyboard-page__card-preview">
                    <div className="storyboard-page__card-placeholder">
                      <svg viewBox="0 0 24 24" fill="none">
                        <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="1.5" />
                        <circle cx="8.5" cy="8.5" r="1.5" fill="currentColor" />
                        <path d="M21 15l-5-5L5 21" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                      </svg>
                    </div>
                    <div className="storyboard-page__card-description">
                      <div className="storyboard-page__card-description-text">
                        {item.sceneDescription}
                      </div>
                      <div className="storyboard-page__card-preview-img">
                        <svg viewBox="0 0 24 24" fill="none">
                          <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="1.5" />
                          <circle cx="8.5" cy="8.5" r="1.5" fill="currentColor" />
                          <path d="M21 15l-5-5L5 21" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                        </svg>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="storyboard-page__card-divider" />

                <div className="storyboard-page__card-field">
                  <span className="storyboard-page__card-field-label">光影</span>
                  <span className="storyboard-page__card-field-value">{item.lighting}</span>
                </div>

                <div className="storyboard-page__card-divider" />

                <div className="storyboard-page__card-field">
                  <span className="storyboard-page__card-field-label">环境音</span>
                  <span className="storyboard-page__card-field-value">{item.ambientSound}</span>
                </div>

                <div className="storyboard-page__card-divider" />

                <div className="storyboard-page__card-field">
                  <span className="storyboard-page__card-field-label">旁白配音</span>
                  <span className="storyboard-page__card-field-value">{item.narration}</span>
                </div>

                <div className="storyboard-page__card-divider" />

                <div className="storyboard-page__card-field">
                  <span className="storyboard-page__card-field-label">主体参考</span>
                  <div className="storyboard-page__card-image-placeholder">
                    <svg viewBox="0 0 24 24" fill="none">
                      <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="1.5" />
                      <circle cx="8.5" cy="8.5" r="1.5" fill="currentColor" />
                      <path d="M21 15l-5-5L5 21" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                    </svg>
                  </div>
                </div>

                <div className="storyboard-page__card-divider" />

                <div className="storyboard-page__card-field">
                  <span className="storyboard-page__card-field-label">分镜图</span>
                  <div className="storyboard-page__card-image-placeholder">
                    <svg viewBox="0 0 24 24" fill="none">
                      <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="1.5" />
                      <circle cx="8.5" cy="8.5" r="1.5" fill="currentColor" />
                      <path d="M21 15l-5-5L5 21" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                    </svg>
                  </div>
                </div>

                <div className="storyboard-page__card-divider" />

                <div className="storyboard-page__card-field">
                  <span className="storyboard-page__card-field-label">分镜视频</span>
                  <div className="storyboard-page__card-image-placeholder">
                    <svg viewBox="0 0 24 24" fill="none">
                      <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="1.5" />
                      <path d="M10 8l6 4-6 4V8z" fill="currentColor" />
                    </svg>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </main>
      </div>
    </div>
  );
}

export default StoryboardPage;
