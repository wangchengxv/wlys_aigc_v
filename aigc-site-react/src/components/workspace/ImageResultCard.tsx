import { AppButton } from '@/components/common/AppButton'

type Props = {
  url: string
  onPreview: () => void
  onDownload: () => void
}

export function ImageResultCard({ url, onPreview, onDownload }: Props) {
  return (
    <figure className="img-card panel">
      <button type="button" className="preview" onClick={onPreview}>
        <img src={url} alt="生成图片" />
      </button>
      <figcaption>
        <AppButton size="sm" onClick={onDownload}>
          下载
        </AppButton>
      </figcaption>
    </figure>
  )
}
