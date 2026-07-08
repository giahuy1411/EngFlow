<template>
  <button 
    @click.stop="playAudio" 
    class="audio-button p-2 bg-playful-sky text-black rounded-blob border-2 border-black transition-all duration-200 shadow-pop-sm active:translate-x-[2px] active:translate-y-[2px] active:shadow-none"
    :class="{'animate-pulse bg-playful-lavender': isPlaying}"
    title="Listen"
  >
    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" :class="{'hidden': isPlaying}" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072M17.95 6.05a8 8 0 010 11.9M6.5 14h-3a.5.5 0 01-.5-.5v-3a.5.5 0 01.5-.5h3l4-4v12l-4-4z" />
    </svg>
    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" :class="{'hidden': !isPlaying}" viewBox="0 0 20 20" fill="currentColor">
      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd" />
    </svg>
  </button>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  word: {
    type: String,
    required: true
  },
  audioUrl: {
    type: String,
    default: null
  }
})

const isPlaying = ref(false)

const playAudio = () => {
  if (isPlaying.value) return
  
  isPlaying.value = true
  
  if (props.audioUrl) {
    const audio = new Audio(props.audioUrl)
    audio.onended = () => { isPlaying.value = false }
    audio.onerror = () => { fallbackAudio() }
    audio.play().catch(() => fallbackAudio())
  } else {
    fallbackAudio()
  }
}

const fallbackAudio = () => {
  if ('speechSynthesis' in window) {
    const utterance = new SpeechSynthesisUtterance(props.word)
    utterance.lang = 'en-US'
    utterance.onend = () => { isPlaying.value = false }
    utterance.onerror = () => { isPlaying.value = false }
    window.speechSynthesis.speak(utterance)
  } else {
    isPlaying.value = false
  }
}
</script>
