<template>
  <button
    :class="[
      'inline-flex items-center justify-center font-bold uppercase tracking-wider transition-all duration-200 ease-out',
      'border-2 border-black focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2',
      'active:translate-x-[2px] active:translate-y-[2px] active:shadow-none',
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
    validator: (value) => ['primary', 'secondary', 'pink', 'amber', 'emerald', 'outline', 'ghost'].includes(value)
  },
  shape: {
    type: String,
    default: 'pill',
    validator: (value) => ['pill', 'square', 'blob-speech', 'blob-arch'].includes(value)
  },
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg', 'xl'].includes(value)
  }
})

const variantClasses = computed(() => {
  switch (props.variant) {
    case 'primary': return 'bg-accent text-white shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active'
    case 'pink': return 'bg-secondary text-white shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5'
    case 'emerald': return 'bg-quaternary text-white shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5'
    case 'amber': return 'bg-tertiary text-foreground shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5'
    case 'secondary': return 'bg-transparent text-foreground hover:bg-tertiary'
    case 'outline': return 'bg-white text-foreground shadow-pop hover:bg-gray-100'
    case 'ghost': return 'bg-transparent text-foreground border-transparent hover:bg-muted'
    default: return ''
  }
})

const shapeClasses = computed(() => {
  switch (props.shape) {
    case 'pill': return 'rounded-full'
    case 'blob-speech': return 'rounded-lg rounded-bl-none'
    case 'blob-arch': return 'rounded-t-full rounded-b-none'
    case 'square':
    default: return 'rounded-md'
  }
})

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'sm': return 'px-4 py-1.5 text-xs min-h-[36px]'
    case 'lg': return 'px-8 py-3.5 text-base'
    case 'xl': return 'px-10 py-4.5 text-lg'
    case 'md':
    default: return 'px-6 py-2.5 text-sm'
  }
})
</script>
