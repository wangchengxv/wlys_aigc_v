import { useMemo } from 'react'
import type { PipelineStatus } from '@/types'

type Props = {
  pipeline: PipelineStatus | null
}

export function PipelineProgressBar({ pipeline }: Props) {
  const percent = useMemo(() => {
    if (!pipeline || pipeline.totalCount === 0) return 0
    return Math.round(((pipeline.successCount + pipeline.failedCount) / pipeline.totalCount) * 100)
  }, [pipeline])

  return (
    <div className="pipeline panel glass">
      <div className="top">
        <div>
          <p className="label">流水线状态</p>
          <h3>{pipeline?.projectStatus || '未开始'}</h3>
        </div>
        <strong>{percent}%</strong>
      </div>
      <div className="track">
        <span style={{ width: `${percent}%` }} />
      </div>
      <div className="stats muted">
        <span>总任务 {pipeline?.totalCount ?? 0}</span>
        <span>成功 {pipeline?.successCount ?? 0}</span>
        <span>失败 {pipeline?.failedCount ?? 0}</span>
        <span>运行中 {pipeline?.runningCount ?? 0}</span>
        <span>排队 {pipeline?.queuedCount ?? 0}</span>
      </div>
    </div>
  )
}
