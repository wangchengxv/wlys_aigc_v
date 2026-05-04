export interface StepIndicatorProps {
  activeStep: number
  onStepChange: (step: number) => void
  maxAllowedStep: number
}

const STEPS = [
  { id: 1, label: '创建会话' },
  { id: 2, label: '剧本与三视图' },
  { id: 3, label: '确认关键帧' },
  { id: 4, label: '视频生成' },
]

export function StepIndicator({ activeStep, onStepChange, maxAllowedStep }: StepIndicatorProps) {
  return (
    <div className="sl-step-indicator">
      {STEPS.map((step) => {
        const isCompleted = step.id < activeStep
        const isActive = step.id === activeStep
        const isDisabled = step.id > maxAllowedStep

        let className = 'sl-step-item'
        if (isActive) className += ' active'
        else if (isCompleted) className += ' completed'
        if (isDisabled) className += ' disabled'

        return (
          <div
            key={step.id}
            className={className}
            onClick={() => {
              if (!isDisabled) onStepChange(step.id)
            }}
          >
            <div className="sl-step-circle">{isCompleted ? '✓' : step.id}</div>
            <div className="sl-step-label">{step.label}</div>
          </div>
        )
      })}
    </div>
  )
}
