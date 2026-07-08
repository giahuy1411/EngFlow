<template>
  <div class="bg-geo-bg min-h-screen py-16">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="flex items-center gap-4 mb-10">
        <router-link to="/decks"
          class="w-10 h-10 border-2 border-foreground rounded-full flex items-center justify-center bg-card hover:bg-tertiary/20 transition-all"
        >
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
        </router-link>
        <div>
          <h1 class="font-black text-3xl uppercase tracking-tight">AI Generator</h1>
          <p class="font-bold text-xs uppercase tracking-wider text-muted-foreground mt-1">Tự động sinh từ vựng</p>
        </div>
      </div>

      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl">
        <form @submit.prevent="generate" class="space-y-6">
          <div>
            <label class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Chủ đề</label>
            <input v-model="topic" placeholder="e.g. Environment, Technology, Travel..." required
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            />
          </div>

          <div>
            <label class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">CEFR Level</label>
            <select v-model="level"
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none"
            >
              <option v-for="lv in ['A1','A2','B1','B2','C1','C2']" :key="lv" :value="lv">{{ lv }}</option>
            </select>
          </div>

          <div>
            <label class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Số lượng từ</label>
            <input v-model.number="count" type="number" min="3" max="20" required
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none"
            />
          </div>

          <p v-if="error" class="font-bold text-xs uppercase tracking-wider text-secondary">{{ error }}</p>

          <button type="submit" :disabled="generating"
            class="w-full py-3.5 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5 transition-all duration-300 ease-bounce disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            <span v-if="generating" class="flex items-center justify-center gap-2">
              <span class="inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
              Đang sinh...
            </span>
            <span v-else>Sinh từ vựng</span>
          </button>
        </form>

        <!-- Results -->
        <div v-if="generatedWords.length > 0" class="mt-10 space-y-4">
          <h2 class="font-black text-xl uppercase tracking-tight">Kết quả</h2>
          <div v-for="(w, i) in generatedWords" :key="i"
            class="border-2 border-foreground rounded-md p-5 shadow-pop-lg"
          >
            <div class="flex items-start justify-between">
              <div>
                <h3 class="font-black text-lg uppercase">{{ w.word }}</h3>
                <p class="font-bold text-xs text-muted-foreground">{{ w.phonetic }}</p>
              </div>
              <span class="px-2 py-0.5 bg-accent/10 border-2 border-foreground rounded-full text-xs font-bold">{{ w.type }}</span>
            </div>
            <p class="font-medium text-foreground mt-2">{{ w.meaning }}</p>
            <p v-if="w.example" class="text-sm text-muted-foreground italic mt-1">"{{ w.example }}"</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import vocabularyService from '@/services/vocabularyService'

const topic = ref('')
const level = ref('B1')
const count = ref(10)
const generating = ref(false)
const error = ref('')
const generatedWords = ref([])

async function generate() {
  error.value = ''
  generating.value = true
  generatedWords.value = []
  try {
    const result = await vocabularyService.generateByAi({ topic: topic.value, level: level.value, count: count.value })
    generatedWords.value = result.words || []
  } catch (e) {
    error.value = e.response?.data?.message || 'Sinh từ thất bại'
  } finally {
    generating.value = false
  }
}
</script>
