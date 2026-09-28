<script setup>
/*
 * AppErrorState — primitive UI hiển thị trạng thái LỖI (tải dữ liệu thất bại...).
 *
 * Thuộc nhóm primitive trong components/ui/ nên theo contract ở ui/index.js:
 * KHÔNG gọi API, KHÔNG import store, KHÔNG biết gì về routing — thuần trình bày.
 *
 * Props:
 *   - title       (String): tiêu đề lỗi, không truyền thì ẩn.
 *   - description (String): mô tả/chi tiết lỗi, không truyền thì ẩn.
 *
 * Slots:
 *   - visual : hình/icon minh hoạ phía trên.
 *   - action : hành động khắc phục (ví dụ nút "Thử lại"), render khi có slot.
 *
 * Không emit sự kiện. Khác AppEmptyState ở chỗ dùng biến thể .app-state--error
 * (viền/nền đỏ) và role="alert" để screen reader đọc ngay khi xuất hiện.
 */
import { computed } from 'vue'
const props = defineProps({
  title: { type: String, default: undefined },
  description: { type: String, default: undefined },
})
const classes = computed(() => ['app-state', { 'app-state--error': true }])
</script>
<template>
  <div :class="classes" role="alert">
    <slot name="visual" />
    <h3 v-if="title" class="app-state__title">{{ title }}</h3>
    <p v-if="description" class="app-state__desc">{{ description }}</p>
    <div v-if="$slots.action" class="app-state__action"><slot name="action" /></div>
  </div>
</template>
