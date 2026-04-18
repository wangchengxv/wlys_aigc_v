import type {
  PipelineStatus,
  ScriptProjectAggregate,
  VideoEditingDraft,
  VideoEditingPublishRequest,
  VideoEditingDraftSegment,
  VideoEditingRenderTask,
  VideoEditingSaveDraftRequest,
  VideoEditingSourceType,
  VideoEditingTransitionMode,
} from '@/types'

type UnknownRecord = Record<string, unknown>

function asRecord(value: unknown): UnknownRecord {
  return value && typeof value === 'object' ? (value as UnknownRecord) : {}
}

function asString(value: unknown) {
  return typeof value === 'string' ? value : ''
}

function asNullableString(value: unknown) {
  return typeof value === 'string' && value ? value : null
}

function asNumber(value: unknown, fallback = 0) {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}

function asNullableNumber(value: unknown) {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

function toSeconds(value: unknown) {
  const milliseconds = asNullableNumber(value)
  return milliseconds == null ? 0 : Number((milliseconds / 1000).toFixed(3))
}

function toMilliseconds(value: number | null | undefined) {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0
  return Math.max(0, Math.round(value * 1000))
}

function normalizeTransitionMode(value: unknown): VideoEditingTransitionMode {
  if (value === 'FADE' || value === 'DIP_TO_BLACK') return value
  return 'CUT'
}

function normalizeSourceType(value: unknown): VideoEditingSourceType {
  return value === 'LIP_SYNC' ? 'LIP_SYNC' : 'VIDEO'
}

function normalizeRenderType(value: unknown): VideoEditingRenderTask['renderType'] {
  return value === 'PUBLISH' ? 'PUBLISH' : 'PREVIEW'
}

function normalizeStatus(value: unknown): VideoEditingRenderTask['status'] {
  if (value === 'QUEUED' || value === 'RUNNING' || value === 'SUCCESS' || value === 'FAILED') return value
  return 'PENDING'
}

function normalizeSegment(raw: unknown): VideoEditingDraftSegment {
  const record = asRecord(raw)
  const sourceDurationSeconds = asNullableNumber(record.sourceDurationSeconds) ?? toSeconds(record.sourceDurationMs)
  const transitionMode = normalizeTransitionMode(record.transitionMode)
  const transitionDurationSeconds =
    asNullableNumber(record.transitionDurationSeconds) ??
    (transitionMode === 'CUT' ? 0 : undefined)

  return {
    segmentId: asString(record.segmentId),
    shotId: asString(record.shotId),
    sequenceNo: asNumber(record.sequenceNo, 0),
    enabled: record.enabled !== false,
    sourceType: normalizeSourceType(record.sourceType),
    sourceFileId: asString(record.sourceFileId),
    sourceTaskId: asNullableString(record.sourceTaskId),
    sourceDurationSeconds,
    trimInSeconds: asNullableNumber(record.trimInSeconds) ?? toSeconds(record.trimInMs),
    trimOutSeconds:
      asNullableNumber(record.trimOutSeconds) ??
      (record.trimOutMs != null ? toSeconds(record.trimOutMs) : sourceDurationSeconds ?? 0),
    transitionMode,
    transitionDurationSeconds,
    notes: asNullableString(record.notes),
    extension: (record.extension ?? record.extensions ?? null) as Record<string, unknown> | null,
    availableSources: Array.isArray(record.availableSources) ? (record.availableSources as VideoEditingDraftSegment['availableSources']) : [],
  }
}

function normalizeRenderSegment(raw: unknown): VideoEditingRenderTask['inputSegments'][number] {
  const segment = normalizeSegment(raw)
  return {
    segmentId: segment.segmentId,
    shotId: segment.shotId,
    sequenceNo: segment.sequenceNo,
    sourceType: segment.sourceType,
    sourceFileId: segment.sourceFileId,
    sourceTaskId: segment.sourceTaskId,
    trimInSeconds: segment.trimInSeconds,
    trimOutSeconds: segment.trimOutSeconds,
    transitionMode: segment.transitionMode,
    transitionDurationSeconds: segment.transitionDurationSeconds,
  }
}

function sortRenderTasks(tasks: VideoEditingRenderTask[]) {
  return [...tasks].sort((left, right) => {
    const leftTime = new Date(right.finishedAt || right.startedAt || 0).getTime()
    const rightTime = new Date(left.finishedAt || left.startedAt || 0).getTime()
    return leftTime - rightTime
  })
}

export function normalizeVideoEditingRenderTask(raw: unknown): VideoEditingRenderTask {
  const record = asRecord(raw)
  const renderType = normalizeRenderType(record.renderType ?? record.taskType)
  const publishedAt = asNullableString(record.publishedAt)
  return {
    renderTaskId: asString(record.renderTaskId),
    projectId: asString(record.projectId),
    draftId: asNullableString(record.draftId),
    draftVersion: asNumber(record.draftVersion, 0),
    renderType,
    inputSegments: Array.isArray(record.inputSegments) ? record.inputSegments.map(normalizeRenderSegment) : [],
    requestPayloadFileId: asNullableString(record.requestPayloadFileId),
    resultVideoFileId: asNullableString(record.resultVideoFileId),
    providerTaskId: asNullableString(record.providerTaskId),
    modelName: asNullableString(record.modelName),
    status: normalizeStatus(record.status),
    retryCount: asNumber(record.retryCount, 0),
    published: Boolean(record.published) || renderType === 'PUBLISH' || !!publishedAt,
    publishedAt,
    startedAt: asNullableString(record.startedAt),
    finishedAt: asNullableString(record.finishedAt),
    errorMessage: asNullableString(record.errorMessage),
  }
}

export function mergeVideoEditingDraftWithTasks(
  draft: VideoEditingDraft,
  renderTasks: VideoEditingRenderTask[],
): VideoEditingDraft {
  const nextTasks = sortRenderTasks(renderTasks)
  const publishedTask =
    nextTasks.find((task) => task.published || (task.renderType === 'PUBLISH' && task.status === 'SUCCESS')) ?? null
  const previewTask = nextTasks.find((task) => task.renderType === 'PREVIEW') ?? null

  return {
    ...draft,
    renderTasks: nextTasks,
    latestPreviewRenderTaskId: draft.latestPreviewRenderTaskId ?? previewTask?.renderTaskId ?? null,
    publishedRenderTaskId: draft.publishedRenderTaskId ?? publishedTask?.renderTaskId ?? null,
    publishedAt: draft.publishedAt ?? publishedTask?.publishedAt ?? null,
    publishedVideoFileId: draft.publishedVideoFileId ?? publishedTask?.resultVideoFileId ?? null,
  }
}

export function normalizeVideoEditingDraftResponse(
  raw: unknown,
  renderTasks: VideoEditingRenderTask[] = [],
): VideoEditingDraft {
  const record = asRecord(raw)
  const draft: VideoEditingDraft = {
    draftId: asString(record.draftId),
    projectId: asString(record.projectId),
    version: asNumber(record.version, 1),
    publishedVersion: asNullableNumber(record.publishedVersion),
    hasPublishedResult: Boolean(record.hasPublishedResult),
    hasUnpublishedChanges: Boolean(record.hasUnpublishedChanges),
    lastSavedAt: asNullableString(record.lastSavedAt ?? record.updatedAt),
    publishedAt: asNullableString(record.publishedAt),
    publishedRenderTaskId: asNullableString(record.publishedRenderTaskId),
    latestPreviewRenderTaskId: asNullableString(record.latestPreviewRenderTaskId ?? record.latestPreviewTaskId),
    latestPublishTaskId: asNullableString(record.latestPublishTaskId),
    publishedVideoFileId: asNullableString(record.publishedVideoFileId),
    extension: (record.extension ?? record.extensions ?? null) as Record<string, unknown> | null,
    segments: Array.isArray(record.segments) ? record.segments.map(normalizeSegment) : [],
    renderTasks: [],
  }
  return mergeVideoEditingDraftWithTasks(draft, renderTasks)
}

export function toVideoEditingSaveDraftPayload(payload: VideoEditingSaveDraftRequest) {
  return {
    expectedVersion: payload.version ?? null,
    extensions: payload.extension ?? null,
    segments: payload.segments.map((segment) => ({
      segmentId: segment.segmentId ?? null,
      shotId: segment.shotId,
      sequenceNo: segment.sequenceNo,
      enabled: segment.enabled,
      sourceType: segment.sourceType,
      sourceFileId: segment.sourceFileId,
      sourceTaskId: segment.sourceTaskId ?? null,
      trimInMs: toMilliseconds(segment.trimInSeconds),
      trimOutMs: toMilliseconds(segment.trimOutSeconds),
      transitionMode: segment.transitionMode,
      extensions: segment.extension ?? null,
    })),
  }
}

export function toVideoEditingPublishPayload(payload?: VideoEditingPublishRequest | null) {
  return {
    draftVersion: asNullableNumber(payload?.draftVersion) ?? null,
    renderTaskId: asNullableString(payload?.renderTaskId) ?? null,
  }
}

export function normalizePipelineStatus(raw: unknown): PipelineStatus {
  const record = asRecord(raw)
  const videoEditingReady =
    Boolean(record.videoEditingReady) ||
    Boolean(record.videoEditHasPublishedResult) ||
    asString(record.projectStatus) === 'VIDEO_EDITING_READY'

  return {
    ...(record as unknown as PipelineStatus),
    videoEditingDraftVersion: asNullableNumber(record.videoEditingDraftVersion) ?? asNullableNumber(record.videoEditDraftVersion) ?? undefined,
    videoEditingRenderTaskCount: asNullableNumber(record.videoEditingRenderTaskCount) ?? asNullableNumber(record.videoEditRenderTaskCount) ?? undefined,
    videoEditingSuccessCount: asNullableNumber(record.videoEditingSuccessCount) ?? asNullableNumber(record.videoEditRenderSuccessCount) ?? undefined,
    videoEditingFailedCount: asNullableNumber(record.videoEditingFailedCount) ?? asNullableNumber(record.videoEditRenderFailedCount) ?? undefined,
    videoEditingRunningCount: asNullableNumber(record.videoEditingRunningCount) ?? asNullableNumber(record.videoEditRenderRunningCount) ?? undefined,
    videoEditingQueuedCount: asNullableNumber(record.videoEditingQueuedCount) ?? asNullableNumber(record.videoEditRenderQueuedCount) ?? undefined,
    videoEditingPendingCount: asNullableNumber(record.videoEditingPendingCount) ?? asNullableNumber(record.videoEditRenderPendingCount) ?? undefined,
    videoEditingReady,
  }
}

export function normalizeScriptProjectAggregate(raw: unknown): ScriptProjectAggregate {
  const record = asRecord(raw)
  const renderTasks = Array.isArray(record.videoEditRenderTasks)
    ? record.videoEditRenderTasks.map(normalizeVideoEditingRenderTask)
    : Array.isArray(record.videoEditingRenderTasks)
      ? record.videoEditingRenderTasks.map(normalizeVideoEditingRenderTask)
      : []

  const draftSource = record.videoEditingDraft ?? record.videoEditDraft ?? null

  return {
    ...(record as unknown as ScriptProjectAggregate),
    videoEditingDraft: draftSource ? normalizeVideoEditingDraftResponse(draftSource, renderTasks) : null,
  }
}
