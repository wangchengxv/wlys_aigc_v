import { useEffect, useMemo, useState } from 'react'
import { resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { AssetHistoryPanel } from '@/components/script/AssetHistoryPanel'
import { PromptVersionsEditor } from '@/components/script/PromptVersionsEditor'
import { VideoReferenceImageField } from '@/components/workspace/VideoReferenceImageField'
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
  assets?: ExtractedAsset[]
  onApplyFirstFrame?: (
    shotId: string,
    payload: {
      assetId?: string
      mode?: 'NONE' | 'FULL_GRID' | 'CROPPED_PANEL' | 'ASSET_IMAGE' | 'UPLOADED_IMAGE'
      panelIndex?: number
      imageFileId?: string
      imageUrl?: string
    },
  ) => void
  onVideoHistoryRestored?: () => void | Promise<void>
}

const SHOT_TYPE_OPTIONS = ['ECU极特写', 'CU特写', 'MCU中近景', 'MS中景', 'MLS中远景', 'LS全景', 'ELS大远景'] as const
const CAMERA_MOVE_OPTIONS = ['static静止', 'pan摇', 'tilt俯仰', 'dolly推拉', 'truck横移', 'crane升降', 'handheld手持', 'zoom变焦'] as const
type FirstFrameSourceTab = 'upload' | 'asset-image' | 'storyboard'

function labelForFirstFrameMode(mode?: StoryboardShot['firstFrameMode']) {
  switch (mode) {
    case 'FULL_GRID':
      return '九宫格整图'
    case 'CROPPED_PANEL':
      return '九宫格单格'
    case 'ASSET_IMAGE':
      return '项目素材图'
    case 'UPLOADED_IMAGE':
      return '上传图片'
    case 'NONE':
      return '未绑定'
    default:
      return '首帧参考'
  }
}

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
  assets = [],
  onApplyFirstFrame,
  onVideoHistoryRestored,
}: Props) {
  const [videoHistOpen, setVideoHistOpen] = useState(false)
  const [shotType, setShotType] = useState(shot.shotType ?? '')
  const [cameraMove, setCameraMove] = useState(shot.cameraMove ?? '')
  const [emotion, setEmotion] = useState(shot.emotion ?? '')
  const [firstFrameTab, setFirstFrameTab] = useState<FirstFrameSourceTab>(
    shot.firstFrameMode === 'ASSET_IMAGE'
      ? 'asset-image'
      : shot.firstFrameMode === 'UPLOADED_IMAGE'
        ? 'upload'
        : 'storyboard',
  )
  const [selectedAssetId, setSelectedAssetId] = useState(shot.storyboardAssetId ?? '')
  const [firstFrameMode, setFirstFrameMode] = useState<'FULL_GRID' | 'CROPPED_PANEL'>(
    shot.firstFrameMode === 'CROPPED_PANEL' ? 'CROPPED_PANEL' : 'FULL_GRID',
  )
  const [panelIndex, setPanelIndex] = useState<number>(shot.storyboardCropIndex ?? 0)
  const [selectedImageFileId, setSelectedImageFileId] = useState(shot.firstFrameImageFileId ?? '')
  const [uploadedImageUrl, setUploadedImageUrl] = useState('')
  const [vpDraft, setVpDraft] = useState(shot.visualPrompt ?? '')

  useEffect(() => {
    setShotType(shot.shotType ?? '')
    setCameraMove(shot.cameraMove ?? '')
    setEmotion(shot.emotion ?? '')
    setFirstFrameTab(
      shot.firstFrameMode === 'ASSET_IMAGE'
        ? 'asset-image'
        : shot.firstFrameMode === 'UPLOADED_IMAGE'
          ? 'upload'
          : 'storyboard',
    )
    setSelectedAssetId(shot.storyboardAssetId ?? '')
    setFirstFrameMode(shot.firstFrameMode === 'CROPPED_PANEL' ? 'CROPPED_PANEL' : 'FULL_GRID')
    setPanelIndex(shot.storyboardCropIndex ?? 0)
    setSelectedImageFileId(shot.firstFrameImageFileId ?? '')
    setUploadedImageUrl('')
    setVpDraft(shot.visualPrompt ?? '')
  }, [
    shot.shotId,
    shot.shotType,
    shot.cameraMove,
    shot.emotion,
    shot.firstFrameImageFileId,
    shot.storyboardAssetId,
    shot.firstFrameMode,
    shot.storyboardCropIndex,
    shot.visualPrompt,
  ])

  const storyboardAssets = useMemo(() => assets.filter((item) => !!item.storyboardImageFileId), [assets])
  const selectedAsset = storyboardAssets.find((item) => item.assetId === shot.storyboardAssetId)
  const firstFramePreviewFileId = shot.firstFrameImageFileId || shot.storyboardCropFileId || shot.storyboardImageFileId
  const assetImageOptions = useMemo(() => {
    const items: Array<{ key: string; fileId: string; label: string }> = []
    for (const asset of assets) {
      const candidates = [
        asset.threeViewImageFileId ? { suffix: 'three', fileId: asset.threeViewImageFileId, label: `${asset.name} · 三视图` } : null,
        asset.turnaroundImageFileId ? { suffix: 'turn', fileId: asset.turnaroundImageFileId, label: `${asset.name} · 九宫格造型图` } : null,
        asset.storyboardImageFileId ? { suffix: 'story', fileId: asset.storyboardImageFileId, label: `${asset.name} · 分镜九宫格整图` } : null,
      ].filter(Boolean) as Array<{ suffix: string; fileId: string; label: string }>
      for (const candidate of candidates) {
        items.push({
          key: `${asset.assetId}-${candidate.suffix}`,
          fileId: candidate.fileId,
          label: candidate.label,
        })
      }
    }
    return items
  }, [assets])

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
            来源：{labelForFirstFrameMode(shot.firstFrameMode)}
            {selectedAsset ? ` · 资产：${selectedAsset.name}` : ''}
            {shot.storyboardCropIndex != null ? ` · 格子 #${shot.storyboardCropIndex + 1}` : ''}
          </p>
          {firstFramePreviewFileId ? (
            <img
              className="video"
              src={resolveScriptFileUrl(firstFramePreviewFileId)}
              alt="首帧参考"
            />
          ) : null}
        </div>
      ) : null}

      {onApplyFirstFrame ? (
        <div className="shot-firstframe-bind">
          <div className="head-actions">
            <div>
              <p className="eyebrow">第一步：选择首帧来源</p>
              <p className="muted">九宫格保留为可选来源，你也可以直接上传图片或从项目素材中点选。</p>
            </div>
          </div>

          <div className="shot-firstframe-tabs">
            <AppButton size="sm" variant={firstFrameTab === 'upload' ? 'primary' : 'ghost'} onClick={() => setFirstFrameTab('upload')}>
              上传图片
            </AppButton>
            <AppButton size="sm" variant={firstFrameTab === 'asset-image' ? 'primary' : 'ghost'} onClick={() => setFirstFrameTab('asset-image')}>
              项目素材
            </AppButton>
            <AppButton size="sm" variant={firstFrameTab === 'storyboard' ? 'primary' : 'ghost'} onClick={() => setFirstFrameTab('storyboard')}>
              九宫格选图
            </AppButton>
          </div>

          {firstFrameTab === 'upload' ? (
            <>
              <VideoReferenceImageField
                value={uploadedImageUrl}
                onChange={setUploadedImageUrl}
                label="上传图片作为首帧"
                placeholder="支持拖拽上传、粘贴图片链接，或粘贴 data:image/... base64"
              />
              <div className="shot-firstframe-actions">
                <AppButton
                  size="sm"
                  variant="primary"
                  loading={shotSaving}
                  onClick={() =>
                    uploadedImageUrl.trim() &&
                    onApplyFirstFrame(shot.shotId, {
                      mode: 'UPLOADED_IMAGE',
                      imageUrl: uploadedImageUrl.trim(),
                    })
                  }
                >
                  绑定上传图片
                </AppButton>
              </div>
            </>
          ) : null}

          {firstFrameTab === 'asset-image' ? (
            <>
              <label className="input-wrap">
                <span className="label">选择项目内已生成图片</span>
                <select className="ctrl" value={selectedImageFileId} onChange={(e) => setSelectedImageFileId(e.target.value)}>
                  <option value="">请选择三视图、九宫格造型图或分镜图</option>
                  {assetImageOptions.map((item) => (
                    <option key={item.key} value={item.fileId}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </label>
              {selectedImageFileId ? <img className="video" src={resolveScriptFileUrl(selectedImageFileId)} alt="项目素材首帧预览" /> : null}
              <div className="shot-firstframe-actions">
                <AppButton
                  size="sm"
                  variant="primary"
                  loading={shotSaving}
                  onClick={() =>
                    selectedImageFileId &&
                    onApplyFirstFrame(shot.shotId, {
                      mode: 'ASSET_IMAGE',
                      imageFileId: selectedImageFileId,
                    })
                  }
                >
                  绑定项目素材图
                </AppButton>
              </div>
            </>
          ) : null}

          {firstFrameTab === 'storyboard' ? (
            <>
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
                  <span className="label">使用方式</span>
                  <select
                    className="ctrl"
                    value={firstFrameMode}
                    onChange={(e) => setFirstFrameMode(e.target.value as 'FULL_GRID' | 'CROPPED_PANEL')}
                  >
                    <option value="FULL_GRID">整张九宫格</option>
                    <option value="CROPPED_PANEL">九宫格某一格</option>
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
            </>
          ) : null}
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
