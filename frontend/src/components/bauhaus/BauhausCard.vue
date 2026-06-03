<template>
  <div
    :class="[
      'bg-white border-4 border-black shadow-hard-lg relative p-6',
      hoverEffect ? 'bauhaus-card-hover' : '',
    ]"
    v-bind="$attrs"
  >
    <!-- Geometric decoration -->
    <div
      v-if="decoration"
      :class="[
        'absolute -top-1 -right-1 w-6 h-6 border-2 border-black z-10',
        decorationShapeClass,
        decorationColorClass
      ]"
      style="transform: translate(50%, -50%)"
    ></div>
    
    <slot></slot>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  hoverEffect: {
    type: Boolean,
    default: true
  },
  decoration: {
    type: Boolean,
    default: true
  },
  decorationShape: {
    type: String,
    default: 'circle',
    validator: (value) => ['circle', 'square', 'triangle'].includes(value)
  },
  decorationColor: {
    type: String,
    default: 'red',
    validator: (value) => ['red', 'blue', 'yellow'].includes(value)
  }
})

const decorationShapeClass = computed(() => {
  switch (props.decorationShape) {
    case 'circle': return 'rounded-full'
    case 'square': return 'rounded-none rotate-12'
    case 'triangle': return 'rounded-none' // Custom clip-path applied via CSS if needed
    default: return 'rounded-full'
  }
})

const decorationColorClass = computed(() => {
  switch (props.decorationColor) {
    case 'red': return 'bg-primary-red'
    case 'blue': return 'bg-primary-blue'
    case 'yellow': return 'bg-primary-yellow'
    default: return 'bg-primary-red'
  }
})
</script>
