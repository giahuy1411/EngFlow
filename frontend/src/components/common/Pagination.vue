<template>
  <nav
    :class="[
      'flex flex-wrap items-center gap-4 border-t-2 border-foreground pt-4',
      showSummary ? 'justify-between' : 'justify-center'
    ]"
    aria-label="Phân trang"
  >
    <div v-if="showSummary" class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
      Hiển thị <span class="font-black text-foreground">{{ startItem }} - {{ endItem }}</span> / {{ totalItems }} {{ itemLabel }}
    </div>

    <div :class="['flex flex-wrap items-center gap-2', showSummary ? 'justify-end' : 'justify-center']">
      <button
        type="button"
        :disabled="currentPage <= 1"
        class="pagination-control"
        aria-label="Trang trước"
        @click="changePage(currentPage - 1)"
      >
        &larr; Trước
      </button>

      <button
        type="button"
        :disabled="currentPage <= 1 || totalPages <= 1"
        class="pagination-control"
        aria-label="Trang đầu tiên"
        @click="changePage(1)"
      >
        1
      </button>

      <div class="pagination-page-selector" role="group" aria-label="Chọn trang">
        <input
          :id="paginationInputId"
          :value="pageInput"
          class="pagination-page-input"
          type="text"
          inputmode="numeric"
          pattern="[0-9]*"
          autocomplete="off"
          aria-label="Nhập số trang muốn tới"
          @input="handlePageInput"
          @keydown.enter.prevent="submitPage"
          @change="submitPage"
          @focus="selectAll"
        />
        <span class="pagination-page-divider" aria-hidden="true">/</span>
        <span class="pagination-total-pages" aria-hidden="true">{{ totalPages }}</span>
      </div>

      <button
        type="button"
        :disabled="currentPage >= totalPages || totalPages <= 1"
        class="pagination-control"
        :aria-label="`Trang cuối cùng: ${totalPages}`"
        @click="changePage(totalPages)"
      >
        {{ totalPages }}
      </button>

      <button
        type="button"
        :disabled="currentPage >= totalPages"
        class="pagination-control"
        aria-label="Trang sau"
        @click="changePage(currentPage + 1)"
      >
        Sau &rarr;
      </button>
    </div>
  </nav>
</template>

<script setup>
import { computed, getCurrentInstance, ref, watch } from 'vue'

const props = defineProps({
  currentPage: {
    type: Number,
    required: true,
    default: 1
  },
  totalPages: {
    type: Number,
    required: true,
    default: 1
  },
   totalItems: {
    type: Number,
    required: true,
    default: 0
  },
  pageSize: {
    type: Number,
    default: 10
  },
  itemLabel: {
    type: String,
    default: 'mục'
  },
  showSummary: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['page-change'])
const pageInput = ref(String(props.currentPage))
const paginationInputId = `pagination-page-input-${getCurrentInstance()?.uid ?? 'default'}`

watch(() => props.currentPage, page => {
  pageInput.value = String(page)
})

const startItem = computed(() => {
  if (props.totalItems === 0) return 0
  return (props.currentPage - 1) * props.pageSize + 1
})

const endItem = computed(() => Math.min(props.currentPage * props.pageSize, props.totalItems))

function changePage(page) {
  if (!Number.isInteger(page) || page < 1 || page > props.totalPages || page === props.currentPage) return
  emit('page-change', page)
}

function handlePageInput(event) {
  pageInput.value = event.target.value.replace(/\D+/g, '')
}

function submitPage() {
  const page = Number(pageInput.value)
  if (!Number.isInteger(page) || page < 1 || page > props.totalPages) {
    pageInput.value = String(props.currentPage)
    return
  }
  changePage(page)
}

function selectAll(e) {
  e.target.select()
}
</script>

<style scoped>
.pagination-control {
  min-height: 2.5rem;
  border: 2px solid var(--geo-fg);
  background: var(--geo-card);
  padding: 0.55rem 0.95rem;
  color: var(--geo-fg);
  font-size: 0.7rem;
  font-weight: 900;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  border-radius: 9999px;
  box-shadow: var(--geo-shadow-md);
  transition: transform 160ms ease, box-shadow 160ms ease, background-color 160ms ease, opacity 160ms ease;
}

.pagination-control:hover:not(:disabled) {
  background: color-mix(in srgb, var(--geo-tertiary) 35%, white);
  box-shadow: var(--geo-shadow-lg);
  transform: translate(-1px, -1px);
}

.pagination-control:active:not(:disabled) {
  box-shadow: var(--geo-shadow-active);
  transform: translate(1px, 1px) scale(0.98);
}

.pagination-control:disabled {
  cursor: not-allowed;
  opacity: 0.4;
  box-shadow: var(--geo-shadow-xs);
}

.pagination-control--accent {
  background: var(--geo-accent);
  color: var(--geo-accent-fg);
}

.pagination-control--accent:hover:not(:disabled) {
  background: color-mix(in srgb, var(--geo-accent) 92%, white);
}

.pagination-page-selector {
  display: inline-flex;
  align-items: center;
  border: 2px solid var(--geo-fg);
  background: var(--geo-card);
  border-radius: 9999px;
  box-shadow: var(--geo-shadow-md);
  min-height: 2.5rem;
  overflow: hidden;
  transition: box-shadow 160ms ease, transform 160ms ease, border-color 160ms ease;
}

.pagination-page-selector:focus-within {
  border-color: var(--geo-accent);
  box-shadow: var(--geo-shadow-accent);
  transform: translate(-1px, -1px);
}

.pagination-page-input {
  width: 2.75rem;
  border: none;
  background: transparent;
  padding: 0.45rem 0.45rem 0.45rem 0.65rem;
  color: var(--geo-fg);
  font-size: 0.9rem;
  font-variant-numeric: tabular-nums;
  font-weight: 900;
  text-align: center;
  outline: none;
  -webkit-appearance: none;
  -moz-appearance: textfield;
  appearance: none;
}

.pagination-page-input::-webkit-inner-spin-button,
.pagination-page-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  appearance: none;
  margin: 0;
}

.pagination-page-divider {
  display: inline-flex;
  align-items: center;
  color: var(--geo-border);
  font-size: 0.85rem;
  font-weight: 900;
}

.pagination-total-pages {
  display: inline-flex;
  align-items: center;
  min-height: 2.5rem;
  padding: 0 0.65rem 0 0.45rem;
  color: var(--geo-accent);
  font-size: 0.85rem;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.pagination-page-input:focus-visible {
  outline: none;
}

@media (max-width: 767px) {
  .pagination-control {
    flex: 1 1 auto;
  }
}
</style>
