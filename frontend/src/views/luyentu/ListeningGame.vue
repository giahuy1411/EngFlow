<template>
  <div class="bg-geo-bg min-h-screen py-16">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
      <UserPageHeader
        eyebrow="Audio drill"
        title="Nghe và chọn"
        subtitle="Nghe từ và chọn đáp án đúng."
        root-class="mb-10"
      >
        <template #accent>Listening</template>
      </UserPageHeader>

      <!-- Audio play button — icon-only circular control (w-20 h-20); keep raw <button> to preserve the circular layout -->
      <div v-if="currentWord" class="mb-8">
        <button @click="playAudio" aria-label="Nghe từ"
          class="w-20 h-20 bg-accent border-2 border-foreground rounded-full flex items-center justify-center mx-auto text-white hover:bg-accent/90 transition-all shadow-pop-xl active:shadow-pop-active animate-pop-in"
        >
          <Volume2 class="w-10 h-10" />
        </button>
        <p class="font-bold text-sm text-muted-foreground mt-4">Nhấn để nghe</p>
      </div>

      <!-- Options — answer buttons with dynamic per-state coloring (answerState); keep raw <button> since AppButton variants don't cover the correct/wrong states -->
      <div v-if="currentWord" class="grid grid-cols-2 gap-4 max-w-lg mx-auto mb-8">
        <button v-for="(opt, oi) in currentWord.options" :key="oi"
          @click="selectAnswer(oi)"
          class="p-6 border-2 border-foreground rounded-md font-black text-xl uppercase tracking-tight transition-all duration-300"
          :class="answerState(oi)"
          :disabled="answered"
        >{{ opt }}</button>
      </div>

      <!-- Score -->
      <div class="flex items-center justify-center gap-8 mb-8">
        <div class="text-center">
          <p class="font-black text-3xl text-accent">{{ correct }}</p>
          <p class="font-bold text-[10px] uppercase tracking-widest text-muted-foreground">Đúng</p>
        </div>
        <div class="text-center">
          <p class="font-black text-3xl">{{ currentIndex }}/{{ words.length }}</p>
          <p class="font-bold text-[10px] uppercase tracking-widest text-muted-foreground">Tiến độ</p>
        </div>
      </div>

      <!-- Completed -->
      <div v-if="currentIndex >= words.length" class="py-8">
        <div class="inline-flex items-center justify-center w-20 h-20 bg-quaternary border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
          <Check class="w-10 h-10 text-white" />
        </div>
        <h2 class="font-black text-3xl uppercase tracking-tight mb-4">Hoàn thành!</h2>
        <router-link :to="'/decks/' + deckId"
          class="inline-flex px-8 py-3.5 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
        >Quay lại</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import deckService from '@/services/deckService'
import { Volume2, Check } from 'lucide-vue-next'
import UserPageHeader from '@/components/common/UserPageHeader.vue'

const route = useRoute()
const deckId = route.params.id
const words = ref([])
const currentIndex = ref(0)
const correct = ref(0)
const answered = ref(false)
const selectedAnswer = ref(null)

const currentWord = computed(() => words.value[currentIndex.value])

onMounted(async () => {
  try {
    const data = await deckService.getDeckById(deckId)
    words.value = (data.words || []).map(w => ({
      ...w,
      options: [w.word, ...(w.distractors || ['???'])].sort(() => Math.random() - 0.5)
    }))
  } catch (e) { console.error(e) }
})

function playAudio() {
  const w = currentWord.value
  if (!w) return
  if (w.audioUrl) {
    new Audio(w.audioUrl).play()
  } else if ('speechSynthesis' in window) {
    const utterance = new SpeechSynthesisUtterance(w.word)
    utterance.lang = 'en-US'
    utterance.rate = 0.85
    window.speechSynthesis.speak(utterance)
  }
}

function selectAnswer(idx) {
  if (answered.value) return
  answered.value = true
  selectedAnswer.value = idx
  if (currentWord.value.options[idx] === currentWord.value.word) {
    correct.value++
    setTimeout(nextWord, 1000)
  } else {
    setTimeout(nextWord, 1500)
  }
}

function answerState(idx) {
  if (!answered.value) return 'bg-card hover:bg-tertiary/10'
  const opt = currentWord.value.options[idx]
  if (opt === currentWord.value.word) return 'bg-quaternary/20 border-quaternary'
  if (idx === selectedAnswer.value) return 'bg-secondary/20 border-secondary'
  return 'opacity-50'
}

function nextWord() {
  currentIndex.value++
  answered.value = false
  selectedAnswer.value = null
}
</script>
