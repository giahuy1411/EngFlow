<script setup>
import { computed } from 'vue'
const props = defineProps({
  title: { type: String, default: undefined },
  description: { type: String, default: undefined },
})
const classes = computed(() => ['app-state', { 'app-state--error': true }])
</script>
<template>
  <div :class="classes" role="alert">
    <slot name="visual" />
    <h3 v-if="title" class="app-state__title">{{ title }}</h3>
    <p v-if="description" class="app-state__desc">{{ description }}</p>
    <div v-if="$slots.action" class="app-state__action"><slot name="action" /></div>
  </div>
</template>
