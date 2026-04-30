import { useMemo, useState } from 'react';
import type { ChangeEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { SearchOutlined, CloseOutlined } from '@ant-design/icons';
import { message } from 'antd';
import { createProject, uploadProjectCover } from '@/api/project';
import { aspectRatioOptions, styleOptions } from '@/types/project';
import type { VisualStyleMode } from '@/types/project';
import {
  getEffectiveCreationSettings,
  setProjectCreationOverride,
} from '@/stores/globalCreationSettingsStore';
import '@/styles/home-page.css';

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_COVER_SIZE = 5 * 1024 * 1024;
const DRAFT_OVERRIDE_KEY = 'home:new-project-draft';

function HomePage() {
  const navigate = useNavigate();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [projectName, setProjectName] = useState('');
  const [projectDesc, setProjectDesc] = useState('');
  const [selectedAspectRatio, setSelectedAspectRatio] = useState('16:9');
  const [selectedStyleId, setSelectedStyleId] = useState(styleOptions[0]?.id || 1);
  const [uploadLoading, setUploadLoading] = useState(false);
  const [uploadPreviewUrl, setUploadPreviewUrl] = useState('');
  const [coverImage, setCoverImage] = useState('');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<{
    projectName?: string;
    aspectRatio?: string;
    style?: string;
    coverImage?: string;
  }>({});

  const styleItems = useMemo(
    () =>
      styleOptions.map((item) => ({
        ...item,
        image: '',
      })),
    [],
  );

  const selectedStyleItem = styleItems.find((item) => item.id === selectedStyleId) || styleItems[0];

  const openModal = () => {
    const effective = getEffectiveCreationSettings(DRAFT_OVERRIDE_KEY);
    const matchedStyle =
      styleItems.find((item) => item.styleKey === effective.styleKey) ||
      styleItems.find((item) => item.styleTemplateId === effective.styleTemplateId) ||
      styleItems[0];
    setSelectedAspectRatio(effective.aspectRatio || '16:9');
    setSelectedStyleId(matchedStyle?.id || styleItems[0]?.id || 1);
    setErrors({});
    setIsModalOpen(true);
  };

  const resetUploadPreview = () => {
    if (uploadPreviewUrl) {
      URL.revokeObjectURL(uploadPreviewUrl);
    }
    setUploadPreviewUrl('');
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setProjectName('');
    setProjectDesc('');
    setSelectedAspectRatio('16:9');
    setSelectedStyleId(styleOptions[0]?.id || 1);
    setCoverImage('');
    setErrors({});
    setUploadLoading(false);
    resetUploadPreview();
  };

  const validateForm = () => {
    const nextErrors: typeof errors = {};
    if (!projectName.trim()) {
      nextErrors.projectName = '请输入项目名称';
    }
    if (!selectedAspectRatio) {
      nextErrors.aspectRatio = '请选择画面比例';
    }
    if (!selectedStyleItem) {
      nextErrors.style = '请选择视觉风格';
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleCoverFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      setErrors((prev) => ({ ...prev, coverImage: '仅支持 JPG、PNG、WEBP 格式图片' }));
      message.error('仅支持 JPG、PNG、WEBP 格式图片');
      event.target.value = '';
      return;
    }
    if (file.size > MAX_COVER_SIZE) {
      setErrors((prev) => ({ ...prev, coverImage: '封面图片大小不能超过 5MB' }));
      message.error('封面图片大小不能超过 5MB');
      event.target.value = '';
      return;
    }
    setUploadLoading(true);
    try {
      const uploaded = await uploadProjectCover(file);
      resetUploadPreview();
      setUploadPreviewUrl(URL.createObjectURL(file));
      setCoverImage(uploaded.data.url);
      setErrors((prev) => ({ ...prev, coverImage: undefined }));
      message.success('封面上传成功');
    } catch {
      message.error('封面上传失败，请重试');
    } finally {
      setUploadLoading(false);
      event.target.value = '';
    }
  };

  const handleCreateProject = async () => {
    if (!validateForm()) return;
    if (uploadLoading) {
      message.warning('封面上传中，请稍后提交');
      return;
    }
    if (!selectedStyleItem) {
      message.error('请选择视觉风格');
      return;
    }
    const effective = getEffectiveCreationSettings(DRAFT_OVERRIDE_KEY);
    setLoading(true);
    try {
      const visualStyleMode: VisualStyleMode = 'preset';
      const visualStyleLongTextMode = effective.visualStyleLongTextMode ?? false;
      const visualStylePrompt = selectedStyleItem.visualStylePrompt || effective.visualStylePrompt || '';
      const response = await createProject({
        name: projectName.trim(),
        description: projectDesc.trim(),
        style: selectedStyleItem.value,
        styleKey: selectedStyleItem.styleKey,
        visualStylePrompt,
        styleTemplateId: selectedStyleItem.styleTemplateId,
        visualStyleMode,
        visualStyleLongTextMode,
        customStyleText: visualStyleLongTextMode ? visualStylePrompt : '',
        aspectRatio: selectedAspectRatio,
        coverImage,
      });
      const overridePayload = {
        aspectRatio: selectedAspectRatio,
        styleKey: selectedStyleItem.styleKey,
        styleTemplateId: selectedStyleItem.styleTemplateId,
        visualStylePrompt,
        visualStyleMode,
        visualStyleLongTextMode,
      };
      setProjectCreationOverride(DRAFT_OVERRIDE_KEY, overridePayload);
      const createdProjectId = response.data?.id;
      if (createdProjectId != null) {
        setProjectCreationOverride(`project:${createdProjectId}`, overridePayload);
      }
      message.success('项目创建成功');
      closeModal();
      navigate('/projects');
    } catch {
      message.error('创建失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="home-page">
      <div className="home-page__container">
        <header className="home-page__header">
          <div className="home-page__search">
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
          <button type="button" className="home-page__create-btn" onClick={openModal}>
            新建项目
          </button>
        </header>

        <section className="home-page__project-card">
          <div className="home-page__project-cover" />
          <div className="home-page__project-info">
            <h1>霓虹下的星光</h1>
            <p>都市夜景中的职场情感漫剧，聚焦男女主在工作压力与成长中的双向奔赴。</p>
          </div>
        </section>

        <section className="home-page__style-section">
          <h2>项目风格</h2>
          <div className="home-page__style-grid">
            {styleItems.map((item) => (
              <button
                type="button"
                key={item.id}
                className={`home-page__style-card ${
                  item.id === selectedStyleId ? 'home-page__style-card--active' : ''
                }`}
                onClick={() => setSelectedStyleId(item.id)}
              >
                <div className="home-page__style-card-icon" />
              </button>
            ))}
          </div>
        </section>
      </div>

      {isModalOpen ? (
        <div className="home-modal__mask" role="presentation" onClick={closeModal}>
          <section className="home-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <button type="button" className="home-modal__close" onClick={closeModal} aria-label="关闭">
              <CloseOutlined />
            </button>
            <h3>新建项目</h3>

            <div className="home-modal__field">
              <label htmlFor="project-name-input">项目名称 *</label>
              <input
                id="project-name-input"
                value={projectName}
                onChange={(event) => {
                  setProjectName(event.target.value);
                  setErrors((prev) => ({ ...prev, projectName: undefined }));
                }}
                placeholder="请输入项目名称"
                maxLength={50}
              />
              {errors.projectName ? <p className="home-modal__error">{errors.projectName}</p> : null}
            </div>

            <div className="home-modal__field">
              <label htmlFor="project-desc-input">项目简介</label>
              <textarea
                id="project-desc-input"
                value={projectDesc}
                onChange={(event) => setProjectDesc(event.target.value)}
                placeholder="请输入项目简介"
                maxLength={500}
              />
            </div>

            <div className="home-modal__field">
              <label>选择画面比例 *</label>
              <div className="home-modal__aspect-ratios">
                {aspectRatioOptions.map((ratio) => (
                  <button
                    key={ratio.value}
                    type="button"
                    className={`home-modal__aspect-btn ${
                      selectedAspectRatio === ratio.value ? 'home-modal__aspect-btn--active' : ''
                    }`}
                    onClick={() => {
                      setSelectedAspectRatio(ratio.value);
                      setErrors((prev) => ({ ...prev, aspectRatio: undefined }));
                    }}
                  >
                    {ratio.label}
                  </button>
                ))}
              </div>
              {errors.aspectRatio ? <p className="home-modal__error">{errors.aspectRatio}</p> : null}
            </div>

            <div className="home-modal__field">
              <label>视觉风格 *</label>
              <div className="home-modal__style-grid">
                {styleItems.map((item) => (
                  <button
                    type="button"
                    key={item.id}
                    className={`home-modal__style-card ${
                      item.id === selectedStyleId ? 'home-modal__style-card--active' : ''
                    }`}
                    onClick={() => {
                      setSelectedStyleId(item.id);
                      setErrors((prev) => ({ ...prev, style: undefined }));
                    }}
                  >
                    <div className="home-modal__style-card-icon" />
                    <span>{item.title}</span>
                  </button>
                ))}
              </div>
              {errors.style ? <p className="home-modal__error">{errors.style}</p> : null}
            </div>

            <div className="home-modal__field">
              <label>提示词模板（可留占位符）</label>
              <pre className="home-modal__prompt-preview">
                {selectedStyleItem?.visualStylePrompt || '请选择视觉风格后查看提示词模板'}
              </pre>
            </div>

            <div className="home-modal__field">
              <label htmlFor="home-modal-cover-upload">项目封面</label>
              <label
                className={`home-modal__upload ${uploadLoading ? 'home-modal__upload--loading' : ''}`}
                htmlFor="home-modal-cover-upload"
              >
                {uploadPreviewUrl ? (
                  <>
                    <img src={uploadPreviewUrl} alt="封面预览" className="home-modal__upload-preview" />
                    <span className="home-modal__upload-reupload">重新上传</span>
                  </>
                ) : (
                  <>
                    <span className="home-modal__upload-plus">+</span>
                    <span>{uploadLoading ? '上传中...' : '上传封面（可选）'}</span>
                  </>
                )}
              </label>
              <input
                id="home-modal-cover-upload"
                type="file"
                className="home-modal__upload-input"
                accept="image/jpeg,image/png,image/webp"
                disabled={loading || uploadLoading}
                onChange={handleCoverFileChange}
              />
              {errors.coverImage ? <p className="home-modal__error">{errors.coverImage}</p> : null}
            </div>

            <div className="home-modal__actions">
              <button
                type="button"
                className="home-modal__btn home-modal__btn--ghost"
                onClick={closeModal}
                disabled={loading}
              >
                取消
              </button>
              <button
                type="button"
                className="home-modal__btn home-modal__btn--primary"
                onClick={handleCreateProject}
                disabled={loading || uploadLoading}
              >
                {loading ? '创建中...' : uploadLoading ? '上传中...' : '确定创建'}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}

export default HomePage;
