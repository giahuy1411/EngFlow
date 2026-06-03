<template>
  <div class="ai-generator max-w-4xl mx-auto px-4 py-8">
    <div class="flex items-center mb-8">
      <button @click="$router.push('/decks')" class="font-bold text-gray-500 hover:text-black uppercase mr-4">
        &larr; Back
      </button>
      <h1 class="text-4xl font-black uppercase text-black">✨ AI Deck Generator</h1>
    </div>

    <div class="bg-indigo-100 border-4 border-black rounded-2xl p-8 shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] relative overflow-hidden mb-8">
      <div class="absolute -right-10 -bottom-10 text-9xl opacity-20">🤖</div>
      
      <div class="relative z-10 max-w-xl">
        <p class="text-xl font-bold mb-6">Create a custom vocabulary deck on any topic instantly using NVIDIA Nemotron AI.</p>
        
        <div class="space-y-4 mb-6">
          <div>
            <label class="block font-black uppercase mb-2">Topic</label>
            <input 
              v-model="topic" 
              type="text" 
              placeholder="e.g. Artificial Intelligence, Space Travel, Business Negotiations" 
              class="w-full px-4 py-3 bg-white border-4 border-black rounded-xl font-bold focus:outline-none focus:ring-4 focus:ring-indigo-300 transition-all"
              :disabled="loading"
            >
          </div>
          
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block font-black uppercase mb-2">CEFR Level</label>
              <select v-model="level" class="w-full px-4 py-3 bg-white border-4 border-black rounded-xl font-bold focus:outline-none appearance-none" :disabled="loading">
                <option value="A1">A1 Beginner</option>
                <option value="A2">A2 Elementary</option>
                <option value="B1">B1 Intermediate</option>
                <option value="B2">B2 Upper Intermediate</option>
                <option value="C1">C1 Advanced</option>
                <option value="C2">C2 Mastery</option>
              </select>
            </div>
            
            <div>
              <label class="block font-black uppercase mb-2">Word Count</label>
              <input v-model="count" type="number" min="5" max="20" class="w-full px-4 py-3 bg-white border-4 border-black rounded-xl font-bold focus:outline-none" :disabled="loading">
            </div>
          </div>
        </div>
        
        <button 
          @click="generateDeck" 
          :disabled="!topic || loading"
          class="w-full sm:w-auto px-8 py-4 bg-indigo-500 text-white font-black uppercase border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-indigo-600 hover:translate-y-1 hover:shadow-none transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
        >
          <span v-if="loading" class="animate-spin h-5 w-5 mr-3 border-4 border-white border-t-transparent rounded-full"></span>
          {{ loading ? 'Generating...' : 'Generate Deck' }}
        </button>
      </div>
    </div>
    
    <!-- Generated Result -->
    <div v-if="generatedWords.length > 0 && !loading" class="animate-fade-in-up">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-2xl font-black uppercase">Generated Words ({{ generatedWords.length }})</h2>
        <button @click="saveDeck" class="px-6 py-2 bg-green-400 text-black font-black uppercase border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-green-500 hover:translate-y-1 hover:shadow-none transition-all" :disabled="saving">
          {{ saving ? 'Saving...' : 'Save to My Decks' }}
        </button>
      </div>
      
      <div class="space-y-4">
        <div v-for="(word, index) in generatedWords" :key="index" class="bg-white border-4 border-black rounded-xl p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div class="flex justify-between items-start mb-2">
            <div>
              <h3 class="text-2xl font-black inline-block mr-2">{{ word.word }}</h3>
              <span class="text-gray-500 font-bold font-mono">{{ word.pronunciation }}</span>
            </div>
            <span class="px-2 py-1 bg-black text-white text-xs font-bold uppercase rounded-md">{{ word.wordType }}</span>
          </div>
          
          <div class="grid sm:grid-cols-2 gap-4 mt-4">
            <div class="bg-blue-50 border-2 border-black rounded-lg p-3">
              <p class="font-bold text-xs uppercase mb-1 text-gray-500">English</p>
              <p class="font-bold">{{ word.definitionEn }}</p>
            </div>
            <div class="bg-green-50 border-2 border-black rounded-lg p-3">
              <p class="font-bold text-xs uppercase mb-1 text-gray-500">Vietnamese</p>
              <p class="font-bold">{{ word.definitionVi }}</p>
            </div>
          </div>
          <div class="mt-4 bg-yellow-50 border-2 border-black rounded-lg p-3">
             <p class="font-bold text-xs uppercase mb-1 text-gray-500">Example</p>
             <p class="italic">"{{ word.exampleSentence }}"</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import aiService from '@/services/aiService'
import deckService from '@/services/deckService'
import vocabularyService from '@/services/vocabularyService'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const toast = useToast()

const topic = ref('')
const level = ref('B2')
const count = ref(10)

const loading = ref(false)
const saving = ref(false)
const generatedWords = ref([])

const generateDeck = async () => {
  if (!topic.value) return
  
  loading.value = true
  generatedWords.value = []
  
  try {
    const data = await aiService.generateVocab(topic.value, level.value, count.value)
    generatedWords.value = data
    toast.success('Successfully generated vocabulary!')
  } catch (error) {
    console.error("AI Generation failed", error)
    toast.error(error.response?.data?.message || 'Failed to generate vocabulary. Please try again.')
  } finally {
    loading.value = false
  }
}

const saveDeck = async () => {
  if (generatedWords.value.length === 0) return
  
  saving.value = true
  let deck = null
  try {
    // 1. Create deck
    deck = await deckService.createDeck({
      name: `${topic.value} (${level.value})`,
      description: `AI generated vocabulary deck about ${topic.value} at ${level.value} level.`,
      isPublic: false,
      cefrLevel: level.value,
      source: 'AI_GENERATED'
    })
    
    // 2. Save each generated word and add to deck
    const createdVocabIds = []
    for (const word of generatedWords.value) {
      const vocab = await vocabularyService.create({
        word: word.word,
        pronunciation: word.pronunciation,
        meaning: word.definitionVi,
        definitionEn: word.definitionEn,
        exampleSentence: word.exampleSentence,
        wordType: word.wordType,
        cefrLevel: level.value,
        source: 'AI_GENERATED'
      })
      createdVocabIds.push(vocab.id)
      await deckService.addWordToDeck(deck.id, vocab.id)
    }
    
    toast.success('Deck saved successfully!')
    router.push(`/decks/${deck.id}`)
  } catch (error) {
    console.error("Failed to save deck", error)
    if (deck) {
      await deckService.deleteDeck(deck.id).catch(() => {})
    }
    for (const vocabId of createdVocabIds) {
      await vocabularyService.delete(vocabId).catch(() => {})
    }
    toast.error('Failed to save the deck. Changes rolled back.')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.animate-fade-in-up {
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
