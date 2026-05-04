import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Form, Image, Input, Modal, Select, Table, Tabs, Upload, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import type { Project, ProjectConfig } from '@/types/project';
import { getProject, updateProject, uploadProjectCover } from '@/api/v2/projects';
import { getProjectConfig, updateProjectConfig } from '@/api/v2/config';
import type { Script } from '@/api/v2/scripts';
import { getLatestScript, saveOrUpdateLatestScript } from '@/api/v2/scripts';
import type { Storyboard } from '@/api/v2/storyboards';
import { listStoryboards, saveStoryboardsBatch } from '@/api/v2/storyboards';
import type { Character } from '@/api/character';
import { getCharacters, createCharacter, updateCharacter, deleteCharacter, uploadCharacterImage } from '@/api/character';
import type { Video, VideoForm } from '@/api/v2/videos';
import { listProjectVideos, createVideo } from '@/api/v2/videos';
import type { Episode, EpisodeForm } from '@/api/v2/episodes';
import { listProjectEpisodes, createEpisode, updateEpisode, deleteEpisode } from '@/api/v2/episodes';
import sampleCover from '@/assets/figma/cartoon-workflow-v2/v2-project-cover-sample.png';
import V2Nav from './V2Nav';
import '@/styles/v2-workspace-page.css';

type TabKey = 'global' | 'script' | 'storyboard' | 'assets' | 'videos' | 'episodes';

function toNumber(value?: string) {
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

export default function V2ProjectWorkspacePage() {
  const navigate = useNavigate();
  const params = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const projectId = toNumber(params.projectId);
  const tab = (searchParams.get('tab') as TabKey) || 'global';

  const [project, setProject] = useState<Project | null>(null);
  const [script, setScript] = useState<Script | null>(null);
  const [existingStoryboards, setExistingStoryboards] = useState<Storyboard[]>([]);
  const [draftStoryboards, setDraftStoryboards] = useState<Storyboard[]>([]);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [loading, setLoading] = useState(false);
  const [savingConfig, setSavingConfig] = useState(false);
  const [savingScript, setSavingScript] = useState(false);
  const [savingStoryboards, setSavingStoryboards] = useState(false);
  const [savingVideo, setSavingVideo] = useState(false);
  const [coverFileList, setCoverFileList] = useState<UploadFile[]>([]);
  const [uploadingCover, setUploadingCover] = useState(false);
  const [videos, setVideos] = useState<Video[]>([]);
  const [loadingVideos, setLoadingVideos] = useState(false);
  const [videoModalVisible, setVideoModalVisible] = useState(false);
  const [videoForm] = Form.useForm<VideoForm>();

  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loadingEpisodes, setLoadingEpisodes] = useState(false);
  const [episodeModalVisible, setEpisodeModalVisible] = useState(false);
  const [editingEpisode, setEditingEpisode] = useState<Episode | null>(null);
  const [savingEpisode, setSavingEpisode] = useState(false);
  const [episodeForm] = Form.useForm<EpisodeForm>();

  const [characterModalVisible, setCharacterModalVisible] = useState(false);
  const [editingCharacter, setEditingCharacter] = useState<Character | null>(null);
  const [savingCharacter, setSavingCharacter] = useState(false);
  const [characterForm] = Form.useForm<{
    name: string;
    description?: string;
    appearancePrompt?: string;
    voiceConfig?: string;
    imageUrl?: string;
  }>();
  const [characterImageUrl, setCharacterImageUrl] = useState<string>('');
  const [uploadingCharacterImage, setUploadingCharacterImage] = useState(false);

  const [configForm] = Form.useForm<ProjectConfig>();
  const [scriptForm] = Form.useForm<{ title?: string; content?: string }>();

  const loadAll = async () => {
    if (!projectId) return;
    setLoading(true);
    try {
      const [p, c, s] = await Promise.all([
        getProject(projectId),
        getProjectConfig(projectId),
        getLatestScript(projectId),
      ]);
      setProject(p);
      configForm.setFieldsValue(c);
      if (p?.coverImage) {
        setCoverFileList([{ uid: '-1', name: 'cover', status: 'done', url: p.coverImage }]);
      } else {
        setCoverFileList([]);
      }

      setScript(s);
      scriptForm.setFieldsValue({ title: s?.title, content: s?.content });

      const [charList, boardList] = await Promise.all([
        getCharacters(projectId).then((r) => r.data || []).catch(() => [] as Character[]),
        s?.id ? listStoryboards(s.id).catch(() => [] as Storyboard[]) : Promise.resolve([] as Storyboard[]),
      ]);
      setCharacters(charList || []);
      setExistingStoryboards((boardList || []).sort((a, b) => (a.sceneNumber || 0) - (b.sceneNumber || 0)));
      setDraftStoryboards([]);
    } catch (err) {
      message.error('加载项目数据失败，请重试');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void loadAll(); }, [projectId]);

  useEffect(() => {
    if (tab === 'videos' && projectId) {
      setLoadingVideos(true);
      listProjectVideos(projectId)
        .then((data) => setVideos(data || []))
        .catch(() => setVideos([]))
        .finally(() => setLoadingVideos(false));
    }
    if (tab === 'episodes' && projectId) {
      setLoadingEpisodes(true);
      listProjectEpisodes(projectId)
        .then((data) => setEpisodes(data || []))
        .catch(() => setEpisodes([]))
        .finally(() => setLoadingEpisodes(false));
    }
  }, [tab, projectId]);

  const onCreateVideo = async () => {
    if (!projectId) return;
    try {
      const values = await videoForm.validateFields();
      setSavingVideo(true);
      await createVideo({ ...values, projectId });
      message.success('视频记录创建成功');
      setVideoModalVisible(false);
      videoForm.resetFields();
      const data = await listProjectVideos(projectId);
      setVideos(data || []);
    } catch (err) {
      message.error('创建失败，请重试');
      console.error(err);
    } finally {
      setSavingVideo(false);
    }
  };

  const openEpisodeModal = (episode?: Episode) => {
    setEditingEpisode(episode || null);
    episodeForm.setFieldsValue({
      episodeNumber: episode?.episodeNumber || (episodes.length + 1),
      title: episode?.title || '',
      status: episode?.status || 'draft',
      scriptSummary: episode?.scriptSummary || '',
    });
    setEpisodeModalVisible(true);
  };

  const onSaveEpisode = async () => {
    if (!projectId) return;
    try {
      const values = await episodeForm.validateFields();
      setSavingEpisode(true);
      if (editingEpisode?.id) {
        await updateEpisode(editingEpisode.id, values);
        message.success('剧集更新成功');
      } else {
        await createEpisode({ ...values, projectId });
        message.success('剧集创建成功');
      }
      setEpisodeModalVisible(false);
      episodeForm.resetFields();
      const data = await listProjectEpisodes(projectId);
      setEpisodes(data || []);
    } catch (err) {
      message.error('保存失败，请重试');
      console.error(err);
    } finally {
      setSavingEpisode(false);
    }
  };

  const onDeleteEpisode = (episode: Episode) => {
    if (!episode.id) return;
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除第 ${episode.episodeNumber} 集吗？`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          if (episode.id && projectId) {
            await deleteEpisode(episode.id);
            message.success('剧集已删除');
            const data = await listProjectEpisodes(projectId);
            setEpisodes(data || []);
          }
        } catch {
          message.error('删除失败');
        }
      },
    });
  };

  const openCharacterModal = (character?: Character) => {
    setEditingCharacter(character || null);
    setCharacterImageUrl(character?.imageUrl || '');
    characterForm.setFieldsValue({
      name: character?.name || '',
      description: character?.description || '',
      appearancePrompt: character?.appearancePrompt || '',
      voiceConfig: character?.voiceConfig || '',
    });
    setCharacterModalVisible(true);
  };

  const handleCharacterImageUpload = async (file: File) => {
    setUploadingCharacterImage(true);
    try {
      const result = await uploadCharacterImage(file);
      setCharacterImageUrl(result.data.url);
      characterForm.setFieldsValue({ imageUrl: result.data.url });
      message.success('图片上传成功');
    } catch {
      message.error('图片上传失败');
    } finally {
      setUploadingCharacterImage(false);
    }
    return false;
  };

  const onSaveCharacter = async () => {
    if (!projectId) return;
    try {
      const values = await characterForm.validateFields();
      setSavingCharacter(true);
      if (editingCharacter?.id) {
        await updateCharacter(editingCharacter.id, { ...values, imageUrl: characterImageUrl });
        message.success('角色更新成功');
      } else {
        await createCharacter({ ...values, imageUrl: characterImageUrl, projectId });
        message.success('角色创建成功');
      }
      setCharacterModalVisible(false);
      characterForm.resetFields();
      setCharacterImageUrl('');
      await loadAll();
    } catch (err) {
      message.error('保存失败，请重试');
      console.error(err);
    } finally {
      setSavingCharacter(false);
    }
  };

  const onDeleteCharacter = (character: Character) => {
    if (!character.id) return;
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除角色「${character.name}」吗？`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          if (character.id) {
            await deleteCharacter(character.id);
            message.success('角色已删除');
            await loadAll();
          }
        } catch {
          message.error('删除失败');
        }
      },
    });
  };

  const addStoryboardDraft = () => {
    if (!script?.id) {
      message.info('请先保存剧本');
      return;
    }
    const maxExisting = Math.max(0, ...existingStoryboards.map((x) => x.sceneNumber || 0));
    const maxDraft = Math.max(0, ...draftStoryboards.map((x) => x.sceneNumber || 0));
    const next = Math.max(maxExisting, maxDraft) + 1;
    setDraftStoryboards((prev) => [
      ...prev,
      { scriptId: script.id as number, sceneNumber: next, sceneDescription: '', duration: 0, bgPrompt: '', characterPrompt: '' },
    ]);
  };

  const onSaveConfig = async () => {
    if (!projectId) return;
    try {
      const values = await configForm.validateFields();
      setSavingConfig(true);
      await updateProjectConfig(projectId, values);
      message.success('配置已保存');
      await loadAll();
    } catch (err) {
      message.error('保存失败，请重试');
      console.error(err);
    } finally {
      setSavingConfig(false);
    }
  };

  const onSaveScript = async () => {
    if (!projectId) return;
    try {
      const values = await scriptForm.validateFields();
      setSavingScript(true);
      const saved = await saveOrUpdateLatestScript(projectId, values);
      setScript(saved);
      message.success('剧本已保存');
      await loadAll();
      setSearchParams({ tab: 'script' });
    } catch (err) {
      message.error('保存失败，请重试');
      console.error(err);
    } finally {
      setSavingScript(false);
    }
  };

  const onSaveStoryboards = async () => {
    if (!script?.id) return;
    if (!draftStoryboards.length) {
      message.info('请先添加分镜');
      return;
    }
    setSavingStoryboards(true);
    try {
      await saveStoryboardsBatch(
        draftStoryboards.map((s) => ({
          ...s,
          scriptId: script.id as number,
        })),
      );
      message.success('分镜已保存');
      await loadAll();
      setSearchParams({ tab: 'storyboard' });
    } catch (err) {
      message.error('保存失败，请重试');
      console.error(err);
    } finally {
      setSavingStoryboards(false);
    }
  };

  const handleCoverUpload = async (file: File) => {
    if (!projectId) return;
    setUploadingCover(true);
    try {
      const { url } = await uploadProjectCover(file);
      await updateProject(projectId, { coverImage: url });
      setCoverFileList([{ uid: '-1', name: 'cover', status: 'done', url }]);
      setProject((prev) => (prev ? { ...prev, coverImage: url } : prev));
      message.success('封面上传成功');
    } catch (err) {
      message.error('封面上传失败');
      console.error(err);
    } finally {
      setUploadingCover(false);
    }
    return false;
  };

  const existingColumns = useMemo<ColumnsType<Storyboard>>(
    () => [
      { title: '序号', dataIndex: 'sceneNumber', width: 80 },
      { title: '场景描述', dataIndex: 'sceneDescription' },
      { title: '时长(秒)', dataIndex: 'duration', width: 100 },
      { title: '背景描述', dataIndex: 'bgPrompt', width: 180, ellipsis: true },
      { title: '角色描述', dataIndex: 'characterPrompt', width: 180, ellipsis: true },
    ],
    [],
  );

  const draftColumns = useMemo<ColumnsType<Storyboard>>(
    () => [
      { title: '序号', dataIndex: 'sceneNumber', width: 80, render: (_, row, idx) => (
        <Input value={row.sceneNumber} size="small" onChange={(e) => { const v = Number(e.target.value); setDraftStoryboards((prev) => prev.map((x, i) => i === idx ? { ...x, sceneNumber: Number.isFinite(v) ? v : 0 } : x)); }} />
      )},
      { title: '场景描述', dataIndex: 'sceneDescription', render: (_, row, idx) => (
        <Input.TextArea value={row.sceneDescription} size="small" autoSize={{ minRows: 1, maxRows: 3 }} onChange={(e) => setDraftStoryboards((prev) => prev.map((x, i) => i === idx ? { ...x, sceneDescription: e.target.value } : x))} />
      )},
      { title: '时长(秒)', dataIndex: 'duration', width: 80, render: (_, row, idx) => (
        <Input value={row.duration} size="small" type="number" onChange={(e) => { const v = Number(e.target.value); setDraftStoryboards((prev) => prev.map((x, i) => i === idx ? { ...x, duration: Number.isFinite(v) ? v : 0 } : x)); }} />
      )},
      { title: '背景提示词', dataIndex: 'bgPrompt', render: (_, row, idx) => (
        <Input value={row.bgPrompt} size="small" placeholder="描述场景背景" onChange={(e) => setDraftStoryboards((prev) => prev.map((x, i) => i === idx ? { ...x, bgPrompt: e.target.value } : x))} />
      )},
      { title: '角色提示词', dataIndex: 'characterPrompt', render: (_, row, idx) => (
        <Input value={row.characterPrompt} size="small" placeholder="描述角色动作" onChange={(e) => setDraftStoryboards((prev) => prev.map((x, i) => i === idx ? { ...x, characterPrompt: e.target.value } : x))} />
      )},
      { title: '', dataIndex: 'actions', width: 60, render: (_, __, idx) => (
        <Button type="link" danger size="small" onClick={() => setDraftStoryboards((prev) => prev.filter((_, i) => i !== idx))}>删除</Button>
      )},
    ],
    [],
  );

  if (!projectId) {
    return <div className="v2-workspace v2-workspace__empty">项目不存在或加载中...</div>;
  }

  const statsItems = [
    { label: '角色', value: characters.length },
    { label: '分镜', value: existingStoryboards.length },
    { label: '集数', value: episodes.length },
    { label: '剧本', value: script?.id ? 1 : 0 },
  ];

  return (
    <div className="v2-workspace">
      <div className="v2-workspace__topBar">
        <button className="v2-workspace__back" type="button" onClick={() => navigate('/v2/projects')}>‹ 返回</button>
        <V2Nav />
      </div>

      <div className="v2-workspace__header">
        <div className="v2-workspace__projectName">{loading ? '加载中...' : (project?.name || '项目')}</div>
        <Tabs
          activeKey={tab}
          onChange={(key) => setSearchParams({ tab: key as TabKey })}
          className="v2-workspace__tabs"
          items={[
            { key: 'global', label: '全局设定' },
            { key: 'script', label: '剧本' },
            { key: 'storyboard', label: '分镜' },
            { key: 'assets', label: '资产库' },
            { key: 'videos', label: '视频' },
            { key: 'episodes', label: '剧集' },
          ]}
        />
      </div>

      <div className="v2-workspace__content">
        <div className="v2-workspace__left">
          <Card className="v2-workspace__card" title="项目概况" bordered={false}>
            <div className="v2-workspace__coverBox">
              <img src={project?.coverImage || sampleCover} alt="" className="v2-workspace__coverImg" />
              <Upload fileList={coverFileList} onChange={({ fileList: next }) => setCoverFileList(next.slice(-1))} beforeUpload={handleCoverUpload} maxCount={1} showUploadList={false}>
                <Button size="small" loading={uploadingCover}>更换封面</Button>
              </Upload>
            </div>
            <div className="v2-workspace__kv"><div className="v2-workspace__k">画面比例</div><div className="v2-workspace__v">{project?.aspectRatio || '-'}</div></div>
            <div className="v2-workspace__kv"><div className="v2-workspace__k">视觉风格</div><div className="v2-workspace__v">{project?.style || '-'}</div></div>
            <div className="v2-workspace__kv"><div className="v2-workspace__k">项目描述</div><div className="v2-workspace__v">{project?.description || '-'}</div></div>
          </Card>

          <Card className="v2-workspace__card" title="项目统计" bordered={false}>
            {statsItems.map((item) => (
              <div key={item.label} className="v2-workspace__kv">
                <div className="v2-workspace__k">{item.label}</div>
                <div className="v2-workspace__v">{item.value}</div>
              </div>
            ))}
          </Card>
        </div>

        <div className="v2-workspace__main">
          {tab === 'global' && (
            <Card className="v2-workspace__card" title="全局设定" bordered={false}>
              <Form form={configForm} layout="vertical" requiredMark={false} disabled={loading}>
                <Form.Item label="对话模型" name="dialogModel">
                  <Select options={[{ label: 'GPT-5.4', value: 'GPT-5.4' }, { label: 'GLM-5.1', value: 'GLM-5.1' }, { label: 'Doubao-Seed-2.0-Pro', value: 'Doubao-Seed-2.0-Pro' }]} />
                </Form.Item>
                <Form.Item label="图片模型" name="imageModel">
                  <Select options={[{ label: '默认', value: 'default' }]} />
                </Form.Item>
                <Form.Item label="视频模型" name="videoModel">
                  <Select options={[{ label: '默认', value: 'default' }]} />
                </Form.Item>
                <Form.Item label="音频模型" name="audioModel">
                  <Select options={[{ label: '默认', value: 'default' }]} />
                </Form.Item>
                <Form.Item label="集数" name="episodeCount">
                  <Input />
                </Form.Item>
                <Form.Item label="批量生成模型" name="batchModel">
                  <Select options={[{ label: 'Doubao-Seed-2.0-Pro', value: 'Doubao-Seed-2.0-Pro' }, { label: 'GPT-5.4', value: 'GPT-5.4' }, { label: 'GLM-5.1', value: 'GLM-5.1' }]} />
                </Form.Item>
                <Form.Item label="批量生成比例" name="batchRatio">
                  <Select options={[{ label: '16:9', value: '16:9' }, { label: '9:16', value: '9:16' }, { label: '1:1', value: '1:1' }]} />
                </Form.Item>
                <Form.Item label="批量生成质量" name="batchQuality">
                  <Select options={[{ label: '1K', value: '1K' }, { label: '2K', value: '2K' }, { label: '4K', value: '4K' }]} />
                </Form.Item>
                <Button type="primary" onClick={onSaveConfig} loading={savingConfig} disabled={loading}>保存配置</Button>
              </Form>
            </Card>
          )}

          {tab === 'script' && (
            <Card className="v2-workspace__card" title="剧本" bordered={false}>
              <Form form={scriptForm} layout="vertical" requiredMark={false} disabled={loading}>
                <Form.Item label="标题" name="title"><Input placeholder="请输入标题" /></Form.Item>
                <Form.Item label="内容" name="content"><Input.TextArea autoSize={{ minRows: 10, maxRows: 18 }} placeholder="请输入剧本内容" /></Form.Item>
                <Button type="primary" onClick={onSaveScript} loading={savingScript} disabled={loading}>保存剧本</Button>
              </Form>
            </Card>
          )}

          {tab === 'storyboard' && (
            <Card className="v2-workspace__card" title="分镜" bordered={false}>
              {!script?.id && <div className="v2-workspace__hint">请先在「剧本」页保存一次剧本，再添加分镜。</div>}
              {!!existingStoryboards.length && (
                <div className="v2-workspace__section">
                  <div className="v2-workspace__sectionTitle">已保存分镜</div>
                  <Table rowKey={(r) => String(r.id || `${r.sceneNumber}-${r.sceneDescription}`)} columns={existingColumns} dataSource={existingStoryboards} pagination={false} size="small" />
                </div>
              )}
              <div className="v2-workspace__section">
                <div className="v2-workspace__sectionHeader">
                  <div className="v2-workspace__sectionTitle">新增分镜</div>
                  <Button onClick={addStoryboardDraft} disabled={!script?.id}>添加</Button>
                </div>
                {!!draftStoryboards.length && (
                  <Table rowKey={(_, idx) => String(idx)} columns={draftColumns} dataSource={draftStoryboards} pagination={false} size="small" scroll={{ x: 800 }} />
                )}
                {draftStoryboards.length === 0 && (
                  <div className="v2-workspace__hint">点击「添加」新增分镜</div>
                )}
                <div className="v2-workspace__actions">
                  <Button type="primary" onClick={onSaveStoryboards} loading={savingStoryboards} disabled={!script?.id || draftStoryboards.length === 0}>批量保存</Button>
                </div>
              </div>
            </Card>
          )}

          {tab === 'assets' && (
            <Card className="v2-workspace__card" title="角色管理" bordered={false}>
              <div style={{ marginBottom: 16 }}>
                <Button type="primary" onClick={() => openCharacterModal()}>新增角色</Button>
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
                    <div style={{ display: 'flex', gap: 8 }}>
                      <Button size="small" onClick={() => openCharacterModal(c)}>编辑</Button>
                      <Button size="small" danger type="link" onClick={() => onDeleteCharacter(c)}>删除</Button>
                    </div>
                  </div>
                ))}
                {characters.length === 0 && <div className="v2-workspace__hint">暂无角色，点击「新增角色」添加</div>}
              </div>
            </Card>
          )}

          {tab === 'videos' && (
            <Card className="v2-workspace__card" title="视频记录" bordered={false}>
              <div style={{ marginBottom: 16 }}>
                <Button type="primary" onClick={() => { videoForm.resetFields(); setVideoModalVisible(true); }}>新增视频记录</Button>
              </div>
              {loadingVideos ? (
                <div style={{ textAlign: 'center', padding: 40 }}>加载中...</div>
              ) : videos.length === 0 ? (
                <div className="v2-workspace__hint">暂无视频记录</div>
              ) : (
                <Table
                  rowKey="id"
                  dataSource={videos}
                  pagination={false}
                  columns={[
                    { title: '缩略图', dataIndex: 'thumbnailUrl', width: 120, render: (url?: string) => url ? <Image src={url} width={80} height={45} /> : <div style={{ width: 80, height: 45, background: '#f0f0f0', textAlign: 'center', lineHeight: '45px' }}>?</div> },
                    { title: '视频地址', dataIndex: 'videoUrl', ellipsis: true, render: (url?: string) => url ? <a href={url} target="_blank" rel="noopener noreferrer">{url}</a> : '-' },
                    { title: '时长(秒)', dataIndex: 'duration', width: 100, render: (d?: number) => d ?? '-' },
                    { title: '状态', dataIndex: 'status', width: 100, render: (s?: string) => ({ pending: '处理中', ready: '就绪', failed: '失败' }[s || ''] || s || '-') },
                    { title: '创建时间', dataIndex: 'createTime', width: 180 },
                  ]}
                />
              )}
            </Card>
          )}

          {tab === 'episodes' && (
            <Card className="v2-workspace__card" title="剧集管理" bordered={false}>
              <div style={{ marginBottom: 16 }}>
                <Button type="primary" onClick={() => openEpisodeModal()}>新增剧集</Button>
              </div>
              {loadingEpisodes ? (
                <div style={{ textAlign: 'center', padding: 40 }}>加载中...</div>
              ) : episodes.length === 0 ? (
                <div className="v2-workspace__hint">暂无剧集，点击「新增剧集」添加</div>
              ) : (
                <Table
                  rowKey="id"
                  dataSource={episodes}
                  pagination={false}
                  columns={[
                    { title: '集号', dataIndex: 'episodeNumber', width: 80 },
                    { title: '标题', dataIndex: 'title', ellipsis: true },
                    { title: '状态', dataIndex: 'status', width: 100, render: (s?: string) => ({ draft: '草稿', published: '已发布' }[s || ''] || s || '-') },
                    { title: '创建时间', dataIndex: 'createTime', width: 180 },
                    { title: '操作', width: 150, render: (_: unknown, episode: Episode) => (
                      <>
                        <Button size="small" onClick={() => openEpisodeModal(episode)}>编辑</Button>
                        <Button size="small" danger type="link" onClick={() => onDeleteEpisode(episode)}>删除</Button>
                      </>
                    )},
                  ]}
                />
              )}
            </Card>
          )}
        </div>
      </div>

      <Modal
        title={editingCharacter ? '编辑角色' : '新增角色'}
        open={characterModalVisible}
        onOk={onSaveCharacter}
        onCancel={() => setCharacterModalVisible(false)}
        confirmLoading={savingCharacter}
      >
        <Form form={characterForm} layout="vertical">
          <Form.Item label="角色名称" name="name" rules={[{ required: true, message: '请输入角色名称' }]}>
            <Input placeholder="请输入角色名称" />
          </Form.Item>
          <Form.Item label="角色描述" name="description">
            <Input.TextArea placeholder="请输入角色描述" autoSize={{ minRows: 2, maxRows: 4 }} />
          </Form.Item>
          <Form.Item label="形象提示词" name="appearancePrompt">
            <Input.TextArea placeholder="描述角色外貌特征" autoSize={{ minRows: 2, maxRows: 4 }} />
          </Form.Item>
          <Form.Item label="语音配置" name="voiceConfig">
            <Input placeholder="请输入语音配置" />
          </Form.Item>
          <Form.Item label="角色图片">
            {characterImageUrl && (
              <div style={{ marginBottom: 8 }}>
                <Image src={characterImageUrl} width={120} height={120} style={{ objectFit: 'cover', borderRadius: 8 }} />
              </div>
            )}
            <Upload beforeUpload={handleCharacterImageUpload} showUploadList={false}>
              <Button loading={uploadingCharacterImage}>上传图片</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="新增视频记录"
        open={videoModalVisible}
        onOk={onCreateVideo}
        onCancel={() => setVideoModalVisible(false)}
        confirmLoading={savingVideo}
      >
        <Form form={videoForm} layout="vertical">
          <Form.Item label="视频地址" name="videoUrl" rules={[{ required: true, message: '请输入视频地址' }]}>
            <Input placeholder="请输入视频URL" />
          </Form.Item>
          <Form.Item label="状态" name="status" initialValue="pending">
            <Select options={[{ label: '处理中', value: 'pending' }, { label: '就绪', value: 'ready' }, { label: '失败', value: 'failed' }]} />
          </Form.Item>
          <Form.Item label="缩略图地址" name="thumbnailUrl">
            <Input placeholder="请输入缩略图URL" />
          </Form.Item>
          <Form.Item label="时长(秒)" name="duration">
            <Input type="number" placeholder="请输入时长" />
          </Form.Item>
          <Form.Item label="关联分镜ID" name="storyboardId">
            <Input type="number" placeholder="请输入分镜ID" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingEpisode ? '编辑剧集' : '新增剧集'}
        open={episodeModalVisible}
        onOk={onSaveEpisode}
        onCancel={() => setEpisodeModalVisible(false)}
        confirmLoading={savingEpisode}
      >
        <Form form={episodeForm} layout="vertical">
          <Form.Item label="集号" name="episodeNumber" rules={[{ required: true, message: '请输入集号' }]}>
            <Input type="number" placeholder="请输入集号" />
          </Form.Item>
          <Form.Item label="标题" name="title">
            <Input placeholder="请输入剧集标题" />
          </Form.Item>
          <Form.Item label="状态" name="status" initialValue="draft">
            <Select options={[{ label: '草稿', value: 'draft' }, { label: '已发布', value: 'published' }]} />
          </Form.Item>
          <Form.Item label="剧本概要" name="scriptSummary">
            <Input.TextArea placeholder="请输入剧本概要" autoSize={{ minRows: 2, maxRows: 4 }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
