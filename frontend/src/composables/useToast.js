import { reactive } from 'vue'

const toasts = reactive([])
let counter = 0

export function useToast() {
  function add(message, type = 'info', duration = 4000) {
    const id = ++counter
    toasts.push({ id, message, type })
    const safeDuration = Math.max(1000, duration)
    setTimeout(() => remove(id), safeDuration)
    return id
  }

  function remove(id) {
    const idx = toasts.findIndex(t => t.id === id)
    if (idx !== -1) toasts.splice(idx, 1)
  }

  function success(message, duration = 4000) {
    return add(message, 'success', duration)
  }

  function error(message, duration = 4000) {
    return add(message, 'error', duration)
  }

  function info(message, duration = 4000) {
    return add(message, 'info', duration)
  }

  function toastBackground(type) {
    switch (type) {
      case 'success': return 'bg-accent text-white'
      case 'error': return 'bg-danger text-danger-fg'
      case 'info': return 'bg-card text-foreground'
      default: return 'bg-card text-foreground'
    }
  }

  return { toasts, add, remove, success, error, info, toastBackground, showError: error, showSuccess: success }
}



