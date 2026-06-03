<template>
  <div class="deck-detail max-w-5xl mx-auto px-4 py-8">
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin inline-block w-12 h-12 border-4 border-black border-t-transparent rounded-full"></div>
    </div>

    <div v-else-if="error" class="text-center py-20 bg-red-50 border-4 border-red-500 rounded-xl">
      <div class="text-6xl mb-6">😵</div>
      <h2 class="text-3xl font-black uppercase mb-4">Không thể tải deck</h2>
      <p class="font-bold text-gray-600 mb-8">{{ error }}</p>
      <button @click="loadDeck" class="px-8 py-3 bg-red-400 text-black font-black uppercase border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-red-500 hover:translate-y-1 hover:shadow-none transition-all">
        Thử lại
      </button>
    </div>
    
    <div v-else-if="deck">
      <button @click="$router.push('/decks')" class="mb-6 font-bold text-gray-500 hover:text-black uppercase flex items-center">
        &larr; Back to Decks
      </button>
      
      <div class="bg-blue-300 border-4 border-black rounded-2xl p-8 mb-8 shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] relative overflow-hidden">
        <div class="absolute -right-20 -top-20 w-64 h-64 bg-yellow-300 rounded-full border-4 border-black opacity-50 z-0"></div>
        
        <div class="relative z-10">
          <div class="flex flex-wrap gap-2 mb-4">
             <span class="px-3 py-1 bg-white text-black font-bold text-sm uppercase border-2 border-black rounded-full">
              {{ deck.source }}
            </span>
            <span class="px-3 py-1 bg-pink-400 text-black font-bold text-sm uppercase border-2 border-black rounded-full" v-if="deck.cefrLevel">
              {{ deck.cefrLevel }}
            </span>
          </div>
          
          <h1 class="text-5xl font-black uppercase text-black mb-4">{{ deck.name }}</h1>
          <p class="text-xl font-bold text-gray-800 max-w-2xl">{{ deck.description }}</p>
        </div>
      </div>
      
      <h2 class="text-3xl font-black uppercase mb-6">Choose a Game</h2>
      
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
        <!-- Flashcard / Review Mode -->
        <div 
          class="game-card bg-orange-300 border-4 border-black rounded-xl p-6 shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] hover:translate-x-1 hover:translate-y-1 hover:shadow-none transition-all cursor-pointer flex flex-col justify-between"
          @click="startGame('flashcard')"
        >
          <div>
            <div class="text-4xl mb-2">🃏</div>
            <h3 class="text-2xl font-black uppercase mb-2">Flashcards</h3>
            <p class="font-bold text-gray-800">Learn new words or review with Spaced Repetition (SM-2).</p>
          </div>
        </div>
        
        <!-- Quiz Mode -->
        <div 
          class="game-card bg-green-300 border-4 border-black rounded-xl p-6 shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] hover:translate-x-1 hover:translate-y-1 hover:shadow-none transition-all cursor-pointer flex flex-col justify-between"
          @click="startGame('quiz')"
        >
          <div>
            <div class="text-4xl mb-2">📝</div>
            <h3 class="text-2xl font-black uppercase mb-2">Multiple Choice</h3>
            <p class="font-bold text-gray-800">Test your knowledge by choosing the correct meaning.</p>
          </div>
        </div>
        
        <!-- Memory Match -->
        <div 
          class="game-card bg-purple-300 border-4 border-black rounded-xl p-6 shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] hover:translate-x-1 hover:translate-y-1 hover:shadow-none transition-all cursor-pointer flex flex-col justify-between"
          @click="startGame('memory')"
        >
          <div>
            <div class="text-4xl mb-2">🧠</div>
            <h3 class="text-2xl font-black uppercase mb-2">Memory Match</h3>
            <p class="font-bold text-gray-800">Match the word to its definition in this classic game.</p>
          </div>
        </div>

        <!-- Typing -->
        <div 
          class="game-card bg-pink-300 border-4 border-black rounded-xl p-6 shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] hover:translate-x-1 hover:translate-y-1 hover:shadow-none transition-all cursor-pointer flex flex-col justify-between"
          @click="startGame('typing')"
        >
          <div>
            <div class="text-4xl mb-2">⌨️</div>
            <h3 class="text-2xl font-black uppercase mb-2">Typing Practice</h3>
            <p class="font-bold text-gray-800">Type the correct word based on its meaning.</p>
          </div>
        </div>

        <!-- Listening -->
        <div 
          class="game-card bg-yellow-300 border-4 border-black rounded-xl p-6 shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] hover:translate-x-1 hover:translate-y-1 hover:shadow-none transition-all cursor-pointer flex flex-col justify-between"
          @click="startGame('listening')"
        >
          <div>
            <div class="text-4xl mb-2">🎧</div>
            <h3 class="text-2xl font-black uppercase mb-2">Listening</h3>
            <p class="font-bold text-gray-800">Listen to the word and identify it correctly.</p>
          </div>
        </div>

        <!-- Mixed -->
        <div 
          class="game-card bg-teal-300 border-4 border-black rounded-xl p-6 shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] hover:translate-x-1 hover:translate-y-1 hover:shadow-none transition-all cursor-pointer flex flex-col justify-between"
          @click="startGame('mixed')"
        >
          <div>
            <div class="text-4xl mb-2">🌪️</div>
            <h3 class="text-2xl font-black uppercase mb-2">Mixed Mode</h3>
            <p class="font-bold text-gray-800">The ultimate challenge mixing all game types.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import deckService from '@/services/deckService'

const route = useRoute()
const router = useRouter()
const deckId = ref(route.params.id)
const deck = ref(null)
const loading = ref(true)
const error = ref(null)

const loadDeck = async () => {
  loading.value = true
  error.value = null
  try {
    const data = await deckService.getDeckById(deckId.value)
    deck.value = data
  } catch (err) {
    console.error("Error loading deck", err)
    error.value = 'Không tìm thấy deck hoặc có lỗi kết nối.'
  } finally {
    loading.value = false
  }
}

onMounted(loadDeck)

const startGame = (type) => {
  router.push(`/decks/${deckId.value}/play/${type}`)
}
</script>
