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
      // audit-v17 F-17-01: 'bg-accent text-white' measured 4.23:1 — below the 4.5:1
      // WCAG 1.4.3 floor for body text, and it bypassed the audit-v11 F132 ink/strong
      // layer that every other call site goes through. --geo-accent-strong (#7C3AED) is
      // 5.70:1 under white text and keeps the same violet identity.
      case 'success': return 'bg-accent-strong text-white'
      case 'error': return 'bg-danger text-danger-fg'
      case 'info': return 'bg-card text-foreground'
      default: return 'bg-card text-foreground'
    }
  }

  return { toasts, add, remove, success, error, info, toastBackground, showError: error, showSuccess: success }
}



