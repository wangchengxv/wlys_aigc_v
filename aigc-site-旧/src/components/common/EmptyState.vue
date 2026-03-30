<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
  }>(),
  {
    description: '',
  },
)
</script>

<template>
  <div class="empty panel glass">
    <div class="orb"></div>
    <h3>{{ title }}</h3>
    <p v-if="description" class="muted">{{ description }}</p>
    <slot />
  </div>
</template>

<style scoped>
.empty {
  padding: var(--space-xl);
  text-align: center;
  display: grid;
  gap: var(--space-sm);
  justify-items: center;
  border-style: dashed;
}

.orb {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, var(--primary), var(--secondary));
  animation: float 2.6s ease-in-out infinite;
}

h3 {
  margin: 0;
  font-size: 18px;
}

p {
  margin: 0;
}
</style>
