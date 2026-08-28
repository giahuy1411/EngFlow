<script setup>
/**
 * AppPagination — page-number navigation with ellipsis.
 * v-model:page (1-indexed). Props: totalPages.
 */
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  totalPages: { type: Number, default: 0 },
})

const emit = defineEmits(['update:page', 'change'])

const setPage = (p) => {
  if (p < 1 || p > props.totalPages || p === props.page) return
  emit('update:page', p)
  emit('change', p)
}

const window = computed(() => {
  const total = props.totalPages
  const current = props.page
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  // Build page list with ellipsis markers
  const pages = new Set([1, total, current - 1, current, current + 1])
  let arr = [...pages].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b)
  const out = []
  let prev = 0
  for (const p of arr) {
    if (p - prev > 1) out.push('...')
    out.push(p)
    prev = p
  }
  return out
})
</script>

<template>
  <nav class="app-pagination" aria-label="Pagination">
    <button
      class="app-pagination__item"
      :disabled="page <= 1"
      aria-label="Previous page"
      @click="setPage(page - 1)"
    >
      ‹
    </button>
    <template v-for="(p, i) in window" :key="i">
      <span v-if="p === '...'" class="app-pagination__ellipsis">…</span>
      <button
        v-else
        class="app-pagination__item"
        :class="{ 'app-pagination__item--active': p === page }"
        :aria-current="p === page ? 'page' : undefined"
        @click="setPage(p)"
      >
        {{ p }}
      </button>
    </template>
    <button
      class="app-pagination__item"
      :disabled="page >= totalPages"
      aria-label="Next page"
      @click="setPage(page + 1)"
    >
      ›
    </button>
  </nav>
</template>
