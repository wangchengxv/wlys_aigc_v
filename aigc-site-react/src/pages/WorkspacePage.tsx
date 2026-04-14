import { useMemo } from 'react'
import { useLocation } from 'react-router-dom'
import { PromptPanel } from '@/components/workspace/PromptPanel'
import { ResultPanel } from '@/components/workspace/ResultPanel'
import { useToast } from '@/context/ToastContext'
import type { GenerateMode } from '@/types'

function defaultModeFromPath(pathname: string): GenerateMode | undefined {
  if (pathname.includes('/tools/image') && !pathname.includes('/tools/image-to-video')) return 'image'
  if (pathname.includes('/tools/video') || pathname.includes('/tools/image-to-video')) return 'video'
  // 与「小工具」子路由区分：回到综合创作页时恢复默认「图文」模式
  if (pathname === '/workspace') return 'both'
  return undefined
}

export function WorkspacePage() {
  const { showToast } = useToast()
  const { pathname } = useLocation()
  const defaultMode = useMemo(() => defaultModeFromPath(pathname), [pathname])
  return (
    <section className="workspace-grid">
      <PromptPanel defaultMode={defaultMode} onGenerated={() => showToast('生成成功', 'success')} />
      <ResultPanel />
    </section>
  )
}
