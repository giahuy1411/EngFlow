<template>
  <div class="my-4">
    <img :src="data.imageUrl" :alt="data.caption || ''" class="max-w-full rounded-lg" />
    <p v-if="data.caption" class="text-sm text-muted-foreground mt-2 italic">{{ data.caption }}</p>
  </div>
</template>

<script setup>
const props = defineProps({
  data: { type: Object, required: true }
});
</script>

