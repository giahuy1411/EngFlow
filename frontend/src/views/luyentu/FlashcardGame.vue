<template>
  <div class="bg-geo-bg min-h-screen py-12">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Back -->
      <router-link :to="'/decks/' + deckId"
        class="inline-flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent transition-colors mb-6"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
        Quay lại
      </router-link>

      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-accent border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
          <BookOpen class="w-8 h-8 text-white" />
        </div>
        <h1 class="font-black text-3xl uppercase tracking-tight mb-4">{{ deck?.name || 'Flashcard' }}</h1>
        <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground mb-8">{{ words.length }} từ</p>

        <!-- Progress bar -->
        <div class="max-w-md mx-auto mb-8">
          <div class="flex justify-between text-xs font-bold uppercase tracking-wider mb-2">
            <span class="text-muted-foreground">{{ currentIndex + 1 }}/{{ words.length }}</span>
            <span class="text-accent">{{ Math.round((currentIndex / words.length) * 100) }}%</span>
          </div>
          <div class="w-full h-2 border-2 border-foreground bg-muted rounded-full overflow-hidden">
            <div class="h-full bg-accent rounded-full transition-all duration-500" :style="{ width: `${(currentIndex / words.length) * 100}%` }"></div>
          </div>
        </div>

        <!-- Card area -->
        <div v-if="currentWord" class="relative mb-8">
          <div @click="flipCard"
            class="bg-card border-2 border-foreground rounded-md p-10 shadow-pop-xl min-h-[280px] flex items-center justify-center cursor-pointer hover:shadow-pop-lg transition-all duration-500 relative"
          >
            <!-- Flip decoration -->
            <div class="absolute top-3 right-3 w-6 h-6 bg-tertiary border-2 border-foreground rounded-sm rotate-12"></div>
            <div class="absolute bottom-3 left-3 w-5 h-5 bg-accent/20 border-2 border-foreground rounded-full"></div>
            
            <div class="text-center">
              <p class="text-xs font-bold uppercase tracking-widest text-muted-foreground mb-2">{{ isFlipped ? 'Nghĩa' : 'Từ' }}</p>
              <h2 class="font-black text-4xl md:text-5xl uppercase tracking-tight">{{ isFlipped ? currentWord.meaning : currentWord.word }}</h2>
              <p v-if="!isFlipped && currentWord.phonetic" class="font-bold text-lg text-muted-foreground mt-2">{{ currentWord.phonetic }}</p>
              <p v-if="isFlipped && currentWord.example" class="font-medium text-muted-foreground mt-4 max-w-lg mx-auto italic">"{{ currentWord.example }}"</p>
            </div>

            <!-- Audio button -->
            <button v-if="currentWord.audioUrl" @click.stop="playAudio(currentWord.audioUrl)"
              class="absolute top-4 left-4 w-10 h-10 bg-accent border-2 border-foreground rounded-full flex items-center justify-center text-white hover:bg-accent/90 transition-all shadow-pop-sm"
            >
              <Volume2 class="w-5 h-5" />
            </button>
          </div>
          <p class="text-center mt-4">
            <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">
              {{ isFlipped ? 'Nhấn để xem từ' : 'Nhấn để lật thẻ' }}
            </span>
          </p>
        </div>

        <!-- Navigation buttons -->
        <div class="flex items-center justify-center gap-4">
          <button @click="markWord('again')"
            class="px-6 py-3 font-bold text-sm bg-secondary text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
          >Lại</button>
          <button @click="markWord('good')"
            class="px-6 py-3 font-bold text-sm bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
          >Tiếp theo</button>
          <button @click="markWord('easy')"
            class="px-6 py-3 font-bold text-sm bg-quaternary text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
          >Dễ</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import deckService from '@/services/deckService'
import { BookOpen, Volume2 } from 'lucide-vue-next'

const route = useRoute()
const deckId = route.params.id
const deck = ref(null)
const words = ref([])
const currentIndex = ref(0)
const isFlipped = ref(false)

const currentWord = computed(() => words.value[currentIndex.value])

onMounted(async () => {
  try {
    const data = await deckService.getDeckById(deckId)
    deck.value = data
    words.value = data.words || []
  } catch (e) { console.error(e) }
})

function flipCard() {
  isFlipped.value = !isFlipped.value
}

function playAudio(url) {
  new Audio(url).play()
}

function markWord(rating) {
  if (currentIndex.value < words.value.length - 1) {
    currentIndex.value++
    isFlipped.value = false
  }
}
</script>
