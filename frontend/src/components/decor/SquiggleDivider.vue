<template>
  <div class="squiggle-divider" :style="styleVars" aria-hidden="true" />
</template>

<script setup>
/**
 * SquiggleDivider — vạch ngăn kiểu nét vẽ tay của design system.
 *
 * audit-v10 sửa hai lỗi thật ở phiên bản trước:
 *  1. `color` và `height` được khai báo nhưng không dùng: stroke của SVG bị
 *     hardcode `%231E293B` và `--deco-height` không được set ở đâu cả.
 *  2. Object style ghi cả một *khai báo* CSS (`'color:var(--geo-fg)'`) vào một
 *     custom property — không phải giá trị hợp lệ để `background-image` dùng.
 *
 * Nay squiggle được tô bằng `mask-image` + `background-color`, không dùng
 * `background-image` màu. Lý do: `currentColor` bên trong data-URI SVG KHÔNG kế
 * thừa từ phần tử host (SVG là document riêng, `color` khởi tạo là đen), nên
 * background-image màu không bao giờ nhận được từ prop. Mask tách hình khỏi
 * màu, nhờ đó prop `color` thật sự chạm tới pixel.
 */
import { computed } from 'vue'

const props = defineProps({
  color: { type: String, default: 'var(--geo-fg)' },
  height: { type: String, default: '16px' },
})

const styleVars = computed(() => ({
  '--deco-squiggle-color': props.color,
  '--deco-height': props.height,
}))
</script>

<style scoped>
.squiggle-divider {
  width: 100%;
  height: var(--deco-height, 16px);
  opacity: 0.25;
  background-color: var(--deco-squiggle-color, var(--geo-fg));
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg width='100' height='16' viewBox='0 0 100 16' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 8 Q25 0 50 8 T100 8' fill='none' stroke='%23000' stroke-width='2'/%3E%3C/svg%3E");
  mask-image: url("data:image/svg+xml,%3Csvg width='100' height='16' viewBox='0 0 100 16' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 8 Q25 0 50 8 T100 8' fill='none' stroke='%23000' stroke-width='2'/%3E%3C/svg%3E");
  -webkit-mask-repeat: repeat-x;
  mask-repeat: repeat-x;
  -webkit-mask-size: 100px 16px;
  mask-size: 100px 16px;
}
</style>
