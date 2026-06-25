<template>
  <div class="vocabulary-skill">
    <div v-if="!skillContent" class="bg-white border-4 border-black rounded-xl p-8 text-center">
      <p class="font-bold text-lg">No vocabulary content available for this lesson.</p>
    </div>

    <div v-else>
      <div class="bg-white border-4 border-black rounded-xl shadow-[10px_10px_0px_0px_rgba(0,0,0,1)] overflow-hidden mb-8">
        <div class="bg-black text-white px-6 py-3">
          <h3 class="font-black text-lg uppercase">Vocabulary</h3>
        </div>
        <div class="p-6">
          <div v-if="quizData.videoHtml" v-html="quizData.videoHtml" class="mb-6"></div>
          <div v-html="quizData.staticHtml" class="skill-html"></div>
        </div>
      </div>

      <QuizEngine
        v-if="quizData.questions.length"
        :questions="quizData.questions"
        :lesson-id="lesson.id"
        skill-code="vcb"
      />
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
const quizData = computed(() => skillContent.value ? parseQuizContent(skillContent.value.content) : { staticHtml: '', questions: [] })
</script>
