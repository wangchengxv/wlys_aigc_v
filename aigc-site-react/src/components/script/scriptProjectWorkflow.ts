import { filterScriptProjectWorkflowSteps } from '@/lib/scriptProject/workflowFeatureGate'

export type ScriptProjectWorkflowStep = {
  key: string
  label: string
  hint: string
  to: (projectId: string) => string
}

const ALL_SCRIPT_PROJECT_WORKFLOW_STEPS: ScriptProjectWorkflowStep[] = [
  {
    key: 'global-settings',
    label: '全局设定',
    hint: '确认画幅、风格与模型偏好',
    to: (id) => `/script-projects/${id}/global-settings`,
  },
  {
    key: 'overview',
    label: '总览',
    hint: '项目全局状态',
    to: (id) => `/script-projects/${id}`,
  },
  {
    key: 'script',
    label: '剧本',
    hint: '完善剧本内容',
    to: (id) => `/script-projects/${id}/preview`,
  },
  {
    key: 'assets',
    label: '资产',
    hint: '生成角色道具',
    to: (id) => `/script-projects/${id}/assets`,
  },
  {
    key: 'video',
    label: '视频',
    hint: '生成视频分段',
    to: (id) => `/script-projects/${id}/video`,
  },
  {
    key: 'dubbing',
    label: '配音',
    hint: '添加语音旁白',
    to: (id) => `/script-projects/${id}/dubbing`,
  },
  {
    key: 'lip-sync',
    label: '口型',
    hint: '同步口型动画',
    to: (id) => `/script-projects/${id}/lip-sync`,
  },
  {
    key: 'edit',
    label: '剪辑',
    hint: '合成最终成片',
    to: (id) => `/script-projects/${id}/final-composition`,
  },
  {
    key: 'export',
    label: '导出',
    hint: '打包交付文件',
    to: (id) => `/script-projects/${id}/export`,
  },
]

export function getScriptProjectWorkflowSteps() {
  return filterScriptProjectWorkflowSteps(ALL_SCRIPT_PROJECT_WORKFLOW_STEPS)
}

export const scriptProjectWorkflowSteps: ScriptProjectWorkflowStep[] = getScriptProjectWorkflowSteps()

export function getScriptProjectWorkflowCurrentIndex(pathname: string, projectId: string) {
  return scriptProjectWorkflowSteps.findIndex((step) => {
    const target = step.to(projectId).replace(/\/$/, '')
    const current = pathname.replace(/\/$/, '')
    return current === target || current.startsWith(`${target}/`)
  })
}
