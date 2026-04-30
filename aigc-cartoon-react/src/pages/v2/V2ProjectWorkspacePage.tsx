import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Form, Input, Select, Table, Tabs, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useParams, useSearchParams } from 'react-router-dom';
import type { Project, ProjectConfig } from '@/types/project';
import { getProject } from '@/api/v2/projects';
import { getProjectConfig, updateProjectConfig } from '@/api/v2/config';
import type { Script } from '@/api/v2/scripts';
import { getLatestScript, saveOrUpdateLatestScript } from '@/api/v2/scripts';
import type { Storyboard } from '@/api/v2/storyboards';
import { listStoryboards, saveStoryboardsBatch } from '@/api/v2/storyboards';
import '@/styles/v2-workspace-page.css';

type TabKey = 'global' | 'script' | 'storyboard';

function toNumber(value?: string) {
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

export default function V2ProjectWorkspacePage() {
  const params = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const projectId = toNumber(params.projectId);
  const tab = (searchParams.get('tab') as TabKey) || 'global';

  const [project, setProject] = useState<Project | null>(null);
  const [script, setScript] = useState<Script | null>(null);
  const [existingStoryboards, setExistingStoryboards] = useState<Storyboard[]>([]);
  const [draftStoryboards, setDraftStoryboards] = useState<Storyboard[]>([]);
  const [loading, setLoading] = useState(false);
  const [savingConfig, setSavingConfig] = useState(false);
  const [savingScript, setSavingScript] = useState(false);
  const [savingStoryboards, setSavingStoryboards] = useState(false);

  const [configForm] = Form.useForm<ProjectConfig>();
  const [scriptForm] = Form.useForm<{ title?: string; content?: string }>();

  const loadAll = async () => {
    if (!projectId) return;
    setLoading(true);
    try {
      const [p, c, s] = await Promise.all([getProject(projectId), getProjectConfig(projectId), getLatestScript(projectId)]);
      setProject(p);
      configForm.setFieldsValue(c);

      setScript(s);
      scriptForm.setFieldsValue({ title: s?.title, content: s?.content });

      if (s?.id) {
        const boards = await listStoryboards(s.id);
        setExistingStoryboards((boards || []).slice().sort((a, b) => (a.sceneNumber || 0) - (b.sceneNumber || 0)));
      } else {
        setExistingStoryboards([]);
      }
      setDraftStoryboards([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadAll();
  }, [projectId]);

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
      {
        scriptId: script.id as number,
        sceneNumber: next,
        sceneDescription: '',
        duration: 0,
        bgPrompt: '',
        characterPrompt: '',
      },
    ]);
  };

  const onSaveConfig = async () => {
    if (!projectId) return;
    const values = await configForm.validateFields();
    setSavingConfig(true);
    try {
      await updateProjectConfig(projectId, values);
      message.success('已保存');
      await loadAll();
    } finally {
      setSavingConfig(false);
    }
  };

  const onSaveScript = async () => {
    if (!projectId) return;
    const values = await scriptForm.validateFields();
    setSavingScript(true);
    try {
      const saved = await saveOrUpdateLatestScript(projectId, values);
      setScript(saved);
      message.success('剧本已保存');
      await loadAll();
      setSearchParams({ tab: 'script' });
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
    } finally {
      setSavingStoryboards(false);
    }
  };

  const existingColumns = useMemo<ColumnsType<Storyboard>>(
    () => [
      { title: '序号', dataIndex: 'sceneNumber', width: 80 },
      { title: '场景描述', dataIndex: 'sceneDescription' },
      { title: '时长', dataIndex: 'duration', width: 100 },
    ],
    [],
  );

  const draftColumns = useMemo<ColumnsType<Storyboard>>(
    () => [
      {
        title: '序号',
        dataIndex: 'sceneNumber',
        width: 80,
        render: (_, row, idx) => (
          <Input
            value={row.sceneNumber}
            onChange={(e) => {
              const v = Number(e.target.value);
              setDraftStoryboards((prev) => prev.map((x, i) => (i === idx ? { ...x, sceneNumber: Number.isFinite(v) ? v : 0 } : x)));
            }}
          />
        ),
      },
      {
        title: '场景描述',
        dataIndex: 'sceneDescription',
        render: (_, row, idx) => (
          <Input
            value={row.sceneDescription}
            onChange={(e) => setDraftStoryboards((prev) => prev.map((x, i) => (i === idx ? { ...x, sceneDescription: e.target.value } : x)))}
          />
        ),
      },
      {
        title: '时长',
        dataIndex: 'duration',
        width: 120,
        render: (_, row, idx) => (
          <Input
            value={row.duration}
            onChange={(e) => {
              const v = Number(e.target.value);
              setDraftStoryboards((prev) => prev.map((x, i) => (i === idx ? { ...x, duration: Number.isFinite(v) ? v : 0 } : x)));
            }}
          />
        ),
      },
      {
        title: '',
        dataIndex: 'actions',
        width: 80,
        render: (_, __, idx) => (
          <Button type="link" danger onClick={() => setDraftStoryboards((prev) => prev.filter((_, i) => i !== idx))}>
            删除
          </Button>
        ),
      },
    ],
    [draftStoryboards],
  );

  if (!projectId) {
    return <div className="v2-workspace v2-workspace__empty">项目不存在</div>;
  }

  return (
    <div className="v2-workspace">
      <div className="v2-workspace__header">
        <div className="v2-workspace__projectName">{project?.name || '项目'}</div>
        <Tabs
          activeKey={tab}
          onChange={(key) => setSearchParams({ tab: key as TabKey })}
          className="v2-workspace__tabs"
          items={[
            { key: 'global', label: '全局设定' },
            { key: 'script', label: '剧本' },
            { key: 'storyboard', label: '分镜' },
          ]}
        />
      </div>

      <div className="v2-workspace__content">
        <div className="v2-workspace__left">
          <Card className="v2-workspace__card" title="项目概况" bordered={false}>
            <div className="v2-workspace__kv">
              <div className="v2-workspace__k">画面比例</div>
              <div className="v2-workspace__v">{project?.aspectRatio || '-'}</div>
            </div>
            <div className="v2-workspace__kv">
              <div className="v2-workspace__k">视觉风格</div>
              <div className="v2-workspace__v">{project?.style || '-'}</div>
            </div>
          </Card>

          <Card className="v2-workspace__card" title="模型配置" bordered={false}>
            <Form form={configForm} layout="vertical" requiredMark={false} disabled={loading}>
              <Form.Item label="对话模型" name="dialogModel">
                <Select
                  options={[
                    { label: 'GPT-5.4', value: 'GPT-5.4' },
                    { label: 'GLM-5.1', value: 'GLM-5.1' },
                    { label: 'Doubao-Seed-2.0-Pro', value: 'Doubao-Seed-2.0-Pro' },
                  ]}
                />
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
              <Button type="primary" onClick={onSaveConfig} loading={savingConfig}>
                保存配置
              </Button>
            </Form>
          </Card>
        </div>

        <div className="v2-workspace__main">
          {tab === 'global' && (
            <Card className="v2-workspace__card" title="全局设定" bordered={false}>
              <div className="v2-workspace__hint">在左侧设置模型与参数。</div>
            </Card>
          )}

          {tab === 'script' && (
            <Card className="v2-workspace__card" title="剧本" bordered={false}>
              <Form form={scriptForm} layout="vertical" requiredMark={false} disabled={loading}>
                <Form.Item label="标题" name="title">
                  <Input placeholder="请输入标题" />
                </Form.Item>
                <Form.Item label="内容" name="content">
                  <Input.TextArea autoSize={{ minRows: 10, maxRows: 18 }} placeholder="请输入剧本内容" />
                </Form.Item>
                <Button type="primary" onClick={onSaveScript} loading={savingScript}>
                  保存剧本
                </Button>
              </Form>
            </Card>
          )}

          {tab === 'storyboard' && (
            <Card className="v2-workspace__card" title="分镜" bordered={false}>
              {!script?.id && <div className="v2-workspace__hint">请先在「剧本」页保存一次剧本，再添加分镜。</div>}

              {!!existingStoryboards.length && (
                <div className="v2-workspace__section">
                  <div className="v2-workspace__sectionTitle">已保存分镜</div>
                  <Table
                    rowKey={(r) => String(r.id || `${r.sceneNumber}-${r.sceneDescription}`)}
                    columns={existingColumns}
                    dataSource={existingStoryboards}
                    pagination={false}
                    size="small"
                  />
                </div>
              )}

              <div className="v2-workspace__section">
                <div className="v2-workspace__sectionHeader">
                  <div className="v2-workspace__sectionTitle">新增分镜</div>
                  <Button onClick={addStoryboardDraft} disabled={!script?.id}>
                    添加
                  </Button>
                </div>
                <Table
                  rowKey={(_, idx) => String(idx)}
                  columns={draftColumns}
                  dataSource={draftStoryboards}
                  pagination={false}
                  size="small"
                />
                <div className="v2-workspace__actions">
                  <Button type="primary" onClick={onSaveStoryboards} loading={savingStoryboards} disabled={!script?.id}>
                    批量保存
                  </Button>
                </div>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
