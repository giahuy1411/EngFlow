<template>
  <button
    :class="[
      'inline-flex items-center justify-center gap-2 font-bold border-2 border-foreground cursor-pointer transition-all duration-300 ease-bounce select-none min-h-[48px] leading-none no-underline',
      'focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-geo-ring focus-visible:ring-offset-2',
      'disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none',
      variantClasses,
      sizeClasses,
      shapeClasses,
    ]"
    v-bind="$attrs"
  >
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: v => ['primary', 'secondary', 'ghost', 'pink', 'emerald', 'amber'].includes(v)
  },
  size: {
    type: String,
    default: 'md',
    validator: v => ['sm', 'md', 'lg', 'xl'].includes(v)
  },
  shape: {
    type: String,
    default: 'pill',
    validator: v => ['pill', 'square', 'blob-speech', 'blob-arch'].includes(v)
  }
})

const variantClasses = computed(() => {
  const map = {
    primary: 'bg-accent text-white shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5',
    pink: 'bg-secondary text-white shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5',
    emerald: 'bg-quaternary text-white shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5',
    amber: 'bg-tertiary text-foreground shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5',
    secondary: 'bg-transparent text-foreground hover:bg-tertiary',
    ghost: 'bg-transparent text-foreground border-transparent hover:bg-muted',
  }
  return map[props.variant] || map.primary
})

const shapeClasses = computed(() => {
  const map = {
    pill: 'rounded-full',
    square: 'rounded-md',
    'blob-speech': 'rounded-lg rounded-bl-none',
    'blob-arch': 'rounded-t-full rounded-b-none',
  }
  return map[props.shape] || map.pill
})

const sizeClasses = computed(() => {
  const map = {
    sm: 'px-4 py-1.5 text-xs min-h-[36px]',
    md: 'px-6 py-2.5 text-sm',
    lg: 'px-8 py-3.5 text-base',
    xl: 'px-10 py-4.5 text-lg',
  }
  return map[props.size] || map.md
})
</script>
