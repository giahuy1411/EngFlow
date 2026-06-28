<template>
  <div class="my-4 p-4 bg-gray-50 rounded-lg">
    <audio controls class="w-full" :src="data.audioUrl"></audio>
    <p v-if="data.transcript" class="text-sm text-gray-500 mt-2">{{ data.transcript }}</p>
  </div>
</template>

<script setup>
const props = defineProps({
  data: { type: Object, required: true }
});
</script>
