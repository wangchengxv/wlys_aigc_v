import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getApiBaseUrl, healthCheck } from '@/api'
import { useThemeStore } from '@/stores/themeStore'

export function SettingsPage() {
  const navigate = useNavigate()
  const theme = useThemeStore((s) => s.theme)
  const setTheme = useThemeStore((s) => s.setTheme)

  const [status, setStatus] = useState<'loading' | 'ok' | 'fail'>('loading')
  const [mode, setMode] = useState('unknown')
  const envMode = import.meta.env.MODE
  const apiBaseUrl = getApiBaseUrl()

  useEffect(() => {
    void (async () => {
      try {
        const res = await healthCheck()
        setStatus(res.ok ? 'ok' : 'fail')
        setMode(res.mode)
      } catch {
        setStatus('fail')
      }
    })()
  }, [])

  return (
    <section className="panel glass settings-page">
      <div className="setting-item theme-block">
        <p>外观</p>
        <p className="hint muted">浅色 / 深色 / OneLink（青靛科技风，参考 onelinkai.cloud）</p>
        <div className="theme-pills" role="group" aria-label="主题选择">
          <button type="button" className={`pill${theme === 'light' ? ' active' : ''}`} onClick={() => setTheme('light')}>
            浅色
          </button>
          <button type="button" className={`pill${theme === 'dark' ? ' active' : ''}`} onClick={() => setTheme('dark')}>
            深色
          </button>
          <button type="button" className={`pill${theme === 'onelink' ? ' active' : ''}`} onClick={() => setTheme('onelink')}>
            OneLink
          </button>
        </div>
      </div>
      <div className="setting-item">
        <p>模型服务状态</p>
        <strong className={`health ${status}`}>
          <span className={`dot ${status}`} />
          {status === 'loading' ? '检测中...' : status === 'ok' ? `正常（${mode.toUpperCase()}）` : '异常，请检查后端服务'}
        </strong>
      </div>
      <div className="setting-item">
        <p>系统信息</p>
        <ul>
          <li>运行模式：{envMode}</li>
          <li>接口地址：{apiBaseUrl}</li>
          <li>前端版本：v1.0 (React)</li>
        </ul>
      </div>
      <div className="setting-item">
        <p>全局创作默认</p>
        <p className="hint muted">画面比例、剧本类型、模型策略、创作模式与分镜布局</p>
        <div className="actions-row">
          <button type="button" className="pill" onClick={() => navigate('/global-settings')}>
            打开全局设定
          </button>
        </div>
      </div>
      <div className="setting-item">
        <p>模型配置管理</p>
        <div className="actions-row">
          <button type="button" className="pill" onClick={() => navigate('/models')}>
            模型配置
          </button>
          <button type="button" className="pill" onClick={() => navigate('/models/hub')}>
            服务商中心
          </button>
        </div>
      </div>
      <div className="setting-item">
        <p>使用说明</p>
        <ul>
          <li>1. 输入主题提示词，选择生成模式与风格。</li>
          <li>2. 图文模式可选图片模型；视频模式可选即梦视频模型。</li>
          <li>3. 点击开始生成，视频任务可能需要 10~60 秒返回。</li>
          <li>4. 支持复制文案、下载图片/视频、复制视频链接、历史回看。</li>
        </ul>
      </div>
    </section>
  )
}
