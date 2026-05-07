type Props = {
  meta: Record<string, string>
  onChange: (next: Record<string, string>) => void
}

/** Additional API keys (newline-separated) for rotation on 401; primary key remains above. */
export function ExtraApiKeysSection({ meta, onChange }: Props) {
  return (
    <div className="provider-extra-keys">
      <h4 className="form-section-title">附加 API Key（轮换）</h4>
      <p className="catalog-hint muted">每行一个，与主密钥一起在路由代理中按顺序尝试；仅在返回 401 时尝试下一密钥。</p>
      <label className="input-wrap">
        <span className="label">附加密钥</span>
        <textarea
          className="ctrl"
          rows={4}
          autoComplete="off"
          value={meta.extraApiKeys ?? ''}
          onChange={(e) => onChange({ ...meta, extraApiKeys: e.target.value })}
          placeholder="留空或每行一个密钥"
        />
      </label>
    </div>
  )
}
