<script setup>
/**
 * StickerCard — sticker-style surface primitive.
 * Non-interactive by default; `interactive` enables hover lift.
 * `featured` applies the featured hard shadow.
 */
import { computed } from 'vue'

const props = defineProps({
  as: { type: String, default: 'div' },
  interactive: { type: Boolean, default: false },
  featured: { type: Boolean, default: false },
})

const classes = computed(() => [
  'app-card',
  { 'app-card--interactive': props.interactive },
  { 'app-card--featured': props.featured },
])
</script>

<template>
  <component :is="as" :class="classes">
    <slot name="icon" />
    <div class="app-card__body">
      <slot />
    </div>
    <slot name="footer" />
  </component>
</template>
