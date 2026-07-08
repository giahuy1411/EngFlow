<template>
  <div class="bg-geo-bg min-h-screen py-16">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <router-link :to="'/decks/' + deck.id"
        class="inline-flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent transition-colors mb-6"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
        Quay lại
      </router-link>

      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl mb-8">
        <div class="flex items-start justify-between">
          <div>
            <h1 class="font-black text-3xl uppercase tracking-tight">{{ deck.name }}</h1>
            <p v-if="deck.description" class="font-medium text-muted-foreground mt-2">{{ deck.description }}</p>
          </div>
          <div class="flex gap-2">
            <span v-if="deck.source" class="px-3 py-1 bg-tertiary border-2 border-foreground rounded-full text-xs font-bold">{{ deck.source }}</span>
            <span v-if="deck.cefrLevel" class="px-3 py-1 bg-accent/10 border-2 border-foreground rounded-full text-xs font-bold">{{ deck.cefrLevel }}</span>
          </div>
        </div>
      </div>

      <!-- Game Buttons -->
      <div class="flex flex-wrap gap-3 mb-10">
        <router-link :to="`/decks/${deck.id}/flashcard`"
          class="px-6 py-3 bg-accent text-white font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
        >Flashcard</router-link>
        <router-link :to="`/decks/${deck.id}/quiz`"
          class="px-6 py-3 bg-secondary text-white font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
        >Quiz</router-link>
        <router-link :to="`/decks/${deck.id}/listening`"
          class="px-6 py-3 bg-tertiary text-foreground font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
        >Nghe</router-link>
        <router-link :to="`/decks/${deck.id}/typing`"
          class="px-6 py-3 bg-quaternary text-white font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
        >Gõ từ</router-link>
        <router-link :to="`/decks/${deck.id}/memory`"
          class="px-6 py-3 bg-card font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
        >Ghép cặp</router-link>
      </div>

      <!-- Word List -->
      <div class="space-y-3">
        <h2 class="font-black text-xl uppercase tracking-tight mb-4">{{ deck.words?.length || 0 }} từ</h2>
        <div v-for="(w, i) in deck.words || []" :key="i"
          class="bg-card border-2 border-foreground rounded-md p-4 shadow-pop-lg flex items-start justify-between transition-all"
        >
          <div>
            <h3 class="font-black text-lg uppercase">{{ w.word }}</h3>
            <p class="font-bold text-xs text-muted-foreground">{{ w.phonetic || '' }}</p>
            <p class="font-medium text-foreground mt-1">{{ w.meaning }}</p>
            <p v-if="w.example" class="text-sm text-muted-foreground italic mt-1">"{{ w.example }}"</p>
          </div>
          <button v-if="w.audioUrl" @click="playAudio(w.audioUrl)"
            class="w-9 h-9 bg-accent border-2 border-foreground rounded-full flex items-center justify-center text-white hover:bg-accent/90 transition-all shadow-pop-sm flex-shrink-0"
          ><Volume2 class="w-4 h-4" /></button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import deckService from '@/services/deckService'
import { Volume2 } from 'lucide-vue-next'

const route = useRoute()
const deck = ref({})

onMounted(async () => {
  try {
    deck.value = await deckService.getDeckById(route.params.id)
  } catch (e) { console.error(e) }
})

function playAudio(url) {
  new Audio(url).play()
}
</script>
