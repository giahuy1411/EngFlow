<template>
  <span aria-hidden="true" :class="classList" />
</template>

<script setup>
/*
 * DecoShape — hoạ tiết hình học trang trí (hiện tại là hình TRÒN) đặt lệch trong
 * một khối cha `position: relative`; dùng để "làm dày" nền theo phong cách
 * Playful Geometric. Luôn aria-hidden vì thuần trang trí.
 *
 * Props:
 *   - variant (String): màu, một trong 'tertiary' | 'secondary' | 'accent' |
 *                       'quaternary'; mặc định 'tertiary'.
 *   - size    (String): kích thước, 'sm' (48px) | 'md' (88px) | 'lg' (144px);
 *                       mặc định 'md'.
 *
 * Không slot, không emit. classList ghép ba class: 'geoshape' (định vị + viền +
 * opacity 0.4) và hai class biến thể `geoshape--{variant}` / `geoshape--{size}`
 * khai báo trong <style scoped>.
 *
 * LƯU Ý: mô tả ban đầu của task nói prop tên `kind` (circle/triangle/...) nhưng
 * code thực tế dùng `variant` + `size` và chỉ vẽ hình tròn — không có tam giác.
 */
import { computed } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'tertiary' },
  size: { type: String, default: 'md' },
})
const classList = computed(() => ['geoshape', `geoshape--${props.variant || 'tertiary'}`, `geoshape--${props.size || 'md'}`].join(' '))
</script>

<style scoped>
.geoshape {
  position: absolute;
  pointer-events: none;
  border-radius: 50%;
  border: var(--geo-border-width) solid var(--geo-fg);
  opacity: 0.4;
}
.geoshape--tertiary-sm { width: 48px; height: 48px; background: var(--geo-tertiary); }
.geoshape--tertiary-md { width: 88px; height: 88px; background: var(--geo-tertiary); }
.geoshape--tertiary-lg { width: 144px; height: 144px; background: var(--geo-tertiary); }
.geoshape--secondary-sm { width: 48px; height: 48px; background: var(--geo-secondary); }
.geoshape--secondary-md { width: 88px; height: 88px; background: var(--geo-secondary); }
.geoshape--secondary-lg { width: 144px; height: 144px; background: var(--geo-secondary); }
.geoshape--accent-sm { width: 48px; height: 48px; background: var(--geo-accent); }
.geoshape--accent-md { width: 88px; height: 88px; background: var(--geo-accent); }
.geoshape--accent-lg { width: 144px; height: 144px; background: var(--geo-accent); }
.geoshape--quaternary-sm { width: 48px; height: 48px; background: var(--geo-quaternary); }
.geoshape--quaternary-md  { width: 88px; height: 88px; background: var(--geo-quaternary); }
.geoshape--quaternary-lg  { width: 144px; height: 144px; background: var(--geo-quaternary); }

@media (prefers-reduced-motion: reduce) {
  .geoshape { opacity: 0.25; }
}
</style>