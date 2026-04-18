import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getModels, rollbackShotVisualPrompt } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PipelineProgressBar } from '@/components/script/PipelineProgressBar'
import { VideoSegmentCard } from '@/components/script/VideoSegmentCard'
import { ProjectSubpageShell } from '@/components/script/ProjectSubpageShell'
import { WorkflowModelPanel } from '@/components/script/WorkflowModelPanel'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { ModelConfig, VideoSegmentTask } from '@/types'

export function ScriptProjectVideoPage() {
  const { projectId = '' } = useParams()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const assets = useScriptProjectStore((s) => s.assets)
  const shots = useScriptProjectStore((s) => s.shots)
  const videoTasks = useScriptProjectStore((s) => s.videoTasks)
  const pipelineStatus = useScriptProjectStore((s) => s.pipelineStatus)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const shotLoading = useScriptProjectStore((s) => s.shotLoading)
  const videoLoading = useScriptProjectStore((s) => s.videoLoading)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadShots = useScriptProjectStore((s) => s.loadShots)
  const loadAssets = useScriptProjectStore((s) => s.loadAssets)
  const loadVideoTasks = useScriptProjectStore((s) => s.loadVideoTasks)
  const loadPipelineStatus = useScriptProjectStore((s) => s.loadPipelineStatus)
  const splitShots = useScriptProjectStore((s) => s.splitShots)
  const startVideoGeneration = useScriptProjectStore((s) => s.startVideoGeneration)
  const retryVideoTask = useScriptProjectStore((s) => s.retryVideoTask)
  const startPolling = useScriptProjectStore((s) => s.startPolling)
  const stopPolling = useScriptProjectStore((s) => s.stopPolling)
  const shotVisualLoading = useScriptProjectStore((s) => s.shotVisualLoading)
  const shotSaving = useScriptProjectStore((s) => s.shotSaving)
  const generateVisualPromptForShot = useScriptProjectStore((s) => s.generateVisualPromptForShot)
  const saveShot = useScriptProjectStore((s) => s.saveShot)
  const applyStoryboardFirstFrameForShot = useScriptProjectStore((s) => s.applyStoryboardFirstFrameForShot)

  const [allModels, setAllModels] = useState<ModelConfig[]>([])

  const taskByShot = useMemo(() => {
    return videoTasks.reduce<Record<string, VideoSegmentTask>>((acc, item) => {
      acc[item.shotId] = item
      return acc
    }, {})
  }, [videoTasks])

  const storyboardAssets = useMemo(
    () => assets.filter((asset) => !!asset.storyboardImageFileId),
    [assets],
  )

  useEffect(() => {
    void getModels().then(setAllModels).catch(() => {})
  }, [])

  useEffect(() => {
    if (!projectId) return
    void (async () => {
      await Promise.all([
        loadProject(projectId),
        loadAssets(projectId),
        loadShots(projectId),
        loadVideoTasks(projectId),
        loadPipelineStatus(projectId),
      ])
      const st = useScriptProjectStore.getState().pipelineStatus
      if (st?.projectStatus === 'VIDEO_GENERATING') {
        startPolling(projectId)
      }
    })()
    return () => stopPolling()
  }, [projectId, loadProject, loadAssets, loadShots, loadVideoTasks, loadPipelineStatus, startPolling, stopPolling])

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

  async function handleShotVisualPrompt(shotId: string) {
    try {
      await generateVisualPromptForShot(projectId, shotId)
      showToast('分镜提示词已生成（B-9）', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '生成失败', 'error')
    }
  }

  async function handleSaveShot(
    shotId: string,
    payload: { shotType?: string; cameraMove?: string; emotion?: string; visualPrompt?: string },
  ) {
    try {
      await saveShot(projectId, shotId, payload)
      showToast(payload.visualPrompt !== undefined ? '分镜提示词已保存' : '镜头参数已保存', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  async function handleRollbackShotVisual(shotId: string, versionId: string) {
    try {
      await rollbackShotVisualPrompt(projectId, shotId, { versionId })
      await loadShots(projectId)
      showToast('已回滚到所选版本', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '回滚失败', 'error')
    }
  }

  async function handleClearFirstFrame(shotId: string) {
    try {
      await applyStoryboardFirstFrameForShot(projectId, shotId, { mode: 'NONE' })
      showToast('镜头首帧引用已清空', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '清空首帧失败', 'error')
    }
  }

  async function handleApplyFirstFrame(
    shotId: string,
    payload: { assetId: string; mode: 'FULL_GRID' | 'CROPPED_PANEL'; panelIndex?: number },
  ) {
    try {
      await applyStoryboardFirstFrameForShot(projectId, shotId, payload)
      showToast(payload.mode === 'FULL_GRID' ? '已绑定整张九宫格首帧' : '已绑定单格裁剪首帧', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '绑定九宫格首帧失败', 'error')
    }
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请返回列表重新选择项目。" />
  }

  const project = currentProject.project
  const videoReadyCount = videoTasks.filter((task) => task.status === 'SUCCESS' && !!task.resultVideoFileId).length

  return (
    <ProjectSubpageShell
      projectId={projectId}
      title="镜头拆分与视频生成"
      description="把镜头拆分、B-9 分镜提示词、首帧引用和视频任务统一到一个工作区里，避免同类操作在多个页面重复出现。"
      meta={
        <>
          <span className="soft-badge">{project.name}</span>
          <span className={`soft-badge ${pipelineStatus?.videoReady ? 'is-success' : ''}`}>
            {pipelineStatus?.videoReady ? '视频已就绪' : '视频待生成'}
          </span>
        </>
      }
      stats={[
        { key: 'shots', label: '镜头总数', value: shots.length },
        { key: 'assets', label: '可用分镜资产', value: storyboardAssets.length },
        { key: 'ready', label: '已出片镜头', value: videoReadyCount },
        { key: 'tasks', label: '视频任务', value: videoTasks.length },
      ]}
      toolbar={
        <>
          <div className="project-subpage-shell__toolbar-head">
            <div className="project-subpage-shell__toolbar-copy">
              <p className="eyebrow">Video Pipeline</p>
              <h3>模型与主动作</h3>
              <p className="muted">先拆分镜头，再补充分镜提示词与首帧引用，最后统一启动视频生成。</p>
            </div>
            <div className="project-subpage-shell__toolbar-side">
              <WorkflowModelPanel projectId={projectId} scope="video" allModels={allModels} />
            </div>
          </div>
          <div className="project-subpage-shell__toolbar-actions">
            <AppButton loading={shotLoading} onClick={() => void split()}>
              拆分镜头
            </AppButton>
            <AppButton variant="primary" loading={videoLoading} onClick={() => void startVideo()}>
              启动视频生成
            </AppButton>
          </div>
        </>
      }
      helpTitle="查看视频页说明"
      help={
        <>
          <p>这页负责镜头级生产动作，配音、口型同步、成片和导出已经下沉到各自次级页中处理。</p>
          <p>当前模式下不再单独展示额外流程说明，重点保留镜头任务卡和关键动作。</p>
        </>
      }
    >
      <PipelineProgressBar pipeline={pipelineStatus} />

      {detailLoading ? (
        <LoadingSpinner />
      ) : shots.length ? (
        <div className="segment-list">
          {shots.map((shot) => (
            <VideoSegmentCard
              key={shot.shotId}
              shot={shot}
              task={taskByShot[shot.shotId]}
              projectId={projectId}
              busy={videoLoading}
              shotVisualBusy={shotVisualLoading}
              shotSaving={shotSaving}
              onRetry={(id) => void retry(id)}
              onGenerateVisualPrompt={(id) => void handleShotVisualPrompt(id)}
              onSaveShot={(id, payload) => void handleSaveShot(id, payload)}
              onRollbackShotVisual={(id, vid) => void handleRollbackShotVisual(id, vid)}
              onClearFirstFrame={(id) => void handleClearFirstFrame(id)}
              storyboardAssets={storyboardAssets}
              onApplyFirstFrame={(id, payload) => void handleApplyFirstFrame(id, payload)}
              onVideoHistoryRestored={async () => {
                await loadVideoTasks(projectId)
              }}
            />
          ))}
        </div>
      ) : (
        <EmptyState title="还没有镜头" description="请先从完善剧本中拆分镜头，系统会为每个镜头生成独立的视频任务。" />
      )}
    </ProjectSubpageShell>
  )
}
