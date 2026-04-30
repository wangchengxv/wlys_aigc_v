import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message } from 'antd';
import { saveOrUpdateScript } from '@/api/script';
import { updateProjectConfig } from '@/api/config';
import '@/styles/creation-page.css';

const workflowSteps = [
  { id: 'global', label: '全局设定', path: '/creation/global' },
  { id: 'script', label: '剧本', path: '/creation' },
  { id: 'subject', label: '主体', path: '/creation' },
  { id: 'storyboard', label: '分镜', path: '/creation' },
  { id: 'editing', label: '剪辑成片', path: '/creation' },
];

const stepIcons: { [key: string]: string } = {
  global: '⚙',
  script: '✎',
  subject: '◉',
  storyboard: '▦',
  editing: '◈',
};

interface BatchConfig {
  model: string;
  ratio: string;
  quality: string;
  method: string;
}

const batchOptions = {
  model: ['Doubao-Seed-2.0-Pro', 'GPT-5.4', 'GLM-5.1'],
  ratio: ['16:9', '9:16', '1:1'],
  quality: ['1K', '2K', '4K'],
  method: ['多视图', '单视图', '连续帧'],
};

function CreationPage() {
  const navigate = useNavigate();
  const [scriptInput, setScriptInput] = useState('');
  const [episodes, setEpisodes] = useState('自动适应');
  const [showBatchModal, setShowBatchModal] = useState(false);
  const [batchConfig, setBatchConfig] = useState<BatchConfig>({
    model: 'Doubao-Seed-2.0-Pro',
    ratio: '16:9',
    quality: '1K',
    method: '多视图',
  });
  const [saving, setSaving] = useState(false);
  const projectId = 1;

  const handleWorkflowClick = (step: typeof workflowSteps[0]) => {
    if (step.path) {
      navigate(step.path);
    }
  };

  const handleSaveScript = async () => {
    if (!scriptInput.trim()) {
      message.warning('请输入剧本内容');
      return;
    }

    setSaving(true);
    try {
      await saveOrUpdateScript(projectId, {
        title: '剧本',
        content: scriptInput,
      });
      message.success('剧本保存成功');
    } catch {
      message.error('保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  const handleBatchGenerate = async () => {
    setSaving(true);
    try {
      const episodeCount = episodes === '自动适应' ? 0 : parseInt(episodes) || 1;
      await updateProjectConfig(projectId, {
        batchModel: batchConfig.model,
        batchRatio: batchConfig.ratio,
        batchQuality: batchConfig.quality,
        batchMethod: batchConfig.method,
        episodeCount,
      });
      message.success('批量生成配置已保存');
      setShowBatchModal(false);
    } catch {
      message.error('配置保存失败');
    } finally {
      setSaving(false);
    }
  };

  const updateBatchConfig = (key: keyof BatchConfig) => {
    const options = batchOptions[key];
    const currentIndex = options.indexOf(batchConfig[key]);
    const nextIndex = (currentIndex + 1) % options.length;
    setBatchConfig((prev) => ({ ...prev, [key]: options[nextIndex] }));
  };

  return (
    <div className="creation-page">
      <div className="workflow-container">
        <div className="workflow-progress-bar">
          <div className="workflow-progress-fill"></div>
        </div>
        <div className="workflow-steps">
          {workflowSteps.map((step, index) => (
            <div key={step.id} className="workflow-step-wrapper">
              <button
                type="button"
                className={`workflow-step ${
                  index === 1 ? 'workflow-step--active' : ''
                }`}
                onClick={() => handleWorkflowClick(step)}
              >
                <div className="workflow-step__glow"></div>
                <span className="workflow-step__number">{index + 1}</span>
                <span className="workflow-step__icon">{stepIcons[step.id]}</span>
                <span className="workflow-step__label">{step.label}</span>
              </button>
              {index < workflowSteps.length - 1 && (
                <div className="workflow-connector">
                  <div className="workflow-connector__line"></div>
                  <div className="workflow-connector__arrow">›</div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="creation-page__greeting">
        <h1>欢迎使用</h1>
      </div>

      <div className="creation-page__input-area">
        <div className="creation-page__input-box">
          <textarea
            value={scriptInput}
            onChange={(e) => setScriptInput(e.target.value)}
            placeholder="告诉导演你想拍什么或者直接上传剧本"
            className="creation-page__textarea"
          />
          <button type="button" className="creation-page__upload-btn">
            <span>+</span>
          </button>
        </div>

        <div className="creation-page__config-row">
          <button
            type="button"
            className="creation-page__config-btn"
            onClick={() => updateBatchConfig('model')}
          >
            <span>{batchConfig.model}</span>
            <span className="creation-page__config-arrow">&gt;</span>
          </button>
          <button
            type="button"
            className="creation-page__config-btn"
            onClick={() => {
              const newEpisodes = episodes === '自动适应' ? '1集' : episodes === '1集' ? '2集' : '自动适应';
              setEpisodes(newEpisodes);
            }}
          >
            <span>集数：{episodes}</span>
            <span className="creation-page__config-arrow">&gt;</span>
          </button>
        </div>

        <button
          type="button"
          className="creation-page__start-btn"
          onClick={handleSaveScript}
          disabled={saving || !scriptInput.trim()}
        >
          <span>{saving ? '保存中...' : '开始提取主体'}</span>
          <div className="creation-page__start-icon"></div>
        </button>
      </div>

      <button
        type="button"
        className="creation-page__batch-btn"
        onClick={() => setShowBatchModal(true)}
      >
        批量生成
      </button>

      {showBatchModal && (
        <div className="modal-overlay" onClick={() => setShowBatchModal(false)}>
          <div className="batch-generate-modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="batch-generate-modal__title">批量生成</h2>
            <div className="batch-generate-modal__form">
              <div className="batch-generate-modal__field">
                <label>选择模型</label>
                <div
                  className="batch-generate-modal__select"
                  onClick={() => updateBatchConfig('model')}
                >
                  <span>{batchConfig.model}</span>
                  <span>&gt;</span>
                </div>
              </div>
              <div className="batch-generate-modal__field">
                <label>比例</label>
                <div
                  className="batch-generate-modal__select"
                  onClick={() => updateBatchConfig('ratio')}
                >
                  <span>{batchConfig.ratio}</span>
                  <span>&gt;</span>
                </div>
              </div>
              <div className="batch-generate-modal__field">
                <label>质量</label>
                <div
                  className="batch-generate-modal__select"
                  onClick={() => updateBatchConfig('quality')}
                >
                  <span>{batchConfig.quality}</span>
                  <span>&gt;</span>
                </div>
              </div>
              <div className="batch-generate-modal__field">
                <label>生成方式</label>
                <div
                  className="batch-generate-modal__select"
                  onClick={() => updateBatchConfig('method')}
                >
                  <span>{batchConfig.method}</span>
                  <span>&gt;</span>
                </div>
              </div>
            </div>
            <div className="batch-generate-modal__actions">
              <button
                type="button"
                className="batch-generate-modal__btn"
                onClick={() => setShowBatchModal(false)}
              >
                取消
              </button>
              <button
                type="button"
                className="batch-generate-modal__btn"
                onClick={handleBatchGenerate}
                disabled={saving}
              >
                {saving ? '保存中...' : '确定'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default CreationPage;
