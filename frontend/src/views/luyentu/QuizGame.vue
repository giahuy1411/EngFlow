<template>
  <div class="bg-geo-bg min-h-screen py-12">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Back -->
      <router-link :to="'/decks/' + deckId"
        class="inline-flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent transition-colors mb-6"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
        Quay lại
      </router-link>

      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-tertiary border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
          <HelpCircle class="w-8 h-8 text-foreground" />
        </div>
        <h1 class="font-black text-3xl uppercase tracking-tight mb-2">Trắc nghiệm</h1>
        <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground mb-8">{{ words.length }} câu hỏi</p>

        <!-- Progress -->
        <div class="max-w-md mx-auto mb-8">
          <div class="flex justify-between text-xs font-bold uppercase tracking-wider mb-2">
            <span class="text-muted-foreground">{{ currentIndex + 1 }}/{{ words.length }}</span>
            <span class="text-accent">{{ correct }}/{{ total }} đúng</span>
          </div>
          <div class="w-full h-2 border-2 border-foreground bg-muted rounded-full overflow-hidden">
            <div class="h-full bg-accent rounded-full transition-all duration-500" :style="{ width: `${(currentIndex / words.length) * 100}%` }"></div>
          </div>
        </div>

        <!-- Question -->
        <div v-if="currentWord" class="mb-8">
          <p class="text-xs font-bold uppercase tracking-widest text-muted-foreground mb-3">Từ này nghĩa là gì?</p>
          <h2 class="font-black text-4xl uppercase tracking-tight mb-8">{{ currentWord.word }}</h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-xl mx-auto">
            <button v-for="(opt, oi) in currentWord.options" :key="oi"
              @click="selectAnswer(oi)"
              class="p-4 border-2 border-foreground rounded-md font-bold text-base transition-all duration-300"
              :class="answerState(oi)"
              :disabled="answered"
            >{{ opt }}</button>
          </div>
        </div>

        <!-- Score -->
        <div v-if="currentIndex >= words.length" class="py-8">
          <div class="inline-flex items-center justify-center w-20 h-20 bg-quaternary border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
            <Check class="w-10 h-10 text-white" />
          </div>
          <h2 class="font-black text-3xl uppercase tracking-tight mb-2">Hoàn thành!</h2>
          <p class="font-black text-5xl text-accent mb-4">{{ correct }}/{{ total }}</p>
          <router-link :to="'/decks/' + deckId"
            class="inline-flex px-8 py-3.5 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
          >Quay lại</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import deckService from '@/services/deckService'
import { HelpCircle, Check } from 'lucide-vue-next'

const route = useRoute()
const deckId = route.params.id
const deck = ref(null)
const words = ref([])
const currentIndex = ref(0)
const correct = ref(0)
const total = ref(0)
const answered = ref(false)
const selectedAnswer = ref(null)

const currentWord = computed(() => words.value[currentIndex.value])

onMounted(async () => {
  try {
    const data = await deckService.getDeckById(deckId)
    deck.value = data
    words.value = (data.words || []).map(w => ({
      ...w,
      options: w.options || [w.meaning, ...(w.distractors || [])].sort(() => Math.random() - 0.5)
    }))
  } catch (e) { console.error(e) }
})

function selectAnswer(idx) {
  if (answered.value) return
  answered.value = true
  selectedAnswer.value = idx
  total.value++
  if (currentWord.value.options[idx] === currentWord.value.meaning) {
    correct.value++
    setTimeout(nextWord, 1000)
  } else {
    setTimeout(nextWord, 1500)
  }
}

function answerState(idx) {
  if (!answered.value) return 'bg-card hover:bg-tertiary/10'
  const word = currentWord.value.options[idx]
  const correctAnswer = currentWord.value.meaning
  if (word === correctAnswer) return 'bg-quaternary/20 border-quaternary text-quaternary'
  if (idx === selectedAnswer.value) return 'bg-secondary/20 border-secondary text-secondary'
  return 'opacity-50'
}

function nextWord() {
  currentIndex.value++
  answered.value = false
  selectedAnswer.value = null
}
</script>
