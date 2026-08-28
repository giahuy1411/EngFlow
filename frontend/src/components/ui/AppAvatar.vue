<script setup>
/**
 * AppAvatar — circular avatar with initials fallback or image.
 */
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, default: '' },
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
  size: { type: String, default: '' }, // ''|sm|lg|xl
  color: { type: String, default: '' }, // ''|accent|tertiary|quaternary
  loading: { type: String, default: 'lazy' },
})

const initials = computed(() => {
  const parts = props.name.trim().split(/\s+/)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return props.name.trim().slice(0, 1).toUpperCase() || '?'
})

const cls = computed(() => [
  'app-avatar',
  props.size ? `app-avatar--${props.size}` : '',
  props.color ? `app-avatar--${props.color}` : '',
])
</script>

<template>
  <span :class="cls" :title="name || alt || undefined">
    <img v-if="src" :src="src" :alt="alt || name" :loading="loading" />
    <template v-else>{{ initials }}</template>
  </span>
</template>
