import { useEffect, useMemo, useState } from 'react'
import { AppButton } from '@/components/common/AppButton'
import { getReversePromptModels, generateReversePrompt } from '@/api'
import { useToast } from '@/context/ToastContext'
import { buildReversePromptMarkdown, pickReversePromptDefaultModel } from '@/lib/workspace/reversePrompt'
import type { ReversePromptResponse } from '@/types'
import { VideoReferenceImageField } from './VideoReferenceImageField'

export function ReversePromptPanel() {
  const { showToast } = useToast()
  const [imageInput, setImageInput] = useState('')
  const [models, setModels] = useState<string[]>([])
  const [selectedModel, setSelectedModel] = useState('')
  const [loadingModels, setLoadingModels] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<ReversePromptResponse | null>(null)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setLoadingModels(true)
      try {
        const payload = await getReversePromptModels()
        if (cancelled) return
        const options = Array.isArray(payload.options) ? payload.options : []
        setModels(options)
        setSelectedModel(pickReversePromptDefaultModel(options, payload.defaultModel))
      } catch (e) {
        if (cancelled) return
        setError(e instanceof Error ? e.message : '加载模型失败')
      } finally {
        if (!cancelled) {
          setLoadingModels(false)
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const hasModel = useMemo(() => selectedModel.trim().length > 0, [selectedModel])
  const resultMarkdown = useMemo(() => {
    if (!result) return ''
    return buildReversePromptMarkdown(result)
  }, [result])

  async function runReversePrompt() {
    setError('')
    if (!imageInput.trim()) {
      setError('请先上传图片或填写图片 URL')
      return
    }
    if (!hasModel) {
      setError('暂无可用豆包模型，请先到模型配置里启用')
      return
    }
    setSubmitting(true)
    try {
      const data = await generateReversePrompt({
        image: imageInput.trim(),
        model: selectedModel.trim(),
      })
      setResult(data)
      showToast('反推完成', 'success')
    } catch (e) {
      setError(e instanceof Error ? e.message : '反推失败')
    } finally {
      setSubmitting(false)
    }
  }

  async function copy(value: string, label: string) {
    if (!value.trim()) return
    await navigator.clipboard.writeText(value)
    showToast(`${label}已复制`, 'success')
  }

  async function copyJson() {
    if (!result) return
    await navigator.clipboard.writeText(JSON.stringify(result, null, 2))
    showToast('JSON 已复制', 'success')
  }

  async function copyMarkdown() {
    if (!resultMarkdown) return
    await navigator.clipboard.writeText(resultMarkdown)
    showToast('Markdown 已复制', 'success')
  }

  return (
    <section className="reverse-prompt-panel panel glass">
      <div className="reverse-prompt-panel__header">
        <div className="reverse-prompt-panel__title-wrap">
          <h3 className="reverse-prompt-panel__title">反推提示词</h3>
          <p className="reverse-prompt-panel__subtitle">上传图片后，调用已配置豆包模型反推可编辑提示词</p>
        </div>
      </div>

      <div className="reverse-prompt-panel__content">
        <div className="reverse-prompt-panel__section">
          <div className="reverse-prompt-panel__field">
            <div className="reverse-prompt-panel__field-header">
              <span className="reverse-prompt-panel__label">豆包模型</span>
              <span className={`soft-badge ${loadingModels ? 'is-muted' : models.length > 0 ? 'is-success' : 'is-muted'}`}>
                {loadingModels ? '模型加载中' : models.length > 0 ? `可用 ${models.length} 个` : '暂无模型'}
              </span>
            </div>
            <div className="reverse-prompt-panel__input-row">
              <select
                className="ctrl reverse-prompt-panel__select"
                value={selectedModel}
                disabled={loadingModels || models.length === 0}
                onChange={(event) => setSelectedModel(event.target.value)}
              >
                {loadingModels ? <option value="">加载中...</option> : null}
                {models.length === 0 && !loadingModels ? <option value="">暂无可用模型</option> : null}
                {models.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
              <p className="reverse-prompt-panel__hint">模型由配置中心动态读取，仅显示可用豆包系列能力。</p>
            </div>
          </div>
        </div>

        <div className="reverse-prompt-panel__section">
          <VideoReferenceImageField
            value={imageInput}
            onChange={setImageInput}
            label="图片输入"
            placeholder="https://... 或 data:image/png;base64,..."
          />
        </div>

        {error ? (
          <div className="reverse-prompt-panel__error">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <p>{error}</p>
          </div>
        ) : null}

        <div className="reverse-prompt-panel__actions">
          <AppButton variant="primary" loading={submitting} onClick={() => void runReversePrompt()}>
            {submitting ? '反推中...' : '开始反推'}
          </AppButton>
          <AppButton
            variant="ghost"
            onClick={() => {
              setResult(null)
              setError('')
              setImageInput('')
            }}
            disabled={submitting}
          >
            清空
          </AppButton>
        </div>

        {result ? (
        <div className="reverse-prompt-panel__result">
          <div className="reverse-prompt-panel__result-head">
            <div className="reverse-prompt-panel__result-title">
              <strong>模型：{result.model || '-'}</strong>
              <span className="muted">可复制后直接粘贴到工作台或画布节点。</span>
            </div>
            <div className="reverse-prompt-panel__copy-actions">
              <AppButton size="sm" onClick={() => void copy(result.positivePrompt || '', '正向提示词')}>
                复制正向
              </AppButton>
              <AppButton size="sm" onClick={() => void copy(result.negativePrompt || '', '反向提示词')}>
                复制反向
              </AppButton>
              <AppButton size="sm" onClick={() => void copyJson()}>
                复制 JSON
              </AppButton>
              <AppButton size="sm" onClick={() => void copyMarkdown()}>
                复制 Markdown
              </AppButton>
            </div>
          </div>
          <div className="reverse-prompt-panel__grid">
            <article className="content-card">
              <h4>正向提示词</h4>
              <p>{result.positivePrompt || '-'}</p>
            </article>
            <article className="content-card">
              <h4>反向提示词</h4>
              <p>{result.negativePrompt || '-'}</p>
            </article>
          </div>
          <article className="content-card reverse-prompt-panel__meta">
            <h4>参数建议</h4>
            <ul>
              <li>风格：{result.style || '-'}</li>
              <li>光线：{result.lighting || '-'}</li>
              <li>构图：{result.composition || '-'}</li>
              <li>镜头：{result.camera || '-'}</li>
              <li>色彩：{result.colorTone || '-'}</li>
            </ul>
            <pre>{JSON.stringify(result.parameters || {}, null, 2)}</pre>
          </article>
        </div>
      ) : null}
      </div>
    </section>
  )
}
