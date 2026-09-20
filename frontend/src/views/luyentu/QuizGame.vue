<template>
  <div class="min-h-screen bg-background py-12">
    <div class="max-w-3xl mx-auto px-4">
      <div v-if="loading" class="flex items-center justify-center py-16">
        <div class="animate-spin inline-block w-12 h-12 border-4 border-foreground border-t-accent rounded-full"></div>
      </div>

      <div v-else-if="error" class="max-w-xl mx-auto text-center py-12">
        <div class="inline-flex items-center justify-center w-16 h-16 border-2 border-foreground bg-accent/10 mb-6 rounded-blob">
          <span class="text-3xl font-black text-accent-ink">!</span>
        </div>
        <p class="font-bold text-lg text-accent-ink">{{ error }}</p>
      </div>
      <UserPageHeader v-else-if="deck" eyebrow="Trắc nghiệm" :title="deck.name || 'Trắc nghiệm từ vựng'" subtitle="Chọn đáp án đúng cho từ xuất hiện" root-class="mb-8" />

      <div v-if="!loading && !error" class="bg-card border-2 border-foreground rounded-md shadow-pop-lg p-6 sm:p-8">
        <!-- Progress -->
        <div class="flex items-center gap-3 mb-6">
          <div class="flex-1 bg-muted border-2 border-foreground rounded-full h-3 overflow-hidden">
            <div class="h-full bg-accent rounded-full transition-all duration-500" :style="{ width: `${progressPercent}%` }"></div>
          </div>
          <span class="font-black text-sm uppercase whitespace-nowrap">{{ correct }}/{{ total }}</span>
        </div>

        <!-- Word -->
        <div v-if="currentWord" class="text-center mb-8">
          <h2 class="font-black text-4xl uppercase tracking-tight mb-1">{{ currentWord.word }}</h2>
          <p v-if="currentWord.pronunciation" class="font-bold text-muted-foreground">{{ currentWord.pronunciation }}</p>
        </div>

        <!-- Options — answer buttons with dynamic per-state coloring (answerState: correct=quaternary, wrong=secondary); keep raw <button> since AppButton variants don't cover these states -->
        <div class="space-y-3">
          <button v-for="(opt, idx) in currentWord?.options || []" :key="idx"
            @click="selectAnswer(idx)"
            class="w-full px-5 py-4 border-2 border-foreground rounded-md font-bold text-left transition-all shadow-pop-sm"
            :class="answerState(idx)"
            :disabled="answered"
          >
            {{ opt }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import gameService from '@/services/gameService'
import UserPageHeader from '@/components/common/UserPageHeader.vue'

const route = useRoute()
const deckId = route.params.id
const deck = ref(null)
const words = ref([])
const currentIndex = ref(0)
const correct = ref(0)
const total = ref(0)
const answered = ref(false)
const selectedAnswer = ref(null)
const loading = ref(true)
const error = ref('')
let sessionId = null
const userAnswers = []

const currentWord = computed(() => words.value[currentIndex.value])
const displayIndex = computed(() => Math.min(currentIndex.value + 1, words.value.length))
const progressPercent = computed(() => words.value.length ? Math.round((Math.min(currentIndex.value, words.value.length) / words.value.length) * 100) : 0)

onMounted(loadGame)

async function loadGame() {
  loading.value = true
  error.value = ''
  sessionId = null
  userAnswers.length = 0
  try {
    const data = await gameService.startQuiz(deckId)
    sessionId = data.sessionId
    deck.value = data
    // Backend returns { sessionId, data: [{ vocabId, word, pronunciation, options }] } - answer stored server-side only
    const questions = ((data.data || data.questions) || []).filter(q => q.word && Array.isArray(q.options) && q.options.length)
    words.value = questions.map(q => ({
      vocabId: q.vocabId,
      word: q.word,
      pronunciation: q.pronunciation,
      options: q.options,
    }))
    currentIndex.value = 0
    correct.value = 0
    total.value = 0
    answered.value = false
    selectedAnswer.value = null
  } catch (e) {
    error.value = e.response?.data?.message || 'Không tải được game.'
  } finally {
    loading.value = false
  }
}

async function submitResult() {
  if (!sessionId) return
  try {
    const res = await gameService.submit(sessionId, userAnswers, 0)
    if (res && typeof res.correctAnswers === 'number') correct.value = res.correctAnswers
  } catch (e) {
    console.error('Game submit failed:', e)
  }
}

function selectAnswer(idx) {
  if (answered.value) return
  answered.value = true
  selectedAnswer.value = idx
  total.value++
  userAnswers.push({ vocabId: currentWord.value.vocabId, answer: currentWord.value.options[idx] })
  if (currentIndex.value >= words.value.length - 1) {
    setTimeout(() => submitResult(), 300)
  } else {
    setTimeout(nextWord, 600)
  }
}

function answerState(idx) {
  if (!answered.value) return 'bg-card hover:bg-tertiary/10'
  if (idx === selectedAnswer.value) return 'bg-accent/20 border-accent text-foreground'
  return 'opacity-60'
}

function nextWord() {
  currentIndex.value++
  answered.value = false
  selectedAnswer.value = null
}
</script>