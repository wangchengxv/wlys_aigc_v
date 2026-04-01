import { AppButton } from '@/components/common/AppButton'

type Props = {
  features: string[]
  onStart: () => void
  /** 自定义 API / 服务商中心 */
  onOtherProvider?: () => void
}

export function HeroSection({ features, onStart, onOtherProvider }: Props) {
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
        <p className="badge">Creative suite</p>
        <h1>AIGC 图文生成平台</h1>
        <p className="lead muted">输入简单文案或上传图片，快速生成可用于发布的图片与视频内容——流程清晰、反馈即时。</p>
        <div className="cta-row">
          <AppButton variant="primary" className="cta" onClick={onStart}>
            开始创作
          </AppButton>
          {onOtherProvider ? (
            <AppButton variant="ghost" className="cta-secondary" onClick={onOtherProvider}>
              选择其他服务商
            </AppButton>
          ) : null}
          <span className="cta-note muted">无需复杂配置，从想法到素材一步到位</span>
        </div>
      </div>
      <div className="right panel examples">
        <p className="title">Prompt ideas</p>
        <div className="example-grid">
          <div className="example-item">3D游戏人物风格，戴夸张耳饰的民族风少女</div>
          <div className="example-item">插画风格，太空飞行器在粉色星球低空飞行</div>
          <div className="example-item">未来风，女性机械人形 AI</div>
          <div className="example-item">超写实插画，夜晚月光下的花园</div>
        </div>
      </div>
    </section>
  )
}
