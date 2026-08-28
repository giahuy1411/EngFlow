<template>
  <div class="bg-background min-h-screen py-16">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
      <UserPageHeader
        eyebrow="Memory drill"
        title="Ghép cặp"
        subtitle="Tìm cặp từ vựng tương ứng và luyện nhớ nghĩa qua hình thức lật thẻ."
        root-class="mb-10"
      >
        <template #accent>Memory</template>
      </UserPageHeader>

      <div v-if="loading" class="bg-card border-2 border-foreground rounded-md p-12 shadow-pop-xl" role="status">
        <div class="w-10 h-10 mx-auto border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
        <p class="mt-4 font-bold text-sm uppercase tracking-wider text-muted-foreground">Đang tải ghép cặp...</p>
      </div>

      <div v-else-if="error" class="bg-danger/10 border-2 border-danger rounded-md p-8 shadow-pop-xl" role="alert">
        <p class="font-black text-lg text-danger">{{ error }}</p>
        <button class="mt-4 px-5 py-2 border-2 border-foreground rounded-full font-bold uppercase text-sm" @click="loadDeck">Thử lại</button>
      </div>

      <div v-else-if="cards.length === 0" class="bg-card border-2 border-dashed border-foreground rounded-md p-12 shadow-pop-xl">
        <p class="font-black text-2xl uppercase">Chưa đủ từ</p>
        <p class="mt-2 text-muted-foreground font-medium">Bộ từ cần ít nhất một cặp để chơi.</p>
      </div>

      <template v-else>

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
        <p v-if="submitting" class="mb-6 font-bold text-sm uppercase tracking-wider text-muted-foreground">Đang lưu kết quả...</p>
        <p v-else-if="submitResult" class="mb-6 font-black text-lg text-quaternary">
          Đã lưu: {{ submitResult.correctAnswers }}/{{ submitResult.totalQuestions }} đúng · +{{ submitResult.correctAnswers }} điểm
        </p>
        <router-link :to="'/decks/' + deckId"
          class="inline-flex px-8 py-3.5 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
        >Quay lại</router-link>
      </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import gameService from '@/services/gameService'
import { Check } from 'lucide-vue-next'
import UserPageHeader from '@/components/common/UserPageHeader.vue'

const route = useRoute()
const deckId = route.params.id

const sessionId = ref('')
const pairs = ref([])
const cards = ref([])
const flippedIndices = ref([])
const matchedPairs = ref(0)
const attempts = ref(0)
const lockBoard = ref(false)
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const submitResult = ref(null)

onMounted(loadDeck)

async function loadDeck() {
  loading.value = true
  error.value = ''
  submitResult.value = null
  try {
    const data = await gameService.startMemory(deckId)
    sessionId.value = data.sessionId
    const rawCards = data.data.map(c => ({
      id: c.id,
      pairId: c.pairId,
      text: c.content,
      flipped: false,
      matched: false,
    }))
    cards.value = rawCards.sort(() => Math.random() - 0.5)
    const seen = new Set()
    pairs.value = rawCards.filter(c => { const k = c.pairId; if (seen.has(k)) return false; seen.add(k); return true })
    flippedIndices.value = []
    matchedPairs.value = 0
    attempts.value = 0
    lockBoard.value = false
  } catch (e) {
    error.value = e.response?.data?.message || 'Không tải được bộ từ.'
  } finally {
    loading.value = false
  }
}

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
    if (matchedPairs.value === pairs.value.length) {
      submitGame()
    }
  } else {
    setTimeout(() => {
      cards.value[i1].flipped = false
      cards.value[i2].flipped = false
      resetFlipped()
    }, 1000)
  }
}

async function submitGame() {
  submitting.value = true
  try {
    submitResult.value = await gameService.submit(sessionId.value, null, matchedPairs.value)
  } catch (e) {
    // Silent fail — user can still navigate back
  } finally {
    submitting.value = false
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
