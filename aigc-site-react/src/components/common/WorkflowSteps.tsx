type WorkflowStep = {
  key: string
  title: string
  description: string
}

type Props = {
  items: WorkflowStep[]
  className?: string
}

export function WorkflowSteps({ items, className = '' }: Props) {
  return (
    <ol className={['workflow-steps', className].filter(Boolean).join(' ')}>
      {items.map((item, index) => (
        <li key={item.key} className="workflow-steps__item">
          <span className="workflow-steps__index">{index + 1}</span>
          <div>
            <strong>{item.title}</strong>
            <p>{item.description}</p>
          </div>
        </li>
      ))}
    </ol>
  )
}
