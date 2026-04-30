import { SCRIPT_PROJECT_FEATURE_FLAGS, SCRIPT_PROJECT_POST_VIDEO_STEP_KEYS } from '../../config/scriptProjectFeatureFlags'

const SCRIPT_PROJECT_POST_VIDEO_STEP_KEY_SET = new Set<string>(SCRIPT_PROJECT_POST_VIDEO_STEP_KEYS)

export function isScriptProjectWorkflowStepHidden(stepKey: string) {
  if (SCRIPT_PROJECT_FEATURE_FLAGS.enablePostVideoWorkflow) {
    return false
  }
  return SCRIPT_PROJECT_POST_VIDEO_STEP_KEY_SET.has(stepKey)
}

export function filterScriptProjectWorkflowSteps<T extends { key: string }>(steps: T[]) {
  return steps.filter((step) => !isScriptProjectWorkflowStepHidden(step.key))
}

export function getScriptProjectWorkflowFallbackPath(projectId: string) {
  return `/script-projects/${projectId}/video`
}
