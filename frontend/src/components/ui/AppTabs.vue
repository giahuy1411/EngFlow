<script setup>
/**
 * AppTabs — primitive tablist accessible.
 *
 * Hợp đồng: `v-model:active` giữ chỉ số tab đang chọn. Truyền `items` cho mảng
 * đơn giản ([{ label, value? }]), hoặc dùng slot đầy đủ cho panel phức tạp.
 * Slot mặc định là render-prop, nhận `{ active, activate }` để panel tự hiển thị
 * theo tab hiện tại.
 *
 * Lưu ý: `active` so sánh với chỉ số `i` chứ không phải `item.value`; muốn dùng
 * value thì phải xử lý ở phía consumer. Template có tham chiếu `ariaLabel`
 * nhưng prop này chưa được khai báo nên hiện luôn là `undefined` (tablist không
 * có nhãn) — cần khai báo prop nếu muốn gắn aria-label thật.
 * Primitive thuần trình bày.
 */
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: [Number, String], default: 0 },
  items: { type: Array, default: () => [] }, // [{ label, value? }]
})

const emit = defineEmits(['update:modelValue'])

// computed hai chiều: đọc prop, ghi bằng emit để consumer giữ v-model.
const active = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

// Kích hoạt tab theo chỉ số — consumer cũng nhận được `activate` qua slot scope.
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
