import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { SearchOutlined, CloseOutlined } from '@ant-design/icons';
import style1 from '@/assets/figma/cartoon-workflow-v2/v2-style-1.png';
import style2 from '@/assets/figma/cartoon-workflow-v2/v2-style-2.png';
import style3 from '@/assets/figma/cartoon-workflow-v2/v2-style-3.png';
import style4 from '@/assets/figma/cartoon-workflow-v2/v2-style-4.png';
import style5 from '@/assets/figma/cartoon-workflow-v2/v2-style-5.png';
import '@/styles/figma-main-page.css';

interface FigmaMainPageProps {
  defaultModalOpen?: boolean;
}

interface StyleItem {
  id: number;
  title: string;
  image: string;
}

const styleItems: StyleItem[] = [
  { id: 1, title: '动漫日韩', image: style1 },
  { id: 2, title: '3D-皮克斯卡通', image: style2 },
  { id: 3, title: '写实-真人', image: style3 },
  { id: 4, title: '动漫-Q版可爱', image: style4 },
  { id: 5, title: '风格化-像素风', image: style5 },
];

function FigmaMainPage({ defaultModalOpen = false }: FigmaMainPageProps) {
  const navigate = useNavigate();
  const [isModalOpen, setIsModalOpen] = useState(defaultModalOpen);
  const [selectedStyle, setSelectedStyle] = useState<number>(1);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [projectName, setProjectName] = useState('霓虹下的星光');
  const [projectDesc, setProjectDesc] = useState(
    '都市夜景中的职场情感漫剧，聚焦男女主在工作压力与成长中的双向奔赴。',
  );

  const openModal = () => {
    setIsModalOpen(true);
    navigate('/modal');
  };

  const closeModal = () => {
    setIsModalOpen(false);
    navigate('/');
  };

  const selectedStyleItem = styleItems.find((item) => item.id === selectedStyle);

  return (
    <div className="figma-page">
      <div className="figma-page__container">
        <header className="figma-page__header">
          <div className="figma-page__search">
            <input
              value={searchKeyword}
              onChange={(event) => setSearchKeyword(event.target.value)}
              placeholder="搜索项目..."
              aria-label="搜索项目"
            />
            <button type="button" aria-label="搜索">
              <SearchOutlined />
            </button>
          </div>
          <button type="button" className="figma-page__create-btn" onClick={openModal}>
            新建项目
          </button>
        </header>

        <section className="figma-page__project-card">
          <div className="figma-page__project-cover" />
          <div className="figma-page__project-info">
            <h1>{projectName}</h1>
            <p>{projectDesc}</p>
          </div>
        </section>

        <section className="figma-page__style-section">
          <h2>项目风格</h2>
          <div className="figma-page__style-grid">
            {styleItems.map((item) => (
              <button
                type="button"
                key={item.id}
                className={`figma-page__style-card ${
                  item.id === selectedStyle ? 'figma-page__style-card--active' : ''
                }`}
                onClick={() => setSelectedStyle(item.id)}
              >
                <img src={item.image} alt={item.title} />
              </button>
            ))}
          </div>
        </section>
      </div>

      {isModalOpen ? (
        <div className="figma-modal__mask" role="presentation" onClick={closeModal}>
          <section className="figma-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <button type="button" className="figma-modal__close" onClick={closeModal} aria-label="关闭">
              <CloseOutlined />
            </button>
            <h3>新建漫剧项目</h3>
            <label htmlFor="project-name-input">项目名称</label>
            <input
              id="project-name-input"
              value={projectName}
              onChange={(event) => setProjectName(event.target.value)}
              placeholder="请输入项目名称"
            />
            <label htmlFor="project-desc-input">项目简介</label>
            <textarea
              id="project-desc-input"
              value={projectDesc}
              onChange={(event) => setProjectDesc(event.target.value)}
              placeholder="请输入项目简介"
            />
            <label>风格选择</label>
            <div className="figma-modal__style-picker">
              {styleItems.map((item) => (
                <button
                  type="button"
                  key={item.id}
                  className={`figma-modal__style-option ${
                    item.id === selectedStyle ? 'figma-modal__style-option--active' : ''
                  }`}
                  onClick={() => setSelectedStyle(item.id)}
                >
                  {item.title}
                </button>
              ))}
            </div>
            {selectedStyleItem ? (
              <div className="figma-modal__preview">
                <img src={selectedStyleItem.image} alt={selectedStyleItem.title} />
                <p>{selectedStyleItem.title}</p>
              </div>
            ) : null}
            <div className="figma-modal__actions">
              <button type="button" className="figma-modal__btn figma-modal__btn--ghost" onClick={closeModal}>
                取消
              </button>
              <button type="button" className="figma-modal__btn figma-modal__btn--primary">
                创建项目
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}

export default FigmaMainPage;
