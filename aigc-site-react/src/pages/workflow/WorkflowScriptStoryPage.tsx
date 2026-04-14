import { WorkflowScriptProjectHub } from '@/pages/workflow/WorkflowScriptProjectHub'

const STEPS = [
  {
    title: '创建并导入剧本',
    body: '新建工程并粘贴或上传剧本文档，完成解析与结构化。',
  },
  {
    title: '预览与改写',
    body: '在预览页查看分场、对白与故事线，按需使用改写建议。',
  },
  {
    title: '衔接后续工作流',
    body: '与「场景与道具」「镜头拆分与视频生成」衔接，继续资产与镜头生产。',
  },
]

export function WorkflowScriptStoryPage() {
  return (
    <WorkflowScriptProjectHub
      eyebrow="剧本与故事"
      title="从文本到结构化剧本"
      lede="在工程中粘贴或上传剧本，解析后在预览页查看分场、对白与故事线。"
      headerActions={[
        { label: '新建剧本工程', to: '/script-projects/new', variant: 'primary' },
        { label: '打开工程列表', to: '/script-projects' },
      ]}
      steps={STEPS}
      projectsTitle="从工程进入剧本预览"
      projectsHint="点击下方主按钮直达该工程的预览页，继续编辑与导入。"
      primaryCta={{
        label: '进入剧本预览',
        to: (id) => `/script-projects/${id}/preview`,
      }}
      emptyState={{
        title: '还没有剧本工程',
        description: '创建工程后即可导入剧本并查看结构化结果。',
        newProjectLabel: '新建剧本工程',
      }}
    />
  )
}
