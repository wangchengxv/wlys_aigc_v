import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserOutlined, PlusOutlined, MoreOutlined } from '@ant-design/icons';
import { message } from 'antd';
import { getProjectList, deleteProject } from '@/api/project';
import type { Project } from '@/types/project';
import '@/styles/project-list-page.css';

const visualStyleItems = ['剧本', '分镜', '角色', '渲染'];

function ProjectListPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      setLoading(true);
      const response = await getProjectList();
      setProjects(response.data || []);
    } catch {
      message.error('加载项目列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteProject = async (id: number) => {
    try {
      setDeletingId(id);
      await deleteProject(id);
      message.success('项目删除成功');
      loadProjects();
    } catch {
      message.error('删除失败');
    } finally {
      setDeletingId(null);
    }
  };

  const handleCreateProject = () => {
    navigate('/');
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  };

  return (
    <div className="project-list-page">
      <div className="project-list-page__background">
        <img
          src={`https://figma-prd-bucket.s3.us-west-2.amazonaws.com/attachments/${encodeURIComponent('1d48b7b9346509470a7b47eb1f70d5591dc1036e')}.png`}
          alt="背景"
        />
      </div>

      <header className="project-list-page__header">
        <div className="project-list-page__logo">Miioo</div>
        <div className="project-list-page__avatar">
          <UserOutlined />
        </div>
      </header>

      <aside className="project-list-page__steps">
        {visualStyleItems.map((item, index) => (
          <div
            key={item}
            className={`project-list-page__step-item ${index === 0 ? 'project-list-page__step-item--active' : ''}`}
          >
            <div className="project-list-page__step-icon">
              <span className="project-list-page__step-dot" />
            </div>
            <span className="project-list-page__step-label">{item}</span>
          </div>
        ))}
      </aside>

      <section className="project-list-page__main">
        <div className="project-list-page__title-row">
          <h2 className="project-list-page__title">所有项目</h2>
          <button
            type="button"
            className="project-list-page__new-btn"
            onClick={handleCreateProject}
          >
            <PlusOutlined />
            <span>新建项目</span>
          </button>
        </div>

        <div className="project-list-page__cards">
          {loading ? (
            <div className="project-list-page__loading">加载中...</div>
          ) : projects.length === 0 ? (
            <div className="project-list-page__empty">
              <p>暂无项目</p>
              <button type="button" onClick={handleCreateProject}>
                创建第一个项目
              </button>
            </div>
          ) : (
            projects.map((project) => (
              <article key={project.id} className="project-list-page__card">
                <div className="project-list-page__card-cover">
                  {project.coverImage ? (
                    <img src={project.coverImage} alt={project.name} />
                  ) : (
                    <div className="project-list-page__card-placeholder">暂无封面</div>
                  )}
                </div>
                <div className="project-list-page__card-content">
                  <h3 className="project-list-page__card-name">{project.name}</h3>
                  <p className="project-list-page__card-date">
                    {project.createTime ? formatDate(project.createTime) : ''} 创建时间
                  </p>
                  <div className="project-list-page__card-actions">
                    <button
                      type="button"
                      className="project-list-page__card-menu"
                      onClick={() => {
                        if (window.confirm('确定要删除该项目吗？')) {
                          project.id && handleDeleteProject(project.id);
                        }
                      }}
                      disabled={deletingId === project.id}
                    >
                      <MoreOutlined />
                    </button>
                    <div className="project-list-page__status-tags">
                      <div className={`project-list-page__status-tag project-list-page__status-tag--${project.status || 'draft'}`}>
                        <span>{project.status === 'published' ? '已发布' : '草稿'}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </article>
            ))
          )}
        </div>
      </section>
    </div>
  );
}

export default ProjectListPage;