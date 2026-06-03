<template>
  <div class="decks-view max-w-6xl mx-auto px-4 py-8">
    <div class="flex flex-col sm:flex-row justify-between items-center mb-8 gap-4">
      <h1 class="text-5xl font-black uppercase text-black">Vocabulary Decks</h1>
      
      <router-link to="/ai-vocab-generator" class="px-6 py-3 bg-indigo-400 text-black font-black uppercase border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:translate-x-1 hover:translate-y-1 hover:shadow-none transition-all">
        ✨ AI Generator
      </router-link>
    </div>

    <!-- Tabs -->
    <div class="flex space-x-4 mb-8">
      <button 
        @click="activeTab = 'public'"
        class="px-6 py-2 font-black uppercase text-lg border-b-4 transition-colors"
        :class="activeTab === 'public' ? 'border-black text-black' : 'border-transparent text-gray-400 hover:text-black'"
      >
        Public Decks
      </button>
      <button 
        @click="activeTab = 'my'"
        class="px-6 py-2 font-black uppercase text-lg border-b-4 transition-colors"
        :class="activeTab === 'my' ? 'border-black text-black' : 'border-transparent text-gray-400 hover:text-black'"
      >
        My Decks
      </button>
    </div>

    <!-- Deck Grid -->
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin inline-block w-12 h-12 border-4 border-black border-t-transparent rounded-full"></div>
    </div>
    
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="deck in displayedDecks" :key="deck.id" 
        class="deck-card bg-white border-4 border-black rounded-2xl p-6 flex flex-col justify-between shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] hover:translate-x-2 hover:translate-y-2 hover:shadow-[0px_0px_0px_0px_rgba(0,0,0,1)] transition-all cursor-pointer"
        @click="goToDeck(deck.id)"
      >
        <div>
          <div class="flex justify-between items-start mb-4">
            <span v-if="deck.source" class="px-3 py-1 bg-yellow-300 text-black font-bold text-xs uppercase border-2 border-black rounded-full">
              {{ deck.source }}
            </span>
            <span class="px-3 py-1 bg-pink-300 text-black font-bold text-xs uppercase border-2 border-black rounded-full" v-if="deck.cefrLevel">
              {{ deck.cefrLevel }}
            </span>
          </div>
          <h3 class="text-2xl font-black mb-2 line-clamp-2">{{ deck.name }}</h3>
          <p class="text-gray-600 font-medium mb-6 line-clamp-3">{{ deck.description }}</p>
        </div>
        
        <div class="flex justify-between items-center pt-4 border-t-4 border-black">
          <span class="font-black text-blue-600 uppercase tracking-wider">Start Learning &rarr;</span>
        </div>
      </div>
    </div>
    
    <div v-if="!loading && displayedDecks.length === 0" class="text-center py-12 bg-gray-50 border-4 border-black rounded-2xl border-dashed">
      <p class="text-2xl font-black text-gray-400 uppercase">No decks found</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import deckService from '@/services/deckService'
import { useAuthStore } from '@/store/modules/auth'

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('public')
const publicDecks = ref([])
const myDecks = ref([])
const loading = ref(true)

const isLoggedIn = computed(() => authStore.isLoggedIn)

const displayedDecks = computed(() => {
  return activeTab.value === 'public' ? publicDecks.value : myDecks.value
})

onMounted(async () => {
  try {
    loading.value = true
    const publicResponse = await deckService.getPublicDecks()
    publicDecks.value = publicResponse
    
    if (isLoggedIn.value) {
      const myResponse = await deckService.getMyDecks()
      myDecks.value = myResponse
    }
  } catch (error) {
    console.error("Failed to load decks", error)
  } finally {
    loading.value = false
  }
})

const goToDeck = (id) => {
  router.push(`/decks/${id}`)
}
</script>
