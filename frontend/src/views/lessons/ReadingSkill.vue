<template>
  <div class="reading-skill">
    <div v-if="!skillContent" class="bg-white border-4 border-black rounded-xl p-8 text-center">
      <p class="font-bold text-lg">No reading content available for this lesson.</p>
    </div>

    <div v-else>
      <div class="bg-white border-4 border-black rounded-xl shadow-[10px_10px_0px_0px_rgba(0,0,0,1)] overflow-hidden mb-8">
        <div class="bg-black text-white px-6 py-3">
          <h3 class="font-black text-lg uppercase">Reading</h3>
        </div>
        <div class="p-6">
          <div v-html="readingData.staticHtml" class="skill-html"></div>
        </div>
      </div>

      <ReadingQuestions
        v-if="readingData.questions.length"
        :questions="readingData.questions"
        :answer-key="answerKey"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { parseReadingContent } from '@/utils/skillParser'
import answerKeyService from '@/utils/answerKeyService'
import ReadingQuestions from '@/components/lessons/ReadingQuestions.vue'

const props = defineProps({
  lesson: { type: Object, required: true },
  skills: { type: Array, required: true },
})

const answerKey = ref({})
const skillContent = computed(() => props.skills.find(s => s.skillType === 'READING'))
const readingData = computed(() => skillContent.value ? parseReadingContent(skillContent.value.content) : { staticHtml: '', questions: [] })

onMounted(async () => {
  try {
    answerKey.value = await answerKeyService.getAnswers(props.lesson.id, 'rea')
  } catch (e) {
    console.error('Failed to load reading answer key', e)
  }
})
</script>
