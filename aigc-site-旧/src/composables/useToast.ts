import { computed, ref } from 'vue'

type ToastType = 'success' | 'error' | 'info'

const visible = ref(false)
const message = ref('')
const type = ref<ToastType>('info')
let timer: number | null = null

export function useToast() {
  function showToast(text: string, toastType: ToastType = 'info', duration = 2000) {
    message.value = text
    type.value = toastType
    visible.value = true
    if (timer) {
      window.clearTimeout(timer)
    }
    timer = window.setTimeout(() => {
      visible.value = false
      timer = null
    }, duration)
  }

  function hideToast() {
    visible.value = false
    if (timer) {
      window.clearTimeout(timer)
      timer = null
    }
  }

  return {
    visible: computed(() => visible.value),
    message: computed(() => message.value),
    type: computed(() => type.value),
    showToast,
    hideToast,
  }
}
