<template>
  <div class="grammar-skill">
    <div v-if="!skillContent" class="bg-white border-4 border-black rounded-xl p-8 text-center">
      <p class="font-bold text-lg">No grammar content available for this lesson.</p>
    </div>

    <div v-else>
      <div v-html="quizData.staticHtml" class="mb-8 skill-html"></div>

      <QuizEngine
        v-if="quizData.questions.length"
        :questions="quizData.questions"
        :lesson-id="lesson.id"
        skill-code="gra"
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

const skillContent = computed(() => props.skills.find(s => s.skillType === 'GRAMMAR'))
const quizData = computed(() => skillContent.value ? parseQuizContent(skillContent.value.content) : { staticHtml: '', questions: [] })
</script>
