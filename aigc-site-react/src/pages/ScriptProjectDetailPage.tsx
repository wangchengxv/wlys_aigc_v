import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { resolveScriptFileUrl } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { EmptyState } from '@/components/common/EmptyState'
import { FixedPanelDock, type FixedPanelDockItem } from '@/components/common/FixedPanelDock'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PipelineProgressBar } from '@/components/script/PipelineProgressBar'
import { ScriptProjectWorkflowNav } from '@/components/script/ScriptProjectWorkflowNav'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { ContentReviewStatus, ProjectStatus } from '@/types'

export function ScriptProjectDetailPage() {
  const { projectId = '' } = useParams()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const pipelineStatus = useScriptProjectStore((s) => s.pipelineStatus)
  const contentReviewStatus = useScriptProjectStore((s) => s.contentReviewStatus)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadPipelineStatus = useScriptProjectStore((s) => s.loadPipelineStatus)
  const loadContentReviewStatus = useScriptProjectStore((s) => s.loadContentReviewStatus)
  const removeProject = useScriptProjectStore((s) => s.removeProject)
  const stopPolling = useScriptProjectStore((s) => s.stopPolling)
  const refineWithPrompt = useScriptProjectStore((s) => s.refineWithPrompt)
  const refinePromptLoading = useScriptProjectStore((s) => s.refinePromptLoading)

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [briefPrompt, setBriefPrompt] = useState('')
  const [activePanel, setActivePanel] = useState('overview')

  function formatDateTime(value?: string | null) {
    if (!value) return '未记录'
    const time = new Date(value)
    return Number.isNaN(time.getTime()) ? value : time.toLocaleString('zh-CN')
  }

  function reviewStatusLabel() {
    switch (contentReviewStatus?.status ?? p.contentReviewStatus) {
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

  function deliveryStatusLabel() {
    if (pipelineStatus?.exportPackageReady || p.status === 'EXPORT_PACKAGE_READY' || p.status === 'COMPLETED') {
      return '可交付'
    }
    if (
      p.status === 'EXPORT_PACKAGE_GENERATING' ||
      p.status === 'FINAL_COMPOSITION_GENERATING' ||
      p.status === 'LIP_SYNC_GENERATING' ||
      p.status === 'DUBBING_GENERATING' ||
      p.status === 'VIDEO_GENERATING'
    ) {
      return '交付处理中'
    }
    return '未达交付'
  }

  async function handleRefineWithPrompt() {
    const prompt = briefPrompt.trim()
    if (!prompt) {
      showToast('请输入短提示词', 'error')
      return
    }
    try {
      await refineWithPrompt(projectId, prompt)
      showToast('剧本完善功能完成', 'success')
      await navigate(`/script-projects/${projectId}/preview?tab=structured`)
    } catch (e) {
      showToast(e instanceof Error ? e.message : '完善剧本失败', 'error')
    }
  }

  async function handleDeleteConfirm() {
    setDeleting(true)
    try {
      await removeProject(projectId)
      stopPolling()
      setShowDeleteConfirm(false)
      showToast('剧本工程已移入回收站', 'success')
      await navigate('/script-projects')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '删除失败', 'error')
    } finally {
      setDeleting(false)
    }
  }

  useEffect(() => {
    if (!projectId) return
    void (async () => {
      try {
        await Promise.all([loadProject(projectId), loadPipelineStatus(projectId), loadContentReviewStatus(projectId)])
      } catch (e) {
        showToast(e instanceof Error ? e.message : '页面初始化失败，请重试', 'error')
      }
    })()
  }, [projectId, loadProject, loadPipelineStatus, loadContentReviewStatus, showToast])

  if (detailLoading && (!currentProject || currentProject.project.projectId !== projectId)) {
    return <LoadingSpinner />
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return (
      <EmptyState title="项目不存在" description="请返回列表重新选择项目。">
        <Link to="/script-projects">返回项目列表</Link>
      </EmptyState>
    )
  }

  const p = currentProject.project
  const reviewStatus = contentReviewStatus?.status ?? p.contentReviewStatus
  const reviewSubmittedAt = contentReviewStatus?.reviewSubmittedAt || p.reviewSubmittedAt
  const reviewedAt = contentReviewStatus?.reviewedAt || p.reviewedAt
  const reviewerName = contentReviewStatus?.reviewerUserName || p.reviewerUserName || p.reviewerUserId || '待分配'
  const latestReviewComment = contentReviewStatus?.latestReviewComment || p.latestReviewComment
  const coverUrl = resolveScriptFileUrl(currentProject.keyframes.find((item) => item.imageFileId)?.imageFileId)
  const statusCards = [
    { label: '文档版本', value: currentProject.documents.length, hint: '版本迭代' },
    { label: '已抽取资产', value: currentProject.assets.length, hint: '角色/场景/道具' },
    { label: '关键帧', value: currentProject.keyframes.length, hint: '可作为封面与汇报素材' },
    { label: '镜头任务', value: currentProject.videoTasks.length, hint: `${currentProject.shots.length} 个镜头` },
  ]
  const milestoneItems = [
    {
      label: '剧本定稿',
      detail: currentProject.documents.length > 1 ? '已有完善版与结构化文档，可继续预览与迭代。' : '当前仍以初始文本为主，建议先完善剧本。',
      tone: currentProject.documents.length > 1 ? 'success' : 'warning',
    },
    {
      label: '资产与关键帧',
      detail:
        currentProject.assets.length && currentProject.keyframes.length
          ? `已沉淀 ${currentProject.assets.length} 个资产、${currentProject.keyframes.length} 张关键帧。`
          : '资产或关键帧仍待补齐，建议前往资产页继续生成。',
      tone: currentProject.assets.length && currentProject.keyframes.length ? 'success' : 'warning',
    },
    {
      label: '视频生产',
      detail:
        pipelineStatus?.videoReady || currentProject.videoTasks.some((task) => task.resultVideoFileId)
          ? '镜头视频已进入可汇报阶段，可继续推进配音、口型同步与成片。'
          : '镜头视频尚未全部就绪，需要继续拆分镜头或生成视频。',
      tone: pipelineStatus?.videoReady || currentProject.videoTasks.some((task) => task.resultVideoFileId) ? 'success' : 'info',
    },
    {
      label: '治理与交付',
      detail:
        pipelineStatus?.exportPackageReady
          ? '导出包已就绪，可直接进入交付中心处理审核与下载。'
          : '尚未达到最终交付状态，但可提前查看缺口并规划后续动作。',
      tone: pipelineStatus?.exportPackageReady ? 'success' : 'warning',
    },
  ] as const
  const actionCards = [
    {
      title: '剧本预览',
      description: '查看结构化内容、汇报摘要与脚本版本。',
      to: `/script-projects/${projectId}/preview`,
      primary: true,
    },
    {
      title: '资产与关键帧',
      description: '继续补充角色、场景、道具和项目封面素材。',
      to: `/script-projects/${projectId}/assets`,
    },
    {
      title: '镜头与视频',
      description: '进入镜头拆分、视频生成与多任务生产链路。',
      to: `/script-projects/${projectId}/video`,
    },
    {
      title: '交付中心',
      description: '集中查看成片、导出包、审核状态和下载入口。',
      to: `/script-projects/${projectId}/export`,
    },
    {
      title: '审计日志',
      description: '追踪项目治理留痕与关键操作记录。',
      to: `/audit-logs?entityType=SCRIPT_PROJECT&entityId=${encodeURIComponent(projectId)}`,
    },
  ]
  const dockItems: FixedPanelDockItem[] = [
    {
      id: 'overview',
      label: '项目概览',
      eyebrow: 'Overview',
      summary: '基础信息和资产统计合并查看，减少重复扫读两块信息卡。',
      badge: `${statusCards.length} 项快照`,
      content: (
        <div className="delivery-showcase-grid">
          <article className="meta-card delivery-section-card">
            <div className="delivery-section-card__head">
              <div>
                <p className="eyebrow">基础信息</p>
                <h3>项目概览</h3>
              </div>
            </div>
            <div className="delivery-description-list">
              <div>
                <span>当前阶段</span>
                <strong>{getProjectStatusLabel(p.status)}</strong>
              </div>
              <div>
                <span>课程归属</span>
                <strong>{p.courseId || '未绑定课程'}</strong>
              </div>
              <div>
                <span>提交人</span>
                <strong>{p.ownerName || p.ownerId || '未记录'}</strong>
              </div>
              <div>
                <span>创建时间</span>
                <strong>{formatDateTime(p.createdAt)}</strong>
              </div>
              <div>
                <span>更新时间</span>
                <strong>{formatDateTime(p.updatedAt)}</strong>
              </div>
              <div>
                <span>交付判断</span>
                <strong>{deliveryStatusLabel()}</strong>
              </div>
            </div>
          </article>

          <article className="meta-card delivery-section-card">
            <div className="delivery-section-card__head">
              <div>
                <p className="eyebrow">成果矩阵</p>
                <h3>资产统计</h3>
              </div>
            </div>
            <div className="delivery-description-list">
              <div>
                <span>文档版本</span>
                <strong>{currentProject.documents.length}</strong>
              </div>
              <div>
                <span>已抽取资产</span>
                <strong>{currentProject.assets.length}</strong>
              </div>
              <div>
                <span>关键帧</span>
                <strong>{currentProject.keyframes.length}</strong>
              </div>
              <div>
                <span>镜头</span>
                <strong>{currentProject.shots.length}</strong>
              </div>
              <div>
                <span>视频任务</span>
                <strong>{currentProject.videoTasks.length}</strong>
              </div>
              <div>
                <span>导出就绪</span>
                <strong>{pipelineStatus?.exportPackageReady ? '是' : '否'}</strong>
              </div>
            </div>
          </article>
        </div>
      ),
    },
    {
      id: 'milestones',
      label: '里程碑与治理',
      eyebrow: 'Progress',
      summary: '查看生产成熟度、审核上下文和交付缺口，帮助快速判断下一步。',
      badge: `${milestoneItems.length} 个阶段`,
      content: (
        <div className="delivery-showcase-grid">
          <article className="meta-card delivery-section-card">
            <div className="delivery-section-card__head">
              <div>
                <p className="eyebrow">里程碑</p>
                <h3>生产与治理进度</h3>
              </div>
            </div>
            <div className="delivery-checklist">
              {milestoneItems.map((item) => (
                <div key={item.label} className={`delivery-checklist__item delivery-checklist__item--${item.tone}`}>
                  <div>
                    <strong>{item.label}</strong>
                    <p>{item.detail}</p>
                  </div>
                </div>
              ))}
            </div>
          </article>

          <article className="meta-card delivery-section-card">
            <div className="delivery-section-card__head">
              <div>
                <p className="eyebrow">治理上下文</p>
                <h3>审核与交付记录</h3>
              </div>
            </div>
            <div className="delivery-description-list">
              <div>
                <span>审核状态</span>
                <strong>{reviewStatusLabel()}</strong>
              </div>
              <div>
                <span>提审时间</span>
                <strong>{formatDateTime(reviewSubmittedAt)}</strong>
              </div>
              <div>
                <span>审核时间</span>
                <strong>{formatDateTime(reviewedAt)}</strong>
              </div>
              <div>
                <span>审核人</span>
                <strong>{reviewerName}</strong>
              </div>
              <div>
                <span>导出包</span>
                <strong>{pipelineStatus?.exportPackageReady ? '已就绪' : '未就绪'}</strong>
              </div>
              <div>
                <span>审计入口</span>
                <strong>支持回溯操作留痕</strong>
              </div>
            </div>
          </article>
        </div>
      ),
    },
    {
      id: 'links',
      label: '关键入口',
      eyebrow: 'Links',
      summary: '统一放置项目相关入口，避免按钮散落在多块卡片里。',
      badge: `${actionCards.length} 个入口`,
      content: (
        <div className="delivery-showcase-grid delivery-showcase-grid--single">
          <div className="delivery-section-card__head">
            <div>
              <p className="eyebrow">关键入口</p>
              <h3>成果展示与治理链路</h3>
            </div>
            <div className="delivery-action-cluster">
              {p.courseId ? (
                <Link className="nav-btn" to={`/courses/${encodeURIComponent(p.courseId)}`}>
                  返回所属课程
                </Link>
              ) : null}
              <Link className="nav-btn" to="/script-projects">
                返回项目库
              </Link>
            </div>
          </div>
          <div className="delivery-quick-link-grid">
            {actionCards.map((item) => (
              <Link key={item.title} className={`delivery-quick-link${item.primary ? ' delivery-quick-link--primary' : ''}`} to={item.to}>
                <strong>{item.title}</strong>
                <span>{item.description}</span>
              </Link>
            ))}
          </div>
        </div>
      ),
    },
    {
      id: 'actions',
      label: '创作调整',
      eyebrow: 'Actions',
      summary: '把剧本完善和危险操作收进单一工作台，避免首屏干扰项目信息。',
      badge: latestReviewComment ? '有审核意见' : '可继续打磨',
      content: (
        <div className="delivery-feedback-workbench">
          <div className="delivery-feedback-workbench__input">
            <AppInput
              value={briefPrompt}
              onChange={(v) => setBriefPrompt(String(v))}
              label="短提示词"
              as="textarea"
              rows={4}
              placeholder="例如：让节奏更紧凑，突出主角情绪变化。"
            />
          </div>
          <div className="delivery-feedback-workbench__actions">
            <AppButton variant="primary" loading={refinePromptLoading} onClick={() => void handleRefineWithPrompt()}>
              剧本完善功能
            </AppButton>
            <Link className="nav-btn" to={`/script-projects/${projectId}/preview?tab=structured`}>
              查看结构化剧本
            </Link>
            <Link className="nav-btn" to={`/script-projects/${projectId}/export`}>
              查看交付中心
            </Link>
            <button
              type="button"
              className="nav-btn danger"
              disabled={deleting}
              onClick={() => setShowDeleteConfirm(true)}
            >
              删除剧本
            </button>
          </div>
        </div>
      ),
    },
  ]

  return (
    <div className="script-project-workflow-layout">
      <div className="script-project-workflow-layout__main">
        <section className="script-detail-page delivery-showcase-page">
          <div className="delivery-hero panel glass">
            <div className="delivery-hero__media">
              <p className="eyebrow">成果封面</p>
              {coverUrl ? (
                <img className="delivery-hero__poster" src={coverUrl} alt={`${p.name} 项目封面`} />
              ) : (
                <div className="delivery-hero__poster delivery-hero__poster--placeholder">
                  <span>{p.name.slice(0, 1)}</span>
                </div>
              )}
              <p className="delivery-hero__caption muted">
                {coverUrl ? '封面优先使用现有关键帧，适合答辩、汇报与成果展示。' : '当前还没有可用封面，生成关键帧后会自动增强展示效果。'}
              </p>
            </div>

            <div className="delivery-hero__content">
              <p className="eyebrow">Project Showcase</p>
              <h2>{p.name}</h2>
              <p className="delivery-hero__summary muted">{p.scriptSummary || '暂无摘要，可通过剧本完善继续补充故事亮点与成果说明。'}</p>

              <div className="script-detail-page__hero-meta">
                <span>课程：{p.courseId || '未绑定课程'}</span>
                <span>提交人：{p.ownerName || p.ownerId || '未记录'}</span>
                <span>风格：{p.visualStyle}</span>
                <span>比例：{p.aspectRatio}</span>
                <span>时长：{p.targetDuration} 秒</span>
                <span>语言：{p.language}</span>
              </div>

              <div className="delivery-status-row">
                <span className={getStatusBadgeClass(getProjectTone(p.status))}>项目阶段 · {getProjectStatusLabel(p.status)}</span>
                <span className={getStatusBadgeClass(getReviewTone(reviewStatus))}>审核状态 · {reviewStatusLabel()}</span>
                <span className={getStatusBadgeClass(getDeliveryTone(deliveryStatusLabel()))}>交付状态 · {deliveryStatusLabel()}</span>
              </div>

              <div className={`delivery-feedback-note${latestReviewComment ? '' : ' delivery-feedback-note--neutral'}`}>
                {latestReviewComment ? `最新审核意见：${latestReviewComment}` : '当前没有最新审核意见，可先完成成果自检后再进入提审。'}
              </div>
            </div>

            <aside className="delivery-hero__aside">
              <div className="delivery-section-card__head">
                <div>
                  <p className="eyebrow">项目快照</p>
                  <h3>成果与治理摘要</h3>
                </div>
              </div>
              <div className="delivery-kpi-grid delivery-kpi-grid--compact">
                {statusCards.map((item) => (
                  <article key={item.label} className="delivery-kpi-card">
                    <strong>{item.value}</strong>
                    <span>{item.label}</span>
                    <small>{item.hint}</small>
                  </article>
                ))}
              </div>
            </aside>
          </div>

          <ScriptProjectWorkflowNav projectId={projectId} />

          <ConfirmDialog
            visible={showDeleteConfirm}
            title="删除剧本工程"
            message="确定将该项目移入回收站吗？你之后可以在回收站恢复。"
            confirmText="移入回收站"
            confirmLoading={deleting}
            disableCancel={deleting}
            cancelText="取消"
            onConfirm={handleDeleteConfirm}
            onCancel={() => setShowDeleteConfirm(false)}
          />

          <FixedPanelDock
            title="项目详情面板"
            description="默认只展示封面、状态和流程条，其余内容通过固定触发区按需展开。"
            items={dockItems}
            activeId={activePanel}
            onChange={setActivePanel}
          />

          <PipelineProgressBar pipeline={pipelineStatus} />
        </section>
      </div>
    </div>
  )
}

function getProjectStatusLabel(status: ProjectStatus) {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'SCRIPT_REFINING':
      return '剧本完善中'
    case 'SCRIPT_READY':
      return '剧本已就绪'
    case 'ASSET_EXTRACTING':
      return '资产抽取中'
    case 'ASSET_READY':
      return '资产已就绪'
    case 'KEYFRAME_GENERATING':
      return '关键帧生成中'
    case 'KEYFRAME_READY':
      return '关键帧已就绪'
    case 'VIDEO_GENERATING':
      return '视频生成中'
    case 'DUBBING_GENERATING':
      return '配音生成中'
    case 'LIP_SYNC_GENERATING':
      return '口型同步中'
    case 'FINAL_COMPOSITION_GENERATING':
      return '成片编排中'
    case 'EXPORT_PACKAGE_GENERATING':
      return '导出打包中'
    case 'VIDEO_READY':
      return '视频已就绪'
    case 'DUBBING_READY':
      return '配音已就绪'
    case 'LIP_SYNC_READY':
      return '口型同步已就绪'
    case 'FINAL_COMPOSITION_READY':
      return '成片已就绪'
    case 'EXPORT_PACKAGE_READY':
      return '导出包已就绪'
    case 'COMPLETED':
      return '已完成'
    case 'PARTIAL_FAILED':
      return '部分失败'
    case 'FAILED':
      return '失败'
    default:
      return status
  }
}

function getProjectTone(status: ProjectStatus) {
  if (status === 'COMPLETED' || status === 'EXPORT_PACKAGE_READY' || status === 'FINAL_COMPOSITION_READY') {
    return 'success'
  }
  if (status === 'FAILED' || status === 'PARTIAL_FAILED') {
    return 'danger'
  }
  if (
    status === 'VIDEO_GENERATING' ||
    status === 'DUBBING_GENERATING' ||
    status === 'LIP_SYNC_GENERATING' ||
    status === 'FINAL_COMPOSITION_GENERATING' ||
    status === 'EXPORT_PACKAGE_GENERATING'
  ) {
    return 'info'
  }
  return 'warning'
}

function getReviewTone(status?: ContentReviewStatus | null) {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'PENDING':
      return 'info'
    case 'NOT_SUBMITTED':
    default:
      return 'warning'
  }
}

function getDeliveryTone(status: string) {
  if (status === '可交付') return 'success'
  if (status === '交付处理中') return 'info'
  return 'warning'
}

function getStatusBadgeClass(tone: 'neutral' | 'success' | 'warning' | 'danger' | 'info') {
  return `delivery-status-badge delivery-status-badge--${tone}`
}
