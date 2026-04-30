import { WorkflowScriptProjectHub } from '@/pages/workflow/WorkflowScriptProjectHub'

const STEPS = [
  {
    title: '进入工程资产页',
    body: '从工程列表进入详情，再进入「资产与关键帧」页面；或在此直接选择工程。',
  },
  {
    title: '按流程推进资产',
    body: '按“视觉提示词 -> 三视图 -> 九宫格 -> 关键帧”的顺序推进，已完成步骤会高亮显示。',
  },
  {
    title: '自动保存并复用',
    body: '资产与关键帧编辑默认自动保存，确认后可在「镜头拆分与视频生成」与「成片与导出」直接复用。',
  },
]

export function WorkflowScenesPropsPage() {
  return (
    <WorkflowScriptProjectHub
      eyebrow="场景与道具"
      title="资产、关键帧与场景物料"
      lede="在单个工程内维护参考图、关键帧与道具设定，为视频生成提供一致视觉。"
      headerActions={[
        { label: '选择剧本工程', to: '/script-projects', variant: 'primary' },
        { label: '新建工程', to: '/script-projects/new' },
      ]}
      steps={STEPS}
      projectsTitle="从工程进入资产与关键帧"
      projectsHint="点击下方主按钮直达该工程的资产页。"
      primaryCta={{
        label: '进入资产与关键帧',
        to: (id) => `/script-projects/${id}/assets`,
      }}
      emptyState={{
        title: '还没有剧本工程',
        description: '创建工程后即可在此维护参考图、关键帧与道具设定。',
      }}
    />
  )
}
