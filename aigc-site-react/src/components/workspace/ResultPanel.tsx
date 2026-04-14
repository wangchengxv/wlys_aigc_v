import { useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import { AppButton } from '@/components/common/AppButton'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { SkeletonCard } from '@/components/common/SkeletonCard'
import { useToast } from '@/context/ToastContext'
import { resolveApiMediaUrl } from '@/api'
import { useGenerationStore } from '@/stores/generationStore'
import { ImageResultCard } from './ImageResultCard'
import { TextResultCard } from './TextResultCard'
import { VideoResultCard } from './VideoResultCard'

export function ResultPanel() {
  const store = useGenerationStore()
  const { showToast } = useToast()
  const favorites = useGenerationStore((s) => s.favorites)
  const favoriteSet = useMemo(() => new Set(favorites), [favorites])

  const [previewImage, setPreviewImage] = useState('')
  const [previewVideo, setPreviewVideo] = useState('')
  const [previewVideoError, setPreviewVideoError] = useState(false)

  async function copyText(text: string) {
    await navigator.clipboard.writeText(text)
    showToast('文案已复制', 'success')
  }

  async function copyLink(url: string) {
    await navigator.clipboard.writeText(url)
    showToast('链接已复制', 'success')
  }

  function downloadImage(url: string) {
    const a = document.createElement('a')
    a.href = url
    a.download = `aigc-${Date.now()}.jpg`
    a.click()
    showToast('图片已开始下载', 'info')
  }

  async function downloadVideo(url: string) {
    showToast('正在准备视频下载', 'info')
    const filename = `aigc-${Date.now()}.mp4`
    const ok = await downloadVideoWithBrowser(url, filename)
    if (ok) {
      showToast('视频已开始下载', 'success')
      return
    }
    window.open(url, '_blank', 'noopener,noreferrer')
    showToast('浏览器已打开视频页面，请使用浏览器保存', 'info')
  }

  function openVideo(url: string) {
    window.open(url, '_blank', 'noopener,noreferrer')
  }

  function openVideoPreview(url: string) {
    setPreviewVideoError(false)
    setPreviewVideo(url)
  }

  async function downloadVideoWithBrowser(url: string, filename: string) {
    try {
      const res = await fetch(url)
      if (!res.ok) return false
      const blob = await res.blob()
      if (!blob.size) return false
      const objectUrl = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = objectUrl
      a.download = filename
      a.click()
      setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
      return true
    } catch {
      return false
    }
  }

  function toggleFavorite() {
    const t = store.currentTask
    if (!t) return
    const was = favoriteSet.has(t.taskId)
    store.toggleFavorite(t.taskId)
    showToast(was ? '已取消收藏' : '已加入收藏', 'success')
  }

  const task = store.currentTask

  return (
    <>
      <section className="result panel glass">
        <header className="head">
          <h3>结果展示</h3>
          {task ? <p className="muted">任务：{task.taskId}</p> : null}
        </header>

        {store.loading ? (
          <>
            <LoadingSpinner />
            <div className="skeletons">
              <SkeletonCard />
              <SkeletonCard />
            </div>
          </>
        ) : !task ? (
          <EmptyState title="还没有生成内容" description="在左侧输入提示词并点击“开始生成”，结果会展示在这里。" />
        ) : (
          <>
            {task.textResults.length > 0 ? (
              <section className="group">
                <p className="group-title">文案结果</p>
                <div className="list">
                  {task.textResults.map((text, idx) => (
                    <TextResultCard
                      key={`${task.taskId}-text-${idx}`}
                      text={text}
                      favorite={favoriteSet.has(task.taskId)}
                      onCopy={() => void copyText(text)}
                      onFavorite={toggleFavorite}
                    />
                  ))}
                </div>
              </section>
            ) : null}

            {task.imageResults.length > 0 ? (
              <section className="group">
                <p className="group-title">图片结果</p>
                <div className="gallery">
                  {task.imageResults.map((url) => {
                    const src = resolveApiMediaUrl(url)
                    return (
                      <ImageResultCard key={url} url={src} onPreview={() => setPreviewImage(src)} onDownload={() => downloadImage(src)} />
                    )
                  })}
                </div>
              </section>
            ) : null}

            {task.videoResults.length > 0 ? (
              <section className="group">
                <p className="group-title">视频结果</p>
                <div className="list">
                  {task.videoResults.map((url) => {
                    const src = resolveApiMediaUrl(url)
                    return (
                      <VideoResultCard
                        key={url}
                        url={src}
                        onPreview={() => openVideoPreview(src)}
                        onDownload={() => void downloadVideo(src)}
                        onCopyLink={() => void copyLink(src)}
                        onOpen={() => openVideo(src)}
                      />
                    )
                  })}
                </div>
              </section>
            ) : null}
          </>
        )}
      </section>

      {previewImage
        ? createPortal(
            <div className="lightbox" role="presentation" onClick={(e) => e.target === e.currentTarget && setPreviewImage('')}>
              <figure className="lightbox-card">
                <img src={previewImage} alt="预览大图" />
                <figcaption>
                  <AppButton size="sm" onClick={() => downloadImage(previewImage)}>
                    下载
                  </AppButton>
                  <AppButton size="sm" onClick={() => setPreviewImage('')}>
                    关闭
                  </AppButton>
                </figcaption>
              </figure>
            </div>,
            document.body,
          )
        : null}

      {previewVideo
        ? createPortal(
            <div className="lightbox" role="presentation" onClick={(e) => e.target === e.currentTarget && (setPreviewVideo(''), setPreviewVideoError(false))}>
              <section className="lightbox-card video-lightbox">
                {!previewVideoError ? (
                  <video
                    src={previewVideo}
                    controls
                    autoPlay
                    preload="metadata"
                    onError={() => setPreviewVideoError(true)}
                  />
                ) : (
                  <p className="video-error">视频预览加载失败，请尝试“新窗口播放”或重新生成。</p>
                )}
                <div className="actions">
                  <AppButton size="sm" onClick={() => void downloadVideo(previewVideo)}>
                    下载
                  </AppButton>
                  <AppButton size="sm" onClick={() => openVideo(previewVideo)}>
                    新窗口播放
                  </AppButton>
                  <AppButton
                    size="sm"
                    onClick={() => {
                      setPreviewVideo('')
                      setPreviewVideoError(false)
                    }}
                  >
                    关闭
                  </AppButton>
                </div>
              </section>
            </div>,
            document.body,
          )
        : null}
    </>
  )
}
