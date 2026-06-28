<template>
  <div class="prose max-w-none" v-html="content"></div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data: { type: Object, required: true }
});

const content = computed(() => props.data?.content || '');
</script>
