<script setup>
/**
 * FormField — labeled form control wrapper.
 * Provides label, optional hint, error message, and required marker.
 * Wires aria-describedby between hint/error and the slotted control.
 */
import { computed } from 'vue'

const props = defineProps({
  id: { type: String, required: true },
  label: { type: String, required: true },
  hint: { type: String, default: undefined },
  error: { type: String, default: undefined },
  required: { type: Boolean, default: false },
})

const hasError = computed(() => Boolean(props.error))
const hintId = computed(() => (props.hint ? `${props.id}-hint` : undefined))
const errorId = computed(() => (hasError.value ? `${props.id}-error` : undefined))
const describedBy = computed(() => [hintId.value, errorId.value].filter(Boolean).join(' ') || undefined)
</script>

<template>
  <div class="app-form-field" :class="{ 'app-form-field--invalid': hasError }">
    <label class="app-form-field__label" :for="id">
      {{ label }}
      <span v-if="required" class="app-form-field__required" aria-hidden="true">*</span>
    </label>
    <div class="app-form-field__control">
      <slot v-bind="{ id, describedBy, hasError }" />
    </div>
    <p v-if="hint" :id="hintId" class="app-form-field__hint">{{ hint }}</p>
    <p v-if="hasError" :id="errorId" class="app-form-field__error" role="alert">
      {{ error }}
    </p>
  </div>
</template>
