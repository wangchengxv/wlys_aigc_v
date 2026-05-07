<template>
  <section class="not-found-page">
    <p class="code">404</p>
    <h1>页面走丢了</h1>
    <p class="desc">你访问的页面不存在，可能已被移动或删除。</p>
    <RouterLink class="back-home" to="/">返回首页</RouterLink>
  </section>
</template>

<style scoped>
.not-found-page {
  min-height: 70vh;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 12px;
  text-align: center;
}

.code {
  margin: 0;
  font-size: clamp(72px, 14vw, 160px);
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(120deg, var(--primary), var(--secondary));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

h1 {
  margin: 0;
  font-size: 30px;
}

.desc {
  margin: 0;
  color: var(--text-muted);
}

.back-home {
  margin-top: 8px;
  min-height: 44px;
  padding: 10px 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  color: var(--text-main);
  text-decoration: none;
  background: linear-gradient(135deg, color-mix(in srgb, var(--primary) 30%, transparent), transparent);
}
</style>
