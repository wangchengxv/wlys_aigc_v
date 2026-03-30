import { AppButton } from '@/components/common/AppButton'

type Props = {
  url: string
  onPreview: () => void
  onDownload: () => void
  onCopyLink: () => void
  onOpen: () => void
}

export function VideoResultCard({ url, onPreview, onDownload, onCopyLink, onOpen }: Props) {
  return (
    <figure className="video-card panel">
      <video src={url} controls preload="metadata" />
      <figcaption>
        <AppButton size="sm" onClick={onPreview}>
          预览视频
        </AppButton>
        <AppButton size="sm" onClick={onDownload}>
          下载视频
        </AppButton>
        <AppButton size="sm" onClick={onCopyLink}>
          复制链接
        </AppButton>
        <AppButton size="sm" onClick={onOpen}>
          新窗口播放
        </AppButton>
      </figcaption>
    </figure>
  )
}
