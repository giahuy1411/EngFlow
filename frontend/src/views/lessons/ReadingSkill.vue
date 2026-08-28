<template>
  <div>
    <div v-if="readingContent" class="geo-markdown" v-html="parseMarkdown(readingContent)"></div>
    <div v-if="questions.length > 0" class="mt-8 space-y-6">
      <div v-for="(q, idx) in questions" :key="idx"
        class="border-2 border-foreground rounded-md p-6 shadow-pop-lg"
      >
        <div class="geo-markdown text-lg font-bold mb-4" v-html="parseMarkdown(q.question)"></div>
        <div v-if="q.options" class="space-y-2">
          <label v-for="(opt, oi) in q.options" :key="oi"
            class="flex items-center gap-3 p-3 border-2 border-foreground rounded-md cursor-pointer transition-all"
            :class="answers[idx] === oi ? 'bg-accent/10 border-accent' : 'hover:bg-tertiary/10'"
          >
            <input type="radio" :name="'read-q-' + idx" :value="oi" v-model="answers[idx]" class="geo-radio" />
            <span class="font-medium">{{ opt }}</span>
          </label>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  readingContent: { type: String, default: '' },
  questions: { type: Array, default: () => [] },
})

const answers = ref([])
onMounted(() => { answers.value = props.questions.map(() => null) })

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}
</script>

<style scoped>
input.geo-radio {
  appearance: none;
  width: 20px; height: 20px;
  border: 2px solid var(--geo-fg, #1E293B);
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 300ms cubic-bezier(0.34, 1.56, 0.64, 1);
  flex-shrink: 0;
}
input.geo-radio:checked {
  border-color: var(--geo-accent, #8B5CF6);
  background: var(--geo-accent, #8B5CF6);
  box-shadow: inset 0 0 0 3px white;
}
</style>
