type Props = {
  value: string | number
  onChange: (value: string | number) => void
  label?: string
  as?: 'input' | 'textarea'
  placeholder?: string
  type?: string
  rows?: number
  min?: number
  max?: number
}

export function AppInput({
  value,
  onChange,
  label = '',
  as = 'input',
  placeholder = '',
  type = 'text',
  rows = 5,
  min,
  max,
}: Props) {
  function onUpdate(e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    const t = e.target
    if (t instanceof HTMLInputElement && t.type === 'number') {
      onChange(Number(t.value))
      return
    }
    onChange(t.value)
  }

  return (
    <label className="input-wrap">
      {label ? <span className="label">{label}</span> : null}
      {as === 'textarea' ? (
        <textarea className="ctrl" rows={rows} placeholder={placeholder} value={String(value)} onChange={onUpdate} />
      ) : (
        <input className="ctrl" type={type} placeholder={placeholder} value={String(value)} min={min} max={max} onChange={onUpdate} />
      )}
    </label>
  )
}
