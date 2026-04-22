import { NavLink, useLocation } from 'react-router-dom'
import { useMemo, memo, useState } from 'react'
import { scriptProjectWorkflowSteps, getScriptProjectWorkflowCurrentIndex } from '@/components/script/scriptProjectWorkflow'
import { useGlobalSettingsStore } from '@/stores/globalSettingsStore'

type Props = {
  projectId: string
  collapsed?: boolean
  onCollapsedChange?: (collapsed: boolean) => void
}

type StepState = 'completed' | 'active' | 'pending'

const getStepState = (index: number, currentIndex: number): StepState => {
  if (currentIndex === -1) return 'pending'
  if (index < currentIndex) return 'completed'
  if (index === currentIndex) return 'active'
  return 'pending'
}

const CheckIcon = memo(() => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="20 6 9 17 4 12" />
  </svg>
))

const ArrowIcon = memo(() => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="9 18 15 12 9 6" />
  </svg>
))

const InfoIcon = memo(() => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="10" />
    <path d="M12 16v-4" />
    <path d="M12 8h.01" />
  </svg>
))

const LayersIcon = memo(() => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 2L2 7l10 5 10-5-10-5z" />
    <path d="M2 17l10 5 10-5" />
    <path d="M2 12l10 5 10-5" />
  </svg>
))

const ShieldIcon = memo(() => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
  </svg>
))

const ChevronIcon = memo(() => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="15 18 9 12 15 6" />
  </svg>
))

interface StepItemProps {
  step: typeof scriptProjectWorkflowSteps[0] & { state: StepState; index: number }
  projectId: string
  locked: boolean
}

const StepItem = memo(function StepItem({ step, projectId, locked }: StepItemProps) {
  const isLast = step.index === scriptProjectWorkflowSteps.length - 1

  return (
    <li className={`wf-step wf-step--${step.state}${locked ? ' wf-step--locked' : ''}`}>
      <NavLink
        to={step.to(projectId)}
        className={({ isActive }) => `wf-step__link${isActive ? ' is-active' : ''}${locked ? ' is-locked' : ''}`}
        onClick={(event) => {
          if (!locked) return
          event.preventDefault()
        }}
      >
        <div className="wf-step__indicator">
          <div className="wf-step__dot">
            {step.state === 'completed' ? (
              <CheckIcon />
            ) : (
              <span className="wf-step__num">{step.index + 1}</span>
            )}
          </div>
          {!isLast && (
            <div className={`wf-step__line ${step.state === 'completed' ? 'is-filled' : ''}`} />
          )}
        </div>

        <div className="wf-step__body">
          <span className="wf-step__label">{step.label}</span>
          {step.state === 'active' && step.hint && !locked && (
            <span className="wf-step__hint">{step.hint}</span>
          )}
          {locked ? <span className="wf-step__hint">先完成全局设定</span> : null}
        </div>

        <div className="wf-step__chevron">
          <ArrowIcon />
        </div>
      </NavLink>
    </li>
  )
})

export function ScriptProjectWorkflowNav({ projectId, collapsed: controlledCollapsed, onCollapsedChange }: Props) {
  const location = useLocation()
  const [internalCollapsed, setInternalCollapsed] = useState(false)
  const collapsed = controlledCollapsed ?? internalCollapsed
  const setCollapsed = (value: boolean | ((prev: boolean) => boolean)) => {
    const newValue = typeof value === 'function' ? value(internalCollapsed) : value
    if (onCollapsedChange) {
      onCollapsedChange(newValue)
    } else {
      setInternalCollapsed(newValue)
    }
  }
  const toggleCollapsed = () => setCollapsed((prev) => !prev)
  const visualStyleMode = useGlobalSettingsStore((s) => s.visualStyleMode)
  const visualStylePresetId = useGlobalSettingsStore((s) => s.visualStylePresetId)
  const customVisualStyle = useGlobalSettingsStore((s) => s.customVisualStyle)
  const targetDurationSec = useGlobalSettingsStore((s) => s.targetDurationSec)

  const currentIndex = useMemo(() => {
    return getScriptProjectWorkflowCurrentIndex(location.pathname, projectId)
  }, [location.pathname, projectId])

  const completedCount = useMemo(() => {
    return currentIndex > 0 ? currentIndex : 0
  }, [currentIndex])

  const progressPercent = useMemo(() => {
    return (completedCount / (scriptProjectWorkflowSteps.length - 1)) * 100
  }, [completedCount])

  const stepsWithState = useMemo(() => {
    return scriptProjectWorkflowSteps.map((step, index) => ({
      ...step,
      state: getStepState(index, currentIndex),
      index,
    }))
  }, [currentIndex])
  const isGlobalSettingsReady =
    Number.isFinite(targetDurationSec) &&
    targetDurationSec >= 1 &&
    targetDurationSec <= 600 &&
    (
      (visualStyleMode === 'preset' && !!visualStylePresetId?.trim()) ||
      (visualStyleMode === 'custom' && customVisualStyle.trim().length > 0)
    )

  const currentStep = stepsWithState.find((s) => s.state === 'active')

  return (
    <nav className={`wf-sidebar${collapsed ? ' is-collapsed' : ''}`}>
      <div className="wf-sidebar__head">
        <div className="wf-sidebar__icon">
          <LayersIcon />
        </div>
        <div className="wf-sidebar__meta">
          <p className="wf-sidebar__label">生产流程</p>
          <strong>项目工作区</strong>
        </div>
        <button
          type="button"
          className="wf-sidebar__toggle"
          onClick={toggleCollapsed}
          aria-label={collapsed ? '展开导航' : '收起导航'}
        >
          <ChevronIcon />
        </button>
      </div>

      <div className="wf-progress">
        <div className="wf-progress__track">
          <div
            className="wf-progress__fill"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
        <span className="wf-progress__count">
          {completedCount}/{scriptProjectWorkflowSteps.length - 1}
        </span>
      </div>

      <ol className="wf-steps">
        {stepsWithState.map((step) => (
          <StepItem key={step.key} step={step} projectId={projectId} locked={!isGlobalSettingsReady && step.index > 0} />
        ))}
      </ol>

      {currentStep && (
        <div className="wf-guide">
          <div className="wf-guide__icon">
            <InfoIcon />
          </div>
          <div className="wf-guide__text">
            <strong>当前：{currentStep.label}</strong>
            <span>{currentStep.hint}</span>
          </div>
        </div>
      )}

      <div className="wf-sidebar__foot">
        <NavLink
          to={`/audit-logs?entityType=SCRIPT_PROJECT&entityId=${encodeURIComponent(projectId)}`}
          className="wf-audit-link"
        >
          <ShieldIcon />
          查看审计记录
        </NavLink>
      </div>
    </nav>
  )
}
