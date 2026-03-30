<script setup lang="ts">
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

const label = computed(() => {
  switch (themeStore.theme) {
    case 'light':
      return '切换到深色主题'
    case 'dark':
      return '切换到 OneLink 风格'
    case 'onelink':
      return '切换到浅色主题'
    default:
      return '切换主题'
  }
})
</script>

<template>
  <button
    class="theme-toggle"
    type="button"
    :aria-label="label"
    :title="label"
    @click="themeStore.toggle()"
  >
    <span v-show="themeStore.theme === 'light'" class="icon" aria-hidden="true">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path
          d="M21 14.5A8.5 8.5 0 0 1 9.5 3a8.5 8.5 0 1 0 11.5 11.5Z"
          stroke="currentColor"
          stroke-width="1.75"
          stroke-linejoin="round"
        />
      </svg>
    </span>
    <span v-show="themeStore.theme === 'dark'" class="icon" aria-hidden="true">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path
          d="M9 9.5 12 2l3 7.5L22 12l-7 2.5L12 22l-3-7.5L2 12l7-2.5Z"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linejoin="round"
        />
      </svg>
    </span>
    <span v-show="themeStore.theme === 'onelink'" class="icon" aria-hidden="true">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="4" stroke="currentColor" stroke-width="1.75" />
        <path
          d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"
          stroke="currentColor"
          stroke-width="1.75"
          stroke-linecap="round"
        />
      </svg>
    </span>
  </button>
</template>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: var(--tint-primary-08);
  color: var(--text-main);
  cursor: pointer;
  transition:
    background var(--duration-fast),
    border-color var(--duration-fast),
    color var(--duration-fast),
    transform var(--duration-fast);
}

.theme-toggle:hover {
  background: var(--tint-primary-14);
  border-color: color-mix(in srgb, var(--primary) 35%, var(--line));
}

.theme-toggle:active {
  transform: scale(0.96);
}

.icon {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
