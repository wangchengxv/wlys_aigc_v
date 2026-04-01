import { Link } from 'react-router-dom'

type Props = {
  to?: string
  children?: React.ReactNode
  className?: string
}

export function PageBackLink({ to = '/settings', children = '返回设置', className = '' }: Props) {
  return (
    <Link to={to} className={`page-back-link${className ? ` ${className}` : ''}`}>
      <span className="page-back-link__glyph" aria-hidden>
        ←
      </span>
      {children}
    </Link>
  )
}
