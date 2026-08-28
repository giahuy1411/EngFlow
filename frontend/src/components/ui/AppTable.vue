<script setup>
/**
 * AppTable — striped/unstriped HTML table wrapper accessible.
 * Columns via `columns`: [{ key, label, tdClass? }].
 * Rows via `rows`. Supports slots per column (slot scope `{ row, col }`).
 */
import { useSlots } from 'vue'

const props = defineProps({
  columns: { type: Array, default: () => [] },
  rows: { type: Array, default: () => [] },
  striped: { type: Boolean, default: true },
  caption: { type: String, default: '' },
})

const attrs = useSlots()
</script>

<template>
  <div class="app-table-wrap">
    <table :class="['app-table', striped ? 'app-table--striped' : '']">
      <caption v-if="caption" class="sr-only">{{ caption }}</caption>
      <thead>
        <tr>
          <th v-for="col in columns" :key="col.key" :style="{ width: col.width }">
            {{ col.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, ri) in rows" :key="ri">
          <td v-for="col in columns" :key="col.key" :class="col.tdClass">
            <slot v-if="$slots[`col-${col.key}`]" :name="`col-${col.key}`" :row="row" :col="col" :index="ri">
              {{ row[col.key] }}
            </slot>
            <slot v-else name="cell" :row="row" :col="col" :index="ri">
              {{ row[col.key] }}
            </slot>
          </td>
        </tr>
        <tr v-if="!rows.length">
          <td :colspan="columns.length" class="text-center py-8" style="text-align:center">
            <slot name="empty">No data</slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
