import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { QuickActionGrid } from '@/components/common/QuickActionGrid'
import { StatStrip } from '@/components/common/StatStrip'
import { getApiBaseUrl, getSocialAuthUrl, getSocialLinks, healthCheck, unbindSocialLink } from '@/api'
import { useAuthStore } from '@/stores/authStore'
import type { SocialLinkItem } from '@/types'

export function SettingsPage() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const signOut = useAuthStore((s) => s.signOut)
  const [status, setStatus] = useState<'loading' | 'ok' | 'fail'>('loading')
  const [mode, setMode] = useState('unknown')
  const [socialLinks, setSocialLinks] = useState<SocialLinkItem[]>([])
  const [socialLoading, setSocialLoading] = useState(false)
  const [socialError, setSocialError] = useState<string | null>(null)
  const [unbindLoading, setUnbindLoading] = useState(false)
  const envMode = import.meta.env.MODE
  const apiBaseUrl = getApiBaseUrl()
  const isAdmin = user?.role === 'ADMIN'
  const canAccessGlobalSettings = user?.role === 'ADMIN' || user?.role === 'TEACHER' || user?.role === 'STUDENT'
  const systemEntries = [
    ...(isAdmin ? [{ key: 'models', title: '模型配置', description: '查看模型列表与联调状态', to: '/models', badge: '管理员' }] : []),
    ...(isAdmin ? [{ key: 'hub', title: '服务商中心', description: '维护多服务商接入', to: '/models/hub', badge: '管理员' }] : []),
    ...(canAccessGlobalSettings
      ? [{ key: 'global', title: '全局设定', description: '维护创作默认参数', to: '/global-settings', badge: '三角色可用' }]
      : []),
  ]

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

  useEffect(() => {
    if (!user) {
      setSocialLinks([])
      setSocialError(null)
      return
    }
    setSocialLoading(true)
    setSocialError(null)
    void (async () => {
      try {
        const links = await getSocialLinks()
        setSocialLinks(links)
      } catch (error) {
        setSocialError(error instanceof Error ? error.message : '获取第三方账号绑定失败')
      } finally {
        setSocialLoading(false)
      }
    })()
  }, [user])

  async function handleBindOnelinkai() {
    setSocialError(null)
    try {
      const payload = await getSocialAuthUrl('onelinkai')
      window.location.href = payload.authUrl
    } catch (error) {
      setSocialError(error instanceof Error ? error.message : '发起 OneLinkAI 绑定失败')
    }
  }

  async function handleUnbindOnelinkai() {
    setUnbindLoading(true)
    setSocialError(null)
    try {
      await unbindSocialLink('onelinkai')
      const links = await getSocialLinks()
      setSocialLinks(links)
    } catch (error) {
      setSocialError(error instanceof Error ? error.message : '解绑 OneLinkAI 失败')
    } finally {
      setUnbindLoading(false)
    }
  }

  const onelinkBound = socialLinks.some((item) => item.provider.toLowerCase() === 'onelinkai')
  const onelinkLink = socialLinks.find((item) => item.provider.toLowerCase() === 'onelinkai') || null

  return (
    <section className="settings-page settings-page--revamp">
      <StatStrip
        items={[
          { key: 'role', label: '当前角色', value: user?.role || '未登录' },
          { key: 'service', label: '服务状态', value: status === 'loading' ? '检测中' : status === 'ok' ? '正常' : '异常' },
          { key: 'mode', label: '运行模式', value: mode === 'unknown' ? envMode : mode.toUpperCase() },
        ]}
      />

      <div className="settings-page__focus-zone">
        <div className="content-card-grid">
          <section className="content-card">
            <div className="section-heading">
              <h3>账号与访问</h3>
              <span>只保留常用操作</span>
            </div>
            {user ? (
              <div className="settings-page__list">
                <div><span>显示名称</span><strong>{user.displayName}</strong></div>
                <div><span>用户名</span><strong>{user.username}</strong></div>
                <div><span>组织</span><strong>{user.orgUnitId || '未设置'}</strong></div>
                <div><span>班级</span><strong>{user.classroomId || '未设置'}</strong></div>
              </div>
            ) : (
              <p className="muted">当前为访客模式，登录后可查看角色相关后台入口。</p>
            )}
            <div className="inline-actions">
              <button type="button" className="app-btn v-ghost s-md" onClick={() => navigate('/login')}>
                {user ? '切换账号' : '去登录'}
              </button>
              {user ? (
                <button
                  type="button"
                  className="app-btn v-ghost s-md"
                  onClick={() => {
                    void signOut().then(() => navigate('/login'))
                  }}
                >
                  退出登录
                </button>
              ) : null}
            </div>
          </section>

          <section className="content-card">
            <div className="section-heading">
              <h3>系统入口</h3>
              <span>按角色收起复杂配置</span>
            </div>
            <QuickActionGrid
              items={systemEntries}
            />
            {!canAccessGlobalSettings ? <p className="muted">登录后可访问全局设定。</p> : null}
          </section>
        </div>

        <section className="content-card">
          <div className="section-heading">
            <h3>环境信息</h3>
            <span>轻量展示，便于联调</span>
          </div>
          <div className="settings-page__list">
            <div><span>前端模式</span><strong>{envMode}</strong></div>
            <div><span>接口地址</span><strong>{apiBaseUrl}</strong></div>
            <div><span>主题策略</span><strong>浅色工作台</strong></div>
          </div>
        </section>

        <section className="content-card">
          <div className="section-heading">
            <h3>第三方账号绑定</h3>
            <span>支持 OneLinkAI 统一账号体系</span>
          </div>
          {!user ? <p className="muted">请先登录，再管理第三方账号绑定。</p> : null}
          {user ? (
            <div className="settings-page__list">
              <div>
                <span>OneLinkAI</span>
                <strong>{socialLoading ? '加载中...' : onelinkBound ? '已绑定' : '未绑定'}</strong>
              </div>
              <div>
                <span>绑定账号</span>
                <strong>{onelinkLink?.providerUserId || '-'}</strong>
              </div>
            </div>
          ) : null}
          {socialError ? <p className="hint-error">{socialError}</p> : null}
          {user ? (
            <div className="inline-actions">
              {!onelinkBound ? (
                <button type="button" className="app-btn v-ghost s-md" onClick={handleBindOnelinkai}>
                  绑定 OneLinkAI
                </button>
              ) : (
                <button type="button" className="app-btn v-ghost s-md" disabled={unbindLoading} onClick={handleUnbindOnelinkai}>
                  {unbindLoading ? '解绑中...' : '解绑 OneLinkAI'}
                </button>
              )}
            </div>
          ) : null}
        </section>
      </div>

    </section>
  )
}
