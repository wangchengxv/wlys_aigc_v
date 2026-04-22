import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PageBackLink } from '@/components/common/PageBackLink'
import { useToast } from '@/context/ToastContext'
import { presetDescriptor } from '@/data/videoStylePresets'
import {
  batchReviewSubmissions,
  createAssignmentSubmission,
  exportAssignmentGrades,
  getAdminUsers,
  getAssignmentStats,
  getAssignmentSubmissions,
  getCourseAssignments,
  getCourses,
  getOrgUnits,
  getSubmissionReviews,
  listScriptProjects,
  resolveScriptFileUrl,
  reviewAssignmentSubmission,
  updateCourseAssignmentStatus,
} from '@/api'
import type { AssignmentStats } from '@/api'
import { useAuthStore } from '@/stores/authStore'
import { useStyleTemplateStore } from '@/stores/styleTemplateStore'
import type {
  AssignmentSubmission,
  AdminUser,
  OrgUnit,
  ReviewRecord,
  ScriptProjectSummary,
  StyleTemplate,
  SubmissionStatus,
  TeachingAssignment,
  TeachingCourse,
  UserRole,
} from '@/types'

type ReviewDraft = {
  status: SubmissionStatus
  score: number
  comment: string
}

function fmt(value?: string | null) {
  if (!value) return '未记录'
  const time = new Date(value)
  return Number.isNaN(time.getTime()) ? value : time.toLocaleString('zh-CN')
}

function roleScopeLabel(role?: UserRole | null) {
  if (role === 'ADMIN') return '管理员视角'
  if (role === 'TEACHER') return '教师视角'
  if (role === 'STUDENT') return '学生视角'
  return '访客视角'
}

function assignmentStatusLabel(status: TeachingAssignment['status']) {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'PUBLISHED':
      return '进行中'
    case 'CLOSED':
      return '已关闭'
    default:
      return status
  }
}

function submissionStatusLabel(status: SubmissionStatus) {
  switch (status) {
    case 'SUBMITTED':
      return '待评分'
    case 'RETURNED':
      return '退回修改'
    case 'REVIEWED':
      return '已评分'
    default:
      return status
  }
}

function submissionStatusTone(status: SubmissionStatus) {
  switch (status) {
    case 'REVIEWED':
      return 'success'
    case 'RETURNED':
      return 'danger'
    default:
      return 'warning'
  }
}

function assignmentDueStatus(assignment: TeachingAssignment): 'ONGOING' | 'OVERDUE' | 'UNLIMITED' {
  if (!assignment.dueAt) return 'UNLIMITED'
  const dueTime = new Date(assignment.dueAt).getTime()
  if (Number.isNaN(dueTime)) return 'UNLIMITED'
  return Date.now() < dueTime ? 'ONGOING' : 'OVERDUE'
}

function dueStatusLabel(status: 'ONGOING' | 'OVERDUE' | 'UNLIMITED') {
  if (status === 'OVERDUE') return '已截止'
  if (status === 'ONGOING') return '进行中'
  return '不限时'
}

function classroomNameOf(userId: string, userMap: Record<string, AdminUser>, unitMap: Record<string, OrgUnit>) {
  const classroomId = userMap[userId]?.classroomId
  if (!classroomId) return '未绑定班级'
  return unitMap[classroomId]?.name || classroomId
}

function averageScore(list: AssignmentSubmission[]) {
  const scored = list.filter((item) => typeof item.score === 'number')
  if (!scored.length) return '--'
  const total = scored.reduce((sum, item) => sum + (item.score ?? 0), 0)
  return (total / scored.length).toFixed(1)
}

export function TeachingAssignmentDetailPage() {
  const { courseId = '', assignmentId = '' } = useParams()
  const { showToast } = useToast()
  const user = useAuthStore((s) => s.user)
  const [course, setCourse] = useState<TeachingCourse | null>(null)
  const [assignment, setAssignment] = useState<TeachingAssignment | null>(null)
  const [assignmentStats, setAssignmentStats] = useState<AssignmentStats | null>(null)
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([])
  const [projects, setProjects] = useState<ScriptProjectSummary[]>([])
  const [directoryUsers, setDirectoryUsers] = useState<AdminUser[]>([])
  const [orgUnits, setOrgUnits] = useState<OrgUnit[]>([])
  const [reviewMap, setReviewMap] = useState<Record<string, ReviewRecord[]>>({})
  const [reviewDrafts, setReviewDrafts] = useState<Record<string, ReviewDraft>>({})
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [reviewingId, setReviewingId] = useState('')
  const [closing, setClosing] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [selectedProjectId, setSelectedProjectId] = useState('')
  const [submissionNote, setSubmissionNote] = useState('')
  const [submissionKeyword, setSubmissionKeyword] = useState('')
  const [submissionClassroomFilter, setSubmissionClassroomFilter] = useState('ALL')
  const [submissionOwnerFilter, setSubmissionOwnerFilter] = useState('ALL')
  const [submissionStatusFilter, setSubmissionStatusFilter] = useState<'ALL' | SubmissionStatus>('ALL')
  const [showMinorSections, setShowMinorSections] = useState(false)
  const [selectedSubmissionIds, setSelectedSubmissionIds] = useState<Set<string>>(new Set())
  const [batchScore, setBatchScore] = useState(90)
  const [batchComment, setBatchComment] = useState('')
  const templates = useStyleTemplateStore((s) => s.templates)
  const loadTemplates = useStyleTemplateStore((s) => s.loadTemplates)
  const canReviewSubmissions = user?.role === 'ADMIN' || user?.role === 'TEACHER'
  const canSubmitProject = !canReviewSubmissions

  const availableProjects = useMemo(
    () =>
      projects.filter((item) => {
        const courseMatched = !item.courseId || item.courseId === courseId
        if (!courseMatched) return false
        if (!assignment?.styleTemplateId) return true
        const templateMatched = item.styleTemplateId === assignment.styleTemplateId
        if (!templateMatched) return false
        if (!canSubmitProject) return true
        if (!user?.userId) return false
        return item.ownerId === user.userId
      }),
    [assignment?.styleTemplateId, canSubmitProject, courseId, projects, user?.userId],
  )
  const templateMap = useMemo(
    () => Object.fromEntries(templates.map((item) => [item.templateId, item])),
    [templates],
  )
  const templateScopeLabel: Record<StyleTemplate['scope'], string> = {
    SYSTEM: '系统',
    COURSE: '课程',
    PERSONAL: '个人',
  }
  const requiredTemplate = assignment?.styleTemplateId ? templateMap[assignment.styleTemplateId] ?? null : null
  const projectMap = useMemo(() => Object.fromEntries(projects.map((item) => [item.projectId, item])), [projects])
  const directoryUserMap = useMemo(() => Object.fromEntries(directoryUsers.map((item) => [item.userId, item])), [directoryUsers])
  const orgUnitMap = useMemo(() => Object.fromEntries(orgUnits.map((item) => [item.unitId, item])), [orgUnits])
  const baseSubmissions = useMemo(() => {
    if (canReviewSubmissions) return submissions
    if (!user?.userId) return []
    return submissions.filter((item) => item.studentUserId === user.userId)
  }, [canReviewSubmissions, submissions, user?.userId])
  const classroomOptions = useMemo(
    () =>
      orgUnits
        .filter((item) => item.type === 'CLASSROOM')
        .sort((left, right) => left.name.localeCompare(right.name, 'zh-CN')),
    [orgUnits],
  )
  const submitterOptions = useMemo(
    () =>
      Array.from(
        new Map(
          baseSubmissions.map((item) => [
            item.studentUserId,
            {
              value: item.studentUserId,
              label: item.studentUserName || item.studentUserId,
            },
          ]),
        ).values(),
      ),
    [baseSubmissions],
  )
  const filteredSubmissions = useMemo(
    () =>
      baseSubmissions
        .filter((submission) => {
          const project = projectMap[submission.projectId]
          const classroomLabel = classroomNameOf(submission.studentUserId, directoryUserMap, orgUnitMap)
          const source = [submission.studentUserName, submission.studentUserId, submission.note, project?.name, project?.ownerName, classroomLabel]
            .filter(Boolean)
            .join(' ')
            .toLowerCase()
          if (submissionKeyword.trim() && !source.includes(submissionKeyword.trim().toLowerCase())) return false
          if (canReviewSubmissions && submissionClassroomFilter !== 'ALL') {
            const classroomId = directoryUserMap[submission.studentUserId]?.classroomId || ''
            if (classroomId !== submissionClassroomFilter) return false
          }
          if (canReviewSubmissions && submissionOwnerFilter !== 'ALL' && submission.studentUserId !== submissionOwnerFilter) return false
          if (submissionStatusFilter !== 'ALL' && submission.status !== submissionStatusFilter) return false
          return true
        })
        .sort((left, right) => {
          const leftTime = new Date(left.updatedAt || left.createdAt).getTime()
          const rightTime = new Date(right.updatedAt || right.createdAt).getTime()
          return rightTime - leftTime
        }),
    [
      baseSubmissions,
      canReviewSubmissions,
      directoryUserMap,
      orgUnitMap,
      projectMap,
      submissionClassroomFilter,
      submissionKeyword,
      submissionOwnerFilter,
      submissionStatusFilter,
    ],
  )
  const latestScoredSubmission = useMemo(
    () =>
      [...baseSubmissions]
        .filter((item) => typeof item.score === 'number')
        .sort((left, right) => {
          const leftTime = new Date(left.reviewedAt || left.updatedAt || left.createdAt).getTime()
          const rightTime = new Date(right.reviewedAt || right.updatedAt || right.createdAt).getTime()
          return rightTime - leftTime
        })[0] ?? null,
    [baseSubmissions],
  )

  const loadAssignmentDetail = useCallback(async () => {
    if (!courseId || !assignmentId) return
    setLoading(true)
    try {
      const [courses, assignments, nextSubmissions, nextProjects] = await Promise.all([
        getCourses(),
        getCourseAssignments(courseId),
        getAssignmentSubmissions(assignmentId),
        listScriptProjects(),
      ])
      if (canReviewSubmissions) {
        const [usersResult, orgUnitsResult, statsResult] = await Promise.allSettled([getAdminUsers(), getOrgUnits(), getAssignmentStats(assignmentId)])
        if (usersResult.status === 'fulfilled') {
          setDirectoryUsers(usersResult.value)
        }
        if (orgUnitsResult.status === 'fulfilled') {
          setOrgUnits(orgUnitsResult.value)
        }
        if (statsResult.status === 'fulfilled') {
          setAssignmentStats(statsResult.value)
        }
      } else {
        setDirectoryUsers([])
        setOrgUnits([])
        setAssignmentStats(null)
      }
      const currentCourse = courses.find((item) => item.courseId === courseId) ?? null
      const currentAssignment = assignments.find((item) => item.assignmentId === assignmentId) ?? null
      setCourse(currentCourse)
      setAssignment(currentAssignment)
      setSubmissions(nextSubmissions)
      setProjects(nextProjects)
      if (!selectedProjectId && nextProjects.length) {
        setSelectedProjectId(nextProjects[0].projectId)
      }

      const reviewPairs = await Promise.all(
        nextSubmissions.map(async (item) => [item.submissionId, await getSubmissionReviews(item.submissionId)] as const),
      )
      setReviewMap(Object.fromEntries(reviewPairs))
      setReviewDrafts(
        Object.fromEntries(
          nextSubmissions.map((item) => [
            item.submissionId,
            {
              status: item.status === 'SUBMITTED' ? 'REVIEWED' : item.status,
              score: item.score ?? 90,
              comment: item.reviewComment ?? '',
            },
          ]),
        ),
      )
    } catch (error) {
      showToast(error instanceof Error ? error.message : '加载作业详情失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [assignmentId, canReviewSubmissions, courseId, selectedProjectId, showToast])

  useEffect(() => {
    void loadAssignmentDetail()
  }, [loadAssignmentDetail])

  useEffect(() => {
    if (!courseId) return
    void loadTemplates(courseId)
  }, [courseId, loadTemplates])

  useEffect(() => {
    if (availableProjects.some((item) => item.projectId === selectedProjectId)) {
      return
    }
    setSelectedProjectId(availableProjects[0]?.projectId ?? '')
  }, [availableProjects, selectedProjectId])

  async function handleSubmitProject() {
    if (!assignmentId) return
    if (!selectedProjectId) {
      showToast('请先选择一个剧本项目', 'error')
      return
    }
    setSubmitting(true)
    try {
      await createAssignmentSubmission(assignmentId, {
        projectId: selectedProjectId,
        note: submissionNote.trim() || undefined,
      })
      setSubmissionNote('')
      showToast('作业已提交', 'success')
      await loadAssignmentDetail()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '提交作业失败', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleReview(submissionId: string) {
    const draft = reviewDrafts[submissionId]
    if (!draft) return
    setReviewingId(submissionId)
    try {
      await reviewAssignmentSubmission(submissionId, {
        status: draft.status,
        score: draft.score,
        comment: draft.comment.trim() || undefined,
      })
      showToast('评分已保存', 'success')
      await loadAssignmentDetail()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '评分失败', 'error')
    } finally {
      setReviewingId('')
    }
  }

  async function handleAssignmentStatus(nextStatus: TeachingAssignment['status']) {
    if (!courseId || !assignmentId) return
    setClosing(true)
    try {
      await updateCourseAssignmentStatus(courseId, assignmentId, { status: nextStatus })
      showToast(nextStatus === 'CLOSED' ? '作业已关闭' : '作业已重新开放', 'success')
      await loadAssignmentDetail()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '更新作业状态失败', 'error')
    } finally {
      setClosing(false)
    }
  }

  async function handleExportGrades() {
    try {
      showToast('正在导出成绩...', 'info')
      setExporting(true)
      await exportAssignmentGrades(assignmentId)
      showToast('成绩已导出', 'success')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '导出失败', 'error')
    } finally {
      setExporting(false)
    }
  }

  async function handleBatchReview() {
    if (selectedSubmissionIds.size === 0) {
      showToast('请先选择要评分的提交', 'error')
      return
    }
    setSubmitting(true)
    try {
      await batchReviewSubmissions(assignmentId, {
        submissionIds: Array.from(selectedSubmissionIds),
        status: 'REVIEWED',
        score: batchScore,
        comment: batchComment.trim() || undefined,
      })
      setSelectedSubmissionIds(new Set())
      showToast(`成功评分 ${selectedSubmissionIds.size} 份提交`, 'success')
      await loadAssignmentDetail()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '批量评分失败', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  function toggleSubmissionSelection(submissionId: string) {
    setSelectedSubmissionIds((current) => {
      const next = new Set(current)
      if (next.has(submissionId)) {
        next.delete(submissionId)
      } else {
        next.add(submissionId)
      }
      return next
    })
  }

  function toggleSelectAll() {
    const selectableIds = filteredSubmissions
      .filter((s) => s.status === 'SUBMITTED' || s.status === 'RETURNED')
      .map((s) => s.submissionId)
    if (selectedSubmissionIds.size === selectableIds.length) {
      setSelectedSubmissionIds(new Set())
    } else {
      setSelectedSubmissionIds(new Set(selectableIds))
    }
  }

  function patchReviewDraft(submissionId: string, next: Partial<ReviewDraft>) {
    setReviewDrafts((current) => ({
      ...current,
      [submissionId]: {
        status: current[submissionId]?.status ?? 'REVIEWED',
        score: current[submissionId]?.score ?? 90,
        comment: current[submissionId]?.comment ?? '',
        ...next,
      },
    }))
  }

  if (loading) {
    return <LoadingSpinner />
  }

  if (!course || !assignment) {
    return <EmptyState title="作业不存在" description="请返回课程页重新选择。" />
  }

  const dueStatus = assignmentDueStatus(assignment)
  const submitDisabled = assignment.status === 'CLOSED' || course.archived || dueStatus === 'OVERDUE'

  return (
    <section className="teaching-page teaching-assignment-detail-page">
      <div className="page-back-row">
        <PageBackLink to={`/courses/${courseId}`}>返回课程</PageBackLink>
      </div>
      <div className="minor-entry-bar">
        <button type="button" className="nav-btn" onClick={() => setShowMinorSections((current) => !current)}>
          {showMinorSections ? '收起次级信息' : '展开次级信息'}
        </button>
        {!showMinorSections ? <span className="muted">已隐藏说明文本与扩展元信息，保留评分与提交主入口。</span> : null}
      </div>

      <div className="teaching-page__hero panel glass">
        <div>
          <h2>{assignment.title}</h2>
          {showMinorSections ? <p className="muted">{assignment.brief || '暂无作业说明。'}</p> : null}
          {showMinorSections && assignment.styleTemplateId ? (
            <p className="muted">
              指定模板：
              {requiredTemplate
                ? `[${templateScopeLabel[requiredTemplate.scope]}] ${presetDescriptor(requiredTemplate)}`
                : assignment.styleTemplateId}
            </p>
          ) : null}
        </div>
        <div className="teaching-meta teaching-meta--stack">
          <span>当前角色：{roleScopeLabel(user?.role)}</span>
          <span>课程：{course.name}</span>
          <span>状态：{assignmentStatusLabel(assignment.status)}</span>
          <span>最晚提交：{assignment.dueAt ? fmt(assignment.dueAt) : '不限时'}</span>
          <span>截止状态：{dueStatusLabel(dueStatus)}</span>
          {showMinorSections ? <span>{canReviewSubmissions ? '全部提交数' : '我的提交数'}：{baseSubmissions.length}</span> : null}
          <div className="teaching-actions">
            <Link
              className="pill"
              to={`/script-projects/new?courseId=${encodeURIComponent(courseId)}${assignment.styleTemplateId ? `&styleTemplateId=${encodeURIComponent(assignment.styleTemplateId)}` : ''}&assignmentTitle=${encodeURIComponent(assignment.title)}`}
            >
              {canReviewSubmissions ? '新建课程项目' : '新建我的项目'}
            </Link>
            {canReviewSubmissions ? (
              <>
                <AppButton
                  size="sm"
                  variant={assignment.status === 'CLOSED' ? 'ghost' : 'danger'}
                  loading={closing}
                  onClick={() => void handleAssignmentStatus(assignment.status === 'CLOSED' ? 'PUBLISHED' : 'CLOSED')}
                >
                  {assignment.status === 'CLOSED' ? '重新开放' : '关闭作业'}
                </AppButton>
                <Link className="pill" to={`/audit-logs?entityType=ASSIGNMENT&entityId=${encodeURIComponent(assignment.assignmentId)}`}>
                  查看审计
                </Link>
                <Link className="pill" to={`/script-projects?courseId=${encodeURIComponent(courseId)}`}>
                  课程项目库
                </Link>
              </>
            ) : null}
          </div>
        </div>
      </div>

      <div className="operations-dashboard-summary">
        <article className="panel glass operations-dashboard-summary__item">
          <p className="operations-dashboard-summary__label">{canReviewSubmissions ? '提交总数' : '我的提交次数'}</p>
          <strong className="operations-dashboard-summary__value">{baseSubmissions.length}</strong>
        </article>
        <article className="panel glass operations-dashboard-summary__item">
          <p className="operations-dashboard-summary__label">{canReviewSubmissions ? '待评分' : '待教师反馈'}</p>
          <strong className="operations-dashboard-summary__value">{baseSubmissions.filter((item) => item.status === 'SUBMITTED').length}</strong>
        </article>
        <article className="panel glass operations-dashboard-summary__item">
          <p className="operations-dashboard-summary__label">退回修改</p>
          <strong className="operations-dashboard-summary__value">{baseSubmissions.filter((item) => item.status === 'RETURNED').length}</strong>
        </article>
        <article className="panel glass operations-dashboard-summary__item">
          <p className="operations-dashboard-summary__label">{canReviewSubmissions ? '平均分' : '最近得分'}</p>
          <strong className="operations-dashboard-summary__value">{canReviewSubmissions ? averageScore(baseSubmissions) : latestScoredSubmission?.score ?? '--'}</strong>
        </article>
      </div>

      {canReviewSubmissions && assignmentStats ? (
        <div className="assignment-stats-panel panel glass">
          <div className="assignment-stats-panel__header">
            <h3>作业统计</h3>
          </div>
          <div className="assignment-stats-panel__grid">
            <div className="assignment-stats-panel__metric">
              <span className="assignment-stats-panel__metric-label">提交率</span>
              <div className="assignment-stats-panel__progress-wrap">
                <div className="assignment-stats-panel__progress-bar">
                  <div
                    className="assignment-stats-panel__progress-fill"
                    style={{ width: `${assignmentStats.totalStudents > 0 ? (assignmentStats.submittedCount / assignmentStats.totalStudents) * 100 : 0}%` }}
                  />
                </div>
                <span className="assignment-stats-panel__progress-text">
                  {assignmentStats.submittedCount} / {assignmentStats.totalStudents}
                </span>
              </div>
            </div>
            <div className="assignment-stats-panel__metric">
              <span className="assignment-stats-panel__metric-label">平均分</span>
              <span className="assignment-stats-panel__metric-value">{assignmentStats.averageScore != null ? assignmentStats.averageScore.toFixed(1) : '--'}</span>
            </div>
            <div className="assignment-stats-panel__metric">
              <span className="assignment-stats-panel__metric-label">最高分</span>
              <span className="assignment-stats-panel__metric-value">{assignmentStats.maxScore > 0 ? assignmentStats.maxScore : '--'}</span>
            </div>
            <div className="assignment-stats-panel__metric">
              <span className="assignment-stats-panel__metric-label">最低分</span>
              <span className="assignment-stats-panel__metric-value">{assignmentStats.minScore > 0 ? assignmentStats.minScore : '--'}</span>
            </div>
            <div className="assignment-stats-panel__metric">
              <span className="assignment-stats-panel__metric-label">待评分</span>
              <span className="assignment-stats-panel__metric-value">{assignmentStats.pendingReviewCount}</span>
            </div>
            <div className="assignment-stats-panel__metric">
              <span className="assignment-stats-panel__metric-label">已评分</span>
              <span className="assignment-stats-panel__metric-value">{assignmentStats.reviewedCount}</span>
            </div>
          </div>
          {assignmentStats.scoreBuckets && assignmentStats.scoreBuckets.length > 0 ? (
            <div className="assignment-stats-panel__distribution">
              <span className="assignment-stats-panel__metric-label">分数分布</span>
              <div className="assignment-stats-panel__buckets">
                {assignmentStats.scoreBuckets.map((bucket) => {
                  const maxCount = Math.max(...assignmentStats.scoreBuckets.map((b) => b.count), 1)
                  return (
                    <div key={bucket.label} className="assignment-stats-panel__bucket">
                      <span className="assignment-stats-panel__bucket-label">{bucket.label}</span>
                      <div className="assignment-stats-panel__bucket-bar-wrap">
                        <div
                          className="assignment-stats-panel__bucket-bar"
                          style={{ width: `${(bucket.count / maxCount) * 100}%` }}
                        />
                      </div>
                      <span className="assignment-stats-panel__bucket-count">{bucket.count}人</span>
                    </div>
                  )
                })}
              </div>
            </div>
          ) : null}
        </div>
      ) : null}

      <div className="teaching-grid">
        <div className="teaching-stack">
          {canReviewSubmissions && filteredSubmissions.length > 0 ? (
            <div className="panel glass teaching-panel batch-operations-panel">
              <div className="batch-operations-toolbar">
                <label className="batch-select-all">
                  <input
                    type="checkbox"
                    checked={
                      selectedSubmissionIds.size > 0 &&
                      selectedSubmissionIds.size === filteredSubmissions.filter((s) => s.status === 'SUBMITTED' || s.status === 'RETURNED').length
                    }
                    onChange={() => void toggleSelectAll()}
                  />
                  <span>全选/取消全选</span>
                </label>
                <span className="batch-selected-count">已选中 {selectedSubmissionIds.size} 项</span>
                <div className="batch-quick-scores">
                  <span className="batch-quick-scores__label">快捷分数：</span>
                  {[60, 70, 80, 90, 100].map((score) => (
                    <button
                      key={score}
                      type="button"
                      className={`batch-quick-score batch-quick-score--${score}`}
                      onClick={() => setBatchScore(score)}
                      style={{ fontWeight: batchScore === score ? 'bold' : 'normal' }}
                    >
                      {score}
                    </button>
                  ))}
                </div>
                <AppInput
                  label="批量评语"
                  as="textarea"
                  rows={2}
                  value={batchComment}
                  onChange={(value) => setBatchComment(String(value))}
                  placeholder="可选填统一评语"
                />
                <AppButton
                  variant="primary"
                  size="sm"
                  loading={submitting}
                  disabled={selectedSubmissionIds.size === 0}
                  onClick={() => void handleBatchReview()}
                >
                  批量评分
                </AppButton>
              </div>
            </div>
          ) : null}
          <div className="panel glass teaching-panel">
            <div className="teaching-panel__head">
              <div>
                <h3>{canReviewSubmissions ? '提交筛选' : '我的提交筛选'}</h3>
              </div>
              <span className="pill small">{filteredSubmissions.length} 条结果</span>
            </div>
            <div className="teaching-form">
              <div className="teaching-form__row">
                <AppInput
                  label="关键词"
                  value={submissionKeyword}
                  onChange={(value) => setSubmissionKeyword(String(value))}
                  placeholder={canReviewSubmissions ? '搜索学生、班级、项目名或提交说明' : '搜索项目名或提交说明'}
                />
                <label className="input-wrap">
                  <span className="label">提交状态</span>
                  <select className="ctrl" value={submissionStatusFilter} onChange={(event) => setSubmissionStatusFilter(event.target.value as 'ALL' | SubmissionStatus)}>
                    <option value="ALL">全部状态</option>
                    <option value="SUBMITTED">待评分</option>
                    <option value="RETURNED">退回修改</option>
                    <option value="REVIEWED">已评分</option>
                  </select>
                </label>
                {canReviewSubmissions ? (
                  <label className="input-wrap">
                    <span className="label">班级</span>
                    <select className="ctrl" value={submissionClassroomFilter} onChange={(event) => setSubmissionClassroomFilter(event.target.value)}>
                      <option value="ALL">全部班级</option>
                      {classroomOptions.map((item) => (
                        <option key={item.unitId} value={item.unitId}>
                          {item.name}
                        </option>
                      ))}
                    </select>
                  </label>
                ) : null}
              </div>
              {canReviewSubmissions ? (
                <div className="teaching-form__row">
                  <label className="input-wrap">
                    <span className="label">提交人</span>
                    <select className="ctrl" value={submissionOwnerFilter} onChange={(event) => setSubmissionOwnerFilter(event.target.value)}>
                      <option value="ALL">全部提交人</option>
                      {submitterOptions.map((item) => (
                        <option key={item.value} value={item.value}>
                          {item.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <div className="teaching-actions">
                    <button
                      type="button"
                      className="nav-btn"
                      onClick={() => {
                        setSubmissionKeyword('')
                        setSubmissionClassroomFilter('ALL')
                        setSubmissionOwnerFilter('ALL')
                        setSubmissionStatusFilter('ALL')
                      }}
                    >
                      重置筛选
                    </button>
                  </div>
                </div>
              ) : null}
            </div>
          </div>

          <div className="panel glass teaching-panel">
            <div className="teaching-panel__head">
              <div>
                <h3>{canReviewSubmissions ? '教师操作入口' : '提交项目'}</h3>
              </div>
              {!canReviewSubmissions ? (
                <Link
                  className="pill"
                  to={`/script-projects/new?courseId=${encodeURIComponent(courseId)}${assignment.styleTemplateId ? `&styleTemplateId=${encodeURIComponent(assignment.styleTemplateId)}` : ''}&assignmentTitle=${encodeURIComponent(assignment.title)}`}
                >
                  新建项目
                </Link>
              ) : null}
            </div>

            {canReviewSubmissions ? (
              <div className="teaching-form">
                <div className="teaching-banner">
                  <strong>教师端可查看全部学生提交、保存评分，并根据结果退回修改或确认通过。</strong>
                  <p className="muted">学生端不显示全班提交记录和评分控件，只保留个人提交通道与反馈内容。</p>
                </div>
                <div className="teaching-actions">
                  <Link className="nav-btn primary" to={`/courses/${courseId}`}>
                    返回课程布置
                  </Link>
                  <Link className="pill" to={`/script-projects?courseId=${encodeURIComponent(courseId)}`}>
                    查看课程项目库
                  </Link>
                  <Link className="pill" to={`/script-projects?courseId=${encodeURIComponent(courseId)}&deliveryStatus=READY`}>
                    查看可交付作品
                  </Link>
                  <button
                    type="button"
                    className="nav-btn primary"
                    disabled={exporting}
                    onClick={() => void handleExportGrades()}
                  >
                    {exporting ? '导出中...' : '导出成绩'}
                  </button>
                </div>
                <div className="teaching-meta">
                  <span>作业状态：{assignmentStatusLabel(assignment.status)}</span>
                  <span>最晚提交：{assignment.dueAt ? fmt(assignment.dueAt) : '不限时'}</span>
                  <span>截止状态：{dueStatusLabel(dueStatus)}</span>
                  <span>指定模板：{requiredTemplate ? `[${templateScopeLabel[requiredTemplate.scope]}] ${presetDescriptor(requiredTemplate)}` : '未限制'}</span>
                  <span>归档状态：{course.archived ? '课程已归档' : '课程进行中'}</span>
                </div>
              </div>
            ) : availableProjects.length ? (
              <div className="teaching-form">
                <div className="teaching-banner">
                  <strong>仅展示当前学生本人且符合课程要求的项目。</strong>
                  <p className="muted">
                    {assignment.styleTemplateId
                      ? `当前作业要求使用指定模板：${
                          requiredTemplate ? `[${templateScopeLabel[requiredTemplate.scope]}] ${presetDescriptor(requiredTemplate)}` : assignment.styleTemplateId
                        }`
                      : '当前作业未强制指定模板，可提交本课程下的个人项目。'}
                  </p>
                </div>
                <label className="input-wrap">
                  <span className="label">选择剧本项目</span>
                  <select className="ctrl" value={selectedProjectId} onChange={(e) => setSelectedProjectId(e.target.value)}>
                    {availableProjects.map((item) => (
                      <option key={item.projectId} value={item.projectId}>
                        {item.name}｜{item.status}{item.styleTemplateId ? `｜模板 ${templateMap[item.styleTemplateId]?.name || item.styleTemplateId}` : ''}
                      </option>
                    ))}
                  </select>
                </label>
                <AppInput
                  label="提交说明"
                  as="textarea"
                  rows={4}
                  value={submissionNote}
                  onChange={(value) => setSubmissionNote(String(value))}
                  placeholder="例如：这一版主要完成了节奏和镜头组织"
                />
                <AppButton variant="primary" loading={submitting} disabled={submitDisabled} onClick={() => void handleSubmitProject()}>
                  提交到作业
                </AppButton>
                {submitDisabled ? (
                  <p className="muted">
                    {course.archived
                      ? '课程已归档，当前不再接受新提交。'
                      : assignment.status === 'CLOSED'
                        ? '作业已关闭，当前不再接受新提交。'
                        : '作业已截止，当前不再接受新提交。'}
                  </p>
                ) : null}
              </div>
            ) : (
              <EmptyState
                title="暂无可提交项目"
                description={
                  assignment.styleTemplateId
                    ? '这条作业要求项目绑定指定模板。先按作业模板创建个人项目，再回到这里提交。'
                    : '先创建一个属于你的剧本项目，再回到这里完成作业提交。'
                }
              >
                <Link
                  className="create-link"
                  to={`/script-projects/new?courseId=${encodeURIComponent(courseId)}${assignment.styleTemplateId ? `&styleTemplateId=${encodeURIComponent(assignment.styleTemplateId)}` : ''}&assignmentTitle=${encodeURIComponent(assignment.title)}`}
                >
                  去创建项目
                </Link>
              </EmptyState>
            )}
          </div>
        </div>

        <div className="teaching-stack">
          <div className="panel glass teaching-panel">
            <div className="teaching-panel__head">
              <div>
                <p className="eyebrow">Submission List</p>
                <h3>{canReviewSubmissions ? '学生提交与评分' : '我的提交记录与教师反馈'}</h3>
              </div>
              <div className="teaching-meta">
                {showMinorSections ? <span>{canReviewSubmissions ? '支持评分、退回修改与查看历史记录' : '仅显示本人提交与教师反馈历史'}</span> : null}
              </div>
            </div>
          </div>
          {filteredSubmissions.length ? (
            filteredSubmissions.map((submission, index) => {
              const draft = reviewDrafts[submission.submissionId] ?? {
                status: 'REVIEWED' as SubmissionStatus,
                score: 90,
                comment: '',
              }
              const reviews = reviewMap[submission.submissionId] ?? []
              const project = projectMap[submission.projectId]
              const classroomName = classroomNameOf(submission.studentUserId, directoryUserMap, orgUnitMap)
              const projectCoverUrl = project?.coverFileId ? resolveScriptFileUrl(project.coverFileId) : ''
              const projectInitial = project?.name?.slice(0, 1) ?? '?'
              return (
                <article key={submission.submissionId} className="panel glass teaching-card teaching-card--submission">
                  <div className="teaching-card__head">
                    {canReviewSubmissions ? (
                      <label className="batch-submission-checkbox">
                        <input
                          type="checkbox"
                          checked={selectedSubmissionIds.has(submission.submissionId)}
                          disabled={submission.status === 'REVIEWED'}
                          onChange={() => void toggleSubmissionSelection(submission.submissionId)}
                        />
                      </label>
                    ) : null}
                    {canReviewSubmissions ? (
                      <div className="teaching-card__head-left">
                        {projectCoverUrl ? (
                          <Link to={`/script-projects/${encodeURIComponent(submission.projectId)}`} className="project-preview">
                            <img src={projectCoverUrl} alt={project?.name || '项目封面'} />
                          </Link>
                        ) : (
                          <Link to={`/script-projects/${encodeURIComponent(submission.projectId)}`} className="project-preview project-preview--placeholder">
                            <span>{projectInitial}</span>
                          </Link>
                        )}
                      </div>
                    ) : null}
                    <div className="teaching-card__head-right">
                      <h3>{canReviewSubmissions ? submission.studentUserName || submission.studentUserId : project?.name || `我的提交 ${index + 1}`}</h3>
                    </div>
                    <span className={`pill small operations-dashboard-pill is-${submissionStatusTone(submission.status)}`}>
                      {submissionStatusLabel(submission.status)}
                    </span>
                  </div>
                  <div className="teaching-meta">
                    {canReviewSubmissions ? <span>学生：{submission.studentUserName || submission.studentUserId}</span> : <span>所属课程：{course.name}</span>}
                    {canReviewSubmissions ? <span>班级：{classroomName}</span> : null}
                    <span>项目：{project?.name || submission.projectId}</span>
                    <span>提交时间：{fmt(submission.submittedAt)}</span>
                    <span>当前分数：{submission.score ?? '未评分'}</span>
                    {project ? <span>项目状态：{project.status}</span> : null}
                  </div>
                  {showMinorSections ? <p className="muted">{submission.note || '提交者未填写说明。'}</p> : null}

                  {canReviewSubmissions ? (
                    <>
                      <div className="teaching-review-grid">
                        <label className="input-wrap">
                          <span className="label">评审状态</span>
                          <select
                            className="ctrl"
                            value={draft.status}
                            onChange={(e) => patchReviewDraft(submission.submissionId, { status: e.target.value as SubmissionStatus })}
                          >
                            <option value="REVIEWED">通过</option>
                            <option value="RETURNED">退回修改</option>
                          </select>
                        </label>
                        <AppInput
                          label="评分"
                          type="number"
                          min={0}
                          max={100}
                          value={draft.score}
                          onChange={(value) => patchReviewDraft(submission.submissionId, { score: Number(value) || 0 })}
                        />
                      </div>
                      <AppInput
                        label="点评"
                        as="textarea"
                        rows={4}
                        value={draft.comment}
                        onChange={(value) => patchReviewDraft(submission.submissionId, { comment: String(value) })}
                        placeholder="例如：节奏已经稳定，可以继续强化开场镜头张力"
                      />
                      <div className="teaching-actions">
                        <AppButton
                          size="sm"
                          variant="primary"
                          loading={reviewingId === submission.submissionId}
                          onClick={() => void handleReview(submission.submissionId)}
                        >
                          保存评分
                        </AppButton>
                        <Link className="pill" to={`/script-projects/${encodeURIComponent(submission.projectId)}`}>
                          查看项目详情
                        </Link>
                        <Link className="pill" to={`/script-projects/${encodeURIComponent(submission.projectId)}/export`}>
                          查看导出与审核
                        </Link>
                        <Link
                          className="pill"
                          to={`/script-projects?courseId=${encodeURIComponent(courseId)}&ownerId=${encodeURIComponent(submission.studentUserId)}`}
                        >
                          按提交人查看项目
                        </Link>
                      </div>
                    </>
                  ) : (
                    <div className="teaching-banner">
                      <strong>当前反馈状态：{submissionStatusLabel(submission.status)}</strong>
                      <p className="muted">{submission.reviewComment || reviews[0]?.comment || '教师暂未填写详细反馈。'}</p>
                    </div>
                  )}

                  <div className="teaching-review-log">
                    {showMinorSections ? <p className="eyebrow">评分记录</p> : null}
                    {reviews.length ? (
                      reviews.map((record) => (
                        <div key={record.reviewId} className="teaching-review-log__item">
                          <strong>{record.reviewerUserName || record.reviewerUserId}</strong>
                          <span>
                            {submissionStatusLabel(record.status)} · {record.score ?? '未评分'}
                          </span>
                          <span className="muted">{fmt(record.createdAt)}</span>
                          <p className="muted">{record.comment || '未填写点评。'}</p>
                        </div>
                      ))
                    ) : (
                      <p className="muted">暂无评分记录。</p>
                    )}
                  </div>
                </article>
              )
            })
          ) : (
            <EmptyState
              title={baseSubmissions.length ? '没有符合条件的提交记录' : '还没有提交记录'}
              description={
                baseSubmissions.length
                  ? '请调整筛选条件后重新查看。'
                  : canReviewSubmissions
                    ? '学生完成第一次提交后，这里会出现评分区、历史记录和退回意见。'
                    : '先把一个项目交上来，这里就会出现教师评分与反馈日志。'
              }
            />
          )}
        </div>
      </div>
    </section>
  )
}
