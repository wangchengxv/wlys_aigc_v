import { AppButton } from '@/components/common/AppButton'

type Props = {
  features: string[]
  highlights: Array<{ label: string; value: string }>
  scenarios: Array<{ title: string; desc: string }>
  onStart: () => void
  /** 自定义 API / 服务商中心 */
  onOtherProvider?: () => void
}

export function HeroSection({ features, highlights, scenarios, onStart, onOtherProvider }: Props) {
  return (
    <section className="hero panel glass">
      <ul className="sr-only">
        {features.map((f) => (
          <li key={f}>{f}</li>
        ))}
      </ul>
      <div className="hero-bg" aria-hidden>
        <span className="glow g1" />
        <span className="glow g2" />
        <span className="glow g3" />
        <span className="grid-lines" />
      </div>
      <div className="left">
        <p className="badge">Campus AI Studio</p>
        <div className="hero-highlights" aria-label="平台品牌摘要">
          {highlights.map((item) => (
            <article key={item.label} className="hero-highlights__item">
              <strong>{item.value}</strong>
              <span>{item.label}</span>
            </article>
          ))}
        </div>
        <h1>高校 AIGC 实训与创作中台</h1>
        <p className="lead muted">面向高校教学、项目创作与作品交付的一体化平台，把课程实训、AI 内容生产、审核治理和资源管理整合到统一工作流中。</p>
        <div className="hero-feature-list" aria-label="平台能力摘要">
          {features.map((f) => (
            <span key={f} className="hero-feature-chip">
              {f}
            </span>
          ))}
        </div>
        <div className="cta-row">
          <AppButton variant="primary" className="cta" onClick={onStart}>
            进入创作工作台
          </AppButton>
          {onOtherProvider ? (
            <AppButton variant="ghost" className="cta-secondary" onClick={onOtherProvider}>
              查看模型与服务商
            </AppButton>
          ) : null}
          <span className="cta-note muted">从课程任务、素材生成到作品导出，统一沉淀在一个平台里</span>
        </div>
      </div>
      <div className="right panel examples">
        <div className="hero-snapshot">
          <p className="title">Platform snapshot</p>
          <div className="example-grid">
            <div className="example-item">课程实训视角：快速组织课程、班级、作业、提交与批改流程</div>
            <div className="example-item">项目生产视角：围绕剧本、素材、分镜、配音、成片与导出建立完整工作流</div>
            <div className="example-item">校园治理视角：支持组织用户、审计日志、模型资源与系统配置统一管理</div>
            <div className="example-item">成果交付视角：学生提交作品，教师审核归档，平台形成可追溯的实训数据资产</div>
          </div>
        </div>
        <div className="hero-scenarios">
          <p className="title">Scenario showcase</p>
          <div className="hero-scenarios__list">
            {scenarios.map((item) => (
              <article key={item.title} className="hero-scenarios__item">
                <strong>{item.title}</strong>
                <p>{item.desc}</p>
              </article>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
