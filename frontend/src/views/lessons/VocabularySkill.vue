<template>
  <div>
    <!-- Header -->
    <div class="flex items-center justify-between mb-8">
      <router-link :to="'/lessons/' + lessonId"
        class="inline-flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent transition-colors"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
        Quay lại
      </router-link>
      <div class="flex items-center gap-2">
        <div class="w-3 h-3 bg-accent rounded-full"></div>
        <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">{{ skill?.name || 'Kỹ năng' }}</span>
      </div>
    </div>

    <!-- Content Area -->
    <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl">
      <!-- Skill Content -->
      <div v-if="content" class="geo-markdown" v-html="parseMarkdown(content)"></div>

      <!-- Exercises -->
      <div v-if="exercises.length > 0" class="mt-8 space-y-6">
        <h3 class="font-black text-xl uppercase tracking-tight mb-4">Bài tập</h3>
        <div v-for="(ex, idx) in exercises" :key="idx"
          class="border-2 border-foreground rounded-md p-6 shadow-pop-lg"
        >
          <div class="geo-markdown text-lg font-bold mb-4" v-html="parseMarkdown(ex.question)"></div>
          <div v-if="ex.options" class="space-y-2">
            <label v-for="(opt, oi) in ex.options" :key="oi"
              class="flex items-center gap-3 p-3 border-2 border-foreground rounded-md cursor-pointer transition-all"
              :class="answers[idx] === oi ? 'bg-accent/10 border-accent' : 'hover:bg-tertiary/10'"
            >
              <input type="radio" :name="'skill-q-' + idx" :value="oi" v-model="answers[idx]" class="geo-radio" />
              <span class="font-medium">{{ opt }}</span>
            </label>
          </div>
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
  lessonId: { type: [String, Number], required: true },
  skill: { type: Object, default: null },
  content: { type: String, default: '' },
  exercises: { type: Array, default: () => [] },
})

const answers = ref([])

onMounted(() => {
  answers.value = props.exercises.map(() => null)
})

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}
</script>

<style scoped>
input.geo-radio {
  appearance: none;
  width: 20px;
  height: 20px;
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
