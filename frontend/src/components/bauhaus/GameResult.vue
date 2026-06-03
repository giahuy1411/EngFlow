<template>
  <div class="game-result-container bg-white border-4 border-black rounded-2xl p-8 max-w-md mx-auto shadow-[12px_12px_0px_0px_rgba(0,0,0,1)] text-center relative overflow-hidden">
    <!-- Confetti or background decoration -->
    <div class="absolute -top-10 -right-10 w-32 h-32 bg-yellow-300 rounded-full border-4 border-black z-0"></div>
    <div class="absolute -bottom-10 -left-10 w-24 h-24 bg-blue-500 rounded-full border-4 border-black z-0"></div>
    
    <div class="relative z-10">
      <h2 class="text-4xl font-black mb-2 uppercase">Complete!</h2>
      <p class="text-xl font-bold text-gray-600 mb-8">Great job on your practice</p>
      
      <div class="grid grid-cols-2 gap-4 mb-8">
        <!-- Score Card -->
        <div class="bg-blue-100 border-4 border-black rounded-xl p-4 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <p class="font-bold uppercase text-sm mb-1">Score</p>
          <p class="text-3xl font-black">{{ correct }}/{{ total }}</p>
        </div>
        
        <!-- Coins Card -->
        <div class="bg-yellow-100 border-4 border-black rounded-xl p-4 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] flex flex-col items-center justify-center">
          <p class="font-bold uppercase text-sm mb-1">Earned</p>
          <p class="text-3xl font-black text-yellow-600 flex items-center">
            +{{ coins }} 
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 ml-1" viewBox="0 0 20 20" fill="currentColor">
              <path d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" />
            </svg>
          </p>
        </div>
      </div>
      
      <div class="flex flex-col space-y-4">
        <button @click="$emit('continue')" class="w-full py-4 bg-green-400 text-black font-black text-xl uppercase border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-green-500 transition-colors hover:translate-x-1 hover:translate-y-1 hover:shadow-none">
          Continue Learning
        </button>
        <button @click="$emit('back')" class="w-full py-4 bg-white text-black font-black text-xl uppercase border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-gray-100 transition-colors hover:translate-x-1 hover:translate-y-1 hover:shadow-none">
          Back to Decks
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  correct: {
    type: Number,
    required: true
  },
  total: {
    type: Number,
    required: true
  },
  coins: {
    type: Number,
    default: 0
  }
})
defineEmits(['continue', 'back'])
</script>
