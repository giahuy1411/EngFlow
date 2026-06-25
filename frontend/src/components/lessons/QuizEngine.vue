<template>
  <div class="quiz-engine w-full max-w-6xl mx-auto">
    <div v-if="loading" class="flex items-center justify-center py-16">
      <div class="animate-spin inline-block w-12 h-12 border-4 border-black border-t-transparent rounded-full"></div>
    </div>

    <div v-else-if="!started" class="text-center py-12">
      <div class="text-5xl mb-4 font-black uppercase">Ready?</div>
      <p class="text-lg mb-6">{{ questions.length }} questions</p>
      <button @click="started = true" class="px-8 py-3 bg-black text-white font-bold uppercase text-lg border-4 border-black rounded-xl shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all">
        Start Quiz
      </button>
    </div>

    <div v-else-if="!finished" class="bg-white border-4 border-black rounded-xl shadow-[10px_10px_0px_0px_rgba(0,0,0,1)] overflow-hidden">
      <div class="bg-black text-white px-6 py-3 flex justify-between items-center">
        <span class="font-bold text-lg">Question {{ currentIdx + 1 }} / {{ questions.length }}</span>
        <span class="font-bold text-lg">{{ correctCount }} correct</span>
      </div>

      <div class="p-6 sm:p-8">
        <div v-html="questions[currentIdx].title" class="text-lg font-bold mb-6"></div>

        <div class="space-y-3">
          <button
            v-for="(option, oIdx) in questions[currentIdx].options"
            :key="oIdx"
            @click="selectOption(oIdx)"
            class="w-full flex items-center gap-4 px-5 py-4 text-left border-4 border-black rounded-xl font-semibold text-base transition-all shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]"
            :class="getOptionClass(oIdx)"
            :disabled="selectedOpt !== null"
          >
            <span class="flex-shrink-0 w-8 h-8 flex items-center justify-center border-2 border-black rounded-full font-black text-sm">
              {{ optionLabels[oIdx] }}
            </span>
            <span v-html="option.text" class="flex-1"></span>
          </button>
        </div>

        <button
          v-if="selectedOpt !== null"
          @click="nextQuestion"
          class="w-full mt-6 py-3 bg-yellow-400 text-black font-black text-lg uppercase border-4 border-black rounded-xl hover:bg-yellow-300 transition-all shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5"
        >
          {{ currentIdx < questions.length - 1 ? 'Next' : 'See Results' }}
        </button>
      </div>
    </div>

    <div v-else class="bg-white border-4 border-black rounded-xl shadow-[10px_10px_0px_0px_rgba(0,0,0,1)] overflow-hidden">
      <div class="bg-black text-white px-6 py-4 text-center">
        <div class="text-3xl font-black uppercase">Results</div>
      </div>
      <div class="p-6 sm:p-8 text-center">
        <div class="text-7xl font-black mb-2">{{ correctCount }}<span class="text-3xl text-gray-400">/{{ questions.length }}</span></div>
        <div class="text-xl font-bold uppercase">{{ percentage }}%</div>
        <div class="w-full bg-gray-200 border-2 border-black rounded-full h-4 mt-4 overflow-hidden">
          <div class="h-full bg-yellow-400 transition-all" :style="{ width: percentage + '%' }"></div>
        </div>
      </div>
      <div class="border-t-4 border-black">
        <div class="px-6 py-3 bg-gray-100 font-bold uppercase text-sm">Question Review</div>
        <div v-for="(q, qi) in questions" :key="qi" class="px-6 py-3 border-t-2 border-gray-200 flex items-center gap-3">
          <span class="flex-shrink-0 w-8 h-8 flex items-center justify-center border-2 border-black rounded-full font-bold text-sm">{{ qi + 1 }}</span>
          <span class="flex-1 text-sm font-medium truncate" v-html="q.title"></span>
          <span v-if="results[qi]" class="flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-full font-bold text-sm" :class="results[qi].correct ? 'bg-green-400' : 'bg-red-400'">
            {{ results[qi].correct ? '✓' : '✗' }}
          </span>
          <span v-else class="flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-full bg-gray-200 font-bold text-sm">—</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import answerKeyService from '@/utils/answerKeyService'

const props = defineProps({
  questions: { type: Array, required: true },
  lessonId: { type: [String, Number], required: true },
  skillCode: { type: String, required: true },
})

const optionLabels = ['A', 'B', 'C', 'D', 'E', 'F']

const loading = ref(true)
const started = ref(false)
const finished = ref(false)
const currentIdx = ref(0)
const selectedOpt = ref(null)
const correctCount = ref(0)
const results = ref([])
const answers = ref({})

const percentage = computed(() =>
  questions.value.length ? Math.round((correctCount.value / questions.value.length) * 100) : 0
)

onMounted(async () => {
  try {
    answers.value = await answerKeyService.getAnswers(props.lessonId, props.skillCode)
  } catch (e) {
    console.error('Failed to load answer key', e)
  }
  loading.value = false
})

function selectOption(oIdx) {
  if (selectedOpt.value !== null) return
  selectedOpt.value = oIdx
  const label = optionLabels[oIdx]
  const expected = answers.value[currentIdx.value]
  // Normalize: compare with label (A/B/C/D) or option index
  const isCorrect = expected != null && (
    expected.toUpperCase() === label ||
    parseInt(expected) === oIdx ||
    expected.toUpperCase() === optionLabels[oIdx]
  )
  if (isCorrect) correctCount.value++
  results.value[currentIdx.value] = { correct: isCorrect, selected: oIdx }
}

function getOptionClass(oIdx) {
  if (selectedOpt.value === null) {
    return 'bg-white hover:bg-yellow-50 hover:-translate-y-0.5 hover:shadow-[4px_6px_0px_0px_rgba(0,0,0,1)]'
  }
  if (optionLabels[oIdx] === answers.value[currentIdx.value]?.toUpperCase()) {
    return 'bg-green-400 border-green-600 text-white shadow-none translate-y-1'
  }
  if (selectedOpt.value === oIdx) {
    return 'bg-red-400 border-red-600 text-white shadow-none translate-y-1'
  }
  return 'bg-gray-100 opacity-40 shadow-none translate-y-1'
}

function nextQuestion() {
  if (currentIdx.value < props.questions.length - 1) {
    currentIdx.value++
    selectedOpt.value = null
  } else {
    finished.value = true
  }
}
</script>
