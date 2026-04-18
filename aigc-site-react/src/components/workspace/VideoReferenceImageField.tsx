import { useCallback, useId, useRef, useState } from 'react'

const MAX_READ_BYTES = 20 * 1024 * 1024
const COMPRESS_IF_LARGER_THAN = 2.5 * 1024 * 1024

type Props = {
  value: string
  onChange: (v: string) => void
  label: string
  placeholder: string
  /** 当前为 Moark 等仅适合 http(s) 的模型时展示提示 */
  showHttpOnlyHint?: boolean
}

function isDataImageUrl(s: string): boolean {
  const t = s.trim().toLowerCase()
  return t.startsWith('data:image/') && t.includes('base64')
}

function isHttpImageUrl(s: string): boolean {
  const t = s.trim()
  return t.startsWith('http://') || t.startsWith('https://')
}

/** 大图压成 JPEG，避免请求体过大 */
async function fileToCompressedDataUrl(file: File): Promise<string> {
  if (file.size <= COMPRESS_IF_LARGER_THAN && file.type !== 'image/svg+xml') {
    return readFileAsDataUrl(file)
  }
  return new Promise((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(url)
      try {
        const maxEdge = 2048
        let { width, height } = img
        if (width > maxEdge || height > maxEdge) {
          const r = Math.min(maxEdge / width, maxEdge / height)
          width = Math.round(width * r)
          height = Math.round(height * r)
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          resolve(readFileAsDataUrl(file))
          return
        }
        ctx.drawImage(img, 0, 0, width, height)
        const data = canvas.toDataURL('image/jpeg', 0.88)
        resolve(data)
      } catch {
        resolve(readFileAsDataUrl(file))
      }
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片无法读取'))
    }
    img.src = url
  })
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const r = new FileReader()
    r.onload = () => resolve(String(r.result || ''))
    r.onerror = () => reject(new Error('读取文件失败'))
    r.readAsDataURL(file)
  })
}

export function VideoReferenceImageField({ value, onChange, label, placeholder, showHttpOnlyHint }: Props) {
  const inputId = useId()
  const fileRef = useRef<HTMLInputElement>(null)
  const [dragOver, setDragOver] = useState(false)
  const [busy, setBusy] = useState(false)
  const [localErr, setLocalErr] = useState('')

  const trimmed = value.trim()
  const isData = isDataImageUrl(trimmed)
  const previewSrc = isData || isHttpImageUrl(trimmed) ? trimmed : ''

  const ingestFile = useCallback(
    async (file: File | undefined) => {
      if (!file || !file.type.startsWith('image/')) {
        setLocalErr('请选择图片文件（image/*）')
        return
      }
      if (file.size > MAX_READ_BYTES) {
        setLocalErr(`图片过大（>${Math.round(MAX_READ_BYTES / 1024 / 1024)}MB），请压缩后重试`)
        return
      }
      setLocalErr('')
      setBusy(true)
      try {
        const dataUrl = await fileToCompressedDataUrl(file)
        onChange(dataUrl)
      } catch (e) {
        setLocalErr(e instanceof Error ? e.message : '处理图片失败')
      } finally {
        setBusy(false)
      }
    },
    [onChange],
  )

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setDragOver(false)
    const f = e.dataTransfer.files?.[0]
    void ingestFile(f)
  }

  const onPick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0]
    void ingestFile(f)
    e.target.value = ''
  }

  return (
    <div className="video-ref-field field">
      <p className="label">{label}</p>
      {showHttpOnlyHint ? (
        <p className="hint muted video-ref-field__hint">
          Moark 等接口通常需要<strong>公网可访问的 http(s) 图片链接</strong>。本地上传会转为 Base64，更适合 Vidu；若用 Moark 请将图片传到图床后粘贴 URL。
        </p>
      ) : null}

      <div
        className={`video-ref-dropzone${dragOver ? ' video-ref-dropzone--active' : ''}${busy ? ' video-ref-dropzone--busy' : ''}`}
        onDragEnter={(e) => {
          e.preventDefault()
          e.stopPropagation()
          setDragOver(true)
        }}
        onDragOver={(e) => {
          e.preventDefault()
          e.stopPropagation()
          setDragOver(true)
        }}
        onDragLeave={(e) => {
          e.preventDefault()
          e.stopPropagation()
          if (e.currentTarget.contains(e.relatedTarget as Node)) return
          setDragOver(false)
        }}
        onDrop={onDrop}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault()
            fileRef.current?.click()
          }
        }}
        role="button"
        tabIndex={0}
        aria-label="拖拽或点击上传参考图"
        onClick={() => !busy && fileRef.current?.click()}
      >
        <input
          ref={fileRef}
          id={inputId}
          type="file"
          accept="image/*"
          className="video-ref-file-input"
          onChange={onPick}
          aria-hidden
          tabIndex={-1}
        />
        <div className="video-ref-dropzone__inner">
          {busy ? (
            <span className="muted">正在处理图片…</span>
          ) : (
            <>
              <span className="video-ref-dropzone__title">拖拽图片到此处，或点击选择</span>
              <span className="muted video-ref-dropzone__sub">支持 JPG / PNG / WebP / GIF；大文件会自动压缩为 JPEG</span>
            </>
          )}
        </div>
      </div>

      {previewSrc ? (
        <div className="video-ref-preview">
          <img src={previewSrc} alt="参考图预览" className="video-ref-preview__img" />
          <div className="video-ref-preview__meta">
            <span className="muted">{isData ? '已选择本地图片（Base64）' : '来自链接预览'}</span>
            <button type="button" className="video-ref-clear" onClick={() => onChange('')}>
              清除参考图
            </button>
          </div>
        </div>
      ) : null}

      <label className="video-ref-manual-label" htmlFor={`${inputId}-manual`}>
        或手动填写 / 粘贴
      </label>
      <textarea
        id={`${inputId}-manual`}
        className="video-ref-textarea"
        value={value}
        onChange={(e) => {
          setLocalErr('')
          onChange(e.target.value)
        }}
        placeholder={placeholder}
        rows={isData && trimmed.length > 800 ? 6 : 3}
        spellCheck={false}
      />

      {localErr ? <p className="error video-ref-field__err">{localErr}</p> : null}
    </div>
  )
}
