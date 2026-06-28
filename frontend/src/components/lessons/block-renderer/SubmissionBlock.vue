<template>
  <div class="my-4 p-4 border rounded-lg bg-gray-50">
    <p class="font-medium mb-3">{{ data.prompt }}</p>
    <textarea v-if="data.submissionType === 'TEXT'" v-model="text"
      class="w-full border rounded px-3 py-2 min-h-[120px]" placeholder="Nhập câu trả lời..."></textarea>
    <div v-else-if="data.submissionType === 'AUDIO'" class="text-gray-500 italic">
      Tính năng ghi âm đang phát triển...
    </div>
    <p v-if="submitted" class="text-green-600 mt-2">Đã nộp bài!</p>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const props = defineProps({
  data: { type: Object, required: true }
});

const text = ref('');
const submitted = ref(false);
</script>
