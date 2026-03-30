import type { ReactNode } from 'react'
import type { ProviderCatalogEntry } from '@/types'
import { AzurePanel } from './AzurePanel'
import { BedrockPanel } from './BedrockPanel'
import { LocalCompatPanel } from './LocalCompatPanel'
import type { ProviderPanelProps } from './types'
import { gatewayKindOf } from './types'
import { VertexPanel } from './VertexPanel'

/**
 * Renders provider-specific connection fields (Cherry-style sub-panels).
 */
export function renderProviderConnectionPanel(props: ProviderPanelProps): ReactNode {
  const { catalogEntry } = props
  const kind = gatewayKindOf(catalogEntry ?? undefined)
  const key = catalogEntry?.key ?? ''

  if (kind === 'AZURE_OPENAI' || key === 'azure_openai') {
    return <AzurePanel {...props} />
  }
  if (kind === 'BEDROCK' || key === 'aws_bedrock') {
    return <BedrockPanel {...props} />
  }
  if (kind === 'VERTEX' || key === 'vertex_ai') {
    return <VertexPanel {...props} />
  }
  if (kind === 'OPENAI_COMPAT' && (key === 'lm_studio' || key === 'ollama')) {
    return <LocalCompatPanel {...props} catalogEntry={catalogEntry as ProviderCatalogEntry} />
  }

  return null
}
