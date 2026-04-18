import { useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { resolveScriptFileUrl } from '@/api'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { ContentReviewStatus, ProjectStatus, ScriptProjectSummary } from '@/types'

type Props = {
  project: ScriptProjectSummary
  deletedView?: boolean
  primaryCta?: { label: string; to: string }
  secondaryCta?: { label: string; to: string }
  courseLabel?: string
  courseLifecycleLabel?: string
  classroomLabel?: string
  ownerLabel?: string
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function getProjectStatusLabel(status: ProjectStatus) {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'SCRIPT_REFINING':
      return '剧本处理中'
    case 'SCRIPT_READY':
      return '剧本就绪'
    case 'ASSET_READY':
      return '资产就绪'
    case 'VIDEO_GENERATING':
      return '视频生成中'
    case 'DUBBING_GENERATING':
      return '配音生成中'
    case 'LIP_SYNC_GENERATING':
      return '口型生成中'
    case 'FINAL_COMPOSITION_READY':
      return '成片就绪'
    case 'EXPORT_PACKAGE_READY':
    case 'COMPLETED':
      return '可交付'
    case 'FAILED':
    case 'PARTIAL_FAILED':
      return '异常'
    default:
      return '进行中'
  }
}

function getReviewStatusLabel(status?: ContentReviewStatus | null) {
  switch (status) {
    case 'PENDING':
      return '待审核'
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已驳回'
    case 'NOT_SUBMITTED':
    default:
      return '未提审'
  }
}

export function ScriptProjectCard({
  project,
  deletedView = false,
  primaryCta,
  secondaryCta,
  courseLabel,
  courseLifecycleLabel,
  classroomLabel,
  ownerLabel,
}: Props) {
  const { showToast } = useToast()
  const removeProject = useScriptProjectStore((s) => s.removeProject)
  const restoreProject = useScriptProjectStore((s) => s.restoreProject)
  const deleting = useScriptProjectStore((s) => s.deletingProjectIds.includes(project.projectId))
  const restoring = useScriptProjectStore((s) => s.restoringProjectIds.includes(project.projectId))
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const coverUrl = resolveScriptFileUrl(project.coverFileId)

  async function handleDeleteConfirm() {
    try {
      await removeProject(project.projectId)
      setShowDeleteConfirm(false)
      showToast('项目已移入回收站', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '删除失败', 'error')
    }
  }

  async function handleRestore() {
    try {
      await restoreProject(project.projectId)
      showToast('项目已恢复', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '恢复失败', 'error')
    }
  }

  return (
    <article className="project-summary-card">
      <div className="project-summary-card__cover">
        {coverUrl ? <img src={coverUrl} alt={project.name} /> : <span>{project.name.slice(0, 1)}</span>}
      </div>

      <div className="project-summary-card__body">
        <div className="project-summary-card__head">
          <div>
            <h3>{project.name}</h3>
            <p>{project.scriptSummary || '暂无摘要，进入项目后可继续完善剧本与流程。'}</p>
          </div>
          <div className="inline-badges">
            <span className="soft-badge">{getProjectStatusLabel(project.status)}</span>
            <span className="soft-badge">{getReviewStatusLabel(project.contentReviewStatus)}</span>
          </div>
        </div>

        <div className="project-summary-card__meta">
          <span>课程：{courseLabel || project.courseId || '未绑定课程'}</span>
          <span>状态：{courseLifecycleLabel || '课程进行中'}</span>
          <span>班级：{classroomLabel || '未绑定班级'}</span>
          <span>负责人：{ownerLabel || project.ownerName || project.ownerId || '未记录'}</span>
          <span>更新：{formatDate(project.updatedAt)}</span>
        </div>

        <div className="inline-badges">
          <span className="soft-badge">资产 {project.assetCount}</span>
          <span className="soft-badge">关键帧 {project.keyframeCount}</span>
          <span className="soft-badge">视频任务 {project.videoTaskCount}</span>
          <span className="soft-badge">{project.aspectRatio}</span>
        </div>

        <div className="project-summary-card__actions">
          {deletedView ? (
            <AppActionButton disabled={restoring} onClick={() => void handleRestore()}>
              {restoring ? '恢复中...' : '恢复项目'}
            </AppActionButton>
          ) : (
            <>
              <Link className="app-btn v-primary s-md" to={primaryCta?.to || `/script-projects/${project.projectId}`}>
                {primaryCta?.label || '进入工作区'}
              </Link>
              <Link className="app-btn v-ghost s-md" to={secondaryCta?.to || `/script-projects/${project.projectId}/export`}>
                {secondaryCta?.label || '导出与审核'}
              </Link>
              <button type="button" className="app-btn v-ghost s-md" disabled={deleting} onClick={() => setShowDeleteConfirm(true)}>
                {deleting ? '删除中...' : '删除'}
              </button>
            </>
          )}
        </div>
      </div>

      <ConfirmDialog
        visible={showDeleteConfirm}
        title="删除项目"
        message="确定将该项目移入回收站吗？之后仍可恢复。"
        confirmText="移入回收站"
        confirmLoading={deleting}
        disableCancel={deleting}
        cancelText="取消"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setShowDeleteConfirm(false)}
      />
    </article>
  )
}

function AppActionButton({
  children,
  disabled,
  onClick,
}: {
  children: ReactNode
  disabled?: boolean
  onClick: () => void
}) {
  return (
    <button type="button" className="app-btn v-primary s-md" disabled={disabled} onClick={onClick}>
      {children}
    </button>
  )
}
