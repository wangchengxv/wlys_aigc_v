import { AppButton } from '@/components/common/AppButton'

type Props = {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

export function Pagination({ page, totalPages, onChange }: Props) {
  return (
    <div className="pager">
      <AppButton size="sm" disabled={page <= 1} onClick={() => onChange(page - 1)}>
        上一页
      </AppButton>
      <span>
        第 {page} / {totalPages} 页
      </span>
      <AppButton size="sm" disabled={page >= totalPages} onClick={() => onChange(page + 1)}>
        下一页
      </AppButton>
    </div>
  )
}
