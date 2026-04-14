import { useEffect, useMemo } from 'react'
import { Link, useParams } from 'react-router-dom'
import { resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PipelineProgressBar } from '@/components/script/PipelineProgressBar'
import { ScriptProjectWorkflowNav } from '@/components/script/ScriptProjectWorkflowNav'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { StoryboardShot, VideoSegmentTask } from '@/types'

export function ScriptProjectExportPage() {
  const { projectId = '' } = useParams()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const shots = useScriptProjectStore((s) => s.shots)
  const videoTasks = useScriptProjectStore((s) => s.videoTasks)
  const pipelineStatus = useScriptProjectStore((s) => s.pipelineStatus)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadShots = useScriptProjectStore((s) => s.loadShots)
  const loadVideoTasks = useScriptProjectStore((s) => s.loadVideoTasks)
  const loadPipelineStatus = useScriptProjectStore((s) => s.loadPipelineStatus)
  const startPolling = useScriptProjectStore((s) => s.startPolling)
  const stopPolling = useScriptProjectStore((s) => s.stopPolling)

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

  useEffect(() => {
    if (!projectId) return
    void (async () => {
      await Promise.all([loadProject(projectId), loadShots(projectId), loadVideoTasks(projectId), loadPipelineStatus(projectId)])
      const st = useScriptProjectStore.getState().pipelineStatus
      if (st?.projectStatus === 'VIDEO_GENERATING') {
        startPolling(projectId)
      }
    })()
    return () => stopPolling()
  }, [projectId, loadProject, loadShots, loadVideoTasks, loadPipelineStatus, startPolling, stopPolling])

  function copyVideoUrl(fileId: string | null | undefined) {
    const url = resolveScriptFileUrl(fileId)
    if (!url) {
      showToast('暂无可复制的链接', 'error')
      return
    }
    void navigator.clipboard.writeText(url).then(
      () => showToast('已复制视频直链', 'success'),
      () => showToast('复制失败', 'error'),
    )
  }

  function openVideoUrl(fileId: string | null | undefined) {
    const url = resolveScriptFileUrl(fileId)
    if (!url) return
    window.open(url, '_blank', 'noopener,noreferrer')
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请返回列表重新选择项目。" />
  }

  return (
    <div className="script-project-workflow-layout">
      <ScriptProjectWorkflowNav projectId={projectId} />
      <div className="script-project-workflow-layout__main">
        <section className="script-video-page">
          <div className="toolbar panel glass">
            <div>
              <h2>成片与导出</h2>
              <p className="muted">
                按镜头顺序汇总本工程视频片段，可复制直链或新窗口打开。需要拆分镜头与生成任务请前往「镜头拆分与视频生成」页。
              </p>
            </div>
            <div className="actions" style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
              <Link className="nav-btn" to={`/script-projects/${projectId}/video`}>
                前往镜头拆分与视频生成
              </Link>
              <Link className="nav-btn" to="/history">
                全局历史（工作台任务）
              </Link>
            </div>
          </div>

          <PipelineProgressBar pipeline={pipelineStatus} />

          {detailLoading ? (
            <LoadingSpinner />
          ) : shots.length ? (
            <>
              <p className="muted" style={{ marginBottom: 12 }}>
                已成功生成可导出片段：<strong>{successCount}</strong> / {shots.length} 镜
              </p>
              <div className="segment-list">
                {shots.map((shot) => (
                  <ExportSegmentRow
                    key={shot.shotId}
                    shot={shot}
                    task={taskByShot[shot.shotId]}
                    onCopy={() => copyVideoUrl(taskByShot[shot.shotId]?.resultVideoFileId)}
                    onOpen={() => openVideoUrl(taskByShot[shot.shotId]?.resultVideoFileId)}
                  />
                ))}
              </div>
            </>
          ) : (
            <EmptyState
              title="还没有镜头"
              description="请先在「镜头拆分与视频生成」中拆分镜头并生成视频。"
            >
              <Link className="nav-btn primary" to={`/script-projects/${projectId}/video`}>
                前往镜头拆分与视频生成
              </Link>
            </EmptyState>
          )}
        </section>
      </div>
    </div>
  )
}

function ExportSegmentRow({
  shot,
  task,
  onCopy,
  onOpen,
}: {
  shot: StoryboardShot
  task?: VideoSegmentTask
  onCopy: () => void
  onOpen: () => void
}) {
  const hasVideo = !!task?.resultVideoFileId
  const status = task?.status ?? shot.status

  return (
    <article className="segment panel glass">
      <div className="head">
        <div>
          <p className="eyebrow">Shot {shot.sequenceNo}</p>
          <h3>{shot.title}</h3>
        </div>
        <span className="status">{status}</span>
      </div>
      {hasVideo ? (
        <video className="video" src={resolveScriptFileUrl(task!.resultVideoFileId)} controls preload="metadata" />
      ) : (
        <div className="video placeholder muted">该镜头尚无成片文件，请在视频页重试或等待生成完成。</div>
      )}
      <div className="footer" style={{ justifyContent: 'flex-end', gap: 8 }}>
        <AppButton size="sm" variant="ghost" disabled={!hasVideo} onClick={onCopy}>
          复制直链
        </AppButton>
        <AppButton size="sm" variant="ghost" disabled={!hasVideo} onClick={onOpen}>
          新窗口打开
        </AppButton>
      </div>
      {task?.errorMessage ? <p className="muted">{task.errorMessage}</p> : null}
    </article>
  )
}
