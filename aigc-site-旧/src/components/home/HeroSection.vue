<script setup lang="ts">
import AppButton from '@/components/common/AppButton.vue'

defineProps<{
  features: string[]
}>()

const emit = defineEmits<{
  (e: 'start'): void
}>()
</script>

<template>
  <section class="hero panel glass">
    <div class="hero-bg" aria-hidden="true">
      <span class="glow g1"></span>
      <span class="glow g2"></span>
      <span class="glow g3"></span>
      <span class="grid-lines"></span>
    </div>
    <div class="left">
      <p class="badge">Creative suite</p>
      <h1>AIGC 图文生成平台</h1>
      <p class="lead muted">
        输入简单文案或上传图片，快速生成可用于发布的图片与视频内容——流程清晰、反馈即时。
      </p>
      <div class="cta-row">
        <AppButton variant="primary" class="cta" @click="emit('start')">开始创作</AppButton>
        <span class="cta-note muted">无需复杂配置，从想法到素材一步到位</span>
      </div>
    </div>
    <div class="right panel examples">
      <p class="title">Prompt ideas</p>
      <div class="example-grid">
        <div class="example-item">3D游戏人物风格，戴夸张耳饰的民族风少女</div>
        <div class="example-item">插画风格，太空飞行器在粉色星球低空飞行</div>
        <div class="example-item">未来风，女性机械人形 AI</div>
        <div class="example-item">超写实插画，夜晚月光下的花园</div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  overflow: hidden;
  padding: clamp(40px, 6vw, 72px) clamp(28px, 5vw, 56px);
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: clamp(28px, 4vw, 52px);
  min-height: min(560px, 85vh);
  align-items: center;
  border-radius: var(--radius-xl);
}

.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.grid-lines {
  position: absolute;
  inset: 0;
  opacity: 0.06;
  background-image:
    linear-gradient(var(--line) 1px, transparent 1px),
    linear-gradient(90deg, var(--line) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, #000 20%, transparent 100%);
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(88px);
  opacity: 0.22;
  animation: float 9s ease-in-out infinite;
}

.g1 {
  width: 400px;
  height: 400px;
  background: color-mix(in srgb, var(--primary) 55%, transparent);
  top: -150px;
  right: -100px;
}

.g2 {
  width: 340px;
  height: 340px;
  background: color-mix(in srgb, var(--secondary) 50%, transparent);
  bottom: -130px;
  left: -90px;
  animation-delay: 1.4s;
}

.g3 {
  width: 240px;
  height: 240px;
  background: color-mix(in srgb, var(--primary) 35%, var(--secondary) 40%, transparent);
  top: 38%;
  right: 22%;
  animation-delay: 2.6s;
}

.left,
.right {
  position: relative;
  z-index: 1;
}

.badge {
  display: inline-flex;
  margin: 0;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--primary) 45%, transparent);
  background: color-mix(in srgb, var(--primary) 12%, transparent);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: color-mix(in srgb, var(--primary) 92%, var(--text-main));
}

h1 {
  margin: 18px 0 20px;
  font-family: var(--font-display);
  font-size: clamp(2.5rem, 5.5vw, 3.75rem);
  line-height: 1.08;
  font-weight: 600;
  letter-spacing: -0.015em;
}

.lead {
  margin: 0;
  max-width: 34ch;
  font-size: 1.0625rem;
  line-height: 1.75;
}

.cta-row {
  margin-top: 32px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 24px;
}

.cta {
  padding: 15px 36px;
  font-size: 0.9375rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  border-radius: var(--radius-md);
  animation: pulseGlow 3.2s ease-in-out infinite;
}

.cta-note {
  font-size: 0.8125rem;
  max-width: 220px;
  line-height: 1.5;
}

.examples {
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 28px 26px;
  background: color-mix(in srgb, var(--bg-alt) 88%, transparent);
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: inset 0 1px 0 color-mix(in srgb, var(--text-main) 8%, transparent);
}

.title {
  margin: 0 0 22px;
  color: var(--text-main);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.example-grid {
  display: grid;
  gap: 12px;
  flex: 1;
}

.example-item {
  background: color-mix(in srgb, var(--text-main) 5%, transparent);
  border: 1px solid color-mix(in srgb, var(--text-main) 10%, transparent);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  font-size: 0.8125rem;
  line-height: 1.55;
  color: var(--text-muted);
  transition:
    background var(--duration-fast),
    border-color var(--duration-fast),
    transform var(--duration-normal);
}

.example-item:hover {
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  border-color: color-mix(in srgb, var(--primary) 28%, transparent);
  transform: translateX(3px);
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .examples {
    padding: 22px 20px;
  }

  .cta-note {
    max-width: none;
  }
}
</style>
