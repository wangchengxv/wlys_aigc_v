import { Link } from 'react-router-dom'
import { QuickActionGrid } from '@/components/common/QuickActionGrid'
import { StatStrip } from '@/components/common/StatStrip'
import { ReversePromptPanel } from '@/components/workspace/ReversePromptPanel'

export function ReversePromptPage() {
  return (
    <section className="reverse-prompt-page reverse-prompt-page--revamp">
      <StatStrip
        className="stat-strip--compact"
        items={[
          { key: 'input', label: '图片输入', value: '上传 + URL', hint: '支持本地上传与链接输入' },
          { key: 'model', label: '模型选择', value: '豆包系列', hint: '按配置自动筛选可用项' },
          { key: 'output', label: '输出结构', value: '正向 + 反向 + 参数', hint: '可一键复制回填工作流' },
          { key: 'flow', label: '适配场景', value: '工作台 / 画布', hint: '统一入口，跨页面复用' },
        ]}
      />

      <div className="workspace-home__hero">
        <div className="workspace-home__hero-copy">
          <span className="workspace-home__pill">豆包反推能力</span>
          <h2>图片反推提示词</h2>
          <p>上传图片后，自动提取正向/反向提示词和关键参数，便于回填到工作台或画布流程。</p>
        </div>
        <div className="workspace-home__hero-actions">
          <Link className="app-btn v-primary s-md" to="/workspace">
            返回创作工作台
          </Link>
          <Link className="app-btn v-ghost s-md" to="/canvas">
            打开无限画布
          </Link>
        </div>
      </div>

      <div className="reverse-prompt-page__grid">
        <ReversePromptPanel />
        <aside className="content-card reverse-prompt-page__guide">
          <div className="section-heading">
            <h3>推荐使用流程</h3>
            <span>保持与创作工作台一致的操作节奏</span>
          </div>
          <ol className="workflow-steps reverse-prompt-page__steps">
            <li className="workflow-steps__item">
              <span className="workflow-steps__index">1</span>
              <div>
                <strong>输入图片并选择模型</strong>
                <p>上传本地图片或填写 URL，优先选择与你目标生成场景一致的豆包模型。</p>
              </div>
            </li>
            <li className="workflow-steps__item">
              <span className="workflow-steps__index">2</span>
              <div>
                <strong>查看结构化反推结果</strong>
                <p>重点关注正向、反向和参数建议，并结合风格词做二次微调。</p>
              </div>
            </li>
            <li className="workflow-steps__item">
              <span className="workflow-steps__index">3</span>
              <div>
                <strong>回填到工作台或画布</strong>
                <p>将提示词复制到文生图、图生视频或画布 Prompt 节点，形成闭环迭代。</p>
              </div>
            </li>
          </ol>
        </aside>
      </div>

      <QuickActionGrid
        items={[
          {
            key: 'workspace',
            title: '前往创作工作台',
            description: '把反推结果快速用于文生图、文生视频或图生视频任务',
            to: '/workspace',
            badge: '工作台',
          },
          {
            key: 'canvas',
            title: '前往无限画布',
            description: '把提示词回填到节点流，继续编排与保存草稿',
            to: '/canvas',
            badge: '画布',
          },
        ]}
      />
    </section>
  )
}
