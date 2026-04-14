import { WorkflowScriptProjectHub } from '@/pages/workflow/WorkflowScriptProjectHub'

const STEPS = [
  {
    title: '打开镜头拆分与视频生成',
    body: '在工程内进入该页，按分镜组织片段任务。',
  },
  {
    title: '配置与提交任务',
    body: '为各镜头选择模型与参数，提交并发生成任务。',
  },
  {
    title: '预览与迭代',
    body: '在时间线中预览衔接，结合历史记录回看与重试。',
  },
]

export function WorkflowDirectorPage() {
  return (
    <WorkflowScriptProjectHub
      eyebrow="导演模式"
      title="镜头调度与视频任务"
      lede="按分镜组织视频片段生成任务，预览时间线与镜头衔接。"
      headerActions={[
        { label: '选择剧本工程', to: '/script-projects', variant: 'primary' },
        { label: '新建工程', to: '/script-projects/new' },
      ]}
      steps={STEPS}
      projectsTitle="从工程进入镜头拆分与视频生成"
      projectsHint="点击下方主按钮直达该工程的视频页，调度镜头与任务。"
      primaryCta={{
        label: '进入视频生成',
        to: (id) => `/script-projects/${id}/video`,
      }}
      emptyState={{
        title: '还没有剧本工程',
        description: '创建工程并完成资产与分镜后，即可在此生成视频任务。',
      }}
    />
  )
}
