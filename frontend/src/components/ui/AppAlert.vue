<script setup>
/**
 * AppAlert — semantic alert surface.
 * Role mapping: success=status, warning=alert, error=alert, info=status.
 */
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'info' },
  title: { type: String, default: undefined },
  message: { type: String, default: undefined },
  dismissible: { type: Boolean, default: false },
})

const emit = defineEmits(['dismiss'])

const role = computed(() => {
  if (props.type === 'error' || props.type === 'warning') return 'alert'
  return 'status'
})

const classes = computed(() => ['app-alert', `app-alert--${props.type}`])
</script>

<template>
  <div :class="classes" :role="role" aria-live="polite">
    <div class="app-alert__body">
      <slot name="icon" />
      <div>
        <p v-if="title" class="app-alert__title">{{ title }}</p>
        <p v-if="message" class="app-alert__message">{{ message }}</p>
      </div>
    </div>
    <button
      v-if="dismissible"
      type="button"
      class="app-alert__close"
      aria-label="Đóng"
      @click="emit('dismiss')"
    >
      ×
    </button>
  </div>
</template>
