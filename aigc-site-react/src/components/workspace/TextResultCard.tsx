import { AppButton } from '@/components/common/AppButton'

type Props = {
  text: string
  favorite: boolean
  onCopy: () => void
  onFavorite: () => void
}

export function TextResultCard({ text, favorite, onCopy, onFavorite }: Props) {
  return (
    <article className="text-card panel">
      <p>{text}</p>
      <div className="actions">
        <AppButton size="sm" onClick={onCopy}>
          复制
        </AppButton>
        <AppButton size="sm" onClick={onFavorite}>
          {favorite ? '已收藏' : '收藏'}
        </AppButton>
      </div>
    </article>
  )
}
