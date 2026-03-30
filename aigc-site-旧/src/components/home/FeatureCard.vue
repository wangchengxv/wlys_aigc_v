<script setup lang="ts">
defineProps<{
  title: string
  desc: string
  index: string
}>()
</script>

<template>
  <article class="feature-card panel glass">
    <span class="index" aria-hidden="true">{{ index }}</span>
    <h3>{{ title }}</h3>
    <p class="muted">{{ desc }}</p>
  </article>
</template>

<style scoped>
.feature-card {
  position: relative;
  padding: 28px 26px 30px;
  transition:
    border-color var(--duration-normal),
    box-shadow var(--duration-normal),
    transform var(--duration-normal);
  height: 100%;
  overflow: hidden;
}

.feature-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 120px;
  height: 120px;
  background: radial-gradient(circle at 100% 0%, color-mix(in srgb, var(--primary) 18%, transparent), transparent 70%);
  pointer-events: none;
  opacity: 0;
  transition: opacity var(--duration-normal);
}

.feature-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--primary) 42%, var(--line));
  box-shadow:
    var(--shadow-primary),
    0 0 0 1px color-mix(in srgb, var(--primary) 15%, transparent);
}

.feature-card:hover::after {
  opacity: 1;
}

.index {
  display: block;
  font-family: var(--font-display);
  font-size: 2.5rem;
  font-weight: 600;
  line-height: 1;
  color: color-mix(in srgb, var(--primary) 35%, var(--text-muted));
  margin-bottom: 14px;
  letter-spacing: -0.02em;
}

h3 {
  margin: 0 0 12px;
  font-size: 1.125rem;
  font-weight: 600;
  letter-spacing: 0.01em;
}

p {
  margin: 0;
  line-height: 1.72;
  font-size: 0.9375rem;
}
</style>
