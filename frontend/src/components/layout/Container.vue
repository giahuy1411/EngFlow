<template>
  <component :is="tag" class="geo-container" :data-size="size">
    <slot />
  </component>
</template>

<script setup>
defineProps({
  tag: { type: String, default: 'div' },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'xl'].includes(v),
  },
})
</script>

<style scoped>
.geo-container {
  max-width: 80rem;
  margin-left: auto;
  margin-right: auto;
  padding-left: 1.5rem;
  padding-right: 1.5rem;
}
.geo-container[data-size~='sm'] { max-width: 40rem; }
.geo-container[data-size~='xl'] { max-width: 96rem; }
</style>
