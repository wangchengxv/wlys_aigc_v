import test from 'node:test'
import assert from 'node:assert/strict'
import { SCRIPT_PROJECT_FEATURE_FLAGS } from '../../config/scriptProjectFeatureFlags.ts'
import {
  filterScriptProjectWorkflowSteps,
  getScriptProjectWorkflowFallbackPath,
  isScriptProjectWorkflowStepHidden,
} from './workflowFeatureGate.ts'

const POST_VIDEO_STEP_KEYS = ['dubbing', 'lip-sync', 'edit', 'export']

function withFeatureFlag(value, run) {
  const original = SCRIPT_PROJECT_FEATURE_FLAGS.enablePostVideoWorkflow
  SCRIPT_PROJECT_FEATURE_FLAGS.enablePostVideoWorkflow = value
  try {
    run()
  } finally {
    SCRIPT_PROJECT_FEATURE_FLAGS.enablePostVideoWorkflow = original
  }
}

test('后置流程关闭时隐藏步骤并拦截判定为真', () => {
  withFeatureFlag(false, () => {
    for (const key of POST_VIDEO_STEP_KEYS) {
      assert.equal(isScriptProjectWorkflowStepHidden(key), true)
    }
    const visibleStepKeys = filterScriptProjectWorkflowSteps(
      POST_VIDEO_STEP_KEYS.map((key) => ({ key })),
    ).map((step) => step.key)
    for (const key of POST_VIDEO_STEP_KEYS) {
      assert.equal(visibleStepKeys.includes(key), false)
    }
  })
})

test('后置流程开启时恢复步骤并放行判定', () => {
  withFeatureFlag(true, () => {
    for (const key of POST_VIDEO_STEP_KEYS) {
      assert.equal(isScriptProjectWorkflowStepHidden(key), false)
    }
    const visibleStepKeys = filterScriptProjectWorkflowSteps(
      POST_VIDEO_STEP_KEYS.map((key) => ({ key })),
    ).map((step) => step.key)
    for (const key of POST_VIDEO_STEP_KEYS) {
      assert.equal(visibleStepKeys.includes(key), true)
    }
  })
})

test('后置流程禁用时统一回落到视频页', () => {
  assert.equal(getScriptProjectWorkflowFallbackPath('p123'), '/script-projects/p123/video')
})
