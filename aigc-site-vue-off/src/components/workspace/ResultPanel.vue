<script setup lang="ts">
import { ref } from 'vue'
import AppButton from '@/components/common/AppButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import TextResultCard from '@/components/workspace/TextResultCard.vue'
import ImageResultCard from '@/components/workspace/ImageResultCard.vue'
import VideoResultCard from '@/components/workspace/VideoResultCard.vue'
import { useToast } from '@/composables/useToast'
import { useGenerationStore } from '@/stores/generation'

const store = useGenerationStore()
const toast = useToast()
const previewImage = ref('')
const previewVideo = ref('')
const previewVideoError = ref(false)

async function copyText(text: string) {
  await navigator.clipboard.writeText(text)
  toast.showToast('文案已复制', 'success')
}

async function copyLink(url: string) {
  await navigator.clipboard.writeText(url)
  toast.showToast('链接已复制', 'success')
}

function downloadImage(url: string) {
  downloadUrl(url, 'jpg')
  toast.showToast('图片已开始下载', 'info')
}

async function downloadVideo(url: string) {
  toast.showToast('正在准备视频下载', 'info')
  const filename = `aigc-${Date.now()}.mp4`
  const ok = await downloadVideoWithBrowser(url, filename)
  if (ok) {
    toast.showToast('视频已开始下载', 'success')
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
  toast.showToast('浏览器已打开视频页面，请使用浏览器保存', 'info')
}

function openVideo(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')
}

function openVideoPreview(url: string) {
  previewVideoError.value = false
  previewVideo.value = url
}

function downloadUrl(url: string, ext: string) {
  const a = document.createElement('a')
  a.href = url
  a.download = `aigc-${Date.now()}.${ext}`
  a.click()
}

async function downloadVideoWithBrowser(url: string, filename: string) {
  try {
    const res = await fetch(url)
    if (!res.ok) {
      return false
    }
    const blob = await res.blob()
    if (!blob.size) {
      return false
    }
    const objectUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = objectUrl
    a.download = filename
    a.click()
    setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
    return true
  } catch {
    return false
  }
}

function toggleFavorite() {
  if (!store.currentTask) return
  store.toggleFavorite(store.currentTask.taskId)
  const msg = store.favoriteSet.has(store.currentTask.taskId) ? '已加入收藏' : '已取消收藏'
  toast.showToast(msg, 'success')
}

function closePreview() {
  previewImage.value = ''
}

function closeVideoPreview() {
  previewVideo.value = ''
  previewVideoError.value = false
}
</script>

<template>
  <section class="result panel glass">
    <header class="head">
      <h3>结果展示</h3>
      <p class="muted" v-if="store.currentTask">任务：{{ store.currentTask.taskId }}</p>
    </header>

    <template v-if="store.loading">
      <LoadingSpinner />
      <div class="skeletons">
        <SkeletonCard />
        <SkeletonCard />
      </div>
    </template>

    <EmptyState
      v-else-if="!store.currentTask"
      title="还没有生成内容"
      description="在左侧输入提示词并点击“开始生成”，结果会展示在这里。"
    />

    <template v-else>
      <section v-if="store.currentTask.textResults.length" class="group">
        <p class="group-title">文案结果</p>
        <TransitionGroup name="list" tag="div" class="list">
          <TextResultCard
            v-for="(text, idx) in store.currentTask.textResults"
            :key="`${store.currentTask.taskId}-text-${idx}`"
            :text="text"
            :favorite="store.favoriteSet.has(store.currentTask.taskId)"
            @copy="copyText(text)"
            @favorite="toggleFavorite"
          />
        </TransitionGroup>
      </section>

      <section v-if="store.currentTask.imageResults.length" class="group">
        <p class="group-title">图片结果</p>
        <TransitionGroup name="list" tag="div" class="gallery">
          <ImageResultCard
            v-for="url in store.currentTask.imageResults"
            :key="url"
            :url="url"
            @preview="previewImage = url"
            @download="downloadImage(url)"
          />
        </TransitionGroup>
      </section>

      <section v-if="store.currentTask.videoResults.length" class="group">
        <p class="group-title">视频结果</p>
        <TransitionGroup name="list" tag="div" class="list">
          <VideoResultCard
            v-for="url in store.currentTask.videoResults"
            :key="url"
            :url="url"
            @preview="openVideoPreview(url)"
            @download="downloadVideo(url)"
            @copy-link="copyLink(url)"
            @open="openVideo(url)"
          />
        </TransitionGroup>
      </section>
    </template>
  </section>

  <Teleport to="body">
    <div v-if="previewImage" class="lightbox" @click.self="closePreview">
      <figure class="lightbox-card">
        <img :src="previewImage" alt="预览大图" />
        <figcaption>
          <AppButton size="sm" @click="downloadImage(previewImage)">下载</AppButton>
          <AppButton size="sm" @click="closePreview">关闭</AppButton>
        </figcaption>
      </figure>
    </div>
  </Teleport>

  <Teleport to="body">
    <div v-if="previewVideo" class="lightbox" @click.self="closeVideoPreview">
      <section class="lightbox-card video-lightbox">
        <video
          v-if="!previewVideoError"
          :src="previewVideo"
          controls
          autoplay
          preload="metadata"
          @error="previewVideoError = true"
        />
        <p v-else class="video-error">视频预览加载失败，请尝试“新窗口播放”或重新生成。</p>
        <div class="actions">
          <AppButton size="sm" @click="downloadVideo(previewVideo)">下载</AppButton>
          <AppButton size="sm" @click="openVideo(previewVideo)">新窗口播放</AppButton>
          <AppButton size="sm" @click="closeVideoPreview">关闭</AppButton>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.result {
  padding: var(--space-lg);
}

.head {
  margin-bottom: var(--space-md);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
}

h3 {
  margin: 0;
}

.head p {
  margin: 0;
  font-size: 13px;
}

.group + .group {
  margin-top: var(--space-xl);
}

.group-title {
  margin: 0 0 var(--space-sm);
  color: var(--primary);
}

.list,
.skeletons {
  display: grid;
  gap: var(--space-sm);
}

.gallery {
  display: grid;
  gap: var(--space-sm);
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.list-enter-active,
.list-leave-active {
  transition: all var(--duration-normal);
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.lightbox {
  position: fixed;
  inset: 0;
  z-index: var(--z-overlay);
  background: var(--overlay-scrim);
  display: grid;
  place-items: center;
  padding: var(--space-xl);
}

.lightbox-card {
  margin: 0;
  max-width: min(1000px, 95vw);
  background: var(--modal-surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
}

.lightbox-card img {
  max-width: 100%;
  display: block;
  border-radius: var(--radius-md);
}

.video-lightbox {
  width: min(1000px, 95vw);
}

.video-lightbox video {
  width: 100%;
  display: block;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: #000;
}

.video-error {
  margin: var(--space-sm) 0;
  color: var(--danger, #ff6f9f);
}

figcaption {
  margin-top: var(--space-sm);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
}

.actions {
  margin-top: var(--space-sm);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
}

@media (max-width: 600px) {
  .gallery {
    grid-template-columns: 1fr;
  }
}
</style>
