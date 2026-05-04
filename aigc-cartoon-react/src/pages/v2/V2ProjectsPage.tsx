import { useEffect, useState } from 'react';
import { Dropdown, Modal, message } from 'antd';
import type { MenuProps } from 'antd';
import { useNavigate } from 'react-router-dom';
import type { Project } from '@/types/project';
import { deleteProject, listProjects, renameProject } from '@/api/v2/projects';
import projectsBadge from '@/assets/figma/cartoon-workflow-v2/v2-projects-badge.png';
import sampleCover from '@/assets/figma/cartoon-workflow-v2/v2-project-cover-sample.png';
import V2Nav from './V2Nav';
import NewProjectModalV2 from './NewProjectModalV2';
import '@/styles/v2-projects-page.css';

function formatDate(value?: string) {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

export default function V2ProjectsPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);

  const refresh = async () => {
    setLoading(true);
    setFetchError(false);
    try {
      const list = await listProjects();
      setProjects(list || []);
    } catch (err) {
      setFetchError(true);
      message.error('加载项目列表失败，请重试');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void refresh(); }, []);

  const onRename = (p: Project) => {
    let nextName = p.name;
    Modal.confirm({
      title: '重命名',
      content: (
        <input
          autoFocus
          defaultValue={p.name}
          style={{ color: '#000', width: '100%', padding: '8px', borderRadius: 6, border: '1px solid #ccc' }}
          onChange={(e) => { nextName = e.target.value; }}
        />
      ),
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        if (!nextName?.trim()) { message.error('请输入名称'); return; }
        try {
          await renameProject(p.id as number, nextName.trim());
          await refresh();
          message.success('已重命名');
        } catch (err) {
          message.error('重命名失败');
          console.error(err);
        }
      },
    });
  };

  const onDelete = (p: Project) => {
    Modal.confirm({
      title: '删除项目',
      content: `确定删除「${p.name}」吗？此操作不可撤销。`,
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        try {
          await deleteProject(p.id as number);
          message.success('已删除');
          await refresh();
        } catch (err) {
          message.error('删除失败');
          console.error(err);
        }
      },
    });
  };

  return (
    <div className="v2-projects">
      <div className="v2-projects__brand">Miioo</div>
      <div className="v2-projects__avatar" />
      <img className="v2-projects__badge" src={projectsBadge} alt="" />
      <V2Nav />
      <div className="v2-projects__title">所有项目</div>
      <div className={`v2-projects__grid ${loading ? 'is-loading' : ''}`}>
        <button className="v2-projects__createCard" type="button" onClick={() => setCreateOpen(true)}>
          +新建项目
        </button>

        {fetchError && !loading && (
          <div className="v2-projects__errorBox">
            <div className="v2-projects__errorMsg">加载失败</div>
            <button className="v2-projects__retryBtn" type="button" onClick={() => void refresh()}>重试</button>
          </div>
        )}

        {!fetchError && !loading && projects.length === 0 && (
          <div className="v2-projects__emptyBox">
            <div className="v2-projects__emptyTitle">还没有项目</div>
            <div className="v2-projects__emptyHint">点击上方「+新建项目」开始你的第一个漫剧创作</div>
          </div>
        )}

        {projects.map((p) => {
          const menuItems: MenuProps['items'] = [
            { key: 'rename', label: '重命名', onClick: () => onRename(p) },
            { key: 'delete', label: <span style={{ color: '#ff0000' }}>删除</span>, onClick: () => onDelete(p) },
          ];
          return (
            <div key={p.id} className="v2-projects__card">
              <button type="button" className="v2-projects__cardMain" onClick={() => navigate(`/v2/projects/${p.id}?tab=global`)}>
                <div className="v2-projects__cardCover">
                  <img src={p.coverImage || sampleCover} alt="" />
                </div>
                <div className="v2-projects__cardName">{p.name}</div>
                <div className="v2-projects__cardMeta">{formatDate(p.createTime)}创建时间</div>
              </button>
              <Dropdown menu={{ items: menuItems }} placement="bottomRight" trigger={['click']}>
                <button className="v2-projects__cardMenu" type="button" onClick={(e) => e.stopPropagation()}>···</button>
              </Dropdown>
            </div>
          );
        })}
      </div>
      <NewProjectModalV2
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={async (project) => {
          await refresh();
          message.success('项目已创建');
          navigate(`/v2/projects/${project.id}?tab=global`);
        }}
      />
    </div>
  );
}