<template>
  <div class="mixed-game max-w-4xl mx-auto px-4 py-8 text-center">
    <!-- Simplified Mixed game that redirects to one of the others randomly for now -->
    <div v-if="loading" class="py-20">
      <div class="animate-spin inline-block w-12 h-12 border-4 border-black border-t-transparent rounded-full"></div>
      <p class="mt-4 font-black text-xl uppercase">Choosing a challenge...</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const deckId = route.params.id
const loading = ref(true)

let timeoutId = null

onMounted(() => {
  if (!deckId) {
    router.replace('/decks')
    return
  }
  timeoutId = setTimeout(() => {
    const games = ['quiz', 'memory', 'typing', 'listening']
    const randomGame = games[Math.floor(Math.random() * games.length)]
    router.replace(`/decks/${deckId}/play/${randomGame}`)
  }, 1000)
})

onUnmounted(() => {
  if (timeoutId) clearTimeout(timeoutId)
})
</script>
