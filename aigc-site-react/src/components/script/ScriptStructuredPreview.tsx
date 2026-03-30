function asRecordList(value: unknown): Record<string, unknown>[] {
  if (!Array.isArray(value)) return []
  return value.filter((item): item is Record<string, unknown> => item != null && typeof item === 'object')
}

function str(v: unknown): string {
  if (v == null) return ''
  if (typeof v === 'string') return v
  if (typeof v === 'number' || typeof v === 'boolean') return String(v)
  try {
    return JSON.stringify(v)
  } catch {
    return String(v)
  }
}

type Props = {
  structured: Record<string, unknown>
  subView: 'overview' | 'scenes' | 'characters' | 'props'
}

export function ScriptStructuredPreview({ structured, subView }: Props) {
  const title = str(structured.title)
  const summary = str(structured.summary)
  const scenes = asRecordList(structured.scenes)
  const segments = asRecordList(structured.segments)
  const characters = asRecordList(structured.characters)
  const props = asRecordList(structured.props)

  if (subView === 'overview') {
    return (
      <div className="script-structured-preview">
        <div className="script-preview-block">
          <h4>标题</h4>
          <p>{title || '—'}</p>
        </div>
        <div className="script-preview-block">
          <h4>摘要</h4>
          <p className="script-preview-body">{summary || '—'}</p>
        </div>
        <div className="script-preview-stats muted">
          <span>场次 {scenes.length}</span>
          <span>分段 {segments.length}</span>
          <span>角色 {characters.length}</span>
          <span>道具 {props.length}</span>
        </div>
      </div>
    )
  }

  if (subView === 'scenes') {
    return (
      <div className="script-structured-preview script-preview-scene-list">
        {scenes.length === 0 ? (
          <p className="muted">暂无场次数据，请先完善剧本。</p>
        ) : (
          scenes.map((scene, i) => (
            <article key={str(scene.id) || `${i}`} className="script-preview-card">
              <header>
                <strong>{str(scene.title) || `场景 ${i + 1}`}</strong>
                <span className="muted">
                  {str(scene.location)} · {str(scene.time)}
                </span>
              </header>
              <p>{str(scene.summary)}</p>
              {scene.estimatedDurationSec != null && str(scene.estimatedDurationSec) && (
                <p className="script-preview-note">
                  <em>预计时长</em> {str(scene.estimatedDurationSec)}秒
                </p>
              )}
              {scene.shootingNotes != null && str(scene.shootingNotes) && (
                <p className="script-preview-note">
                  <em>拍摄</em> {str(scene.shootingNotes)}
                </p>
              )}
              {scene.blocking != null && str(scene.blocking) && (
                <p className="script-preview-note">
                  <em>走位</em> {str(scene.blocking)}
                </p>
              )}
            </article>
          ))
        )}
        {segments.length > 0 && (
          <div className="script-preview-block">
            <h4>分段</h4>
            <ol className="script-segment-list">
              {segments.map((seg, i) => (
                <li key={str(seg.id) || `${i}`}>
                  <strong>{str(seg.title) || `分段 ${i + 1}`}</strong>
                  <p className="script-preview-body">{str(seg.scriptText)}</p>
                  {(seg.shootingNotes != null && str(seg.shootingNotes)) || (seg.blocking != null && str(seg.blocking)) ? (
                    <p className="muted small">
                      {str(seg.shootingNotes) && <>拍摄：{str(seg.shootingNotes)} </>}
                      {str(seg.blocking) && <>走位：{str(seg.blocking)}</>}
                    </p>
                  ) : null}
                  {seg.estimatedDurationSec != null && str(seg.estimatedDurationSec) ? (
                    <p className="muted small">预计时长：{str(seg.estimatedDurationSec)}秒</p>
                  ) : null}
                </li>
              ))}
            </ol>
          </div>
        )}
      </div>
    )
  }

  if (subView === 'characters') {
    return (
      <div className="script-structured-preview script-preview-char-grid">
        {characters.length === 0 ? (
          <p className="muted">暂无角色数据。</p>
        ) : (
          characters.map((c, i) => (
            <article key={str(c.id) || `${i}`} className="script-preview-card">
              <h4>{str(c.name) || `角色 ${i + 1}`}</h4>
              <p>{str(c.description)}</p>
              {c.persona != null && str(c.persona) && (
                <p className="script-preview-note">
                  <em>人设</em> {str(c.persona)}
                </p>
              )}
              {c.traits != null && str(c.traits) && (
                <p className="script-preview-note">
                  <em>特质</em> {str(c.traits)}
                </p>
              )}
              {c.quirks != null && str(c.quirks) && (
                <p className="script-preview-note">
                  <em>记忆点</em> {str(c.quirks)}
                </p>
              )}
              {c.relationships != null && str(c.relationships) && (
                <p className="script-preview-note">
                  <em>关系</em> {str(c.relationships)}
                </p>
              )}
            </article>
          ))
        )}
      </div>
    )
  }

  return (
    <div className="script-structured-preview script-preview-prop-grid">
      {props.length === 0 ? (
        <p className="muted">暂无道具数据。</p>
      ) : (
        props.map((p, i) => (
          <article key={str(p.id) || `${i}`} className="script-preview-card">
            <h4>{str(p.name) || `道具 ${i + 1}`}</h4>
            <p>{str(p.description)}</p>
            {p.creativeUse != null && str(p.creativeUse) && (
              <p className="script-preview-note">
                <em>巧思</em> {str(p.creativeUse)}
              </p>
            )}
            {p.importance != null && str(p.importance) && (
              <p className="script-preview-note">
                <em>重要性</em> {str(p.importance)}
              </p>
            )}
            {p.sceneRefs != null && str(p.sceneRefs) && (
              <p className="script-preview-note">
                <em>关联场景</em> {str(p.sceneRefs)}
              </p>
            )}
          </article>
        ))
      )}
    </div>
  )
}
