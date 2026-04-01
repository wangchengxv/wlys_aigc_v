import { useMemo } from 'react'
import { PageBackLink } from '@/components/common/PageBackLink'
import { groupPresetsByCategory } from '@/data/videoStylePresets'
import { useToast } from '@/context/ToastContext'
import { AppInput } from '@/components/common/AppInput'
import type {
  GlobalAspectRatio,
  GlobalCreationMode,
  GlobalModelStrategy,
  GlobalScriptType,
  GlobalStoryboardLayout,
} from '@/types'
import { useGlobalSettingsStore } from '@/stores/globalSettingsStore'

const DEFAULT_PRESET_ID = 'film-cinematic'

const ASPECT_OPTIONS: GlobalAspectRatio[] = ['16:9', '9:16', '4:3', '3:4', '1:1', '21:9']
const SCRIPT_TYPE_OPTIONS: GlobalScriptType[] = ['剧情演绎', '真人解说']
const STRATEGY_OPTIONS: GlobalModelStrategy[] = ['省钱优先', '画质优先']
const CREATION_OPTIONS: GlobalCreationMode[] = ['生视频模式', '多参数生视频']
const STORYBOARD_OPTIONS: GlobalStoryboardLayout[] = ['单', '九宫格机位']
const DURATION_OPTIONS: number[] = [5, 10, 15, 20]

export function GlobalSettingsPage() {
  const { showToast } = useToast()
  const aspectRatio = useGlobalSettingsStore((s) => s.aspectRatio)
  const scriptType = useGlobalSettingsStore((s) => s.scriptType)
  const modelStrategy = useGlobalSettingsStore((s) => s.modelStrategy)
  const creationMode = useGlobalSettingsStore((s) => s.creationMode)
  const storyboardLayout = useGlobalSettingsStore((s) => s.storyboardLayout)
  const targetDurationSec = useGlobalSettingsStore((s) => s.targetDurationSec)
  const visualStyleMode = useGlobalSettingsStore((s) => s.visualStyleMode)
  const visualStylePresetId = useGlobalSettingsStore((s) => s.visualStylePresetId)
  const customVisualStyle = useGlobalSettingsStore((s) => s.customVisualStyle)
  const visualStyleLongTextMode = useGlobalSettingsStore((s) => s.visualStyleLongTextMode)
  const patch = useGlobalSettingsStore((s) => s.patch)
  const reset = useGlobalSettingsStore((s) => s.reset)

  const presetsByCategory = useMemo(() => groupPresetsByCategory(), [])

  async function copyFullPrompt(text: string) {
    try {
      await navigator.clipboard.writeText(text)
      showToast('完整示例提示词已复制', 'success')
    } catch {
      showToast('复制失败，请手动选择文本复制', 'error')
    }
  }

  return (
    <section className="panel glass settings-page global-settings-page">
      <div className="global-settings-header">
        <div className="page-back-row">
          <PageBackLink />
          <button type="button" className="pill" onClick={() => reset()}>
            恢复默认
          </button>
        </div>
        <p className="hint muted">
          以下选项保存在本机浏览器。新建剧本工程时默认采用「画面比例」与「视觉风格」。系统预设默认写入「风格名 + 核心特点」短描述；可在视觉风格中开启「长文本模式」将完整示例提示词写入项目。
        </p>
      </div>

      <div className="setting-item">
        <p>画面比例</p>
        <p className="hint muted">适用于剧本视频与分镜参考；新建项目时作为默认比例。</p>
        <div className="theme-pills" role="group" aria-label="画面比例">
          {ASPECT_OPTIONS.map((opt) => (
            <button key={opt} type="button" className={`pill${aspectRatio === opt ? ' active' : ''}`} onClick={() => patch({ aspectRatio: opt })}>
              {opt}
            </button>
          ))}
        </div>
      </div>

      <div className="setting-item">
        <p>生成视频时长</p>
        <p className="hint muted">用于新建剧本的“目标时长（秒）”。也会参与镜头时长分配（音画同步）。</p>
        <div className="theme-pills" role="group" aria-label="生成视频时长">
          {DURATION_OPTIONS.map((opt) => (
            <button
              key={opt}
              type="button"
              className={`pill${targetDurationSec === opt ? ' active' : ''}`}
              onClick={() => patch({ targetDurationSec: opt })}
            >
              {opt}S
            </button>
          ))}
        </div>
        <div style={{ marginTop: 'var(--space-md)' }}>
          <AppInput
            label="自定义（秒）"
            value={targetDurationSec}
            onChange={(v) => patch({ targetDurationSec: Number(v) })}
            type="number"
            min={1}
            max={600}
          />
        </div>
      </div>

      <div className="setting-item style-library-section">
        <p>视觉风格</p>
        <p className="hint muted">
          系统库条目来自《AI 视频生成风格与提示词大全》。选用预设时默认写入短描述；开启「长文本模式」则把完整示例提示词整段写入项目 visualStyle（可能与分镜资产描述重复，请按需使用）。自定义则完全由你填写。
        </p>
        <div className="theme-pills" role="group" aria-label="视觉风格来源">
          <button
            type="button"
            className={`pill${visualStyleMode === 'preset' ? ' active' : ''}`}
            onClick={() =>
              patch({
                visualStyleMode: 'preset',
                visualStylePresetId: visualStylePresetId ?? DEFAULT_PRESET_ID,
              })
            }
          >
            系统风格库
          </button>
          <button
            type="button"
            className={`pill${visualStyleMode === 'custom' ? ' active' : ''}`}
            onClick={() => patch({ visualStyleMode: 'custom' })}
          >
            自定义
          </button>
        </div>

        {visualStyleMode === 'custom' ? (
          <label className="style-custom-field">
            <span className="label">自定义视觉风格描述</span>
            <textarea
              className="style-custom-textarea"
              rows={4}
              value={customVisualStyle}
              placeholder="例如：电影感写实、国风、赛博朋克…"
              onChange={(e) => patch({ visualStyleMode: 'custom', customVisualStyle: e.target.value })}
            />
          </label>
        ) : (
          <>
            <div className="style-preset-write-mode">
              <p className="style-preset-write-label">预设写入方式</p>
              <p className="hint muted">短描述推荐用于后端 keyframe 等模板；长文本模式将「完整示例提示词」整段作为默认视觉风格。</p>
              <div className="theme-pills" role="group" aria-label="预设写入方式">
                <button
                  type="button"
                  className={`pill${!visualStyleLongTextMode ? ' active' : ''}`}
                  onClick={() => patch({ visualStyleLongTextMode: false })}
                >
                  短描述
                </button>
                <button
                  type="button"
                  className={`pill${visualStyleLongTextMode ? ' active' : ''}`}
                  onClick={() => patch({ visualStyleLongTextMode: true })}
                >
                  长文本模式
                </button>
              </div>
            </div>
            <div className="style-library-wrap">
            {presetsByCategory.map(({ category, presets }) => (
              <div key={category} className="style-category-block">
                <h4 className="style-category-title">{category}</h4>
                <div className="style-preset-grid">
                  {presets.map((p) => {
                    const selected = visualStylePresetId === p.id && visualStyleMode === 'preset'
                    return (
                      <div key={p.id} className={`style-preset-card${selected ? ' selected' : ''}`}>
                        <div className="style-preset-head">
                          <strong>{p.name}</strong>
                          <button
                            type="button"
                            className="pill small"
                            onClick={() => patch({ visualStyleMode: 'preset', visualStylePresetId: p.id })}
                          >
                            选用
                          </button>
                        </div>
                        <p className="style-preset-traits muted">{p.traits}</p>
                        <details className="style-preset-details">
                          <summary>完整示例提示词</summary>
                          <pre className="style-prompt-pre">{p.fullPrompt}</pre>
                          <button type="button" className="pill small style-copy-btn" onClick={() => void copyFullPrompt(p.fullPrompt)}>
                            复制完整提示词
                          </button>
                        </details>
                      </div>
                    )
                  })}
                </div>
              </div>
            ))}
            </div>
          </>
        )}
      </div>

      <div className="setting-item">
        <p>剧本类型</p>
        <p className="hint muted">剧情演绎偏叙事与镜头；真人解说偏口播、字幕与 B-roll 节奏。</p>
        <div className="theme-pills" role="group" aria-label="剧本类型">
          {SCRIPT_TYPE_OPTIONS.map((opt) => (
            <button key={opt} type="button" className={`pill${scriptType === opt ? ' active' : ''}`} onClick={() => patch({ scriptType: opt })}>
              {opt}
            </button>
          ))}
        </div>
      </div>

      <div className="setting-item">
        <p>模型策略</p>
        <p className="hint muted">省钱优先倾向调用成本更低的模型组合；画质优先倾向高规格出图/出视频。</p>
        <div className="theme-pills" role="group" aria-label="模型策略">
          {STRATEGY_OPTIONS.map((opt) => (
            <button key={opt} type="button" className={`pill${modelStrategy === opt ? ' active' : ''}`} onClick={() => patch({ modelStrategy: opt })}>
              {opt}
            </button>
          ))}
        </div>
      </div>

      <div className="setting-item">
        <p>创作模式</p>
        <p className="hint muted">生视频模式侧重直接成片；多参数生视频可分别调节运镜、时长、参考图等参数。</p>
        <div className="theme-pills" role="group" aria-label="创作模式">
          {CREATION_OPTIONS.map((opt) => (
            <button key={opt} type="button" className={`pill${creationMode === opt ? ' active' : ''}`} onClick={() => patch({ creationMode: opt })}>
              {opt}
            </button>
          ))}
        </div>
      </div>

      <div className="setting-item">
        <p>分镜生成</p>
        <p className="hint muted">单：一镜一图；九宫格机位：一镜多机位/多构图参考。</p>
        <div className="theme-pills" role="group" aria-label="分镜生成">
          {STORYBOARD_OPTIONS.map((opt) => (
            <button key={opt} type="button" className={`pill${storyboardLayout === opt ? ' active' : ''}`} onClick={() => patch({ storyboardLayout: opt })}>
              {opt}
            </button>
          ))}
        </div>
      </div>
    </section>
  )
}
