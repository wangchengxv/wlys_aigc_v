import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAdminUsers, getOperationsDashboard, getOrgUnits } from '@/api'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { QuickActionGrid } from '@/components/common/QuickActionGrid'
import { StatStrip } from '@/components/common/StatStrip'
import { WorkflowSteps } from '@/components/common/WorkflowSteps'
import { useToast } from '@/context/ToastContext'
import { useAuthStore } from '@/stores/authStore'
import type { AdminUser, OperationsDashboardResponse, OrgUnit, UserRole } from '@/types'

type DirectorySnapshot = {
  organizations: number
  classrooms: number
  teachers: number
  students: number
}

function fmtDateTime(value?: string | null) {
  if (!value) return '未记录'
  const time = new Date(value)
  return Number.isNaN(time.getTime()) ? value : time.toLocaleString('zh-CN')
}

function fmtCount(value?: number | null) {
  if (typeof value !== 'number' || Number.isNaN(value)) return '--'
  return value.toLocaleString('zh-CN')
}

function canAccessDashboard(role?: UserRole | null) {
  return role === 'ADMIN' || role === 'TEACHER'
}

function isAdminRole(role?: UserRole | null) {
  return role === 'ADMIN'
}

function buildDirectorySnapshot(orgUnits: OrgUnit[], users: AdminUser[]): DirectorySnapshot {
  return {
    organizations: orgUnits.filter((item) => item.type === 'ORGANIZATION').length,
    classrooms: orgUnits.filter((item) => item.type === 'CLASSROOM').length,
    teachers: users.filter((item) => item.role === 'TEACHER').length,
    students: users.filter((item) => item.role === 'STUDENT').length,
  }
}

function metricOf(dashboard: OperationsDashboardResponse | null, key: string) {
  return dashboard?.overviewCards.find((item) => item.key === key)?.value ?? 0
}

export function OperationsDashboardPage() {
  const { showToast } = useToast()
  const user = useAuthStore((s) => s.user)
  const [dashboard, setDashboard] = useState<OperationsDashboardResponse | null>(null)
  const [directorySnapshot, setDirectorySnapshot] = useState<DirectorySnapshot | null>(null)
  const [loading, setLoading] = useState(true)

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    try {
      const nextDashboard = await getOperationsDashboard()
      setDashboard(nextDashboard)

      if (isAdminRole(user?.role)) {
        const [orgUnitsResult, usersResult] = await Promise.allSettled([getOrgUnits(), getAdminUsers()])
        if (orgUnitsResult.status === 'fulfilled' && usersResult.status === 'fulfilled') {
          setDirectorySnapshot(buildDirectorySnapshot(orgUnitsResult.value, usersResult.value))
        }
      } else {
        setDirectorySnapshot(null)
      }
    } catch (error) {
      showToast(error instanceof Error ? error.message : '加载平台概览失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [showToast, user?.role])

  useEffect(() => {
    if (!canAccessDashboard(user?.role)) {
      setLoading(false)
      return
    }
    void loadDashboard()
  }, [loadDashboard, user?.role])

  const stats = useMemo(
    () => [
      { key: 'course', label: '课程数', value: fmtCount(metricOf(dashboard, 'courseCount')) },
      { key: 'assignment', label: '任务数', value: fmtCount(metricOf(dashboard, 'assignmentCount')) },
      { key: 'project', label: '项目数', value: fmtCount(metricOf(dashboard, 'projectCount')) },
      { key: 'review', label: '待审核', value: fmtCount(metricOf(dashboard, 'pendingReviewCount')) },
    ],
    [dashboard],
  )

  const pendingItems = useMemo(() => {
    const failed = dashboard?.statusDistribution.find((item) => item.key === 'failed')?.count ?? 0
    const generating = dashboard?.statusDistribution.find((item) => item.key === 'generating')?.count ?? 0
    const pendingReview = metricOf(dashboard, 'pendingReviewCount')
    const items = [
      pendingReview > 0 ? { key: 'review', title: '待审核项目', description: `${pendingReview} 个项目仍待处理`, to: '/script-projects', badge: '高优先' } : null,
      failed > 0 ? { key: 'failed', title: '异常项目', description: `${failed} 个项目需要排查`, to: '/script-projects', badge: '异常' } : null,
      generating > 0 ? { key: 'generating', title: '生产中的项目', description: `${generating} 个项目正在生成`, to: '/script-projects', badge: '进行中' } : null,
    ].filter(Boolean)
    return items as Array<{ key: string; title: string; description: string; to: string; badge?: string }>
  }, [dashboard])

  const quickEntries = useMemo(() => {
    const entries = [
      { key: 'courses', title: '课程中心', description: '查看课程、作业与评分流', to: '/courses', badge: '教学' },
      { key: 'projects', title: '项目库', description: '进入项目工作区与交付链路', to: '/script-projects', badge: '项目' },
      { key: 'resources', title: '资源中心', description: '查看媒体资源与工具入口', to: '/admin/media-resources', badge: '资源' },
      { key: 'directory', title: '组织用户', description: '维护组织、班级与账号', to: '/admin/directory', badge: '管理' },
    ]
    return entries.filter((item) => item.key !== 'directory' || isAdminRole(user?.role))
  }, [user?.role])

  const steps = [
    { key: 'teaching', title: '教学运行', description: '从课程、任务、提交和评分四个节点跟踪教学完成度。' },
    { key: 'production', title: '项目生产', description: '从剧本、资产、视频、配音到导出观察项目成熟度。' },
    { key: 'governance', title: '治理交付', description: '对待审核、异常和导出就绪项目做集中处置。' },
  ]

  if (!canAccessDashboard(user?.role)) {
    return <EmptyState title="当前角色不可访问" description="平台概览面向管理员和教师开放，学生请直接进入课程或项目工作区。" />
  }

  if (loading) return <LoadingSpinner />

  return (
    <section className="operations-dashboard-page operations-dashboard-page--revamp">
      <StatStrip items={stats} className="stat-strip--compact" showHint={false} />

      <div className="content-card-grid">
        <section className="content-card">
          <div className="section-heading">
            <h3>快捷入口</h3>
            <span>常用后台能力收口在这里</span>
          </div>
          <QuickActionGrid items={quickEntries} />
        </section>

        <section className="content-card">
          <div className="section-heading">
            <h3>待处理项</h3>
            <span>只保留高价值提醒</span>
          </div>
          {pendingItems.length ? (
            <QuickActionGrid items={pendingItems} className="quick-action-grid--stack" />
          ) : (
            <EmptyState title="当前没有待处理项" description="审核、异常和生产中的项目都会在这里集中提醒。" />
          )}
        </section>
      </div>

      <section className="content-card">
        <div className="section-heading">
          <h3>最近活动</h3>
          <span>按时间查看最近发生的课程与项目动作</span>
        </div>
        <div className="compact-list">
          {(dashboard?.recentActivities || []).slice(0, 6).map((activity) => (
            <Link key={activity.key} to={activity.link || '/script-projects'} className="compact-list__row">
              <div>
                <strong>{activity.label}</strong>
                <span>{activity.summary}</span>
              </div>
              <em>{fmtDateTime(activity.occurredAt)}</em>
            </Link>
          ))}
        </div>
      </section>

      <div className="content-card-grid">
        <section className="content-card">
          <div className="section-heading">
            <h3>运行方式</h3>
            <span>教学、项目、治理三段式</span>
          </div>
          <WorkflowSteps items={steps} />
        </section>

        {isAdminRole(user?.role) && directorySnapshot ? (
          <section className="content-card">
            <div className="section-heading">
              <h3>组织快照</h3>
              <span>管理员视角</span>
            </div>
            <div className="settings-page__list">
              <div><span>组织数</span><strong>{directorySnapshot.organizations}</strong></div>
              <div><span>班级数</span><strong>{directorySnapshot.classrooms}</strong></div>
              <div><span>教师数</span><strong>{directorySnapshot.teachers}</strong></div>
              <div><span>学生数</span><strong>{directorySnapshot.students}</strong></div>
            </div>
          </section>
        ) : null}
      </div>

    </section>
  )
}
