<template>
  <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
    <router-link :to="'/lessons/' + lessonId"
      class="inline-flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent transition-colors"
    >
      <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
      Quay lại bài học
    </router-link>
    <div class="flex items-center gap-3">
      <div class="w-2.5 h-2.5 bg-accent rounded-full"></div>
      <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Preview</span>
    </div>
  </div>
  <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl">
    <div class="geo-markdown" v-html="parseMarkdown(content)"></div>
  </div>
</template>

<script setup>
import { marked } from 'marked'
import DOMPurify from 'dompurify'

defineProps({
  lessonId: { type: [String, Number], required: true },
  content: { type: String, default: '' },
})

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}
</script>
