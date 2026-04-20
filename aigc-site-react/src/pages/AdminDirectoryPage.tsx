import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { Link } from 'react-router-dom'
import { ActionDrawer } from '@/components/common/ActionDrawer'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PageBackLink } from '@/components/common/PageBackLink'
import { useToast } from '@/context/ToastContext'
import {
  batchUpdateAdminUserLock,
  batchUpdateAdminUserStatus,
  createAdminUser,
  createOrgUnit,
  downloadAdminUserImportTemplate,
  exportAdminUsers,
  getAdminUserBatchStats,
  getAdminUserImportTask,
  getAdminUserImportTasks,
  getAdminUsersPaged,
  getAuditLogs,
  importAdminUsers,
  getOrgUnits,
  updateAdminUserStatus,
} from '@/api'
import { useAuthStore } from '@/stores/authStore'
import type {
  AdminUser,
  AdminUserBatchStatsResponse,
  AdminUserImportResult,
  AdminUserImportTask,
  AuditLogRecord,
  CurrentUser,
  OrgUnit,
  OrgUnitType,
  UserRole,
} from '@/types'

const ACCOUNT_PERMISSION = {
  MENU_ACCOUNT_DIRECTORY: 'menu:account:directory:view',
  API_ORG_UNIT_LIST: 'api:account:org-unit:list',
  API_ORG_UNIT_CREATE: 'api:account:org-unit:create',
  API_USER_LIST: 'api:account:user:list',
  API_USER_CREATE: 'api:account:user:create',
  API_USER_STATUS_UPDATE: 'api:account:user:status:update',
  API_USER_BATCH_STATUS: 'api:account:user:batch:status',
  API_USER_BATCH_LOCK: 'api:account:user:batch:lock',
  API_USER_IMPORT_TEMPLATE: 'api:account:user:import-template:download',
  API_USER_IMPORT: 'api:account:user:import',
  API_USER_IMPORT_TASK_QUERY: 'api:account:user:import-task:query',
  API_USER_EXPORT: 'api:account:user:export',
  API_USER_BATCH_STATS: 'api:account:user:batch:stats',
} as const

const ROLE_PERMISSION_FALLBACK: Record<UserRole, string[]> = {
  ADMIN: ['*'],
  TEACHER: [
    ACCOUNT_PERMISSION.MENU_ACCOUNT_DIRECTORY,
    ACCOUNT_PERMISSION.API_ORG_UNIT_LIST,
    ACCOUNT_PERMISSION.API_USER_LIST,
    ACCOUNT_PERMISSION.API_USER_IMPORT_TEMPLATE,
    ACCOUNT_PERMISSION.API_USER_IMPORT_TASK_QUERY,
    ACCOUNT_PERMISSION.API_USER_EXPORT,
    ACCOUNT_PERMISSION.API_USER_BATCH_STATS,
  ],
  STUDENT: [],
}

function resolvePermissionSet(user: CurrentUser | null) {
  if (!user?.role) return new Set<string>()
  const fromUser = Array.isArray(user.permissions) ? user.permissions.filter(Boolean) : []
  const source = fromUser.length ? fromUser : ROLE_PERMISSION_FALLBACK[user.role] ?? []
  return new Set(source)
}

function hasPermission(permissions: Set<string>, permission: string) {
  return permissions.has('*') || permissions.has(permission)
}

function fmt(value?: string | null) {
  if (!value) return '未记录'
  const time = new Date(value)
  return Number.isNaN(time.getTime()) ? value : time.toLocaleString('zh-CN')
}

function roleLabel(role: UserRole) {
  switch (role) {
    case 'ADMIN':
      return '平台管理员'
    case 'TEACHER':
      return '教师'
    case 'STUDENT':
      return '学生'
    default:
      return role
  }
}

function orgUnitTypeLabel(type: OrgUnitType) {
  return type === 'ORGANIZATION' ? '组织' : '班级'
}

function deriveMajor(user: AdminUser, organization?: OrgUnit, classroom?: OrgUnit) {
  const source = `${organization?.name ?? ''} ${classroom?.name ?? ''} ${user.role}`.toLowerCase()
  if (source.includes('影视')) return '影视制作'
  if (source.includes('动画')) return '动画设计'
  if (source.includes('设计')) return '视觉传达'
  if (source.includes('admin')) return '平台治理'
  if (user.role === 'ADMIN') return '平台治理'
  if (user.role === 'TEACHER') return '课程教学'
  return 'AIGC 内容创作'
}

function deriveIdentity(user: AdminUser, organization?: OrgUnit, classroom?: OrgUnit) {
  if (user.role === 'ADMIN') return '校级后台 / 平台治理'
  if (user.role === 'TEACHER') return `教师身份 / ${organization?.name || '未分配院系'}`
  return `学生身份 / ${classroom?.name || organization?.name || '待分班'}`
}

function summarizeAudit(detailsJson?: string | null) {
  if (!detailsJson) return '无附加详情'
  try {
    const parsed = JSON.parse(detailsJson) as Record<string, unknown>
    const summary = Object.entries(parsed)
      .slice(0, 4)
      .map(([key, value]) => `${key}: ${String(value)}`)
      .join(' ｜ ')
    return summary || '无附加详情'
  } catch {
    return detailsJson
  }
}

type DirectoryRow = {
  user: AdminUser
  organization?: OrgUnit
  classroom?: OrgUnit
  department: string
  major: string
  identity: string
}

const PAGE_SIZE_OPTIONS = [10, 20, 50]

export function AdminDirectoryPage() {
  const { showToast } = useToast()
  const currentUser = useAuthStore((s) => s.user)
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const permissionSet = useMemo(() => resolvePermissionSet(currentUser), [currentUser])
  const canListOrgUnits = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_ORG_UNIT_LIST)
  const canListUsers = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_USER_LIST)
  const canViewDirectory =
    hasPermission(permissionSet, ACCOUNT_PERMISSION.MENU_ACCOUNT_DIRECTORY) && canListOrgUnits && canListUsers
  const canCreateOrgUnit = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_ORG_UNIT_CREATE)
  const canCreateUser = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_USER_CREATE)
  const canUpdateUserStatus = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_USER_STATUS_UPDATE)
  const canBatchStatus = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_USER_BATCH_STATUS)
  const canBatchLock = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_USER_BATCH_LOCK)
  const canDownloadImportTemplate = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_USER_IMPORT_TEMPLATE)
  const canImportUsers = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_USER_IMPORT)
  const canExportUsers = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_USER_EXPORT)
  const canQueryImportTasks = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_USER_IMPORT_TASK_QUERY)
  const canQueryBatchStats = hasPermission(permissionSet, ACCOUNT_PERMISSION.API_USER_BATCH_STATS)
  const hasBatchActionPermission = canBatchStatus || canBatchLock
  const [loading, setLoading] = useState(true)
  const [savingUnit, setSavingUnit] = useState(false)
  const [savingUser, setSavingUser] = useState(false)
  const [downloadingTemplate, setDownloadingTemplate] = useState(false)
  const [importingUsers, setImportingUsers] = useState(false)
  const [exportingUsers, setExportingUsers] = useState(false)
  const [importTasksLoading, setImportTasksLoading] = useState(false)
  const [batchStatsLoading, setBatchStatsLoading] = useState(false)
  const [orgUnits, setOrgUnits] = useState<OrgUnit[]>([])
  const [users, setUsers] = useState<AdminUser[]>([])
  const [latestImportResult, setLatestImportResult] = useState<AdminUserImportResult | null>(null)
  const [importTasks, setImportTasks] = useState<AdminUserImportTask[]>([])
  const [batchStats, setBatchStats] = useState<AdminUserBatchStatsResponse | null>(null)

  const [unitName, setUnitName] = useState('')
  const [unitCode, setUnitCode] = useState('')
  const [unitType, setUnitType] = useState<OrgUnitType>('ORGANIZATION')
  const [parentUnitId, setParentUnitId] = useState('')

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [role, setRole] = useState<UserRole>('STUDENT')
  const [orgUnitId, setOrgUnitId] = useState('')
  const [classroomId, setClassroomId] = useState('')
  const [keyword, setKeyword] = useState('')
  const [roleFilter, setRoleFilter] = useState<'ALL' | UserRole>('ALL')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ENABLED' | 'DISABLED'>('ALL')
  const [lockFilter, setLockFilter] = useState<'ALL' | 'LOCKED' | 'UNLOCKED'>('ALL')
  const [filterOrgUnitId, setFilterOrgUnitId] = useState('')
  const [filterClassroomId, setFilterClassroomId] = useState('')
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([])
  const [activeUserId, setActiveUserId] = useState('')
  const [bulkUpdating, setBulkUpdating] = useState(false)
  const [showBatchDialog, setShowBatchDialog] = useState(false)
  const [batchAction, setBatchAction] = useState<'ENABLE' | 'DISABLE' | 'LOCK'>('ENABLE')
  const [showUnitDrawer, setShowUnitDrawer] = useState(false)
  const [showUserDrawer, setShowUserDrawer] = useState(false)
  const [showLogDialog, setShowLogDialog] = useState(false)
  const [showDetailModal, setShowDetailModal] = useState(false)
  const [detailLogsLoading, setDetailLogsLoading] = useState(false)
  const [detailLogs, setDetailLogs] = useState<AuditLogRecord[]>([])
  const [pageSize, setPageSize] = useState(10)
  const [currentPage, setCurrentPage] = useState(1)
  const [totalUsers, setTotalUsers] = useState(0)

  const organizations = useMemo(() => orgUnits.filter((item) => item.type === 'ORGANIZATION'), [orgUnits])
  const unitMap = useMemo(() => new Map(orgUnits.map((item) => [item.unitId, item] as const)), [orgUnits])
  const classrooms = useMemo(
    () => orgUnits.filter((item) => item.type === 'CLASSROOM' && (!orgUnitId || item.parentUnitId === orgUnitId)),
    [orgUnitId, orgUnits],
  )
  const filterClassrooms = useMemo(
    () => orgUnits.filter((item) => item.type === 'CLASSROOM' && (!filterOrgUnitId || item.parentUnitId === filterOrgUnitId)),
    [filterOrgUnitId, orgUnits],
  )
  const directoryRows = useMemo<DirectoryRow[]>(
    () =>
      users.map((item) => {
        const organization = item.orgUnitId ? unitMap.get(item.orgUnitId) : undefined
        const classroom = item.classroomId ? unitMap.get(item.classroomId) : undefined
        return {
          user: item,
          organization,
          classroom,
          department: organization?.name || '未分配院系',
          major: deriveMajor(item, organization, classroom),
          identity: deriveIdentity(item, organization, classroom),
        }
      }),
    [unitMap, users],
  )
  const activeRow = useMemo(() => directoryRows.find((item) => item.user.userId === activeUserId) ?? null, [activeUserId, directoryRows])
  const totalPages = useMemo(() => Math.max(1, Math.ceil(totalUsers / pageSize)), [pageSize, totalUsers])
  const pagedRows = directoryRows
  const pageStart = totalUsers ? (currentPage - 1) * pageSize + 1 : 0
  const pageEnd = totalUsers ? Math.min((currentPage - 1) * pageSize + pagedRows.length, totalUsers) : 0
  const enabledCount = useMemo(() => users.filter((item) => item.enabled).length, [users])
  const lockedCount = useMemo(() => users.filter((item) => item.locked).length, [users])
  const classroomCount = useMemo(() => orgUnits.filter((item) => item.type === 'CLASSROOM').length, [orgUnits])
  const unassignedCount = useMemo(() => users.filter((item) => !item.orgUnitId || !item.classroomId).length, [users])
  const organizationStats = useMemo(
    () =>
      organizations.map((item) => ({
        org: item,
        classroomCount: orgUnits.filter((unit) => unit.type === 'CLASSROOM' && unit.parentUnitId === item.unitId).length,
        userCount: users.filter((user) => user.orgUnitId === item.unitId).length,
      })),
    [organizations, orgUnits, users],
  )

  const loadData = useCallback(async () => {
    if (!canViewDirectory) {
      setOrgUnits([])
      setUsers([])
      setTotalUsers(0)
      setLoading(false)
      return
    }
    setLoading(true)
    try {
      const [units, usersPaged] = await Promise.all([
        getOrgUnits(),
        getAdminUsersPaged({
          page: currentPage,
          pageSize,
          keyword: keyword.trim() || undefined,
          role: roleFilter === 'ALL' ? undefined : roleFilter,
          enabled: statusFilter === 'ALL' ? undefined : statusFilter === 'ENABLED',
          locked: lockFilter === 'ALL' ? undefined : lockFilter === 'LOCKED',
          orgUnitId: filterOrgUnitId || undefined,
          classroomId: filterClassroomId || undefined,
          sortBy: 'updatedAt',
          sortOrder: 'desc',
        }),
      ])
      setOrgUnits(units)
      setUsers(usersPaged.list)
      setTotalUsers(usersPaged.total)
    } catch (error) {
      showToast(error instanceof Error ? error.message : '加载目录管理数据失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [canViewDirectory, currentPage, filterClassroomId, filterOrgUnitId, keyword, lockFilter, pageSize, roleFilter, showToast, statusFilter])

  useEffect(() => {
    void loadData()
  }, [loadData])

  useEffect(() => {
    if (!users.length) {
      setActiveUserId('')
      return
    }
    if (!activeUserId || !users.some((item) => item.userId === activeUserId)) {
      setActiveUserId(users[0].userId)
    }
  }, [activeUserId, users])

  useEffect(() => {
    if (!filterClassroomId) return
    const exists = filterClassrooms.some((item) => item.unitId === filterClassroomId)
    if (!exists) {
      setFilterClassroomId('')
    }
  }, [filterClassroomId, filterClassrooms])

  useEffect(() => {
    setCurrentPage(1)
    setSelectedUserIds([])
  }, [filterClassroomId, filterOrgUnitId, keyword, lockFilter, pageSize, roleFilter, statusFilter])

  useEffect(() => {
    setCurrentPage((current) => Math.min(current, totalPages))
  }, [totalPages])

  useEffect(() => {
    if (!hasBatchActionPermission && selectedUserIds.length) {
      setSelectedUserIds([])
    }
  }, [hasBatchActionPermission, selectedUserIds.length])

  useEffect(() => {
    if (!activeUserId) {
      setDetailLogs([])
      return
    }
    let cancelled = false
    setDetailLogsLoading(true)
    void getAuditLogs({ actorUserId: activeUserId })
      .then((items) => {
        if (!cancelled) {
          setDetailLogs(items.slice(0, 6))
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDetailLogs([])
        }
      })
      .finally(() => {
        if (!cancelled) {
          setDetailLogsLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [activeUserId])

  const refreshOperationFeedback = useCallback(async () => {
    if (canQueryImportTasks) {
      setImportTasksLoading(true)
      try {
        const taskPage = await getAdminUserImportTasks({ page: 1, pageSize: 6 })
        setImportTasks(taskPage.list)
      } catch (error) {
        showToast(error instanceof Error ? error.message : '获取导入任务失败', 'error')
      } finally {
        setImportTasksLoading(false)
      }
    } else {
      setImportTasks([])
    }

    if (canQueryBatchStats) {
      setBatchStatsLoading(true)
      try {
        const stats = await getAdminUserBatchStats(10)
        setBatchStats(stats)
      } catch (error) {
        showToast(error instanceof Error ? error.message : '获取批量任务统计失败', 'error')
      } finally {
        setBatchStatsLoading(false)
      }
    } else {
      setBatchStats(null)
    }
  }, [canQueryBatchStats, canQueryImportTasks, showToast])

  useEffect(() => {
    if (!canViewDirectory) {
      setImportTasks([])
      setBatchStats(null)
      return
    }
    void refreshOperationFeedback()
  }, [canViewDirectory, refreshOperationFeedback])

  function downloadBlob(blob: Blob, fileName: string) {
    const url = window.URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = fileName
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    window.URL.revokeObjectURL(url)
  }

  async function handleDownloadTemplate() {
    setDownloadingTemplate(true)
    try {
      const blob = await downloadAdminUserImportTemplate()
      downloadBlob(blob, 'account-import-template.csv')
      showToast('导入模板已下载', 'success')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '下载导入模板失败', 'error')
    } finally {
      setDownloadingTemplate(false)
    }
  }

  async function handleExport() {
    setExportingUsers(true)
    try {
      const blob = await exportAdminUsers({
        keyword: keyword.trim() || undefined,
        role: roleFilter === 'ALL' ? undefined : roleFilter,
        enabled: statusFilter === 'ALL' ? undefined : statusFilter === 'ENABLED',
        locked: lockFilter === 'ALL' ? undefined : lockFilter === 'LOCKED',
        orgUnitId: filterOrgUnitId || undefined,
        classroomId: filterClassroomId || undefined,
        sortBy: 'updatedAt',
        sortOrder: 'desc',
      })
      downloadBlob(blob, 'accounts-export.csv')
      showToast('导出文件已生成', 'success')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '导出账号失败', 'error')
    } finally {
      setExportingUsers(false)
    }
  }

  function handlePickImportFile() {
    fileInputRef.current?.click()
  }

  async function handleImportFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.currentTarget.value = ''
    if (!file) return
    setImportingUsers(true)
    try {
      const result = await importAdminUsers(file)
      setLatestImportResult(result)
      showToast(`导入完成：成功 ${result.successRows}，失败 ${result.failedRows}`, result.failedRows ? 'error' : 'success')
      await Promise.all([loadData(), refreshOperationFeedback()])
    } catch (error) {
      showToast(error instanceof Error ? error.message : '导入账号失败', 'error')
    } finally {
      setImportingUsers(false)
    }
  }

  async function handleViewImportTask(taskId: string) {
    try {
      const task = await getAdminUserImportTask(taskId)
      setLatestImportResult({
        taskId: task.taskId,
        totalRows: task.totalRows,
        successRows: task.successRows,
        failedRows: task.failedRows,
        errors: task.errors ?? [],
      })
      showToast(`已加载导入任务 ${task.taskId} 的明细`, 'success')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '获取导入任务详情失败', 'error')
    }
  }

  async function handleCreateUnit() {
    if (!unitName.trim()) {
      showToast('请先填写组织或班级名称', 'error')
      return
    }
    setSavingUnit(true)
    try {
      await createOrgUnit({
        name: unitName.trim(),
        code: unitCode.trim() || undefined,
        type: unitType,
        parentUnitId: unitType === 'CLASSROOM' ? parentUnitId || undefined : undefined,
      })
      setUnitName('')
      setUnitCode('')
      setUnitType('ORGANIZATION')
      setParentUnitId('')
      setShowUnitDrawer(false)
      showToast('组织目录已保存', 'success')
      await loadData()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '保存组织目录失败', 'error')
    } finally {
      setSavingUnit(false)
    }
  }

  async function handleCreateUser() {
    if (!username.trim() || !password.trim() || !displayName.trim()) {
      showToast('请完整填写用户名、密码和显示名称', 'error')
      return
    }
    setSavingUser(true)
    try {
      await createAdminUser({
        username: username.trim(),
        password: password,
        displayName: displayName.trim(),
        role,
        orgUnitId: orgUnitId || undefined,
        classroomId: classroomId || undefined,
        enabled: true,
      })
      setUsername('')
      setPassword('')
      setDisplayName('')
      setRole('STUDENT')
      setOrgUnitId('')
      setClassroomId('')
      setShowUserDrawer(false)
      showToast('用户已创建', 'success')
      await loadData()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '创建用户失败', 'error')
    } finally {
      setSavingUser(false)
    }
  }

  async function toggleUserEnabled(user: AdminUser) {
    if (!canUpdateUserStatus) {
      showToast('当前账号无状态变更权限', 'error')
      return
    }
    try {
      await updateAdminUserStatus(user.userId, !user.enabled)
      showToast(user.enabled ? '用户已停用' : '用户已启用', 'success')
      await loadData()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '更新用户状态失败', 'error')
    }
  }

  async function handleBatchOperation() {
    if (batchAction === 'LOCK' && !canBatchLock) {
      showToast('当前账号无批量锁定权限', 'error')
      return
    }
    if ((batchAction === 'ENABLE' || batchAction === 'DISABLE') && !canBatchStatus) {
      showToast('当前账号无批量状态变更权限', 'error')
      return
    }
    if (!selectedUserIds.length) {
      showToast('请先勾选需要批量处理的账号', 'error')
      return
    }
    setBulkUpdating(true)
    try {
      let result
      if (batchAction === 'LOCK') {
        result = await batchUpdateAdminUserLock(selectedUserIds, true, '管理员批量锁定')
      } else {
        const nextEnabled = batchAction === 'ENABLE'
        result = await batchUpdateAdminUserStatus(selectedUserIds, nextEnabled)
      }
      showToast(`批量处理完成：成功 ${result.success}，失败 ${result.failed}`, result.failed ? 'error' : 'success')
      setSelectedUserIds([])
      setShowBatchDialog(false)
      await Promise.all([loadData(), refreshOperationFeedback()])
    } catch (error) {
      showToast(error instanceof Error ? error.message : '批量更新账号状态失败', 'error')
    } finally {
      setBulkUpdating(false)
    }
  }

  function resetFilters() {
    setKeyword('')
    setRoleFilter('ALL')
    setStatusFilter('ALL')
    setLockFilter('ALL')
    setFilterOrgUnitId('')
    setFilterClassroomId('')
  }

  function toggleSelectedUser(userId: string) {
    setSelectedUserIds((current) => (current.includes(userId) ? current.filter((item) => item !== userId) : [...current, userId]))
  }

  function toggleSelectAllVisible() {
    const visibleIds = pagedRows.map((item) => item.user.userId)
    const allSelected = visibleIds.length > 0 && visibleIds.every((id) => selectedUserIds.includes(id))
    if (allSelected) {
      setSelectedUserIds((current) => current.filter((id) => !visibleIds.includes(id)))
      return
    }
    setSelectedUserIds((current) => Array.from(new Set([...current, ...visibleIds])))
  }

  const allVisibleSelected = pagedRows.length > 0 && pagedRows.every((item) => selectedUserIds.includes(item.user.userId))
  if (!canViewDirectory) {
    return (
      <section className="teaching-page">
        <div className="page-back-row">
          <PageBackLink to="/settings">返回设置</PageBackLink>
        </div>
        <EmptyState title="暂无访问权限" description="该页面已切换为权限点控制，请联系管理员分配账号目录访问权限后再进入。" />
      </section>
    )
  }

  return (
    <section className="teaching-page">
      <div className="page-back-row">
        <PageBackLink to="/settings">返回设置</PageBackLink>
      </div>

      <div className="teaching-page__hero panel glass">
        <div>
          <h2>组织 / 班级 / 用户归属管理</h2>
        </div>
      </div>
      <input ref={fileInputRef} type="file" accept=".csv,text/csv" style={{ display: 'none' }} onChange={(event) => void handleImportFileChange(event)} />

      {loading ? (
        <LoadingSpinner />
      ) : (
        <div className="admin-directory-page__content">
          <div className="operations-dashboard-summary">
            <article className="panel glass operations-dashboard-summary__item">
              <p className="operations-dashboard-summary__label">组织 / 院系</p>
              <strong className="operations-dashboard-summary__value">{organizations.length}</strong>
            </article>
            <article className="panel glass operations-dashboard-summary__item">
              <p className="operations-dashboard-summary__label">班级总数</p>
              <strong className="operations-dashboard-summary__value">{classroomCount}</strong>
            </article>
            <article className="panel glass operations-dashboard-summary__item">
              <p className="operations-dashboard-summary__label">当前页启用</p>
              <strong className="operations-dashboard-summary__value">
                {enabledCount} / {users.length}
              </strong>
            </article>
            <article className="panel glass operations-dashboard-summary__item">
              <p className="operations-dashboard-summary__label">当前页锁定</p>
              <strong className="operations-dashboard-summary__value">{lockedCount}</strong>
            </article>
          </div>

          <div className="admin-directory-page__layout">
            <div className="teaching-stack">
              <div className="panel glass teaching-panel">
                <div className="teaching-panel__head">
                  <div>
                    <p className="eyebrow">Filter</p>
                    <h3>筛选区</h3>
                  </div>
                  <div className="teaching-actions">
                    <AppButton size="sm" variant="ghost" onClick={resetFilters}>
                      重置条件
                    </AppButton>
                  </div>
                </div>
                <div className="teaching-form">
                  <div className="teaching-form__row admin-directory-page__filters">
                    <AppInput
                      label="关键词"
                      value={keyword}
                      onChange={(value) => setKeyword(String(value))}
                      placeholder="搜索姓名、账号、院系、班级或专业方向"
                    />
                    <label className="field-label">
                      <span>角色</span>
                      <select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value as 'ALL' | UserRole)}>
                        <option value="ALL">全部角色</option>
                        <option value="ADMIN">管理员</option>
                        <option value="TEACHER">教师</option>
                        <option value="STUDENT">学生</option>
                      </select>
                    </label>
                    <label className="field-label">
                      <span>账号状态</span>
                      <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as 'ALL' | 'ENABLED' | 'DISABLED')}>
                        <option value="ALL">全部状态</option>
                        <option value="ENABLED">启用中</option>
                        <option value="DISABLED">已停用</option>
                      </select>
                    </label>
                    <label className="field-label">
                      <span>锁定状态</span>
                      <select value={lockFilter} onChange={(event) => setLockFilter(event.target.value as 'ALL' | 'LOCKED' | 'UNLOCKED')}>
                        <option value="ALL">全部</option>
                        <option value="UNLOCKED">未锁定</option>
                        <option value="LOCKED">已锁定</option>
                      </select>
                    </label>
                    <label className="field-label">
                      <span>所属院系</span>
                      <select value={filterOrgUnitId} onChange={(event) => setFilterOrgUnitId(event.target.value)}>
                        <option value="">全部院系</option>
                        {organizations.map((item) => (
                          <option key={item.unitId} value={item.unitId}>
                            {item.name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label className="field-label">
                      <span>所属班级</span>
                      <select value={filterClassroomId} onChange={(event) => setFilterClassroomId(event.target.value)}>
                        <option value="">全部班级</option>
                        {filterClassrooms.map((item) => (
                          <option key={item.unitId} value={item.unitId}>
                            {item.name}
                          </option>
                        ))}
                      </select>
                    </label>
                  </div>
                </div>
              </div>

              <div className="panel glass teaching-panel">
                <div className="teaching-panel__head">
                  <div>
                    <p className="eyebrow">Directory Table</p>
                    <h3>表格区</h3>
                  </div>
                  <div className="teaching-actions">
                    {canCreateOrgUnit ? (
                      <AppButton size="sm" variant="primary" onClick={() => setShowUnitDrawer(true)}>
                        新建组织
                      </AppButton>
                    ) : null}
                    {canCreateUser ? (
                      <AppButton size="sm" variant="primary" onClick={() => setShowUserDrawer(true)}>
                        新建用户
                      </AppButton>
                    ) : null}
                    {canDownloadImportTemplate ? (
                      <AppButton size="sm" variant="ghost" loading={downloadingTemplate} onClick={() => void handleDownloadTemplate()}>
                        下载导入模板
                      </AppButton>
                    ) : null}
                    {canImportUsers ? (
                      <AppButton size="sm" variant="ghost" loading={importingUsers} onClick={handlePickImportFile}>
                        导入账号
                      </AppButton>
                    ) : null}
                    {canExportUsers ? (
                      <AppButton size="sm" variant="ghost" loading={exportingUsers} onClick={() => void handleExport()}>
                        导出筛选结果
                      </AppButton>
                    ) : null}
                    {canBatchStatus ? (
                      <AppButton
                        size="sm"
                        variant="ghost"
                        disabled={!selectedUserIds.length}
                        onClick={() => {
                          setBatchAction('ENABLE')
                          setShowBatchDialog(true)
                        }}
                      >
                        批量启用
                      </AppButton>
                    ) : null}
                    {canBatchStatus ? (
                      <AppButton
                        size="sm"
                        variant="ghost"
                        disabled={!selectedUserIds.length}
                        onClick={() => {
                          setBatchAction('DISABLE')
                          setShowBatchDialog(true)
                        }}
                      >
                        批量停用
                      </AppButton>
                    ) : null}
                    {canBatchLock ? (
                      <AppButton
                        size="sm"
                        variant="ghost"
                        disabled={!selectedUserIds.length}
                        onClick={() => {
                          setBatchAction('LOCK')
                          setShowBatchDialog(true)
                        }}
                      >
                        批量锁定
                      </AppButton>
                    ) : null}
                  </div>
                </div>
                <div className="teaching-meta">
                  <span>筛选结果：{totalUsers}</span>
                  <span>待补归属：{unassignedCount}</span>
                  <span>已选账号：{selectedUserIds.length}</span>
                </div>

                {hasBatchActionPermission ? (
                  selectedUserIds.length ? (
                    <div className="admin-directory-page__batch-tip">
                      已选择 {selectedUserIds.length} 个账号，可执行已授权的批量动作并查看统计反馈。
                    </div>
                  ) : (
                    <div className="admin-directory-page__batch-tip">
                      勾选当前页用户后可执行已授权的批量启停/锁定操作。
                    </div>
                  )
                ) : (
                  <div className="admin-directory-page__batch-tip">当前账号未开通批量变更权限，可查看只读目录与任务反馈。</div>
                )}

                {pagedRows.length ? (
                  <div className="admin-directory-page__table-wrap">
                    <table className="admin-directory-table">
                      <thead>
                        <tr>
                          <th>{hasBatchActionPermission ? <input type="checkbox" checked={allVisibleSelected} onChange={toggleSelectAllVisible} aria-label="全选当前页结果" /> : null}</th>
                          <th>姓名 / 账号</th>
                          <th>院系 / 专业</th>
                          <th>班级</th>
                          <th>角色</th>
                          <th>账号状态</th>
                          <th>锁定状态</th>
                          <th>失败次数</th>
                          <th>最近登录</th>
                          <th>密码更新时间</th>
                          <th>操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        {pagedRows.map((item) => (
                          <tr
                            key={item.user.userId}
                            className={activeUserId === item.user.userId ? 'is-active' : undefined}
                            onClick={() => setActiveUserId(item.user.userId)}
                          >
                            <td onClick={(event) => event.stopPropagation()}>
                              {hasBatchActionPermission ? (
                                <input
                                  type="checkbox"
                                  checked={selectedUserIds.includes(item.user.userId)}
                                  onChange={() => toggleSelectedUser(item.user.userId)}
                                  aria-label={`选择 ${item.user.displayName}`}
                                />
                              ) : null}
                            </td>
                            <td>
                              <strong>{item.user.displayName}</strong>
                              <p className="muted">{item.user.username}</p>
                            </td>
                            <td>
                              <strong>{item.department}</strong>
                              <p className="muted">{item.major}</p>
                            </td>
                            <td>{item.classroom?.name || '未分班'}</td>
                            <td>{roleLabel(item.user.role)}</td>
                            <td>
                              <span className={`pill small ${item.user.enabled ? 'admin-directory-page__pill--success' : 'admin-directory-page__pill--danger'}`}>
                                {item.user.enabled ? '启用中' : '已停用'}
                              </span>
                            </td>
                            <td>
                              <span className={`pill small ${item.user.locked ? 'admin-directory-page__pill--danger' : 'admin-directory-page__pill--success'}`}>
                                {item.user.locked ? '已锁定' : '未锁定'}
                              </span>
                            </td>
                            <td>{item.user.failedLoginCount ?? 0}</td>
                            <td>{fmt(item.user.lastLoginAt)}</td>
                            <td>{fmt(item.user.passwordUpdatedAt)}</td>
                            <td onClick={(event) => event.stopPropagation()}>
                              <div className="admin-directory-page__row-actions">
                                <button type="button" className="pill" onClick={() => { setActiveUserId(item.user.userId); setShowDetailModal(true); }}>
                                  查看详情
                                </button>
                                {canUpdateUserStatus ? (
                                  <button type="button" className="pill" onClick={() => void toggleUserEnabled(item.user)}>
                                    {item.user.enabled ? '停用' : '启用'}
                                  </button>
                                ) : null}
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <EmptyState title="暂无匹配用户" description="可以放宽筛选条件，或通过“新建组织/新建用户”按钮继续补录目录。" />
                )}

                <div className="admin-directory-page__pagination" role="navigation" aria-label="组织与用户分页">
                  <div className="admin-directory-page__pagination-status">
                    <strong>分页状态</strong>
                    {totalUsers ? (
                      <span>
                        第 {currentPage} / {totalPages} 页，当前显示第 {pageStart}-{pageEnd} 条，共 {totalUsers} 条
                      </span>
                    ) : (
                      <span>当前无匹配结果，可调整筛选条件后重试。</span>
                    )}
                  </div>
                  <div className="admin-directory-page__pagination-actions">
                    <label className="field-label admin-directory-page__page-size">
                      <span>每页条数</span>
                      <select value={pageSize} onChange={(event) => setPageSize(Number(event.target.value))}>
                        {PAGE_SIZE_OPTIONS.map((option) => (
                          <option key={option} value={option}>
                            {option} 条 / 页
                          </option>
                        ))}
                      </select>
                    </label>
                    <AppButton size="sm" variant="ghost" disabled={currentPage <= 1 || !totalUsers} onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}>
                      上一页
                    </AppButton>
                    <AppButton
                      size="sm"
                      variant="ghost"
                      disabled={currentPage >= totalPages || !totalUsers}
                      onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
                    >
                      下一页
                    </AppButton>
                  </div>
                </div>
              </div>

              {canQueryImportTasks || canQueryBatchStats ? (
                <div className="panel glass teaching-panel">
                  <div className="teaching-panel__head">
                    <div>
                      <p className="eyebrow">Task Feedback</p>
                      <h3>导入与批量反馈</h3>
                    </div>
                    <div className="teaching-actions">
                      <AppButton size="sm" variant="ghost" onClick={() => void refreshOperationFeedback()}>
                        刷新反馈
                      </AppButton>
                    </div>
                  </div>
                  {latestImportResult ? (
                    <div className="admin-directory-page__batch-tip">
                      最近导入任务：总计 {latestImportResult.totalRows}，成功 {latestImportResult.successRows}，失败 {latestImportResult.failedRows}。
                    </div>
                  ) : null}
                  <div className="teaching-form__row">
                    {canQueryImportTasks ? (
                      <section>
                        <h4>导入任务</h4>
                        {importTasksLoading ? (
                          <p className="muted">正在加载导入任务...</p>
                        ) : importTasks.length ? (
                          <div className="teaching-review-log">
                            {importTasks.map((task) => (
                              <article key={task.taskId} className="teaching-review-log__item">
                                <strong>{task.taskId}</strong>
                                <span className="muted">
                                  {task.status} ｜ 成功 {task.successRows} / 失败 {task.failedRows}
                                </span>
                                <span className="muted">{fmt(task.createdAt)}</span>
                                <div className="teaching-actions">
                                  <button type="button" className="pill" onClick={() => void handleViewImportTask(task.taskId)}>
                                    查看错误明细
                                  </button>
                                </div>
                              </article>
                            ))}
                          </div>
                        ) : (
                          <p className="muted">暂无导入任务记录。</p>
                        )}
                      </section>
                    ) : null}
                    {canQueryBatchStats ? (
                      <section>
                        <h4>批量统计</h4>
                        {batchStatsLoading ? (
                          <p className="muted">正在加载批量统计...</p>
                        ) : batchStats ? (
                          <div className="teaching-review-log">
                            <article className="teaching-review-log__item">
                              <strong>汇总</strong>
                              <span className="muted">
                                请求总数 {batchStats.totalRequested} ｜ 成功 {batchStats.totalSuccess} ｜ 失败 {batchStats.totalFailed}
                              </span>
                            </article>
                            {batchStats.items.slice(0, 6).map((item, index) => (
                              <article key={`${item.action}-${item.createdAt || index}`} className="teaching-review-log__item">
                                <strong>{item.action}</strong>
                                <span className="muted">
                                  {fmt(item.createdAt)} ｜ 成功 {item.success} / 失败 {item.failed}
                                </span>
                              </article>
                            ))}
                          </div>
                        ) : (
                          <p className="muted">暂无批量处理统计。</p>
                        )}
                      </section>
                    ) : null}
                  </div>
                  {latestImportResult?.errors?.length ? (
                    <div className="teaching-review-log">
                      {latestImportResult.errors.slice(0, 6).map((error) => (
                        <article key={`${error.rowNumber}-${error.username}`} className="teaching-review-log__item">
                          <strong>第 {error.rowNumber} 行（{error.username || '未识别账号'}）</strong>
                          <span className="muted">{error.message}</span>
                        </article>
                      ))}
                    </div>
                  ) : null}
                </div>
              ) : null}

              <div className="panel glass teaching-panel">
                <div className="teaching-panel__head">
                  <div>
                    <p className="eyebrow">Organization</p>
                    <h3>组织与班级概览</h3>
                  </div>
                  <div className="teaching-meta">
                    <span>组织：{organizations.length}</span>
                    <span>班级：{classroomCount}</span>
                  </div>
                </div>
                <div className="admin-directory-page__org-grid">
                  {organizationStats.map((item) => (
                    <article key={item.org.unitId} className="operations-dashboard-link-card">
                      <div className="teaching-card__head">
                        <div>
                          <p className="eyebrow">Org Unit</p>
                          <h3>{item.org.name}</h3>
                        </div>
                        <span className="pill small">{orgUnitTypeLabel(item.org.type)}</span>
                      </div>
                      <div className="teaching-meta">
                        <span>编码：{item.org.code || '未设置'}</span>
                        <span>班级：{item.classroomCount}</span>
                        <span>成员：{item.userCount}</span>
                      </div>
                    </article>
                  ))}
                </div>
              </div>
            </div>


          </div>
        </div>
      )}

      <ActionDrawer
        open={canCreateOrgUnit && showUnitDrawer}
        title="新建组织 / 班级"
        description="目录创建改为抽屉承载，避免右侧栏持续堆叠。"
        onClose={() => setShowUnitDrawer(false)}
        footer={
          <>
            <AppButton onClick={() => setShowUnitDrawer(false)}>取消</AppButton>
            <AppButton variant="primary" loading={savingUnit} onClick={() => void handleCreateUnit()}>
              保存组织目录
            </AppButton>
          </>
        }
      >
        <div className="teaching-form">
          <AppInput label="名称" value={unitName} onChange={(value) => setUnitName(String(value))} placeholder="例如：数字影视学院 / 2026 级 1 班" />
          <AppInput label="编码" value={unitCode} onChange={(value) => setUnitCode(String(value))} placeholder="例如：film-school / class-2026-1" />
          <label className="field-label">
            <span>类型</span>
            <select value={unitType} onChange={(event) => setUnitType(event.target.value as OrgUnitType)}>
              <option value="ORGANIZATION">组织</option>
              <option value="CLASSROOM">班级</option>
            </select>
          </label>
          {unitType === 'CLASSROOM' ? (
            <label className="field-label">
              <span>上级组织</span>
              <select value={parentUnitId} onChange={(event) => setParentUnitId(event.target.value)}>
                <option value="">请选择组织</option>
                {organizations.map((item) => (
                  <option key={item.unitId} value={item.unitId}>
                    {item.name}
                  </option>
                ))}
              </select>
            </label>
          ) : null}
        </div>
      </ActionDrawer>

      <ActionDrawer
        open={canCreateUser && showUserDrawer}
        title="新建用户"
        description="保留完整字段，按需打开抽屉录入。"
        onClose={() => setShowUserDrawer(false)}
        footer={
          <>
            <AppButton onClick={() => setShowUserDrawer(false)}>取消</AppButton>
            <AppButton variant="primary" loading={savingUser} onClick={() => void handleCreateUser()}>
              创建用户
            </AppButton>
          </>
        }
      >
        <div className="teaching-form">
          <AppInput label="用户名" value={username} onChange={(value) => setUsername(String(value))} placeholder="例如：teacher.li" />
          <AppInput label="初始密码" type="password" value={password} onChange={(value) => setPassword(String(value))} placeholder="至少 6 位" />
          <AppInput label="显示名称" value={displayName} onChange={(value) => setDisplayName(String(value))} placeholder="例如：李老师" />
          <label className="field-label">
            <span>角色</span>
            <select value={role} onChange={(event) => setRole(event.target.value as UserRole)}>
              <option value="ADMIN">管理员</option>
              <option value="TEACHER">教师</option>
              <option value="STUDENT">学生</option>
            </select>
          </label>
          <label className="field-label">
            <span>所属组织</span>
            <select value={orgUnitId} onChange={(event) => setOrgUnitId(event.target.value)}>
              <option value="">未设置</option>
              {organizations.map((item) => (
                <option key={item.unitId} value={item.unitId}>
                  {item.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field-label">
            <span>所属班级</span>
            <select value={classroomId} onChange={(event) => setClassroomId(event.target.value)}>
              <option value="">未设置</option>
              {classrooms.map((item) => (
                <option key={item.unitId} value={item.unitId}>
                  {item.name}
                </option>
              ))}
            </select>
          </label>
        </div>
      </ActionDrawer>

      <ConfirmDialog
        visible={hasBatchActionPermission && showBatchDialog}
        title="批量操作确认"
        message={`已选择 ${selectedUserIds.length} 个账号，是否确认${
          batchAction === 'LOCK' ? '批量锁定' : batchAction === 'ENABLE' ? '批量启用' : '批量停用'
        }？`}
        confirmText={batchAction === 'LOCK' ? '确认锁定' : batchAction === 'ENABLE' ? '确认启用' : '确认停用'}
        confirmLoading={bulkUpdating}
        onConfirm={() => void handleBatchOperation()}
        onCancel={() => setShowBatchDialog(false)}
      />

      {showLogDialog && activeRow ? (
        <div className="dialog-overlay" role="dialog" aria-modal onClick={(event) => event.currentTarget === event.target && setShowLogDialog(false)}>
          <div className="dialog glass modal-form-dialog">
            <h3 className="dialog-title">{activeRow.user.displayName} · 操作日志</h3>
            {detailLogsLoading ? (
              <LoadingSpinner />
            ) : detailLogs.length ? (
              <div className="teaching-review-log">
                {detailLogs.map((item) => (
                  <article key={item.id} className="teaching-review-log__item">
                    <strong>{item.action}</strong>
                    <span className="muted">
                      {fmt(item.createdAt)} ｜ {item.entityType} / {item.entityId}
                    </span>
                    <span className="muted">{summarizeAudit(item.detailsJson)}</span>
                  </article>
                ))}
              </div>
            ) : (
              <div className="teaching-review-log">
                <article className="teaching-review-log__item">
                  <strong>创建账号</strong>
                  <span className="muted">{fmt(activeRow.user.createdAt)}</span>
                  <span className="muted">已录入后台目录，可继续补充课程成员关系与更多权限细分。</span>
                </article>
                <article className="teaching-review-log__item">
                  <strong>最近信息变更</strong>
                  <span className="muted">{fmt(activeRow.user.updatedAt)}</span>
                  <span className="muted">当前暂无独立审计记录，页面以账号目录创建与更新时间作为兜底时间线。</span>
                </article>
              </div>
            )}
            <div className="dialog-actions">
              <button type="button" className="btn-cancel" onClick={() => setShowLogDialog(false)}>
                关闭
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {showDetailModal && activeRow ? (
        <div className="dialog-overlay" role="dialog" aria-modal onClick={(event) => { if (event.currentTarget === event.target) { setShowDetailModal(false); setActiveUserId(''); } }}>
          <div className="dialog glass modal-form-dialog admin-directory-page__detail-modal">
            <div className="dialog-title-row">
              <h3 className="dialog-title">{activeRow.user.displayName} · 详情</h3>
              <button type="button" className="dialog-close-btn" aria-label="关闭详情" onClick={() => { setShowDetailModal(false); setActiveUserId(''); }}>
                ×
              </button>
            </div>
            <div className="teaching-form">
              <div className="admin-directory-page__detail-hero">
                <div>
                  <h3>{activeRow.user.displayName}</h3>
                  <p className="muted">
                    {roleLabel(activeRow.user.role)} ｜ {activeRow.identity}
                  </p>
                </div>
                <span
                  className={`pill small ${
                    activeRow.user.enabled ? 'admin-directory-page__pill--success' : 'admin-directory-page__pill--danger'
                  }`}
                >
                  {activeRow.user.enabled ? '启用中' : '已停用'}
                </span>
              </div>

              <section className="admin-directory-page__detail-section">
                <h4>基础信息</h4>
                <div className="admin-directory-page__kv-grid">
                  <div>
                    <span>用户名</span>
                    <strong>{activeRow.user.username}</strong>
                  </div>
                  <div>
                    <span>显示名称</span>
                    <strong>{activeRow.user.displayName}</strong>
                  </div>
                  <div>
                    <span>账号状态</span>
                    <strong>{activeRow.user.enabled ? '启用中' : '已停用'}</strong>
                  </div>
                  <div>
                    <span>锁定状态</span>
                    <strong>{activeRow.user.locked ? '已锁定' : '未锁定'}</strong>
                  </div>
                  <div>
                    <span>失败次数</span>
                    <strong>{activeRow.user.failedLoginCount ?? 0}</strong>
                  </div>
                  <div>
                    <span>最近登录</span>
                    <strong>{fmt(activeRow.user.lastLoginAt)}</strong>
                  </div>
                  <div>
                    <span>密码更新时间</span>
                    <strong>{fmt(activeRow.user.passwordUpdatedAt)}</strong>
                  </div>
                  <div>
                    <span>创建时间</span>
                    <strong>{fmt(activeRow.user.createdAt)}</strong>
                  </div>
                  <div>
                    <span>更新时间</span>
                    <strong>{fmt(activeRow.user.updatedAt)}</strong>
                  </div>
                </div>
              </section>

              <section className="admin-directory-page__detail-section">
                <h4>校园身份</h4>
                <div className="admin-directory-page__kv-grid">
                  <div>
                    <span>角色</span>
                    <strong>{roleLabel(activeRow.user.role)}</strong>
                  </div>
                  <div>
                    <span>院系</span>
                    <strong>{activeRow.department}</strong>
                  </div>
                  <div>
                    <span>专业方向</span>
                    <strong>{activeRow.major}</strong>
                  </div>
                  <div>
                    <span>班级</span>
                    <strong>{activeRow.classroom?.name || '未分班'}</strong>
                  </div>
                  <div>
                    <span>校园身份说明</span>
                    <strong>{activeRow.identity}</strong>
                  </div>
                </div>
              </section>

              <div className="teaching-actions">
                {canUpdateUserStatus ? (
                  <AppButton size="sm" variant={activeRow.user.enabled ? 'danger' : 'primary'} onClick={() => void toggleUserEnabled(activeRow.user)}>
                    {activeRow.user.enabled ? '停用当前账号' : '启用当前账号'}
                  </AppButton>
                ) : null}
                {hasBatchActionPermission ? (
                  <button type="button" className="pill" onClick={() => toggleSelectedUser(activeRow.user.userId)}>
                    {selectedUserIds.includes(activeRow.user.userId) ? '取消批量选择' : '加入批量操作'}
                  </button>
                ) : null}
              </div>
            </div>
            <div className="dialog-actions">
              <button type="button" className="btn-cancel" onClick={() => { setShowDetailModal(false); setActiveUserId(''); }}>
                关闭
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
