<template>
  <div class="bg-geo-bg min-h-screen py-16">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
      <div class="inline-flex items-center justify-center w-16 h-16 bg-accent border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
        <Keyboard class="w-8 h-8 text-white" />
      </div>
      <h1 class="font-black text-3xl uppercase tracking-tight mb-2">Gõ từ</h1>
      <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground mb-10">Gõ từ tiếng Anh dựa vào nghĩa</p>

      <div v-if="currentWord" class="mb-8">
        <!-- Meaning prompt -->
        <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl mb-6">
          <p class="text-xs font-bold uppercase tracking-widest text-muted-foreground mb-2">Nghĩa</p>
          <h2 class="font-black text-3xl uppercase tracking-tight">{{ currentWord.meaning }}</h2>
        </div>

        <!-- Input -->
        <div class="max-w-md mx-auto">
          <input v-model="userInput" @keyup.enter="checkAnswer"
            placeholder="Gõ từ..."
            class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-lg text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground text-center uppercase tracking-wider font-black"
            autofocus
          />
          <button @click="checkAnswer"
            class="mt-4 px-10 py-3.5 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5 transition-all"
          :disabled="!userInput.trim()">Kiểm tra</button>
          <p v-if="feedback" class="mt-4 font-bold text-sm uppercase tracking-wider" :class="feedback === correct ? 'text-quaternary' : 'text-secondary'">
            {{ feedback === correct ? 'Đúng!' : ('Sai. Đáp án: ' + correct) }}
          </p>
        </div>
      </div>

      <!-- Progress -->
      <div class="max-w-md mx-auto">
        <div class="flex justify-between text-xs font-bold uppercase tracking-wider mb-2 mt-8">
          <span class="text-muted-foreground">{{ currentIndex + 1 }}/{{ words.length }}</span>
          <span class="text-accent">{{ correct }}/{{ total }} đúng</span>
        </div>
        <div class="w-full h-2 border-2 border-foreground bg-muted rounded-full overflow-hidden">
          <div class="h-full bg-accent rounded-full transition-all" :style="{ width: `${(currentIndex / words.length) * 100}%` }"></div>
        </div>
      </div>

      <!-- Completed -->
      <div v-if="currentIndex >= words.length" class="py-12">
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import deckService from '@/services/deckService'
import { Keyboard, Check } from 'lucide-vue-next'

const route = useRoute()
const deckId = route.params.id
const words = ref([])
const currentIndex = ref(0)
const correct = ref(0)
const total = ref(0)
const userInput = ref('')
const feedback = ref('')
const currentWord = computed(() => words.value[currentIndex.value])

onMounted(async () => {
  try {
    const data = await deckService.getDeckById(deckId)
    words.value = data.words || []
  } catch (e) { console.error(e) }
})

function checkAnswer() {
  if (!userInput.value.trim()) return
  total.value++
  if (userInput.value.trim().toLowerCase() === currentWord.value.word.toLowerCase()) {
    correct.value++
    feedback.value = '✓'
    setTimeout(nextWord, 800)
  } else {
    feedback.value = currentWord.value.word
    setTimeout(nextWord, 1200)
  }
}

function nextWord() {
  currentIndex.value++
  userInput.value = ''
  feedback.value = ''
  nextTick(() => document.querySelector('input')?.focus())
}
</script>
