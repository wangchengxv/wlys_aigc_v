import { useEffect, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PipelineProgressBar } from '@/components/script/PipelineProgressBar'
import { VideoSegmentCard } from '@/components/script/VideoSegmentCard'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { VideoSegmentTask } from '@/types'

export function ScriptProjectVideoPage() {
  const { projectId = '' } = useParams()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const shots = useScriptProjectStore((s) => s.shots)
  const videoTasks = useScriptProjectStore((s) => s.videoTasks)
  const pipelineStatus = useScriptProjectStore((s) => s.pipelineStatus)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const shotLoading = useScriptProjectStore((s) => s.shotLoading)
  const videoLoading = useScriptProjectStore((s) => s.videoLoading)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadShots = useScriptProjectStore((s) => s.loadShots)
  const loadVideoTasks = useScriptProjectStore((s) => s.loadVideoTasks)
  const loadPipelineStatus = useScriptProjectStore((s) => s.loadPipelineStatus)
  const splitShots = useScriptProjectStore((s) => s.splitShots)
  const startVideoGeneration = useScriptProjectStore((s) => s.startVideoGeneration)
  const retryVideoTask = useScriptProjectStore((s) => s.retryVideoTask)
  const startPolling = useScriptProjectStore((s) => s.startPolling)
  const stopPolling = useScriptProjectStore((s) => s.stopPolling)

  const taskByShot = useMemo(() => {
    return videoTasks.reduce<Record<string, VideoSegmentTask>>((acc, item) => {
      acc[item.shotId] = item
      return acc
    }, {})
  }, [videoTasks])

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

  async function split() {
    try {
      await splitShots(projectId)
      showToast('镜头拆分完成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '拆分镜头失败', 'error')
    }
  }

  async function startVideo() {
    try {
      await startVideoGeneration(projectId)
      showToast('已启动并发视频生成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '视频生成启动失败', 'error')
    }
  }

  async function retry(taskId: string) {
    try {
      await retryVideoTask(projectId, taskId)
      showToast('片段已重新加入队列', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '重试失败', 'error')
    }
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请返回列表重新选择项目。" />
  }

  return (
    <section className="script-video-page">
      <div className="toolbar panel glass">
        <div>
          <h2>镜头拆分与视频生成</h2>
          <p className="muted">先拆分镜头，再基于已确认关键帧启动并发视频任务。</p>
        </div>
        <div className="actions">
          <AppButton loading={shotLoading} onClick={() => void split()}>
            拆分镜头
          </AppButton>
          <AppButton variant="primary" loading={videoLoading} onClick={() => void startVideo()}>
            启动视频生成
          </AppButton>
        </div>
      </div>

      <PipelineProgressBar pipeline={pipelineStatus} />

      {detailLoading ? (
        <LoadingSpinner />
      ) : shots.length ? (
        <div className="segment-list">
          {shots.map((shot) => (
            <VideoSegmentCard key={shot.shotId} shot={shot} task={taskByShot[shot.shotId]} busy={videoLoading} onRetry={(id) => void retry(id)} />
          ))}
        </div>
      ) : (
        <EmptyState title="还没有镜头" description="请先从完善剧本中拆分镜头，系统会为每个镜头生成独立的视频任务。" />
      )}
    </section>
  )
}
