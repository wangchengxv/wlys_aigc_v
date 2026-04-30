import { useEffect, useMemo, useState } from 'react';
import { Dropdown, Input, Modal, message } from 'antd';
import type { MenuProps } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import type { Project } from '@/types/project';
import { deleteProject, listProjects, renameProject } from '@/api/v2/projects';
import projectsBadge from '@/assets/figma/cartoon-workflow-v2/v2-projects-badge.png';
import sampleCover from '@/assets/figma/cartoon-workflow-v2/v2-project-cover-sample.png';
import NewProjectModalV2 from './NewProjectModalV2';
import '@/styles/v2-projects-page.css';

type NavItem = {
  key: string;
  label: string;
  to: string;
};

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
  const location = useLocation();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);

  const navItems = useMemo<NavItem[]>(
    () => [
      { key: 'home', label: '首页', to: '/v2' },
      { key: 'projects', label: '项目', to: '/v2/projects' },
      { key: 'creation', label: '创作', to: '/v2/projects' },
      { key: 'assets', label: '资产库', to: '/v2/projects' },
      { key: 'config', label: '配置中心', to: '/v2/projects' },
    ],
    [],
  );

  const refresh = async () => {
    setLoading(true);
    try {
      const list = await listProjects();
      setProjects(list || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void refresh();
  }, []);

  const onRename = (p: Project) => {
    let nextName = p.name;
    Modal.confirm({
      title: '重命名',
      content: (
        <Input
          defaultValue={p.name}
          onChange={(e) => {
            nextName = e.target.value;
          }}
        />
      ),
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        if (!nextName?.trim()) {
          message.error('请输入名称');
          return Promise.reject();
        }
        await renameProject(p.id as number, nextName.trim());
        await refresh();
      },
    });
  };

  const onDelete = (p: Project) => {
    Modal.confirm({
      title: '删除项目',
      content: `确定删除「${p.name}」吗？`,
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        await deleteProject(p.id as number);
        await refresh();
      },
    });
  };

  return (
    <div className="v2-projects">
      <div className="v2-projects__brand">Miioo</div>
      <div className="v2-projects__avatar" />
      <img className="v2-projects__badge" src={projectsBadge} alt="" />

      <nav className="v2-projects__nav">
        {navItems.map((item) => {
          const active = item.to === '/v2' ? location.pathname === '/v2' : location.pathname.startsWith(item.to);
          return (
            <button
              key={item.key}
              className={`v2-projects__navItem ${active ? 'is-active' : ''}`}
              onClick={() => navigate(item.to)}
              type="button"
            >
              {item.label}
            </button>
          );
        })}
      </nav>

      <div className="v2-projects__title">所有项目</div>

      <div className={`v2-projects__grid ${loading ? 'is-loading' : ''}`}>
        <button className="v2-projects__createCard" type="button" onClick={() => setCreateOpen(true)}>
          +新建项目
        </button>

        {projects.map((p) => {
          const menuItems: MenuProps['items'] = [
            {
              key: 'rename',
              label: '重命名',
              onClick: () => onRename(p),
            },
            {
              key: 'delete',
              label: <span style={{ color: '#ff0000' }}>删除</span>,
              onClick: () => onDelete(p),
            },
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
                <button className="v2-projects__cardMenu" type="button" onClick={(e) => e.stopPropagation()}>
                  ···
                </button>
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

