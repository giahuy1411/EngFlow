<template>
  <span aria-hidden="true" :class="classList" />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'tertiary' },
  size: { type: String, default: 'md' },
})
const classList = computed(() => ['geoshape', `geoshape--${props.variant || 'tertiary'}`, `geoshape--${props.size || 'md'}`].join(' '))
</script>

<style scoped>
.geoshape {
  position: absolute;
  pointer-events: none;
  border-radius: 50%;
  border: var(--geo-border-width) solid var(--geo-fg);
  opacity: 0.4;
}
.geoshape--tertiary-sm { width: 48px; height: 48px; background: var(--geo-tertiary); }
.geoshape--tertiary-md { width: 88px; height: 88px; background: var(--geo-tertiary); }
.geoshape--tertiary-lg { width: 144px; height: 144px; background: var(--geo-tertiary); }
.geoshape--secondary-sm { width: 48px; height: 48px; background: var(--geo-secondary); }
.geoshape--secondary-md { width: 88px; height: 88px; background: var(--geo-secondary); }
.geoshape--secondary-lg { width: 144px; height: 144px; background: var(--geo-secondary); }
.geoshape--accent-sm { width: 48px; height: 48px; background: var(--geo-accent); }
.geoshape--accent-md { width: 88px; height: 88px; background: var(--geo-accent); }
.geoshape--accent-lg { width: 144px; height: 144px; background: var(--geo-accent); }
.geoshape--quaternary-sm { width: 48px; height: 48px; background: var(--geo-quaternary); }
.geoshape--quaternary-md  { width: 88px; height: 88px; background: var(--geo-quaternary); }
.geoshape--quaternary-lg  { width: 144px; height: 144px; background: var(--geo-quaternary); }

@media (prefers-reduced-motion: reduce) {
  .geoshape { opacity: 0.25; }
}
</style>