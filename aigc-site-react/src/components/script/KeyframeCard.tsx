import { resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import type { KeyframeRecord } from '@/types'

type Props = {
  item: KeyframeRecord
  busy?: boolean
  onConfirm: (keyframeId: string) => void
  onRegenerate: (keyframeId: string) => void
}

export function KeyframeCard({ item, busy, onConfirm, onRegenerate }: Props) {
  return (
    <article className="keyframe panel glass">
      {item.imageFileId ? (
        <img className="image" src={resolveScriptFileUrl(item.imageFileId)} alt={item.promptText} />
      ) : (
        <div className="image placeholder muted">暂无图像</div>
      )}
      <div className="content">
        <p className="muted clamp">{item.promptText}</p>
        <div className="actions">
          <AppButton size="sm" variant={item.selected ? 'primary' : 'ghost'} loading={busy} onClick={() => onConfirm(item.keyframeId)}>
            {item.selected ? '已确认' : '确认选中'}
          </AppButton>
          <AppButton size="sm" loading={busy} onClick={() => onRegenerate(item.keyframeId)}>
            重新生成
          </AppButton>
        </div>
      </div>
    </article>
  )
}
