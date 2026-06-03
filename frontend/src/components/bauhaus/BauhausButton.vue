<template>
  <button
    :class="[
      'inline-flex items-center justify-center font-bold uppercase tracking-wider',
      'border-2 border-black bauhaus-btn-press focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2',
      variantClasses,
      shapeClasses,
      sizeClasses,
    ]"
    v-bind="$attrs"
  >
    <slot></slot>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'yellow', 'outline', 'ghost'].includes(value)
  },
  shape: {
    type: String,
    default: 'square',
    validator: (value) => ['square', 'pill'].includes(value)
  },
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value)
  }
})

const variantClasses = computed(() => {
  switch (props.variant) {
    case 'primary':
      return 'bg-primary-red text-white shadow-hard-sm hover:bg-primary-red/90'
    case 'secondary':
      return 'bg-primary-blue text-white shadow-hard-sm hover:bg-primary-blue/90'
    case 'yellow':
      return 'bg-primary-yellow text-black shadow-hard-sm hover:bg-primary-yellow/90'
    case 'outline':
      return 'bg-white text-black shadow-hard-sm hover:bg-gray-100'
    case 'ghost':
      return 'border-none text-black hover:bg-gray-200'
    default:
      return ''
  }
})

const shapeClasses = computed(() => {
  return props.shape === 'pill' ? 'rounded-full' : 'rounded-none'
})

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'px-4 py-1.5 text-sm'
    case 'lg':
      return 'px-8 py-4 text-lg'
    case 'md':
    default:
      return 'px-6 py-2.5 text-base'
  }
})
</script>
