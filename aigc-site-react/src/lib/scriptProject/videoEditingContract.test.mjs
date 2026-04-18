import test from 'node:test'
import assert from 'node:assert/strict'
import {
  mergeVideoEditingDraftWithTasks,
  normalizePipelineStatus,
  normalizeVideoEditingDraftResponse,
  normalizeVideoEditingRenderTask,
  toVideoEditingSaveDraftPayload,
} from './videoEditingContract.ts'

test('后端 draft 响应会转换为前端秒级剪辑草稿', () => {
  const draft = normalizeVideoEditingDraftResponse({
    projectId: 'p1',
    draftId: 'draft_1',
    version: 3,
    publishedAt: '2026-04-17T10:00:00Z',
    latestPreviewTaskId: 'preview_1',
    publishedRenderTaskId: 'publish_1',
    hasUnpublishedChanges: false,
    extensions: { tracks: 1 },
    segments: [
      {
        segmentId: 'seg_1',
        shotId: 'shot_1',
        sequenceNo: 1,
        enabled: true,
        sourceType: 'VIDEO',
        sourceFileId: 'file_1',
        sourceTaskId: 'task_1',
        sourceDurationMs: 6500,
        trimInMs: 500,
        trimOutMs: 6000,
        transitionMode: 'FADE',
        extensions: { lane: 'main' },
      },
    ],
  })

  assert.equal(draft.projectId, 'p1')
  assert.equal(draft.extension?.tracks, 1)
  assert.equal(draft.segments[0].sourceDurationSeconds, 6.5)
  assert.equal(draft.segments[0].trimInSeconds, 0.5)
  assert.equal(draft.segments[0].trimOutSeconds, 6)
  assert.equal(draft.segments[0].transitionMode, 'FADE')
  assert.deepEqual(draft.segments[0].extension, { lane: 'main' })
})

test('后端 render task 会从 taskType 和毫秒字段归一到前端结构', () => {
  const task = normalizeVideoEditingRenderTask({
    renderTaskId: 'vedit_1',
    projectId: 'p1',
    draftVersion: 4,
    taskType: 'PUBLISH',
    inputSegments: [
      {
        segmentId: 'seg_1',
        shotId: 'shot_1',
        sequenceNo: 1,
        sourceType: 'LIP_SYNC',
        sourceFileId: 'file_1',
        trimInMs: 0,
        trimOutMs: 4200,
        transitionMode: 'CUT',
      },
    ],
    resultVideoFileId: 'render_file',
    status: 'SUCCESS',
    publishedAt: '2026-04-17T11:00:00Z',
  })

  assert.equal(task.renderType, 'PUBLISH')
  assert.equal(task.published, true)
  assert.equal(task.inputSegments[0].trimOutSeconds, 4.2)
})

test('草稿会合并独立 render tasks，供剪辑页和导出页消费', () => {
  const draft = normalizeVideoEditingDraftResponse({
    projectId: 'p1',
    draftId: 'draft_1',
    version: 2,
    hasUnpublishedChanges: false,
    segments: [],
  })
  const merged = mergeVideoEditingDraftWithTasks(draft, [
    normalizeVideoEditingRenderTask({
      renderTaskId: 'preview_1',
      projectId: 'p1',
      draftVersion: 2,
      taskType: 'PREVIEW',
      inputSegments: [],
      status: 'SUCCESS',
      resultVideoFileId: 'preview_file',
      finishedAt: '2026-04-17T12:00:00Z',
    }),
    normalizeVideoEditingRenderTask({
      renderTaskId: 'publish_1',
      projectId: 'p1',
      draftVersion: 2,
      taskType: 'PUBLISH',
      inputSegments: [],
      status: 'SUCCESS',
      resultVideoFileId: 'publish_file',
      publishedAt: '2026-04-17T12:10:00Z',
      finishedAt: '2026-04-17T12:10:00Z',
    }),
  ])

  assert.equal(merged.renderTasks.length, 2)
  assert.equal(merged.publishedRenderTaskId, 'publish_1')
  assert.equal(merged.publishedVideoFileId, 'publish_file')
  assert.equal(merged.latestPreviewRenderTaskId, 'preview_1')
})

test('保存草稿请求会转换为后端 expectedVersion 和毫秒字段', () => {
  const payload = toVideoEditingSaveDraftPayload({
    version: 7,
    extension: { tracks: 1 },
    segments: [
      {
        segmentId: 'seg_1',
        shotId: 'shot_1',
        sequenceNo: 1,
        enabled: true,
        sourceType: 'VIDEO',
        sourceFileId: 'file_1',
        sourceTaskId: 'task_1',
        trimInSeconds: 0.25,
        trimOutSeconds: 5.5,
        transitionMode: 'CUT',
        transitionDurationSeconds: 0,
        notes: null,
        extension: { lane: 'main' },
      },
    ],
  })

  assert.equal(payload.expectedVersion, 7)
  assert.equal(payload.extensions.tracks, 1)
  assert.equal(payload.segments[0].trimInMs, 250)
  assert.equal(payload.segments[0].trimOutMs, 5500)
  assert.deepEqual(payload.segments[0].extensions, { lane: 'main' })
})

test('后端 PipelineStatusData 会映射为前端 videoEditing 统计字段', () => {
  const status = normalizePipelineStatus({
    projectId: 'p1',
    projectStatus: 'VIDEO_EDITING_READY',
    totalCount: 1,
    successCount: 1,
    failedCount: 0,
    runningCount: 0,
    queuedCount: 0,
    pendingCount: 0,
    videoEditDraftVersion: 5,
    videoEditRenderTaskCount: 2,
    videoEditRenderSuccessCount: 1,
    videoEditRenderFailedCount: 1,
    videoEditRenderRunningCount: 0,
    videoEditRenderQueuedCount: 0,
    videoEditRenderPendingCount: 0,
    videoEditHasPublishedResult: true,
    finalCompositionReady: false,
    exportPackageReady: false,
  })

  assert.equal(status.videoEditingDraftVersion, 5)
  assert.equal(status.videoEditingRenderTaskCount, 2)
  assert.equal(status.videoEditingSuccessCount, 1)
  assert.equal(status.videoEditingFailedCount, 1)
  assert.equal(status.videoEditingReady, true)
})
