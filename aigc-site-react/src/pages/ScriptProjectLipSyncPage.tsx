import { useEffect, useMemo } from 'react'
import { Link, useParams } from 'react-router-dom'
import { resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PipelineProgressBar } from '@/components/script/PipelineProgressBar'
import { ProjectSubpageShell } from '@/components/script/ProjectSubpageShell'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { DubbingTask, LipSyncTask, PipelineStatus, StoryboardShot, VideoSegmentTask } from '@/types'

export function ScriptProjectLipSyncPage() {
  const { projectId = '' } = useParams()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const shots = useScriptProjectStore((s) => s.shots)
  const videoTasks = useScriptProjectStore((s) => s.videoTasks)
  const dubbingTasks = useScriptProjectStore((s) => s.dubbingTasks)
  const lipSyncTasks = useScriptProjectStore((s) => s.lipSyncTasks)
  const pipelineStatus = useScriptProjectStore((s) => s.pipelineStatus)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const lipSyncLoading = useScriptProjectStore((s) => s.lipSyncLoading)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadShots = useScriptProjectStore((s) => s.loadShots)
  const loadVideoTasks = useScriptProjectStore((s) => s.loadVideoTasks)
  const loadDubbingTasks = useScriptProjectStore((s) => s.loadDubbingTasks)
  const loadLipSyncTasks = useScriptProjectStore((s) => s.loadLipSyncTasks)
  const loadPipelineStatus = useScriptProjectStore((s) => s.loadPipelineStatus)
  const startLipSyncGeneration = useScriptProjectStore((s) => s.startLipSyncGeneration)
  const retryLipSyncTask = useScriptProjectStore((s) => s.retryLipSyncTask)
  const startPolling = useScriptProjectStore((s) => s.startPolling)
  const stopPolling = useScriptProjectStore((s) => s.stopPolling)

  const videoTaskByShot = useMemo(() => {
    return videoTasks.reduce<Record<string, VideoSegmentTask>>((acc, item) => {
      acc[item.shotId] = item
      return acc
    }, {})
  }, [videoTasks])

  const dubbingTaskByShot = useMemo(() => {
    return dubbingTasks.reduce<Record<string, DubbingTask>>((acc, item) => {
      acc[item.shotId] = item
      return acc
    }, {})
  }, [dubbingTasks])

  const lipSyncTaskByShot = useMemo(() => {
    return lipSyncTasks.reduce<Record<string, LipSyncTask>>((acc, item) => {
      acc[item.shotId] = item
      return acc
    }, {})
  }, [lipSyncTasks])

  const lipSyncPipeline = useMemo<PipelineStatus | null>(() => {
    if (!pipelineStatus) return null
    return {
      ...pipelineStatus,
      totalCount: pipelineStatus.lipSyncTaskCount ?? lipSyncTasks.length,
      successCount: pipelineStatus.lipSyncSuccessCount ?? lipSyncTasks.filter((task) => task.status === 'SUCCESS').length,
      failedCount: pipelineStatus.lipSyncFailedCount ?? lipSyncTasks.filter((task) => task.status === 'FAILED').length,
      runningCount: pipelineStatus.lipSyncRunningCount ?? lipSyncTasks.filter((task) => task.status === 'RUNNING').length,
      queuedCount: pipelineStatus.lipSyncQueuedCount ?? lipSyncTasks.filter((task) => task.status === 'QUEUED').length,
      pendingCount: pipelineStatus.lipSyncPendingCount ?? lipSyncTasks.filter((task) => task.status === 'PENDING').length,
    }
  }, [lipSyncTasks, pipelineStatus])

  useEffect(() => {
    if (!projectId) return
    void (async () => {
      await Promise.all([
        loadProject(projectId),
        loadShots(projectId),
        loadVideoTasks(projectId),
        loadDubbingTasks(projectId),
        loadLipSyncTasks(projectId),
        loadPipelineStatus(projectId),
      ])
      const st = useScriptProjectStore.getState().pipelineStatus
      if (st?.projectStatus === 'LIP_SYNC_GENERATING') {
        startPolling(projectId)
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
    loadPipelineStatus,
    startPolling,
    stopPolling,
  ])

  async function startLipSync() {
    try {
      await startLipSyncGeneration(projectId)
      showToast('已启动整项目口型同步', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '口型同步启动失败', 'error')
    }
  }

  async function retry(taskId: string) {
    try {
      await retryLipSyncTask(projectId, taskId)
      showToast('口型同步任务已重新加入队列', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '重试失败', 'error')
    }
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请返回列表重新选择项目。" />
  }

  const project = currentProject.project

  return (
    <ProjectSubpageShell
      projectId={projectId}
      title="项目口型同步"
      description="统一检查来源视频、来源配音和同步结果，避免在视频页与配音页之间来回确认输入准备度。"
      meta={
        <>
          <span className="soft-badge">{project.name}</span>
          <span className={`soft-badge ${pipelineStatus?.lipSyncReady ? 'is-success' : ''}`}>
            {pipelineStatus?.lipSyncReady ? '口型已就绪' : '口型待处理'}
          </span>
        </>
      }
      stats={[
        { key: 'shots', label: '镜头总数', value: shots.length },
        { key: 'tasks', label: '同步任务', value: lipSyncPipeline?.totalCount ?? 0 },
        { key: 'video', label: '视频已准备', value: videoTasks.filter((task) => !!task.resultVideoFileId).length },
        { key: 'audio', label: '配音已准备', value: dubbingTasks.filter((task) => !!task.resultAudioFileId).length },
      ]}
      toolbar={
        <div className="project-subpage-shell__toolbar-head">
          <div className="project-subpage-shell__toolbar-copy">
            <p className="eyebrow">Lip Sync Pipeline</p>
            <h3>任务动作</h3>
            <p className="muted">支持整项目发起口型同步，并在结果列表中检查每镜输入是否齐备。</p>
          </div>
          <div className="project-subpage-shell__toolbar-actions">
            <AppButton variant="primary" loading={lipSyncLoading} onClick={() => void startLipSync()}>
              生成整项目口型同步
            </AppButton>
            <Link className="nav-btn" to={`/script-projects/${projectId}/export`}>
              前往成片与导出
            </Link>
          </div>
        </div>
      }
      helpTitle="查看口型同步说明"
      help={
        <>
          <p>口型同步依赖镜头视频和配音音频，两侧输入不完整时会直接在任务卡上暴露缺口。</p>
          <p>首屏不再重复解释流程，只保留进度、主动作和每镜结果检查。</p>
        </>
      }
    >
      <PipelineProgressBar pipeline={lipSyncPipeline} />

      {detailLoading ? (
        <LoadingSpinner />
      ) : shots.length ? (
        <div className="segment-list">
          {shots.map((shot) => (
            <LipSyncTaskRow
              key={shot.shotId}
              shot={shot}
              videoTask={videoTaskByShot[shot.shotId]}
              dubbingTask={dubbingTaskByShot[shot.shotId]}
              task={lipSyncTaskByShot[shot.shotId]}
              busy={lipSyncLoading}
              onRetry={(taskId) => void retry(taskId)}
            />
          ))}
        </div>
      ) : (
        <EmptyState title="还没有镜头" description="请先在“镜头拆分与视频生成”页拆分镜头，并准备镜头视频与配音音频。" />
      )}
    </ProjectSubpageShell>
  )
}

function LipSyncTaskRow({
  shot,
  videoTask,
  dubbingTask,
  task,
  busy,
  onRetry,
}: {
  shot: StoryboardShot
  videoTask?: VideoSegmentTask
  dubbingTask?: DubbingTask
  task?: LipSyncTask
  busy?: boolean
  onRetry: (taskId: string) => void
}) {
  const sourceVideoUrl = resolveScriptFileUrl(task?.sourceVideoFileId || videoTask?.resultVideoFileId)
  const sourceAudioUrl = resolveScriptFileUrl(task?.sourceAudioFileId || dubbingTask?.resultAudioFileId)
  const resultVideoUrl = resolveScriptFileUrl(task?.resultVideoFileId)
  const canRetry = !!task?.lipSyncTaskId && task.status === 'FAILED'

  return (
    <article className="segment panel glass">
      <div className="head">
        <div>
          <p className="eyebrow">镜头 {shot.sequenceNo}</p>
          <h3>{shot.title}</h3>
        </div>
        <span className="status">{task?.status ?? 'PENDING'}</span>
      </div>

      <p className="muted">{shot.scriptText}</p>

      <div className="meta">
        <span>来源视频 {sourceVideoUrl ? '已准备' : '缺失'}</span>
        <span>来源配音 {sourceAudioUrl ? '已准备' : '缺失'}</span>
        <span>模型 {task?.modelName || '待分配'}</span>
        <span>重试次数 {task?.retryCount ?? 0}</span>
      </div>

      <div style={{ display: 'grid', gap: 12 }}>
        <div>
          <p className="muted" style={{ marginBottom: 8 }}>
            来源视频
          </p>
          {sourceVideoUrl ? (
            <video className="video" src={sourceVideoUrl} controls preload="metadata" />
          ) : (
            <div className="video placeholder muted">该镜头暂无来源视频，请先完成镜头视频生成。</div>
          )}
        </div>

        <div>
          <p className="muted" style={{ marginBottom: 8 }}>
            来源配音
          </p>
          {sourceAudioUrl ? (
            <audio style={{ width: '100%' }} src={sourceAudioUrl} controls preload="metadata" />
          ) : (
            <div className="video placeholder muted">该镜头暂无来源音频，请先完成配音生成。</div>
          )}
        </div>

        <div>
          <p className="muted" style={{ marginBottom: 8 }}>
            结果预览
          </p>
          {resultVideoUrl ? (
            <video className="video" src={resultVideoUrl} controls preload="metadata" />
          ) : (
            <div className="video placeholder muted">该镜头暂未产出口型同步结果视频，可等待生成完成或在失败后重试。</div>
          )}
        </div>
      </div>

      {task?.errorMessage ? <p className="muted">失败原因：{task.errorMessage}</p> : null}

      <div className="footer" style={{ justifyContent: 'space-between', gap: 8 }}>
        <span className="muted">{task?.providerTaskId ? `Provider Task: ${task.providerTaskId}` : '尚未分配 providerTaskId'}</span>
        <AppButton size="sm" variant="ghost" disabled={!canRetry} loading={busy && canRetry} onClick={() => task && onRetry(task.lipSyncTaskId)}>
          重试口型同步
        </AppButton>
      </div>
    </article>
  )
}
