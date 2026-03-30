import { resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import type { StoryboardShot, VideoSegmentTask } from '@/types'

type Props = {
  shot: StoryboardShot
  task?: VideoSegmentTask
  busy?: boolean
  onRetry: (taskId: string) => void
}

export function VideoSegmentCard({ shot, task, busy, onRetry }: Props) {
  return (
    <article className="segment panel glass">
      <div className="head">
        <div>
          <p className="eyebrow">Shot {shot.sequenceNo}</p>
          <h3>{shot.title}</h3>
        </div>
        <span className="status">{task?.status || shot.status}</span>
      </div>

      <p className="muted">{shot.scriptText}</p>
      <div className="meta">
        <span>动作：{shot.actionSummary}</span>
        <span>运镜：{shot.cameraMovement}</span>
      </div>

      {task?.resultVideoFileId ? (
        <video className="video" src={resolveScriptFileUrl(task.resultVideoFileId)} controls preload="metadata" />
      ) : (
        <div className="video placeholder muted">视频结果将在生成完成后出现在这里</div>
      )}

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
