<template>
  <div class="prose max-w-none" v-html="sanitizedContent"></div>
</template>

<script setup>
import { computed } from 'vue'
import DOMPurify from 'dompurify'

const props = defineProps({
  data: { type: Object, required: true }
})

const content = computed(() => props.data?.content || '')

// Sanitize trước khi chèn HTML để chặn XSS từ lesson content.
const sanitizedContent = computed(() =>
  content.value ? DOMPurify.sanitize(content.value) : ''
)
</script>
