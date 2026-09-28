<script setup>
/**
 * AppProgress — thanh tiến độ xác định (deterministic).
 * Props: value (0-100), max (100), color (default|secondary|tertiary|quaternary|success), label.
 *
 * Lưu ý: `value` được kẹp trong [0, max] trước khi tính %, nên truyền quá biên
 * cũng không làm thanh tràn. `role="progressbar"` cùng aria-valuenow/valuemax
 * giúp screen reader đọc đúng. Primitive thuần trình bày.
 */
import { computed } from 'vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 100 },
  color: { type: String, default: '' }, // ''|secondary|tertiary|quaternary|success
  label: { type: String, default: '' },
})

// Kẹp giá trị về 0..100% để tránh thanh vượt biên khi value/max bất thường.
const pct = computed(() => Math.max(0, Math.min(100, (props.value / props.max) * 100)))

// Chỉ thêm class màu khi có prop; để rỗng thì dùng màu mặc định của design system.
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
