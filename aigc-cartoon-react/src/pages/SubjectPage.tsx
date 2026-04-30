import { useState, useEffect } from 'react';
import { UserOutlined } from '@ant-design/icons';
import { message } from 'antd';
import {
  getCharacters,
  createCharacter,
  updateCharacter,
  deleteCharacter,
  uploadCharacterImage,
  type Character,
} from '@/api/character';
import '@/styles/global.css';
import '@/styles/subject-page.css';

const stepItems = [
  { id: 1, label: '全局设定' },
  { id: 2, label: '剧本' },
  { id: 3, label: '主体' },
  { id: 4, label: '分镜' },
  { id: 5, label: '剪辑成片' },
];

interface CharacterFormData {
  name: string;
  description: string;
  voice: string;
}

const defaultCharacterForm: CharacterFormData = {
  name: '',
  description: '',
  voice: '',
};

function SubjectPage() {
  const [activeStep, setActiveStep] = useState(3);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [showMenu, setShowMenu] = useState<number | null>(null);
  const [editingCharacter, setEditingCharacter] = useState<Character | null>(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const [characterForm, setCharacterForm] = useState<CharacterFormData>(defaultCharacterForm);
  const [loading, setLoading] = useState(false);
  const projectId = 1;

  useEffect(() => {
    loadCharacters();
  }, []);

  const loadCharacters = async () => {
    try {
      setLoading(true);
      const data = await getCharacters(projectId);
      setCharacters(data.data || []);
    } catch {
      message.error('加载角色列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAddCharacter = () => {
    setEditingCharacter(null);
    setCharacterForm(defaultCharacterForm);
    setShowEditModal(true);
  };

  const handleEditCharacter = (character: Character) => {
    setEditingCharacter(character);
    setCharacterForm({
      name: character.name || '',
      description: character.description || '',
      voice: character.voiceConfig || '',
    });
    setShowEditModal(true);
  };

  const handleSaveCharacter = async () => {
    if (!characterForm.name.trim()) {
      message.warning('请输入角色名称');
      return;
    }

    try {
      setLoading(true);
      if (editingCharacter?.id) {
        await updateCharacter(editingCharacter.id, {
          name: characterForm.name,
          description: characterForm.description,
          voiceConfig: characterForm.voice,
        });
        message.success('角色更新成功');
      } else {
        await createCharacter({
          projectId,
          name: characterForm.name,
          description: characterForm.description,
          voiceConfig: characterForm.voice,
        });
        message.success('角色创建成功');
      }
      setShowEditModal(false);
      loadCharacters();
    } catch {
      message.error(editingCharacter ? '更新失败' : '创建失败');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteCharacter = async (id: number) => {
    try {
      await deleteCharacter(id);
      message.success('角色删除成功');
      setShowMenu(null);
      loadCharacters();
    } catch {
      message.error('删除失败');
    }
  };

  const handleImageUpload = async (characterId: number, file: File) => {
    try {
      const result = await uploadCharacterImage(file);
      await updateCharacter(characterId, { imageUrl: result.data.url });
      message.success('图片上传成功');
      loadCharacters();
    } catch {
      message.error('图片上传失败');
    }
  };

  return (
    <div className="subject-page">
      <header className="subject-page__header">
        <div className="subject-page__logo">logo</div>
        <div className="subject-page__avatar" />
      </header>

      <div className="subject-page__layout">
        <aside className="subject-page__steps">
          {stepItems.map((item) => (
            <div
              key={item.id}
              className={`subject-page__step-item ${
                activeStep === item.id ? 'subject-page__step-item--active' : ''
              }`}
              onClick={() => setActiveStep(item.id)}
            >
              <div className="subject-page__step-bar" />
              <span>{item.label}</span>
            </div>
          ))}
        </aside>

        <main className="subject-page__content">
          <div className="subject-page__top-section">
            <div className="subject-page__stats">
              <div className="subject-page__stat-item">
                <span className="subject-page__stat-label">角色</span>
                <span className="subject-page__stat-value">{characters.length}</span>
              </div>
              <div className="subject-page__stat-item">
                <span className="subject-page__stat-label">场景</span>
                <span className="subject-page__stat-value">0</span>
              </div>
              <div className="subject-page__stat-item">
                <span className="subject-page__stat-label">道具</span>
                <span className="subject-page__stat-value">0</span>
              </div>
            </div>

            <div className="subject-page__breadcrumb">
              <div className="subject-page__project-icon">📁</div>
              <span className="subject-page__project-name">两只老虎的青枫奇遇</span>
              <span className="subject-page__separator">›</span>
              <div className="subject-page__episode-badge">第一集</div>
            </div>
          </div>

          <div className="subject-page__characters">
            {characters.map((character) => (
              <div key={character.id} className="subject-page__character-card">
                <div
                  className="subject-page__character-icon"
                  onClick={() => {
                    const input = document.createElement('input');
                    input.type = 'file';
                    input.accept = 'image/*';
                    input.onchange = (e) => {
                      const file = (e.target as HTMLInputElement).files?.[0];
                      if (file && character.id) {
                        handleImageUpload(character.id, file);
                      }
                    };
                    input.click();
                  }}
                  style={{ cursor: 'pointer' }}
                >
                  {character.imageUrl ? (
                    <img src={character.imageUrl} alt={character.name} />
                  ) : (
                    <UserOutlined />
                  )}
                </div>
                <div className="subject-page__character-info">
                  <div
                    className="subject-page__character-name"
                    onClick={() => handleEditCharacter(character)}
                    style={{ cursor: 'pointer' }}
                  >
                    {character.name}
                  </div>
                  <div className="subject-page__character-desc">{character.description}</div>
                  <div className="subject-page__character-voice">
                    <span>选择音色：</span>
                    <span className="subject-page__voice-value">{character.voiceConfig || '默认音色'}</span>
                    <span className="subject-page__arrow">›</span>
                  </div>
                </div>
                <button
                  type="button"
                  className="subject-page__menu-btn"
                  onClick={() => {
                    if (!character.id) return;
                    setShowMenu(showMenu === character.id ? null : character.id);
                  }}
                >
                  ···
                </button>
                {showMenu === character.id && (
                  <div className="subject-page__menu">
                    <div
                      className="subject-page__menu-item"
                      onClick={() => handleEditCharacter(character)}
                    >
                      编辑
                    </div>
                    <div
                      className="subject-page__menu-item subject-page__menu-item--delete"
                      onClick={() => character.id && handleDeleteCharacter(character.id)}
                    >
                      删除
                    </div>
                  </div>
                )}
              </div>
            ))}

            <div className="subject-page__add-character" onClick={handleAddCharacter}>
              <span className="subject-page__add-icon">+</span>
              <div className="subject-page__add-label">添加角色</div>
            </div>
          </div>

          <div className="subject-page__actions">
            <div className="subject-page__action-buttons">
              <button className="subject-page__btn subject-page__btn--secondary">
                批量生成角色
              </button>
              <button className="subject-page__btn subject-page__btn--primary" onClick={handleAddCharacter}>
                添加角色
              </button>
              <button className="subject-page__btn subject-page__btn--disabled" disabled>
                开始智能分镜
              </button>
            </div>
          </div>

          <div className="subject-page__bottom-section">
            <div className="subject-page__generate-card">
              <div className="subject-page__generate-icon">🎬</div>
              <span className="subject-page__generate-text">生成图片</span>
            </div>

            <div className="subject-page__upload-section">
              <span className="subject-page__upload-icon">+</span>
              <span className="subject-page__upload-text">上传</span>
            </div>
          </div>
        </main>
      </div>

      {showEditModal && (
        <div className="modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="character-edit-modal" onClick={(e) => e.stopPropagation()}>
            <h2>{editingCharacter ? '编辑角色' : '添加角色'}</h2>
            <div className="character-edit-form">
              <div className="form-field">
                <label>角色名称 *</label>
                <input
                  type="text"
                  value={characterForm.name}
                  onChange={(e) => setCharacterForm({ ...characterForm, name: e.target.value })}
                  placeholder="请输入角色名称"
                />
              </div>
              <div className="form-field">
                <label>角色描述</label>
                <textarea
                  value={characterForm.description}
                  onChange={(e) => setCharacterForm({ ...characterForm, description: e.target.value })}
                  placeholder="请输入角色描述"
                />
              </div>
              <div className="form-field">
                <label>音色配置</label>
                <input
                  type="text"
                  value={characterForm.voice}
                  onChange={(e) => setCharacterForm({ ...characterForm, voice: e.target.value })}
                  placeholder="如：霸气威武、奶声奶气"
                />
              </div>
            </div>
            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => setShowEditModal(false)}
              >
                取消
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleSaveCharacter}
                disabled={loading}
              >
                {loading ? '保存中...' : '保存'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default SubjectPage;
