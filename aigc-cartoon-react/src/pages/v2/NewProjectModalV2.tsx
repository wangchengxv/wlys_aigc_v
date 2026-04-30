import { useMemo, useState } from 'react';
import { Form, Input, Modal, Radio, Upload, message } from 'antd';
import type { UploadFile } from 'antd';
import type { Project, ProjectForm, VisualStyleOption } from '@/types/project';
import { styleOptions } from '@/types/project';
import { createProject, updateProject, uploadProjectCover } from '@/api/v2/projects';
import style1 from '@/assets/figma/cartoon-workflow-v2/v2-style-1.png';
import style2 from '@/assets/figma/cartoon-workflow-v2/v2-style-2.png';
import style3 from '@/assets/figma/cartoon-workflow-v2/v2-style-3.png';
import style4 from '@/assets/figma/cartoon-workflow-v2/v2-style-4.png';
import style5 from '@/assets/figma/cartoon-workflow-v2/v2-style-5.png';
import '@/styles/v2-new-project-modal.css';

type Props = {
  open: boolean;
  onClose: () => void;
  onCreated: (project: Project) => void;
};

type FormValues = {
  name: string;
  description?: string;
  aspectRatio: '16:9' | '9:16';
  styleId: number;
};

export default function NewProjectModalV2({ open, onClose, onCreated }: Props) {
  const [form] = Form.useForm<FormValues>();
  const [submitting, setSubmitting] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  const styleThumbs = useMemo<Record<number, string>>(
    () => ({
      1: style1,
      2: style2,
      3: style3,
      4: style4,
      5: style5,
    }),
    [],
  );

  const createPayloadFromValues = (values: FormValues, style: VisualStyleOption): ProjectForm => ({
    name: values.name,
    description: values.description,
    aspectRatio: values.aspectRatio,
    style: style.value,
    styleKey: style.styleKey,
    styleTemplateId: style.styleTemplateId,
    visualStylePrompt: style.visualStylePrompt,
    visualStyleMode: 'preset',
  });

  const handleOk = async () => {
    const values = await form.validateFields();
    const style = styleOptions.find((s) => s.id === values.styleId);
    if (!style) {
      message.error('请选择视觉风格');
      return;
    }

    setSubmitting(true);
    try {
      const created = await createProject(createPayloadFromValues(values, style));

      const file = fileList[0]?.originFileObj;
      if (file instanceof File) {
        const { url } = await uploadProjectCover(file);
        await updateProject(created.id as number, { coverImage: url });
        created.coverImage = url;
      }

      onCreated(created);
      onClose();
      form.resetFields();
      setFileList([]);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onCancel={onClose}
      onOk={handleOk}
      okText="确定"
      cancelText="取消"
      confirmLoading={submitting}
      centered
      width={820}
      className="v2-newProjectModal"
    >
      <div className="v2-newProjectModal__title">新建项目</div>

      <Form
        form={form}
        layout="vertical"
        requiredMark={false}
        initialValues={{ aspectRatio: '16:9', styleId: 1 }}
        className="v2-newProjectModal__form"
      >
        <Form.Item name="name" label="项目名称*" rules={[{ required: true, message: '请输入项目名称' }]}>
          <Input className="v2-newProjectModal__input" />
        </Form.Item>

        <Form.Item name="description" label="项目描述">
          <Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} className="v2-newProjectModal__input" />
        </Form.Item>

        <Form.Item name="aspectRatio" label="选择画面比例">
          <Radio.Group className="v2-newProjectModal__radio" options={[{ label: '16:9', value: '16:9' }, { label: '9：16', value: '9:16' }]} />
        </Form.Item>

        <Form.Item name="styleId" label="视觉风格">
          <div className="v2-newProjectModal__styleGrid">
            {styleOptions.slice(0, 5).map((s) => (
              <label key={s.id} className="v2-newProjectModal__styleItem">
                <Form.Item noStyle shouldUpdate={(p, c) => p.styleId !== c.styleId}>
                  {() => {
                    const selected = form.getFieldValue('styleId') === s.id;
                    return (
                      <div className={`v2-newProjectModal__styleThumb ${selected ? 'is-selected' : ''}`}>
                        <img src={styleThumbs[s.id]} alt={s.title} />
                      </div>
                    );
                  }}
                </Form.Item>
                <input
                  type="radio"
                  name="styleId"
                  value={s.id}
                  checked={form.getFieldValue('styleId') === s.id}
                  onChange={() => form.setFieldValue('styleId', s.id)}
                  className="v2-newProjectModal__styleRadio"
                />
              </label>
            ))}
          </div>
        </Form.Item>

        <Form.Item label="项目封面">
          <Upload
            fileList={fileList}
            onChange={({ fileList: next }) => setFileList(next.slice(-1))}
            beforeUpload={() => false}
            maxCount={1}
            listType="picture"
            className="v2-newProjectModal__upload"
          >
            <div className="v2-newProjectModal__uploadBox">上传</div>
          </Upload>
        </Form.Item>
      </Form>
    </Modal>
  );
}
