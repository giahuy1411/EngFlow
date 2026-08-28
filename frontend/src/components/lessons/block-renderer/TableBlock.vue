<template>
  <div class="my-4 overflow-x-auto">
    <table class="min-w-full border-collapse border">
      <thead v-if="headers.length">
        <tr>
          <th v-for="h in headers" :key="h" class="border px-4 py-2 bg-muted">{{ h }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, i) in rows" :key="i">
          <td v-for="(cell, j) in row" :key="j" class="border px-4 py-2">{{ cell }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data: { type: Object, required: true }
});

const headers = computed(() => props.data?.headers || []);
const rows = computed(() => props.data?.rows || []);
</script>

