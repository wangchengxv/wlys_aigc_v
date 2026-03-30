import { useState } from 'react'
import { Link } from 'react-router-dom'
import { resolveScriptFileUrl } from '@/api'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { ScriptProjectSummary } from '@/types'

type Props = {
  project: ScriptProjectSummary
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function ScriptProjectCard({ project }: Props) {
  const { showToast } = useToast()
  const removeProject = useScriptProjectStore((s) => s.removeProject)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)

  const coverUrl = resolveScriptFileUrl(project.coverFileId)

  async function handleDeleteConfirm() {
    setDeleting(true)
    try {
      await removeProject(project.projectId)
      setShowDeleteConfirm(false)
      showToast('剧本项目已删除', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '删除失败', 'error')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <article className="project-card panel glass">
      {coverUrl ? (
        <div className="cover-wrap">
          <img className="cover" src={coverUrl} alt={project.name} />
        </div>
      ) : (
        <div className="cover placeholder">
          <span>{project.name.slice(0, 1)}</span>
        </div>
      )}
      <div className="content">
        <div className="top-line">
          <h3>{project.name}</h3>
          <span className="status">{project.status}</span>
        </div>
        <p className="summary muted">{project.scriptSummary || '暂无摘要，创建后可先完善剧本。'}</p>
        <div className="meta">
          <span>{project.visualStyle}</span>
          <span>{project.aspectRatio}</span>
          <span>{project.targetDuration} 秒</span>
        </div>
        <div className="counts muted">
          <span>资产 {project.assetCount}</span>
          <span>关键帧 {project.keyframeCount}</span>
          <span>视频任务 {project.videoTaskCount}</span>
        </div>
        <div className="footer">
          <span className="muted">{formatDate(project.updatedAt)}</span>
          <div className="links">
            <Link to={`/script-projects/${project.projectId}`}>详情</Link>
            <Link to={`/script-projects/${project.projectId}/preview`}>预览</Link>
            <button
              type="button"
              className="btn-delete"
              disabled={deleting}
              onClick={() => setShowDeleteConfirm(true)}
            >
              删除
            </button>
          </div>
        </div>
      </div>

      <ConfirmDialog
        visible={showDeleteConfirm}
        title="删除剧本项目"
        message="确定删除该项目吗？剧本、资产与生成记录将一并删除且不可恢复。"
        confirmText="删除"
        cancelText="取消"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setShowDeleteConfirm(false)}
      />
    </article>
  )
}
