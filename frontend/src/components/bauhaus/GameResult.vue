<template>
  <div class="game-result-container bg-white border-4 border-black rounded-2xl p-8 max-w-md mx-auto shadow-[12px_12px_0px_0px_rgba(0,0,0,1)] text-center relative overflow-hidden">
    <!-- Confetti or background decoration -->
    <div class="absolute -top-10 -right-10 w-32 h-32 bg-yellow-300 rounded-full border-4 border-black z-0"></div>
    <div class="absolute -bottom-10 -left-10 w-24 h-24 bg-blue-500 rounded-full border-4 border-black z-0"></div>
    
    <div class="relative z-10">
      <h2 class="text-4xl font-black mb-2 uppercase">Complete!</h2>
      <p class="text-xl font-bold text-gray-600 mb-8">Great job on your practice</p>
      
      <div class="flex justify-center mb-8">
        <!-- Score Card -->
        <div class="bg-blue-100 border-4 border-black rounded-xl p-4 px-8 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] min-w-[150px]">
          <p class="font-bold uppercase text-sm mb-1">Score</p>
          <p class="text-3xl font-black">{{ correct }}/{{ total }}</p>
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
  }
})
defineEmits(['continue', 'back'])
</script>
