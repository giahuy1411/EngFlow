<template>
  <div
    :class="[
      'bg-card border-2 border-foreground rounded-md relative overflow-hidden',
      shadowClass,
      hoverEffect ? 'hover:-rotate-1 hover:scale-[1.02] transition-all duration-300 ease-bounce' : '',
      paddingClass,
    ]"
    v-bind="$attrs"
  >
    <!-- Decorative shape corner -->
    <div
      v-if="decoration"
      :class="[
        'absolute -top-1 -right-1 w-6 h-6 border-2 border-foreground z-10',
        decorationShapeClass,
        decorationColorClass,
      ]"
      style="transform: translate(50%, -50%)"
    ></div>

    <slot></slot>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  hoverEffect: { type: Boolean, default: true },
  decoration: { type: Boolean, default: true },
  decorationShape: { type: String, default: 'circle' },
  decorationColor: { type: String, default: 'accent' },
  featured: { type: Boolean, default: false },
  padding: { type: String, default: 'md' },
})

const shadowClass = computed(() => {
  if (props.featured) return 'shadow-pop-featured'
  return 'shadow-pop-xl'
})

const paddingClass = computed(() => {
  const map = { none: '', sm: 'p-4', md: 'p-6', lg: 'p-8' }
  return map[props.padding] || map.md
})

const decorationShapeClass = computed(() => {
  const map = { circle: 'rounded-full', square: 'rounded-none rotate-12', triangle: 'clip-triangle', blob: 'rounded-blob' }
  return map[props.decorationShape] || map.circle
})

const decorationColorClass = computed(() => {
  const map = {
    accent: 'bg-accent',
    secondary: 'bg-secondary',
    tertiary: 'bg-tertiary',
    quaternary: 'bg-quaternary',
  }
  return map[props.decorationColor] || map.accent
})
</script>
