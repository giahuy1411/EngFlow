<script setup>
/**
 * AppTabs — accessible tablist primitive.
 * v-model:active for active tab index. Render-prop via slot: passes { active, activate }.
 * Use `items` prop for simple arrays, or full slots for complex panels.
 */
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: [Number, String], default: 0 },
  items: { type: Array, default: () => [] }, // [{ label, value? }]
})

const emit = defineEmits(['update:modelValue'])

const active = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

function activate(index) {
  active.value = index
}
</script>

<template>
  <div class="app-tabs" role="tablist" :aria-label="ariaLabel || undefined">
    <template v-if="items.length">
      <button
        v-for="(item, i) in items"
        :key="i"
        class="app-tabs__tab"
        :class="{ 'app-tabs__tab--active': active === i }"
        role="tab"
        :aria-selected="active === i"
        @click="activate(i)"
      >
        <slot :name="`tab-${i}`" :item="item">{{ item.label }}</slot>
      </button>
    </template>
    <slot v-else name="tabs" />
  </div>
  <slot :active="active" :activate="activate" />
</template>
