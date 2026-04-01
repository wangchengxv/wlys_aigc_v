<script setup lang="ts">
import { computed } from 'vue'
import AppInput from '@/components/common/AppInput.vue'
import { groupPresetsByCategory } from '@/data/videoStylePresets'
import { useToast } from '@/composables/useToast'
import { useGlobalSettingsStore } from '@/stores/globalSettings'
import type {
  GlobalAspectRatio,
  GlobalCreationMode,
  GlobalModelStrategy,
  GlobalScriptType,
  GlobalStoryboardLayout,
} from '@/types'

const toast = useToast()
const store = useGlobalSettingsStore()
const presetsByCategory = groupPresetsByCategory()

const s = computed(() => store.snapshot)

const ASPECT_OPTIONS: GlobalAspectRatio[] = ['16:9', '9:16', '4:3', '3:4', '1:1', '21:9']
const SCRIPT_TYPE_OPTIONS: GlobalScriptType[] = ['剧情演绎', '真人解说']
const STRATEGY_OPTIONS: GlobalModelStrategy[] = ['省钱优先', '画质优先']
const CREATION_OPTIONS: GlobalCreationMode[] = ['生视频模式', '多参数生视频']
const STORYBOARD_OPTIONS: GlobalStoryboardLayout[] = ['单', '九宫格机位']
const DURATION_OPTIONS: number[] = [5, 10, 15, 20]

async function copyFullPrompt(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    toast.showToast('完整示例提示词已复制', 'success')
  } catch {
    toast.showToast('复制失败，请手动复制', 'error')
  }
}
</script>

<template>
  <section class="panel glass settings-page global-settings-page">
    <div class="setting-item">
      <div class="actions">
        <button type="button" class="pill" @click="store.reset()">恢复默认</button>
      </div>
      <p class="hint muted">
        以下选项保存在本机浏览器。新建剧本工程时默认采用画面比例与视觉风格；开启长文本模式会将完整提示词写入项目。
      </p>
    </div>

    <div class="setting-item">
      <p>画面比例</p>
      <div class="theme-pills">
        <button
          v-for="opt in ASPECT_OPTIONS"
          :key="opt"
          type="button"
          class="pill"
          :class="{ active: s.aspectRatio === opt }"
          @click="store.patch({ aspectRatio: opt })"
        >
          {{ opt }}
        </button>
      </div>
    </div>

    <div class="setting-item">
      <p>生成视频时长</p>
      <div class="theme-pills">
        <button
          v-for="opt in DURATION_OPTIONS"
          :key="opt"
          type="button"
          class="pill"
          :class="{ active: s.targetDurationSec === opt }"
          @click="store.patch({ targetDurationSec: opt })"
        >
          {{ opt }}S
        </button>
      </div>
      <AppInput
        :model-value="s.targetDurationSec"
        label="自定义（秒）"
        type="number"
        :min="1"
        :max="600"
        @update:model-value="store.patch({ targetDurationSec: Number($event) })"
      />
    </div>

    <div class="setting-item">
      <p>视觉风格</p>
      <div class="theme-pills">
        <button type="button" class="pill" :class="{ active: s.visualStyleMode === 'preset' }" @click="store.patch({ visualStyleMode: 'preset' })">
          系统风格库
        </button>
        <button type="button" class="pill" :class="{ active: s.visualStyleMode === 'custom' }" @click="store.patch({ visualStyleMode: 'custom' })">
          自定义
        </button>
      </div>
      <AppInput
        v-if="s.visualStyleMode === 'custom'"
        :model-value="s.customVisualStyle"
        label="自定义视觉风格描述"
        as="textarea"
        :rows="3"
        @update:model-value="store.patch({ customVisualStyle: String($event), visualStyleMode: 'custom' })"
      />
      <template v-else>
        <div class="theme-pills">
          <button
            type="button"
            class="pill"
            :class="{ active: !s.visualStyleLongTextMode }"
            @click="store.patch({ visualStyleLongTextMode: false })"
          >
            短描述
          </button>
          <button
            type="button"
            class="pill"
            :class="{ active: s.visualStyleLongTextMode }"
            @click="store.patch({ visualStyleLongTextMode: true })"
          >
            长文本模式
          </button>
        </div>
        <div class="style-library-wrap">
          <div v-for="{ category, presets } in presetsByCategory" :key="category" class="style-category-block">
            <h4>{{ category }}</h4>
            <div class="style-preset-grid">
              <article v-for="p in presets" :key="p.id" class="style-preset-card" :class="{ selected: s.visualStylePresetId === p.id }">
                <div class="style-preset-head">
                  <strong>{{ p.name }}</strong>
                  <button type="button" class="pill small" @click="store.patch({ visualStyleMode: 'preset', visualStylePresetId: p.id })">
                    选用
                  </button>
                </div>
                <p class="muted">{{ p.traits }}</p>
                <details>
                  <summary>完整示例提示词</summary>
                  <pre class="style-prompt-pre">{{ p.fullPrompt }}</pre>
                  <button type="button" class="pill small" @click="copyFullPrompt(p.fullPrompt)">复制完整提示词</button>
                </details>
              </article>
            </div>
          </div>
        </div>
      </template>
    </div>

    <div class="setting-item">
      <p>剧本类型</p>
      <div class="theme-pills">
        <button
          v-for="opt in SCRIPT_TYPE_OPTIONS"
          :key="opt"
          type="button"
          class="pill"
          :class="{ active: s.scriptType === opt }"
          @click="store.patch({ scriptType: opt })"
        >
          {{ opt }}
        </button>
      </div>
    </div>

    <div class="setting-item">
      <p>模型策略</p>
      <div class="theme-pills">
        <button
          v-for="opt in STRATEGY_OPTIONS"
          :key="opt"
          type="button"
          class="pill"
          :class="{ active: s.modelStrategy === opt }"
          @click="store.patch({ modelStrategy: opt })"
        >
          {{ opt }}
        </button>
      </div>
    </div>

    <div class="setting-item">
      <p>创作模式</p>
      <div class="theme-pills">
        <button
          v-for="opt in CREATION_OPTIONS"
          :key="opt"
          type="button"
          class="pill"
          :class="{ active: s.creationMode === opt }"
          @click="store.patch({ creationMode: opt })"
        >
          {{ opt }}
        </button>
      </div>
    </div>

    <div class="setting-item">
      <p>分镜生成</p>
      <div class="theme-pills">
        <button
          v-for="opt in STORYBOARD_OPTIONS"
          :key="opt"
          type="button"
          class="pill"
          :class="{ active: s.storyboardLayout === opt }"
          @click="store.patch({ storyboardLayout: opt })"
        >
          {{ opt }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.settings-page {
  padding: var(--space-xl);
  display: grid;
  gap: var(--space-md);
}

.setting-item {
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--tint-primary-08);
  padding: var(--space-lg);
  display: grid;
  gap: var(--space-sm);
}

.theme-pills,
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.pill {
  min-height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-muted);
}

.pill.active {
  color: var(--text-main);
  background: var(--tint-primary-14);
  border-color: color-mix(in srgb, var(--primary) 45%, transparent);
}

.style-library-wrap {
  display: grid;
  gap: var(--space-md);
}

.style-preset-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

.style-preset-card {
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: var(--space-md);
}

.style-preset-card.selected {
  border-color: color-mix(in srgb, var(--primary) 45%, transparent);
}

.style-preset-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-sm);
  align-items: center;
}

.style-prompt-pre {
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
}

@media (max-width: 800px) {
  .style-preset-grid {
    grid-template-columns: 1fr;
  }
}
</style>
