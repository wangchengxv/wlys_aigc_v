import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { socialLoginCallback } from '@/api'
import { useAuthStore } from '@/stores/authStore'

export function SocialLoginCallbackPage() {
  const { provider = 'onelinkai' } = useParams<{ provider: string }>()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const refresh = useAuthStore((s) => s.refresh)
  const providerLabel = provider.toLowerCase() === 'wechat' ? '微信' : 'OneLinkAI'
  const [message, setMessage] = useState(`正在完成 ${providerLabel} 登录...`)

  useEffect(() => {
    let cancelled = false
    const code = searchParams.get('code') || ''
    const state = searchParams.get('state') || ''
    const error = searchParams.get('error') || ''

    async function run() {
      if (error) {
        setMessage(`${providerLabel} 登录失败：${error}`)
        return
      }
      if (!code || !state) {
        setMessage(`缺少 ${providerLabel} 回调参数，请重新发起登录`)
        return
      }
      try {
        await socialLoginCallback(provider, code, state)
        await refresh()
        if (!cancelled) {
          navigate('/', { replace: true })
        }
      } catch (e) {
        if (!cancelled) {
          setMessage(e instanceof Error ? e.message : `${providerLabel} 登录失败，请稍后重试`)
        }
      }
    }

    void run()
    return () => {
      cancelled = true
    }
  }, [navigate, provider, providerLabel, refresh, searchParams])

  return (
    <section className="page stack-lg">
      <div className="card">
        <h1>第三方登录</h1>
        <p>{message}</p>
      </div>
    </section>
  )
}
