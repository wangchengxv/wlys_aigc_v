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
      <div className="section-heading">
        <h3>反推提示词</h3>
        <span>上传图片后，调用已配置豆包模型反推可编辑提示词</span>
      </div>

      <div className="reverse-prompt-panel__toolbar">
        <div className="reverse-prompt-panel__field">
          <span className="label">豆包模型</span>
          <select
            className="ctrl"
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
        </div>
        <div className="reverse-prompt-panel__status">
          <span className="soft-badge">{loadingModels ? '模型加载中' : models.length > 0 ? `可用 ${models.length} 个` : '暂无模型'}</span>
          <p className="muted">模型由配置中心动态读取，仅显示可用豆包系列能力。</p>
        </div>
      </div>

      <VideoReferenceImageField
        value={imageInput}
        onChange={setImageInput}
        label="图片输入（上传或 URL）"
        placeholder="https://... 或 data:image/png;base64,..."
      />

      {error ? <p className="error">{error}</p> : null}

      <div className="reverse-prompt-panel__actions">
        <AppButton variant="primary" loading={submitting} onClick={() => void runReversePrompt()}>
          {submitting ? '反推中...' : '开始反推'}
        </AppButton>
        <AppButton
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
    </section>
  )
}
