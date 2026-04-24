import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { getSocialAuthUrl } from '@/api'
import { useAuthStore } from '@/stores/authStore'
import '@/styles/welcome-page.css'

const presets = [
  { label: '管理员', username: 'admin', password: 'Admin@123', icon: '👨‍💼' },
  { label: '教师', username: 'teacher', password: 'Teacher@123', icon: '👩‍🏫' },
  { label: '学生', username: 'student', password: 'Student@123', icon: '🎓' },
]

const features = [
  { icon: '🎬', title: '剧本创作', desc: 'AI 辅助剧本生成与优化' },
  { icon: '🎨', title: '视觉资产', desc: '文生图与三视图工具' },
  { icon: '🎥', title: '视频生成', desc: '图生视频与视频合成' },
  { icon: '📚', title: '课程实训', desc: '教学管理一体化' },
]

export function WelcomePage() {
  const navigate = useNavigate()
  const loggingIn = useAuthStore((s) => s.loggingIn)
  const signIn = useAuthStore((s) => s.signIn)
  const [username, setUsername] = useState('teacher')
  const [password, setPassword] = useState('Teacher@123')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [socialLoading, setSocialLoading] = useState(false)

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      await signIn(username.trim(), password)
      navigate('/workspace')
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : '登录失败，请稍后重试')
    }
  }

  async function handleOnelinkLogin() {
    setSubmitError(null)
    setSocialLoading(true)
    try {
      const payload = await getSocialAuthUrl('onelinkai')
      window.location.href = payload.authUrl
    } catch (error) {
      setSocialLoading(false)
      setSubmitError(error instanceof Error ? error.message : '发起 OneLinkAI 登录失败')
    }
  }

  function fillPreset(user: string, pass: string) {
    setUsername(user)
    setPassword(pass)
    setSubmitError(null)
  }

  return (
    <div className="welcome-page">
      <div className="welcome-bg">
        <div className="welcome-glow welcome-glow--1" />
        <div className="welcome-glow welcome-glow--2" />
        <div className="welcome-grid" />
      </div>

      <div className="welcome-container">
        <div className="welcome-panel">
          <div className="welcome-header">
            <div className="welcome-logo">
              <span className="welcome-logo__icon">✦</span>
            </div>
            <h1 className="welcome-title">高校 AIGC 实训平台</h1>
            <p className="welcome-subtitle">探索 AI 创作，开启智能影像新篇章</p>
          </div>

          <form className="welcome-form" onSubmit={onSubmit}>
            <AppInput
              label="用户名"
              value={username}
              onChange={(v) => {
                setUsername(String(v))
                setSubmitError(null)
              }}
              placeholder="输入用户名"
            />
            <AppInput
              label="密码"
              type="password"
              value={password}
              onChange={(v) => {
                setPassword(String(v))
                setSubmitError(null)
              }}
              placeholder="输入密码"
            />
            {submitError && <p className="welcome-error">{submitError}</p>}
            <AppButton variant="primary" block type="submit" loading={loggingIn}>
              登录平台
            </AppButton>
          </form>

          <div className="welcome-divider">
            <span>快捷登录</span>
          </div>

          <div className="welcome-presets">
            {presets.map((item) => (
              <button
                key={item.username}
                type="button"
                className="welcome-preset"
                onClick={() => fillPreset(item.username, item.password)}
              >
                <span className="welcome-preset__icon">{item.icon}</span>
                <span className="welcome-preset__label">{item.label}</span>
              </button>
            ))}
          </div>

          <div className="welcome-social">
            <AppButton
              variant="ghost"
              block
              loading={socialLoading}
              disabled={loggingIn || socialLoading}
              onClick={handleOnelinkLogin}
            >
              <span className="welcome-social__icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
                </svg>
              </span>
              使用 OneLinkAI 登录
            </AppButton>
          </div>
        </div>

        <div className="welcome-features">
          {features.map((item, index) => (
            <div key={item.title} className="welcome-feature" style={{ animationDelay: `${index * 0.1}s` }}>
              <span className="welcome-feature__icon">{item.icon}</span>
              <div className="welcome-feature__text">
                <strong>{item.title}</strong>
                <span>{item.desc}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      <footer className="welcome-footer">
        <p>AIGC 高校智能实训平台 · 创新影像创作</p>
      </footer>
    </div>
  )
}