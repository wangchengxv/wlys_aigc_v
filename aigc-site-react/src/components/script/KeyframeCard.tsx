import { useEffect, useRef, useState } from 'react'
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
  const [autosaveState, setAutosaveState] = useState<'idle' | 'saving' | 'error'>('idle')
  const initialSyncRef = useRef(true)

  useEffect(() => {
    setDraft(item.promptText)
    setAutosaveState('idle')
    initialSyncRef.current = true
  }, [item.keyframeId, item.updatedAt, item.promptText])

  useEffect(() => {
    if (!onSavePrompt) return
    if (initialSyncRef.current) {
      initialSyncRef.current = false
      return
    }
    if (draft.trim() === (item.promptText || '').trim()) return
    const timer = window.setTimeout(() => {
      setAutosaveState('saving')
      void Promise.resolve(onSavePrompt(item.keyframeId, draft.trim()))
        .then(() => setAutosaveState('idle'))
        .catch(() => setAutosaveState('error'))
    }, 1000)
    return () => window.clearTimeout(timer)
  }, [draft, item.keyframeId, item.promptText, onSavePrompt])

  return (
    <article className={`keyframe panel glass ${item.selected ? 'is-completed' : ''}`}>
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
          <AppButton size="sm" className={item.selected ? 'btn-completed' : ''} variant={item.selected ? 'ghost' : 'primary'} loading={busy} onClick={() => onConfirm(item.keyframeId)}>
            {item.selected ? '已完成确认' : '确认选中'}
          </AppButton>
          <AppButton size="sm" variant="ghost" loading={busy} onClick={() => onRegenerate(item.keyframeId)}>
            重新生成
          </AppButton>
          {projectId ? (
            <AppButton size="sm" variant="ghost" onClick={() => setHistoryOpen(true)}>
              历史版本
            </AppButton>
          ) : null}
        </div>
        {autosaveState !== 'idle' ? (
          <p className={`muted keyframe-autosave ${autosaveState === 'error' ? 'is-error' : ''}`}>
            {autosaveState === 'saving' ? '正在自动保存提示词…' : '自动保存失败，请稍后重试或手动保存'}
          </p>
        ) : null}
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
