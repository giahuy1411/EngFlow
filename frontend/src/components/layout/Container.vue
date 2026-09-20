<template>
  <component :is="tag" class="geo-container" :data-size="size">
    <slot />
  </component>
</template>

<script setup>
/**
 * audit-v10: the default width is now the prompt's `max-w-6xl` (72rem / 1152px),
 * down from 80rem (1280px / `max-w-7xl`).
 *
 * `xl` (96rem) is kept for the wide admin tables, which genuinely need the extra
 * columns; `sm` (40rem) stays for auth forms. Anything that must keep the old
 * generous width should opt into `size="xl"` explicitly rather than relying on
 * the default.
 */
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
  /* Prompt: "Container: max-w-6xl (Generous width)". */
  max-width: 72rem;
  margin-left: auto;
  margin-right: auto;
  padding-left: 1.5rem;
  padding-right: 1.5rem;
}
.geo-container[data-size~='sm'] { max-width: 40rem; }
.geo-container[data-size~='xl'] { max-width: 96rem; }
</style>
