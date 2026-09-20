<template>
  <div class="squiggle-divider" :style="styleVars" aria-hidden="true" />
</template>

<script setup>
/**
 * Squiggle section divider — the design system's hand-drawn separator.
 *
 * audit-v10 fixes two real defects in the previous version:
 *  1. `color` and `height` were declared but never used: the SVG stroke was
 *     hardcoded to `%231E293B` and `--deco-height` was never set by anything.
 *  2. The style object wrote a whole CSS *declaration*
 *     (`'color:var(--geo-fg)'`) into a custom property, which is not a valid
 *     value for `background-image` to consume.
 *
 * The squiggle is now painted with `mask-image` + `background-color`, not with a
 * coloured `background-image`. This matters: `currentColor` inside a data-URI
 * SVG does NOT inherit from the host element (the SVG is a separate document
 * whose initial `color` is black), so a coloured background-image can never be
 * driven by a prop. Masking decouples the shape from the colour, so the `color`
 * prop genuinely reaches the pixels.
 */
import { computed } from 'vue'

const props = defineProps({
  color: { type: String, default: 'var(--geo-fg)' },
  height: { type: String, default: '16px' },
})

const styleVars = computed(() => ({
  '--deco-squiggle-color': props.color,
  '--deco-height': props.height,
}))
</script>

<style scoped>
.squiggle-divider {
  width: 100%;
  height: var(--deco-height, 16px);
  opacity: 0.25;
  background-color: var(--deco-squiggle-color, var(--geo-fg));
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg width='100' height='16' viewBox='0 0 100 16' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 8 Q25 0 50 8 T100 8' fill='none' stroke='%23000' stroke-width='2'/%3E%3C/svg%3E");
  mask-image: url("data:image/svg+xml,%3Csvg width='100' height='16' viewBox='0 0 100 16' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 8 Q25 0 50 8 T100 8' fill='none' stroke='%23000' stroke-width='2'/%3E%3C/svg%3E");
  -webkit-mask-repeat: repeat-x;
  mask-repeat: repeat-x;
  -webkit-mask-size: 100px 16px;
  mask-size: 100px 16px;
}
</style>
