<template>
  <div class="coin-display flex items-center bg-yellow-300 border-2 border-black rounded-full px-3 py-1 font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-yellow-600 mr-1" viewBox="0 0 20 20" fill="currentColor">
      <circle cx="10" cy="10" r="8" fill="#FCD34D" stroke="black" stroke-width="2"/>
      <path d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" fill="#B45309" />
    </svg>
    <span class="text-sm">{{ displayAmount }}</span>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  amount: {
    type: Number,
    required: true,
    default: 0
  }
})

const displayAmount = ref(props.amount)
let animTimer = null

// Animate counting up when amount changes
watch(() => props.amount, (newVal, oldVal) => {
  if (animTimer) {
    clearInterval(animTimer)
    animTimer = null
  }
  if (newVal > oldVal) {
    const diff = newVal - oldVal
    const step = Math.max(1, Math.floor(diff / 20))
    let current = oldVal
    
    animTimer = setInterval(() => {
      current += step
      if (current >= newVal) {
        displayAmount.value = newVal
        clearInterval(animTimer)
        animTimer = null
      } else {
        displayAmount.value = current
      }
    }, 50)
  } else {
    displayAmount.value = newVal
  }
})

onUnmounted(() => {
  if (animTimer) {
    clearInterval(animTimer)
    animTimer = null
  }
})
</script>
