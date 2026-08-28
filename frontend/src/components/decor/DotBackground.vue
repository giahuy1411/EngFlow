<template>
  <div
    class="geodot"
    :style="{ '--deco-dot-opacity': String(tint), backgroundSize: `${gap}px ${gap}px` }"
    aria-hidden="true"
  />
</template>
<script setup>
defineProps({
  gap: { type: [Number, String], default: 20 },
  tint: { type: Number, default: 0.18 },
})
</script>

<style scoped>
.geodot {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: var(--deco-dot-opacity, 0.18);
  background-image: radial-gradient(circle, var(--geo-fg) 1px, transparent 1px);
}
</style>