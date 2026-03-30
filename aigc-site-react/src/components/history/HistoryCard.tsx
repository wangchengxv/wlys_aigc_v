import { AppButton } from '@/components/common/AppButton'
import type { GenerateResponse } from '@/types'

type Props = {
  task: GenerateResponse
  onOpen: () => void
  onRemove: () => void
}

function statusText(status: GenerateResponse['status']) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'PROCESSING') return '处理中'
  return '失败'
}

function modeText(mode: GenerateResponse['mode']) {
  if (mode === 'text') return '仅文本'
  if (mode === 'image') return '仅图片'
  if (mode === 'video') return '仅视频'
  return '图文一起'
}

export function HistoryCard({ task, onOpen, onRemove }: Props) {
  return (
    <article className="history-card panel glass">
      <div className="meta">
        <span className={`status s-${task.status.toLowerCase()}`}>{statusText(task.status)}</span>
        <span className="muted">{new Date(task.createdAt).toLocaleString()}</span>
      </div>
      <p className="title">{task.prompt}</p>
      <p className="sub muted">
        {modeText(task.mode)} | {task.style} | 文{task.textResults.length} 图{task.imageResults.length} 视{task.videoResults.length} | {task.latencyMs}ms
      </p>
      <div className="actions">
        <AppButton size="sm" onClick={onOpen}>
          查看并重生成
        </AppButton>
        <AppButton size="sm" variant="danger" onClick={onRemove}>
          删除记录
        </AppButton>
      </div>
    </article>
  )
}
