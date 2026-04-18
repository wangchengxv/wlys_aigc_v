import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getAdminUsers, getCourses, getOrgUnits } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { CompactFilterBar } from '@/components/common/CompactFilterBar'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { StatStrip } from '@/components/common/StatStrip'
import { ScriptProjectCard } from '@/components/script/ScriptProjectCard'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { AdminUser, ContentReviewStatus, OrgUnit, ProjectStatus, TeachingCourse } from '@/types'

type DeliveryFilter = 'ALL' | 'READY' | 'PROCESSING' | 'PENDING'
type CourseLifecycleFilter = 'ALL' | 'ACTIVE' | 'ARCHIVED'

function getProjectStatusLabel(status: ProjectStatus) {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'SCRIPT_REFINING':
      return '剧本完善中'
    case 'SCRIPT_READY':
      return '剧本已就绪'
    case 'ASSET_READY':
      return '资产已就绪'
    case 'VIDEO_GENERATING':
      return '视频生成中'
    case 'DUBBING_GENERATING':
      return '配音生成中'
    case 'LIP_SYNC_GENERATING':
      return '口型同步中'
    case 'FINAL_COMPOSITION_READY':
      return '成片已就绪'
    case 'EXPORT_PACKAGE_READY':
    case 'COMPLETED':
      return '可交付'
    case 'FAILED':
    case 'PARTIAL_FAILED':
      return '失败'
    default:
      return status
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

function getDeliveryStatus(status: ProjectStatus): Exclude<DeliveryFilter, 'ALL'> {
  if (status === 'EXPORT_PACKAGE_READY' || status === 'COMPLETED') return 'READY'
  if (
    status === 'EXPORT_PACKAGE_GENERATING' ||
    status === 'FINAL_COMPOSITION_GENERATING' ||
    status === 'LIP_SYNC_GENERATING' ||
    status === 'DUBBING_GENERATING' ||
    status === 'VIDEO_GENERATING'
  ) {
    return 'PROCESSING'
  }
  return 'PENDING'
}

function matchKeyword(source: string[], keyword: string) {
  if (!keyword.trim()) return true
  return source.join(' ').toLowerCase().includes(keyword.trim().toLowerCase())
}

export function ScriptProjectListPage() {
  const { showToast } = useToast()
  const loadProjects = useScriptProjectStore((s) => s.loadProjects)
  const listLoading = useScriptProjectStore((s) => s.listLoading)
  const projects = useScriptProjectStore((s) => s.projects)
  const [courses, setCourses] = useState<TeachingCourse[]>([])
  const [users, setUsers] = useState<AdminUser[]>([])
  const [orgUnits, setOrgUnits] = useState<OrgUnit[]>([])
  const [searchParams, setSearchParams] = useSearchParams()

  const deletedView = searchParams.get('deleted') === 'true'
  const keyword = searchParams.get('keyword') ?? ''
  const courseId = searchParams.get('courseId') ?? ''
  const classroomId = searchParams.get('classroomId') ?? ''
  const ownerId = searchParams.get('ownerId') ?? ''
  const projectStatus = searchParams.get('projectStatus') ?? ''
  const reviewStatus = searchParams.get('reviewStatus') ?? ''
  const deliveryStatus = (searchParams.get('deliveryStatus') as DeliveryFilter | null) ?? 'ALL'
  const courseLifecycle = (searchParams.get('courseLifecycle') as CourseLifecycleFilter | null) ?? 'ALL'

  useEffect(() => {
    void loadProjects({ deleted: deletedView })
  }, [deletedView, loadProjects])

  useEffect(() => {
    void (async () => {
      try {
        setCourses(await getCourses())
      } catch (error) {
        showToast(error instanceof Error ? error.message : '加载课程信息失败', 'error')
      }
    })()
  }, [showToast])

  useEffect(() => {
    void (async () => {
      const [userResult, orgResult] = await Promise.allSettled([getAdminUsers(), getOrgUnits()])
      if (userResult.status === 'fulfilled') setUsers(userResult.value)
      if (orgResult.status === 'fulfilled') setOrgUnits(orgResult.value)
    })()
  }, [])

  const courseMap = useMemo(() => Object.fromEntries(courses.map((item) => [item.courseId, item])), [courses])
  const userMap = useMemo(() => Object.fromEntries(users.map((item) => [item.userId, item])), [users])
  const unitMap = useMemo(() => Object.fromEntries(orgUnits.map((item) => [item.unitId, item])), [orgUnits])

  const userOptions = useMemo(
    () =>
      Array.from(
        new Map(
          projects
            .filter((item) => !courseId || item.courseId === courseId)
            .map((item) => [
              item.ownerId || item.projectId,
              { value: item.ownerId || '', label: item.ownerName || item.ownerId || '未记录提交人' },
            ]),
        ).values(),
      ).filter((item) => item.value),
    [courseId, projects],
  )

  const classroomOptions = useMemo(
    () => orgUnits.filter((item) => item.type === 'CLASSROOM').sort((left, right) => left.name.localeCompare(right.name, 'zh-CN')),
    [orgUnits],
  )

  const filteredProjects = useMemo(() => {
    return projects.filter((project) => {
      const course = project.courseId ? courseMap[project.courseId] : null
      const owner = project.ownerId ? userMap[project.ownerId] : null
      const classroom = owner?.classroomId ? unitMap[owner.classroomId] : null
      const resolvedDeliveryStatus = getDeliveryStatus(project.status)
      const source = [
        project.name,
        project.scriptSummary,
        project.ownerName,
        project.ownerId,
        project.courseId,
        course?.name,
        classroom?.name,
        getProjectStatusLabel(project.status),
        getReviewStatusLabel(project.contentReviewStatus),
      ].filter(Boolean) as string[]

      if (!matchKeyword(source, keyword)) return false
      if (courseId && project.courseId !== courseId) return false
      if (classroomId && owner?.classroomId !== classroomId) return false
      if (ownerId && project.ownerId !== ownerId) return false
      if (projectStatus && project.status !== projectStatus) return false
      if (reviewStatus && (project.contentReviewStatus ?? 'NOT_SUBMITTED') !== reviewStatus) return false
      if (deliveryStatus !== 'ALL' && resolvedDeliveryStatus !== deliveryStatus) return false
      if (courseLifecycle === 'ACTIVE' && course?.archived) return false
      if (courseLifecycle === 'ARCHIVED' && !course?.archived) return false
      return true
    })
  }, [classroomId, courseId, courseLifecycle, courseMap, deliveryStatus, keyword, ownerId, projectStatus, projects, reviewStatus, unitMap, userMap])

  const summary = useMemo(
    () => ({
      total: projects.length,
      pendingReview: projects.filter((item) => item.contentReviewStatus === 'PENDING').length,
      readyToDeliver: projects.filter((item) => getDeliveryStatus(item.status) === 'READY').length,
      archivedCourse: projects.filter((item) => item.courseId && courseMap[item.courseId]?.archived).length,
    }),
    [courseMap, projects],
  )

  function updateQuery(next: Record<string, string | null>) {
    const params = new URLSearchParams(searchParams)
    Object.entries(next).forEach(([key, value]) => {
      if (!value || value === 'ALL') {
        params.delete(key)
      } else {
        params.set(key, value)
      }
    })
    setSearchParams(params, { replace: true })
  }

  return (
    <section className="script-list-page script-list-page--revamp">
      <StatStrip
        items={[
          { key: 'total', label: deletedView ? '回收站项目' : '项目总数', value: summary.total },
          { key: 'review', label: '待审核项目', value: summary.pendingReview },
          { key: 'ready', label: '可交付项目', value: summary.readyToDeliver },
          { key: 'archived', label: '归档课程项目', value: summary.archivedCourse },
        ]}
      />

      <CompactFilterBar
        title="项目筛选"
        summary={<span>{filteredProjects.length} 项结果</span>}
        actions={
          <div className="inline-actions">
            <AppButton variant={deletedView ? 'ghost' : 'primary'} onClick={() => updateQuery({ deleted: null })}>
              项目列表
            </AppButton>
            <AppButton variant={deletedView ? 'primary' : 'ghost'} onClick={() => updateQuery({ deleted: 'true' })}>
              回收站
            </AppButton>
            <Link className="app-btn v-ghost s-md" to="/script-projects/new">
              新建项目
            </Link>
          </div>
        }
      >
        <AppInput label="关键词" value={keyword} onChange={(value) => updateQuery({ keyword: String(value).trim() || null })} placeholder="搜索项目名、课程、班级、提交人" />
        <label className="input-wrap">
          <span className="label">课程</span>
          <select className="ctrl" value={courseId} onChange={(event) => updateQuery({ courseId: event.target.value || null })}>
            <option value="">全部课程</option>
            {courses.map((course) => (
              <option key={course.courseId} value={course.courseId}>
                {course.name}
              </option>
            ))}
          </select>
        </label>
        <label className="input-wrap">
          <span className="label">班级</span>
          <select className="ctrl" value={classroomId} onChange={(event) => updateQuery({ classroomId: event.target.value || null })}>
            <option value="">全部班级</option>
            {classroomOptions.map((unit) => (
              <option key={unit.unitId} value={unit.unitId}>
                {unit.name}
              </option>
            ))}
          </select>
        </label>
        <label className="input-wrap">
          <span className="label">提交人</span>
          <select className="ctrl" value={ownerId} onChange={(event) => updateQuery({ ownerId: event.target.value || null })}>
            <option value="">全部提交人</option>
            {userOptions.map((owner) => (
              <option key={owner.value} value={owner.value}>
                {owner.label}
              </option>
            ))}
          </select>
        </label>
      </CompactFilterBar>

      {listLoading ? (
        <LoadingSpinner />
      ) : filteredProjects.length ? (
        <div className="project-card-grid">
          {filteredProjects.map((project) => {
            const course = project.courseId ? courseMap[project.courseId] : null
            const owner = project.ownerId ? userMap[project.ownerId] : null
            const classroom = owner?.classroomId ? unitMap[owner.classroomId] : null
            return (
              <ScriptProjectCard
                key={project.projectId}
                project={project}
                deletedView={deletedView}
                courseLabel={course?.name || project.courseId || '未绑定课程'}
                courseLifecycleLabel={course ? (course.archived ? '课程已归档' : '课程进行中') : '未绑定课程'}
                classroomLabel={classroom?.name || '未绑定班级'}
                ownerLabel={owner?.displayName || project.ownerName || project.ownerId || '未记录提交人'}
              />
            )
          })}
        </div>
      ) : (
        <EmptyState
          title={projects.length ? '没有符合条件的项目' : '还没有项目'}
          description={projects.length ? '请调整筛选条件后再查看。' : '先创建项目，再把剧本、资产、视频和导出放进同一工作区。'}
        />
      )}

    </section>
  )
}
