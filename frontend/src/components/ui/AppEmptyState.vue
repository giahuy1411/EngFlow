<script setup>
import { computed } from 'vue'
const props = defineProps({
  title: { type: String, default: undefined },
  description: { type: String, default: undefined },
})
const classes = computed(() => ['app-state', { 'app-state--empty': true }])
</script>
<template>
  <div :class="classes" role="region" aria-labelledby="app-empty-title">
    <slot name="visual" />
    <h3 v-if="title" id="app-empty-title" class="app-state__title">{{ title }}</h3>
    <p v-if="description" class="app-state__desc">{{ description }}</p>
    <div v-if="$slots.action" class="app-state__action"><slot name="action" /></div>
  </div>
</template>
