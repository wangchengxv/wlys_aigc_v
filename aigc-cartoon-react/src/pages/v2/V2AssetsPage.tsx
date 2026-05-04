import { useEffect, useState } from 'react';
import { Select, Spin } from 'antd';
import { useNavigate } from 'react-router-dom';
import type { Project } from '@/types/project';
import { listProjects } from '@/api/v2/projects';
import type { Character } from '@/api/character';
import { getCharacters } from '@/api/character';
import V2Nav from './V2Nav';
import '@/styles/v2-projects-page.css';

export default function V2AssetsPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [loadingProjects, setLoadingProjects] = useState(false);
  const [loadingCharacters, setLoadingCharacters] = useState(false);
  const [fetchError, setFetchError] = useState(false);

  const loadProjects = async () => {
    setLoadingProjects(true);
    try {
      const list = await listProjects();
      setProjects(list || []);
      if (list?.length && !selectedProjectId) {
        setSelectedProjectId(list[0].id as number);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingProjects(false);
    }
  };

  const loadCharacters = async (projectId: number) => {
    setLoadingCharacters(true);
    setFetchError(false);
    try {
      const data = await getCharacters(projectId);
      setCharacters(data?.data || []);
    } catch (err) {
      setFetchError(true);
      console.error(err);
    } finally {
      setLoadingCharacters(false);
    }
  };

  useEffect(() => {
    void loadProjects();
  }, []);

  useEffect(() => {
    if (selectedProjectId) {
      void loadCharacters(selectedProjectId);
    }
  }, [selectedProjectId]);

  const selectedProject = projects.find((p) => p.id === selectedProjectId);

  return (
    <div className="v2-projects">
      <div className="v2-projects__brand">Miioo</div>
      <div className="v2-projects__avatar" />
      <V2Nav />

      <div style={{ position: 'relative', padding: '300px 120px 80px 196px' }}>
        <div style={{ marginBottom: 24 }}>
          <div style={{ color: 'rgba(255,255,255,0.6)', fontSize: 14, marginBottom: 8 }}>选择项目</div>
          {loadingProjects ? (
            <Spin size="small" />
          ) : (
            <Select
              style={{ width: 300 }}
              placeholder="请选择项目"
              value={selectedProjectId}
              onChange={(val) => setSelectedProjectId(val as number)}
              options={projects.map((p) => ({ label: p.name, value: p.id }))}
              popupMatchSelectWidth={false}
            />
          )}
        </div>

        {loadingCharacters && (
          <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
            <Spin size="large" />
          </div>
        )}

        {!loadingCharacters && fetchError && (
          <div className="v2-projects__errorBox">
            <div className="v2-projects__errorMsg">加载角色列表失败</div>
            <button className="v2-projects__retryBtn" type="button" onClick={() => { if (selectedProjectId) void loadCharacters(selectedProjectId); }}>重试</button>
          </div>
        )}

        {!loadingCharacters && !fetchError && selectedProjectId && characters.length === 0 && (
          <div className="v2-projects__emptyBox">
            <div className="v2-projects__emptyTitle">暂无角色</div>
            <div className="v2-projects__emptyHint">在项目工作区中添加角色后，将在此处显示</div>
          </div>
        )}

        {!loadingCharacters && !fetchError && characters.length > 0 && (
          <div>
            <div style={{ color: '#fff', fontWeight: 700, fontSize: 18, marginBottom: 16 }}>
              {selectedProject?.name} - 角色列表
            </div>
            <div className="v2-workspace__assetList">
              {characters.map((c) => (
                <div key={c.id} className="v2-workspace__assetItem">
                  <div className="v2-workspace__assetAvatar">
                    {c.imageUrl ? <img src={c.imageUrl} alt={c.name} /> : <span>{c.name?.[0] || '?'}</span>}
                  </div>
                  <div className="v2-workspace__assetInfo">
                    <div className="v2-workspace__assetName">{c.name}</div>
                    <div className="v2-workspace__assetDesc">{c.description || '暂无描述'}</div>
                  </div>
                  <button
                    type="button"
                    onClick={() => navigate(`/v2/projects/${selectedProjectId}?tab=assets`)}
                    style={{
                      padding: '6px 12px',
                      borderRadius: 6,
                      border: '1px solid rgba(255,255,255,0.3)',
                      background: 'transparent',
                      color: '#fff',
                      fontSize: 13,
                      cursor: 'pointer',
                    }}
                  >
                    编辑
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
