import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { EmptyState } from '@/components/common/EmptyState'
import { HistoryCard } from '@/components/history/HistoryCard'
import { HistoryFilter } from '@/components/history/HistoryFilter'
import { Pagination } from '@/components/history/Pagination'
import { useToast } from '@/context/ToastContext'
import { useGenerationStore } from '@/stores/generationStore'
import type { GenerateMode } from '@/types'

export function HistoryPage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const tasks = useGenerationStore((s) => s.tasks)
  const loadHistory = useGenerationStore((s) => s.loadHistory)
  const setCurrentTask = useGenerationStore((s) => s.setCurrentTask)
  const removeTaskById = useGenerationStore((s) => s.removeTask)

  const [page, setPage] = useState(1)
  const pageSize = 6
  const [total, setTotal] = useState(0)
  const [mode, setMode] = useState<GenerateMode | 'all'>('all')

  const fetchHistory = useCallback(async () => {
    try {
      const data = await loadHistory(page, pageSize, mode)
      setTotal(data.total)
    } catch (error) {
      showToast(error instanceof Error ? error.message : '加载历史记录失败', 'error')
      setTotal(0)
    }
  }, [loadHistory, mode, page, showToast])

  const totalPages = Math.max(1, Math.ceil(total / pageSize))

  useEffect(() => {
    void fetchHistory()
  }, [fetchHistory])

  async function changePage(next: number) {
    setPage(Math.min(totalPages, Math.max(1, next)))
  }

  async function applyFilter(nextMode: GenerateMode | 'all') {
    setMode(nextMode)
    setPage(1)
  }

  function openInWorkspace(taskId: string) {
    const task = tasks.find((item) => item.taskId === taskId)
    if (task) {
      setCurrentTask(task)
      navigate('/workspace')
    }
  }

  async function remove(taskId: string) {
    await removeTaskById(taskId)
    showToast('记录已删除', 'success')
    const remaining = useGenerationStore.getState().tasks
    if (!remaining.length && page > 1) {
      setPage((p) => p - 1)
    }
    await fetchHistory()
  }

  return (
    <section className="panel glass history-page">
      <div className="history-topbar">
        <HistoryFilter mode={mode} onChange={(m) => void applyFilter(m)} />
      </div>

      {!tasks.length ? (
        <EmptyState title="暂无历史记录" description="去工作台生成内容后，这里会自动保存你的结果。" />
      ) : (
        <div className="history-list">
          {tasks.map((task) => (
            <HistoryCard key={task.taskId} task={task} onOpen={() => openInWorkspace(task.taskId)} onRemove={() => void remove(task.taskId)} />
          ))}
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={(p) => void changePage(p)} />
    </section>
  )
}
