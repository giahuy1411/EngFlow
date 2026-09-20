<template>
  <div>
    <!-- Writing prompt -->
    <div class="border-2 border-foreground rounded-md p-6 shadow-pop-lg mb-6">
      <div class="geo-markdown text-lg font-bold mb-4" v-html="parseMarkdown(prompt)"></div>
    </div>
    <div>
      <label for="writing-answer" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Bài viết của bạn</label>
      <textarea id="writing-answer" v-model="answer" rows="6" placeholder="Viết câu trả lời của bạn..."
        class="w-full bg-input border-2 border-border rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
      ></textarea>
    </div>
    <div v-if="modelAnswer" class="mt-6 p-6 bg-card border-2 border-foreground rounded-md shadow-pop-lg">
      <p class="font-black text-sm uppercase tracking-wider text-accent-ink mb-2">Gợi ý</p>
      <div class="geo-markdown" v-html="parseMarkdown(modelAnswer)"></div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  prompt: { type: String, default: '' },
  modelAnswer: { type: String, default: '' },
})

const answer = ref('')

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}
</script>
