import { useState } from 'react';
import { UserOutlined, PlayCircleOutlined, FileTextOutlined, CameraOutlined, HeartOutlined, ThunderboltOutlined } from '@ant-design/icons';
import '@/styles/workflow-page.css';

interface StepItem {
  id: number;
  icon: React.ReactNode;
  label: string;
}

const stepItems: StepItem[] = [
  { id: 1, icon: <FileTextOutlined />, label: '剧本' },
  { id: 2, icon: <CameraOutlined />, label: '分镜' },
  { id: 3, icon: <HeartOutlined />, label: '角色' },
  { id: 4, icon: <ThunderboltOutlined />, label: '渲染' },
];

function WorkflowPage() {
  const [activeStep, setActiveStep] = useState(1);

  return (
    <div className="workflow-page">
      <header className="workflow-page__header">
        <div className="workflow-page__logo">Miioo</div>
        <div className="workflow-page__avatar">
          <UserOutlined />
        </div>
      </header>

      <aside className="workflow-page__steps">
        {stepItems.map((item) => (
          <div
            key={item.id}
            className={`workflow-page__step-item ${activeStep === item.id ? 'workflow-page__step-item--active' : ''}`}
            onClick={() => setActiveStep(item.id)}
          >
            <div className="workflow-page__step-icon">{item.icon}</div>
            <span className="workflow-page__step-label">{item.label}</span>
          </div>
        ))}
      </aside>

      <footer className="workflow-page__footer">
        <button type="button" className="workflow-page__action-btn">
          <PlayCircleOutlined />
          <span>开始创作</span>
        </button>
      </footer>
    </div>
  );
}

export default WorkflowPage;
