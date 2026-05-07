import { useNavigate } from 'react-router-dom'
import { FeatureCard } from '@/components/home/FeatureCard'
import { HeroSection } from '@/components/home/HeroSection'

const features = [
  '提示词一键生成文案/图片/视频',
  '风格标签与参数可调，快速产出素材',
  '支持历史回溯、复制下载、重生成',
]

const cards = [
  { title: '极速创作', desc: '最少输入，快速得到可发布素材，减少从想法到发布的时间。', index: '01' },
  { title: '沉浸体验', desc: '柔和动效与清晰层级，让每一次生成反馈都易于理解。', index: '02' },
  { title: '可追溯结果', desc: '支持历史查询、收藏与重生成，持续迭代你的内容版本。', index: '03' },
]

export function HomePage() {
  const navigate = useNavigate()
  return (
    <section className="home-page">
      <HeroSection
        features={features}
        onStart={() => navigate('/workspace')}
        onOtherProvider={() => navigate('/models/hub')}
      />
      <section className="value-section" aria-labelledby="value-heading">
        <header className="value-head">
          <p id="value-heading" className="eyebrow">
            Why this product
          </p>
          <h2 className="value-title">为创作流程而设计</h2>
          <p className="value-lead muted">更少干扰，更专注在提示、生成与迭代上。</p>
        </header>
        <div className="cards">
          {cards.map((item) => (
            <FeatureCard key={item.title} title={item.title} desc={item.desc} index={item.index} />
          ))}
        </div>
      </section>
    </section>
  )
}
