import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '@/layouts/AppLayout'
import { HistoryPage } from '@/pages/HistoryPage'
import { HomePage } from '@/pages/HomePage'
import { ModelConfigPage } from '@/pages/ModelConfigPage'
import { ProviderHubPage } from '@/pages/ProviderHubPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { ScriptProjectAssetsPage } from '@/pages/ScriptProjectAssetsPage'
import { ScriptProjectCreatePage } from '@/pages/ScriptProjectCreatePage'
import { ScriptProjectDetailPage } from '@/pages/ScriptProjectDetailPage'
import { ScriptProjectListPage } from '@/pages/ScriptProjectListPage'
import { ScriptProjectPreviewPage } from '@/pages/ScriptProjectPreviewPage'
import { ScriptProjectExportPage } from '@/pages/ScriptProjectExportPage'
import { ScriptProjectPromptTemplatesPage } from '@/pages/ScriptProjectPromptTemplatesPage'
import { ScriptProjectVideoPage } from '@/pages/ScriptProjectVideoPage'
import { CanvasPage } from '@/pages/CanvasPage'
import { GlobalSettingsPage } from '@/pages/GlobalSettingsPage'
import { SettingsPage } from '@/pages/SettingsPage'
import { WorkflowDirectorPage } from '@/pages/workflow/WorkflowDirectorPage'
import { WorkflowExportPage } from '@/pages/workflow/WorkflowExportPage'
import { WorkflowScenesPropsPage } from '@/pages/workflow/WorkflowScenesPropsPage'
import { WorkflowScriptStoryPage } from '@/pages/workflow/WorkflowScriptStoryPage'
import { ToolsAssetVisualPage } from '@/pages/ToolsAssetVisualPage'
import { WorkspacePage } from '@/pages/WorkspacePage'
import type { RouteHandle } from '@/routes/types'

const H = (h: RouteHandle) => h

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage />, handle: H({ title: '首页', eyebrow: 'Overview' }) },
      { path: 'canvas', element: <CanvasPage />, handle: H({ title: '敬请期待', eyebrow: 'Canvas' }) },
      {
        path: 'workflow/script-story',
        element: <WorkflowScriptStoryPage />,
        handle: H({ title: '剧本与故事', eyebrow: 'Workflow' }),
      },
      {
        path: 'workflow/scenes-props',
        element: <WorkflowScenesPropsPage />,
        handle: H({ title: '场景与道具', eyebrow: 'Workflow' }),
      },
      {
        path: 'workflow/director',
        element: <WorkflowDirectorPage />,
        handle: H({ title: '导演模式', eyebrow: 'Workflow' }),
      },
      {
        path: 'workflow/export',
        element: <WorkflowExportPage />,
        handle: H({ title: '成片与导出', eyebrow: 'Workflow' }),
      },
      { path: 'workflow/prompts', element: <WorkspacePage />, handle: H({ title: '提示词管理', eyebrow: 'Workflow' }) },
      { path: 'tools/image', element: <WorkspacePage />, handle: H({ title: '文生图', eyebrow: 'Tools' }) },
      { path: 'tools/video', element: <WorkspacePage />, handle: H({ title: '文生视频', eyebrow: 'Tools' }) },
      {
        path: 'tools/image-to-video',
        element: <WorkspacePage />,
        handle: H({ title: '图生视频', eyebrow: 'Tools' }),
      },
      {
        path: 'tools/asset-visual',
        element: <ToolsAssetVisualPage />,
        handle: H({ title: '三视图 / 九宫格', eyebrow: 'Tools' }),
      },
      { path: 'workspace', element: <WorkspacePage />, handle: H({ title: '文生图/文生视频', eyebrow: 'Create' }) },
      { path: 'script-projects', element: <ScriptProjectListPage />, handle: H({ title: '剧本工程', eyebrow: 'Workflow' }) },
      { path: 'script-projects/new', element: <ScriptProjectCreatePage />, handle: H({ title: '新建剧本工程', eyebrow: 'Create' }) },
      { path: 'script-projects/:projectId', element: <ScriptProjectDetailPage />, handle: H({ title: '项目详情', eyebrow: 'Project' }) },
      { path: 'script-projects/:projectId/preview', element: <ScriptProjectPreviewPage />, handle: H({ title: '剧本预览', eyebrow: 'Script' }) },
      { path: 'script-projects/:projectId/assets', element: <ScriptProjectAssetsPage />, handle: H({ title: '资产与关键帧', eyebrow: 'Assets' }) },
      { path: 'script-projects/:projectId/export', element: <ScriptProjectExportPage />, handle: H({ title: '成片与导出', eyebrow: 'Project' }) },
      { path: 'script-projects/:projectId/video', element: <ScriptProjectVideoPage />, handle: H({ title: '视频生成', eyebrow: 'Video' }) },
      {
        path: 'script-projects/:projectId/prompt-templates',
        element: <ScriptProjectPromptTemplatesPage />,
        handle: H({ title: '提示词模板', eyebrow: 'Project' }),
      },
      { path: 'history', element: <HistoryPage />, handle: H({ title: '历史记录', eyebrow: 'Library' }) },
      { path: 'settings', element: <SettingsPage />, handle: H({ title: '设置', eyebrow: 'Preferences' }) },
      { path: 'global-settings', element: <GlobalSettingsPage />, handle: H({ title: '全局设定', eyebrow: 'Defaults' }) },
      { path: 'models/hub', element: <ProviderHubPage />, handle: H({ title: '服务商中心', eyebrow: 'Providers' }) },
      { path: 'models', element: <ModelConfigPage />, handle: H({ title: '模型配置', eyebrow: 'Admin' }) },
      { path: '*', element: <NotFoundPage />, handle: H({ title: '页面不存在', eyebrow: 'Error' }) },
    ],
  },
])
