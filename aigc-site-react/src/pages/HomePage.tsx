import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { QuickActionGrid } from '@/components/common/QuickActionGrid'
import { StatStrip } from '@/components/common/StatStrip'
import { WorkflowSteps } from '@/components/common/WorkflowSteps'
import { useAuthStore } from '@/stores/authStore'

export function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const user = useAuthStore((s) => s.user)
  const isAdmin = user?.role === 'ADMIN'
  const isTeacher = user?.role === 'TEACHER'
  const isStudent = user?.role === 'STUDENT'

  const quickActions = useMemo(
    () =>
      [
        { key: 'workspace', title: '开始创作', description: '进入图像 / 视频统一工作台', to: '/workspace', badge: '创作' },
        { key: 'canvas', title: '无限画布', description: '进入节点式编排与 Comfy 提交工具页', to: '/canvas', badge: '画布' },
        { key: 'courses', title: '课程中心', description: '查看课程、作业和评分进度', to: '/courses', badge: '教学' },
        { key: 'projects', title: isStudent ? '我的项目' : '项目库', description: '管理剧本、资产、视频与导出', to: '/script-projects', badge: '项目' },
        isAdmin
          ? { key: 'resources', title: '媒体资源', description: '查看统一存储记录与资源入口', to: '/admin/media-resources', badge: '资源' }
          : isTeacher
            ? { key: 'asset-visual', title: '三视图工具', description: '快速处理角色设定与三视图素材', to: '/tools/asset-visual', badge: '工具' }
            : { key: 'history', title: '历史记录', description: '回看个人生成结果与下载记录', to: '/history', badge: '我的' },
      ],
    [isAdmin, isStudent, isTeacher],
  )

  const sectionActions = [
    {
      key: 'teaching',
      title: '教学管理区',
      description: '课程、作业、提交与评分放在同一条链路里，减少来回跳页。',
      to: '/courses',
    },
    {
      key: 'production',
      title: '项目生产区',
      description: '剧本、资产、视频、配音、成片和导出统一进项目工作区。',
      to: '/script-projects',
    },
    isAdmin
      ? {
          key: 'resource',
          title: '资源与配置',
          description: '模型、媒体资源和系统设置收口到轻量后台区。',
          to: '/settings',
        }
      : isTeacher
        ? {
            key: 'tooling',
            title: '创作工具区',
            description: '统一从工作台、三视图工具和历史记录切入创作动作。',
            to: '/tools/asset-visual',
          }
        : {
            key: 'personal',
            title: '个人创作区',
            description: '从工作台、课程任务和历史记录组织个人创作节奏。',
            to: '/history',
          },
  ]

  const steps = [
    { key: 'course', title: '建立课程或进入已有课程', description: '从课程中心承接教学任务、学生项目和评分动作。' },
    { key: 'project', title: '创建或进入项目', description: '在项目工作区推进剧本、镜头、视频、配音和导出。' },
    { key: 'delivery', title: '审核与交付', description: '在同一工作区检查成片状态、导出包和提交结果。' },
  ]

  const summary = [
    { key: 'nav', label: '一级导航', value: '5 组' },
    { key: 'tools', label: '统一入口', value: '1 个' },
    { key: 'roles', label: '角色模式', value: user?.role || 'GUEST' },
  ]

  function openLoginEntry() {
    const next = new URLSearchParams(searchParams)
    next.set('login', '1')
    setSearchParams(next, { replace: true })
  }

  return (
    <section className="workspace-home">
      <div className="workspace-home__hero">
        <div className="workspace-home__hero-copy">
          <span className="workspace-home__pill">{user ? '已登录工作区' : '轻量入口模式'}</span>
          <h2>把教学、创作和交付收进一个清爽工作台</h2>
          <p></p>
        </div>
        <div className="workspace-home__hero-actions">
          <Link className="app-btn v-primary s-md" to="/workspace">
            进入创作工作台
          </Link>
          <Link className="app-btn v-ghost s-md" to="/courses">
            打开课程中心
          </Link>
          {!user ? (
            <button type="button" className="app-btn v-ghost s-md" onClick={openLoginEntry}>
              登录后台
            </button>
          ) : null}
        </div>
      </div>

      <StatStrip items={summary} className="stat-strip--compact" showHint={false} />

      <QuickActionGrid items={quickActions} />

      <div className="workspace-home__grid">
        <section className="content-card">
          <div className="section-heading">
            <h3>功能分区</h3>
            <span>按工作目标组织，不按系统配置堆叠</span>
          </div>
          <QuickActionGrid items={sectionActions} className="quick-action-grid--stack" />
        </section>

        <section className="content-card">
          <div className="section-heading">
            <h3>推荐流程</h3>
            <span>教学发起、项目生产、审核交付</span>
          </div>
          <WorkflowSteps items={steps} />
        </section>
      </div>

    </section>
  )
}
