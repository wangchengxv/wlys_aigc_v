import type { ModelConfig } from '@/types'

export type ModelCapability = 'text' | 'image' | 'video' | 'tts'
type ModelOptionLike = {
  modelName: string
  provider?: string | null
  enabled?: boolean
}

function capabilitiesOf(model: ModelConfig): string[] {
  const raw = model.metadata?.capabilities
  if (!Array.isArray(raw)) return []
  return raw.map((item) => String(item).trim().toLowerCase()).filter(Boolean)
}

export function getEnabledModelNames(models: ModelConfig[], capability: ModelCapability): string[] {
  const expected = capability.toLowerCase()
  return models
    .filter((model) => model.enabled && capabilitiesOf(model).includes(expected))
    .map((model) => model.modelName.trim())
    .filter(Boolean)
}

export function isEnabledModelName(models: ModelConfig[], capability: ModelCapability, value: string | null | undefined): boolean {
  const normalized = value?.trim()
  if (!normalized) return false
  return getEnabledModelNames(models, capability).includes(normalized)
}

export function sanitizeEnabledModelName(
  models: ModelConfig[],
  capability: ModelCapability,
  value: string | null | undefined,
): string | undefined {
  const normalized = value?.trim()
  if (!normalized) return undefined
  return isEnabledModelName(models, capability, normalized) ? normalized : undefined
}

export function buildEnabledModelConfigsFromOptions(
  capability: ModelCapability,
  options: string[],
  details?: ModelOptionLike[],
): ModelConfig[] {
  const detailMap = new Map(details?.map((item) => [item.modelName.trim(), item]) ?? [])
  return options
    .map((item) => item.trim())
    .filter(Boolean)
    .map((modelName, index) => {
      const detail = detailMap.get(modelName)
      return {
        id: `synthetic-${capability}-${index}-${modelName}`,
        name: modelName,
        provider: detail?.provider?.trim() || 'synthetic',
        modelName,
        connectionId: 'synthetic',
        enabled: detail?.enabled ?? true,
        metadata: {
          capabilities: [capability],
        },
        createdAt: '',
        updatedAt: '',
      }
    })
}
