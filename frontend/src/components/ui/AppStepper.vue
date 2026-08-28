<script setup>
/**
 * AppStepper — step indicator.
 * Props: steps (string[]), current (index 0-based, or value if using `values`).
 */
import { computed } from 'vue'

const props = defineProps({
  steps: { type: Array, default: () => [] },
  current: { type: [Number, String], default: 0 },
})

const idx = computed(() => {
  if (typeof props.current === 'number') return props.current
  return props.steps.indexOf(props.current)
})
</script>

<template>
  <div class="app-stepper" aria-label="Stepper">
    <template v-for="(step, i) in steps" :key="i">
      <div class="app-stepper__step">
        <span
          class="app-stepper__dot"
          :class="{
            'app-stepper__dot--active': idx === i,
            'app-stepper__dot--done': idx > i,
          }"
          :aria-current="idx === i ? 'step' : undefined"
        >
          {{ idx > i ? '✓' : i + 1 }}
        </span>
        <span class="app-stepper__label" :class="{ 'app-stepper__label--active': idx === i }">
          {{ step }}
        </span>
      </div>
      <div
        v-if="i < steps.length - 1"
        class="app-stepper__line"
        :class="{ 'app-stepper__line--done': idx > i }"
      />
    </template>
  </div>
</template>
