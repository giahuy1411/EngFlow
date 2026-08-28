<script setup>
/**
 * AppProgress — deterministic progress bar.
 * Props: value (0-100), max (100), color (default|secondary|tertiary|quaternary|success), label.
 */
import { computed } from 'vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 100 },
  color: { type: String, default: '' }, // ''|secondary|tertiary|quaternary|success
  label: { type: String, default: '' },
})

const pct = computed(() => Math.max(0, Math.min(100, (props.value / props.max) * 100)))

const colorClass = computed(() => {
  if (!props.color) return ''
  return `app-progress--${props.color}`
})
</script>

<template>
  <div class="app-progress" :class="colorClass" role="progressbar" :aria-valuenow="value" :aria-valuemax="max">
    <div class="app-progress__bar" :style="{ width: pct + '%' }" />
  </div>
  <div v-if="label" class="app-form-field__hint mt-1">{{ label }}</div>
</template>
