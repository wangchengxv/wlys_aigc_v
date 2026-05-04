import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { VideoReferenceImageField } from '@/components/workspace/VideoReferenceImageField'
import { resolveApiMediaUrl, resolveScriptFileUrl } from '@/api'
import type { StoryboardLiteKeyframe, StoryboardLiteVideoTask, VideoModelOptionDetail } from '@/types'
import { ModelSelectorPanel, type ModelInputMode } from './ModelSelectorPanel'

export interface Step4VideoProps {
  videoSourceMode: 'selected-keyframe' | 'custom-upload'
  setVideoSourceMode: (v: 'selected-keyframe' | 'custom-upload') => void
  selectedKeyframe: StoryboardLiteKeyframe | null
  selectedKeyframePreviewUrl: string
  customVideoReferenceImageUrl: string
  setCustomVideoReferenceImageUrl: (v: string) => void
  videoModelInputMode: ModelInputMode
  setVideoModelInputMode: (v: ModelInputMode) => void
  videoModel: string
  setVideoModel: (v: string) => void
  customVideoModel: string
  setCustomVideoModel: (v: string) => void
  videoModelOptions: string[]
  videoModelDetails: VideoModelOptionDetail[]
  modelsLoading: boolean
  videoPrompt: string
  setVideoPrompt: (v: string) => void
  onGenerateVideo: () => Promise<void>
  busy: boolean
  videoTasks: StoryboardLiteVideoTask[]
}

function renderVideo(task: StoryboardLiteVideoTask) {
  const src = resolveApiMediaUrl(task.videoUrl || resolveScriptFileUrl(task.resultVideoFileId))
  if (!src) return null
  return <video className="sl-video-player" controls src={src} />
}

export function Step4Video({
  videoSourceMode,
  setVideoSourceMode,
  selectedKeyframe,
  selectedKeyframePreviewUrl,
  customVideoReferenceImageUrl,
  setCustomVideoReferenceImageUrl,
  videoModelInputMode,
  setVideoModelInputMode,
  videoModel,
  setVideoModel,
  customVideoModel,
  setCustomVideoModel,
  videoModelOptions,
  videoModelDetails,
  modelsLoading,
  videoPrompt,
  setVideoPrompt,
  onGenerateVideo,
  busy,
  videoTasks,
}: Step4VideoProps) {
  return (
    <div className="sl-step-content">
      <div className="sl-card">
        <h2 className="sl-card-title">第四步：图生视频配置</h2>
        <p className="sl-card-desc">配置首帧来源、选择视频生成模型并填写提示词。</p>

        <div className="sl-form-grid">
          <div className="sl-form-row">
            <div style={{ display: 'flex', gap: 'var(--space-md)', alignItems: 'center' }}>
              <span className="label" style={{ fontWeight: 600 }}>首帧来源：</span>
              <AppButton
                size="sm"
                variant={videoSourceMode === 'selected-keyframe' ? 'primary' : 'ghost'}
                onClick={() => setVideoSourceMode('selected-keyframe')}
              >
                已确认关键帧
              </AppButton>
              <AppButton
                size="sm"
                variant={videoSourceMode === 'custom-upload' ? 'primary' : 'ghost'}
                onClick={() => setVideoSourceMode('custom-upload')}
              >
                上传图片
              </AppButton>
            </div>
          </div>

          {videoSourceMode === 'selected-keyframe' ? (
            selectedKeyframe ? (
              <div
                style={{
                  display: 'flex',
                  gap: 'var(--space-lg)',
                  background: 'var(--surface)',
                  padding: 'var(--space-md)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--line)',
                  alignItems: 'center',
                }}
              >
                <div style={{ width: '160px', height: '90px', background: '#000', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
                  {selectedKeyframePreviewUrl ? (
                    <img
                      src={selectedKeyframePreviewUrl}
                      alt="已确认关键帧"
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  ) : (
                    <span className="muted" style={{ padding: '8px' }}>无预览</span>
                  )}
                </div>
                <div style={{ flex: 1 }}>
                  <p style={{ fontWeight: 600, marginBottom: 'var(--space-xs)' }}>当前将使用已确认关键帧作为首帧参考</p>
                  <p className="muted" style={{ fontSize: 13 }}>{selectedKeyframe.promptText || '无提示词记录'}</p>
                </div>
              </div>
            ) : (
              <p className="muted" style={{ padding: 'var(--space-md) 0' }}>还没有已确认关键帧，请返回上一步确认，或切换到“上传图片”。</p>
            )
          ) : (
            <VideoReferenceImageField
              value={customVideoReferenceImageUrl}
              onChange={setCustomVideoReferenceImageUrl}
              label="上传图片作为视频首帧"
              placeholder="支持拖拽上传、粘贴图片 URL，或粘贴 data:image/... base64"
            />
          )}

          <ModelSelectorPanel
            variant="video"
            title="视频生成模型"
            description="根据镜头风格选择视频模型，模型和提示词共同影响画面表现。"
            mode={videoModelInputMode}
            onModeChange={setVideoModelInputMode}
            selectedModel={videoModel}
            onSelectedModelChange={setVideoModel}
            customModel={customVideoModel}
            onCustomModelChange={setCustomVideoModel}
            options={videoModelOptions}
            details={videoModelDetails}
            loading={modelsLoading}
            helperText="建议先用预设模型快速试片，再切换自定义模型做风格微调。"
            customPlaceholder="例如：viduq3-turbo"
          />

          <AppInput
            as="textarea"
            rows={3}
            label="视频提示词"
            value={videoPrompt}
            onChange={(v) => setVideoPrompt(String(v))}
            placeholder="例如：请基于参考图生成5秒电影感镜头。"
          />
        </div>

        <div className="sl-actions">
          <AppButton variant="primary" loading={busy} onClick={() => void onGenerateVideo()}>
            {videoSourceMode === 'selected-keyframe' ? '从已确认关键帧生成视频' : '从上传图片生成视频'}
          </AppButton>
        </div>
      </div>

      {videoTasks.length > 0 && (
        <div className="sl-card" style={{ marginTop: 'var(--space-md)' }}>
          <h2 className="sl-card-title">生成结果</h2>
          <p className="sl-card-desc">您在此次会话中生成的视频任务记录。</p>
          <div className="sl-assets-grid">
            {videoTasks.map((task) => (
              <article key={task.videoTaskId} className="sl-asset-card">
                {renderVideo(task)}
                <div className="sl-asset-content">
                  <p className="sl-asset-meta">
                    <span style={{ fontWeight: 600 }}>状态：{task.status}</span>
                  </p>
                  <p className="sl-asset-meta">
                    <span>{task.modelName ? `模型：${task.modelName}` : '未知模型'}</span>
                  </p>
                </div>
              </article>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
