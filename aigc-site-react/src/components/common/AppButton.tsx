import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'ghost' | 'danger'
type Size = 'sm' | 'md'

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant
  size?: Size
  loading?: boolean
  block?: boolean
  children: ReactNode
}

export function AppButton({
  variant = 'ghost',
  size = 'md',
  loading = false,
  block = false,
  className = '',
  disabled,
  children,
  type = 'button',
  ...rest
}: Props) {
  const cls = ['app-btn', `v-${variant}`, `s-${size}`, block ? 'block' : '', className].filter(Boolean).join(' ')
  return (
    <button className={cls} disabled={disabled || loading} type={type === 'submit' || type === 'reset' ? type : 'button'} {...rest}>
      {loading ? <span className="spinner" aria-hidden /> : null}
      {children}
    </button>
  )
}
