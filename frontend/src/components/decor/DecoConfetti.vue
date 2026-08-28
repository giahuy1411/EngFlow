<template>
  <span aria-hidden="true" :class="['decoconfetti', `decoconfetti--${kind}`, `decoconfetti--${size}`]">
    <svg v-if="kind === 'circle'" viewBox="0 0 24 24" :width="sizePx" :height="sizePx">
      <circle cx="12" cy="12" r="10" fill="currentColor" />
    </svg>
    <svg v-else-if="kind === 'square'" viewBox="0 0 24 24" :width="sizePx" :height="sizePx">
      <rect x="2" y="2" width="20" height="20" rx="2" fill="currentColor" />
    </svg>
    <svg v-else viewBox="0 0 24 24" :width="sizePx" :height="sizePx">
      <path d="M12 4 L20 20 L4 20 Z" fill="currentColor" />
    </svg>
  </span>
</template>

<script setup>
const sizeMap = { sm: 14, md: 18, lg: 24, xl: 32 }
defineProps({
  kind: {
    type: String,
    default: 'circle',
    validator: (v) => ['circle', 'square', 'triangle'].includes(v),
  },
  color: {
    type: String,
    default: '',
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg', 'xl'].includes(v),
  },
})
const sizePx = sizeMap.md
</script>

<style scoped>
.decoconfetti {
  position: absolute;
  pointer-events: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.decoconfetti--sm { color: var(--geo-tertiary); }
.decoconfetti--md { color: var(--geo-secondary); }
.decoconfetti--lg { color: var(--geo-accent); }
.decoconfetti--xl { color: var(--geo-quaternary); }

@media (prefers-reduced-motion: reduce) {
  .decoconfetti { opacity: 0.7; }
}
</style>