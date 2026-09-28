<script setup>
/**
 * AppAvatar — avatar hình tròn, có ảnh hoặc fallback chữ viết tắt.
 *
 * Hợp đồng: `src` có ảnh thì render `<img>` (alt lấy `alt` rồi tới `name`);
 * không có `src` thì hiển thị chữ viết tắt suy ra từ `name`. `size`
 * (''|sm|lg|xl) và `color` (''|accent|tertiary|quaternary) chỉ đổi class
 * modifier — để rỗng thì dùng mặc định của design system.
 *
 * Lưu ý: `title` gắn `name || alt` nên khi cả hai rỗng sẽ là `undefined` (không
 * render tooltip rỗng). Primitive thuần trình bày, không fetch/store/routing.
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

// Chữ viết tắt: lấy 2 ký tự đầu của 2 từ đầu, hoặc 1 ký tự nếu chỉ có 1 từ.
const initials = computed(() => {
  const parts = props.name.trim().split(/\s+/)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return props.name.trim().slice(0, 1).toUpperCase() || '?'
})

// Chỉ thêm class modifier khi prop được set, tránh class rỗng thừa.
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
