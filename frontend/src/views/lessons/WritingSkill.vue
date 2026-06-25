<template>
  <div class="writing-skill">
    <div v-if="!skillContent" class="bg-white border-4 border-black rounded-xl p-8 text-center">
      <p class="font-bold text-lg">No writing content available for this lesson.</p>
    </div>

    <div v-else>
      <div class="bg-white border-4 border-black rounded-xl shadow-[10px_10px_0px_0px_rgba(0,0,0,1)] overflow-hidden mb-8">
        <div class="bg-black text-white px-6 py-3">
          <h3 class="font-black text-lg uppercase">Writing</h3>
        </div>
        <div class="p-6">
          <div v-html="skillContent.content" class="skill-html mb-6"></div>

          <div class="space-y-4">
            <div v-for="(area, idx) in textareas" :key="idx">
              <label class="block font-bold text-sm uppercase mb-2">{{ area.label }}</label>
              <textarea
                v-model="area.value"
                :rows="area.rows"
                class="w-full px-4 py-3 border-4 border-black rounded-xl font-medium focus:outline-none focus:border-yellow-400 resize-vertical"
                :placeholder="area.placeholder"
              ></textarea>
            </div>
          </div>

          <button
            @click="submitWriting"
            :disabled="submitting"
            class="w-full mt-6 py-3 bg-yellow-400 text-black font-black uppercase text-lg border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ submitting ? 'Submitting...' : (submitted ? 'Submitted' : 'Submit Writing') }}
          </button>

          <div v-if="submitted" class="mt-4 p-4 bg-green-100 border-2 border-green-400 rounded-lg text-green-700 font-bold text-center">
            Writing submitted successfully!
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
