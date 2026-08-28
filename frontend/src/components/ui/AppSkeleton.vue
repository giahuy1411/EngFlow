<script setup>
import { computed } from 'vue'
const props = defineProps({
  width: { type: String, default: '100%' },
  height: { type: String, default: '1rem' },
  radius: { type: String, default: 'var(--geo-radius-sm)' },
})
const style = computed(() => ({
  width: props.width,
  height: props.height,
  borderRadius: props.radius,
}))
</script>
<template>
  <div class="app-skeleton" :style="style" aria-hidden="true" />
</template>
