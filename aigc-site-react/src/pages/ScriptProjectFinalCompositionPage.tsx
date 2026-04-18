import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PipelineProgressBar } from '@/components/script/PipelineProgressBar'
import { ProjectSubpageShell } from '@/components/script/ProjectSubpageShell'
import { useToast } from '@/context/ToastContext'
import { canPublishPreview } from '@/lib/scriptProject/videoEditingPageGuards'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type {
  FinalCompositionTask,
  PipelineStatus,
  StoryboardShot,
  VideoEditingDraft,
  VideoEditingDraftSegment,
  VideoEditingPublishRequest,
  VideoEditingRenderTask,
} from '@/types'

export function ScriptProjectFinalCompositionPage() {
  const { projectId = '' } = useParams()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const shots = useScriptProjectStore((s) => s.shots)
  const videoTasks = useScriptProjectStore((s) => s.videoTasks)
  const lipSyncTasks = useScriptProjectStore((s) => s.lipSyncTasks)
  const videoEditingDraft = useScriptProjectStore((s) => s.videoEditingDraft)
  const finalCompositionTasks = useScriptProjectStore((s) => s.finalCompositionTasks)
  const pipelineStatus = useScriptProjectStore((s) => s.pipelineStatus)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const videoEditingLoading = useScriptProjectStore((s) => s.videoEditingLoading)
  const videoEditingSaving = useScriptProjectStore((s) => s.videoEditingSaving)
  const videoEditingResetting = useScriptProjectStore((s) => s.videoEditingResetting)
  const videoEditingRendering = useScriptProjectStore((s) => s.videoEditingRendering)
  const videoEditingPublishing = useScriptProjectStore((s) => s.videoEditingPublishing)
  const finalCompositionLoading = useScriptProjectStore((s) => s.finalCompositionLoading)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadShots = useScriptProjectStore((s) => s.loadShots)
  const loadVideoTasks = useScriptProjectStore((s) => s.loadVideoTasks)
  const loadLipSyncTasks = useScriptProjectStore((s) => s.loadLipSyncTasks)
  const loadVideoEditingDraft = useScriptProjectStore((s) => s.loadVideoEditingDraft)
  const saveVideoEditingDraft = useScriptProjectStore((s) => s.saveVideoEditingDraft)
  const resetVideoEditingDraft = useScriptProjectStore((s) => s.resetVideoEditingDraft)
  const renderVideoEditingPreview = useScriptProjectStore((s) => s.renderVideoEditingPreview)
  const publishVideoEditingComposition = useScriptProjectStore((s) => s.publishVideoEditingComposition)
  const retryVideoEditingRenderTask = useScriptProjectStore((s) => s.retryVideoEditingRenderTask)
  const loadFinalCompositionTasks = useScriptProjectStore((s) => s.loadFinalCompositionTasks)
  const loadPipelineStatus = useScriptProjectStore((s) => s.loadPipelineStatus)
  const startFinalComposition = useScriptProjectStore((s) => s.startFinalComposition)
  const retryFinalCompositionTask = useScriptProjectStore((s) => s.retryFinalCompositionTask)
  const startPolling = useScriptProjectStore((s) => s.startPolling)
  const stopPolling = useScriptProjectStore((s) => s.stopPolling)
  const [draftForm, setDraftForm] = useState<VideoEditingDraft | null>(null)

  const latestTask = finalCompositionTasks[0]
  const resultVideoUrl = resolveScriptFileUrl(latestTask?.resultVideoFileId)
  const fallbackVideoCount = videoTasks.filter((task) => task.status === 'SUCCESS' && !!task.resultVideoFileId).length
  const lipSyncReadyCount = lipSyncTasks.filter((task) => task.status === 'SUCCESS' && !!task.resultVideoFileId).length
  const availableDraft = draftForm ?? videoEditingDraft

  useEffect(() => {
    if (!projectId) return
    void (async () => {
      await Promise.all([loadProject(projectId), loadShots(projectId), loadVideoTasks(projectId), loadLipSyncTasks(projectId)])
      await Promise.all([loadVideoEditingDraft(projectId), loadFinalCompositionTasks(projectId), loadPipelineStatus(projectId)])
      const st = useScriptProjectStore.getState().pipelineStatus
      if (st?.projectStatus === 'VIDEO_EDITING_RENDERING' || st?.projectStatus === 'FINAL_COMPOSITION_GENERATING') {
        startPolling(projectId)
      }
    })()
    return () => stopPolling()
  }, [
    projectId,
    loadFinalCompositionTasks,
    loadLipSyncTasks,
    loadPipelineStatus,
    loadProject,
    loadShots,
    loadVideoTasks,
    loadVideoEditingDraft,
    startPolling,
    stopPolling,
  ])

  useEffect(() => {
    if (!videoEditingDraft) {
      setDraftForm(null)
      return
    }
    setDraftForm((previous) => {
      if (!previous) return cloneDraft(videoEditingDraft)
      const previousPayload = serializeDraftPayload(previous)
      const storePayload = serializeDraftPayload(videoEditingDraft)
      return previousPayload === storePayload ? cloneDraft(videoEditingDraft) : previous
    })
  }, [videoEditingDraft])

  async function startComposition() {
    try {
      await startFinalComposition(projectId)
      showToast('已启动项目级成片编排', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '成片编排启动失败', 'error')
    }
  }

  async function saveDraft() {
    if (!availableDraft) return null
    const errors = validateDraft(availableDraft)
    if (errors.length) {
      showToast(errors[0], 'error')
      return null
    }
    try {
      const saved = await saveVideoEditingDraft(projectId, toSavePayload(availableDraft))
      setDraftForm(cloneDraft(saved))
      showToast('剪辑草稿已保存', 'success')
      return saved
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存剪辑草稿失败', 'error')
      return null
    }
  }

  async function handleResetDraft() {
    try {
      const resetDraft = await resetVideoEditingDraft(projectId)
      setDraftForm(cloneDraft(resetDraft))
      showToast('已恢复默认剪辑草稿', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '重置默认草稿失败', 'error')
    }
  }

  async function handleRenderPreview() {
    if (!availableDraft) return
    const saved = hasUnsavedChanges && availableDraft ? await saveDraft() : videoEditingDraft
    if (!saved) return
    try {
      await renderVideoEditingPreview(projectId, { draftVersion: saved.version })
      showToast('已发起剪辑预览渲染', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '发起剪辑预览失败', 'error')
    }
  }

  async function handlePublish() {
    if (!publishCandidate) {
      showToast('请先生成与当前草稿一致的成功预览，再执行发布', 'error')
      return
    }
    try {
      const payload: VideoEditingPublishRequest = {
        draftVersion: videoEditingDraft?.version ?? availableDraft?.version,
        renderTaskId: publishCandidate.renderTaskId,
      }
      await publishVideoEditingComposition(projectId, payload)
      showToast('已发布剪辑成片，导出页会优先消费该版本', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '发布剪辑成片失败', 'error')
    }
  }

  async function handleRetryRender(taskId: string) {
    try {
      await retryVideoEditingRenderTask(projectId, taskId)
      showToast('剪辑渲染任务已重新加入队列', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '重试剪辑渲染失败', 'error')
    }
  }

  async function retryComposition(taskId: string) {
    try {
      await retryFinalCompositionTask(projectId, taskId)
      showToast('成片任务已重新加入队列', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '重试失败', 'error')
    }
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请返回列表重新选择项目。" />
  }

  const project = currentProject.project
  const hasUnsavedChanges =
    !!availableDraft && !!videoEditingDraft && serializeDraftPayload(availableDraft) !== serializeDraftPayload(videoEditingDraft)
  const validationErrors = availableDraft ? validateDraft(availableDraft) : []
  const renderTasks = videoEditingDraft?.renderTasks ?? []
  const latestPreviewTask = renderTasks.find((task) => task.renderType === 'PREVIEW')
  const latestSuccessfulPreviewTask = renderTasks.find((task) => task.renderType === 'PREVIEW' && task.status === 'SUCCESS')
  const latestPublishedTask = renderTasks.find((task) => task.published || (task.renderType === 'PUBLISH' && task.status === 'SUCCESS'))
  const publishedResultUrl = resolveScriptFileUrl(latestPublishedTask?.resultVideoFileId || videoEditingDraft?.publishedVideoFileId)
  const publishCandidate = resolvePublishCandidate({
    hasUnsavedChanges,
    draftVersion: videoEditingDraft?.version ?? availableDraft?.version ?? null,
    latestSuccessfulPreviewTask,
  })
  const previewTaskOutdated =
    !!latestSuccessfulPreviewTask &&
    !!videoEditingDraft &&
    latestSuccessfulPreviewTask.draftVersion !== videoEditingDraft.version
  const activeResultUrl =
    publishedResultUrl ||
    resolveScriptFileUrl(latestSuccessfulPreviewTask?.resultVideoFileId) ||
    resultVideoUrl
  const activeResultMode = publishedResultUrl
    ? 'published'
    : latestSuccessfulPreviewTask
      ? 'preview'
      : latestTask?.resultVideoFileId
        ? 'fallback'
        : 'none'
  const editingPipeline = buildEditingPipeline(projectId, renderTasks, pipelineStatus, latestPublishedTask)
  const enabledSegments = availableDraft?.segments.filter((segment) => segment.enabled) ?? []
  const lipSyncSegmentCount = enabledSegments.filter((segment) => segment.sourceType === 'LIP_SYNC').length
  const fallbackSegmentCount = enabledSegments.filter((segment) => segment.sourceType === 'VIDEO').length

  return (
    <ProjectSubpageShell
      projectId={projectId}
      title="视频剪辑工作台"
      description="在项目上下文内完成单轨时间线编辑、预览渲染和发布成片；导出页会优先消费最近一次已发布的剪辑结果。"
      meta={
        <>
          <span className="soft-badge">{project.name}</span>
          <span className={`soft-badge ${latestPublishedTask ? 'is-success' : ''}`}>
            {latestPublishedTask ? '已发布剪辑成片' : latestSuccessfulPreviewTask ? '预览已生成待发布' : '待生成剪辑预览'}
          </span>
          <span className={`soft-badge ${hasUnsavedChanges || availableDraft?.hasUnpublishedChanges ? '' : 'is-success'}`}>
            {hasUnsavedChanges ? '有未保存编辑' : availableDraft?.hasUnpublishedChanges ? '有未发布变更' : '发布状态已同步'}
          </span>
        </>
      }
      stats={[
        { key: 'shots', label: '镜头总数', value: shots.length },
        { key: 'enabled', label: '启用片段', value: enabledSegments.length },
        { key: 'lip-sync', label: '口型来源', value: lipSyncSegmentCount, hint: `${lipSyncReadyCount} 镜可用` },
        { key: 'fallback', label: '视频回退', value: fallbackSegmentCount, hint: `${fallbackVideoCount} 镜可回退` },
        { key: 'tasks', label: '渲染任务', value: renderTasks.length },
      ]}
      toolbar={
        <div className="project-subpage-shell__toolbar-head">
          <div className="project-subpage-shell__toolbar-copy">
            <p className="eyebrow">Video Editing</p>
            <h3>保存、预览、发布</h3>
            <p className="muted">
              {latestPublishedTask
                ? `最近发布于 ${formatDateTime(latestPublishedTask.publishedAt || videoEditingDraft?.publishedAt)}，导出与交付会优先消费该版本。`
                : latestSuccessfulPreviewTask
                  ? '预览已生成但尚未发布，确认无误后再推送到交付链路。'
                  : '首次进入会按镜头顺序生成默认草稿，优先选口型同步结果，缺失时回退到镜头视频。'}
            </p>
          </div>
          <div className="project-subpage-shell__toolbar-actions">
            <AppButton variant="ghost" loading={videoEditingSaving} onClick={() => void saveDraft()}>
              保存草稿
            </AppButton>
            <AppButton variant="ghost" loading={videoEditingResetting} onClick={() => void handleResetDraft()}>
              恢复默认
            </AppButton>
            <AppButton variant="primary" loading={videoEditingRendering} onClick={() => void handleRenderPreview()}>
              生成剪辑预览
            </AppButton>
            <AppButton
              variant="primary"
              loading={videoEditingPublishing}
              disabled={!publishCandidate}
              onClick={() => void handlePublish()}
            >
              发布成片
            </AppButton>
            <Link className="nav-btn" to={`/script-projects/${projectId}/export`}>
              前往导出与交付
            </Link>
          </div>
        </div>
      }
      helpTitle="查看剪辑工作台说明"
      help={
        <>
          <p>本次只做单轨剪辑 MVP，支持顺序调整、启用状态、来源切换、基础裁切和简单转场，不做浏览器内非线编。</p>
          <p>如果项目已有自动成片但尚未发布剪辑版，导出页仍会保留自动成片回退，以免阻断既有交付链路。</p>
        </>
      }
    >
      {editingPipeline ? <PipelineProgressBar pipeline={editingPipeline} /> : null}

      {validationErrors.length ? (
        <section className="panel glass video-editing-alert">
          <div>
            <p className="eyebrow">Draft Validation</p>
            <h3>当前草稿还有待修正项</h3>
          </div>
          <div className="video-editing-alert__list">
            {validationErrors.map((item) => (
              <span key={item} className="soft-badge">
                {item}
              </span>
            ))}
          </div>
        </section>
      ) : null}

      <section className="panel glass video-editing-result">
        <div className="delivery-section-card__head">
          <div>
            <p className="eyebrow">Render Result</p>
            <h3>预览与发布结果</h3>
          </div>
          <span className={`delivery-status-badge delivery-status-badge--${activeResultMode === 'published' ? 'success' : activeResultMode === 'preview' ? 'info' : activeResultMode === 'fallback' ? 'neutral' : 'warning'}`}>
            {activeResultMode === 'published'
              ? '已发布剪辑成片'
              : activeResultMode === 'preview'
                ? '最新剪辑预览'
                : activeResultMode === 'fallback'
                  ? '自动成片回退'
                  : '暂无结果'}
          </span>
        </div>
        {activeResultUrl ? (
          <video className="video" src={activeResultUrl} controls preload="metadata" />
        ) : (
          <div className="video placeholder muted">保存草稿后可发起预览渲染；发布成功后，导出页会优先展示已发布的剪辑成片。</div>
        )}
        <div className="video-editing-result__meta muted">
          <span>草稿版本 {videoEditingDraft?.version ?? availableDraft?.version ?? '-'}</span>
          <span>已发布 {latestPublishedTask ? formatDateTime(latestPublishedTask.publishedAt || videoEditingDraft?.publishedAt) : '否'}</span>
          <span>
            预览状态{' '}
            {latestPreviewTask ? `${latestPreviewTask.status}${previewTaskOutdated ? '（非当前草稿）' : ''}` : '未生成'}
          </span>
          <span>自动成片回退 {latestTask?.resultVideoFileId ? '可用' : '不可用'}</span>
        </div>
        {latestPreviewTask && !latestPublishedTask ? (
          <div className="delivery-feedback-note">
            预览已生成但尚未发布。只有发布后的剪辑成片才会被导出页与交付状态优先消费。
          </div>
        ) : null}
        {latestPublishedTask?.errorMessage ? <p className="muted">发布异常：{latestPublishedTask.errorMessage}</p> : null}
      </section>

      {detailLoading || (videoEditingLoading && !availableDraft) ? (
        <LoadingSpinner />
      ) : availableDraft?.segments.length ? (
        <>
          <section className="panel glass video-editing-workbench">
            <div className="delivery-section-card__head">
              <div>
                <p className="eyebrow">Single Track Timeline</p>
                <h3>片段列表编辑</h3>
              </div>
              <span className={`delivery-status-badge delivery-status-badge--${hasUnsavedChanges ? 'warning' : availableDraft.hasUnpublishedChanges ? 'info' : 'success'}`}>
                {hasUnsavedChanges ? '有未保存编辑' : availableDraft.hasUnpublishedChanges ? '有未发布变更' : '草稿已发布同步'}
              </span>
            </div>
            <div className="video-editing-workbench__list">
              {availableDraft.segments.map((segment, index) => (
                <EditingSegmentCard
                  key={segment.segmentId}
                  shot={shots.find((item) => item.shotId === segment.shotId)}
                  segment={segment}
                  index={index}
                  total={availableDraft.segments.length}
                  disabled={videoEditingSaving || videoEditingRendering || videoEditingPublishing}
                  onMove={(direction) =>
                    setDraftForm((draft) => (draft ? moveSegment(draft, index, direction) : draft))
                  }
                  onChange={(patch) =>
                    setDraftForm((draft) =>
                      draft
                        ? {
                            ...draft,
                            hasUnpublishedChanges: true,
                            segments: draft.segments.map((item) =>
                              item.segmentId === segment.segmentId
                                ? normalizeEditedSegment({ ...item, ...patch }, shots.find((shot) => shot.shotId === item.shotId))
                                : item,
                            ),
                          }
                        : draft,
                    )
                  }
                />
              ))}
            </div>
          </section>

          <section className="panel glass video-editing-workbench">
            <div className="delivery-section-card__head">
              <div>
                <p className="eyebrow">Render History</p>
                <h3>渲染任务与自动成片回退</h3>
              </div>
            </div>
            {renderTasks.length ? (
              <div className="video-editing-render-list">
                {renderTasks.map((task) => (
                  <article key={task.renderTaskId} className="segment panel glass">
                    <div className="head">
                      <div>
                        <p className="eyebrow">{task.renderType === 'PUBLISH' ? 'Publish' : 'Preview'}</p>
                        <h3>{task.renderTaskId}</h3>
                      </div>
                      <span className="status">{task.status}</span>
                    </div>
                    <div className="meta">
                      <span>草稿版本 {task.draftVersion}</span>
                      <span>片段 {task.inputSegments.length}</span>
                      <span>发布时间 {task.published ? formatDateTime(task.publishedAt) : '未发布'}</span>
                    </div>
                    {task.errorMessage ? <p className="muted">失败原因：{task.errorMessage}</p> : null}
                    <div className="footer" style={{ justifyContent: 'space-between', gap: 8 }}>
                      <span className="muted">{task.providerTaskId ? `Provider Task: ${task.providerTaskId}` : '尚未分配 providerTaskId'}</span>
                      <AppButton
                        size="sm"
                        variant="ghost"
                        disabled={task.status !== 'FAILED'}
                        loading={videoEditingRendering && task.status === 'FAILED'}
                        onClick={() => void handleRetryRender(task.renderTaskId)}
                      >
                        重试剪辑渲染
                      </AppButton>
                    </div>
                  </article>
                ))}
              </div>
            ) : latestTask ? (
              <FinalCompositionTaskCard
                task={latestTask}
                resultVideoUrl={resultVideoUrl}
                busy={finalCompositionLoading}
                onRetry={() => void retryComposition(latestTask.finalCompositionTaskId)}
              />
            ) : (
              <p className="muted">还没有剪辑渲染任务。你可以先保存草稿，再生成预览或继续使用旧的自动成片回退链路。</p>
            )}
            {!renderTasks.length ? (
              <div className="delivery-feedback-note delivery-feedback-note--neutral">
                当前仍保留旧“项目级成片”结果作为回退，以兼容未手动编辑时的快速出片路径。
              </div>
            ) : null}
            {!latestTask?.resultVideoFileId ? (
              <AppButton variant="ghost" loading={finalCompositionLoading} onClick={() => void startComposition()}>
                使用自动成片快速出片
              </AppButton>
            ) : null}
          </section>
        </>
      ) : (
        <EmptyState title="还没有可编辑片段" description="请先准备镜头视频或口型同步结果；首次进入工作台时，系统会基于这些结果生成默认剪辑草稿。">
          <Link className="nav-btn primary" to={`/script-projects/${projectId}/video`}>
            前往镜头与视频
          </Link>
        </EmptyState>
      )}
    </ProjectSubpageShell>
  )
}

function EditingSegmentCard({
  shot,
  segment,
  index,
  total,
  disabled,
  onMove,
  onChange,
}: {
  shot?: StoryboardShot
  segment: VideoEditingDraftSegment
  index: number
  total: number
  disabled?: boolean
  onMove: (direction: -1 | 1) => void
  onChange: (patch: Partial<VideoEditingDraftSegment>) => void
}) {
  return (
    <article className="segment panel glass video-editing-segment">
      <div className="head">
        <div>
          <p className="eyebrow">镜头 {segment.sequenceNo}</p>
          <h3>{shot?.title || `片段 ${segment.sequenceNo}`}</h3>
        </div>
        <div className="video-editing-segment__toggle">
          <label>
            <input
              checked={segment.enabled}
              disabled={disabled}
              type="checkbox"
              onChange={(event) => onChange({ enabled: event.target.checked })}
            />
            启用
          </label>
        </div>
      </div>

      <div className="video-editing-segment__grid">
        <label className="input-wrap">
          <span className="label">素材来源</span>
          <select
            className="ctrl"
            disabled={disabled}
            value={`${segment.sourceType}:${segment.sourceFileId}`}
            onChange={(event) => {
              const next = segment.availableSources?.find(
                (item) => `${item.sourceType}:${item.sourceFileId}` === event.target.value,
              )
              onChange({
                sourceType: next?.sourceType,
                sourceFileId: next?.sourceFileId,
                sourceTaskId: next?.sourceTaskId,
                sourceDurationSeconds: next?.durationSeconds,
              })
            }}
          >
            {(segment.availableSources ?? []).map((item) => (
              <option key={`${item.sourceType}:${item.sourceFileId}`} value={`${item.sourceType}:${item.sourceFileId}`}>
                {item.label || item.sourceType}
              </option>
            ))}
          </select>
        </label>

        <label className="input-wrap">
          <span className="label">入点（秒）</span>
          <input
            className="ctrl"
            disabled={disabled}
            min={0}
            step={0.1}
            type="number"
            value={segment.trimInSeconds}
            onChange={(event) => onChange({ trimInSeconds: Number(event.target.value) })}
          />
        </label>

        <label className="input-wrap">
          <span className="label">出点（秒）</span>
          <input
            className="ctrl"
            disabled={disabled}
            min={0.1}
            step={0.1}
            type="number"
            value={segment.trimOutSeconds}
            onChange={(event) => onChange({ trimOutSeconds: Number(event.target.value) })}
          />
        </label>

        <label className="input-wrap">
          <span className="label">转场</span>
          <select
            className="ctrl"
            disabled={disabled}
            value={segment.transitionMode}
            onChange={(event) =>
              onChange({
                transitionMode: event.target.value as VideoEditingDraftSegment['transitionMode'],
                transitionDurationSeconds: event.target.value === 'CUT' ? 0 : segment.transitionDurationSeconds || 0.3,
              })
            }
          >
            <option value="CUT">直切</option>
            <option value="FADE">淡入淡出</option>
            <option value="DIP_TO_BLACK">黑场转场</option>
          </select>
        </label>
      </div>

      <div className="video-editing-segment__meta muted">
        <span>素材时长 {formatSeconds(segment.sourceDurationSeconds)}</span>
        <span>当前片段 {formatSeconds(Math.max(segment.trimOutSeconds - segment.trimInSeconds, 0))}</span>
        <span>{shot?.actionSummary || '未填写镜头摘要'}</span>
      </div>

      <div className="footer">
        <div className="delivery-action-cluster">
          <AppButton size="sm" variant="ghost" disabled={disabled || index === 0} onClick={() => onMove(-1)}>
            上移
          </AppButton>
          <AppButton size="sm" variant="ghost" disabled={disabled || index === total - 1} onClick={() => onMove(1)}>
            下移
          </AppButton>
        </div>
      </div>
    </article>
  )
}

function FinalCompositionTaskCard({
  task,
  resultVideoUrl,
  busy,
  onRetry,
}: {
  task: FinalCompositionTask
  resultVideoUrl: string
  busy?: boolean
  onRetry: () => void
}) {
  const canRetry = task.status === 'FAILED'
  const lipSyncCount = task.inputSegments.filter((item) => item.sourceType === 'LIP_SYNC').length
  const videoCount = task.inputSegments.filter((item) => item.sourceType === 'VIDEO').length

  return (
    <article className="segment panel glass">
      <div className="head">
        <div>
          <p className="eyebrow">Final Composition</p>
          <h3>项目级成片任务</h3>
        </div>
        <span className="status">{task.status}</span>
      </div>

      <div className="meta">
        <span>输入镜头 {task.inputSegments.length}</span>
        <span>口型同步输入 {lipSyncCount}</span>
        <span>视频回退输入 {videoCount}</span>
        <span>模型 {task.modelName || 'mock-final-composition'}</span>
      </div>

      {resultVideoUrl ? (
        <video className="video" src={resultVideoUrl} controls preload="metadata" />
      ) : (
        <div className="video placeholder muted">项目级成片尚未生成，可等待完成或在失败后重试。</div>
      )}

      <div className="panel glass" style={{ marginTop: 12 }}>
        <p className="muted" style={{ marginBottom: 8 }}>
          输入镜头列表
        </p>
        <div className="stats muted" style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {task.inputSegments.map((segment) => (
            <span key={`${segment.shotId}-${segment.sourceFileId}`}>
              {segment.sequenceNo}. {segment.sourceType}
            </span>
          ))}
        </div>
      </div>

      {task.errorMessage ? <p className="muted">失败原因：{task.errorMessage}</p> : null}

      <div className="footer" style={{ justifyContent: 'space-between', gap: 8 }}>
        <span className="muted">
          {task.providerTaskId ? `Provider Task: ${task.providerTaskId}` : '尚未分配 providerTaskId'}
        </span>
        <AppButton size="sm" variant="ghost" disabled={!canRetry} loading={busy && canRetry} onClick={onRetry}>
          重试成片编排
        </AppButton>
      </div>
    </article>
  )
}

function cloneDraft(draft: VideoEditingDraft): VideoEditingDraft {
  return {
    ...draft,
    segments: draft.segments.map((segment) => ({
      ...segment,
      availableSources: segment.availableSources ? [...segment.availableSources] : [],
      extension: segment.extension ? { ...segment.extension } : null,
    })),
    renderTasks: draft.renderTasks.map((task) => ({
      ...task,
      inputSegments: task.inputSegments.map((segment) => ({ ...segment })),
    })),
    extension: draft.extension ? { ...draft.extension } : null,
  }
}

function serializeDraftPayload(draft: VideoEditingDraft) {
  return JSON.stringify(
    draft.segments.map((segment) => ({
      segmentId: segment.segmentId,
      shotId: segment.shotId,
      sequenceNo: segment.sequenceNo,
      enabled: segment.enabled,
      sourceType: segment.sourceType,
      sourceFileId: segment.sourceFileId,
      sourceTaskId: segment.sourceTaskId ?? null,
      trimInSeconds: Number(segment.trimInSeconds.toFixed(2)),
      trimOutSeconds: Number(segment.trimOutSeconds.toFixed(2)),
      transitionMode: segment.transitionMode,
      transitionDurationSeconds: Number((segment.transitionDurationSeconds ?? 0).toFixed(2)),
      notes: segment.notes ?? null,
    })),
  )
}

function toSavePayload(draft: VideoEditingDraft) {
  return {
    version: draft.version,
    extension: draft.extension ?? null,
    segments: draft.segments.map((segment) => ({
      segmentId: segment.segmentId,
      shotId: segment.shotId,
      sequenceNo: segment.sequenceNo,
      enabled: segment.enabled,
      sourceType: segment.sourceType,
      sourceFileId: segment.sourceFileId,
      sourceTaskId: segment.sourceTaskId ?? null,
      trimInSeconds: Number(segment.trimInSeconds.toFixed(2)),
      trimOutSeconds: Number(segment.trimOutSeconds.toFixed(2)),
      transitionMode: segment.transitionMode,
      transitionDurationSeconds: Number((segment.transitionDurationSeconds ?? 0).toFixed(2)),
      notes: segment.notes ?? null,
      extension: segment.extension ?? null,
    })),
  }
}

function validateDraft(draft: VideoEditingDraft) {
  const errors: string[] = []
  const enabledSegments = draft.segments.filter((segment) => segment.enabled)
  if (!enabledSegments.length) {
    errors.push('至少需要启用一个片段后才能保存或渲染')
  }
  enabledSegments.forEach((segment) => {
    const duration = segment.sourceDurationSeconds ?? 0
    if (!segment.sourceFileId) {
      errors.push(`镜头 ${segment.sequenceNo} 缺少素材来源`)
      return
    }
    if (segment.trimInSeconds < 0) {
      errors.push(`镜头 ${segment.sequenceNo} 的入点不能小于 0`)
    }
    if (segment.trimOutSeconds <= segment.trimInSeconds) {
      errors.push(`镜头 ${segment.sequenceNo} 的出点必须大于入点`)
    }
    if (duration > 0 && segment.trimOutSeconds > duration + 0.01) {
      errors.push(`镜头 ${segment.sequenceNo} 的出点不能超过素材时长`)
    }
  })
  return [...new Set(errors)]
}

function buildEditingPipeline(
  projectId: string,
  renderTasks: VideoEditingRenderTask[],
  pipelineStatus: PipelineStatus | null,
  latestPublishedTask?: VideoEditingRenderTask,
): PipelineStatus | null {
  if (!renderTasks.length && !pipelineStatus?.videoEditingRenderTaskCount) return null
  const successCount = renderTasks.filter((task) => task.status === 'SUCCESS').length
  const failedCount = renderTasks.filter((task) => task.status === 'FAILED').length
  const runningCount = renderTasks.filter((task) => task.status === 'RUNNING').length
  const queuedCount = renderTasks.filter((task) => task.status === 'QUEUED').length
  const pendingCount = renderTasks.filter((task) => task.status === 'PENDING').length
  return {
    projectId,
    projectStatus:
      pipelineStatus?.projectStatus === 'VIDEO_EDITING_RENDERING'
        ? 'VIDEO_EDITING_RENDERING'
        : latestPublishedTask
          ? 'VIDEO_EDITING_READY'
          : 'VIDEO_READY',
    latestRun: pipelineStatus?.latestRun ?? null,
    totalCount: renderTasks.length,
    successCount,
    failedCount,
    runningCount,
    queuedCount,
    pendingCount,
  }
}

function moveSegment(draft: VideoEditingDraft, index: number, direction: -1 | 1) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= draft.segments.length) return draft
  const segments = [...draft.segments]
  const [current] = segments.splice(index, 1)
  segments.splice(targetIndex, 0, current)
  return {
    ...draft,
    hasUnpublishedChanges: true,
    segments: segments.map((segment, order) => ({
      ...segment,
      sequenceNo: order + 1,
    })),
  }
}

function normalizeEditedSegment(segment: VideoEditingDraftSegment, shot?: StoryboardShot): VideoEditingDraftSegment {
  const duration = shot?.targetDurationSec && shot.targetDurationSec > 0 ? shot.targetDurationSec : segment.sourceDurationSeconds || 6
  const trimInSeconds = Number.isFinite(segment.trimInSeconds) ? Math.max(0, segment.trimInSeconds) : 0
  let trimOutSeconds = Number.isFinite(segment.trimOutSeconds) ? segment.trimOutSeconds : duration
  if (trimOutSeconds <= trimInSeconds) {
    trimOutSeconds = trimInSeconds + 0.5
  }
  trimOutSeconds = Math.min(trimOutSeconds, duration)
  return {
    ...segment,
    sourceDurationSeconds: duration,
    trimInSeconds,
    trimOutSeconds,
  }
}

function formatDateTime(value?: string | null) {
  if (!value) return '未记录'
  const time = new Date(value)
  return Number.isNaN(time.getTime()) ? value : time.toLocaleString('zh-CN')
}

function formatSeconds(value?: number | null) {
  if (!value || value <= 0) return '未记录'
  return `${value.toFixed(1)}s`
}

export function resolvePublishCandidate({
  hasUnsavedChanges,
  draftVersion,
  latestSuccessfulPreviewTask,
}: {
  hasUnsavedChanges: boolean
  draftVersion: number | null
  latestSuccessfulPreviewTask?: VideoEditingRenderTask
}) {
  if (!latestSuccessfulPreviewTask) {
    return undefined
  }
  if (
    !canPublishPreview({
      hasUnsavedChanges,
      draftVersion,
      previewDraftVersion: latestSuccessfulPreviewTask.draftVersion ?? null,
    })
  ) {
    return undefined
  }
  return latestSuccessfulPreviewTask
}
