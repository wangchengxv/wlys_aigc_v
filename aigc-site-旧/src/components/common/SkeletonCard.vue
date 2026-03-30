<template>
  <div class="skeleton panel">
    <span class="bar w-70"></span>
    <span class="bar w-100"></span>
    <span class="bar w-90"></span>
    <span class="bar w-50"></span>
  </div>
</template>

<style scoped>
.skeleton {
  padding: var(--space-lg);
  background: var(--tint-primary-08);
  display: grid;
  gap: var(--space-sm);
}

.bar {
  display: block;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    color-mix(in srgb, var(--primary) 12%, transparent),
    color-mix(in srgb, var(--primary) 28%, transparent),
    color-mix(in srgb, var(--primary) 12%, transparent)
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s linear infinite;
}

.w-100 {
  width: 100%;
}

.w-90 {
  width: 90%;
}

.w-70 {
  width: 70%;
}

.w-50 {
  width: 50%;
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}
</style>
