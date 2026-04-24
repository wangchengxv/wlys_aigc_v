import test from 'node:test'
import assert from 'node:assert/strict'
import { buildReversePromptMarkdown, pickReversePromptDefaultModel } from './reversePrompt.ts'

test('优先使用服务端默认模型且必须在候选里', () => {
  assert.equal(pickReversePromptDefaultModel(['a', 'b'], 'b'), 'b')
  assert.equal(pickReversePromptDefaultModel(['a', 'b'], 'x'), 'a')
  assert.equal(pickReversePromptDefaultModel([], 'x'), '')
})

test('反推结果可稳定生成可复制 markdown', () => {
  const markdown = buildReversePromptMarkdown({
    model: 'doubao-vision-pro',
    positivePrompt: '电影感少女肖像',
    negativePrompt: '低清晰度',
    style: '写实',
    lighting: '柔光',
    composition: '三分法',
    camera: '50mm',
    colorTone: '暖色调',
    parameters: { aspectRatio: '3:4' },
    rawText: '{}',
  })
  assert.ok(markdown.includes('## 正向提示词'))
  assert.ok(markdown.includes('电影感少女肖像'))
  assert.ok(markdown.includes('"aspectRatio": "3:4"'))
})
