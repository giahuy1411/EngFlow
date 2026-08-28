<script setup>
/**
 * AppToast — transient feedback surface.
 * Emits `close` with the toast id for consumers managing the list.
 */
import { computed } from 'vue'

const props = defineProps({
  id: { type: [Number, String], default: undefined },
  type: { type: String, default: 'info' },
  message: { type: String, required: true },
  dismissible: { type: Boolean, default: true },
})

const emit = defineEmits(['close'])

const role = computed(() => (props.type === 'error' ? 'alert' : 'status'))
const classes = computed(() => ['app-toast', `app-toast--${props.type}`])
</script>

<template>
  <div :class="classes" :role="role" class="app-toast">
    <div class="app-toast__body">
      <slot name="icon" />
      <p class="app-toast__message">{{ message }}</p>
    </div>
    <button
      v-if="dismissible"
      type="button"
      class="app-toast__close"
      aria-label="Đóng thông báo"
      @click="emit('close', id)"
    >
      ×
    </button>
  </div>
</template>
