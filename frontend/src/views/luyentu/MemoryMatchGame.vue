<template>
  <div class="memory-game w-full max-w-4xl mx-auto px-2 sm:px-4 min-h-[calc(100vh-180px)] flex items-center">
    <div class="relative w-full min-h-[500px] sm:min-h-[700px] bg-background border-4 border-foreground shadow-hard-lg overflow-hidden">
      <!-- Top bar -->
      <div class="absolute top-0 left-0 right-0 flex justify-between items-center px-4 sm:px-6 py-3 z-10">
        <button @click="$router.push(`/decks/${deckId}`)" class="text-sm sm:text-base font-bold text-foreground/50 hover:text-foreground uppercase border-b-2 border-transparent hover:border-foreground transition-colors">
          &larr; Quit Session
        </button>
        <div class="font-black text-base sm:text-lg bg-primary-yellow border-2 border-foreground px-4 py-1">
          Matches: {{ matchedPairs }} / {{ totalPairs }}
        </div>
      </div>

      <div v-if="loading" class="absolute inset-0 flex items-center justify-center">
        <div class="animate-spin inline-block w-12 h-12 border-4 border-foreground border-t-primary-red"></div>
      </div>

      <div v-else-if="!sessionComplete && cards.length > 0" class="absolute inset-0 grid grid-cols-4 gap-2 sm:gap-4 p-4 sm:p-8 pt-16 sm:pt-20 content-center justify-items-center">
        <div 
          v-for="card in cards" :key="card.id"
          class="card-container w-full max-w-[120px] sm:max-w-[160px] aspect-square perspective-1000 cursor-pointer"
          @click="flipCard(card)"
        >
          <div 
            class="card-inner w-full h-full transition-transform duration-500 transform-style-preserve-3d relative"
            :class="{ 'rotate-y-180': card.flipped || card.matched }"
          >
            <div class="absolute w-full h-full backface-hidden bg-foreground border-4 border-foreground shadow-hard-sm flex items-center justify-center text-2xl sm:text-4xl hover:shadow-hard-sm hover:translate-x-0.5 hover:translate-y-0.5 transition-all">
              <div class="w-8 h-8 bg-primary-yellow border-2 border-white rotate-45"></div>
            </div>
            
            <div class="absolute w-full h-full backface-hidden bg-white border-4 border-foreground shadow-hard-sm flex items-center justify-center p-1 rotate-y-180 transition-colors duration-300"
                 :class="{'bg-primary-blue border-primary-blue': card.matched, 'bg-primary-red border-primary-red': card.error}">
              <p class="font-bold text-center break-words leading-tight" :class="[card.type === 'meaning' ? 'text-[0.5rem] sm:text-base' : 'text-xs sm:text-xl', (card.matched || card.error) ? 'text-white' : 'text-foreground']">
                {{ card.content }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="sessionComplete" class="absolute inset-0 flex items-center justify-center">
        <GameResult 
          :correct="totalPairs" 
          :total="totalPairs" 
          @continue="loadGame" 
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

const cards = ref([])
const flippedCards = ref([])
const loading = ref(true)
const sessionComplete = ref(false)

const sessionId = ref(null)

const totalPairs = computed(() => Math.floor(cards.value.length / 2))
const matchedPairs = computed(() => Math.floor(cards.value.filter(c => c.matched).length / 2))

onMounted(() => {
  loadGame()
})

const loadGame = async () => {
  loading.value = true
  sessionComplete.value = false
  flippedCards.value = []
  
  try {
    const response = await gameService.getMemoryData(deckId)
    sessionId.value = response.sessionId
    cards.value = response.data.map(c => ({
      ...c,
      flipped: false,
      matched: false,
      error: false
    }))
  } catch (error) {
    console.error("Error loading memory game", error)
  } finally {
    loading.value = false
  }
}

const flipCard = (card) => {
  if (card.flipped || card.matched || flippedCards.value.length >= 2) return
  
  card.flipped = true
  flippedCards.value.push(card)
  
  if (flippedCards.value.length === 2) {
    checkMatch()
  }
}

const checkMatch = () => {
  const [card1, card2] = flippedCards.value
  
  if (card1.pairId === card2.pairId) {
    card1.matched = true
    card2.matched = true
    flippedCards.value = []
    
    if (matchedPairs.value === totalPairs.value) {
      setTimeout(() => finishGame(), 500)
    }
  } else {
    card1.error = true
    card2.error = true
    
    setTimeout(() => {
      card1.flipped = false
      card2.flipped = false
      card1.error = false
      card2.error = false
      flippedCards.value = []
    }, 1000)
  }
}

const finishGame = async () => {
  if (!cards.value.length) return
  try {
    await gameService.submitResult(sessionId.value, matchedPairs.value)
  } catch (error) {
    console.error("Error submitting result", error)
    toast.error('Không thể lưu kết quả. Vui lòng thử lại.')
  }
  sessionComplete.value = true
}
</script>

<style scoped>
.perspective-1000 {
  perspective: 1000px;
}
.transform-style-preserve-3d {
  transform-style: preserve-3d;
}
.backface-hidden {
  backface-visibility: hidden;
}
.rotate-y-180 {
  transform: rotateY(180deg);
}
</style>
