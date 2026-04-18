import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ActionDrawer } from '@/components/common/ActionDrawer'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { CompactFilterBar } from '@/components/common/CompactFilterBar'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { StatStrip } from '@/components/common/StatStrip'
import { useToast } from '@/context/ToastContext'
import { createCourse, getCourses } from '@/api'
import { useAuthStore } from '@/stores/authStore'
import type { TeachingCourse, UserRole } from '@/types'

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

function matchesKeyword(course: TeachingCourse, keyword: string) {
  if (!keyword.trim()) return true
  const source = [course.name, course.code, course.description, course.ownerName, course.ownerId].filter(Boolean).join(' ').toLowerCase()
  return source.includes(keyword.trim().toLowerCase())
}

export function TeachingCoursesPage() {
  const { showToast } = useToast()
  const user = useAuthStore((s) => s.user)
  const [courses, setCourses] = useState<TeachingCourse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [name, setName] = useState('')
  const [code, setCode] = useState('')
  const [description, setDescription] = useState('')
  const [keyword, setKeyword] = useState('')
  const [archivedFilter, setArchivedFilter] = useState<'ALL' | 'ACTIVE' | 'ARCHIVED'>('ALL')
  const [ownerFilter, setOwnerFilter] = useState<'ALL' | 'MINE'>('ALL')
  const [showMinorSections, setShowMinorSections] = useState(false)

  const canManageCourses = user?.role === 'ADMIN' || user?.role === 'TEACHER'

  const loadCourses = useCallback(async () => {
    setLoading(true)
    try {
      setCourses(await getCourses())
    } catch (error) {
      showToast(error instanceof Error ? error.message : '加载课程失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [showToast])

  useEffect(() => {
    void loadCourses()
  }, [loadCourses])

  const filteredCourses = useMemo(
    () =>
      courses
        .filter((course) => {
          if (!matchesKeyword(course, keyword)) return false
          if (archivedFilter === 'ACTIVE' && course.archived) return false
          if (archivedFilter === 'ARCHIVED' && !course.archived) return false
          if (canManageCourses && ownerFilter === 'MINE' && user?.userId) {
            return course.ownerId === user.userId
          }
          return true
        })
        .sort((left, right) => new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime()),
    [archivedFilter, canManageCourses, courses, keyword, ownerFilter, user?.userId],
  )

  const summary = useMemo(() => {
    const activeCourses = courses.filter((item) => !item.archived).length
    const archivedCourses = courses.filter((item) => item.archived).length
    const mineCourses = user?.userId ? courses.filter((item) => item.ownerId === user.userId).length : 0
    return {
      total: courses.length,
      active: activeCourses,
      archived: archivedCourses,
      mine: mineCourses,
    }
  }, [courses, user?.userId])

  async function handleCreate() {
    if (!name.trim()) {
      showToast('请先填写课程名称', 'error')
      return
    }
    setSaving(true)
    try {
      await createCourse({
        name: name.trim(),
        code: code.trim() || undefined,
        description: description.trim() || undefined,
      })
      setName('')
      setCode('')
      setDescription('')
      setDrawerOpen(false)
      showToast('课程已创建', 'success')
      await loadCourses()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '创建课程失败', 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="teaching-courses-page teaching-courses-page--revamp">
      <StatStrip
        items={[
          { key: 'total', label: '课程总数', value: summary.total },
          { key: 'active', label: '进行中', value: summary.active },
          { key: 'archived', label: '已归档', value: summary.archived },
          { key: 'mine', label: canManageCourses ? '我负责的课程' : '当前可见课程', value: canManageCourses ? summary.mine : filteredCourses.length },
        ]}
      />
      <div className="minor-entry-bar">
        <button type="button" className="nav-btn" onClick={() => setShowMinorSections((current) => !current)}>
          {showMinorSections ? '收起次级信息' : '展开次级信息'}
        </button>
        {!showMinorSections ? <span className="muted">已隐藏课程说明与更新时间，保留主操作入口。</span> : null}
      </div>

      <CompactFilterBar
        title="课程筛选"
        summary={<span>{filteredCourses.length} 门课程 · {roleScopeLabel(user?.role)}</span>}
        actions={
          canManageCourses ? (
            <AppButton variant="primary" onClick={() => setDrawerOpen(true)}>
              新建课程
            </AppButton>
          ) : (
            <Link className="app-btn v-ghost s-md" to="/script-projects/new">
              新建我的项目
            </Link>
          )
        }
      >
        <AppInput label="关键词" value={keyword} onChange={(value) => setKeyword(String(value))} placeholder="搜索课程名、编码、负责人" />
        <label className="input-wrap">
          <span className="label">课程状态</span>
          <select className="ctrl" value={archivedFilter} onChange={(event) => setArchivedFilter(event.target.value as 'ALL' | 'ACTIVE' | 'ARCHIVED')}>
            <option value="ALL">全部课程</option>
            <option value="ACTIVE">仅看进行中</option>
            <option value="ARCHIVED">仅看已归档</option>
          </select>
        </label>
        {canManageCourses ? (
          <label className="input-wrap">
            <span className="label">负责范围</span>
            <select className="ctrl" value={ownerFilter} onChange={(event) => setOwnerFilter(event.target.value as 'ALL' | 'MINE')}>
              <option value="ALL">全部课程</option>
              <option value="MINE">我负责的课程</option>
            </select>
          </label>
        ) : null}
      </CompactFilterBar>

      {loading ? (
        <LoadingSpinner />
      ) : filteredCourses.length ? (
        <div className="course-card-grid">
          {filteredCourses.map((course) => (
            <article key={course.courseId} className="course-card">
              <div className="course-card__head">
                <div>
                  <h3>{course.name}</h3>
                  {showMinorSections ? <p>{course.description || '暂未填写课程说明。'}</p> : null}
                </div>
                <div className="course-card__status">
                  {course.code ? <span className="soft-badge">{course.code}</span> : null}
                  <span className={`soft-badge ${course.archived ? 'is-muted' : 'is-success'}`}>{course.archived ? '已归档' : '进行中'}</span>
                </div>
              </div>
              {showMinorSections ? (
                <div className="course-card__meta">
                  <span>负责人：{course.ownerName || course.ownerId || '当前用户'}</span>
                  <span>更新时间：{fmt(course.updatedAt)}</span>
                </div>
              ) : null}
              <div className="course-card__actions">
                <Link className="app-btn v-primary s-md" to={`/courses/${course.courseId}`}>
                  {canManageCourses ? '进入教学管理' : '查看课程任务'}
                </Link>
                <Link className="app-btn v-ghost s-md" to={`/script-projects/new?courseId=${encodeURIComponent(course.courseId)}`}>
                  {canManageCourses ? '创建课程项目' : '去完成实训'}
                </Link>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <EmptyState
          title={courses.length ? '没有符合条件的课程' : '还没有课程'}
          description={
            courses.length
              ? '请调整筛选条件后再查看。'
              : canManageCourses
                ? '先创建课程，再把作业、学生项目和评分收进同一教学链路。'
                : '当前还没有可浏览的课程，请等待教师发布课程后再进入实训。'
          }
        />
      )}

      <ActionDrawer
        open={drawerOpen}
        title="新建课程"
        description="填写课程名称、编码和课程说明，创建后再去布置作业。"
        onClose={() => setDrawerOpen(false)}
        footer={
          <>
            <AppButton onClick={() => setDrawerOpen(false)}>取消</AppButton>
            <AppButton variant="primary" loading={saving} onClick={() => void handleCreate()}>
              保存课程
            </AppButton>
          </>
        }
      >
        <div className="drawer-form">
          <AppInput label="课程名称" value={name} onChange={(value) => setName(String(value))} placeholder="例如：AIGC 影视创作实训" />
          <AppInput label="课程编码" value={code} onChange={(value) => setCode(String(value))} placeholder="例如：AIGC-2026-S01" />
          <AppInput
            label="课程说明"
            as="textarea"
            rows={5}
            value={description}
            onChange={(value) => setDescription(String(value))}
            placeholder="描述课程目标、交付要求、适用班级等"
          />
        </div>
      </ActionDrawer>
    </section>
  )
}
