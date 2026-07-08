<template>
  <div class="bg-geo-bg min-h-screen py-16">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
      <div class="inline-flex items-center justify-center w-16 h-16 bg-secondary border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
        <Grid3x3 class="w-8 h-8 text-white" />
      </div>
      <h1 class="font-black text-3xl uppercase tracking-tight mb-2">Ghép cặp</h1>
      <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground mb-10">Tìm cặp từ vựng tương ứng</p>

      <!-- Grid -->
      <div class="grid grid-cols-4 gap-3 max-w-xl mx-auto mb-8">
        <button v-for="(card, idx) in cards" :key="idx"
          @click="flipCard(idx)"
          class="aspect-square border-2 border-foreground rounded-md flex items-center justify-center font-black text-lg uppercase transition-all duration-300 shadow-pop-sm"
          :class="getCardClass(card)"
          :disabled="card.matched || (flippedIndices.length >= 2)"
        >
          <span v-if="card.flipped || card.matched" class="animate-pop-in">{{ card.text }}</span>
          <span v-else class="text-2xl text-muted-foreground">?</span>
        </button>
      </div>

      <!-- Stats -->
      <div class="flex items-center justify-center gap-8 mb-8">
        <div class="text-center">
          <p class="font-black text-3xl text-accent">{{ matchedPairs }}</p>
          <p class="font-bold text-[10px] uppercase tracking-widest text-muted-foreground">Cặp</p>
        </div>
        <div class="text-center">
          <p class="font-black text-3xl">{{ attempts }}</p>
          <p class="font-bold text-[10px] uppercase tracking-widest text-muted-foreground">Lần thử</p>
        </div>
      </div>

      <!-- Completed -->
      <div v-if="matchedPairs === pairs.length" class="py-8">
        <div class="inline-flex items-center justify-center w-20 h-20 bg-quaternary border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
          <Check class="w-10 h-10 text-white" />
        </div>
        <router-link :to="'/decks/' + deckId"
          class="inline-flex px-8 py-3.5 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
        >Quay lại</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import deckService from '@/services/deckService'
import { Grid3x3, Check } from 'lucide-vue-next'

const route = useRoute()
const deckId = route.params.id

const pairs = ref([])
const cards = ref([])
const flippedIndices = ref([])
const matchedPairs = ref(0)
const attempts = ref(0)
const lockBoard = ref(false)

onMounted(async () => {
  try {
    const data = await deckService.getDeckById(deckId)
    const words = data.words || []
    pairs.value = words.slice(0, 8).map(w => ({ word: w.word, meaning: w.meaning }))
    const rawCards = []
    pairs.value.forEach((p, i) => {
      rawCards.push({ id: i * 2, pairId: i, text: p.word, flipped: false, matched: false })
      rawCards.push({ id: i * 2 + 1, pairId: i, text: p.meaning, flipped: false, matched: false })
    })
    cards.value = rawCards.sort(() => Math.random() - 0.5)
  } catch (e) { console.error(e) }
})

function flipCard(idx) {
  if (lockBoard.value || cards.value[idx].flipped || cards.value[idx].matched) return
  cards.value[idx].flipped = true
  flippedIndices.value.push(idx)
  if (flippedIndices.value.length === 2) {
    attempts.value++
    checkMatch()
  }
}

function checkMatch() {
  lockBoard.value = true
  const [i1, i2] = flippedIndices.value
  if (cards.value[i1].pairId === cards.value[i2].pairId) {
    cards.value[i1].matched = true
    cards.value[i2].matched = true
    matchedPairs.value++
    resetFlipped()
  } else {
    setTimeout(() => {
      cards.value[i1].flipped = false
      cards.value[i2].flipped = false
      resetFlipped()
    }, 1000)
  }
}

function resetFlipped() {
  flippedIndices.value = []
  lockBoard.value = false
}

function getCardClass(card) {
  if (card.matched) return 'bg-quaternary/20 border-quaternary text-quaternary'
  if (card.flipped) return 'bg-card shadow-pop-lg border-accent text-foreground'
  return 'bg-card text-transparent hover:bg-tertiary/10'
}
</script>
