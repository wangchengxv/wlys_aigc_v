import { AppButton } from '@/components/common/AppButton'
import type { PromptVersion, PromptVersionSource } from '@/types'

const SOURCE_LABELS: Record<PromptVersionSource, string> = {
  'ai-generated': 'AI',
  'manual-edit': '手动',
  rollback: '回滚',
  imported: '导入',
  system: '系统',
}

type Props = {
  label?: string
  value: string
  onChange: (next: string) => void
  versions?: PromptVersion[] | null
  onSave: () => void | Promise<void>
  onRollback: (versionId: string) => void | Promise<void>
  busy?: boolean
  placeholder?: string
}

export function PromptVersionsEditor({
  label,
  value,
  onChange,
  versions = [],
  onSave,
  onRollback,
  busy,
  placeholder = '输入提示词…',
}: Props) {
  const recent = [...(versions || [])].slice(-10).reverse()

  return (
    <div className="prompt-versions-editor">
      {label ? (
        <div className="visual-prompt-head">
          <span className="eyebrow">{label}</span>
        </div>
      ) : null}
      <textarea
        className="ctrl prompt-versions-editor__textarea"
        rows={6}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
      />
      <div className="prompt-versions-editor__actions">
        <AppButton size="sm" variant="primary" loading={busy} onClick={() => void Promise.resolve(onSave())}>
          保存提示词
        </AppButton>
      </div>
      {recent.length > 0 ? (
        <div className="prompt-versions-editor__history">
          <p className="eyebrow">历史版本</p>
          <ul className="prompt-version-list" role="list">
            {recent.map((v) => (
              <li key={v.id} className="prompt-version-row">
                <div className="prompt-version-meta">
                  <span>{SOURCE_LABELS[v.source] ?? v.source}</span>
                  <span className="muted">{new Date(v.createdAt).toLocaleString()}</span>
                </div>
                <AppButton size="sm" variant="ghost" onClick={() => void Promise.resolve(onRollback(v.id))}>
                  回滚
                </AppButton>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  )
}
