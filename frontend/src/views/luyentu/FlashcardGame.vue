<template>
  <div class="flashcard-game w-full max-w-6xl mx-auto px-2 sm:px-4 min-h-[calc(100vh-180px)] flex items-center">
    <div class="relative w-full aspect-video bg-background border-4 border-black shadow-hard-lg overflow-hidden">
      <!-- Top bar -->
      <div class="absolute top-0 left-0 right-0 flex justify-between items-center px-4 sm:px-6 py-3 z-10">
        <button @click="$router.push(`/decks/${deckId}`)" class="text-sm sm:text-base font-bold text-foreground/50 hover:text-foreground uppercase border-b-2 border-transparent hover:border-foreground transition-colors">
          &larr; Quit Session
        </button>
        <div class="font-black text-base sm:text-lg bg-primary-yellow border-2 border-black px-4 py-1" v-if="words.length">
          {{ currentIndex + 1 }} / {{ words.length }}
        </div>
      </div>

      <div v-if="loading" class="absolute inset-0 flex items-center justify-center">
        <div class="animate-spin inline-block w-12 h-12 border-4 border-foreground border-t-primary-red"></div>
        <p class="mt-3 font-bold text-base uppercase">Loading cards...</p>
      </div>

      <div v-else-if="!sessionComplete && words.length > 0" class="absolute inset-0 flex flex-col pt-14 px-4 sm:px-6 pb-4 sm:pb-6">
        <div class="flex-1 min-h-0 w-full mb-3">
          <FlashcardFlip 
            :word="currentWord.word"
            :ipa="currentWord.pronunciation"
            :meaning="currentWord.definitionVi"
            :wordType="currentWord.wordType"
            :example="currentWord.exampleSentence"
            :audioUrl="currentWord.audioUrl"
            :flipped="isFlipped"
            :colorIndex="currentIndex"
            @flip="isFlipped = $event"
            :key="currentWord.vocabId"
          />
        </div>

        <div v-if="!isFlipped" class="flex-none text-center">
          <p class="font-bold text-foreground/50 uppercase tracking-widest text-xs sm:text-sm">Think of the answer, then click card to flip</p>
        </div>
        
        <div v-else class="flex-none animate-fade-in-up">
          <p class="font-black text-sm sm:text-base uppercase mb-2 text-center">How well did you know this?</p>
          <div class="grid grid-cols-4 gap-2 sm:gap-3">
            <button @click="rateWord(0)" class="py-2 sm:py-3 bg-primary-red text-white font-black uppercase border-4 border-foreground shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md active:translate-y-0.5 active:shadow-none transition-all text-xs sm:text-sm">
              Forgot
              <span class="block text-[0.6rem] font-bold mt-0.5 opacity-80">&lt; 1 min</span>
            </button>
            <button @click="rateWord(3)" class="py-2 sm:py-3 bg-primary-yellow text-foreground font-black uppercase border-4 border-foreground shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md active:translate-y-0.5 active:shadow-none transition-all text-xs sm:text-sm">
              Hard
              <span class="block text-[0.6rem] font-bold mt-0.5 opacity-80">6 mins</span>
            </button>
            <button @click="rateWord(4)" class="py-2 sm:py-3 bg-primary-blue text-white font-black uppercase border-4 border-foreground shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md active:translate-y-0.5 active:shadow-none transition-all text-xs sm:text-sm">
              Good
              <span class="block text-[0.6rem] font-bold mt-0.5 opacity-80">1 day</span>
            </button>
            <button @click="rateWord(5)" class="py-2 sm:py-3 bg-foreground text-white font-black uppercase border-4 border-foreground shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md active:translate-y-0.5 active:shadow-none transition-all text-xs sm:text-sm">
              Easy
              <span class="block text-[0.6rem] font-bold mt-0.5 opacity-80">4 days</span>
            </button>
          </div>
        </div>
      </div>
      
      <div v-else-if="sessionComplete" class="absolute inset-0 flex items-center justify-center">
        <GameResult 
          :correct="words.length" 
          :total="words.length" 
          @continue="loadDueWords" 
          @back="$router.push(`/decks/${deckId}`)" 
        />
      </div>
      
      <div v-else class="absolute inset-0 flex flex-col items-center justify-center px-6">
        <div class="w-16 h-16 bg-primary-yellow border-4 border-foreground flex items-center justify-center mb-3 rotate-12 shadow-hard-sm">
          <CheckIcon class="w-8 h-8 text-foreground" />
        </div>
        <h2 class="text-xl sm:text-2xl font-black uppercase mb-2">All Caught Up!</h2>
        <p class="font-bold text-foreground/60 mb-4 text-sm max-w-md text-center">You've reviewed all due cards in this deck for today. Great job staying consistent!</p>
        <button @click="$router.push(`/decks/${deckId}`)" class="px-6 py-3 bg-foreground text-white font-black uppercase border-4 border-foreground shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md active:translate-y-0.5 active:shadow-none transition-all text-base">
          Back to Deck
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Check as CheckIcon } from 'lucide-vue-next'
import srsService from '@/services/srsService'
import FlashcardFlip from '@/components/bauhaus/FlashcardFlip.vue'
import GameResult from '@/components/bauhaus/GameResult.vue'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const deckId = route.params.id
const toast = useToast()

const words = ref([])
const currentIndex = ref(0)
const isFlipped = ref(false)
const loading = ref(true)
const sessionComplete = ref(false)


const currentWord = computed(() => words.value[currentIndex.value])

onMounted(() => {
  loadDueWords()
})

const loadDueWords = async () => {
  loading.value = true
  sessionComplete.value = false
  currentIndex.value = 0
  isFlipped.value = false

  
  try {
    const data = await srsService.getDueWords(deckId)
    words.value = Array.isArray(data) ? data.slice(0, 20) : []
  } catch (error) {
    console.error("Error loading due words", error)
  } finally {
    loading.value = false
  }
}

const rateWord = async (quality) => {
  try {
    await srsService.reviewWord(currentWord.value.vocabId, quality)
    
    if (currentIndex.value < words.value.length - 1) {
      isFlipped.value = false
      setTimeout(() => {
        currentIndex.value++
      }, 300)
    } else {
      finishSession()
    }
  } catch (error) {
    console.error("Error saving review", error)
    toast.error('Không thể lưu đánh giá. Vui lòng thử lại.')
  }
}

const finishSession = async () => {
  sessionComplete.value = true
}
</script>

<style scoped>
.animate-fade-in-up {
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
