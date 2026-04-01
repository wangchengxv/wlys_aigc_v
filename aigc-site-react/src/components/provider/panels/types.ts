import type { ProviderCatalogEntry, ProviderGatewayKind } from '@/types'

export type ProviderPanelProps = {
  catalogEntry?: ProviderCatalogEntry | null
  meta: Record<string, string>
  onChange: (next: Record<string, string>) => void
}

export function gatewayKindOf(entry: ProviderCatalogEntry | null | undefined): ProviderGatewayKind | undefined {
  return entry?.gatewayKind
}
