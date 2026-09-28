<script setup>
/*
 * AppSkeleton — primitive UI tạo khối "xương" (placeholder) nhấp nháy trong lúc
 * chờ dữ liệu, giúp giữ chỗ và giảm nhảy layout (CLS).
 *
 * Thuộc nhóm primitive trong components/ui/ nên theo contract ở ui/index.js:
 * KHÔNG gọi API, KHÔNG import store, KHÔNG biết routing — thuần trình bày.
 *
 * Props (đều là chuỗi CSS hợp lệ, ghép thẳng vào style inline):
 *   - width  (String): chiều rộng, mặc định '100%'.
 *   - height (String): chiều cao, mặc định '1rem'.
 *   - radius (String): bo góc, mặc định token 'var(--geo-radius-sm)'.
 *
 * Không slot, không emit. Hiệu ứng shimmer do class .app-skeleton đảm nhiệm;
 * phần tử được aria-hidden="true" vì chỉ là trang trí chờ tải.
 */
import { computed } from 'vue'
const props = defineProps({
  width: { type: String, default: '100%' },
  height: { type: String, default: '1rem' },
  radius: { type: String, default: 'var(--geo-radius-sm)' },
})
const style = computed(() => ({
  width: props.width,
  height: props.height,
  borderRadius: props.radius,
}))
</script>
<template>
  <div class="app-skeleton" :style="style" aria-hidden="true" />
</template>
