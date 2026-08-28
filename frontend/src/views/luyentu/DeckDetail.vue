<template>
  <div class="bg-background min-h-screen py-16">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <router-link to="/decks"
        class="inline-flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent transition-colors mb-6"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
        Quay lại
      </router-link>

      <div v-if="loading" class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl mb-8" role="status">
        <div class="w-10 h-10 mx-auto border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
        <p class="mt-4 text-center font-bold text-sm uppercase tracking-wider text-muted-foreground">Đang tải bộ từ...</p>
      </div>

      <div v-else-if="error" class="bg-danger/10 border-2 border-danger rounded-md p-8 text-center shadow-pop-xl mb-8" role="alert">
        <p class="font-black text-danger">{{ error }}</p>
        <button class="mt-4 px-5 py-2 border-2 border-foreground rounded-full font-bold uppercase text-sm" @click="loadDeck">Thử lại</button>
      </div>

      <template v-else>
        <div class="bg-card border-2 border-foreground rounded-xl p-8 shadow-pop-xl mb-8">
          <UserPageHeader
            eyebrow="Bộ từ cá nhân"
            :title="deck.name"
            :subtitle="deck.description || 'Ôn tập, chơi game và nghe phát âm từ bộ này.'"
            :divided="false"
          >
            <template #accent>{{ deck.words?.length || 0 }} từ</template>
            <template #actions>
              <div class="flex gap-2">
                <span v-if="deck.source" class="px-3 py-1 bg-tertiary border-2 border-foreground rounded-full text-xs font-bold">{{ deck.source }}</span>
                <span v-if="deck.cefrLevel" class="px-3 py-1 bg-accent/10 border-2 border-foreground rounded-full text-xs font-bold">{{ deck.cefrLevel }}</span>
              </div>
            </template>
          </UserPageHeader>
        </div>

        <!-- Game Buttons -->
        <div class="flex flex-wrap gap-3 mb-10">
          <router-link :to="`/decks/${deck.id}/play/flashcard`"
            class="px-6 py-3 bg-accent text-white font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
          >Flashcard</router-link>
          <router-link :to="`/decks/${deck.id}/play/quiz`"
            class="px-6 py-3 bg-secondary text-white font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
          >Quiz</router-link>
          <router-link :to="`/decks/${deck.id}/play/listening`"
            class="px-6 py-3 bg-tertiary text-foreground font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
          >Nghe</router-link>
          <router-link :to="`/decks/${deck.id}/play/typing`"
            class="px-6 py-3 bg-quaternary text-white font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
          >Gõ từ</router-link>
          <router-link :to="`/decks/${deck.id}/play/memory`"
            class="px-6 py-3 bg-card font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
          >Ghép cặp</router-link>
          <router-link :to="`/decks/${deck.id}/play/mixed`"
            class="px-6 py-3 bg-foreground text-white font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
          >Tổng hợp</router-link>
        </div>

        <!-- Word List -->
        <div class="space-y-3">
          <h2 class="font-black text-xl uppercase tracking-tight mb-4">{{ deck.words?.length || 0 }} từ</h2>
          <div v-for="(w, i) in deck.words || []" :key="w.id || i"
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
          <div v-if="!deck.words?.length" class="bg-card border-2 border-dashed border-foreground rounded-md p-10 text-center text-muted-foreground font-bold">
            Bộ từ này chưa có từ vựng.
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import deckService from '@/services/deckService'
import { Volume2 } from 'lucide-vue-next'
import UserPageHeader from '@/components/common/UserPageHeader.vue'

const route = useRoute()
const deck = ref({})
const loading = ref(true)
const error = ref('')

onMounted(loadDeck)

async function loadDeck() {
  loading.value = true
  error.value = ''
  try {
    deck.value = await deckService.getDeckById(route.params.id)
  } catch (cause) {
    error.value = cause.response?.data?.message || 'Không tải được bộ từ.'
  } finally {
    loading.value = false
  }
}

function playAudio(url) {
  new Audio(url).play()
}
</script>
