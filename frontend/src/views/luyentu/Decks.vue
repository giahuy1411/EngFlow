<template>
    <div class="max-w-7xl w-full mx-auto px-4 py-12">
    <div class="flex flex-col sm:flex-row justify-between items-center mb-12 gap-4">
      <div class="relative">
        <div class="absolute -top-3 -left-3 w-6 h-6 bg-primary-red border-2 border-black rotate-12"></div>
        <h1 class="font-black text-5xl uppercase tracking-tighter relative z-10">Bộ Từ Vựng</h1>
      </div>
      
      <div class="flex gap-3">
        <router-link to="/decks/create"
          class="px-6 py-3 bg-primary-red text-white font-black uppercase text-sm tracking-wider border-4 border-black shadow-hard-sm hover:-translate-y-1 hover:shadow-hard-md active:translate-x-1 active:translate-y-1 active:shadow-none transition-all duration-200">
          + Tạo bộ từ
        </router-link>
        <router-link to="/ai-vocab-generator"
          class="px-6 py-3 bg-primary-yellow text-foreground font-black uppercase text-sm tracking-wider border-4 border-black shadow-hard-sm hover:-translate-y-1 hover:shadow-hard-md active:translate-x-1 active:translate-y-1 active:shadow-none transition-all duration-200">
          AI Generator
        </router-link>
      </div>
    </div>

    <div class="flex gap-1 mb-10 border-2 border-black bg-white p-1 inline-flex">
      <button 
        @click="activeTab = 'public'"
        class="px-6 py-2.5 font-black uppercase text-sm tracking-wider border-2 border-black transition-all duration-200"
        :class="activeTab === 'public' ? 'bg-foreground text-white shadow-hard-sm' : 'border-transparent text-foreground/50 hover:text-foreground'">
        Public
      </button>
      <button 
        @click="activeTab = 'my'"
        class="px-6 py-2.5 font-black uppercase text-sm tracking-wider border-2 border-black transition-all duration-200"
        :class="activeTab === 'my' ? 'bg-foreground text-white shadow-hard-sm' : 'border-transparent text-foreground/50 hover:text-foreground'">
        My Decks
      </button>
    </div>

    <div v-if="loading" class="text-center py-24">
      <div class="animate-spin inline-block w-10 h-10 border-4 border-black border-t-primary-red"></div>
    </div>
    
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <template v-if="displayedDecks.length > 0">
        <div v-for="deck in displayedDecks" :key="deck.id" 
          class="bg-white border-4 border-black shadow-hard-lg p-6 flex flex-col justify-between cursor-pointer hover:-translate-y-2 transition-all duration-300 relative group"
          @click="goToDeck(deck.id)">
          <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-blue border-2 border-black rotate-12 z-10"></div>
          
          <div>
            <div class="flex gap-2 mb-4">
              <span v-if="deck.source"
                class="px-3 py-1 bg-primary-yellow text-foreground font-bold text-xs uppercase tracking-wider border-2 border-black">
                {{ deck.source }}
              </span>
              <span v-if="deck.cefrLevel"
                class="px-3 py-1 bg-white text-foreground font-bold text-xs uppercase tracking-wider border-2 border-black">
                {{ deck.cefrLevel }}
              </span>
            </div>
            <h3 class="font-black text-2xl uppercase mb-3 line-clamp-2">{{ deck.name }}</h3>
            <p v-if="deck.description" class="font-medium text-sm text-gray-600 mb-6 line-clamp-3">{{ deck.description }}</p>
          </div>
          
          <div class="flex justify-between items-center pt-4 border-t-4 border-black">
            <span class="font-black text-sm uppercase tracking-wider text-primary-red">Bắt đầu &rarr;</span>
          </div>
        </div>
      </template>
      <div v-else class="col-span-full bg-white border-4 border-black shadow-hard-lg p-6 flex flex-col items-center justify-center text-center relative min-h-[500px]">
        <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-blue border-2 border-black rotate-12 z-10"></div>
        <div class="absolute -bottom-3 -left-3 w-6 h-6 bg-primary-red border-2 border-black -rotate-12 z-10"></div>
        
        <p class="font-black text-3xl uppercase mb-3">{{ activeTab === 'public' ? 'Chưa có bộ từ công khai' : 'Chưa có bộ từ vựng' }}</p>
        <p class="font-medium text-gray-600 mb-8 max-w-md">{{ activeTab === 'public' ? 'Hiện tại chưa có bộ từ vựng công khai nào. Hãy quay lại sau!' : 'Bạn chưa có bộ từ vựng nào. Hãy tạo bộ từ vựng đầu tiên!' }}</p>
        
        <router-link v-if="activeTab === 'my'" to="/ai-vocab-generator"
          class="px-8 py-3 bg-primary-yellow text-foreground font-black uppercase text-sm tracking-wider border-4 border-black shadow-hard-sm hover:-translate-y-1 hover:shadow-hard-md active:translate-x-1 active:translate-y-1 active:shadow-none transition-all duration-200">
          + Tạo bộ từ
        </router-link>
      </div>
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
