<script setup lang="ts">
import { useRouter } from 'vue-router'
import FeatureCard from '@/components/home/FeatureCard.vue'
import HeroSection from '@/components/home/HeroSection.vue'

const router = useRouter()

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
</script>

<template>
  <section class="home-page">
    <HeroSection :features="features" @start="router.push('/workspace')" />
    <section class="value-section" aria-labelledby="value-heading">
      <header class="value-head">
        <p id="value-heading" class="eyebrow">Why this product</p>
        <h2 class="value-title">为创作流程而设计</h2>
        <p class="value-lead muted">更少干扰，更专注在提示、生成与迭代上。</p>
      </header>
      <div class="cards">
        <FeatureCard
          v-for="item in cards"
          :key="item.title"
          :title="item.title"
          :desc="item.desc"
          :index="item.index"
        />
      </div>
    </section>
  </section>
</template>

<style scoped>
.home-page {
  display: grid;
  gap: var(--space-3xl);
}

.value-section {
  display: grid;
  gap: var(--space-xl);
}

.value-head {
  max-width: 36rem;
}

.eyebrow {
  margin: 0 0 var(--space-sm);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.value-title {
  margin: 0 0 var(--space-sm);
  font-family: var(--font-display);
  font-size: clamp(1.5rem, 3vw, 2rem);
  font-weight: 600;
  letter-spacing: 0.01em;
  line-height: 1.25;
}

.value-lead {
  margin: 0;
  font-size: 1rem;
  line-height: 1.65;
}

.cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-lg);
}

@media (max-width: 900px) {
  .home-page {
    gap: var(--space-2xl);
  }

  .cards {
    grid-template-columns: 1fr;
  }
}
</style>
