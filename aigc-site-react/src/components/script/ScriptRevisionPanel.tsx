import { AppButton } from '@/components/common/AppButton'
import type { ScriptRevision } from '@/types'

type Props = {
  revisions: ScriptRevision[]
  loading: boolean
  restoringId: string | null
  onRestore: (revisionId: string) => void
}

const kindLabel: Record<string, string> = {
  REFINE: '完善前',
  USER_EDIT: '编辑前',
  OPTIMIZE_SCENE: '场景优化前',
  OPTIMIZE_CHARACTER: '角色优化前',
  OPTIMIZE_PROP: '道具优化前',
  RESTORE: '恢复前',
  IMPORT: '导入前',
  BEFORE_UPDATE: '更新前',
}

export function ScriptRevisionPanel({ revisions, loading, restoringId, onRestore }: Props) {
  if (loading && revisions.length === 0) {
    return <p className="muted">加载修订记录…</p>
  }
  if (revisions.length === 0) {
    return <p className="muted">暂无修订快照（保存或覆盖完善稿后会自动生成）。</p>
  }
  return (
    <ul className="script-revision-list">
      {revisions.map((r) => (
        <li key={r.revisionId} className="script-revision-item">
          <div>
            <span className="script-revision-kind">{kindLabel[r.kind] ?? r.kind}</span>
            <span className="muted">
              #{r.revisionIndex} · {r.label} · {new Date(r.createdAt).toLocaleString()}
            </span>
          </div>
          <AppButton
            variant="ghost"
            disabled={restoringId === r.revisionId}
            loading={restoringId === r.revisionId}
            onClick={() => onRestore(r.revisionId)}
          >
            恢复到此版本
          </AppButton>
        </li>
      ))}
    </ul>
  )
}
