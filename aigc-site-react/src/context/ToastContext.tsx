import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'

export type ToastType = 'success' | 'error' | 'info'

type ToastContextValue = {
  showToast: (text: string, toastType?: ToastType, duration?: number) => void
  hideToast: () => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [visible, setVisible] = useState(false)
  const [message, setMessage] = useState('')
  const [toastType, setToastType] = useState<ToastType>('info')
  const timerRef = useRef<number | null>(null)

  const hideToast = useCallback(() => {
    setVisible(false)
    if (timerRef.current != null) {
      window.clearTimeout(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const showToast = useCallback(
    (text: string, type: ToastType = 'info', duration = 2000) => {
      setMessage(text)
      setToastType(type)
      setVisible(true)
      if (timerRef.current != null) window.clearTimeout(timerRef.current)
      timerRef.current = window.setTimeout(() => {
        setVisible(false)
        timerRef.current = null
      }, duration)
    },
    [],
  )

  return (
    <ToastContext.Provider value={{ showToast, hideToast }}>
      {children}
      {visible ? (
        <div className={`toast-host toast t-${toastType}`} role="status" aria-live="polite">
          {message}
        </div>
      ) : null}
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
