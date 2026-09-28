<script setup>
/**
 * AppTooltip — tooltip hiện khi hover/focus.
 *
 * Hợp đồng: nội dung tooltip lấy từ prop `text` hoặc slot `#tip`; slot mặc định
 * là phần tử trigger. Phần tooltip mang `role="tooltip"`.
 *
 * Lưu ý: trigger cần tự nhận focus (ví dụ là button/link) để tooltip hiện được
 * bằng bàn phím — component không tự gắn `tabindex`. Primitive thuần trình bày.
 */
defineProps({
  text: { type: String, default: '' },
})
</script>

<template>
  <span class="app-tooltip">
    <span class="app-tooltip__tip" role="tooltip">
      <slot name="tip">{{ text }}</slot>
    </span>
    <span class="app-tooltip__trigger">
      <slot />
    </span>
  </span>
</template>
