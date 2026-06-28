<template>
  <div class="max-w-7xl mx-auto px-4 py-12">
    <div v-if="loading" class="text-center py-24">
      <div class="animate-spin inline-block w-10 h-10 border-4 border-black border-t-primary-red"></div>
    </div>

    <div v-else-if="error" class="text-center py-20 bg-white border-4 border-black shadow-hard-lg max-w-lg mx-auto">
      <div class="w-16 h-16 bg-primary-red border-4 border-black flex items-center justify-center mx-auto mb-6 rotate-12">
        <span class="text-4xl font-black text-white">!</span>
      </div>
      <h2 class="font-black text-3xl uppercase mb-4">Không thể tải deck</h2>
      <p class="font-bold text-gray-500 uppercase text-sm tracking-wider mb-8">{{ error }}</p>
      <button @click="loadDeck" class="px-8 py-3 bg-primary-red text-white font-black uppercase text-sm tracking-wider border-4 border-black shadow-hard-sm hover:-translate-y-1 hover:shadow-hard-md active:translate-x-1 active:translate-y-1 active:shadow-none transition-all duration-200">
        Thử lại
      </button>
    </div>
    
    <div v-else-if="deck">
      <button @click="$router.push('/decks')" class="mb-8 font-bold uppercase text-sm tracking-wider text-gray-500 hover:text-foreground flex items-center gap-2">
        <span class="text-lg">&larr;</span> Back to Decks
      </button>
      
      <div class="bg-primary-blue border-4 border-black shadow-hard-lg p-8 mb-12 relative overflow-hidden">
        <div class="absolute -right-12 -top-12 w-48 h-48 bg-primary-yellow border-4 border-black rounded-full opacity-40"></div>
        <div class="absolute -bottom-8 -left-8 w-24 h-24 bg-primary-red border-4 border-black rotate-45 opacity-30"></div>
        
        <div class="relative z-10">
          <div class="flex flex-wrap gap-3 mb-4">
            <span v-if="deck.source" class="px-3 py-1 bg-white text-foreground font-bold text-xs uppercase tracking-wider border-2 border-black">
              {{ deck.source }}
            </span>
            <span v-if="deck.cefrLevel" class="px-3 py-1 bg-primary-yellow text-foreground font-bold text-xs uppercase tracking-wider border-2 border-black">
              {{ deck.cefrLevel }}
            </span>
          </div>
          
          <h1 class="font-black text-5xl md:text-6xl uppercase text-white tracking-tighter mb-4">{{ deck.name }}</h1>
          <p class="font-bold text-lg text-white/80 max-w-2xl">{{ deck.description }}</p>
        </div>
      </div>
      
      <h2 class="font-black text-3xl uppercase tracking-tight mb-8">Chọn Chế Độ Chơi</h2>
      
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
        <div 
          class="bg-primary-yellow border-4 border-black shadow-hard-lg p-6 hover:-translate-y-2 hover:shadow-hard-xl transition-all duration-300 cursor-pointer flex flex-col justify-between relative group"
          @click="startGame('flashcard')">
          <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-red border-2 border-black rounded-full"></div>
          <div>
            <div class="w-12 h-12 bg-foreground border-4 border-black flex items-center justify-center mb-4 -rotate-6">
              <div class="w-4 h-6 bg-white border-2 border-black"></div>
            </div>
            <h3 class="font-black text-2xl uppercase mb-2">Flashcards</h3>
            <p class="font-bold text-sm text-gray-700">Học từ mới với Spaced Repetition (SM-2).</p>
          </div>
        </div>
        
        <div 
          class="bg-primary-red border-4 border-black shadow-hard-lg p-6 hover:-translate-y-2 hover:shadow-hard-xl transition-all duration-300 cursor-pointer flex flex-col justify-between relative group"
          @click="startGame('quiz')">
          <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-yellow border-2 border-black rotate-12"></div>
          <div>
            <div class="w-12 h-12 bg-white border-4 border-black flex items-center justify-center mb-4 rotate-6">
              <span class="font-black text-lg">?</span>
            </div>
            <h3 class="font-black text-2xl uppercase mb-2 text-white">Trắc Nghiệm</h3>
            <p class="font-bold text-sm text-white/80">Chọn đáp án đúng trong 4 lựa chọn.</p>
          </div>
        </div>
        
        <div 
          class="bg-foreground border-4 border-black shadow-hard-lg p-6 hover:-translate-y-2 hover:shadow-hard-xl transition-all duration-300 cursor-pointer flex flex-col justify-between relative group"
          @click="startGame('memory')">
          <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-yellow border-2 border-black rounded-full"></div>
          <div>
            <div class="w-12 h-12 bg-primary-blue border-4 border-black flex items-center justify-center mb-4 rotate-12">
              <div class="w-6 h-6 border-2 border-white rotate-45">
                <div class="w-2 h-2 bg-white mt-1 ml-1"></div>
              </div>
            </div>
            <h3 class="font-black text-2xl uppercase mb-2 text-white">Memory Match</h3>
            <p class="font-bold text-sm text-white/60">Ghép từ với định nghĩa trong trò chơi kinh điển.</p>
          </div>
        </div>

        <div 
          class="bg-primary-blue border-4 border-black shadow-hard-lg p-6 hover:-translate-y-2 hover:shadow-hard-xl transition-all duration-300 cursor-pointer flex flex-col justify-between relative group"
          @click="startGame('typing')">
          <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-yellow border-2 border-black rotate-12"></div>
          <div>
            <div class="w-12 h-12 bg-white border-4 border-black flex items-center justify-center mb-4">
              <span class="font-black text-sm uppercase">Aa</span>
            </div>
            <h3 class="font-black text-2xl uppercase mb-2 text-white">Gõ Chữ</h3>
            <p class="font-bold text-sm text-white/80">Gõ từ đúng dựa vào nghĩa tiếng Việt.</p>
          </div>
        </div>

        <div 
          class="bg-primary-yellow border-4 border-black shadow-hard-lg p-6 hover:-translate-y-2 hover:shadow-hard-xl transition-all duration-300 cursor-pointer flex flex-col justify-between relative group"
          @click="startGame('listening')">
          <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-red border-2 border-black rounded-full"></div>
          <div>
            <div class="w-12 h-12 bg-foreground border-4 border-black flex items-center justify-center mb-4 -rotate-6">
              <div class="w-0 h-0 border-l-[10px] border-l-transparent border-r-[10px] border-r-transparent border-b-[16px] border-b-white"></div>
            </div>
            <h3 class="font-black text-2xl uppercase mb-2">Nghe</h3>
            <p class="font-bold text-sm text-gray-700">Nghe từ và chọn đáp án đúng.</p>
          </div>
        </div>

        <div 
          class="bg-primary-red border-4 border-black shadow-hard-lg p-6 hover:-translate-y-2 hover:shadow-hard-xl transition-all duration-300 cursor-pointer flex flex-col justify-between relative group"
          @click="startGame('mixed')">
          <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-yellow border-2 border-black rotate-12"></div>
          <div>
            <div class="w-12 h-12 bg-white border-4 border-black flex items-center justify-center mb-4 rotate-45">
              <div class="-rotate-45">
                <span class="font-black text-lg">+</span>
              </div>
            </div>
            <h3 class="font-black text-2xl uppercase mb-2 text-white">Kết Hợp</h3>
            <p class="font-bold text-sm text-white/80">Thử thách tối thượng kết hợp tất cả chế độ.</p>
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
