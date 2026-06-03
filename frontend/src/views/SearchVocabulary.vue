<template>
  <div class="px-4 py-12 max-w-7xl mx-auto font-sans">
    <div class="mb-12 text-center">
      <h1 class="font-black text-5xl uppercase tracking-tight mb-8">
        Tra Cứu <span class="text-primary-red">Từ Vựng</span>
      </h1>

      <div class="flex gap-2 max-w-2xl mx-auto">
        <input
          id="search-keyword"
          type="text"
          class="w-full px-4 py-3 border-2 border-black bg-white font-sans text-base focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-red flex-1"
          placeholder="Nhập từ vựng, nghĩa, hoặc ví dụ..."
          v-model="keyword"
          @keyup.enter="performSearch"
        />
        <BauhausButton variant="primary" @click="performSearch" :disabled="loading">
          Tìm kiếm
        </BauhausButton>
      </div>
      <p class="font-sans text-base mt-4" v-if="hasSearched && results.length > 0">
        Tìm thấy <strong>{{ results.length }}</strong> kết quả cho "{{ lastKeyword }}"
      </p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-12">
      <Loader2 class="animate-spin w-8 h-8" />
    </div>

    <!-- Results Grid -->
    <div v-else-if="results.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      <BauhausCard
        v-for="(vocab, index) in results"
        :key="vocab.id"
        :decorationColor="['red', 'blue', 'yellow'][index % 3]"
      >
        <div class="flex justify-between items-start mb-4">
          <div>
            <h3 class="font-black text-2xl uppercase mb-1">{{ vocab.word }}</h3>
            <span class="bg-gray-100 border-2 border-black px-2 py-0.5 text-xs font-bold uppercase inline-block">{{ vocab.wordType }}</span>
          </div>
          <BauhausButton variant="outline" size="sm" @click="speakWord(vocab.word)" title="Phát âm">
            🔊
          </BauhausButton>
        </div>

        <p class="text-sm text-gray-600 mb-2">{{ vocab.pronunciation }}</p>
        <p class="text-base font-medium mb-3">{{ vocab.meaning }}</p>

        <div v-if="vocab.exampleSentence" class="border-l-4 border-black pl-4 bg-gray-50 p-3 mb-4">
          <span class="font-bold text-xs uppercase block mb-1">Ví dụ:</span>
          <p class="mb-0 italic">"{{ vocab.exampleSentence }}"</p>
        </div>

        <div class="mt-auto pt-3 border-t-2 border-black">
          <BauhausButton variant="ghost" size="sm" @click="showDetails(vocab)">
            Xem chi tiết
          </BauhausButton>
        </div>
      </BauhausCard>
    </div>

    <!-- Initial State (Before Search) -->
    <div v-else-if="!hasSearched && !loading" class="border-4 border-black border-dashed bg-white p-12 text-center">
      <p class="font-black text-2xl uppercase mb-2">Nhập từ vựng để tra cứu</p>
      <p class="font-sans text-base">Gõ từ khóa và nhấn Enter hoặc Tìm kiếm</p>
    </div>

    <!-- Error State -->
    <div v-else-if="searchError && !loading" class="bg-primary-red text-white p-8 border-4 border-black shadow-hard-lg text-center">
      <p class="font-black text-2xl uppercase mb-2">⚠️ Lỗi kết nối</p>
      <p class="font-sans text-base">{{ searchError }}</p>
      <BauhausButton variant="yellow" size="sm" class="mt-4" @click="performSearch">Thử lại</BauhausButton>
    </div>

    <!-- Empty State (After Search) -->
    <div v-else-if="hasSearched && !loading" class="border-4 border-black border-dashed bg-white p-12 text-center">
      <p class="font-black text-2xl uppercase mb-2">Không tìm thấy kết quả</p>
      <p class="font-sans text-base">Thử một từ khóa khác xem sao!</p>
    </div>

    <!-- Details Modal -->
    <div
      v-if="selectedVocab"
      class="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
      @click.self="closeDetails"
      @keydown.escape="closeDetails"
      tabindex="-1"
      ref="modalRef"
    >
      <div class="bg-white border-4 border-black shadow-hard-lg p-8 relative max-w-2xl w-full max-h-[85vh] flex flex-col modal-dialog-custom">
        <button
          class="absolute top-4 right-4 text-2xl font-black leading-none bg-transparent border-none cursor-pointer p-0 select-none"
          @click="closeDetails"
          title="Đóng"
        >
          &times;
        </button>

        <div class="mb-4 pb-4 border-b-2 border-black">
          <div class="flex items-start justify-between pe-4 gap-4">
            <div>
              <h2 class="font-black text-4xl uppercase mb-2">{{ selectedVocab.word }}</h2>
              <div class="flex gap-2 items-center flex-wrap">
                <span
                  v-for="type in uniquePartsOfSpeech"
                  :key="type"
                  class="bg-gray-100 border-2 border-black text-sm font-bold uppercase px-2 py-1"
                >{{ type }}</span>
              </div>
            </div>

            <BauhausButton variant="outline" size="sm" shape="pill" @click="speakWord(selectedVocab.word)" title="Phát âm" class="flex-shrink-0">
              🔊
            </BauhausButton>
          </div>

          <!-- Phonetics & Audios -->
          <div v-if="filteredPhonetics.length > 0" class="mt-3 flex flex-wrap gap-2 items-center">
            <div
              v-for="(phonetic, pIdx) in filteredPhonetics"
              :key="pIdx"
              class="flex items-center gap-2 bg-primary-yellow/20 px-3 py-1 border-2 border-black"
            >
              <span class="font-bold">{{ phonetic.text }}</span>
              <button
                v-if="phonetic.audio"
                class="bg-transparent border-none cursor-pointer p-0 leading-none text-base hover:opacity-70"
                @click="playAudio(phonetic.audio)"
                title="Nghe phát âm"
              >
                🔊
              </button>
            </div>
          </div>
        </div>

        <!-- Meanings & Definitions -->
        <div class="flex-1 overflow-y-auto pr-2 modal-body-content">
          <div v-for="(meaning, mIdx) in selectedVocab.meanings" :key="mIdx" class="mb-6">
            <h4 class="font-bold text-base uppercase mb-3 pb-1 border-b-2 border-black inline-block">
              {{ meaning.partOfSpeech }}
            </h4>

            <ol class="ps-3 m-0">
              <li v-for="(def, dIdx) in meaning.definitions" :key="dIdx" class="mb-4">
                <div class="font-bold mb-1">{{ def.definition }}</div>

                <div v-if="def.example" class="mt-2 border-l-4 border-primary-yellow bg-gray-50 p-3">
                  <span class="font-bold text-xs uppercase block mb-1">Ví dụ thực tế:</span>
                  <p class="mb-0 italic">"{{ def.example }}"</p>
                </div>

                <div v-if="def.synonyms && def.synonyms.length > 0" class="mt-2 text-sm">
                  <span class="font-bold">Đồng nghĩa:</span>
                  <span
                    v-for="(syn, sIdx) in def.synonyms.slice(0, 5)"
                    :key="sIdx"
                    class="bg-gray-100 border-2 border-black px-2 py-0.5 text-xs font-bold uppercase inline-block ms-1"
                  >{{ syn }}</span>
                </div>
              </li>
            </ol>
          </div>
        </div>

        <div class="mt-4 pt-4 border-t-2 border-black flex justify-end">
          <BauhausButton variant="outline" @click="closeDetails">Đóng</BauhausButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import BauhausButton from '@/components/bauhaus/BauhausButton.vue'
import BauhausCard from '@/components/bauhaus/BauhausCard.vue'
import vocabularyService from '@/services/vocabularyService'

const keyword = ref('')
const lastKeyword = ref('')
const results = ref([])
const loading = ref(false)
const hasSearched = ref(false)
const selectedVocab = ref(null)
const modalRef = ref(null)
const searchError = ref(null)

watch(selectedVocab, async (val) => {
  if (val) {
    await nextTick()
    modalRef.value?.focus()
  }
})

async function performSearch() {
  if (!keyword.value.trim() || keyword.value.length < 2) return

  loading.value = true
  lastKeyword.value = keyword.value
  selectedVocab.value = null
  hasSearched.value = true
  searchError.value = null

  try {
    results.value = await vocabularyService.search(keyword.value)
  } catch (e) {
    console.error('Search failed:', e)
    results.value = []
    searchError.value = 'Không thể kết nối đến máy chủ từ điển. Vui lòng thử lại sau.'
  } finally {
    loading.value = false
  }
}

function speakWord(word) {
  if (!word || !window.speechSynthesis) return
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(word)
  utterance.lang = 'en-US'
  utterance.rate = 0.9
  window.speechSynthesis.speak(utterance)
}

function showDetails(vocab) {
  selectedVocab.value = vocab
}

function closeDetails() {
  selectedVocab.value = null
}

function playAudio(audioUrl) {
  if (!audioUrl) return
  const audio = new Audio(audioUrl)
  audio.play().catch(e => console.error('Audio playback failed:', e))
}

const uniquePartsOfSpeech = computed(() => {
  if (!selectedVocab.value || !selectedVocab.value.meanings) return []
  return [...new Set(selectedVocab.value.meanings.map(m => m.partOfSpeech))]
})

const filteredPhonetics = computed(() => {
  if (!selectedVocab.value || !selectedVocab.value.phonetics) return []
  const seen = new Set()
  return selectedVocab.value.phonetics.filter(p => {
    if (!p.text) return false
    if (seen.has(p.text)) return false
    seen.add(p.text)
    return true
  })
})
</script>

<style scoped>
.modal-dialog-custom {
  animation: fadeInScale 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-body-content::-webkit-scrollbar {
  width: 6px;
}

.modal-body-content::-webkit-scrollbar-track {
  background: #e0e0e0;
  border-radius: 4px;
}

.modal-body-content::-webkit-scrollbar-thumb {
  background: #121212;
  border-radius: 4px;
}

.modal-body-content::-webkit-scrollbar-thumb:hover {
  background: #444;
}
</style>
