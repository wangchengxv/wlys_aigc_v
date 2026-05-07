import type { ProviderCatalogEntry } from '@/types'
import { renderProviderConnectionPanel } from '@/components/provider/panels/registry'

type Props = {
  catalogEntry?: ProviderCatalogEntry | null
  meta: Record<string, string>
  onChange: (next: Record<string, string>) => void
}

/** Delegates to `panels/registry` for gateway-specific fields (Cherry-style sub-panels). */
export function ProviderConnectionFields({ catalogEntry, meta, onChange }: Props) {
  const panel = renderProviderConnectionPanel({ catalogEntry, meta, onChange })
  if (!panel) {
    return null
  }
  return (
    <div className="provider-connection-panels">
      <h4 className="form-section-title">服务商专属配置</h4>
      {panel}
    </div>
  )
}
