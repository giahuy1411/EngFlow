<template>
  <div class="listening-skill space-y-8">
    <div v-if="!skillContent" class="bg-white border-4 border-black shadow-hard-lg p-10 text-center">
      <div class="text-6xl mb-4 opacity-30">L</div>
      <p class="font-bold text-xl uppercase">No listening content yet</p>
      <p class="font-bold text-xs uppercase tracking-wider text-foreground/60 mt-2">This lesson doesn't have listening exercises.</p>
    </div>

    <div v-else>
      <!-- Video -->
      <div v-if="listeningData.videoHtml" class="bg-white border-4 border-black shadow-hard-lg">
        <div class="bg-red-600 text-white px-5 py-2 flex items-center gap-2">
          <span class="w-3 h-3 bg-white rounded-full animate-pulse"></span>
          <span class="font-bold text-sm uppercase tracking-wider">Audio / Video</span>
        </div>
        <div class="p-4" v-html="listeningData.videoHtml"></div>
      </div>

      <!-- Content -->
      <div class="bg-white border-4 border-black shadow-hard-lg">
        <div class="bg-black text-white px-6 py-4 flex items-center gap-3">
          <span class="w-8 h-8 bg-yellow-400 text-black rounded-full flex items-center justify-center font-black text-sm">L</span>
          <h3 class="font-black text-xl uppercase tracking-tight">Listening</h3>
        </div>
        <div class="px-6 py-6">
          <div v-html="listeningData.staticHtml" class="skill-html"></div>
        </div>
      </div>

      <!-- Quiz -->
      <div v-if="quizQuestions.length">
        <div class="flex items-center gap-3 mb-5">
          <div class="h-px flex-1 bg-black/20"></div>
          <span class="font-black text-sm uppercase tracking-widest text-gray-500">Practice</span>
          <div class="h-px flex-1 bg-black/20"></div>
        </div>

        <QuizEngine
          :questions="quizQuestions"
          :lesson-id="lesson.id"
          skill-code="lis"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { parseListeningContent, parseQuizContent } from '@/utils/skillParser'
import QuizEngine from '@/components/lessons/QuizEngine.vue'

const props = defineProps({
  lesson: { type: Object, required: true },
  skills: { type: Array, required: true },
})

const skillContent = computed(() => props.skills.find(s => s.skillType === 'LISTENING'))
const listeningData = computed(() => skillContent.value ? parseListeningContent(skillContent.value.content) : { staticHtml: '', videoHtml: '' })
const quizQuestions = computed(() => {
  if (!skillContent.value) return []
  return parseQuizContent(skillContent.value.content).questions
})
</script>
