<template>
  <div class="bg-geo-bg min-h-screen py-16">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="text-center mb-12">
        <h1 class="font-black text-5xl md:text-6xl uppercase tracking-tight leading-none">
          Tra <span class="text-accent">từ</span>
        </h1>
        <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground mt-3">Tra cứu từ vựng tiếng Anh</p>
      </div>

      <!-- Search -->
      <div class="max-w-2xl mx-auto mb-12">
        <div class="flex gap-2">
          <input v-model="query" @keyup.enter="search" placeholder="Nhập từ cần tra..."
            class="flex-1 bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
          />
          <button @click="search" :disabled="!query.trim()"
            class="px-8 py-3 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5 transition-all duration-300 ease-bounce disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            <Search class="w-5 h-5" />
          </button>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-16">
        <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
      </div>

      <!-- Error -->
      <div v-if="error" class="max-w-xl mx-auto text-center py-12">
        <div class="inline-flex items-center justify-center w-16 h-16 border-2 border-foreground bg-accent/10 mb-6 rounded-blob">
          <span class="text-3xl font-black text-accent">!</span>
        </div>
        <p class="font-bold text-lg text-accent">{{ error }}</p>
      </div>

      <!-- Results -->
      <div v-if="results.length > 0" class="space-y-4">
        <div v-for="(word, i) in results" :key="i"
          class="bg-card border-2 border-foreground rounded-md p-6 shadow-pop hover:shadow-pop-lg transition-all duration-300"
        >
          <div class="flex items-start justify-between mb-3">
            <div>
              <h3 class="font-black text-2xl uppercase tracking-tight">{{ word.word }}</h3>
              <p class="font-bold text-sm text-muted-foreground mt-1">{{ word.phonetic || '/' + word.word + '/' }}</p>
            </div>
            <button v-if="word.audioUrl" @click="playAudio(word.audioUrl)"
              class="w-10 h-10 bg-accent border-2 border-foreground rounded-full flex items-center justify-center text-white hover:bg-accent/90 transition-all shadow-pop-sm"
            >
              <Volume2 class="w-5 h-5" />
            </button>
          </div>

          <div class="border-t-2 border-foreground/10 pt-3 space-y-2">
            <div v-for="(meaning, j) in word.meanings" :key="j">
              <span class="font-bold text-xs uppercase tracking-wider text-accent">{{ meaning.partOfSpeech }}</span>
              <p class="font-medium text-foreground mt-0.5">{{ meaning.definition }}</p>
              <p v-if="meaning.example" class="font-medium text-sm text-muted-foreground italic mt-1">"{{ meaning.example }}"</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import vocabularyService from '@/services/vocabularyService'
import { Search, Volume2 } from 'lucide-vue-next'

const query = ref('')
const results = ref([])
const loading = ref(false)
const error = ref('')

async function search() {
  if (!query.value.trim()) return
  loading.value = true
  error.value = ''
  results.value = []
  try {
    results.value = await vocabularyService.search(query.value.trim())
  } catch (e) {
    error.value = e.response?.data?.message || 'Không tìm thấy từ'
  } finally {
    loading.value = false
  }
}

function playAudio(url) {
  new Audio(url).play()
}
</script>
