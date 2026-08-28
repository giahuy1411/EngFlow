<script setup>
/**
 * AppButton — core action primitive.
 * Variants: primary | secondary | tertiary | danger | ghost
 * Sizes: sm | md | lg
 * Emits click only when enabled and not loading.
 * Renders an anchor when as="a" is set (router-aware consumers pass href).
 * Forwards safe native attributes (aria-*, disabled, type).
 * Decorative icons should use aria-hidden via the leading/trailing slots.
 */
import { computed, useAttrs } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  type: { type: String, default: 'button' },
  as: { type: [String, Object, Function], default: 'button' },
  href: { type: String, default: undefined },
  to: { type: [String, Object], default: undefined },
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

const isDisabled = computed(() => props.disabled || props.loading)
const isNativeButton = computed(() => props.as === 'button')
const componentType = computed(() => props.as === 'router-link' ? RouterLink : props.as)
const componentProps = computed(() => {
  const values = {
    ...attrs,
    'aria-disabled': isDisabled.value || undefined,
    'aria-busy': props.loading || undefined,
  }

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
  </component>
</template>
