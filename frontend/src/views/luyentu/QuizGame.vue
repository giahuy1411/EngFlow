<template>
  <div class="quiz-game w-full max-w-6xl mx-auto px-2 sm:px-4 min-h-[calc(100vh-180px)] flex items-center">
    <div class="relative w-full aspect-video bg-background border-4 border-foreground shadow-hard-lg overflow-hidden">
      <!-- Top bar -->
      <div class="absolute top-0 left-0 right-0 flex justify-between items-center px-4 sm:px-6 py-3 z-10">
        <button @click="$router.push(`/decks/${deckId}`)" class="text-sm sm:text-base font-bold text-foreground/50 hover:text-foreground uppercase border-b-2 border-transparent hover:border-foreground transition-colors">
          &larr; Quit Session
        </button>
        <div class="font-black text-base sm:text-lg bg-primary-yellow border-2 border-foreground px-4 py-1" v-if="questions.length">
          {{ currentIndex + 1 }} / {{ questions.length }}
        </div>
      </div>

      <div v-if="loading" class="absolute inset-0 flex items-center justify-center">
        <div class="animate-spin inline-block w-12 h-12 border-4 border-foreground border-t-primary-red"></div>
      </div>

      <div v-else-if="!sessionComplete && questions.length > 0" class="absolute inset-0 flex flex-col px-6 sm:px-10 pt-16 pb-4 sm:pb-6">
        <!-- Word card -->
        <div class="flex-1 flex items-center justify-center min-h-0 mb-3">
          <div class="w-full bg-white border-4 border-foreground shadow-hard-md text-center py-5 sm:py-6 px-4">
            <h2 class="text-3xl sm:text-5xl font-black break-words leading-tight">{{ currentQuestion.word }}</h2>
            <p class="text-base sm:text-xl font-bold text-foreground/50 font-mono mt-1">{{ currentQuestion.pronunciation }}</p>
          </div>
        </div>

        <!-- Options grid -->
        <div class="flex-1 min-h-0 flex items-center justify-center">
          <div class="grid grid-cols-2 gap-2 sm:gap-3 w-full">
            <button 
              v-for="(option, index) in currentQuestion.options" :key="index"
              @click="selectOption(option)"
              class="flex items-center justify-center px-3 sm:px-5 py-3 sm:py-4 text-center border-4 border-foreground font-bold text-sm sm:text-lg leading-tight transition-all shadow-hard-sm"
              :class="getOptionClass(option, index)"
              :disabled="selectedOption !== null"
            >
              {{ option }}
            </button>
          </div>
        </div>
        
        <!-- Next button -->
        <div v-if="selectedOption !== null" class="flex-none mt-2 animate-fade-in-up">
          <button @click="nextQuestion" class="w-full py-2.5 bg-foreground text-white font-black text-sm sm:text-base uppercase border-4 border-foreground shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all">
            {{ currentIndex < questions.length - 1 ? 'Next Question' : 'Finish Quiz' }}
          </button>
        </div>
      </div>

      <div v-else-if="sessionComplete" class="absolute inset-0 flex items-center justify-center">
        <GameResult 
          :correct="correctAnswers" 
          :total="questions.length" 
          @continue="loadQuiz" 
          @back="$router.push(`/decks/${deckId}`)" 
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import gameService from '@/services/gameService'
import GameResult from '@/components/bauhaus/GameResult.vue'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const deckId = route.params.id
const toast = useToast()

const questions = ref([])
const currentIndex = ref(0)
const selectedOption = ref(null)
const correctAnswers = ref(0)
const loading = ref(true)
const sessionComplete = ref(false)

const sessionId = ref(null)

const currentQuestion = computed(() => questions.value[currentIndex.value])

const optionColors = [
  'bg-primary-red text-white hover:bg-primary-red/90',
  'bg-primary-blue text-white hover:bg-primary-blue/90',
  'bg-primary-yellow text-foreground hover:bg-primary-yellow/90',
  'bg-foreground text-white hover:bg-foreground/90',
  'bg-primary-red text-white hover:bg-primary-red/90',
  'bg-primary-blue text-white hover:bg-primary-blue/90'
]

onMounted(() => {
  loadQuiz()
})

const loadQuiz = async () => {
  loading.value = true
  sessionComplete.value = false
  currentIndex.value = 0
  correctAnswers.value = 0
  selectedOption.value = null
  
  try {
    const response = await gameService.getQuizData(deckId)
    questions.value = response.data
    sessionId.value = response.sessionId
  } catch (error) {
    console.error("Error loading quiz", error)
  } finally {
    loading.value = false
  }
}

const selectOption = (option) => {
  if (selectedOption.value !== null) return
  
  selectedOption.value = option
  if (option === currentQuestion.value.answer) {
    correctAnswers.value++
  }
}

const getOptionClass = (option, index) => {
  if (selectedOption.value === null) {
    return `${optionColors[index % optionColors.length]} hover:-translate-y-1 hover:shadow-hard-md`
  }
  
  if (option === currentQuestion.value.answer) {
    return 'bg-primary-blue text-white shadow-none translate-y-1'
  }
  
  if (selectedOption.value === option) {
    return 'bg-primary-red text-white shadow-none translate-y-1'
  }
  
  return `${optionColors[index % optionColors.length]} opacity-40 shadow-none translate-y-1`
}

const nextQuestion = () => {
  if (currentIndex.value < questions.length - 1) {
    currentIndex.value++
    selectedOption.value = null
  } else {
    finishQuiz()
  }
}

const finishQuiz = async () => {
  try {
    await gameService.submitResult(sessionId.value, correctAnswers.value)
  } catch (error) {
    console.error("Error submitting result", error)
    toast.error('Không thể lưu kết quả. Vui lòng thử lại.')
  }
  sessionComplete.value = true
}
</script>

<style scoped>
.animate-fade-in-up {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
