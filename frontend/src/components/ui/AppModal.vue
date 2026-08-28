<script setup>
/**
 * AppModal — accessible dialog with overlay + pop-in animation.
 * Use v-model:open to control visibility.
 * Triggers Escape + focus trap (focus first focusable child).
 */
import { computed, watch, onMounted, onBeforeUnmount, nextTick, ref, useSlots } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  size: { type: String, default: 'md' }, // sm | md | lg
})

const emit = defineEmits(['update:modelValue', 'close'])

const slots = useSlots()
const modalEl = ref(null)
let lastFocused = null

const sizeClass = computed(() => {
  if (props.size === 'sm') return 'max-w-sm'
  if (props.size === 'lg') return 'max-w-3xl'
  return 'max-w-xl'
})

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

function focusables() {
  const root = modalEl.value
  if (!root) return []
  return [...root.querySelectorAll(
    'a[href], button:not([disabled]), textarea, input, select, [tabindex]:not([tabindex="-1"])'
  )].filter((el) => el.offsetParent !== null || el === document.activeElement)
}

function focusFirst() {
  const list = focusables()
  if (list.length) list[0].focus()
}

function close() {
  open.value = false
  emit('close')
}

function onKey(e) {
  if (e.key === 'Escape') {
    e.preventDefault()
    close()
    return
  }
  if (e.key === 'Tab') {
    const list = focusables()
    if (list.length === 0) return
    const first = list[0]
    const last = list[list.length - 1]
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault()
      last.focus()
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault()
      first.focus()
    }
  }
}

function openModal() {
  lastFocused = document.activeElement
  document.addEventListener('keydown', onKey)
  document.body.style.overflow = 'hidden'
  nextTick(focusFirst)
}

function closeModal() {
  document.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
  if (lastFocused && lastFocused.isConnected) {
    lastFocused.focus()
    lastFocused = null
  }
}

watch(open, (v) => {
  if (v) openModal()
  else closeModal()
})

onMounted(() => {
  if (open.value) openModal()
})

onBeforeUnmount(closeModal)
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="open"
        class="app-modal-overlay"
        @click.self="close"
        role="dialog"
        aria-modal="true"
        :aria-label="title || 'Dialog'"
      >
        <div ref="modalEl" :class="['app-modal', sizeClass]">
          <div class="app-modal__header">
            <h2 class="app-modal__title" v-if="title">{{ title }}</h2>
            <slot name="header" />
            <button type="button" class="app-modal__close" aria-label="Đóng" @click="close">
              ✕
            </button>
          </div>
          <div class="app-modal__body">
            <slot />
          </div>
          <div class="app-modal__footer" v-if="slots.footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
