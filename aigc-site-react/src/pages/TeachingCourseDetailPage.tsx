import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ActionDrawer } from '@/components/common/ActionDrawer'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { CompactFilterBar } from '@/components/common/CompactFilterBar'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PageBackLink } from '@/components/common/PageBackLink'
import { SectionTabs } from '@/components/common/SectionTabs'
import { StatStrip } from '@/components/common/StatStrip'
import { useToast } from '@/context/ToastContext'
import { presetDescriptor } from '@/data/videoStylePresets'
import { archiveCourse, createCourseAssignment, getCourseAssignments, getCourses, updateCourseAssignmentStatus } from '@/api'
import { useAuthStore } from '@/stores/authStore'
import { useStyleTemplateStore } from '@/stores/styleTemplateStore'
import type { StyleTemplate, TeachingAssignment, TeachingCourse, UserRole } from '@/types'

type CourseTab = 'overview' | 'assignments' | 'reviews' | 'audit'

function fmt(value?: string | null) {
  if (!value) return '未设置'
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

function formatOffsetDateTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  const offsetMinutes = -date.getTimezoneOffset()
  const sign = offsetMinutes >= 0 ? '+' : '-'
  const absMinutes = Math.abs(offsetMinutes)
  const offsetHours = String(Math.floor(absMinutes / 60)).padStart(2, '0')
  const offsetRemainMinutes = String(absMinutes % 60).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}${sign}${offsetHours}:${offsetRemainMinutes}`
}

export function TeachingCourseDetailPage() {
  const { courseId = '' } = useParams()
  const { showToast } = useToast()
  const user = useAuthStore((s) => s.user)
  const [course, setCourse] = useState<TeachingCourse | null>(null)
  const [assignments, setAssignments] = useState<TeachingAssignment[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [archiving, setArchiving] = useState(false)
  const [updatingAssignmentId, setUpdatingAssignmentId] = useState('')
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [activeTab, setActiveTab] = useState<CourseTab>('overview')
  const [title, setTitle] = useState('')
  const [brief, setBrief] = useState('')
  const [aspectRatio, setAspectRatio] = useState('16:9')
  const [targetDuration, setTargetDuration] = useState<number>(15)
  const [language, setLanguage] = useState('zh-CN')
  const [styleTemplateId, setStyleTemplateId] = useState('')
  const [dueAt, setDueAt] = useState('')
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | TeachingAssignment['status']>('ALL')
  const [templateFilter, setTemplateFilter] = useState<'ALL' | 'BOUND' | 'FREE'>('ALL')
  const [showMinorSections, setShowMinorSections] = useState(false)
  const templates = useStyleTemplateStore((s) => s.templates)
  const loadTemplates = useStyleTemplateStore((s) => s.loadTemplates)
  const canManageAssignments = user?.role === 'ADMIN' || user?.role === 'TEACHER'

  const loadCourseDetail = useCallback(async () => {
    if (!courseId) return
    setLoading(true)
    try {
      const [courses, nextAssignments] = await Promise.all([getCourses(), getCourseAssignments(courseId)])
      setCourse(courses.find((item) => item.courseId === courseId) ?? null)
      setAssignments(nextAssignments)
    } catch (error) {
      showToast(error instanceof Error ? error.message : '加载课程详情失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [courseId, showToast])

  useEffect(() => {
    void loadCourseDetail()
  }, [loadCourseDetail])

  useEffect(() => {
    if (!courseId) return
    void loadTemplates(courseId)
  }, [courseId, loadTemplates])

  const filteredAssignments = useMemo(
    () =>
      assignments
        .filter((assignment) => {
          const source = [
            assignment.title,
            assignment.brief,
            assignment.language,
            assignment.ownerName,
            assignment.ownerId,
            assignment.styleTemplateId,
          ]
            .filter(Boolean)
            .join(' ')
            .toLowerCase()
          if (keyword.trim() && !source.includes(keyword.trim().toLowerCase())) return false
          if (statusFilter !== 'ALL' && assignment.status !== statusFilter) return false
          if (templateFilter === 'BOUND' && !assignment.styleTemplateId) return false
          if (templateFilter === 'FREE' && assignment.styleTemplateId) return false
          return true
        })
        .sort((left, right) => new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime()),
    [assignments, keyword, statusFilter, templateFilter],
  )

  const summary = useMemo(() => {
    const publishedCount = assignments.filter((item) => item.status === 'PUBLISHED').length
    const closedCount = assignments.filter((item) => item.status === 'CLOSED').length
    const templateBoundCount = assignments.filter((item) => Boolean(item.styleTemplateId)).length
    return {
      total: assignments.length,
      published: publishedCount,
      closed: closedCount,
      templateBound: templateBoundCount,
    }
  }, [assignments])

  const templateMap = useMemo(() => Object.fromEntries(templates.map((item) => [item.templateId, item])), [templates])
  const templateScopeLabel: Record<StyleTemplate['scope'], string> = {
    SYSTEM: '系统',
    COURSE: '课程',
    PERSONAL: '个人',
  }

  async function handleCreateAssignment() {
    if (!courseId) return
    if (!title.trim()) {
      showToast('请先填写作业标题', 'error')
      return
    }
    if (!dueAt.trim()) {
      showToast('请先设置最晚提交时间', 'error')
      return
    }
    const dueAtDate = new Date(dueAt)
    if (Number.isNaN(dueAtDate.getTime())) {
      showToast('最晚提交时间格式不正确', 'error')
      return
    }
    if (dueAtDate.getTime() <= Date.now()) {
      showToast('最晚提交时间必须晚于当前时间', 'error')
      return
    }
    const dueAtPayload = formatOffsetDateTime(dueAt)
    if (!dueAtPayload) {
      showToast('最晚提交时间格式不正确', 'error')
      return
    }
    setSaving(true)
    try {
      await createCourseAssignment(courseId, {
        title: title.trim(),
        brief: brief.trim() || undefined,
        styleTemplateId: styleTemplateId || undefined,
        aspectRatio: aspectRatio.trim() || undefined,
        targetDuration: targetDuration || undefined,
        language: language.trim() || undefined,
        dueAt: dueAtPayload,
      })
      setTitle('')
      setBrief('')
      setStyleTemplateId('')
      setAspectRatio('16:9')
      setTargetDuration(15)
      setLanguage('zh-CN')
      setDueAt('')
      setDrawerOpen(false)
      showToast('作业已发布', 'success')
      await loadCourseDetail()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '创建作业失败', 'error')
    } finally {
      setSaving(false)
    }
  }

  async function handleArchiveCourse(nextArchived: boolean) {
    if (!courseId) return
    setArchiving(true)
    try {
      await archiveCourse(courseId, { archived: nextArchived })
      showToast(nextArchived ? '课程已归档' : '课程已恢复为进行中', 'success')
      await loadCourseDetail()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '更新课程状态失败', 'error')
    } finally {
      setArchiving(false)
    }
  }

  async function handleAssignmentStatus(assignmentId: string, nextStatus: TeachingAssignment['status']) {
    if (!courseId) return
    setUpdatingAssignmentId(assignmentId)
    try {
      await updateCourseAssignmentStatus(courseId, assignmentId, { status: nextStatus })
      showToast(nextStatus === 'CLOSED' ? '作业已关闭' : '作业已重新开放', 'success')
      await loadCourseDetail()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '更新作业状态失败', 'error')
    } finally {
      setUpdatingAssignmentId('')
    }
  }

  if (loading) return <LoadingSpinner />
  if (!course) return <EmptyState title="课程不存在" description="请返回课程列表重新选择。" />

  const tabs = [
    { id: 'overview', label: '总览' },
    { id: 'assignments', label: '作业', badge: String(filteredAssignments.length) },
    { id: 'reviews', label: '提交与评分' },
    { id: 'audit', label: '审计' },
  ]

  return (
    <section className="teaching-course-detail teaching-course-detail--revamp">
      <div className="page-back-row">
        <PageBackLink to="/courses">返回课程列表</PageBackLink>
      </div>
      <div className="minor-entry-bar">
        <button type="button" className="nav-btn" onClick={() => setShowMinorSections((current) => !current)}>
          {showMinorSections ? '收起次级信息' : '展开次级信息'}
        </button>
        {!showMinorSections ? <span className="muted">已隐藏课程备注与治理入口，主流程操作保持可用。</span> : null}
      </div>

      <section className="course-header-card">
        <div>
          <div className="inline-badges">
            <span className="soft-badge">{roleScopeLabel(user?.role)}</span>
            {course.code ? <span className="soft-badge">{course.code}</span> : null}
            <span className={`soft-badge ${course.archived ? 'is-muted' : 'is-success'}`}>{course.archived ? '已归档' : '进行中'}</span>
          </div>
          <h2>{course.name}</h2>
          {showMinorSections ? <p>{course.description || '这门课还没有补充详细说明。'}</p> : null}
        </div>
        <div className="course-header-card__actions">
          <Link className="app-btn v-primary s-md" to={`/script-projects/new?courseId=${encodeURIComponent(course.courseId)}`}>
            {canManageAssignments ? '新建课程项目' : '创建我的项目'}
          </Link>
          {canManageAssignments ? (
            <>
              <AppButton onClick={() => setDrawerOpen(true)}>发布作业</AppButton>
              <AppButton variant={course.archived ? 'ghost' : 'danger'} loading={archiving} onClick={() => void handleArchiveCourse(!course.archived)}>
                {course.archived ? '取消归档' : '归档课程'}
              </AppButton>
            </>
          ) : null}
        </div>
      </section>

      <StatStrip
        items={[
          { key: 'total', label: '作业总数', value: summary.total },
          { key: 'published', label: '进行中作业', value: summary.published },
          { key: 'closed', label: '已关闭作业', value: summary.closed },
          { key: 'template', label: canManageAssignments ? '模板约束作业' : '需按模板完成', value: summary.templateBound },
        ]}
      />

      <SectionTabs items={tabs} activeId={activeTab} onChange={(id) => setActiveTab(id as CourseTab)} />

      {activeTab === 'overview' ? (
        <div className="content-card-grid">
          {showMinorSections ? (
            <section className="content-card">
              <div className="section-heading">
                <h3>课程信息</h3>
                <span>教学主容器</span>
              </div>
              <div className="settings-page__list">
                <div><span>负责人</span><strong>{course.ownerName || course.ownerId || '当前用户'}</strong></div>
                <div><span>创建时间</span><strong>{fmt(course.createdAt)}</strong></div>
                <div><span>更新时间</span><strong>{fmt(course.updatedAt)}</strong></div>
                <div><span>状态</span><strong>{course.archived ? '已归档' : '进行中'}</strong></div>
              </div>
            </section>
          ) : null}
          <section className="content-card">
            <div className="section-heading">
              <h3>{canManageAssignments ? '教师入口' : '学生入口'}</h3>
              {showMinorSections ? <span>只保留关键动作</span> : null}
            </div>
            <div className="inline-actions">
              <Link className="app-btn v-primary s-md" to={`/script-projects/new?courseId=${encodeURIComponent(course.courseId)}`}>
                {canManageAssignments ? '创建课程项目' : '创建我的作业项目'}
              </Link>
              <Link className="app-btn v-ghost s-md" to="/script-projects">
                查看项目库
              </Link>
            </div>
          </section>
        </div>
      ) : null}

      {activeTab === 'assignments' ? (
        <>
          <CompactFilterBar
            title="作业筛选"
            summary={<span>{filteredAssignments.length} 项结果</span>}
            actions={canManageAssignments ? <AppButton variant="primary" onClick={() => setDrawerOpen(true)}>发布作业</AppButton> : null}
          >
            <AppInput label="关键词" value={keyword} onChange={(value) => setKeyword(String(value))} placeholder="搜索作业标题、语言、模板或负责人" />
            <label className="input-wrap">
              <span className="label">作业状态</span>
              <select className="ctrl" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as 'ALL' | TeachingAssignment['status'])}>
                <option value="ALL">全部状态</option>
                <option value="DRAFT">草稿</option>
                <option value="PUBLISHED">进行中</option>
                <option value="CLOSED">已关闭</option>
              </select>
            </label>
            <label className="input-wrap">
              <span className="label">模板约束</span>
              <select className="ctrl" value={templateFilter} onChange={(event) => setTemplateFilter(event.target.value as 'ALL' | 'BOUND' | 'FREE')}>
                <option value="ALL">全部作业</option>
                <option value="BOUND">仅看绑定模板</option>
                <option value="FREE">仅看自由风格</option>
              </select>
            </label>
          </CompactFilterBar>

          <div className="assignment-card-grid">
            {filteredAssignments.map((assignment) => {
              const template = assignment.styleTemplateId ? templateMap[assignment.styleTemplateId] : null
              const dueStatus = assignmentDueStatus(assignment)
              return (
                <article key={assignment.assignmentId} className="course-card">
                  <div className="course-card__head">
                    <div>
                      <h3>{assignment.title}</h3>
                      <p>{assignment.brief || '暂未填写作业说明。'}</p>
                    </div>
                    <div className="course-card__status">
                      <span className={`soft-badge ${assignment.status === 'PUBLISHED' ? 'is-success' : assignment.status === 'CLOSED' ? 'is-muted' : ''}`}>
                        {assignmentStatusLabel(assignment.status)}
                      </span>
                    </div>
                  </div>
                  <div className="course-card__meta">
                    <span>画幅：{assignment.aspectRatio || '未设置'}</span>
                    <span>时长：{assignment.targetDuration ? `${assignment.targetDuration} 秒` : '未设置'}</span>
                    <span>语言：{assignment.language || '未设置'}</span>
                    <span>最晚提交：{assignment.dueAt ? fmt(assignment.dueAt) : '不限时'}</span>
                    <span>截止状态：{dueStatusLabel(dueStatus)}</span>
                  </div>
                  {template ? (
                    <div className="inline-badges">
                      <span className="soft-badge">{template.name}</span>
                      <span className="soft-badge">{templateScopeLabel[template.scope]}</span>
                      <span className="soft-badge">{template.styleKey ? presetDescriptor(template) : template.category || '模板风格'}</span>
                    </div>
                  ) : null}
                  <div className="course-card__actions">
                    <Link className="app-btn v-primary s-md" to={`/courses/${course.courseId}/assignments/${assignment.assignmentId}`}>
                      {canManageAssignments ? '进入评分页' : '查看并提交'}
                    </Link>
                    {canManageAssignments ? (
                      <AppButton
                        loading={updatingAssignmentId === assignment.assignmentId}
                        onClick={() => void handleAssignmentStatus(assignment.assignmentId, assignment.status === 'CLOSED' ? 'PUBLISHED' : 'CLOSED')}
                      >
                        {assignment.status === 'CLOSED' ? '重新开放' : '关闭作业'}
                      </AppButton>
                    ) : null}
                  </div>
                </article>
              )
            })}
          </div>
        </>
      ) : null}

      {activeTab === 'reviews' ? (
        <section className="content-card">
          <div className="section-heading">
            <h3>提交与评分入口</h3>
            {showMinorSections ? <span>{canManageAssignments ? '从这里进入每个作业的评分流' : '进入作业页查看提交记录与反馈'}</span> : null}
          </div>
          <div className="compact-list">
            {assignments.map((assignment) => (
              <Link key={assignment.assignmentId} to={`/courses/${course.courseId}/assignments/${assignment.assignmentId}`} className="compact-list__row">
                <div>
                  <strong>{assignment.title}</strong>
                  <span>{assignmentStatusLabel(assignment.status)} · {assignment.dueAt ? `最晚提交 ${fmt(assignment.dueAt)}` : '不限时'} · {dueStatusLabel(assignmentDueStatus(assignment))}</span>
                </div>
                <em>{canManageAssignments ? '进入评分' : '查看提交'}</em>
              </Link>
            ))}
          </div>
        </section>
      ) : null}

      {activeTab === 'audit' ? (
        <section className="content-card">
          <div className="section-heading">
            <h3>审计与治理</h3>
            {showMinorSections ? <span>课程级留痕入口</span> : null}
          </div>
          <div className="inline-actions">
            <Link className="app-btn v-ghost s-md" to={`/audit-logs?entityType=COURSE&entityId=${encodeURIComponent(course.courseId)}`}>
              查看课程审计
            </Link>
            <Link className="app-btn v-ghost s-md" to="/script-projects">
              查看关联项目
            </Link>
          </div>
        </section>
      ) : null}

      <ActionDrawer
        open={drawerOpen}
        title="发布作业"
        description="作业发布后，学生端只展示任务要求和提交通道。"
        onClose={() => setDrawerOpen(false)}
        footer={
          <>
            <AppButton onClick={() => setDrawerOpen(false)}>取消</AppButton>
            <AppButton variant="primary" loading={saving} onClick={() => void handleCreateAssignment()}>
              保存作业
            </AppButton>
          </>
        }
      >
        <div className="drawer-form">
          <AppInput label="作业标题" value={title} onChange={(value) => setTitle(String(value))} placeholder="例如：AIGC 短片实训 01" />
          <AppInput label="作业说明" as="textarea" rows={4} value={brief} onChange={(value) => setBrief(String(value))} placeholder="描述作业要求、交付标准和注意事项" />
          <div className="drawer-form__grid">
            <AppInput label="画面比例" value={aspectRatio} onChange={(value) => setAspectRatio(String(value))} placeholder="16:9" />
            <AppInput label="目标时长（秒）" type="number" value={targetDuration} onChange={(value) => setTargetDuration(Number(value) || 0)} min={1} />
          </div>
          <AppInput label="语言" value={language} onChange={(value) => setLanguage(String(value))} placeholder="zh-CN" />
          <label className="input-wrap">
            <span className="label">模板约束</span>
            <select className="ctrl" value={styleTemplateId} onChange={(event) => setStyleTemplateId(event.target.value)}>
              <option value="">不限制模板</option>
              {templates.map((template) => (
                <option key={template.templateId} value={template.templateId}>
                  {template.name}
                </option>
              ))}
            </select>
          </label>
          <AppInput
            label="最晚提交时间"
            type="datetime-local"
            value={dueAt}
            onChange={(value) => setDueAt(String(value))}
          />
        </div>
      </ActionDrawer>
    </section>
  )
}
