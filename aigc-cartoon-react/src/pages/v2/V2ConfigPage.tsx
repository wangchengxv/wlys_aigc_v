import { useEffect, useState } from 'react';
import { Button, Card, Divider, Form, Input, Select, Switch, message } from 'antd';
import V2Nav from './V2Nav';
import '@/styles/v2-workspace-page.css';

type GlobalConfig = {
  openaiApiKey: string;
  openaiBaseUrl: string;
  dialogModel: string;
  imageModel: string;
  videoModel: string;
  audioModel: string;
  autoSave: boolean;
  maxConcurrent: number;
};

const STORAGE_KEY = 'v2_global_config';

const defaultConfig: GlobalConfig = {
  openaiApiKey: '',
  openaiBaseUrl: '',
  dialogModel: 'GPT-5.4',
  imageModel: 'default',
  videoModel: 'default',
  audioModel: 'default',
  autoSave: true,
  maxConcurrent: 2,
};

export default function V2ConfigPage() {
  const [form] = Form.useForm<GlobalConfig>();
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      const cfg = stored ? { ...defaultConfig, ...JSON.parse(stored) } : defaultConfig;
      form.setFieldsValue(cfg);
    } catch {
      form.setFieldsValue(defaultConfig);
    } finally {
      setLoading(false);
    }
  }, []);

  const onSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      await new Promise((r) => setTimeout(r, 300));
      localStorage.setItem(STORAGE_KEY, JSON.stringify(values));
      message.success('配置已保存');
    } catch {
      message.error('请检查表单');
    } finally {
      setSaving(false);
    }
  };

  const onReset = () => {
    form.setFieldsValue(defaultConfig);
  };

  return (
    <div style={{ minHeight: '100vh', background: '#000', color: '#fff', padding: '60px 40px' }}>
      <div style={{ fontFamily: "'Lobster', cursive", fontSize: 28, marginBottom: 8 }}>Miioo</div>
      <V2Nav />
      <div style={{ marginTop: 32 }}>
        <Card className="v2-workspace__card" title="AI 模型配置" bordered={false}>
          <Form form={form} layout="vertical" requiredMark={false} disabled={loading}>
            <Divider style={{ borderColor: 'rgba(255,255,255,0.1)' }} />
            <Form.Item label="对话模型" name="dialogModel">
              <Select options={[
                { label: 'GPT-5.4', value: 'GPT-5.4' },
                { label: 'GPT-4o', value: 'GPT-4o' },
                { label: 'GLM-5.1', value: 'GLM-5.1' },
                { label: 'Doubao-Seed-2.0-Pro', value: 'Doubao-Seed-2.0-Pro' },
              ]} />
            </Form.Item>
            <Form.Item label="图片模型" name="imageModel">
              <Select options={[
                { label: '默认', value: 'default' },
                { label: 'DALL-E-3', value: 'DALL-E-3' },
                { label: 'Midjourney', value: 'Midjourney' },
              ]} />
            </Form.Item>
            <Form.Item label="视频模型" name="videoModel">
              <Select options={[
                { label: '默认', value: 'default' },
                { label: 'Sora', value: 'Sora' },
                { label: 'Runway', value: 'Runway' },
              ]} />
            </Form.Item>
            <Form.Item label="音频模型" name="audioModel">
              <Select options={[
                { label: '默认', value: 'default' },
                { label: 'ElevenLabs', value: 'ElevenLabs' },
                { label: 'Azure TTS', value: 'Azure TTS' },
              ]} />
            </Form.Item>
            <Divider style={{ borderColor: 'rgba(255,255,255,0.1)' }} />
            <Form.Item label="OpenAI API Key" name="openaiApiKey">
              <Input.Password placeholder="sk-..." />
            </Form.Item>
            <Form.Item label="OpenAI Base URL（可选）" name="openaiBaseUrl">
              <Input placeholder="https://api.openai.com" />
            </Form.Item>
            <Divider style={{ borderColor: 'rgba(255,255,255,0.1)' }} />
            <Form.Item label="自动保存" name="autoSave" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="最大并发任务数" name="maxConcurrent">
              <Select options={[
                { label: '1', value: 1 },
                { label: '2', value: 2 },
                { label: '3', value: 3 },
              ]} />
            </Form.Item>
            <div style={{ display: 'flex', gap: 12, marginTop: 24 }}>
              <Button type="primary" onClick={onSave} loading={saving}>保存配置</Button>
              <Button onClick={onReset} style={{ color: '#fff', borderColor: 'rgba(255,255,255,0.3)' }}>重置</Button>
            </div>
          </Form>
        </Card>
      </div>
    </div>
  );
}