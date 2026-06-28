<template>
  <div class="vocabulary-skill space-y-8">
    <div v-if="!skillContent" class="bg-white border-4 border-black shadow-hard-lg p-10 text-center">
      <div class="text-6xl mb-4 opacity-30">V</div>
      <p class="font-bold text-xl uppercase">No vocabulary content yet</p>
      <p class="text-gray-500 mt-2">This lesson doesn't have vocabulary exercises.</p>
    </div>

    <div v-else>
      <!-- Video -->
      <div v-if="quizData.videoHtml" class="bg-white border-4 border-black shadow-hard-lg">
        <div class="bg-red-600 text-white px-5 py-2 flex items-center gap-2">
          <span class="w-3 h-3 bg-white rounded-full"></span>
          <span class="font-bold text-sm uppercase tracking-wider">Video</span>
        </div>
        <div class="p-4" v-html="quizData.videoHtml"></div>
      </div>

      <!-- Content -->
      <div class="bg-white border-4 border-black shadow-hard-lg">
        <div class="bg-black text-white px-6 py-4 flex items-center gap-3">
          <span class="w-8 h-8 bg-yellow-400 text-black rounded-full flex items-center justify-center font-black text-sm">V</span>
          <h3 class="font-black text-xl uppercase tracking-tight">Vocabulary</h3>
        </div>
        <div class="px-6 py-6">
          <div v-html="quizData.staticHtml" class="skill-html"></div>
        </div>
      </div>

      <!-- Quiz -->
      <div v-if="quizData.questions.length">
        <div class="flex items-center gap-3 mb-5">
          <div class="h-px flex-1 bg-black/20"></div>
          <span class="font-black text-sm uppercase tracking-widest text-gray-500">Practice</span>
          <div class="h-px flex-1 bg-black/20"></div>
        </div>

        <QuizEngine
          :questions="quizData.questions"
          :lesson-id="lesson.id"
          skill-code="vcb"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { parseQuizContent } from '@/utils/skillParser'
import QuizEngine from '@/components/lessons/QuizEngine.vue'

const props = defineProps({
  lesson: { type: Object, required: true },
  skills: { type: Array, required: true },
})

const skillContent = computed(() => props.skills.find(s => s.skillType === 'VOCABULARY'))
const quizData = computed(() => skillContent.value ? parseQuizContent(skillContent.value.content) : { staticHtml: '', videoHtml: '', questions: [] })
</script>
