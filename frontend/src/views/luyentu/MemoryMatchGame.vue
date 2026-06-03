<template>
  <div class="memory-game w-full max-w-4xl mx-auto px-2 sm:px-4 min-h-[calc(100vh-180px)] flex items-center">
    <div class="relative w-full min-h-[500px] sm:min-h-[700px] bg-gradient-to-br from-purple-50 to-purple-100 border-4 border-black rounded-2xl shadow-[12px_12px_0px_0px_rgba(0,0,0,1)] overflow-hidden">
      <!-- Top bar -->
      <div class="absolute top-0 left-0 right-0 flex justify-between items-center px-4 sm:px-6 py-3 z-10">
        <button @click="$router.push(`/decks/${deckId}`)" class="text-sm sm:text-base font-bold text-gray-500 hover:text-black uppercase border-b-2 border-transparent hover:border-black transition-colors">
          &larr; Quit Session
        </button>
        <div class="font-black text-base sm:text-lg bg-purple-300 border-2 border-black rounded-full px-4 py-1">
          Matches: {{ matchedPairs }} / {{ totalPairs }}
        </div>
      </div>

      <div v-if="loading" class="absolute inset-0 flex items-center justify-center">
        <div class="animate-spin inline-block w-12 h-12 border-4 border-black border-t-transparent rounded-full"></div>
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
            <div class="absolute w-full h-full backface-hidden bg-gradient-to-br from-purple-400 to-purple-600 border-4 border-black rounded-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] flex items-center justify-center text-2xl sm:text-4xl hover:shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:translate-x-0.5 hover:translate-y-0.5 transition-all">
              🧠
            </div>
            
            <div class="absolute w-full h-full backface-hidden bg-white border-4 border-black rounded-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] flex items-center justify-center p-1 rotate-y-180 transition-colors duration-300"
                 :class="{'bg-green-200 border-green-500': card.matched, 'bg-red-200 border-red-500': card.error}">
              <p class="font-bold text-center break-words leading-tight" :class="card.type === 'meaning' ? 'text-[0.5rem] sm:text-base' : 'text-xs sm:text-xl'">
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
          :coins="earnedCoins"
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
const earnedCoins = ref(0)
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
    const result = await gameService.submitResult(sessionId.value, matchedPairs.value)
    earnedCoins.value = result.earnedCoins
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
