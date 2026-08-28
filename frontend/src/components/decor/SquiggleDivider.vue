<template>
  <div class="squiggle-divider" :style="styleVars" aria-hidden="true" />
</template>

<script setup>
defineProps({
  color: { type: String, default: 'var(--geo-fg)' },
  height: { type: String, default: '16px' },
})
const styleVars = {
  '--deco-squiggle-color': 'color:var(--geo-fg)',
}
</script>

<style scoped>
.squiggle-divider {
  width: 100%;
  height: var(--deco-height, 16px);
  opacity: 0.25;
  background-image: url("data:image/svg+xml,%3Csvg width='100' height='16' viewBox='0 0 100 16' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 8 Q25 0 50 8 T100 8' fill='none' stroke='%231E293B' stroke-width='2'/%3E%3C/svg%3E");
  background-repeat: repeat-x;
  background-size: 100px 16px;
}
</style>