<template>
  <span aria-hidden="true" :class="['decoconfetti', `decoconfetti--${kind}`]" :style="styleVars">
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
/**
 * audit-v10 fixes two real defects in the previous version:
 *  1. `sizePx` was a module-level `const sizeMap.md` — every confetti rendered at
 *     18px regardless of the `size` prop, so `sm`/`lg`/`xl` silently did nothing.
 *  2. `color` was declared but never read; the colour came from a `size`-based
 *     class, which conflated "how big" with "what colour". Passing `color` had no
 *     effect at all.
 *
 * Size and colour are now independent: `size` drives the pixel dimensions and
 * `color` drives the fill, defaulting to the design system's rotational palette
 * (violet → pink → amber → mint) so existing call sites keep their look.
 */
import { computed } from 'vue'

const sizeMap = { sm: 14, md: 18, lg: 24, xl: 32 }
const paletteBySize = {
  sm: 'var(--geo-tertiary)',
  md: 'var(--geo-secondary)',
  lg: 'var(--geo-accent)',
  xl: 'var(--geo-quaternary)',
}

const props = defineProps({
  kind: {
    type: String,
    default: 'circle',
    validator: (v) => ['circle', 'square', 'triangle'].includes(v),
  },
  color: { type: String, default: '' },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg', 'xl'].includes(v),
  },
})

const sizePx = computed(() => sizeMap[props.size] ?? sizeMap.md)
const styleVars = computed(() => ({
  '--deco-confetti-color': props.color || paletteBySize[props.size] || paletteBySize.md,
}))
</script>

<style scoped>
.decoconfetti {
  position: absolute;
  pointer-events: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--deco-confetti-color, var(--geo-secondary));
}

@media (prefers-reduced-motion: reduce) {
  .decoconfetti { opacity: 0.7; }
}
</style>
