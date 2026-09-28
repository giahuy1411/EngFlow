<script setup>
/**
 * StickerCard — primitive bề mặt kiểu sticker của design system.
 *
 * Hợp đồng: mặc định không tương tác; `interactive` bật hiệu ứng nâng lên khi
 * hover (hover-lift); `featured` áp bóng cứng màu hồng của design system.
 * `as` cho phép đổi thẻ bọc (div/article/router-link...) — giống AppButton,
 * component được render động qua `<component :is="as">`.
 * Slot: `icon` (phía trên), mặc định (thân card), `footer`.
 * Primitive thuần trình bày, không fetch/store/routing.
 */
import { computed } from 'vue'

const props = defineProps({
  as: { type: String, default: 'div' },
  interactive: { type: Boolean, default: false },
  featured: { type: Boolean, default: false },
})

const classes = computed(() => [
  'app-card',
  { 'app-card--interactive': props.interactive },
  { 'app-card--featured': props.featured },
])
</script>

<template>
  <component :is="as" :class="classes">
    <slot name="icon" />
    <div class="app-card__body">
      <slot />
    </div>
    <slot name="footer" />
  </component>
</template>
