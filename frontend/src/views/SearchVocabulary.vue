<template>
  <div class="bg-background min-h-screen py-16">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
      <UserPageHeader
        eyebrow="Tra cứu nhanh"
        title="Tra từ"
        subtitle="Tra cứu từ vựng tiếng Anh, phát âm, nghĩa, ví dụ và nhóm từ liên quan."
        root-class="mb-12"
      >
        <template #accent>AI lookup</template>
      </UserPageHeader>

      <!-- Search -->
      <div class="max-w-2xl mx-auto mb-12">
        <div class="flex gap-2">
          <input v-model="query" @keyup.enter="search" placeholder="Nhập từ cần tra..."
            class="flex-1 bg-input border-2 border-border rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
          />
          <AppButton @click="search" :disabled="!query.trim()" aria-label="Tra từ" variant="primary" size="lg">
            <Search class="w-5 h-5" aria-hidden="true" />
          </AppButton>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-16">
        <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
      </div>

      <!-- Error -->
      <div v-if="error" class="max-w-xl mx-auto text-center py-12">
        <div class="inline-flex items-center justify-center w-16 h-16 border-2 border-foreground bg-accent/10 mb-6 rounded-blob">
          <span class="text-3xl font-black text-accent-ink">!</span>
        </div>
        <p class="font-bold text-lg text-accent-ink">{{ error }}</p>
      </div>

      <!-- Not found (searched but empty) -->
      <div v-if="searched && results.length === 0 && !error" class="max-w-xl mx-auto text-center py-12">
        <div class="inline-flex items-center justify-center w-16 h-16 border-2 border-foreground bg-secondary/10 mb-6 rounded-blob">
          <span class="text-3xl font-black text-secondary-ink">?</span>
        </div>
        <p class="font-bold text-lg text-foreground">Không tìm thấy từ "{{ query }}"</p>
        <p class="text-sm text-muted-foreground mt-1">Kiểm tra chính tả hoặc thử một từ khác.</p>
      </div>

      <!-- Results -->
      <div v-if="results.length > 0" class="space-y-4">
        <div v-for="(word, i) in results" :key="i"
          class="bg-card border-2 border-foreground rounded-md p-6 shadow-pop hover:shadow-pop-lg transition-all duration-300"
        >
          <div class="flex items-start justify-between mb-3">
            <div>
              <h3 class="font-black text-2xl uppercase tracking-tight">{{ word.word }}</h3>
              <p class="font-bold text-sm text-muted-foreground mt-1">{{ word.phonetic || '/' + word.word + '/' }}</p>
            </div>
            <button v-if="word.audioUrl" @click="playAudio(word.audioUrl)" :aria-label="`Phát âm ${word.word}`"
              class="w-10 h-10 bg-accent-strong border-2 border-foreground rounded-full flex items-center justify-center text-white hover:bg-accent-strong/90 transition-all shadow-pop-sm"
            >
              <Volume2 class="w-5 h-5" aria-hidden="true" />
            </button>
          </div>

          <div class="border-t-2 border-foreground/10 pt-3 space-y-3">
            <div v-for="(meaning, j) in word.meanings" :key="j">
              <span class="font-black text-xs uppercase tracking-wider text-accent-ink bg-accent/10 px-2 py-0.5 rounded border border-accent/20">{{ meaning.partOfSpeech }}</span>

              <ol class="list-decimal list-inside mt-2 space-y-2">
                <li v-for="(def, k) in meaning.definitions" :key="k" class="text-foreground">
                  <span class="font-medium" v-html="sanitizeText(def.definition)"></span>
                  <p v-if="def.example" class="text-sm text-muted-foreground italic mt-0.5">"<span v-html="sanitizeText(def.example)"></span>"</p>
                </li>
              </ol>

              <div v-if="meaning.synonyms.length" class="mt-1.5 flex flex-wrap gap-1.5">
                <span class="text-[10px] font-bold uppercase tracking-wider text-foreground">Syn:</span>
                <span v-for="(s, idx) in meaning.synonyms" :key="'syn'+idx"
                  class="text-xs font-medium bg-secondary/15 text-secondary-ink border border-foreground/15 px-2 py-0.5 rounded-full">
                  {{ s }}
                </span>
              </div>
              <div v-if="meaning.antonyms.length" class="mt-1 flex flex-wrap gap-1.5">
                <span class="text-[10px] font-bold uppercase tracking-wider text-secondary-ink">Ant:</span>
                <span v-for="(a, idx) in meaning.antonyms" :key="'ant'+idx"
                  class="text-xs font-medium bg-secondary/10 text-secondary-ink border border-secondary/20 px-2 py-0.5 rounded-full">
                  {{ a }}
                </span>
              </div>
            </div>
          </div>

          <div v-if="word.origin" class="mt-3 pt-3 border-t-2 border-foreground/10">
            <p class="text-xs text-muted-foreground"><span class="font-bold uppercase tracking-wider">Nguồn gốc:</span> {{ word.origin }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import vocabularyService from '@/services/vocabularyService'
import { Search, Volume2 } from 'lucide-vue-next'
import { sanitizeText } from '@/utils/markdown'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'

const query = ref('')
const results = ref([])
const loading = ref(false)
const error = ref('')
const searched = ref(false)

async function search() {
  if (!query.value.trim()) return
  loading.value = true
  error.value = ''
  results.value = []
  searched.value = false
  try {
    results.value = await vocabularyService.search(query.value.trim())
    searched.value = true
  } catch (e) {
    if (e && e.message === 'TIMEOUT') {
      error.value = 'Tra cứu quá lâu — từ điển đang chậm phản hồi. Vui lòng thử lại sau ít phút.'
    } else if (e && e.message === 'NETWORK_ERROR') {
      error.value = 'Lỗi mạng — không kết nối được từ điển. Kiểm tra internet và thử lại.'
    } else {
      error.value = e.response?.data?.message || 'Không tìm thấy từ'
    }
  } finally {
    loading.value = false
  }
}

function playAudio(url) {
  new Audio(url).play()
}
</script>
