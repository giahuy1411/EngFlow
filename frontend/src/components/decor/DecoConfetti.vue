<template>
  <span aria-hidden="true" :class="['decoconfetti', `decoconfetti--${kind}`]" :style="styleVars">
    <svg v-if="kind === 'circle'" viewBox="0 0 24 24" :width="sizePx" :height="sizePx">
      <circle cx="12" cy="12" r="10" fill="currentColor" />
    </svg>
    <svg v-else-if="kind === 'square'" viewBox="0 0 24 24" :width="sizePx" :height="sizePx">
      <rect x="2" y="2" width="20" height="20" rx="2" fill="currentColor" />
    </svg>
    <svg v-else viewBox="0 0 24 24" :width="sizePx" :height="sizePx">
      <path d="M12 4 L20 20 L4 20 Z" fill="currentColor" />
    </svg>
  </span>
</template>

<script setup>
/**
 * DecoConfetti — mảnh confetti trang trí của design system Playful Geometric.
 * Thuần trình bày: không fetch dữ liệu, không store, không biết routing.
 *
 * audit-v10 sửa hai lỗi thật ở phiên bản trước:
 *  1. `sizePx` đọc từ `const sizeMap.md` cấp module — mọi confetti đều render
 *     18px bất kể prop `size`, nên `sm`/`lg`/`xl` âm thầm không có tác dụng.
 *  2. `color` được khai báo nhưng không bao giờ đọc; màu lấy từ class theo
 *     `size`, tức gộp "to cỡ nào" với "màu gì". Truyền `color` không có tác dụng.
 *
 * Giờ `size` và `color` độc lập: `size` quyết định kích thước pixel, `color`
 * quyết định màu tô, mặc định theo palette xoay vòng của design system
 * (violet → pink → amber → mint) để các call site cũ giữ nguyên diện mạo.
 */
import { computed } from 'vue'

const sizeMap = { sm: 14, md: 18, lg: 24, xl: 32 }
// Palette mặc định theo size — giữ nguyên bảng màu cũ cho call site hiện hữu.
const paletteBySize = {
  sm: 'var(--geo-tertiary)',
  md: 'var(--geo-secondary)',
  lg: 'var(--geo-accent)',
  xl: 'var(--geo-quaternary)',
}

const props = defineProps({
  kind: {
    type: String,
    default: 'circle',
    validator: (v) => ['circle', 'square', 'triangle'].includes(v),
  },
  color: { type: String, default: '' },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg', 'xl'].includes(v),
  },
})

const sizePx = computed(() => sizeMap[props.size] ?? sizeMap.md)
// Màu ưu tiên: prop `color` → palette theo size → mặc định md.
const styleVars = computed(() => ({
  '--deco-confetti-color': props.color || paletteBySize[props.size] || paletteBySize.md,
}))
</script>

<style scoped>
.decoconfetti {
  position: absolute;
  pointer-events: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--deco-confetti-color, var(--geo-secondary));
}

@media (prefers-reduced-motion: reduce) {
  .decoconfetti { opacity: 0.7; }
}
</style>
