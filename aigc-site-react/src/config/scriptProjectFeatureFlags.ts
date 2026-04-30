export const SCRIPT_PROJECT_POST_VIDEO_STEP_KEYS = ['dubbing', 'lip-sync', 'edit', 'export'] as const

export const SCRIPT_PROJECT_FEATURE_FLAGS = {
  enablePostVideoWorkflow: false,
} as const

export type ScriptProjectPostVideoStepKey = (typeof SCRIPT_PROJECT_POST_VIDEO_STEP_KEYS)[number]
