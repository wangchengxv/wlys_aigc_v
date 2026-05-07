<template>
  <Transition name="page" mode="out-in">
    <slot />
  </Transition>
</template>

<style scoped>
.page-enter-active,
.page-leave-active {
  transition:
    opacity var(--duration-normal),
    transform var(--duration-normal);
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
