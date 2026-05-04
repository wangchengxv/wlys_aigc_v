import type { ComponentType, ReactNode } from 'react'
import { createBrowserRouter, Navigate, useParams } from 'react-router-dom'
import { AppLayout } from '@/layouts/AppLayout'
import type { RouteHandle } from '@/routes/types'
import { getScriptProjectWorkflowFallbackPath, isScriptProjectWorkflowStepHidden } from '@/lib/scriptProject/workflowFeatureGate'

const H = (h: RouteHandle) => h

function lazyPage<T extends object>(
  loader: () => Promise<T>,
  exportName: string,
) {
  return async () => {
    const mod = await loader()
    return { Component: (mod as Record<string, unknown>)[exportName] as ComponentType<object> }
  }
}

function ScriptProjectWorkflowFeatureGate({
  stepKey,
  children,
}: {
  stepKey: string
  children: ReactNode
}) {
  const { projectId = '' } = useParams()
  if (isScriptProjectWorkflowStepHidden(stepKey)) {
    return <Navigate to={getScriptProjectWorkflowFallbackPath(projectId)} replace />
  }
  return <>{children}</>
}

function lazyGuardedScriptProjectPage<T extends object>(
  loader: () => Promise<T>,
  exportName: string,
  stepKey: string,
) {
  return async () => {
    const mod = await loader()
    const Component = (mod as Record<string, unknown>)[exportName] as ComponentType<object>
    const GuardedComponent = () => (
      <ScriptProjectWorkflowFeatureGate stepKey={stepKey}>
        <Component />
      </ScriptProjectWorkflowFeatureGate>
    )
    return { Component: GuardedComponent }
  }
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        lazy: lazyPage(() => import('@/pages/HomePage'), 'HomePage'),
        handle: H({
          title: '平台首页',
          eyebrow: '',
          section: '概览',
          description: '查看平台入口、功能概览与常用创作能力导航。',
        }),
      },
      {
        path: 'canvas',
        lazy: lazyPage(() => import('@/pages/CanvasPage'), 'CanvasPage'),
        handle: H({
          title: '无限画布',
          eyebrow: '',
          section: '创作工具',
          description: '正式可用的节点编排画布，支持草稿同步、项目绑定与 Comfy 执行回显。',
        }),
      },
      {
        path: 'workflow/script-story',
        lazy: lazyPage(() => import('@/pages/workflow/WorkflowScriptStoryPage'), 'WorkflowScriptStoryPage'),
        handle: H({ title: '剧本与故事', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'workflow/scenes-props',
        lazy: lazyPage(() => import('@/pages/workflow/WorkflowScenesPropsPage'), 'WorkflowScenesPropsPage'),
        handle: H({ title: '场景与道具', eyebrow: '流程', section: '项目与作品' }),
      },
      {
        path: 'workflow/director',
        lazy: lazyPage(() => import('@/pages/workflow/WorkflowDirectorPage'), 'WorkflowDirectorPage'),
        handle: H({ title: '导演模式', eyebrow: '流程', section: '项目与作品' }),
      },
      {
        path: 'workflow/dubbing',
        lazy: lazyPage(() => import('@/pages/workflow/WorkflowDubbingPage'), 'WorkflowDubbingPage'),
        handle: H({ title: '配音与旁白', eyebrow: '流程', section: '项目与作品' }),
      },
      {
        path: 'workflow/export',
        lazy: lazyPage(() => import('@/pages/workflow/WorkflowExportPage'), 'WorkflowExportPage'),
        handle: H({ title: '剪辑成片与导出', eyebrow: '流程', section: '项目与作品' }),
      },
      {
        path: 'workflow/prompts',
        lazy: lazyPage(() => import('@/pages/WorkspacePage'), 'WorkspacePage'),
        handle: H({
          title: '提示词管理',
          eyebrow: '流程',
          section: '项目与作品',
          description: '统一管理工作流中可复用的提示词内容与调试入口。',
          workspaceMode: 'both',
          workspaceVariant: 'workspace',
        }),
      },
      {
        path: 'tools/image',
        lazy: lazyPage(() => import('@/pages/WorkspacePage'), 'WorkspacePage'),
        handle: H({
          title: '文生图',
          eyebrow: '',
          section: '创作工具',
          workspaceMode: 'image',
          workspaceVariant: 'image',
        }),
      },
      {
        path: 'tools/video',
        lazy: lazyPage(() => import('@/pages/WorkspacePage'), 'WorkspacePage'),
        handle: H({
          title: '文生视频',
          eyebrow: '',
          section: '创作工具',
          workspaceMode: 'video',
          workspaceVariant: 'video',
        }),
      },
      {
        path: 'tools/image-to-video',
        lazy: lazyPage(() => import('@/pages/WorkspacePage'), 'WorkspacePage'),
        handle: H({
          title: '图生视频',
          eyebrow: '',
          section: '创作工具',
          workspaceMode: 'video',
          workspaceVariant: 'image-to-video',
        }),
      },
      {
        path: 'tools/reverse-prompt',
        lazy: lazyPage(() => import('@/pages/ReversePromptPage'), 'ReversePromptPage'),
        handle: H({
          title: '反推提示词',
          eyebrow: '',
          section: '创作工具',
          description: '上传图片并调用豆包系列模型反推结构化提示词。',
          workspaceVariant: 'reverse-prompt',
        }),
      },
      {
        path: 'tools/asset-visual',
        lazy: lazyPage(() => import('@/pages/ToolsAssetVisualPage'), 'ToolsAssetVisualPage'),
        handle: H({ title: '三视图 / 九宫格', eyebrow: '', section: '创作工具' }),
      },
      {
        path: 'tools/storyboard-lite',
        lazy: lazyPage(() => import('@/pages/storyboard-lite'), 'StoryboardLitePage'),
        handle: H({
          title: '剧本闭环Lite',
          eyebrow: '',
          section: '创作工具',
          description: '独立最小闭环：剧本、三视图/关键帧、图生视频。',
        }),
      },
      {
        path: 'workspace',
        lazy: lazyPage(() => import('@/pages/WorkspacePage'), 'WorkspacePage'),
        handle: H({
          title: '创作工作台',
          eyebrow: '',
          section: '创作工具',
          description: '快速发起图像、视频等 AIGC 生成任务。',
          workspaceMode: 'both',
          workspaceVariant: 'workspace',
        }),
      },
      {
        path: 'courses',
        lazy: lazyPage(() => import('@/pages/TeachingCoursesPage'), 'TeachingCoursesPage'),
        handle: H({
          title: '课程工作台',
          eyebrow: '',
          section: '课程与实训',
          description: '查看课程、作业、提交与实训进度，承载教师与学生的教学主入口。',
        }),
      },
      {
        path: 'courses/:courseId',
        lazy: lazyPage(() => import('@/pages/TeachingCourseDetailPage'), 'TeachingCourseDetailPage'),
        handle: H({ title: '课程详情', eyebrow: '教学', section: '课程与实训' }),
      },
      {
        path: 'courses/:courseId/assignments/:assignmentId',
        lazy: lazyPage(() => import('@/pages/TeachingAssignmentDetailPage'), 'TeachingAssignmentDetailPage'),
        handle: H({ title: '作业提交与评分', eyebrow: '教学', section: '课程与实训' }),
      },
      {
        path: 'script-projects',
        lazy: lazyPage(() => import('@/pages/ScriptProjectListPage'), 'ScriptProjectListPage'),
        handle: H({
          title: '剧本工程',
          eyebrow: '',
          section: '项目与作品',
          description: '按课程、提交人与项目状态统一管理 AIGC 实训项目。',
        }),
      },
      {
        path: 'script-projects/new',
        lazy: lazyPage(() => import('@/pages/ScriptProjectCreatePage'), 'ScriptProjectCreatePage'),
        handle: H({ title: '新建剧本工程', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'script-projects/:projectId',
        lazy: lazyPage(() => import('@/pages/ScriptProjectDetailPage'), 'ScriptProjectDetailPage'),
        handle: H({ title: '项目详情', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'script-projects/:projectId/global-settings',
        lazy: lazyPage(() => import('@/pages/ScriptProjectGlobalSettingsPage'), 'ScriptProjectGlobalSettingsPage'),
        handle: H({ title: '全局设定', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'script-projects/:projectId/preview',
        lazy: lazyPage(() => import('@/pages/ScriptProjectPreviewPage'), 'ScriptProjectPreviewPage'),
        handle: H({ title: '剧本预览', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'script-projects/:projectId/assets',
        lazy: lazyPage(() => import('@/pages/ScriptProjectAssetsPage'), 'ScriptProjectAssetsPage'),
        handle: H({ title: '资产与关键帧', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'script-projects/:projectId/dubbing',
        lazy: lazyGuardedScriptProjectPage(() => import('@/pages/ScriptProjectDubbingPage'), 'ScriptProjectDubbingPage', 'dubbing'),
        handle: H({ title: '配音管理', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'script-projects/:projectId/lip-sync',
        lazy: lazyGuardedScriptProjectPage(() => import('@/pages/ScriptProjectLipSyncPage'), 'ScriptProjectLipSyncPage', 'lip-sync'),
        handle: H({ title: '口型同步', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'script-projects/:projectId/final-composition',
        lazy: lazyGuardedScriptProjectPage(() => import('@/pages/ScriptProjectFinalCompositionPage'), 'ScriptProjectFinalCompositionPage', 'edit'),
        handle: H({ title: '视频剪辑工作台', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'script-projects/:projectId/export',
        lazy: lazyGuardedScriptProjectPage(() => import('@/pages/ScriptProjectExportPage'), 'ScriptProjectExportPage', 'export'),
        handle: H({ title: '剪辑成片与导出', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'script-projects/:projectId/video',
        lazy: lazyPage(() => import('@/pages/ScriptProjectVideoPage'), 'ScriptProjectVideoPage'),
        handle: H({ title: '视频生成', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'script-projects/:projectId/prompt-templates',
        lazy: lazyPage(() => import('@/pages/ScriptProjectPromptTemplatesPage'), 'ScriptProjectPromptTemplatesPage'),
        handle: H({ title: '提示词模板', eyebrow: '', section: '项目与作品' }),
      },
      {
        path: 'history',
        lazy: lazyPage(() => import('@/pages/HistoryPage'), 'HistoryPage'),
        handle: H({ title: '历史记录', eyebrow: '', section: '创作工具' }),
      },
      {
        path: 'login',
        lazy: lazyPage(() => import('@/pages/LoginPage'), 'LoginPage'),
        handle: H({
          title: '登录入口',
          eyebrow: '认证',
          section: '认证中心',
          description: '兼容旧登录地址，访问后会回到首页并打开登录入口。',
        }),
      },
      {
        path: 'social/callback/:provider',
        lazy: lazyPage(() => import('@/pages/SocialLoginCallbackPage'), 'SocialLoginCallbackPage'),
        handle: H({
          title: '第三方登录回调',
          eyebrow: '认证',
          section: '认证中心',
          description: '接收第三方授权回调并完成登录。',
        }),
      },
      {
        path: 'admin/directory',
        lazy: lazyPage(() => import('@/pages/AdminDirectoryPage'), 'AdminDirectoryPage'),
        handle: H({
          title: '组织与用户',
          eyebrow: '',
          section: '组织与用户',
          description: '维护组织、班级和用户归属，支撑高校后台中的账号治理能力。',
        }),
      },
      {
        path: 'admin/media-resources',
        lazy: lazyPage(() => import('@/pages/MediaResourcesPage'), 'MediaResourcesPage'),
        handle: H({ title: '媒体资源中心', eyebrow: '', section: '资源与模型' }),
      },
      {
        path: 'audit-logs',
        lazy: lazyPage(() => import('@/pages/AuditLogsPage'), 'AuditLogsPage'),
        handle: H({ title: '审计日志', eyebrow: '', section: '审核与审计' }),
      },
      {
        path: 'operations-dashboard',
        lazy: lazyPage(() => import('@/pages/OperationsDashboardPage'), 'OperationsDashboardPage'),
        handle: H({
          title: '平台概览',
          eyebrow: '',
          section: '概览',
          description: '汇总课程、提交、项目、审核与导出状态，形成高校实训后台首页的驾驶舱视图。',
        }),
      },
      {
        path: 'settings',
        lazy: lazyPage(() => import('@/pages/SettingsPage'), 'SettingsPage'),
        handle: H({
          title: '设置中心',
          eyebrow: '',
          section: '系统设置',
          description: '保留账号、系统、模型与创作默认配置，不再承担高校业务总入口。',
        }),
      },
      {
        path: 'global-settings',
        lazy: lazyPage(() => import('@/pages/GlobalSettingsPage'), 'GlobalSettingsPage'),
        handle: H({ title: '全局设定', eyebrow: '', section: '系统设置' }),
      },
      {
        path: 'models/hub',
        lazy: lazyPage(() => import('@/pages/ProviderHubPage'), 'ProviderHubPage'),
        handle: H({ title: '服务商中心', eyebrow: '', section: '资源与模型' }),
      },
      {
        path: 'models',
        lazy: lazyPage(() => import('@/pages/ModelConfigPage'), 'ModelConfigPage'),
        handle: H({ title: '模型配置', eyebrow: '', section: '资源与模型' }),
      },
      {
        path: '*',
        lazy: lazyPage(() => import('@/pages/NotFoundPage'), 'NotFoundPage'),
        handle: H({ title: '页面不存在', eyebrow: '', section: '异常页面' }),
      },
    ],
  },
])
