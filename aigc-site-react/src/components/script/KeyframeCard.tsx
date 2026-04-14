import { useEffect, useState } from 'react'
import { resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { AssetHistoryPanel } from '@/components/script/AssetHistoryPanel'
import { PromptVersionsEditor } from '@/components/script/PromptVersionsEditor'
import type { KeyframeRecord } from '@/types'

type Props = {
  item: KeyframeRecord
  projectId?: string
  busy?: boolean
  onConfirm: (keyframeId: string) => void
  onRegenerate: (keyframeId: string) => void
  onSavePrompt?: (keyframeId: string, text: string) => void | Promise<void>
  onRollbackPrompt?: (keyframeId: string, versionId: string) => void | Promise<void>
  onHistoryRestored?: () => void | Promise<void>
}

export function KeyframeCard({
  item,
  projectId,
  busy,
  onConfirm,
  onRegenerate,
  onSavePrompt,
  onRollbackPrompt,
  onHistoryRestored,
}: Props) {
  const [draft, setDraft] = useState(item.promptText)
  const [historyOpen, setHistoryOpen] = useState(false)

  useEffect(() => {
    setDraft(item.promptText)
  }, [item.keyframeId, item.updatedAt, item.promptText])

  return (
    <article className="keyframe panel glass">
      {item.imageFileId ? (
        <img className="image" src={resolveScriptFileUrl(item.imageFileId)} alt={item.promptText} />
      ) : (
        <div className="image placeholder muted">暂无图像</div>
      )}
      <div className="content">
        {onSavePrompt && onRollbackPrompt ? (
          <PromptVersionsEditor
            label="关键帧提示词"
            value={draft}
            onChange={setDraft}
            versions={item.promptVersions ?? undefined}
            busy={busy}
            onSave={async () => {
              await onSavePrompt(item.keyframeId, draft.trim())
            }}
            onRollback={async (versionId) => {
              await onRollbackPrompt(item.keyframeId, versionId)
            }}
          />
        ) : (
          <p className="muted clamp">{item.promptText}</p>
        )}
        <div className="actions">
          <AppButton size="sm" variant={item.selected ? 'primary' : 'ghost'} loading={busy} onClick={() => onConfirm(item.keyframeId)}>
            {item.selected ? '已确认' : '确认选中'}
          </AppButton>
          <AppButton size="sm" loading={busy} onClick={() => onRegenerate(item.keyframeId)}>
            重新生成
          </AppButton>
          {projectId ? (
            <AppButton size="sm" variant="ghost" onClick={() => setHistoryOpen(true)}>
              历史版本
            </AppButton>
          ) : null}
        </div>
      </div>
      {projectId ? (
        <AssetHistoryPanel
          projectId={projectId}
          assetType="KEYFRAME"
          referenceId={item.keyframeId}
          open={historyOpen}
          onClose={() => setHistoryOpen(false)}
          onRestored={onHistoryRestored}
        />
      ) : null}
    </article>
  )
}
