import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { EmptyState } from '@/components/common/EmptyState'
import { FixedPanelDock, type FixedPanelDockItem } from '@/components/common/FixedPanelDock'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PipelineProgressBar } from '@/components/script/PipelineProgressBar'
import { ProjectSubpageShell } from '@/components/script/ProjectSubpageShell'
import { useToast } from '@/context/ToastContext'
import { pickPreferredDeliverySource } from '@/lib/scriptProject/videoEditingPageGuards'
import { useAuthStore } from '@/stores/authStore'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type {
  ContentReviewRecord,
  ContentReviewStatus,
  ContentReviewStatusResponse,
  DubbingTask,
  ExportPackageTask,
  LipSyncTask,
  StoryboardShot,
  VideoEditingDraft,
  VideoSegmentTask,
} from '@/types'

type DeliveryTone = 'neutral' | 'success' | 'warning' | 'danger' | 'info'
type ReadinessState = 'ready' | 'gap' | 'progress' | 'optional'

export function ScriptProjectExportPage() {
  const { projectId = '' } = useParams()
  const { showToast } = useToast()
  const user = useAuthStore((s) => s.user)
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const shots = useScriptProjectStore((s) => s.shots)
  const videoTasks = useScriptProjectStore((s) => s.videoTasks)
  const dubbingTasks = useScriptProjectStore((s) => s.dubbingTasks)
  const lipSyncTasks = useScriptProjectStore((s) => s.lipSyncTasks)
  const videoEditingDraft = useScriptProjectStore((s) => s.videoEditingDraft)
  const finalCompositionTasks = useScriptProjectStore((s) => s.finalCompositionTasks)
  const exportPackageTasks = useScriptProjectStore((s) => s.exportPackageTasks)
  const contentReviewStatus = useScriptProjectStore((s) => s.contentReviewStatus)
  const pipelineStatus = useScriptProjectStore((s) => s.pipelineStatus)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const exportPackageLoading = useScriptProjectStore((s) => s.exportPackageLoading)
  const contentReviewLoading = useScriptProjectStore((s) => s.contentReviewLoading)
  const contentReviewSubmitting = useScriptProjectStore((s) => s.contentReviewSubmitting)
  const contentReviewProcessing = useScriptProjectStore((s) => s.contentReviewProcessing)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadShots = useScriptProjectStore((s) => s.loadShots)
  const loadVideoTasks = useScriptProjectStore((s) => s.loadVideoTasks)
  const loadDubbingTasks = useScriptProjectStore((s) => s.loadDubbingTasks)
  const loadLipSyncTasks = useScriptProjectStore((s) => s.loadLipSyncTasks)
  const loadVideoEditingDraft = useScriptProjectStore((s) => s.loadVideoEditingDraft)
  const loadFinalCompositionTasks = useScriptProjectStore((s) => s.loadFinalCompositionTasks)
  const loadExportPackageTasks = useScriptProjectStore((s) => s.loadExportPackageTasks)
  const loadContentReviewStatus = useScriptProjectStore((s) => s.loadContentReviewStatus)
  const loadPipelineStatus = useScriptProjectStore((s) => s.loadPipelineStatus)
  const startExportPackage = useScriptProjectStore((s) => s.startExportPackage)
  const retryExportPackageTask = useScriptProjectStore((s) => s.retryExportPackageTask)
  const submitContentReview = useScriptProjectStore((s) => s.submitContentReview)
  const approveContentReview = useScriptProjectStore((s) => s.approveContentReview)
  const rejectContentReview = useScriptProjectStore((s) => s.rejectContentReview)
  const startPolling = useScriptProjectStore((s) => s.startPolling)
  const stopPolling = useScriptProjectStore((s) => s.stopPolling)
  const [submitComment, setSubmitComment] = useState('')
  const [reviewComment, setReviewComment] = useState('')
  const [activePanel, setActivePanel] = useState('checklist')

  const taskByShot = useMemo(() => {
    return videoTasks.reduce<Record<string, VideoSegmentTask>>((acc, item) => {
      acc[item.shotId] = item
      return acc
    }, {})
  }, [videoTasks])

  const successCount = useMemo(
    () => videoTasks.filter((t) => t.status === 'SUCCESS' && t.resultVideoFileId).length,
    [videoTasks],
  )

  const dubbingTaskByShot = useMemo(() => {
    return dubbingTasks.reduce<Record<string, DubbingTask>>((acc, item) => {
      acc[item.shotId] = item
      return acc
    }, {})
  }, [dubbingTasks])

  const dubbingReadyCount = useMemo(
    () => dubbingTasks.filter((task) => task.status === 'SUCCESS' && task.resultAudioFileId).length,
    [dubbingTasks],
  )

  const lipSyncTaskByShot = useMemo(() => {
    return lipSyncTasks.reduce<Record<string, LipSyncTask>>((acc, item) => {
      acc[item.shotId] = item
      return acc
    }, {})
  }, [lipSyncTasks])

  const lipSyncReadyCount = useMemo(
    () => lipSyncTasks.filter((task) => task.status === 'SUCCESS' && task.resultVideoFileId).length,
    [lipSyncTasks],
  )

  const latestFinalCompositionTask = finalCompositionTasks[0]
  const deliveryVideo = getPreferredDeliveryVideo(videoEditingDraft, latestFinalCompositionTask)
  const finalCompositionUrl = deliveryVideo.url
  const latestExportPackageTask = exportPackageTasks[0]
  const exportPackageArchiveUrl = resolveDeliveryFileUrl(
    latestExportPackageTask?.resultArchiveFileId,
    latestExportPackageTask?.archivePublicUrl,
  )
  const exportPackageManifestUrl = resolveDeliveryFileUrl(
    latestExportPackageTask?.manifestFileId,
    latestExportPackageTask?.manifestPublicUrl,
  )
  const latestReviewRecord = contentReviewStatus?.records?.[0]

  useEffect(() => {
    if (!projectId) return
    void (async () => {
      try {
        await Promise.all([
          loadProject(projectId),
          loadShots(projectId),
          loadVideoTasks(projectId),
          loadDubbingTasks(projectId),
          loadLipSyncTasks(projectId),
          loadFinalCompositionTasks(projectId),
          loadExportPackageTasks(projectId),
          loadContentReviewStatus(projectId),
          loadPipelineStatus(projectId),
        ])
        await loadVideoEditingDraft(projectId)
        const st = useScriptProjectStore.getState().pipelineStatus
        if (
          st?.projectStatus === 'VIDEO_GENERATING' ||
          st?.projectStatus === 'DUBBING_GENERATING' ||
          st?.projectStatus === 'LIP_SYNC_GENERATING' ||
          st?.projectStatus === 'VIDEO_EDITING_RENDERING' ||
          st?.projectStatus === 'FINAL_COMPOSITION_GENERATING' ||
          st?.projectStatus === 'EXPORT_PACKAGE_GENERATING'
        ) {
          startPolling(projectId)
        }
      } catch (e) {
        showToast(e instanceof Error ? e.message : '交付中心初始化失败', 'error')
      }
    })()
    return () => stopPolling()
  }, [
    projectId,
    loadProject,
    loadShots,
    loadVideoTasks,
    loadDubbingTasks,
    loadLipSyncTasks,
    loadVideoEditingDraft,
    loadFinalCompositionTasks,
    loadExportPackageTasks,
    loadContentReviewStatus,
    loadPipelineStatus,
    startPolling,
    stopPolling,
    showToast,
  ])

  function copyLink(url: string, successText = '已复制链接') {
    if (!url) {
      showToast('暂无可复制的链接', 'error')
      return
    }
    void navigator.clipboard.writeText(url).then(
      () => showToast(successText, 'success'),
      () => showToast('复制失败', 'error'),
    )
  }

  function copyVideoUrl(fileId: string | null | undefined) {
    const url = resolveScriptFileUrl(fileId)
    copyLink(url, '已复制视频直链')
  }

  function openUrl(url: string) {
    if (!url) return
    window.open(url, '_blank', 'noopener,noreferrer')
  }

  function openVideoUrl(fileId: string | null | undefined) {
    openUrl(resolveScriptFileUrl(fileId))
  }

  async function handleGenerateExportPackage() {
    try {
      await startExportPackage(projectId)
      showToast('已启动导出包生成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '导出包生成失败', 'error')
    }
  }

  async function handleRetryExportPackage(taskId: string) {
    try {
      await retryExportPackageTask(projectId, taskId)
      showToast('导出包任务已重新加入队列', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '重试导出包失败', 'error')
    }
  }

  async function handleSubmitContentReview() {
    try {
      await submitContentReview(projectId, { comment: normalizeOptionalText(submitComment) })
      setSubmitComment('')
      showToast(contentReviewStatus?.status === 'REJECTED' ? '已重新提交审核' : '已提交审核', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '提交审核失败', 'error')
    }
  }

  async function handleApproveContentReview() {
    try {
      await approveContentReview(projectId, { comment: normalizeOptionalText(reviewComment) })
      setReviewComment('')
      showToast('审核已通过', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '审核通过失败', 'error')
    }
  }

  async function handleRejectContentReview() {
    if (!normalizeOptionalText(reviewComment)) {
      showToast('驳回时请填写审核意见', 'error')
      return
    }
    try {
      await rejectContentReview(projectId, { comment: normalizeOptionalText(reviewComment) })
      setReviewComment('')
      showToast('审核已驳回', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '驳回审核失败', 'error')
    }
  }

  if (detailLoading && (!currentProject || currentProject.project.projectId !== projectId)) {
    return <LoadingSpinner />
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return (
      <EmptyState title="项目不存在" description="请返回列表重新选择项目。">
        <Link className="nav-btn" to="/script-projects">
          返回项目列表
        </Link>
      </EmptyState>
    )
  }

  const project = currentProject.project
  const reviewLabel = getReviewStatusLabel(contentReviewStatus?.status || project.contentReviewStatus)
  const overallDelivery = getOverallDeliverySummary({
    exportPackageReady: pipelineStatus?.exportPackageReady,
    finalCompositionReady: pipelineStatus?.videoEditingReady || pipelineStatus?.finalCompositionReady || !!deliveryVideo.url,
    reviewStatus: contentReviewStatus?.status || project.contentReviewStatus,
  })
  const readinessItems = [
    {
      label: '镜头视频',
      detail: shots.length ? `已就绪 ${successCount}/${shots.length} 镜` : '尚未拆分镜头',
      state: pipelineStatus?.videoReady ? 'ready' : shots.length ? 'gap' : 'progress',
      actionLabel: '前往镜头页',
      actionTo: `/script-projects/${projectId}/video`,
    },
    {
      label: '配音任务',
      detail: dubbingTasks.length ? `已就绪 ${dubbingReadyCount}/${dubbingTasks.length}` : '按需配置',
      state: !dubbingTasks.length ? 'optional' : pipelineStatus?.dubbingReady ? 'ready' : 'gap',
      actionLabel: '前往配音页',
      actionTo: `/script-projects/${projectId}/dubbing`,
    },
    {
      label: '口型同步',
      detail: lipSyncTasks.length ? `已就绪 ${lipSyncReadyCount}/${lipSyncTasks.length}` : '按需配置',
      state: !lipSyncTasks.length ? 'optional' : pipelineStatus?.lipSyncReady ? 'ready' : 'gap',
      actionLabel: '前往口型同步',
      actionTo: `/script-projects/${projectId}/lip-sync`,
    },
    {
      label: '视频剪辑',
      detail: deliveryVideo.detail,
      state: deliveryVideo.state,
      actionLabel: '前往视频剪辑',
      actionTo: `/script-projects/${projectId}/final-composition`,
    },
    {
      label: '导出包',
      detail: exportPackageArchiveUrl ? 'ZIP 与清单可下载' : '需完成成片后再打包',
      state: pipelineStatus?.exportPackageReady ? 'ready' : latestExportPackageTask ? 'progress' : 'gap',
      actionLabel: '查看导出包',
      actionTo: `/script-projects/${projectId}/export`,
    },
    {
      label: '审核状态',
      detail: reviewLabel,
      state:
        contentReviewStatus?.status === 'APPROVED'
          ? 'ready'
          : contentReviewStatus?.status === 'PENDING'
            ? 'progress'
            : contentReviewStatus?.status === 'REJECTED'
              ? 'gap'
              : 'gap',
      actionLabel: '查看审核区',
      actionTo: `#content-review`,
    },
  ] as const
  const readyCount = readinessItems.filter((item) => item.state === 'ready').length
  const gapItems = readinessItems.filter((item) => item.state === 'gap')
  const optionalCount = readinessItems.filter((item) => item.state === 'optional').length
  const progressCount = readinessItems.filter((item) => item.state === 'progress').length
  const availableDownloads = [finalCompositionUrl, exportPackageArchiveUrl, exportPackageManifestUrl].filter(Boolean).length
  const stageCards = [
    {
      eyebrow: '配音准备',
      title: '确认镜头音频是否齐备',
      tone: !dubbingTasks.length ? 'neutral' : pipelineStatus?.dubbingReady ? 'success' : 'warning',
      statusLabel: !dubbingTasks.length ? '按需确认' : pipelineStatus?.dubbingReady ? '已就绪' : '待补齐',
      stats: [
        `任务 ${pipelineStatus?.dubbingTaskCount ?? dubbingTasks.length}`,
        `已就绪 ${dubbingReadyCount}`,
        `失败 ${pipelineStatus?.dubbingFailedCount ?? dubbingTasks.filter((task) => task.status === 'FAILED').length}`,
        `运行中 ${pipelineStatus?.dubbingRunningCount ?? dubbingTasks.filter((task) => task.status === 'RUNNING').length}`,
      ],
      description: '导出前建议至少确认需要配音的镜头都已产出音频文件。',
      links: [
        { label: '前往配音管理', to: `/script-projects/${projectId}/dubbing` },
        { label: '镜头与视频', to: `/script-projects/${projectId}/video` },
      ],
    },
    {
      eyebrow: '口型同步',
      title: '确认镜头视频与配音已完成同步',
      tone: !lipSyncTasks.length ? 'neutral' : pipelineStatus?.lipSyncReady ? 'success' : 'warning',
      statusLabel: !lipSyncTasks.length ? '按需确认' : pipelineStatus?.lipSyncReady ? '已就绪' : '待补齐',
      stats: [
        `任务 ${pipelineStatus?.lipSyncTaskCount ?? lipSyncTasks.length}`,
        `已就绪 ${lipSyncReadyCount}`,
        `失败 ${pipelineStatus?.lipSyncFailedCount ?? lipSyncTasks.filter((task) => task.status === 'FAILED').length}`,
        `运行中 ${pipelineStatus?.lipSyncRunningCount ?? lipSyncTasks.filter((task) => task.status === 'RUNNING').length}`,
      ],
      description: '如需人物口型效果，请先完成同步后再进入最终交付。',
      links: [
        { label: '前往口型同步', to: `/script-projects/${projectId}/lip-sync` },
        { label: '前往配音管理', to: `/script-projects/${projectId}/dubbing` },
      ],
    },
    {
      eyebrow: '项目级成片',
      title: '导出前确认剪辑发布版本',
      tone: getReadinessTone(deliveryVideo.state),
      statusLabel: deliveryVideo.statusLabel,
      stats: [
        `剪辑任务 ${videoEditingDraft?.renderTasks.length ?? 0}`,
        `已发布 ${videoEditingDraft?.publishedAt ? '1' : '0'}`,
        `任务 ${pipelineStatus?.finalCompositionTaskCount ?? finalCompositionTasks.length}`,
        `已完成 ${pipelineStatus?.finalCompositionSuccessCount ?? finalCompositionTasks.filter((task) => task.status === 'SUCCESS').length}`,
        `失败 ${pipelineStatus?.finalCompositionFailedCount ?? finalCompositionTasks.filter((task) => task.status === 'FAILED').length}`,
      ],
      description: deliveryVideo.description,
      links: [
        { label: '前往视频剪辑', to: `/script-projects/${projectId}/final-composition` },
        { label: '前往口型同步', to: `/script-projects/${projectId}/lip-sync` },
      ],
    },
  ] as const
  const dockItems: FixedPanelDockItem[] = [
    {
      id: 'checklist',
      label: '交付检查',
      eyebrow: 'Checklist',
      summary: '先确认 readiness 清单，再决定是否进入下载或审核流程。',
      badge: gapItems.length ? `缺口 ${gapItems.length}` : '可交付',
      content: (
        <>
          <div className="delivery-action-cluster">
            <span className={getStatusBadgeClass(optionalCount ? 'neutral' : 'success')}>按需项 {optionalCount}</span>
            <span className={getStatusBadgeClass(gapItems.length ? 'warning' : 'success')}>
              {gapItems.length ? `缺口 ${gapItems.length}` : '可进入交付'}
            </span>
          </div>

          <div className="delivery-checklist delivery-checklist--grid">
            {readinessItems.map((item) => (
              <article key={item.label} className={`delivery-checklist__item delivery-checklist__item--${getReadinessTone(item.state)}`}>
                <div>
                  <strong>{item.label}</strong>
                  <p>{item.detail}</p>
                </div>
                {item.actionTo.startsWith('#') ? (
                  <a className="delivery-inline-link" href={item.actionTo}>
                    {item.actionLabel}
                  </a>
                ) : (
                  <Link className="delivery-inline-link" to={item.actionTo}>
                    {item.actionLabel}
                  </Link>
                )}
              </article>
            ))}
          </div>

          <div className={`delivery-feedback-note${gapItems.length ? '' : ' delivery-feedback-note--positive'}`}>
            {gapItems.length
              ? `当前缺口项：${gapItems.map((item) => item.label).join('、')}。建议按清单顺序补齐后再发起最终交付。`
              : '关键检查项已完成，可优先下载导出包并根据权限发起提审或归档。'}
          </div>
        </>
      ),
    },
    {
      id: 'stages',
      label: '阶段准备',
      eyebrow: 'Stages',
      summary: '分阶段查看配音、口型、成片和导出包准备状态，避免一次暴露全部说明。',
      badge: `${stageCards.length + 1} 个阶段`,
      content: (
        <div className="delivery-stage-grid">
          {stageCards.map((card) => (
            <article key={card.title} className="delivery-stage-card">
              <div className="delivery-section-card__head">
                <div>
                  <p className="eyebrow">{card.eyebrow}</p>
                  <h3>{card.title}</h3>
                </div>
                <span className={getStatusBadgeClass(card.tone)}>{card.statusLabel}</span>
              </div>
              <div className="delivery-stat-row muted">
                {card.stats.map((item) => (
                  <span key={item}>{item}</span>
                ))}
              </div>
              <p className="muted">{card.description}</p>
              <div className="delivery-action-cluster">
                {card.links.map((link) => (
                  <Link key={link.label} className="nav-btn" to={link.to}>
                    {link.label}
                  </Link>
                ))}
              </div>
            </article>
          ))}

          <article className="delivery-stage-card">
            <div className="delivery-section-card__head">
              <div>
                <p className="eyebrow">导出包</p>
                <h3>打包并提供归档下载</h3>
              </div>
              <span
                className={getStatusBadgeClass(
                  pipelineStatus?.exportPackageReady ? 'success' : latestExportPackageTask ? 'info' : 'warning',
                )}
              >
                {pipelineStatus?.exportPackageReady ? '已就绪' : latestExportPackageTask ? '处理中' : '待生成'}
              </span>
            </div>
            <div className="delivery-stat-row muted">
              <span>任务 {pipelineStatus?.exportPackageTaskCount ?? exportPackageTasks.length}</span>
              <span>完成 {pipelineStatus?.exportPackageSuccessCount ?? exportPackageTasks.filter((task) => task.status === 'SUCCESS').length}</span>
              <span>失败 {pipelineStatus?.exportPackageFailedCount ?? exportPackageTasks.filter((task) => task.status === 'FAILED').length}</span>
              <span>运行中 {pipelineStatus?.exportPackageRunningCount ?? exportPackageTasks.filter((task) => task.status === 'RUNNING').length}</span>
            </div>
            <p className="muted">
              导出包会优先基于最近一次已发布剪辑成片生成；若尚未发布剪辑版，则回退到自动成片结果。
            </p>
            <div className="delivery-action-cluster">
              <AppButton variant="primary" loading={exportPackageLoading} onClick={() => void handleGenerateExportPackage()}>
                生成导出包
              </AppButton>
              <AppButton
                variant="ghost"
                size="sm"
                disabled={latestExportPackageTask?.status !== 'FAILED'}
                loading={exportPackageLoading && latestExportPackageTask?.status === 'FAILED'}
                onClick={() => latestExportPackageTask && void handleRetryExportPackage(latestExportPackageTask.exportPackageTaskId)}
              >
                重试导出包
              </AppButton>
              <Link className="nav-btn" to={`/script-projects/${projectId}/final-composition`}>
                前往视频剪辑
              </Link>
            </div>
            {latestExportPackageTask ? (
              <ExportPackageTaskSummary task={latestExportPackageTask} archiveUrl={exportPackageArchiveUrl} manifestUrl={exportPackageManifestUrl} />
            ) : (
              <p className="muted">还没有导出包任务，可在剪辑成片发布或自动成片就绪后直接发起打包。</p>
            )}
          </article>
        </div>
      ),
    },
    {
      id: 'delivery',
      label: '下载与审核',
      eyebrow: 'Delivery',
      summary: '成片下载区和内容审核区合并为一组，围绕最终交付集中处理。',
      badge: availableDownloads ? `${availableDownloads} 项可下载` : '待生成',
      content: (
        <>
          <div className="delivery-artifact-grid">
            <article className="delivery-section-card">
              <div className="delivery-section-card__head">
                <div>
                  <p className="eyebrow">成果聚焦</p>
                  <h3>{deliveryVideo.title}</h3>
                </div>
                <span className={getStatusBadgeClass(deliveryVideo.tone)}>
                  {deliveryVideo.badge}
                </span>
              </div>
              {finalCompositionUrl ? (
                <video className="video" src={finalCompositionUrl} controls preload="metadata" />
              ) : (
                <div className="video placeholder muted">当前还没有可交付成片文件，请先在视频剪辑页生成预览并发布，或继续使用自动成片回退。</div>
              )}
              <div className="delivery-feedback-note delivery-feedback-note--neutral">{deliveryVideo.description}</div>
              <div className="delivery-download-actions">
                <AppButton
                  size="sm"
                  variant="ghost"
                  disabled={!deliveryVideo.fileId}
                  onClick={() => copyVideoUrl(deliveryVideo.fileId)}
                >
                  复制成片直链
                </AppButton>
                <AppButton
                  size="sm"
                  variant="ghost"
                  disabled={!deliveryVideo.fileId}
                  onClick={() => openVideoUrl(deliveryVideo.fileId)}
                >
                  新窗口打开成片
                </AppButton>
              </div>
              {deliveryVideo.hint ? (
                <div className="delivery-feedback-note">{deliveryVideo.hint}</div>
              ) : latestFinalCompositionTask?.errorMessage ? (
                <div className="delivery-feedback-note">失败原因：{latestFinalCompositionTask.errorMessage}</div>
              ) : null}
            </article>

            <article className="delivery-section-card">
              <div className="delivery-section-card__head">
                <div>
                  <p className="eyebrow">最终下载区</p>
                  <h3>集中获取项目交付物</h3>
                </div>
                <span className={getStatusBadgeClass(availableDownloads ? 'success' : 'warning')}>
                  {availableDownloads ? '可下载' : '待生成'}
                </span>
              </div>
              <div className="delivery-stat-row muted">
                <span>{deliveryVideo.downloadLabel} {finalCompositionUrl ? '可下载' : '未生成'}</span>
                <span>导出包 ZIP {exportPackageArchiveUrl ? '可下载' : '未生成'}</span>
                <span>清单文件 {exportPackageManifestUrl ? '可下载' : '未生成'}</span>
              </div>
              <div className="delivery-download-actions">
                <AppButton size="sm" variant="ghost" disabled={!finalCompositionUrl} onClick={() => openUrl(finalCompositionUrl)}>
                  下载当前成片
                </AppButton>
                <AppButton size="sm" variant="ghost" disabled={!exportPackageArchiveUrl} onClick={() => openUrl(exportPackageArchiveUrl)}>
                  下载导出包 ZIP
                </AppButton>
                <AppButton size="sm" variant="ghost" disabled={!exportPackageManifestUrl} onClick={() => openUrl(exportPackageManifestUrl)}>
                  下载清单文件
                </AppButton>
                <AppButton
                  size="sm"
                  variant="ghost"
                  disabled={!exportPackageArchiveUrl}
                  onClick={() => copyLink(exportPackageArchiveUrl, '已复制导出包链接')}
                >
                  复制导出包链接
                </AppButton>
                <AppButton
                  size="sm"
                  variant="ghost"
                  disabled={!exportPackageManifestUrl}
                  onClick={() => copyLink(exportPackageManifestUrl, '已复制清单链接')}
                >
                  复制清单链接
                </AppButton>
              </div>
              <div className={`delivery-feedback-note${availableDownloads ? ' delivery-feedback-note--positive' : ''}`}>
                {availableDownloads
                  ? `建议优先提交 ZIP 包，并保留${deliveryVideo.downloadLabel}直链用于校内展示与答辩播放。`
                  : '当前下载区尚未齐备，建议先完成剪辑成片发布或自动成片生成后再打包。'}
              </div>
            </article>
          </div>

          <div id="content-review">
            <ContentReviewPanel
              userRole={user?.role}
              reviewStatus={contentReviewStatus}
              loading={contentReviewLoading}
              submitLoading={contentReviewSubmitting}
              processLoading={contentReviewProcessing}
              submitComment={submitComment}
              reviewComment={reviewComment}
              onSubmitCommentChange={setSubmitComment}
              onReviewCommentChange={setReviewComment}
              onSubmit={() => void handleSubmitContentReview()}
              onApprove={() => void handleApproveContentReview()}
              onReject={() => void handleRejectContentReview()}
              latestReviewRecord={latestReviewRecord}
            />
          </div>
        </>
      ),
    },
    {
      id: 'segments',
      label: '镜头交付',
      eyebrow: 'Segments',
      summary: '逐镜查看可导出片段，只在需要时展开长列表。',
      badge: `${successCount}/${shots.length || 0} 镜`,
      content: detailLoading ? (
        <LoadingSpinner />
      ) : shots.length ? (
        <div className="delivery-section-card">
          <div className="delivery-section-card__head">
            <div>
              <p className="eyebrow">镜头交付清单</p>
              <h3>逐镜查看可导出片段</h3>
            </div>
            <span className={getStatusBadgeClass(successCount === shots.length && shots.length ? 'success' : 'info')}>
              已成功生成 {successCount} / {shots.length} 镜
            </span>
          </div>
          <div className="segment-list">
            {shots.map((shot) => (
              <ExportSegmentRow
                key={shot.shotId}
                shot={shot}
                task={taskByShot[shot.shotId]}
                dubbingTask={dubbingTaskByShot[shot.shotId]}
                lipSyncTask={lipSyncTaskByShot[shot.shotId]}
                onCopy={() =>
                  copyVideoUrl(
                    lipSyncTaskByShot[shot.shotId]?.resultVideoFileId || taskByShot[shot.shotId]?.resultVideoFileId,
                  )
                }
                onOpen={() =>
                  openVideoUrl(
                    lipSyncTaskByShot[shot.shotId]?.resultVideoFileId || taskByShot[shot.shotId]?.resultVideoFileId,
                  )
                }
              />
            ))}
          </div>
        </div>
      ) : (
        <EmptyState title="还没有镜头" description="请先在「镜头拆分与视频生成」中拆分镜头并生成视频。">
          <Link className="nav-btn primary" to={`/script-projects/${projectId}/video`}>
            前往镜头拆分与视频生成
          </Link>
        </EmptyState>
      ),
    },
  ]

  return (
    <ProjectSubpageShell
      projectId={projectId}
      title="成果展示与交付中心"
      description="统一判断项目是否达到可交付状态，并集中处理成片、导出包、审核记录与下载入口。"
      meta={
        <>
          <span className="soft-badge">{project.name}</span>
          <span className={getStatusBadgeClass(overallDelivery.tone)}>总体判断 · {overallDelivery.label}</span>
          <span className={getStatusBadgeClass(getReadinessTone(readinessItems.find((item) => item.label === '审核状态')?.state || 'gap'))}>
            审核 · {reviewLabel}
          </span>
        </>
      }
      stats={[
        { key: 'ready', label: '已完成项', value: readyCount, hint: `${readinessItems.length} 项检查` },
        { key: 'gap', label: '缺口项', value: gapItems.length, hint: '需要补齐' },
        { key: 'progress', label: '处理中', value: progressCount, hint: '等待任务完成' },
        { key: 'downloads', label: '可下载成果', value: availableDownloads, hint: '成片 / ZIP / 清单' },
      ]}
      toolbar={
        <>
          <div className="project-subpage-shell__toolbar-head">
            <div className="project-subpage-shell__toolbar-copy">
              <p className="eyebrow">交付中心</p>
              <h3>交付动作与去重入口</h3>
              <p className="muted">{overallDelivery.description}</p>
            </div>
            <div className="project-subpage-shell__toolbar-side">
              <div className="delivery-status-row">
                <span className={getStatusBadgeClass(getReadinessTone(readinessItems.find((item) => item.label === '导出包')?.state || 'gap'))}>
                  导出包 · {pipelineStatus?.exportPackageReady ? '已就绪' : '待处理'}
                </span>
                <span className="soft-badge">课程：{project.courseId || '未绑定课程'}</span>
                <span className="soft-badge">提交人：{project.ownerName || project.ownerId || '未记录'}</span>
              </div>
            </div>
          </div>
          <div className="project-subpage-shell__toolbar-actions">
            <Link className="nav-btn" to={`/script-projects/${projectId}`}>
              返回项目详情
            </Link>
            {project.courseId ? (
              <Link className="nav-btn" to={`/courses/${encodeURIComponent(project.courseId)}`}>
                返回所属课程
              </Link>
            ) : null}
            <Link className="nav-btn" to={`/script-projects/${projectId}/video`}>
              镜头与视频
            </Link>
            <Link className="nav-btn" to={`/script-projects/${projectId}/final-composition`}>
              视频剪辑
            </Link>
            <Link className="nav-btn" to={`/audit-logs?entityType=SCRIPT_PROJECT&entityId=${encodeURIComponent(projectId)}`}>
              项目审计
            </Link>
          </div>
        </>
      }
      helpTitle="查看交付中心说明"
      help={
        <>
          <p>交付页把 readiness 清单、阶段检查、下载物与审核记录收进固定面板，避免首屏被长说明和重复入口占满。</p>
          <p>建议先看总体判断和成片预览，再进入下载与审核；镜头级明细则放到单独面板按需查看。</p>
        </>
      }
      className="delivery-showcase-page"
    >
      <section className="delivery-section-card panel glass">
        <div className="delivery-section-card__head">
          <div>
            <p className="eyebrow">最终成片</p>
            <h3>交付前快速预览</h3>
          </div>
          <span className={getStatusBadgeClass(deliveryVideo.tone)}>
            {deliveryVideo.badge}
          </span>
        </div>
        {finalCompositionUrl ? (
          <video className="delivery-hero__video" src={finalCompositionUrl} controls preload="metadata" />
        ) : (
          <div className="delivery-hero__poster delivery-hero__poster--placeholder delivery-hero__poster--video">
            <span>交付</span>
          </div>
        )}
        <p className="delivery-hero__caption muted">
          {finalCompositionUrl ? deliveryVideo.description : '当前还没有可交付成片，请先完成视频剪辑发布或等待自动成片回退生成。'}
        </p>
      </section>

      <PipelineProgressBar pipeline={pipelineStatus} />

      <FixedPanelDock
        title="交付面板"
        description="固定触发区聚合检查、阶段、下载审核和镜头清单，首屏只保留状态判断与关键动作。"
        items={dockItems}
        activeId={activePanel}
        onChange={setActivePanel}
      />
    </ProjectSubpageShell>
  )
}

export function getPreferredDeliveryVideo(
  videoEditingDraft: VideoEditingDraft | null,
  latestFinalCompositionTask?: { resultVideoFileId?: string | null; status?: string | null },
) {
  const publishedTask =
    videoEditingDraft?.renderTasks.find((task) => task.published || (task.renderType === 'PUBLISH' && task.status === 'SUCCESS')) ?? null
  const previewTask =
    videoEditingDraft?.renderTasks.find((task) => task.renderType === 'PREVIEW' && task.status === 'SUCCESS') ?? null
  const preferred = pickPreferredDeliverySource(videoEditingDraft, latestFinalCompositionTask)

  if (preferred.source === 'published') {
    return {
      fileId: preferred.fileId,
      url: resolveScriptFileUrl(preferred.fileId),
      state: 'ready' as const,
      tone: 'success' as const,
      badge: '已发布',
      title: '已发布剪辑成片',
      downloadLabel: '已发布剪辑成片',
      detail: `已发布剪辑成片，可直接预览与下载（${formatDateTime(publishedTask?.publishedAt || videoEditingDraft?.publishedAt)}）`,
      statusLabel: '已发布',
      description: '导出页当前优先展示最近一次已发布的剪辑成片，确保交付基于用户确认过的编辑版本。',
      hint: '',
    }
  }

  if (preferred.source === 'preview' && previewTask?.resultVideoFileId) {
    return {
      fileId: previewTask.resultVideoFileId,
      url: resolveScriptFileUrl(previewTask.resultVideoFileId),
      state: 'progress' as const,
      tone: 'info' as const,
      badge: '预览未发布',
      title: '剪辑预览结果',
      downloadLabel: '剪辑预览',
      detail: '预览已生成但尚未发布',
      statusLabel: '待发布',
      description: '当前展示的是剪辑预览结果。发布成片后，导出包和交付状态才会把它视为正式交付版本。',
      hint: '预览已生成但尚未发布，导出链路仍会在必要时回退到自动成片结果。',
    }
  }

  if (preferred.source === 'fallback' && latestFinalCompositionTask?.resultVideoFileId) {
    return {
      fileId: latestFinalCompositionTask.resultVideoFileId,
      url: resolveScriptFileUrl(latestFinalCompositionTask.resultVideoFileId),
      state: latestFinalCompositionTask.status === 'SUCCESS' ? ('ready' as const) : ('progress' as const),
      tone: 'neutral' as const,
      badge: '自动成片回退',
      title: '自动成片回退结果',
      downloadLabel: '自动成片回退',
      detail: '尚未发布剪辑成片，当前回退展示自动成片结果',
      statusLabel: '回退可用',
      description: '当前仍使用旧的自动成片结果兜底，避免在剪辑版本未发布时阻断导出与交付。',
      hint: '建议尽快在视频剪辑页完成预览确认并发布，以替换自动成片回退。',
    }
  }

  return {
    fileId: '',
    url: '',
    state: 'gap' as const,
    tone: 'warning' as const,
    badge: '待生成',
    title: '暂无成片结果',
    downloadLabel: '当前成片',
    detail: '尚未生成可交付成片',
    statusLabel: '待生成',
    description: '请先在视频剪辑页生成预览并发布，或继续等待自动成片完成后再进入导出打包。',
    hint: '',
  }
}

function resolveDeliveryFileUrl(fileId?: string | null, publicUrl?: string | null) {
  return publicUrl || resolveScriptFileUrl(fileId)
}

function normalizeOptionalText(value: string) {
  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}

function formatDateTime(value?: string | null) {
  if (!value) return '未记录'
  const time = new Date(value)
  return Number.isNaN(time.getTime()) ? value : time.toLocaleString('zh-CN')
}

function getReviewStatusLabel(status?: ContentReviewStatus | null) {
  switch (status) {
    case 'PENDING':
      return '待审核'
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已驳回'
    case 'NOT_SUBMITTED':
    default:
      return '未提审'
  }
}

function getReviewStatusHint(reviewStatus?: ContentReviewStatusResponse | null, userRole?: string | null) {
  if (!reviewStatus) return '正在读取审核状态。'
  if (reviewStatus.canProcess) {
    return '当前账号可处理此条审核，可填写意见后执行通过或驳回。'
  }
  if (reviewStatus.canSubmit) {
    return reviewStatus.status === 'REJECTED'
      ? '项目已被驳回，可根据意见修改后重新提审。'
      : '导出包已就绪，可填写补充说明后提交审核。'
  }
  if (reviewStatus.status === 'PENDING') {
    return userRole === 'STUDENT' || userRole === 'ADMIN' || userRole === 'TEACHER'
      ? '项目已进入审核中，等待审核人处理。'
      : '项目已进入审核中。'
  }
  if (reviewStatus.status === 'APPROVED') {
    return '项目已审核通过，可直接下载和交付。'
  }
  if (!reviewStatus.exportPackageReady) {
    return '需先生成可用导出包，之后才能提交审核。'
  }
  return '当前账号暂无可执行的审核操作。'
}

function getOverallDeliverySummary({
  exportPackageReady,
  finalCompositionReady,
  reviewStatus,
}: {
  exportPackageReady?: boolean
  finalCompositionReady?: boolean
  reviewStatus?: ContentReviewStatus | null
}) {
  if (exportPackageReady && reviewStatus === 'APPROVED') {
    return {
      label: '可交付',
      tone: 'success' as const,
      description: '导出包与审核均已完成，可直接下载、提交或归档。',
    }
  }
  if (exportPackageReady && reviewStatus === 'PENDING') {
    return {
      label: '审核中',
      tone: 'info' as const,
      description: '导出包已齐备，当前等待审核处理，可先准备最终交付说明。',
    }
  }
  if (exportPackageReady) {
    return {
      label: '待提审',
      tone: 'warning' as const,
      description: '导出包已就绪，但尚未完成审核闭环，建议尽快发起提审。',
    }
  }
  if (finalCompositionReady) {
    return {
      label: '待打包',
      tone: 'info' as const,
      description: '项目级成片已生成，下一步建议发起导出包打包并检查下载物。',
    }
  }
  return {
    label: '待补齐',
    tone: 'warning' as const,
    description: '当前仍存在关键缺口项，建议先完成成片与导出物准备。',
  }
}

function getStatusBadgeClass(tone: DeliveryTone) {
  return `delivery-status-badge delivery-status-badge--${tone}`
}

function getReadinessTone(state: ReadinessState): DeliveryTone {
  switch (state) {
    case 'ready':
      return 'success'
    case 'progress':
      return 'info'
    case 'optional':
      return 'neutral'
    case 'gap':
    default:
      return 'warning'
  }
}

function getTaskTone(status?: string | null): DeliveryTone {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING' || status === 'QUEUED') return 'info'
  return 'warning'
}

function ExportPackageTaskSummary({
  task,
  archiveUrl,
  manifestUrl,
}: {
  task: ExportPackageTask
  archiveUrl: string
  manifestUrl: string
}) {
  return (
    <div className="delivery-feedback-note delivery-feedback-note--neutral">
      <div className="delivery-stat-row muted">
        <span>重试次数 {task.retryCount}</span>
        <span>
          成片来源 {task.sourceVideoEditingRenderTaskId ? '已发布剪辑成片' : task.sourceFinalCompositionTaskId ? '自动成片回退' : '未记录'}
        </span>
        <span>清单 {manifestUrl ? '已生成' : '未生成'}</span>
        <span>压缩包 {archiveUrl ? '已生成' : '未生成'}</span>
      </div>
      {task.errorMessage ? <p className="muted">失败原因：{task.errorMessage}</p> : null}
    </div>
  )
}

function ContentReviewPanel({
  userRole,
  reviewStatus,
  loading,
  submitLoading,
  processLoading,
  submitComment,
  reviewComment,
  onSubmitCommentChange,
  onReviewCommentChange,
  onSubmit,
  onApprove,
  onReject,
  latestReviewRecord,
}: {
  userRole?: string | null
  reviewStatus: ContentReviewStatusResponse | null
  loading: boolean
  submitLoading: boolean
  processLoading: boolean
  submitComment: string
  reviewComment: string
  onSubmitCommentChange: (value: string) => void
  onReviewCommentChange: (value: string) => void
  onSubmit: () => void
  onApprove: () => void
  onReject: () => void
  latestReviewRecord?: ContentReviewRecord
}) {
  const status = reviewStatus?.status
  const submitLabel = status === 'REJECTED' ? '重新提审' : '提交审核'

  return (
    <div className="panel glass delivery-section-card">
      <div className="delivery-section-card__head">
        <div>
          <p className="eyebrow">内容审核</p>
          <h3>交付前确认审核状态与处理意见</h3>
        </div>
        <span className={getStatusBadgeClass(status === 'APPROVED' ? 'success' : status === 'PENDING' ? 'info' : status === 'REJECTED' ? 'danger' : 'warning')}>
          {getReviewStatusLabel(status)}
        </span>
      </div>

      <div className="delivery-stat-row muted">
        <span>导出包 {reviewStatus?.exportPackageReady ? '已就绪' : '未就绪'}</span>
        <span>重提次数 {reviewStatus?.resubmitCount ?? 0}</span>
        <span>提审时间 {formatDateTime(reviewStatus?.reviewSubmittedAt)}</span>
        <span>审核时间 {formatDateTime(reviewStatus?.reviewedAt)}</span>
        <span>审核人 {reviewStatus?.reviewerUserName || reviewStatus?.reviewerUserId || '待分配'}</span>
      </div>

      <div className="delivery-feedback-note delivery-feedback-note--neutral">
        {getReviewStatusHint(reviewStatus, userRole)}
      </div>

      {loading && !reviewStatus ? <LoadingSpinner /> : null}
      {latestReviewRecord?.submissionComment ? (
        <div className="delivery-feedback-note delivery-feedback-note--neutral">
          最新提审说明：{latestReviewRecord.submissionComment}
        </div>
      ) : null}
      {reviewStatus?.latestReviewComment ? (
        <div className="delivery-feedback-note">最新审核意见：{reviewStatus.latestReviewComment}</div>
      ) : null}

      {reviewStatus?.canSubmit ? (
        <div className="delivery-feedback-workbench">
          <div className="delivery-feedback-workbench__input">
            <AppInput
              label={status === 'REJECTED' ? '重提说明' : '提审说明'}
              as="textarea"
              rows={4}
              value={submitComment}
              onChange={(value) => onSubmitCommentChange(String(value))}
              placeholder="例如：本次已修正导出包命名、补充结尾字幕并完成交付自检"
            />
          </div>
          <div className="delivery-feedback-workbench__actions">
            <AppButton variant="primary" loading={submitLoading} onClick={onSubmit}>
              {submitLabel}
            </AppButton>
          </div>
        </div>
      ) : null}

      {reviewStatus?.canProcess ? (
        <div className="delivery-feedback-workbench">
          <div className="delivery-feedback-workbench__input">
            <AppInput
              label="审核意见"
              as="textarea"
              rows={4}
              value={reviewComment}
              onChange={(value) => onReviewCommentChange(String(value))}
              placeholder="通过可选填备注；驳回时必须填写具体修改意见"
            />
          </div>
          <div className="delivery-feedback-workbench__actions">
            <AppButton variant="primary" loading={processLoading} onClick={onApprove}>
              审核通过
            </AppButton>
            <AppButton variant="danger" loading={processLoading} onClick={onReject}>
              驳回
            </AppButton>
          </div>
        </div>
      ) : null}

      {reviewStatus?.records?.length ? (
        <div className="delivery-record-list">
          {reviewStatus.records.map((record) => (
            <article key={record.reviewId} className="panel glass delivery-record-card">
              <div className="delivery-section-card__head">
                <div>
                  <p className="eyebrow">审核记录</p>
                  <h3>{record.reviewId}</h3>
                </div>
                <span
                  className={getStatusBadgeClass(
                    record.status === 'APPROVED' ? 'success' : record.status === 'PENDING' ? 'info' : record.status === 'REJECTED' ? 'danger' : 'warning',
                  )}
                >
                  {getReviewStatusLabel(record.status)}
                </span>
              </div>
              <div className="delivery-stat-row muted">
                <span>提交人 {record.submitterUserName || record.submitterUserId || '未记录'}</span>
                <span>审核人 {record.reviewerUserName || record.reviewerUserId || '未处理'}</span>
                <span>提交时间 {formatDateTime(record.submittedAt)}</span>
                <span>审核时间 {formatDateTime(record.reviewedAt)}</span>
                <span>第 {Math.max((record.resubmitCount ?? 0) + 1, 1)} 次提审</span>
              </div>
              {record.submissionComment ? <p className="muted">提审说明：{record.submissionComment}</p> : null}
              {record.reviewComment ? <p className="muted">审核意见：{record.reviewComment}</p> : null}
            </article>
          ))}
        </div>
      ) : (
        <p className="muted">当前还没有审核记录。导出包生成完成后，可在这里发起提审或处理审核。</p>
      )}
    </div>
  )
}

function ExportSegmentRow({
  shot,
  task,
  dubbingTask,
  lipSyncTask,
  onCopy,
  onOpen,
}: {
  shot: StoryboardShot
  task?: VideoSegmentTask
  dubbingTask?: DubbingTask
  lipSyncTask?: LipSyncTask
  onCopy: () => void
  onOpen: () => void
}) {
  const exportFileId = lipSyncTask?.resultVideoFileId || task?.resultVideoFileId
  const hasVideo = !!exportFileId
  const status = lipSyncTask?.status ?? task?.status ?? shot.status

  return (
    <article className="segment panel glass">
      <div className="head">
        <div>
          <p className="eyebrow">镜头 {shot.sequenceNo}</p>
          <h3>{shot.title}</h3>
        </div>
        <span className={getStatusBadgeClass(getTaskTone(status))}>{status}</span>
      </div>
      {hasVideo ? (
        <video className="video" src={resolveScriptFileUrl(exportFileId)} controls preload="metadata" />
      ) : (
        <div className="video placeholder muted">该镜头尚无成片文件，请在视频页重试或等待生成完成。</div>
      )}
      <div className="delivery-download-actions">
        <AppButton size="sm" variant="ghost" disabled={!hasVideo} onClick={onCopy}>
          复制直链
        </AppButton>
        <AppButton size="sm" variant="ghost" disabled={!hasVideo} onClick={onOpen}>
          新窗口打开
        </AppButton>
      </div>
      <p className="muted">
        配音：
        {dubbingTask?.resultAudioFileId
          ? ` 已准备（${dubbingTask.voiceName || '默认音色'}）`
          : dubbingTask?.errorMessage
            ? ` 失败 - ${dubbingTask.errorMessage}`
            : dubbingTask
              ? ` ${dubbingTask.status}`
              : ' 尚未生成'}
      </p>
      <p className="muted">
        口型同步：
        {lipSyncTask?.resultVideoFileId
          ? ' 已准备'
          : lipSyncTask?.errorMessage
            ? ` 失败 - ${lipSyncTask.errorMessage}`
            : lipSyncTask
              ? ` ${lipSyncTask.status}`
              : ' 尚未生成'}
      </p>
      {task?.errorMessage ? <p className="muted">{task.errorMessage}</p> : null}
    </article>
  )
}
