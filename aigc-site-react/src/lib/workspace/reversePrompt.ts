import type { ReversePromptResponse } from '@/types'

export function pickReversePromptDefaultModel(options: string[], defaultModel?: string | null): string {
  if (defaultModel && options.includes(defaultModel)) {
    return defaultModel
  }
  return options[0] || ''
}

export function buildReversePromptMarkdown(result: ReversePromptResponse): string {
  const rows = [
    '# 反推提示词结果',
    '',
    `- 模型：${result.model || '-'}`,
    '',
    '## 正向提示词',
    result.positivePrompt || '-',
    '',
    '## 反向提示词',
    result.negativePrompt || '-',
    '',
    '## 分析参数',
    `- 风格：${result.style || '-'}`,
    `- 光线：${result.lighting || '-'}`,
    `- 构图：${result.composition || '-'}`,
    `- 镜头：${result.camera || '-'}`,
    `- 色彩：${result.colorTone || '-'}`,
    '',
    '## 参数建议',
    JSON.stringify(result.parameters || {}, null, 2),
  ]
  return rows.join('\n')
}
