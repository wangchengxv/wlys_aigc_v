import { useEffect, useMemo, useState } from 'react'
import { EmptyState } from '@/components/common/EmptyState'
import { CompactFilterBar } from '@/components/common/CompactFilterBar'
import { StatStrip } from '@/components/common/StatStrip'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { useToast } from '@/context/ToastContext'
import { getMediaResources } from '@/api'
import { useAuthStore } from '@/stores/authStore'
import type { MediaResource } from '@/types'

function fmt(value?: string | null) {
  if (!value) return '未记录'
  const time = new Date(value)
  return Number.isNaN(time.getTime()) ? value : time.toLocaleString('zh-CN')
}

function formatBytes(size: number) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

export function MediaResourcesPage() {
  const { showToast } = useToast()
  const user = useAuthStore((s) => s.user)
  const [loading, setLoading] = useState(true)
  const [resources, setResources] = useState<MediaResource[]>([])
  const [keyword, setKeyword] = useState('')

  useEffect(() => {
    void (async () => {
      setLoading(true)
      try {
        setResources(await getMediaResources())
      } catch (error) {
        showToast(error instanceof Error ? error.message : '加载媒体资源目录失败', 'error')
      } finally {
        setLoading(false)
      }
    })()
  }, [showToast])

  const filtered = useMemo(() => {
    if (!keyword.trim()) return resources
    const lower = keyword.trim().toLowerCase()
    return resources.filter((item) =>
      [item.fileId, item.fileName, item.projectId, item.objectKey, item.bucketName, item.mediaType]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(lower),
    )
  }, [keyword, resources])

  if (user?.role !== 'ADMIN' && user?.role !== 'TEACHER') {
    return <EmptyState title="仅管理员或教师可访问" description="媒体资源目录用于查看统一存储记录，请使用管理员或教师账号登录后访问。" />
  }

  return (
    <section className="resource-page">
      <p className="resource-page__description muted">集中查看素材入库记录、存储键和可访问链接，便于排查导出与回溯问题。</p>

      <StatStrip
        items={[
          { key: 'total', label: '资源总数', value: resources.length },
          { key: 'visible', label: '筛选结果', value: filtered.length },
          { key: 'provider', label: '存储提供方', value: 'LOCAL' },
        ]}
      />

      <CompactFilterBar
        title="资源筛选"
        summary={<span>{filtered.length} 条记录</span>}
      >
        <label className="input-wrap">
          <span className="label">关键词</span>
          <input className="ctrl" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索文件名、项目、Object Key" />
        </label>
      </CompactFilterBar>

      {loading ? (
        <LoadingSpinner />
      ) : filtered.length ? (
        <div className="resource-table">
          {filtered.map((item) => (
            <article key={item.fileId} className="resource-table__row">
              <div>
                <strong>{item.fileName || item.fileId}</strong>
                <span>{item.mediaType || 'application/octet-stream'}</span>
              </div>
              <div>
                <span>项目</span>
                <strong>{item.projectId || '未关联'}</strong>
              </div>
              <div>
                <span>大小</span>
                <strong>{formatBytes(item.sizeBytes)}</strong>
              </div>
              <div>
                <span>存储键</span>
                <strong>{item.objectKey || item.relativePath || '未记录'}</strong>
              </div>
              <div>
                <span>时间</span>
                <strong>{fmt(item.createdAt)}</strong>
              </div>
              <div className="resource-table__actions">
                {item.publicUrl ? (
                  <a className="app-btn v-ghost s-sm" href={item.publicUrl} target="_blank" rel="noreferrer">
                    打开
                  </a>
                ) : (
                  <span className="muted">无公开链接</span>
                )}
              </div>
            </article>
          ))}
        </div>
      ) : (
        <EmptyState title="还没有媒体资源记录" description="等剧本、素材、图片或视频文件入库后，这里会统一展示存储记录。" />
      )}

    </section>
  )
}
