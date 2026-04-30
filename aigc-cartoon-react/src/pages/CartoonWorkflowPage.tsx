import '@/styles/cartoon-workflow-page.css';
import { PlusOutlined } from '@ant-design/icons';

const tabItems = [
  { id: 'role', label: '角色', count: 3 },
  { id: 'scene', label: '场景', count: 3 },
  { id: 'prop', label: '道具', count: 3 },
];

const characterCards = [
  {
    id: 1,
    title: '大虎_1776655165821',
    subtitle: '第一集',
    status: '已完成',
    coverUrl: '',
  },
  {
    id: 2,
    title: '两只老虎的青枫奇遇',
    subtitle: '第一集',
    status: '生成中',
    coverUrl: '',
  },
  {
    id: 3,
    title: '',
    subtitle: '',
    status: '',
    coverUrl: '',
    isEmpty: true,
  },
  {
    id: 4,
    title: '',
    subtitle: '',
    status: '',
    coverUrl: '',
    isEmpty: true,
  },
];

function CartoonWorkflowPage() {
  return (
    <div className="cartoon-workflow-page">
      <header className="cartoon-workflow-page__header">
        <span className="cartoon-workflow-page__logo">logo</span>
        <div className="cartoon-workflow-page__avatar" />
      </header>

      <div className="cartoon-workflow-page__main">
        <aside className="cartoon-workflow-page__sidebar">
          <div className="cartoon-workflow-page__tabs">
            {tabItems.map((tab) => (
              <div key={tab.id} className="cartoon-workflow-page__tab">
                <span className="cartoon-workflow-page__tab-label">{tab.label}</span>
                <span className="cartoon-workflow-page__tab-count">{tab.count}</span>
              </div>
            ))}
          </div>

          <div className="cartoon-workflow-page__project">
            <div className="cartoon-workflow-page__folder-icon" />
            <span className="cartoon-workflow-page__project-title">两只老虎的青枫奇遇</span>
            <span className="cartoon-workflow-page__arrow">&gt;</span>
          </div>

          <div className="cartoon-workflow-page__subtitle">
            <div className="cartoon-workflow-page__subtitle-badge" />
            <span className="cartoon-workflow-page__subtitle-text">第一集</span>
          </div>
        </aside>

        <main className="cartoon-workflow-page__content">
          <div className="cartoon-workflow-page__cards">
            {characterCards.map((card) => (
              <div
                key={card.id}
                className={`cartoon-workflow-page__card ${
                  card.isEmpty ? 'cartoon-workflow-page__card--empty' : ''
                }`}
              >
                {card.isEmpty ? (
                  <>
                    <span className="cartoon-workflow-page__plus">+</span>
                    <div className="cartoon-workflow-page__card-empty-bar" />
                  </>
                ) : (
                  <>
                    <div className="cartoon-workflow-page__card-image">
                      {!card.coverUrl && (
                        <svg
                          className="cartoon-workflow-page__card-placeholder"
                          viewBox="0 0 60 53"
                          fill="none"
                        >
                          <path
                            d="M0 42L15 27L30 40L45 20L60 42H0Z"
                            fill="#666666"
                          />
                          <circle cx="22" cy="22" r="8" fill="#666666" />
                          <circle cx="40" cy="18" r="6" fill="#666666" />
                        </svg>
                      )}
                    </div>
                    <div className="cartoon-workflow-page__card-info">
                      <div className="cartoon-workflow-page__card-bars">
                        <div className="cartoon-workflow-page__card-bar" />
                        <div className="cartoon-workflow-page__card-bar" />
                        <div className="cartoon-workflow-page__card-bar cartoon-workflow-page__card-bar--gray" />
                      </div>
                      <div className="cartoon-workflow-page__card-title">{card.title}</div>
                      <div className="cartoon-workflow-page__card-actions">
                        <button type="button" className="cartoon-workflow-page__card-action">
                          下载
                        </button>
                        <button type="button" className="cartoon-workflow-page__card-action cartoon-workflow-page__card-action--delete">
                          删除
                        </button>
                      </div>
                    </div>
                    {card.status && (
                      <div className="cartoon-workflow-page__card-status">{card.status}</div>
                    )}
                  </>
                )}
              </div>
            ))}
          </div>

          <div className="cartoon-workflow-page__actions">
            <div className="cartoon-workflow-page__menu-btn cartoon-workflow-page__menu-btn--gray" />
            <div className="cartoon-workflow-page__menu-btn cartoon-workflow-page__menu-btn--dark" />
            <button type="button" className="cartoon-workflow-page__action-btn">
              批量生成角色
            </button>
            <button type="button" className="cartoon-workflow-page__action-btn">
              添加角色
            </button>
          </div>

          <button type="button" className="cartoon-workflow-page__start-btn">
            <span>开始智能分镜</span>
            <div className="cartoon-workflow-page__start-icon">
              <PlusOutlined />
            </div>
          </button>
        </main>
      </div>
    </div>
  );
}

export default CartoonWorkflowPage;