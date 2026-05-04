import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'

export interface Step1SessionProps {
  projectId: string
  setProjectId: (v: string) => void
  title: string
  setTitle: (v: string) => void
  onCreateSession: () => Promise<void>
  busy: boolean
  hasSession: boolean
  onNext: () => void
}

export function Step1Session({
  projectId,
  setProjectId,
  title,
  setTitle,
  onCreateSession,
  busy,
  hasSession,
  onNext,
}: Step1SessionProps) {
  return (
    <div className="sl-step-content">
      <div className="sl-card">
        <h2 className="sl-card-title">第一步：创建会话</h2>
        <p className="sl-card-desc">
          输入关联项目ID（可选）或自定义标题来启动独立的剧本闭环会话。
        </p>
        <div className="sl-form-row">
          <AppInput
            label="关联项目ID（可选）"
            value={projectId}
            onChange={(v) => setProjectId(String(v))}
            placeholder="可不填，按用户归属"
            disabled={hasSession}
          />
          <AppInput
            label="会话标题（可选）"
            value={title}
            onChange={(v) => setTitle(String(v))}
            placeholder="剧本闭环测试"
            disabled={hasSession}
          />
        </div>

        <div className="sl-actions sl-actions-between">
          <div />
          {hasSession ? (
            <AppButton variant="primary" onClick={onNext}>
              进入下一步
            </AppButton>
          ) : (
            <AppButton loading={busy} variant="primary" onClick={() => void onCreateSession()}>
              创建独立会话
            </AppButton>
          )}
        </div>
      </div>
    </div>
  )
}
