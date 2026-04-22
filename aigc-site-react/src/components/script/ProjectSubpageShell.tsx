import type { ReactNode } from 'react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { HelpHint } from '@/components/common/HelpHint'
import { StatStrip } from '@/components/common/StatStrip'
import { ScriptProjectWorkflowNav } from '@/components/script/ScriptProjectWorkflowNav'
import { getScriptProjectWorkflowCurrentIndex, scriptProjectWorkflowSteps } from '@/components/script/scriptProjectWorkflow'
import { useGlobalSettingsStore } from '@/stores/globalSettingsStore'

type StatItem = {
  key: string
  label: string
  value: string | number
  hint?: string
}

type Props = {
  projectId: string
  title: string
  description: string
  meta?: ReactNode
  toolbar?: ReactNode
  stats?: StatItem[]
  helpTitle?: string
  help?: ReactNode
  className?: string
  children: ReactNode
}

export function ProjectSubpageShell({
  projectId,
  title,
  description,
  meta,
  toolbar,
  stats,
  helpTitle,
  help,
  className = '',
  children,
}: Props) {
  const location = useLocation()
  const navigate = useNavigate()
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const visualStyleMode = useGlobalSettingsStore((s) => s.visualStyleMode)
  const visualStylePresetId = useGlobalSettingsStore((s) => s.visualStylePresetId)
  const customVisualStyle = useGlobalSettingsStore((s) => s.customVisualStyle)
  const targetDurationSec = useGlobalSettingsStore((s) => s.targetDurationSec)
  const currentIndex = getScriptProjectWorkflowCurrentIndex(location.pathname, projectId)
  const currentStep = currentIndex >= 0 ? scriptProjectWorkflowSteps[currentIndex] : null
  const previousStep = currentIndex > 0 ? scriptProjectWorkflowSteps[currentIndex - 1] : null
  const nextStep = currentIndex >= 0 && currentIndex < scriptProjectWorkflowSteps.length - 1
    ? scriptProjectWorkflowSteps[currentIndex + 1]
    : null
  const isGlobalSettingsStep = currentStep?.key === 'global-settings'
  const isGlobalSettingsReady =
    Number.isFinite(targetDurationSec) &&
    targetDurationSec >= 1 &&
    targetDurationSec <= 600 &&
    (
      (visualStyleMode === 'preset' && !!visualStylePresetId?.trim()) ||
      (visualStyleMode === 'custom' && customVisualStyle.trim().length > 0)
    )
  const shouldShowGlobalSettingsGuard = !isGlobalSettingsStep && !isGlobalSettingsReady

  return (
    <div className={`script-project-workflow-layout${sidebarCollapsed ? ' is-collapsed' : ''}`}>
      <ScriptProjectWorkflowNav projectId={projectId} collapsed={sidebarCollapsed} onCollapsedChange={setSidebarCollapsed} />
      <div className="script-project-workflow-layout__main">
        <section className={['project-subpage-shell', className].filter(Boolean).join(' ')}>
          <header className="project-subpage-shell__hero panel glass">
            <div className="project-subpage-shell__copy">
              <h2>{title}</h2>
              <p className="eyebrow">项目工作区</p>
              <p className="muted">{description}</p>
            </div>
            {meta ? <div className="project-subpage-shell__meta">{meta}</div> : null}
          </header>

          {stats?.length ? <StatStrip items={stats} className="project-subpage-shell__stats" /> : null}

          {helpTitle && help ? (
            <HelpHint title={helpTitle} className="project-subpage-shell__help">
              {help}
            </HelpHint>
          ) : null}

          {toolbar ? <section className="project-subpage-shell__toolbar panel glass">{toolbar}</section> : null}

          {shouldShowGlobalSettingsGuard ? (
            <section className="project-subpage-shell__guard panel glass">
              <div>
                <p className="eyebrow">流程提醒</p>
                <h3>请先完成全局设定</h3>
                <p className="muted">当前全局设定未达到最小通过条件（视觉风格 + 目标时长），后续步骤可能出现生成偏差。</p>
              </div>
              <Link className="nav-btn" to={`/script-projects/${projectId}/global-settings`}>
                返回第 1 步：全局设定
              </Link>
            </section>
          ) : null}

          <div className="project-subpage-shell__content">{children}</div>

          <section className="project-subpage-shell__flow panel glass">
            <div className="project-subpage-shell__flow-meta">
              <span className="eyebrow">步骤切换</span>
              <strong>{currentStep ? `当前：${currentStep.label}` : '当前步骤未纳入工作流'}</strong>
            </div>
            <div className="project-subpage-shell__flow-actions">
              <button
                type="button"
                className="nav-btn"
                onClick={() => {
                  if (!previousStep) return
                  navigate(previousStep.to(projectId))
                }}
                disabled={!previousStep}
              >
                上一步
              </button>
              <button
                type="button"
                className="nav-btn primary"
                onClick={() => {
                  if (!nextStep || shouldShowGlobalSettingsGuard) return
                  navigate(nextStep.to(projectId))
                }}
                disabled={!nextStep || shouldShowGlobalSettingsGuard}
              >
                下一步
              </button>
            </div>
          </section>

        </section>
      </div>
    </div>
  )
}
