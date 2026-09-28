<script setup>
/**
 * AppTable — bọc bảng HTML, hỗ trợ kẻ sọc và accessible.
 *
 * Hợp đồng:
 * - Cột qua `columns`: [{ key, label, tdClass?, width? }].
 * - Dòng qua `rows`. Mỗi cột có thể ghi đè bằng slot tên `col-<key>`; nếu không
 *   có thì rơi về slot `cell` chung, cuối cùng mới hiển thị `row[col.key]`.
 *   Slot nhận scope `{ row, col, index }`.
 * - `caption` render dạng `sr-only` để screen reader có mô tả bảng; `striped`
 *   bật/tắt kẻ sọc. Không có dòng nào thì hiện slot `empty`.
 *
 * Lưu ý: biến đặt tên `attrs` nhưng thực chất là `useSlots()` — dùng để kiểm tra
 * sự tồn tại của slot theo cột (`$slots[...]`), không phải attribute.
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
