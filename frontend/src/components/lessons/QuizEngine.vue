<template>
  <div class="quiz-engine w-full max-w-6xl mx-auto">
    <div v-if="loading" class="flex items-center justify-center py-16">
      <div class="animate-spin inline-block w-12 h-12 border-4 border-foreground border-t-transparent rounded-full"></div>
    </div>

    <div v-else-if="!started" class="text-center py-12">
      <div class="text-5xl mb-4 font-black uppercase">Ready?</div>
      <p class="text-lg mb-6">{{ questions.length }} questions</p>
      <button @click="started = true" class="px-8 py-3 bg-foreground text-white font-bold uppercase text-lg border-4 border-foreground rounded-xl shadow-pop-lg hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all">
        Start Quiz
      </button>
    </div>

    <div v-else-if="!finished" class="bg-white border-4 border-foreground rounded-xl shadow-pop-xl overflow-hidden">
      <div class="bg-foreground text-white px-6 py-3 flex justify-between items-center">
        <span class="font-bold text-lg">Question {{ currentIdx + 1 }} / {{ questions.length }}</span>
        <span class="font-bold text-lg">{{ correctCount }} correct</span>
      </div>

      <div class="p-6 sm:p-8">
        <div v-html="safeTitle(questions[currentIdx].title)" class="text-lg font-bold mb-6"></div>

        <div class="space-y-3">
          <button
            v-for="(option, oIdx) in questions[currentIdx].options"
            :key="oIdx"
            @click="selectOption(oIdx)"
            class="w-full flex items-center gap-4 px-5 py-4 text-left border-4 border-foreground rounded-xl font-semibold text-base transition-all shadow-pop"
            :class="getOptionClass(oIdx)"
            :disabled="selectedOpt !== null"
          >
            <span class="flex-shrink-0 w-8 h-8 flex items-center justify-center border-2 border-foreground rounded-full font-black text-sm">
              {{ optionLabels[oIdx] }}
            </span>
            <span v-html="safeTitle(option.text)" class="flex-1"></span>
          </button>
        </div>

        <button
          v-if="selectedOpt !== null"
          @click="nextQuestion"
          class="w-full mt-6 py-3 bg-tertiary text-foreground font-black text-lg uppercase border-4 border-foreground rounded-xl hover:bg-tertiary transition-all shadow-pop hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5"
        >
          {{ currentIdx < questions.length - 1 ? 'Next' : 'See Results' }}
        </button>
      </div>
    </div>

    <div v-else class="bg-white border-4 border-foreground rounded-xl shadow-pop-xl overflow-hidden">
      <div class="bg-foreground text-white px-6 py-4 text-center">
        <div class="text-3xl font-black uppercase">Results</div>
      </div>
      <div class="p-6 sm:p-8 text-center">
        <div class="text-7xl font-black mb-2">{{ correctCount }}<span class="text-3xl text-muted-foreground">/{{ questions.length }}</span></div>
        <div class="text-xl font-bold uppercase">{{ percentage }}%</div>
        <div class="w-full bg-muted border-2 border-foreground rounded-full h-4 mt-4 overflow-hidden">
          <div class="h-full bg-tertiary transition-all" :style="{ width: percentage + '%' }"></div>
        </div>
      </div>
      <div class="border-t-4 border-foreground">
        <div class="px-6 py-3 bg-muted font-bold uppercase text-sm">Question Review</div>
        <div v-for="(q, qi) in questions" :key="qi" class="px-6 py-3 border-t-2 border-border flex items-center gap-3">
          <span class="flex-shrink-0 w-8 h-8 flex items-center justify-center border-2 border-foreground rounded-full font-bold text-sm">{{ qi + 1 }}</span>
          <span class="flex-1 text-sm font-medium truncate" v-html="safeTitle(q.title)"></span>
          <span v-if="results[qi]" class="flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-full font-bold text-sm" :class="results[qi].correct ? 'bg-success' : 'bg-danger'">
            {{ results[qi].correct ? '✓' : '✕' }}
          </span>
          <span v-else class="flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-full bg-muted font-bold text-sm">-</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import DOMPurify from 'dompurify'
import answerKeyService from '@/utils/answerKeyService'

function safeTitle(text) {
  return text ? DOMPurify.sanitize(text) : ''
}

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
  props.questions.length ? Math.round((correctCount.value / props.questions.length) * 100) : 0
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
    return 'bg-white hover:bg-tertiary/10 hover:-translate-y-0.5 hover:shadow-pop'
  }
  if (optionLabels[oIdx] === answers.value[currentIdx.value]?.toUpperCase()) {
    return 'bg-success border-green-600 text-white shadow-none translate-y-1'
  }
  if (selectedOpt.value === oIdx) {
    return 'bg-danger border-danger text-white shadow-none translate-y-1'
  }
  return 'bg-muted opacity-40 shadow-none translate-y-1'
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