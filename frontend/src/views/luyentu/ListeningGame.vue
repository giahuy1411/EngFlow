<template>
  <div class="listening-game w-full max-w-6xl mx-auto px-2 sm:px-4 min-h-[calc(100vh-180px)] flex items-center">
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

      <div v-else-if="!sessionComplete && questions.length > 0" class="absolute inset-0 flex flex-col items-center justify-center px-6 sm:px-12 pt-14 pb-4 sm:pb-6">
        <!-- Status text -->
        <h2 class="text-base sm:text-xl font-black mb-3 uppercase text-foreground/50 flex-none">Listen and type</h2>

        <!-- Audio button -->
        <button 
          @click="playWordAudio" 
          class="flex-none p-6 sm:p-8 bg-primary-yellow text-foreground border-4 border-foreground hover:bg-primary-yellow/90 transition-all shadow-hard-lg hover:shadow-hard-sm hover:translate-x-1 hover:translate-y-1 mb-3"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 sm:h-16 sm:w-16" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072M17.95 6.05a8 8 0 010 11.9M6.5 14h-3a.5.5 0 01-.5-.5v-3a.5.5 0 01.5-.5h3l4-4v12l-4-4z" />
          </svg>
        </button>
        <p class="text-xs font-bold text-foreground/40 uppercase tracking-wider mb-3 flex-none">Click to hear again</p>

        <!-- Input -->
        <div class="w-full max-w-xl flex-none">
          <input 
            v-model="userInput" 
            @keyup.enter="checkAnswer"
            ref="inputField"
            type="text" 
            placeholder="Type what you hear..." 
            class="w-full px-4 sm:px-6 py-3 sm:py-4 text-lg sm:text-2xl font-black text-center bg-white border-4 border-foreground shadow-hard-sm transition-all"
            :class="inputClass"
            :disabled="isChecking"
            autocomplete="off"
          >
        </div>
        
        <!-- Result feedback -->
        <div v-if="isChecking" class="mt-3 w-full max-w-xl animate-fade-in-up flex-none">
          <p class="text-lg sm:text-xl font-black mb-1 text-center" :class="isCorrect ? 'text-primary-blue' : 'text-primary-red'">
            {{ isCorrect ? 'Correct!' : 'Incorrect!' }}
          </p>
          <div v-if="!isCorrect" class="text-base sm:text-lg font-bold mb-2 text-center">
            <span class="text-foreground/50 line-through mr-2">{{ userInput }}</span>
            <span class="text-foreground">{{ currentQuestion.word }}</span>
          </div>
          
          <p class="text-foreground/60 font-bold mb-2 text-sm sm:text-base text-center">{{ currentQuestion.definitionVi }}</p>
          
          <button @click="nextQuestion" class="w-full py-3 bg-foreground text-white font-black text-sm sm:text-base uppercase border-4 border-foreground shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all">
            {{ currentIndex < questions.length - 1 ? 'Next Word' : 'Finish Game' }}
          </button>
        </div>
      </div>

      <div v-else-if="sessionComplete" class="absolute inset-0 flex items-center justify-center">
        <GameResult 
          :correct="correctAnswers" 
          :total="questions.length" 
          :coins="earnedCoins"
          @continue="loadGame" 
          @back="$router.push(`/decks/${deckId}`)" 
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import gameService from '@/services/gameService'
import GameResult from '@/components/bauhaus/GameResult.vue'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const deckId = route.params.id
const toast = useToast()

const questions = ref([])
const currentIndex = ref(0)
const userInput = ref('')
const isChecking = ref(false)
const isCorrect = ref(false)
const correctAnswers = ref(0)
const loading = ref(true)
const sessionComplete = ref(false)
const earnedCoins = ref(0)
const inputField = ref(null)
const sessionId = ref(null)
let currentAudio = null // Singleton audio (H11)

const currentQuestion = computed(() => questions.value[currentIndex.value])

const inputClass = computed(() => {
  if (!isChecking.value) return 'focus-visible:ring-2 focus-visible:ring-primary-yellow focus-visible:ring-offset-2'
  return isCorrect.value ? 'bg-primary-blue/10 border-primary-blue' : 'bg-primary-red/10 border-primary-red'
})

onMounted(() => {
  loadGame()
})

const loadGame = async () => {
  loading.value = true
  sessionComplete.value = false
  currentIndex.value = 0
  correctAnswers.value = 0
  userInput.value = ''
  isChecking.value = false
  
  try {
    const response = await gameService.getListeningData(deckId)
    questions.value = response.data
    sessionId.value = response.sessionId
    focusInput()
    setTimeout(playWordAudio, 500)
  } catch (error) {
    console.error("Error loading listening game", error)
  } finally {
    loading.value = false
  }
}

const focusInput = () => {
  nextTick(() => {
    if (inputField.value) inputField.value.focus()
  })
}

const playWordAudio = () => {
  if (!currentQuestion.value) return
  
  if (currentQuestion.value.audioUrl) {
    if (currentAudio) {
      currentAudio.pause();
      currentAudio.currentTime = 0;
    }
    currentAudio = new Audio(currentQuestion.value.audioUrl)
    currentAudio.play().catch(fallbackAudio)
  } else {
    fallbackAudio()
  }
  focusInput()
}

const fallbackAudio = () => {
  if ('speechSynthesis' in window) {
    const utterance = new SpeechSynthesisUtterance(currentQuestion.value.word)
    utterance.lang = 'en-US'
    window.speechSynthesis.speak(utterance)
  }
}

const checkAnswer = () => {
  if (isChecking.value || !userInput.value.trim()) return
  
  isChecking.value = true
  const correctWord = currentQuestion.value.word.toLowerCase().trim()
  const typedWord = userInput.value.toLowerCase().trim()
  
  isCorrect.value = (correctWord === typedWord)
  if (isCorrect.value) correctAnswers.value++
}

const nextQuestion = () => {
  if (currentIndex.value < questions.length - 1) {
    currentIndex.value++
    userInput.value = ''
    isChecking.value = false
    focusInput()
    setTimeout(playWordAudio, 500)
  } else {
    finishGame()
  }
}

const finishGame = async () => {
  try {
    const result = await gameService.submitResult(sessionId.value, correctAnswers.value)
    earnedCoins.value = result.earnedCoins
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
