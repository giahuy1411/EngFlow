<template>
  <component :is="tag" class="geo-container" :data-size="size">
    <slot />
  </component>
</template>

<script setup>
/**
 * Container — khung bọc nội dung canh giữa theo bề rộng chuẩn.
 *
 * audit-v10: bề rộng mặc định giờ là `max-w-6xl` (72rem / 1152px) như prompt,
 * giảm từ 80rem (1280px / `max-w-7xl`).
 *
 * `xl` (96rem) giữ lại cho bảng admin rộng, vốn thật sự cần thêm cột; `sm`
 * (40rem) dành cho form auth. Chỗ nào cần giữ bề rộng rộng rãi cũ thì phải
 * chọn `size="xl"` tường minh thay vì dựa vào mặc định.
 */
defineProps({
  tag: { type: String, default: 'div' },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'xl'].includes(v),
  },
})
</script>

<style scoped>
.geo-container {
  /* Prompt: "Container: max-w-6xl (Generous width)". */
  max-width: 72rem;
  margin-left: auto;
  margin-right: auto;
  padding-left: 1.5rem;
  padding-right: 1.5rem;
}
.geo-container[data-size~='sm'] { max-width: 40rem; }
.geo-container[data-size~='xl'] { max-width: 96rem; }
</style>
