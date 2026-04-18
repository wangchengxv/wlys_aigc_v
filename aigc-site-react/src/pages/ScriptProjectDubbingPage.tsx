import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getModels, resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PipelineProgressBar } from '@/components/script/PipelineProgressBar'
import { ProjectSubpageShell } from '@/components/script/ProjectSubpageShell'
import { WorkflowModelPanel } from '@/components/script/WorkflowModelPanel'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { DubbingTask, ModelConfig, PipelineStatus, StoryboardShot } from '@/types'

export function ScriptProjectDubbingPage() {
  const { projectId = '' } = useParams()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const shots = useScriptProjectStore((s) => s.shots)
  const dubbingTasks = useScriptProjectStore((s) => s.dubbingTasks)
  const pipelineStatus = useScriptProjectStore((s) => s.pipelineStatus)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const dubbingLoading = useScriptProjectStore((s) => s.dubbingLoading)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadShots = useScriptProjectStore((s) => s.loadShots)
  const loadDubbingTasks = useScriptProjectStore((s) => s.loadDubbingTasks)
  const loadPipelineStatus = useScriptProjectStore((s) => s.loadPipelineStatus)
  const startDubbingGeneration = useScriptProjectStore((s) => s.startDubbingGeneration)
  const retryDubbingTask = useScriptProjectStore((s) => s.retryDubbingTask)
  const startPolling = useScriptProjectStore((s) => s.startPolling)
  const stopPolling = useScriptProjectStore((s) => s.stopPolling)

  const [allModels, setAllModels] = useState<ModelConfig[]>([])

  const taskByShot = useMemo(() => {
    return dubbingTasks.reduce<Record<string, DubbingTask>>((acc, item) => {
      acc[item.shotId] = item
      return acc
    }, {})
  }, [dubbingTasks])

  const dubbingPipeline = useMemo<PipelineStatus | null>(() => {
    if (!pipelineStatus) return null
    return {
      ...pipelineStatus,
      totalCount: pipelineStatus.dubbingTaskCount ?? dubbingTasks.length,
      successCount: pipelineStatus.dubbingSuccessCount ?? dubbingTasks.filter((task) => task.status === 'SUCCESS').length,
      failedCount: pipelineStatus.dubbingFailedCount ?? dubbingTasks.filter((task) => task.status === 'FAILED').length,
      runningCount: pipelineStatus.dubbingRunningCount ?? dubbingTasks.filter((task) => task.status === 'RUNNING').length,
      queuedCount: pipelineStatus.dubbingQueuedCount ?? dubbingTasks.filter((task) => task.status === 'QUEUED').length,
      pendingCount: pipelineStatus.dubbingPendingCount ?? dubbingTasks.filter((task) => task.status === 'PENDING').length,
    }
  }, [dubbingTasks, pipelineStatus])

  useEffect(() => {
    void getModels().then(setAllModels).catch(() => {})
  }, [])

  useEffect(() => {
    if (!projectId) return
    void (async () => {
      await Promise.all([loadProject(projectId), loadShots(projectId), loadDubbingTasks(projectId), loadPipelineStatus(projectId)])
      const st = useScriptProjectStore.getState().pipelineStatus
      if (st?.projectStatus === 'DUBBING_GENERATING') {
        startPolling(projectId)
      }
    })()
    return () => stopPolling()
  }, [projectId, loadProject, loadShots, loadDubbingTasks, loadPipelineStatus, startPolling, stopPolling])

  async function startDubbing() {
    try {
      await startDubbingGeneration(projectId)
      showToast('已启动整项目配音生成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '配音生成启动失败', 'error')
    }
  }

  async function retry(taskId: string) {
    try {
      await retryDubbingTask(projectId, taskId)
      showToast('配音任务已重新加入队列', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '重试失败', 'error')
    }
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请返回列表重新选择项目。" />
  }

  const project = currentProject.project
  const successCount = dubbingPipeline?.successCount ?? 0
  const failedCount = dubbingPipeline?.failedCount ?? 0

  return (
    <ProjectSubpageShell
      projectId={projectId}
      title="项目配音管理"
      description="把镜头脚本、配音任务、试听结果和失败重试收进同一页，首屏只保留状态、模型入口和主动作。"
      meta={
        <>
          <span className="soft-badge">{project.name}</span>
          <span className={`soft-badge ${pipelineStatus?.dubbingReady ? 'is-success' : ''}`}>
            {pipelineStatus?.dubbingReady ? '配音已就绪' : '配音待生成'}
          </span>
        </>
      }
      stats={[
        { key: 'shots', label: '镜头总数', value: shots.length },
        { key: 'tasks', label: '配音任务', value: dubbingPipeline?.totalCount ?? 0 },
        { key: 'success', label: '已完成', value: successCount },
        { key: 'failed', label: '失败', value: failedCount },
      ]}
      toolbar={
        <>
          <div className="project-subpage-shell__toolbar-head">
            <div className="project-subpage-shell__toolbar-copy">
              <p className="eyebrow">Audio Pipeline</p>
              <h3>模型与任务动作</h3>
              <p className="muted">基于镜头脚本生成整项目配音；支持失败单条重试与结果试听。</p>
            </div>
            <div className="project-subpage-shell__toolbar-side">
              <WorkflowModelPanel projectId={projectId} scope="dubbing" allModels={allModels} />
            </div>
          </div>
          <div className="project-subpage-shell__toolbar-actions">
            <AppButton variant="primary" loading={dubbingLoading} onClick={() => void startDubbing()}>
              生成整项目配音
            </AppButton>
            <Link className="nav-btn" to={`/script-projects/${projectId}/export`}>
              前往成片与导出
            </Link>
          </div>
        </>
      }
      helpTitle="查看配音页说明"
      help={
        <>
          <p>先在视频页准备镜头，再从这里集中生成配音，避免在多个页面重复处理音频任务。</p>
          <p>页面默认只保留关键统计、模型入口和任务列表，详细说明收起到帮助提示中。</p>
        </>
      }
    >
      <PipelineProgressBar pipeline={dubbingPipeline} />

      {detailLoading ? (
        <LoadingSpinner />
      ) : shots.length ? (
        dubbingTasks.length ? (
          <div className="segment-list">
            {shots.map((shot) => (
              <DubbingTaskRow
                key={shot.shotId}
                shot={shot}
                task={taskByShot[shot.shotId]}
                busy={dubbingLoading}
                onRetry={(taskId) => void retry(taskId)}
              />
            ))}
          </div>
        ) : (
          <EmptyState title="还没有配音任务" description="请先为当前工程生成配音任务，再试听或导出音频。">
            <AppButton variant="primary" loading={dubbingLoading} onClick={() => void startDubbing()}>
              立即生成配音
            </AppButton>
          </EmptyState>
        )
      ) : (
        <EmptyState title="还没有镜头" description="请先在“镜头拆分与视频生成”页拆分镜头，再为镜头生成配音。" />
      )}
    </ProjectSubpageShell>
  )
}

function DubbingTaskRow({
  shot,
  task,
  busy,
  onRetry,
}: {
  shot: StoryboardShot
  task?: DubbingTask
  busy?: boolean
  onRetry: (taskId: string) => void
}) {
  const audioUrl = resolveScriptFileUrl(task?.resultAudioFileId)
  const canRetry = !!task?.dubbingTaskId && task.status === 'FAILED'

  return (
    <article className="segment panel glass">
      <div className="head">
        <div>
          <p className="eyebrow">镜头 {shot.sequenceNo}</p>
          <h3>{shot.title}</h3>
        </div>
        <span className="status">{task?.status ?? 'PENDING'}</span>
      </div>

      <p className="muted">{task?.inputText || shot.scriptText}</p>

      <div className="meta">
        <span>音色 {task?.voiceName || '未设置'}</span>
        <span>语言 {task?.language || '未设置'}</span>
        <span>语速 {task?.speechRate ?? '-'}</span>
        <span>模型 {task?.modelName || '待分配'}</span>
      </div>

      {audioUrl ? (
        <audio style={{ width: '100%' }} src={audioUrl} controls preload="metadata" />
      ) : (
        <div className="video placeholder muted">该镜头暂未产出音频文件，可等待生成完成或在失败后重试。</div>
      )}

      {task?.errorMessage ? <p className="muted">失败原因：{task.errorMessage}</p> : null}

      <div className="footer" style={{ justifyContent: 'space-between', gap: 8 }}>
        <span className="muted">
          {task?.providerTaskId ? `Provider Task: ${task.providerTaskId}` : '尚未分配 providerTaskId'}
        </span>
        <AppButton size="sm" variant="ghost" disabled={!canRetry} loading={busy && canRetry} onClick={() => task && onRetry(task.dubbingTaskId)}>
          重试配音
        </AppButton>
      </div>
    </article>
  )
}
