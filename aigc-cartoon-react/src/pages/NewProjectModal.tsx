import { useState } from 'react';
import { UploadOutlined } from '@ant-design/icons';
import '@/styles/new-project-modal.css';

interface NewProjectModalProps {
  isOpen?: boolean;
  onClose?: () => void;
  onConfirm?: (data: ProjectFormData) => void;
}

interface ProjectFormData {
  name: string;
  description: string;
  aspectRatio: '16:9' | '9:16';
  visualStyle: string;
  cover?: File;
}

const aspectRatios = [
  { value: '16:9' as const, label: '16:9' },
  { value: '9:16' as const, label: '9：16' },
];

const visualStyles = [
  { id: 'style-1', image: '' },
  { id: 'style-2', image: '' },
  { id: 'style-3', image: '' },
  { id: 'style-4', image: '' },
  { id: 'style-5', image: '' },
];

function NewProjectModal({ isOpen = true, onClose, onConfirm }: NewProjectModalProps) {
  const [formData, setFormData] = useState<ProjectFormData>({
    name: '',
    description: '',
    aspectRatio: '16:9',
    visualStyle: '',
  });

  const handleInputChange = (field: keyof ProjectFormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleAspectRatioChange = (value: '16:9' | '9:16') => {
    setFormData(prev => ({ ...prev, aspectRatio: value }));
  };

  const handleStyleSelect = (styleId: string) => {
    setFormData(prev => ({ ...prev, visualStyle: styleId }));
  };

  const handleConfirm = () => {
    if (onConfirm) {
      onConfirm(formData);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="new-project-modal">
      <div className="new-project-modal__overlay"></div>
      <div className="new-project-modal__container">
        <div className="new-project-modal__header">
          <h1 className="new-project-modal__title">新建项目</h1>
        </div>

        <div className="new-project-modal__content">
          <div className="new-project-modal__field">
            <div className="new-project-modal__field-row">
              <span className="new-project-modal__label">项目名称*</span>
            </div>
            <input
              type="text"
              className="new-project-modal__input"
              placeholder="请输入项目名称"
              value={formData.name}
              onChange={(e) => handleInputChange('name', e.target.value)}
            />
          </div>

          <div className="new-project-modal__field">
            <div className="new-project-modal__field-row">
              <span className="new-project-modal__label">项目描述</span>
            </div>
            <input
              type="text"
              className="new-project-modal__input"
              placeholder="请输入项目描述"
              value={formData.description}
              onChange={(e) => handleInputChange('description', e.target.value)}
            />
          </div>

          <div className="new-project-modal__field-row new-project-modal__aspect-ratio-section">
            <span className="new-project-modal__label">选择画面比例</span>
            <div className="new-project-modal__aspect-ratio-options">
              {aspectRatios.map((ratio) => (
                <div
                  key={ratio.value}
                  className="new-project-modal__aspect-ratio-option"
                  onClick={() => handleAspectRatioChange(ratio.value)}
                >
                  <div className={`new-project-modal__aspect-ratio-radio ${formData.aspectRatio === ratio.value ? 'new-project-modal__aspect-ratio-radio--active' : ''}`}>
                    {formData.aspectRatio === ratio.value && <div className="new-project-modal__aspect-ratio-dot"></div>}
                  </div>
                  <span className="new-project-modal__aspect-ratio-text">{ratio.label}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="new-project-modal__field-row new-project-modal__visual-style-section">
            <span className="new-project-modal__label">视觉风格</span>
            <div className="new-project-modal__visual-style-grid">
              {visualStyles.map((style) => (
                <div
                  key={style.id}
                  className={`new-project-modal__visual-style-item ${formData.visualStyle === style.id ? 'new-project-modal__visual-style-item--selected' : ''}`}
                  onClick={() => handleStyleSelect(style.id)}
                >
                  <img src={style.image} alt={style.id} />
                </div>
              ))}
            </div>
          </div>

          <div className="new-project-modal__field">
            <div className="new-project-modal__field-row">
              <span className="new-project-modal__label">项目封面</span>
            </div>
            <div className="new-project-modal__upload-area">
              <UploadOutlined />
              <span>上传</span>
            </div>
          </div>
        </div>

        <div className="new-project-modal__footer">
          <button type="button" className="new-project-modal__btn new-project-modal__btn--confirm" onClick={handleConfirm}>
            确定
          </button>
          <button type="button" className="new-project-modal__btn new-project-modal__btn--cancel" onClick={onClose}>
            取消
          </button>
        </div>
      </div>
    </div>
  );
}

export default NewProjectModal;
