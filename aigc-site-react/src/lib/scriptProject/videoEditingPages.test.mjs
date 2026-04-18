import test from 'node:test'
import assert from 'node:assert/strict'
import { canPublishPreview, pickPreferredDeliverySource } from './videoEditingPageGuards.ts'

test('发布入口仅在预览命中当前草稿且无未保存改动时可用', () => {
  assert.equal(
    canPublishPreview({
      hasUnsavedChanges: true,
      draftVersion: 6,
      previewDraftVersion: 6,
    }),
    false,
  )

  assert.equal(
    canPublishPreview({
      hasUnsavedChanges: false,
      draftVersion: 7,
      previewDraftVersion: 6,
    }),
    false,
  )

  assert.equal(canPublishPreview({ hasUnsavedChanges: false, draftVersion: 6, previewDraftVersion: 6 }), true)
})

test('导出页优先消费已发布剪辑成片，再回退预览和自动成片', () => {
  const published = pickPreferredDeliverySource(
    {
      renderTasks: [
        {
          renderType: 'PUBLISH',
          status: 'SUCCESS',
          published: true,
          resultVideoFileId: 'file_publish',
        },
      ],
    },
    { resultVideoFileId: 'file_final', status: 'SUCCESS' },
  )
  assert.equal(published.fileId, 'file_publish')
  assert.equal(published.source, 'published')

  const previewOnly = pickPreferredDeliverySource(
    {
      renderTasks: [
        {
          renderType: 'PREVIEW',
          status: 'SUCCESS',
          published: false,
          resultVideoFileId: 'file_preview',
        },
      ],
    },
    { resultVideoFileId: 'file_final', status: 'SUCCESS' },
  )
  assert.equal(previewOnly.fileId, 'file_preview')
  assert.equal(previewOnly.source, 'preview')

  const fallback = pickPreferredDeliverySource(null, { resultVideoFileId: 'file_final', status: 'SUCCESS' })
  assert.equal(fallback.fileId, 'file_final')
  assert.equal(fallback.source, 'fallback')
})
