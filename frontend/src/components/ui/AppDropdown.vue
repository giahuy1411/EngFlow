<script setup>
/**
 * AppDropdown — toggleable dropdown menu.
 * Use v-model:open or toggle programmatically with `toggle()`.
 * Header slot = trigger; items via `#item="{ item }"` default slot.
 */
import { computed, useSlots } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  items: { type: Array, default: () => [] }, // [{ label, value, danger? }]
  align: { type: String, default: 'right' }, // left|right
})

const emit = defineEmits(['update:modelValue', 'select'])

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

function select(item) {
  emit('select', item)
  open.value = false
}

const slots = useSlots()
</script>

<template>
  <div class="app-dropdown" @keydown.escape="open = false">
    <div
      class="app-dropdown__trigger"
      @click="open = !open"
      @keydown.enter.prevent="open = !open"
      @keydown.space.prevent="open = !open"
      tabindex="0"
      role="button"
      aria-haspopup="listbox"
      :aria-expanded="open"
    >
      <slot name="trigger" :open="open" />
    </div>
    <Transition name="fade">
      <div v-if="open" class="app-dropdown__menu" :style="{ right: align === 'right' ? '0' : 'auto', left: align === 'left' ? '0' : 'auto' }" role="menu">
        <template v-if="items.length">
          <button
            v-for="item in items"
            :key="item.value"
            class="app-dropdown__item"
            :class="{ 'app-dropdown__item--danger': item.danger }"
            role="menuitem"
            @click="select(item)"
          >
            <slot name="item" :item="item">
              <span v-if="item.icon" v-html="item.icon" />
              <span>{{ item.label }}</span>
            </slot>
          </button>
        </template>
        <slot v-else name="items" />
        <div class="app-dropdown__divider" v-if="slots.divider" />
        <slot name="divider" />
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(-6px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
