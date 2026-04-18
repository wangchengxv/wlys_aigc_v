import { useEffect, useRef, useState, type FormEvent } from 'react'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { useAuthStore } from '@/stores/authStore'

const presets = [
  { label: '管理员', username: 'admin', password: 'Admin@123', desc: '查看组织治理、资源配置与系统级能力' },
  { label: '教师', username: 'teacher', password: 'Teacher@123', desc: '管理课程实训、作业批改与项目审核' },
  { label: '学生', username: 'student', password: 'Student@123', desc: '参与课程任务、创作项目与作品提交' },
]

type Props = {
  visible: boolean
  onClose: () => void
  onSuccess?: () => void
}

export function LoginModal({ visible, onClose, onSuccess }: Props) {
  const dialogRef = useRef<HTMLElement | null>(null)
  const loggingIn = useAuthStore((s) => s.loggingIn)
  const signIn = useAuthStore((s) => s.signIn)
  const [username, setUsername] = useState('teacher')
  const [password, setPassword] = useState('Teacher@123')
  const [submitError, setSubmitError] = useState<string | null>(null)

  useEffect(() => {
    if (!visible) return

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    const frameId = window.requestAnimationFrame(() => {
      dialogRef.current?.querySelector<HTMLInputElement>('input')?.focus()
    })

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !loggingIn) {
        onClose()
      }
    }

    window.addEventListener('keydown', onKeyDown)
    setSubmitError(null)

    return () => {
      window.cancelAnimationFrame(frameId)
      window.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [loggingIn, onClose, visible])

  if (!visible) return null

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    try {
      await signIn(username.trim(), password)
      setSubmitError(null)
      onSuccess?.()
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : '登录失败，请稍后重试')
    }
  }

  return (
    <div
      className="dialog-overlay login-modal-overlay"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget && !loggingIn) {
          onClose()
        }
      }}
    >
      <section
        ref={dialogRef}
        className="dialog glass modal-form-dialog wide login-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-modal-title"
      >
        <div className="dialog-title-row login-modal__header">
          <div>
            <p className="page-eyebrow login-modal__eyebrow">Sign In</p>
            <h2 id="login-modal-title" className="dialog-title">
              登录高校 AIGC 实训平台
            </h2>
          </div>
          <button type="button" className="dialog-close-btn" aria-label="关闭登录弹窗" disabled={loggingIn} onClick={onClose}>
            ×
          </button>
        </div>

        <p className="dialog-message login-modal__message">
          使用账号密码完成登录，认证成功后会立即更新当前页面顶部栏身份信息，不打断你正在浏览的页面上下文。
        </p>

        <form className="form login-modal__form" onSubmit={onSubmit}>
          <AppInput
            label="用户名"
            value={username}
            onChange={(value) => {
              setUsername(String(value))
              setSubmitError(null)
            }}
            placeholder="例如：admin / teacher / student"
          />
          <AppInput
            label="密码"
            type="password"
            value={password}
            onChange={(value) => {
              setPassword(String(value))
              setSubmitError(null)
            }}
            placeholder="请输入密码"
          />
          {submitError ? <p className="hint-error login-modal__error">{submitError}</p> : null}
          <div className="form-actions login-modal__actions">
            <AppButton type="button" onClick={onClose} disabled={loggingIn}>
              取消
            </AppButton>
            <AppButton variant="primary" type="submit" loading={loggingIn}>
              登录平台
            </AppButton>
          </div>
        </form>

        <div className="login-modal__accounts">
          <div className="login-modal__accounts-head">
            <strong>联调账号</strong>
            <span>点击即可快速填充</span>
          </div>
          <div className="login-modal__account-grid">
            {presets.map((item) => (
              <button
                key={item.username}
                type="button"
                className="login-modal__account-card"
                onClick={() => {
                  setUsername(item.username)
                  setPassword(item.password)
                  setSubmitError(null)
                }}
              >
                <div className="login-modal__account-title">
                  <strong>{item.label}</strong>
                  <span>{item.username}</span>
                </div>
                <p>{item.desc}</p>
              </button>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}
