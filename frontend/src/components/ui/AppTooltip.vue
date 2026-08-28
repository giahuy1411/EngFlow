<script setup>
/**
 * AppTooltip — hover/focus tooltip.
 * `text` prop or `#tip` slot; default slot = trigger content.
 */
defineProps({
  text: { type: String, default: '' },
})
</script>

<template>
  <span class="app-tooltip">
    <span class="app-tooltip__tip" role="tooltip">
      <slot name="tip">{{ text }}</slot>
    </span>
    <span class="app-tooltip__trigger">
      <slot />
    </span>
  </span>
</template>
