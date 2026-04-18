import { useEffect, useState } from 'react'
import { resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { AssetHistoryPanel } from '@/components/script/AssetHistoryPanel'
import { PromptVersionsEditor } from '@/components/script/PromptVersionsEditor'
import type { ExtractedAsset, StoryboardShot, VideoSegmentTask } from '@/types'

type Props = {
  shot: StoryboardShot
  task?: VideoSegmentTask
  projectId?: string
  busy?: boolean
  shotVisualBusy?: boolean
  shotSaving?: boolean
  onRetry: (taskId: string) => void
  onGenerateVisualPrompt?: (shotId: string) => void
  onSaveShot?: (
    shotId: string,
    payload: { shotType?: string; cameraMove?: string; emotion?: string; visualPrompt?: string },
  ) => void
  onRollbackShotVisual?: (shotId: string, versionId: string) => void | Promise<void>
  onClearFirstFrame?: (shotId: string) => void
  storyboardAssets?: ExtractedAsset[]
  onApplyFirstFrame?: (shotId: string, payload: { assetId: string; mode: 'FULL_GRID' | 'CROPPED_PANEL'; panelIndex?: number }) => void
  onVideoHistoryRestored?: () => void | Promise<void>
}

const SHOT_TYPE_OPTIONS = ['ECU极特写', 'CU特写', 'MCU中近景', 'MS中景', 'MLS中远景', 'LS全景', 'ELS大远景'] as const
const CAMERA_MOVE_OPTIONS = ['static静止', 'pan摇', 'tilt俯仰', 'dolly推拉', 'truck横移', 'crane升降', 'handheld手持', 'zoom变焦'] as const

export function VideoSegmentCard({
  shot,
  task,
  projectId,
  busy,
  shotVisualBusy,
  shotSaving,
  onRetry,
  onGenerateVisualPrompt,
  onSaveShot,
  onRollbackShotVisual,
  onClearFirstFrame,
  storyboardAssets = [],
  onApplyFirstFrame,
  onVideoHistoryRestored,
}: Props) {
  const [videoHistOpen, setVideoHistOpen] = useState(false)
  const [shotType, setShotType] = useState(shot.shotType ?? '')
  const [cameraMove, setCameraMove] = useState(shot.cameraMove ?? '')
  const [emotion, setEmotion] = useState(shot.emotion ?? '')
  const [selectedAssetId, setSelectedAssetId] = useState(shot.storyboardAssetId ?? '')
  const [firstFrameMode, setFirstFrameMode] = useState<'FULL_GRID' | 'CROPPED_PANEL'>(
    shot.firstFrameMode === 'CROPPED_PANEL' ? 'CROPPED_PANEL' : 'FULL_GRID',
  )
  const [panelIndex, setPanelIndex] = useState<number>(shot.storyboardCropIndex ?? 0)
  const [vpDraft, setVpDraft] = useState(shot.visualPrompt ?? '')

  useEffect(() => {
    setShotType(shot.shotType ?? '')
    setCameraMove(shot.cameraMove ?? '')
    setEmotion(shot.emotion ?? '')
    setSelectedAssetId(shot.storyboardAssetId ?? '')
    setFirstFrameMode(shot.firstFrameMode === 'CROPPED_PANEL' ? 'CROPPED_PANEL' : 'FULL_GRID')
    setPanelIndex(shot.storyboardCropIndex ?? 0)
    setVpDraft(shot.visualPrompt ?? '')
  }, [
    shot.shotId,
    shot.shotType,
    shot.cameraMove,
    shot.emotion,
    shot.storyboardAssetId,
    shot.firstFrameMode,
    shot.storyboardCropIndex,
    shot.visualPrompt,
  ])

  const selectedAsset = storyboardAssets.find((item) => item.assetId === shot.storyboardAssetId)

  return (
    <article className="segment panel glass">
      <div className="head">
        <div>
          <p className="eyebrow">镜头 {shot.sequenceNo}</p>
          <h3>{shot.title}</h3>
        </div>
        <div className="head-actions">
          {onGenerateVisualPrompt ? (
            <AppButton size="sm" variant="ghost" loading={shotVisualBusy} onClick={() => onGenerateVisualPrompt(shot.shotId)}>
              分镜提示词 B-9
            </AppButton>
          ) : null}
          <span className="status">{task?.status || shot.status}</span>
        </div>
      </div>

      <p className="muted">{shot.scriptText}</p>
      <div className="meta">
        <span>动作：{shot.actionSummary}</span>
        <span>运镜：{shot.cameraMovement}</span>
      </div>

      {onSaveShot ? (
        <div className="shot-params">
          <div className="shot-params-grid">
            <label className="input-wrap">
              <span className="label">镜头类型（shotType）</span>
              <select className="ctrl" value={shotType} onChange={(e) => setShotType(e.target.value)}>
                <option value="">（自动/默认）</option>
                {SHOT_TYPE_OPTIONS.map((opt) => (
                  <option key={opt} value={opt}>
                    {opt}
                  </option>
                ))}
              </select>
            </label>
            <label className="input-wrap">
              <span className="label">运镜（cameraMove）</span>
              <select className="ctrl" value={cameraMove} onChange={(e) => setCameraMove(e.target.value)}>
                <option value="">（沿用运镜字段/默认）</option>
                {CAMERA_MOVE_OPTIONS.map((opt) => (
                  <option key={opt} value={opt}>
                    {opt}
                  </option>
                ))}
              </select>
            </label>
            <label className="input-wrap">
              <span className="label">情绪（emotion）</span>
              <input className="ctrl" value={emotion} onChange={(e) => setEmotion(e.target.value)} placeholder="例如：紧张、压迫、温暖…" />
            </label>
          </div>
          <div className="shot-params-actions">
            <AppButton
              size="sm"
              loading={shotSaving}
              onClick={() =>
                onSaveShot(shot.shotId, {
                  shotType: shotType.trim() || undefined,
                  cameraMove: cameraMove.trim() || undefined,
                  emotion: emotion.trim() || undefined,
                })
              }
            >
              保存镜头参数
            </AppButton>
          </div>
        </div>
      ) : null}

      {onSaveShot && onRollbackShotVisual ? (
        <div className="shot-visual-prompt">
          <PromptVersionsEditor
            label="分镜图像提示词（B-9）"
            value={vpDraft}
            onChange={setVpDraft}
            versions={shot.promptVersions ?? undefined}
            busy={shotSaving}
            onSave={async () => {
              await onSaveShot(shot.shotId, { visualPrompt: vpDraft.trim() || undefined })
            }}
            onRollback={async (versionId) => {
              await onRollbackShotVisual(shot.shotId, versionId)
            }}
          />
        </div>
      ) : shot.visualPrompt ? (
        <div className="shot-visual-prompt">
          <p className="eyebrow">分镜图像提示词（B-9）</p>
          <p className="shot-vp-text">{shot.visualPrompt}</p>
        </div>
      ) : null}

      {shot.firstFrameMode && shot.firstFrameMode !== 'NONE' ? (
        <div className="shot-visual-prompt">
          <div className="head-actions">
            <p className="eyebrow">首帧参考</p>
            {onClearFirstFrame ? (
              <AppButton size="sm" variant="ghost" loading={shotSaving} onClick={() => onClearFirstFrame(shot.shotId)}>
                清空首帧
              </AppButton>
            ) : null}
          </div>
          <p className="shot-vp-text">
            模式：{shot.firstFrameMode}
            {selectedAsset ? ` · 资产：${selectedAsset.name}` : ''}
            {shot.storyboardCropIndex != null ? ` · 格子 #${shot.storyboardCropIndex + 1}` : ''}
          </p>
          {shot.storyboardCropFileId || shot.storyboardImageFileId ? (
            <img
              className="video"
              src={resolveScriptFileUrl(shot.storyboardCropFileId || shot.storyboardImageFileId)}
              alt="首帧参考"
            />
          ) : null}
        </div>
      ) : null}

      {onApplyFirstFrame ? (
        <div className="shot-firstframe-bind">
          <p className="eyebrow">快捷绑定九宫格来源</p>
          <div className="shot-firstframe-grid">
            <label className="input-wrap">
              <span className="label">来源资产</span>
              <select className="ctrl" value={selectedAssetId} onChange={(e) => setSelectedAssetId(e.target.value)}>
                <option value="">请选择已生成九宫格分镜图的资产</option>
                {storyboardAssets.map((asset) => (
                  <option key={asset.assetId} value={asset.assetId}>
                    {asset.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="input-wrap">
              <span className="label">首帧模式</span>
              <select
                className="ctrl"
                value={firstFrameMode}
                onChange={(e) => setFirstFrameMode(e.target.value as 'FULL_GRID' | 'CROPPED_PANEL')}
              >
                <option value="FULL_GRID">整张九宫格</option>
                <option value="CROPPED_PANEL">单格裁剪</option>
              </select>
            </label>
            {firstFrameMode === 'CROPPED_PANEL' ? (
              <label className="input-wrap">
                <span className="label">面板格子</span>
                <select className="ctrl" value={String(panelIndex)} onChange={(e) => setPanelIndex(Number(e.target.value))}>
                  {Array.from({ length: 9 }, (_, idx) => (
                    <option key={idx} value={idx}>
                      第 {idx + 1} 格
                    </option>
                  ))}
                </select>
              </label>
            ) : null}
          </div>
          <div className="shot-firstframe-actions">
            <AppButton
              size="sm"
              variant="primary"
              loading={shotSaving}
              onClick={() =>
                selectedAssetId &&
                onApplyFirstFrame(shot.shotId, {
                  assetId: selectedAssetId,
                  mode: firstFrameMode,
                  panelIndex: firstFrameMode === 'CROPPED_PANEL' ? panelIndex : undefined,
                })
              }
            >
              绑定九宫格首帧
            </AppButton>
          </div>
        </div>
      ) : null}

      <div className="video-result-row">
        {task?.resultVideoFileId ? (
          <video className="video" src={resolveScriptFileUrl(task.resultVideoFileId)} controls preload="metadata" />
        ) : (
          <div className="video placeholder muted">视频结果将在生成完成后出现在这里</div>
        )}
        {projectId && task?.segmentTaskId ? (
          <AppButton size="sm" variant="ghost" onClick={() => setVideoHistOpen(true)}>
            视频历史
          </AppButton>
        ) : null}
      </div>
      {projectId && task?.segmentTaskId ? (
        <AssetHistoryPanel
          projectId={projectId}
          assetType="VIDEO"
          referenceId={task.segmentTaskId}
          title="视频片段历史"
          open={videoHistOpen}
          onClose={() => setVideoHistOpen(false)}
          onRestored={onVideoHistoryRestored}
        />
      ) : null}

      <div className="footer">
        <span className="muted">{task?.errorMessage || '任务就绪'}</span>
        {task?.segmentTaskId && task.status === 'FAILED' ? (
          <AppButton size="sm" loading={busy} onClick={() => onRetry(task.segmentTaskId)}>
            重试片段
          </AppButton>
        ) : null}
      </div>
    </article>
  )
}
