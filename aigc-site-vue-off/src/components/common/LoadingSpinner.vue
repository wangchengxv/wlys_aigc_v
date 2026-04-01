<template>
  <div class="spinner-wrap" aria-label="加载中">
    <span class="ring"></span>
    <span class="text">生成中，请稍候...</span>
  </div>
</template>

<style scoped>
.spinner-wrap {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--text-muted);
}

.ring {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid color-mix(in srgb, var(--primary) 22%, transparent);
  border-top-color: var(--primary);
  animation: spin 0.9s linear infinite;
}

.text {
  font-size: 13px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
