type RenderTaskLike = {
  renderType?: string
  status?: string
  published?: boolean
  publishedAt?: string | null
  resultVideoFileId?: string | null
  draftVersion?: number
}

type VideoEditingDraftLike = {
  publishedVideoFileId?: string | null
  renderTasks?: RenderTaskLike[]
}

export function canPublishPreview(params: {
  hasUnsavedChanges: boolean
  draftVersion: number | null
  previewDraftVersion: number | null
}) {
  if (params.hasUnsavedChanges) return false
  if (!params.draftVersion || !params.previewDraftVersion) return false
  return params.draftVersion === params.previewDraftVersion
}

export function pickPreferredDeliverySource(
  videoEditingDraft: VideoEditingDraftLike | null,
  latestFinalCompositionTask?: { resultVideoFileId?: string | null; status?: string | null },
) {
  const publishedTask =
    videoEditingDraft?.renderTasks?.find((task) => task.published || (task.renderType === 'PUBLISH' && task.status === 'SUCCESS')) ??
    null
  const previewTask =
    videoEditingDraft?.renderTasks?.find((task) => task.renderType === 'PREVIEW' && task.status === 'SUCCESS') ?? null
  const publishedFileId = publishedTask?.resultVideoFileId || videoEditingDraft?.publishedVideoFileId || null

  if (publishedFileId) {
    return { source: 'published' as const, fileId: publishedFileId }
  }
  if (previewTask?.resultVideoFileId) {
    return { source: 'preview' as const, fileId: previewTask.resultVideoFileId }
  }
  if (latestFinalCompositionTask?.resultVideoFileId) {
    return { source: 'fallback' as const, fileId: latestFinalCompositionTask.resultVideoFileId }
  }
  return { source: 'none' as const, fileId: '' }
}
