import { PromptPanel } from '@/components/workspace/PromptPanel'
import { ResultPanel } from '@/components/workspace/ResultPanel'
import { useToast } from '@/context/ToastContext'

export function WorkspacePage() {
  const { showToast } = useToast()
  return (
    <section className="workspace-grid">
      <PromptPanel onGenerated={() => showToast('生成成功', 'success')} />
      <ResultPanel />
    </section>
  )
}
