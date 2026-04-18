import { WorkflowScriptProjectHub } from '@/pages/workflow/WorkflowScriptProjectHub'

const STEPS = [
  {
    title: '先完成镜头拆分',
    body: '在工程内完成剧本预览、资产准备与镜头拆分，确保每镜都有可用文本。',
  },
  {
    title: '批量生成项目配音',
    body: '进入项目级「配音管理」页，为整项目批量生成配音任务并配置 TTS 模型、音色、语言与语速。',
  },
  {
    title: '试听后进入导出',
    body: '在配音页试听音频、处理失败任务，再进入「成片与导出」查看视频与配音准备状态。',
  },
]

export function WorkflowDubbingPage() {
  return (
    <WorkflowScriptProjectHub
      eyebrow="配音与旁白"
      title="项目级配音任务管理"
      lede="按镜头批量生成配音任务，集中试听、查看失败原因并重试，为最终导出准备音频素材。"
      headerActions={[
        { label: '选择剧本工程', to: '/script-projects', variant: 'primary' },
        { label: '新建工程', to: '/script-projects/new' },
      ]}
      steps={STEPS}
      projectsTitle="从工程进入配音管理"
      projectsHint="点击下方主按钮直达该工程的配音页，批量生成并试听镜头音频。"
      primaryCta={{
        label: '进入配音管理',
        to: (id) => `/script-projects/${id}/dubbing`,
      }}
      secondaryCta={{
        label: '进入成片与导出',
        to: (id) => `/script-projects/${id}/export`,
      }}
      emptyState={{
        title: '还没有剧本工程',
        description: '创建工程并完成镜头拆分后，即可在此批量生成项目配音。',
      }}
    />
  )
}
