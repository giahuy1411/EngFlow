<template>
  <div class="writing-skill space-y-8">
    <div v-if="!skillContent" class="bg-white border-4 border-black rounded-xl shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] p-10 text-center">
      <div class="text-6xl mb-4 opacity-30">W</div>
      <p class="font-bold text-xl uppercase">No writing content yet</p>
      <p class="text-gray-500 mt-2">This lesson doesn't have writing exercises.</p>
    </div>

    <div v-else>
      <!-- Content + Writing form -->
      <div class="bg-white border-4 border-black rounded-xl shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] overflow-hidden">
        <div class="bg-black text-white px-6 py-4 flex items-center gap-3">
          <span class="w-8 h-8 bg-yellow-400 text-black rounded-full flex items-center justify-center font-black text-sm">W</span>
          <h3 class="font-black text-xl uppercase tracking-tight">Writing</h3>
        </div>
        <div class="px-6 py-6">
          <div v-html="skillContent.content" class="skill-html mb-8"></div>

          <div class="border-t-4 border-black pt-6">
            <h4 class="font-black text-base uppercase mb-4 flex items-center gap-2">
              <span class="w-6 h-6 bg-yellow-400 border-2 border-black rounded flex items-center justify-center font-black text-xs">!</span>
              Your Answer
            </h4>

            <div class="space-y-4">
              <div v-for="(area, idx) in textareas" :key="idx">
                <label class="block font-bold text-sm uppercase mb-1.5 text-gray-600">{{ area.label }}</label>
                <textarea
                  v-model="area.value"
                  :rows="area.rows"
                  class="w-full px-4 py-3 border-3 border-black rounded-xl font-medium focus:outline-none focus:border-yellow-400 focus:shadow-[3px_3px_0px_0px_rgba(0,0,0,1)] transition-all resize-vertical"
                  :placeholder="area.placeholder"
                ></textarea>
              </div>
            </div>

            <button
              @click="submitWriting"
              :disabled="submitting || submitted"
              class="w-full mt-6 py-3.5 bg-yellow-400 text-black font-black uppercase text-lg border-4 border-black rounded-xl shadow-[5px_5px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <span v-if="submitting">Submitting...</span>
              <span v-else-if="submitted">Submitted ✓</span>
              <span v-else>Submit Writing</span>
            </button>

            <div v-if="submitted" class="mt-4 p-4 bg-green-100 border-2 border-green-400 rounded-lg text-green-700 font-bold text-center">
              Your writing has been submitted for review.
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { submissionService } from '@/services/submissionService'

const props = defineProps({
  lesson: { type: Object, required: true },
  skills: { type: Array, required: true },
})

const submitting = ref(false)
const submitted = ref(false)

const skillContent = computed(() => props.skills.find(s => s.skillType === 'WRITING'))

const textareas = ref([
  { label: 'Answer 1', rows: 6, value: '', placeholder: 'Write your answer here...' },
  { label: 'Answer 2', rows: 6, value: '', placeholder: 'Write your answer here...' },
  { label: 'Answer 3', rows: 6, value: '', placeholder: 'Write your answer here...' },
  { label: 'Answer 4', rows: 6, value: '', placeholder: 'Write your answer here...' },
])

async function submitWriting() {
  submitting.value = true
  try {
    const submissionText = textareas.value.map(a => a.value).join('\n---\n')
    await submissionService.submitLessonSkill(props.lesson.id, 'WRITING', submissionText)
    submitted.value = true
  } catch (e) {
    console.error('Submission failed', e)
  } finally {
    submitting.value = false
  }
}
</script>
