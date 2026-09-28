/**
 * AppInput — primitive input dạng text.
 *
 * Hợp đồng:
 * - v-model qua modelValue/update:modelValue.
 * - Forward attribute native (autocomplete, name, placeholder, maxlength).
 * - `invalid` đánh dấu control lỗi và expose `aria-invalid`.
 *
 * Primitive thuần trình bày: không fetch dữ liệu, không store, không routing.
 */
<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  id: { type: String, default: undefined },
  type: { type: String, default: 'text' },
  disabled: { type: Boolean, default: false },
  invalid: { type: Boolean, default: false },
  required: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus'])

const classes = computed(() => ['app-input', { 'app-input--invalid': props.invalid }])
</script>

<template>
  <input
    :id="id"
    :type="type"
    :class="classes"
    :value="modelValue"
    :disabled="disabled"
    :aria-invalid="invalid || undefined"
    :required="required || undefined"
    @input="emit('update:modelValue', $event.target.value)"
    @blur="emit('blur', $event)"
    @focus="emit('focus', $event)"
  />
</template>
