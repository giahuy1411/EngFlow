<script setup>
/*
 * AppEmptyState — primitive UI hiển thị trạng thái "rỗng" (không có dữ liệu).
 *
 * Thuộc nhóm primitive trong components/ui/ nên theo contract ở ui/index.js:
 * KHÔNG gọi API, KHÔNG import store, KHÔNG biết gì về routing — thuần trình bày.
 *
 * Props:
 *   - title       (String): tiêu đề, không truyền thì ẩn dòng tiêu đề.
 *   - description (String): mô tả phụ, không truyền thì ẩn.
 *
 * Slots:
 *   - visual : hình/icon minh hoạ đặt phía trên (thường là icon hoặc SVG).
 *   - action : nút/khu vực hành động, chỉ render khi slot được truyền.
 *
 * Không emit sự kiện nào. Style dùng class hệ thống .app-state (+ biến thể).
 */
import { computed } from 'vue'
const props = defineProps({
  title: { type: String, default: undefined },
  description: { type: String, default: undefined },
})
const classes = computed(() => ['app-state', { 'app-state--empty': true }])
</script>
<template>
  <div :class="classes" role="region" aria-labelledby="app-empty-title">
    <slot name="visual" />
    <h3 v-if="title" id="app-empty-title" class="app-state__title">{{ title }}</h3>
    <p v-if="description" class="app-state__desc">{{ description }}</p>
    <div v-if="$slots.action" class="app-state__action"><slot name="action" /></div>
  </div>
</template>
