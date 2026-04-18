import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PageBackLink } from '@/components/common/PageBackLink'
import { useToast } from '@/context/ToastContext'
import { getAuditLogs } from '@/api'
import { useAuthStore } from '@/stores/authStore'
import type { AuditLogRecord, UserRole } from '@/types'

function fmt(value?: string | null) {
  if (!value) return '未记录'
  const time = new Date(value)
  return Number.isNaN(time.getTime()) ? value : time.toLocaleString('zh-CN')
}

function summarize(detailsJson?: string | null) {
  if (!detailsJson) return '无附加详情'
  try {
    const parsed = JSON.parse(detailsJson) as Record<string, unknown>
    return Object.entries(parsed)
      .slice(0, 4)
      .map(([key, value]) => `${key}: ${String(value)}`)
      .join(' ｜ ')
  } catch {
    return detailsJson
  }
}

function canAccessAuditLogs(role?: UserRole | null) {
  return role === 'ADMIN'
}

export function AuditLogsPage() {
  const { showToast } = useToast()
  const user = useAuthStore((s) => s.user)
  const initialized = useAuthStore((s) => s.initialized)
  const authLoading = useAuthStore((s) => s.loading)
  const [searchParams, setSearchParams] = useSearchParams()
  const [logs, setLogs] = useState<AuditLogRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [entityType, setEntityType] = useState(searchParams.get('entityType') ?? '')
  const [entityId, setEntityId] = useState(searchParams.get('entityId') ?? '')
  const [actorUserId, setActorUserId] = useState(searchParams.get('actorUserId') ?? '')

  const pageTitle = useMemo(() => {
    if (entityType && entityId) {
      return `${entityType} / ${entityId} 的过程审计`
    }
    return '平台审计日志'
  }, [entityType, entityId])

  const loadLogs = useCallback(async (next?: { entityType?: string; entityId?: string; actorUserId?: string }) => {
    setLoading(true)
    try {
      const params = {
        entityType: (next?.entityType ?? entityType) || undefined,
        entityId: (next?.entityId ?? entityId) || undefined,
        actorUserId: (next?.actorUserId ?? actorUserId) || undefined,
      }
      setLogs(await getAuditLogs(params))
    } catch (error) {
      showToast(error instanceof Error ? error.message : '加载审计日志失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [actorUserId, entityId, entityType, showToast])

  useEffect(() => {
    if (!initialized || authLoading) {
      return
    }
    if (!canAccessAuditLogs(user?.role)) {
      setLoading(false)
      return
    }
    void loadLogs({
      entityType: searchParams.get('entityType') ?? '',
      entityId: searchParams.get('entityId') ?? '',
      actorUserId: searchParams.get('actorUserId') ?? '',
    })
  }, [authLoading, initialized, loadLogs, searchParams, user?.role])

  function applyFilters() {
    if (!canAccessAuditLogs(user?.role)) {
      return
    }
    const next = {
      entityType: entityType.trim(),
      entityId: entityId.trim(),
      actorUserId: actorUserId.trim(),
    }
    const normalized = Object.fromEntries(Object.entries(next).filter(([, value]) => value))
    setSearchParams(normalized)
    void loadLogs(next)
  }

  if (!initialized || authLoading) {
    return (
      <section className="teaching-page audit-logs-page">
        <div className="page-back-row">
          <PageBackLink to="/settings">返回设置</PageBackLink>
        </div>
        <LoadingSpinner />
      </section>
    )
  }

  if (!canAccessAuditLogs(user?.role)) {
    return (
      <section className="teaching-page audit-logs-page">
        <div className="page-back-row">
          <PageBackLink to="/settings">返回设置</PageBackLink>
        </div>
        <EmptyState
          title="仅管理员可访问"
          description="审计日志用于查看系统治理、审核留痕与关键操作轨迹，请使用管理员账号登录后再进入。"
        />
      </section>
    )
  }

  return (
    <section className="teaching-page audit-logs-page">
      <div className="page-back-row">
        <PageBackLink to="/settings">返回设置</PageBackLink>
      </div>

      <div className="teaching-page__hero panel glass">
        <div>
          <p className="eyebrow">Audit Trail</p>
          <h2>{pageTitle}</h2>
          <p className="muted">聚合课程、作业、提交、模板等关键动作，先把留痕查阅能力补齐，便于教学管理和问题追踪。</p>
        </div>
      </div>

      <div className="panel glass teaching-panel">
        <div className="teaching-panel__head">
          <div>
            <p className="eyebrow">Filter</p>
            <h3>筛选条件</h3>
          </div>
          <AppButton size="sm" variant="primary" onClick={applyFilters}>
            应用筛选
          </AppButton>
        </div>
        <div className="teaching-form__row">
          <AppInput label="实体类型" value={entityType} onChange={(value) => setEntityType(String(value))} placeholder="例如：COURSE / ASSIGNMENT" />
          <AppInput label="实体 ID" value={entityId} onChange={(value) => setEntityId(String(value))} placeholder="例如：course-xxx" />
          <AppInput label="操作人 ID" value={actorUserId} onChange={(value) => setActorUserId(String(value))} placeholder="例如：teacher-1" />
        </div>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : logs.length ? (
        <div className="teaching-stack">
          {logs.map((item) => (
            <article key={item.id} className="panel glass teaching-card audit-log-card">
              <div className="teaching-card__head">
                <div>
                  <p className="eyebrow">Audit</p>
                  <h3>{item.action}</h3>
                </div>
                <span className="pill small">{item.entityType}</span>
              </div>
              <div className="teaching-meta">
                <span>实体：{item.entityId}</span>
                <span>操作人：{item.actorUserName || item.actorUserId || '未知'}</span>
                <span>时间：{fmt(item.createdAt)}</span>
              </div>
              <p className="muted">{summarize(item.detailsJson)}</p>
            </article>
          ))}
        </div>
      ) : (
        <EmptyState title="没有查到审计记录" description="可以先按课程、作业或操作人过滤，也可以从课程页直接跳转到带筛选条件的审计视图。" />
      )}
    </section>
  )
}
