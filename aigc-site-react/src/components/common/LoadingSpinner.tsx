export function LoadingSpinner() {
  return (
    <div className="spinner-wrap" aria-label="加载中">
      <span className="ring" />
      <span className="text">生成中，请稍候...</span>
    </div>
  )
}
