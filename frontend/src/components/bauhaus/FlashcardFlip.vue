<template>
  <div class="flashcard-container group perspective-1000 w-full h-full" @click="flip">
    <div 
      class="flashcard-inner w-full h-full transition-transform duration-500 transform-style-preserve-3d relative"
      :class="{ 'rotate-y-180': isFlipped }"
    >
      <div 
        class="flashcard-front absolute w-full h-full backface-hidden border-4 border-black rounded-xl p-4 sm:p-6 flex flex-col items-center justify-center shadow-[6px_6px_0px_0px_rgba(0,0,0,1)]"
        :class="frontBgClass"
      >
        <h2 class="text-2xl sm:text-4xl font-black mb-2 text-center text-black">{{ word }}</h2>
        <p class="text-sm sm:text-lg font-bold mb-2 font-mono text-black opacity-80">{{ ipa }}</p>
        <button v-if="audioUrl" @click.stop="playAudio" class="p-2 sm:p-3 bg-white text-black rounded-full border-2 border-black hover:bg-gray-100 transition-colors shadow-[3px_3px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 sm:h-6 sm:w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072M17.95 6.05a8 8 0 010 11.9M6.5 14h-3a.5.5 0 01-.5-.5v-3a.5.5 0 01.5-.5h3l4-4v12l-4-4z" />
          </svg>
        </button>
        <p v-else class="italic text-xs mt-2 text-black opacity-60">No audio</p>
        <p class="absolute bottom-3 text-xs font-bold text-black opacity-60">Click to flip</p>
      </div>
      
      <div class="flashcard-back absolute w-full h-full backface-hidden bg-yellow-300 border-4 border-black rounded-xl p-4 sm:p-6 flex flex-col items-center justify-center rotate-y-180 shadow-[6px_6px_0px_0px_rgba(0,0,0,1)]">
        <span class="inline-block px-3 py-1 bg-black text-white font-bold text-xs rounded-full mb-3">{{ wordType }}</span>
        <h3 class="text-xl sm:text-3xl font-black mb-4 text-center text-black">{{ meaning }}</h3>
        
        <div class="w-full bg-white border-2 border-black rounded-lg p-3 sm:p-4 text-left">
          <p class="font-bold mb-1 text-black text-xs sm:text-sm">Example:</p>
          <p class="text-gray-800 italic text-sm sm:text-base">{{ example }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  word: String,
  ipa: String,
  meaning: String,
  wordType: String,
  example: String,
  audioUrl: String,
  flipped: {
    type: Boolean,
    default: false
  },
  colorIndex: {
    type: Number,
    default: 0
  }
})

const colors = [
  'bg-white',
  'bg-pink-300',
  'bg-green-300',
  'bg-blue-300',
  'bg-orange-300',
  'bg-purple-300',
  'bg-red-300'
]
const frontBgClass = computed(() => colors[props.colorIndex % colors.length])

const emit = defineEmits(['flip'])
const isFlipped = ref(props.flipped)

watch(() => props.flipped, (newVal) => {
  isFlipped.value = newVal
})

const flip = () => {
  isFlipped.value = !isFlipped.value
  emit('flip', isFlipped.value)
}

const playAudio = () => {
  if (props.audioUrl) {
    const audio = new Audio(props.audioUrl)
    audio.play().catch(e => {
      fallbackAudio()
    })
  } else {
    fallbackAudio()
  }
}

const fallbackAudio = () => {
  if ('speechSynthesis' in window) {
    const utterance = new SpeechSynthesisUtterance(props.word)
    utterance.lang = 'en-US'
    window.speechSynthesis.speak(utterance)
  }
}
</script>

<style scoped>
.perspective-1000 {
  perspective: 1000px;
}
.transform-style-preserve-3d {
  transform-style: preserve-3d;
}
.backface-hidden {
  backface-visibility: hidden;
}
.rotate-y-180 {
  transform: rotateY(180deg);
}
</style>
