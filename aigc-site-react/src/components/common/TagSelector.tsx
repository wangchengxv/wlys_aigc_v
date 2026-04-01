type Props = {
  options: string[]
  selected: string
  onChange: (value: string) => void
}

export function TagSelector({ options, selected, onChange }: Props) {
  return (
    <div className="tags">
      {options.map((item) => (
        <button key={item} type="button" className={`tag${item === selected ? ' active' : ''}`} onClick={() => onChange(item)}>
          {item}
        </button>
      ))}
    </div>
  )
}
