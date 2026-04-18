import { WorkflowScriptProjectHub } from '@/pages/workflow/WorkflowScriptProjectHub'

const STEPS = [
  {
    title: '收集静帧与参考',
    body: '在「资产与关键帧」中确认画面并导出所需静帧与参考图。',
  },
  {
    title: '生成并汇总视频片段',
    body: '在工程内「镜头拆分与视频生成」中启动任务；完成后进入「成片与导出」页按镜预览、复制直链或新窗口打开。',
  },
  {
    title: '确认配音准备状态',
    body: '如需旁白或镜头配音，先在「配音管理」页试听并处理失败任务，再回到导出页确认准备状态。',
  },
  {
    title: '统一回看与检索',
    body: '工程内成片以本页为主；工作台单次任务可在「历史记录」中跨任务检索。',
  },
]

export function WorkflowExportPage() {
  return (
    <WorkflowScriptProjectHub
      eyebrow="成片与导出"
      title="汇总素材与成片输出"
      lede="在资产页、视频页和配音页准备素材后，进入各工程的「成片与导出」页汇总链接，并确认配音是否已就绪。"
      headerActions={[
        { label: '选择剧本工程', to: '/script-projects', variant: 'primary' },
        { label: '历史记录', to: '/history' },
        { label: '新建工程', to: '/script-projects/new' },
      ]}
      steps={STEPS}
      projectsTitle="从工程进入视频与成片"
      projectsHint="主按钮进入视频页生成片段；第二按钮进入该工程的成片与导出页。静帧与参考请从各工程的资产页导出。"
      primaryCta={{
        label: '进入视频生成',
        to: (id) => `/script-projects/${id}/video`,
      }}
      secondaryCta={{
        label: '工程成片与导出',
        to: (id) => `/script-projects/${id}/export`,
      }}
      emptyState={{
        title: '还没有剧本工程',
        description: '创建工程并生成视频任务后，可在工程成片页汇总链接并在历史记录中回看。',
      }}
    />
  )
}
