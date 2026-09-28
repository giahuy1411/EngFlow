<script setup>
/**
 * AppButton — primitive hành động cốt lõi.
 * Variants: primary | secondary | tertiary | danger | ghost (map sang class `.app-btn--*`)
 * Sizes: sm | md | lg
 * Chỉ emit `click` khi đang bật và không loading.
 *
 * Component đa hình qua prop `as`: mặc định `'button'`; đặt `as="a"` để render
 * thẻ anchor (truyền `href`), `as="router-link"` để render RouterLink (truyền
 * `to`). Forward các attribute native an toàn (aria-*, disabled, type).
 * Icon trang trí nên đặt aria-hidden qua slot leading/trailing.
 *
 * `withArrow` thêm affordance ArrowRight-trong-vòng-tròn-trắng của design system.
 * Đây là tùy chọn (opt-in) chứ không mặc định: ~100 call site hiện hữu không
 * được đổi hình dạng, và mũi tên chỉ là gợi ý trang trí nên luôn `aria-hidden`.
 */
import { computed, useAttrs } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight } from 'lucide-vue-next'

const props = defineProps({
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  type: { type: String, default: 'button' },
  as: { type: [String, Object, Function], default: 'button' },
  href: { type: String, default: undefined },
  to: { type: [String, Object], default: undefined },
  withArrow: { type: Boolean, default: false },
})

const emit = defineEmits(['click'])

const attrs = useAttrs()

const classes = computed(() => [
  'app-btn',
  `app-btn--${props.variant}`,
  `app-btn--${props.size}`,
  {
    'app-btn--loading': props.loading,
    'app-btn--disabled': props.disabled || props.loading,
  },
])

// Trạng thái vô hiệu gộp cả `disabled` lẫn `loading` — nút đang tải không bấm được.
const isDisabled = computed(() => props.disabled || props.loading)
const isNativeButton = computed(() => props.as === 'button')
// `router-link` (chuỗi) phải đổi thành component RouterLink thật; còn lại giữ nguyên.
const componentType = computed(() => props.as === 'router-link' ? RouterLink : props.as)
const componentProps = computed(() => {
  const values = {
    ...attrs,
    'aria-disabled': isDisabled.value || undefined,
    'aria-busy': props.loading || undefined,
  }

  // Chỉ gắn thuộc tính theo đúng loại thẻ đang render để tránh attr rác/không hợp lệ.
  if (isNativeButton.value) {
    values.type = props.type
    values.disabled = isDisabled.value
  } else if (props.as === 'a') {
    values.href = props.href
  } else if (props.as === 'router-link') {
    values.to = props.to
  }

  return values
})

// Chặn emit click khi disabled/loading — kể cả với anchor không có cơ chế disabled gốc.
function handleClick(event) {
  if (!isDisabled.value) emit('click', event)
}
</script>

<template>
  <component
    :is="componentType"
    :class="classes"
    v-bind="componentProps"
    @click="handleClick"
  >
    <span v-if="loading" class="app-btn__spinner" aria-hidden="true" />
    <span class="app-btn__label">
      <slot />
    </span>
    <span v-if="withArrow && !loading" class="app-btn__arrow" aria-hidden="true">
      <ArrowRight class="app-btn__arrow-icon" />
    </span>
  </component>
</template>
