<template>
  <div class="game-result-container bg-white border-4 border-black rounded-2xl p-8 max-w-md mx-auto shadow-pop-xl text-center relative overflow-hidden">
    <!-- Confetti or background decoration -->
    <div class="absolute -top-10 -right-10 w-32 h-32 bg-playful-yellow rounded-blob border-4 border-black rotate-12 z-0"></div>
    <div class="absolute -bottom-10 -left-10 w-24 h-24 bg-playful-sky rounded-blob border-4 border-black -rotate-12 z-0"></div>
    
    <div class="relative z-10">
      <h2 class="text-4xl font-black mb-2 uppercase">Complete!</h2>
      <p class="text-xl font-bold text-gray-600 mb-8">Great job on your practice</p>
      
      <div class="flex justify-center mb-8">
        <!-- Score Card -->
        <div class="bg-playful-lavender border-4 border-black rounded-blob p-4 px-8 shadow-pop-md min-w-[150px]">
          <p class="font-bold uppercase text-sm mb-1">Score</p>
          <p class="text-3xl font-black">{{ correct }}/{{ total }}</p>
        </div>
      </div>
      
      <div class="flex flex-col space-y-4">
        <BauhausButton 
          variant="lime" 
          size="lg"
          class="w-full font-black text-xl py-4" 
          @click="$emit('continue')"
        >
          Continue Learning
        </BauhausButton>
        <BauhausButton 
          variant="outline" 
          size="lg"
          class="w-full font-black text-xl py-4" 
          @click="$emit('back')"
        >
          Back to Decks
        </BauhausButton>
      </div>
    </div>
  </div>
</template>

<script setup>
import BauhausButton from './BauhausButton.vue'

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

