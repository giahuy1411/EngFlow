<template>
  <div class="reading-skill space-y-8">
    <div v-if="!skillContent" class="bg-white border-4 border-black rounded-xl shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] p-10 text-center">
      <div class="text-6xl mb-4 opacity-30">R</div>
      <p class="font-bold text-xl uppercase">No reading content yet</p>
      <p class="text-gray-500 mt-2">This lesson doesn't have reading exercises.</p>
    </div>

    <div v-else>
      <!-- Video -->
      <div v-if="readingData.videoHtml" class="bg-white border-4 border-black rounded-xl shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] overflow-hidden">
        <div class="bg-red-600 text-white px-5 py-2 flex items-center gap-2">
          <span class="w-3 h-3 bg-white rounded-full"></span>
          <span class="font-bold text-sm uppercase tracking-wider">Video</span>
        </div>
        <div class="p-4" v-html="readingData.videoHtml"></div>
      </div>

      <!-- Content -->
      <div class="bg-white border-4 border-black rounded-xl shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] overflow-hidden">
        <div class="bg-black text-white px-6 py-4 flex items-center gap-3">
          <span class="w-8 h-8 bg-yellow-400 text-black rounded-full flex items-center justify-center font-black text-sm">R</span>
          <h3 class="font-black text-xl uppercase tracking-tight">Reading</h3>
        </div>
        <div class="px-6 py-6">
          <div v-html="readingData.staticHtml" class="skill-html"></div>
        </div>
      </div>

      <!-- Questions -->
      <div v-if="readingData.questions.length">
        <div class="flex items-center gap-3 mb-5">
          <div class="h-px flex-1 bg-black/20"></div>
          <span class="font-black text-sm uppercase tracking-widest text-gray-500">Questions</span>
          <div class="h-px flex-1 bg-black/20"></div>
        </div>

        <ReadingQuestions
          :questions="readingData.questions"
          :answer-key="answerKey"
        />
      </div>
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
const readingData = computed(() => skillContent.value ? parseReadingContent(skillContent.value.content) : { staticHtml: '', videoHtml: '', questions: [] })

onMounted(async () => {
  try {
    answerKey.value = await answerKeyService.getAnswers(props.lesson.id, 'rea')
  } catch (e) {
    console.error('Failed to load reading answer key', e)
  }
})
</script>
