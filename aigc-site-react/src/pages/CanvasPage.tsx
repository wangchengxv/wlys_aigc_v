import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { listScriptProjects } from '@/api'
import { ComfyLikeCanvas } from '@/components/canvas/ComfyLikeCanvas'
import { QuickActionGrid } from '@/components/common/QuickActionGrid'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { useToast } from '@/context/ToastContext'
import { useAuthStore } from '@/stores/authStore'
import type { ScriptProjectSummary } from '@/types'

function normalizeTitle(value: string) {
  return value.trim() || '未命名无限画布'
}

export function CanvasPage() {
  const { showToast } = useToast()
  const user = useAuthStore((s) => s.user)
  const [searchParams, setSearchParams] = useSearchParams()
  const [projects, setProjects] = useState<ScriptProjectSummary[]>([])
  const [loadingProjects, setLoadingProjects] = useState(true)
  const [draftTitle, setDraftTitle] = useState('未命名无限画布')
  const [projectId, setProjectId] = useState(searchParams.get('projectId') ?? '')
  const selectedProject = useMemo(
    () => projects.find((item) => item.projectId === projectId) ?? null,
    [projectId, projects],
  )

  useEffect(() => {
    void (async () => {
      setLoadingProjects(true)
      try {
        const list = await listScriptProjects({ deleted: false })
        setProjects(list)
      } catch (error) {
        showToast(error instanceof Error ? error.message : '加载工程列表失败', 'error')
      } finally {
        setLoadingProjects(false)
      }
    })()
  }, [showToast])

  useEffect(() => {
    const next = new URLSearchParams(searchParams)
    if (projectId) {
      next.set('projectId', projectId)
    } else {
      next.delete('projectId')
    }
    if (next.toString() !== searchParams.toString()) {
      setSearchParams(next, { replace: true })
    }
  }, [projectId, searchParams, setSearchParams])

  const quickLinks = useMemo(
    () =>
      [
        {
          key: 'workspace',
          title: '创作工作台',
          description: '先用文生图 / 视频工作台快速验证提示词，再回到画布编排节点',
          to: '/workspace',
          badge: '工具',
        },
        {
          key: 'project-library',
          title: selectedProject ? '打开当前工程' : '剧本工程库',
          description: selectedProject ? '跳到当前绑定工程，继续处理资产、镜头与成片链路' : '从项目库选择工程后，可把画布草稿绑定到项目上下文',
          to: selectedProject ? `/script-projects/${encodeURIComponent(selectedProject.projectId)}` : '/script-projects',
          badge: '项目',
        },
        {
          key: 'image-tool',
          title: '文生图工具',
          description: '与无限画布共享创作链路，可对照验证模型与提示词效果',
          to: '/tools/image',
          badge: '生成',
        },
        {
          key: 'reverse-prompt',
          title: '反推提示词',
          description: '上传图片反推正向/反向提示词，再回填到画布 Prompt 节点',
          to: '/tools/reverse-prompt',
          badge: '反推',
        },
        {
          key: 'history',
          title: '历史记录',
          description: '后续会把画布执行结果纳入统一历史入口，本阶段先保留跳转路径',
          to: '/history',
          badge: '记录',
        },
      ],
    [selectedProject],
  )

  return (
    <section className="canvas-tool-page">
      <div className="workspace-home__hero canvas-tool-page__hero">
        <div className="workspace-home__hero-copy">
          <span className="workspace-home__pill">{user ? `${user.displayName} 的画布工作区` : '访客画布模式'}</span>
          <h2>正式可用的无限画布 AIGC 工具</h2>
          <p>
            在本工程内完成节点编辑、草稿保存、项目绑定、Comfy 提交与最小结果回显。当前阶段只开放受控模板与受控节点目录，
            避免直接暴露全量 ComfyUI 节点生态。
          </p>
        </div>
        <div className="canvas-tool-page__hero-side">
          <div className="canvas-tool-page__hero-meta">
            <strong>使用说明</strong>
            <span>1. 填写标题并按需绑定工程</span>
            <span>2. 在画布中拖拽、缩放、连线并保存草稿</span>
            <span>3. 提交到 Comfy 后在结果区查看状态与回显</span>
          </div>
          <div className="canvas-tool-page__hero-links">
            <Link className="app-btn v-primary s-md" to="/script-projects">
              打开项目库
            </Link>
            <Link className="app-btn v-ghost s-md" to="/workspace">
              返回创作工作台
            </Link>
          </div>
        </div>
      </div>

      <div className="canvas-tool-page__grid">
        <section className="content-card">
          <div className="section-heading">
            <h3>草稿绑定</h3>
            <span>标题、更新时间与项目关联会一起保存；未绑定工程时仍可正常使用</span>
          </div>

          <div className="canvas-tool-page__form-grid">
            <label className="input-wrap">
              <span className="label">草稿标题</span>
              <input
                className="ctrl"
                value={draftTitle}
                onChange={(event) => setDraftTitle(event.target.value)}
                onBlur={() => setDraftTitle((current) => normalizeTitle(current))}
                placeholder="例如：课程海报流程画布"
              />
            </label>

            <label className="input-wrap">
              <span className="label">绑定剧本工程（可选）</span>
              {loadingProjects ? (
                <div className="canvas-tool-page__select-loading">
                  <LoadingSpinner />
                </div>
              ) : (
                <select className="ctrl" value={projectId} onChange={(event) => setProjectId(event.target.value)}>
                  <option value="">暂不绑定工程</option>
                  {projects.map((project) => (
                    <option key={project.projectId} value={project.projectId}>
                      {project.name}
                    </option>
                  ))}
                </select>
              )}
            </label>
          </div>

          <div className="canvas-tool-page__binding-meta">
            <div>
              <strong>{selectedProject?.name || '未绑定工程'}</strong>
              <p className="muted">
                {selectedProject
                  ? `当前草稿会与项目「${selectedProject.name}」关联，便于后续衔接资产页与项目流程。`
                  : '未绑定时仍支持本地缓存、远端保存、Comfy 提交与结果回显。'}
              </p>
            </div>
            {selectedProject ? (
              <Link className="app-btn v-ghost s-sm" to={`/script-projects/${encodeURIComponent(selectedProject.projectId)}`}>
                打开工程
              </Link>
            ) : null}
          </div>
        </section>

        <section className="content-card">
          <div className="section-heading">
            <h3>帮助与边界</h3>
            <span>对齐现有工具页样式、角色策略与项目跳转路径</span>
          </div>
          <div className="canvas-tool-page__notes">
            <p className="muted">
              {user
                ? `当前以${user.role === 'ADMIN' ? '管理员' : user.role === 'TEACHER' ? '教师' : '学生'}身份访问，导航入口与页面标题已升级为正式工具页。`
                : '当前以访客模式访问，草稿会优先保存在本地，并尝试以临时身份同步远端画布。'}
            </p>
            <p className="muted">
              第一阶段结果只做最小可用回显；后续会继续接入历史记录、媒体资源中心和项目资产页。
            </p>
            <p className="muted">
              受控节点目录只开放基础 Prompt / Render 节点，动态同步全量 Comfy 节点保留为后续增强项。
            </p>
          </div>
        </section>
      </div>

      <QuickActionGrid items={quickLinks} />

      <ComfyLikeCanvas
        draftTitle={normalizeTitle(draftTitle)}
        projectId={projectId || null}
        projectName={selectedProject?.name}
        onDraftMetaHydrated={(meta) => {
          setDraftTitle(normalizeTitle(meta.title))
          setProjectId(meta.projectId ?? '')
        }}
      />
    </section>
  )
}
