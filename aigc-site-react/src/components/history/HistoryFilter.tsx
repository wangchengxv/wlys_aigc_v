import type { GenerateMode } from '@/types'

type Props = {
  mode: GenerateMode | 'all'
  onChange: (value: GenerateMode | 'all') => void
}

export function HistoryFilter({ mode, onChange }: Props) {
  return (
    <label className="history-filter filter">
      <span>模式筛选</span>
      <select value={mode} onChange={(e) => onChange(e.target.value as GenerateMode | 'all')}>
        <option value="all">全部模式</option>
        <option value="text">仅文本</option>
        <option value="image">仅图片</option>
        <option value="both">图文一起</option>
        <option value="video">仅视频</option>
      </select>
    </label>
  )
}
