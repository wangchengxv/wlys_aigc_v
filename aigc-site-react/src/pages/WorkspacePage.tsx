import { useMemo, useState } from 'react'
import { useMatches, useNavigate } from 'react-router-dom'
import { FixedPanelDock, type FixedPanelDockItem } from '@/components/common/FixedPanelDock'
import { SectionTabs } from '@/components/common/SectionTabs'
import { StatStrip } from '@/components/common/StatStrip'
import { PromptPanel } from '@/components/workspace/PromptPanel'
import { ResultPanel } from '@/components/workspace/ResultPanel'
import { useToast } from '@/context/ToastContext'
import type { RouteHandle, WorkspaceRouteVariant } from '@/routes/types'
import { useGenerationStore } from '@/stores/generationStore'
import type { GenerateMode } from '@/types'

type WorkspaceTab = {
  id: WorkspaceRouteVariant
  label: string
  badge?: string
  to: string
}

const WORKSPACE_TABS: WorkspaceTab[] = [
  { id: 'workspace', label: '综合创作', badge: '图文 / 文案', to: '/workspace' },
  { id: 'image', label: '文生图', badge: '图像', to: '/tools/image' },
  { id: 'video', label: '文生视频', badge: '视频', to: '/tools/video' },
  { id: 'image-to-video', label: '图生视频', badge: '需参考图', to: '/tools/image-to-video' },
]

function modeLabel(mode?: GenerateMode) {
  switch (mode) {
    case 'image':
      return '图像创作'
    case 'video':
      return '视频创作'
    case 'text':
      return '文案生成'
    case 'both':
    default:
      return '综合创作'
  }
}

function variantLabel(variant: WorkspaceRouteVariant) {
  return WORKSPACE_TABS.find((item) => item.id === variant)?.label || '综合创作'
}

export function WorkspacePage() {
  const { showToast } = useToast()
  const matches = useMatches()
  const navigate = useNavigate()
  const [activePanel, setActivePanel] = useState<'prompt' | 'result'>('prompt')
  const currentTask = useGenerationStore((s) => s.currentTask)
  const favorites = useGenerationStore((s) => s.favorites)
  const last = matches[matches.length - 1]
  const handle = (last?.handle as RouteHandle | undefined) ?? null
  const workspaceVariant = handle?.workspaceVariant ?? 'workspace'
  const workspaceMode = handle?.workspaceMode ?? 'both'
  const availableModes = useMemo(
    () =>
      workspaceVariant === 'workspace'
        ? (['text', 'both', 'image', 'video'] as GenerateMode[])
        : ([workspaceMode] as GenerateMode[]),
    [workspaceMode, workspaceVariant],
  )

  const dockItems = useMemo<FixedPanelDockItem[]>(
    () => [
      {
        id: 'prompt',
        label: '输入与参数',
        eyebrow: '创作',
        summary:
          workspaceVariant === 'workspace'
            ? '综合入口支持在同一面板里切换文案、图像、图文与视频；工具入口则固定落在对应模式。'
            : '当前入口固定在对应生成模式，只保留本任务需要的参数与动作。',
        badge: variantLabel(workspaceVariant),
        content: (
          <PromptPanel
            defaultMode={workspaceMode}
            workspaceVariant={workspaceVariant}
            availableModes={availableModes}
            showQuickVideoAction={workspaceVariant === 'workspace'}
            onGenerated={() => {
              showToast('生成成功', 'success')
              setActivePanel('result')
            }}
          />
        ),
      },
      {
        id: 'result',
        label: '结果与下载',
        eyebrow: '结果',
        summary: currentTask ? '生成结果会集中展示在这里，可直接预览、下载和复制链接。' : '生成完成后自动切到结果面板。',
        badge: currentTask ? '已有结果' : '待生成',
        content: <ResultPanel />,
      },
    ],
    [availableModes, currentTask, showToast, workspaceMode, workspaceVariant],
  )

  return (
    <section className="workspace-page workspace-page--revamp">
      <StatStrip
        items={[
          { key: 'entry', label: '当前入口', value: variantLabel(workspaceVariant) },
          { key: 'mode', label: '默认模式', value: modeLabel(workspaceMode) },
          { key: 'task', label: '最近任务', value: currentTask?.taskId || '尚未生成' },
          { key: 'fav', label: '收藏结果', value: favorites.length },
        ]}
      />

      <section className="workspace-route-switch panel glass">
        <div className="workspace-route-switch__head">
          <div>
            <h3>统一工作台入口</h3>
            <p className="eyebrow">工作台模式</p>
            <p className="muted">四个地址继续保留，但都复用同一套输入、结果与下载结构。</p>
          </div>
        </div>
        <SectionTabs
          items={WORKSPACE_TABS.map(({ id, label, badge }) => ({ id, label, badge }))}
          activeId={workspaceVariant}
          onChange={(id) => {
            const target = WORKSPACE_TABS.find((item) => item.id === id)
            if (target) navigate(target.to)
          }}
        />
      </section>

      <FixedPanelDock
        title="统一工作台"
        description="PromptPanel 与 ResultPanel 在所有工作台入口中保持一致，减少同一任务在多个页面重复出现。"
        items={dockItems}
        activeId={activePanel}
        onChange={(id) => setActivePanel(id as 'prompt' | 'result')}
      />
    </section>
  )
}
