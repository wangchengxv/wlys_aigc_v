import { useCallback, useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { listAssetHistory, resolveScriptFileUrl, restoreAssetHistory } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import type { AssetGenerationHistoryItem, AssetHistoryType } from '@/types'

type Props = {
  projectId: string
  assetType: AssetHistoryType
  referenceId?: string | null
  title?: string
  open: boolean
  onClose: () => void
  onRestored?: () => void | Promise<void>
}

export function AssetHistoryPanel({ projectId, assetType, referenceId, title, open, onClose, onRestored }: Props) {
  const [items, setItems] = useState<AssetGenerationHistoryItem[]>([])
  const [loading, setLoading] = useState(false)
  const [restoringId, setRestoringId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!projectId) return
    setLoading(true)
    setError(null)
    try {
      const list = await listAssetHistory(projectId, {
        type: assetType,
        referenceId: referenceId || undefined,
      })
      setItems(list)
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载失败')
      setItems([])
    } finally {
      setLoading(false)
    }
  }, [projectId, assetType, referenceId])

  useEffect(() => {
    if (open) void load()
  }, [open, load])

  async function handleRestore(id: number) {
    setRestoringId(id)
    try {
      await restoreAssetHistory(projectId, id)
      await onRestored?.()
      onClose()
    } finally {
      setRestoringId(null)
    }
  }

  if (!open) return null

  const heading = title || '历史版本'

  return createPortal(
    <div className="asset-history-overlay" role="presentation" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="asset-history-modal panel glass" onClick={(e) => e.stopPropagation()}>
        <header className="asset-history-head">
          <h3>{heading}</h3>
          <AppButton size="sm" variant="ghost" onClick={onClose}>
            关闭
          </AppButton>
        </header>
        {error ? <p className="muted error-text">{error}</p> : null}
        {loading ? (
          <LoadingSpinner />
        ) : items.length === 0 ? (
          <p className="muted">暂无历史记录（重新生成后会保留上一版）。</p>
        ) : (
          <ul className="asset-history-list">
            {items.map((row) => (
              <li key={row.id} className="asset-history-row">
                <div className="asset-history-thumb-wrap">
                  {row.fileId && assetType === 'VIDEO' ? (
                    <video
                      className="asset-history-thumb"
                      src={resolveScriptFileUrl(row.fileId)}
                      muted
                      playsInline
                      preload="metadata"
                    />
                  ) : row.fileId ? (
                    <img className="asset-history-thumb" src={resolveScriptFileUrl(row.fileId)} alt="" />
                  ) : null}
                </div>
                <div className="asset-history-meta">
                  <span className="muted">#{row.id}</span>
                  {row.createdAt ? <span className="muted">{new Date(row.createdAt).toLocaleString()}</span> : null}
                  {row.modelName ? <span className="muted">模型 {row.modelName}</span> : null}
                  {row.promptText ? <p className="clamp small">{row.promptText}</p> : null}
                </div>
                <AppButton size="sm" variant="primary" loading={restoringId === row.id} onClick={() => void handleRestore(row.id)}>
                  恢复此版本
                </AppButton>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>,
    document.body,
  )
}
