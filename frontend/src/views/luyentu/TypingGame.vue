<template>
  <div class="typing-game w-full max-w-6xl mx-auto px-2 sm:px-4 min-h-[calc(100vh-180px)] flex items-center">
    <div class="relative w-full aspect-video bg-background border-4 border-foreground shadow-hard-lg overflow-hidden">
      <!-- Top bar -->
      <div class="absolute top-0 left-0 right-0 flex justify-between items-center px-4 sm:px-6 py-3 z-10">
        <button @click="$router.push(`/decks/${deckId}`)" class="text-sm sm:text-base font-bold text-foreground/50 hover:text-foreground uppercase border-b-2 border-transparent hover:border-foreground transition-colors">
          &larr; Quit Session
        </button>
        <div class="font-black text-base sm:text-lg bg-primary-yellow border-2 border-foreground px-4 py-1">
          {{ currentIndex + 1 }} / {{ questions.length }}
        </div>
      </div>

      <div v-if="loading" class="absolute inset-0 flex items-center justify-center">
        <div class="animate-spin inline-block w-12 h-12 border-4 border-foreground border-t-primary-red"></div>
      </div>

      <div v-else-if="!sessionComplete && questions.length > 0" class="absolute inset-0 flex flex-col items-center justify-center px-6 sm:px-12 pt-14 pb-4 sm:pb-6">
        <!-- Progress bar -->
        <div class="w-full max-w-xl h-2 bg-white border-2 border-foreground mb-4 overflow-hidden flex-none">
          <div class="h-full bg-primary-blue transition-all duration-300" :style="{ width: `${(currentIndex / questions.length) * 100}%` }"></div>
        </div>

        <!-- Word meaning card -->
        <div class="flex-none bg-white border-4 border-foreground shadow-hard-md text-center w-full max-w-xl py-3 px-4 mb-4">
          <p class="text-lg sm:text-2xl font-bold text-foreground/50">Type the word for:</p>
          <h2 class="text-2xl sm:text-4xl font-black mt-1">{{ currentQuestion.meaning }}</h2>
          <p class="text-xs sm:text-sm font-mono text-foreground/40 mt-1 italic" v-if="currentQuestion.hint">{{ currentQuestion.hint }}</p>
        </div>

        <!-- Input & submit -->
        <div class="flex-none w-full max-w-xl space-y-3">
          <input 
            ref="inputField"
            v-model="userInput"
            @keyup.enter="checkAnswer"
            placeholder="Type the word..."
            class="w-full p-3 border-4 border-foreground font-bold text-base sm:text-lg outline-none shadow-hard-sm transition-colors"
            :class="inputClass"
            :disabled="isChecking"
          />

          <button 
            @click="checkAnswer"
            class="w-full py-3 bg-foreground text-white font-black text-sm sm:text-base uppercase border-4 border-foreground shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all"
            :disabled="!userInput.trim() || isChecking"
          >
            Submit
          </button>

          <!-- Result feedback -->
          <div v-if="isChecking" class="animate-fade-in-up text-center">
            <div v-if="isCorrect" class="bg-primary-blue text-white border-4 border-foreground py-2">
              <p class="font-black text-base">Correct! +10 coins</p>
            </div>
            <div v-else class="bg-primary-red text-white border-4 border-foreground py-2">
              <p class="font-black text-base">Answer: <span class="font-bold underline">{{ currentQuestion.word }}</span></p>
            </div>
            <button @click="nextQuestion" class="mt-2 w-full py-2 bg-foreground text-white font-black text-sm uppercase border-4 border-foreground shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all">
              {{ currentIndex < questions.length - 1 ? 'Next Word' : 'Finish Typing' }}
            </button>
          </div>
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
import AudioButton from '@/components/bauhaus/AudioButton.vue'
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

const currentQuestion = computed(() => questions.value[currentIndex.value])

const inputClass = computed(() => {
  if (!isChecking.value) return 'focus-visible:ring-2 focus-visible:ring-primary-red focus-visible:ring-offset-2'
  return isCorrect.value ? 'bg-primary-blue/10 border-primary-blue' : 'bg-primary-red/10 border-primary-red text-primary-red line-through'
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
    const response = await gameService.getTypingData(deckId)
    questions.value = response.data
    sessionId.value = response.sessionId
    focusInput()
  } catch (error) {
    console.error("Error loading typing game", error)
  } finally {
    loading.value = false
  }
}

const focusInput = () => {
  nextTick(() => {
    if (inputField.value) inputField.value.focus()
  })
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
