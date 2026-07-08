<template>
  <div class="bg-geo-bg min-h-screen py-16">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="flex items-center gap-4 mb-10">
        <router-link to="/decks"
          class="w-10 h-10 border-2 border-foreground rounded-full flex items-center justify-center bg-card hover:bg-tertiary/20 transition-all"
        >
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
        </router-link>
        <div>
          <h1 class="font-black text-3xl uppercase tracking-tight">Tạo Bộ Từ</h1>
          <p class="font-bold text-xs uppercase tracking-wider text-muted-foreground mt-1">Tạo bộ từ vựng mới</p>
        </div>
      </div>

      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl">
        <form @submit.prevent="handleCreate" class="space-y-6">
          <!-- Name -->
          <div>
            <label class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Tên bộ từ</label>
            <input v-model="form.name" placeholder="e.g. IELTS Vocabulary" required
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            />
          </div>

          <!-- Description -->
          <div>
            <label class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Mô tả</label>
            <textarea v-model="form.description" rows="3" placeholder="Mô tả ngắn về bộ từ..."
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            ></textarea>
          </div>

          <!-- Source & Level -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Nguồn</label>
              <input v-model="form.source" placeholder="e.g. Cambridge"
                class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
              />
            </div>
            <div>
              <label class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">CEFR Level</label>
              <select v-model="form.cefrLevel"
                class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none"
              >
                <option value="">Chọn level</option>
                <option v-for="lv in ['A1','A2','B1','B2','C1','C2']" :key="lv" :value="lv">{{ lv }}</option>
              </select>
            </div>
          </div>

          <!-- Words -->
          <div>
            <label class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Từ vựng (mỗi dòng một từ)</label>
            <textarea v-model="form.words" rows="8" placeholder="word1&#10;word2&#10;word3"
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            ></textarea>
          </div>

          <!-- Public toggle -->
          <label class="flex items-center gap-3 cursor-pointer">
            <input type="checkbox" v-model="form.isPublic" class="w-5 h-5 border-2 border-foreground rounded-sm accent-accent" />
            <span class="font-bold text-sm uppercase tracking-wider">Công khai</span>
          </label>

          <p v-if="error" class="font-bold text-xs uppercase tracking-wider text-secondary">{{ error }}</p>

          <button type="submit" :disabled="loading"
            class="w-full py-3.5 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5 transition-all duration-300 ease-bounce disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            <span v-if="loading" class="inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
            <span v-else>Tạo bộ từ</span>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import deckService from '@/services/deckService'

const router = useRouter()
const loading = ref(false)
const error = ref('')

const form = reactive({
  name: '',
  description: '',
  source: '',
  cefrLevel: '',
  words: '',
  isPublic: false,
})

async function handleCreate() {
  error.value = ''
  loading.value = true
  try {
    const words = form.words.split('\n').filter(w => w.trim())
    await deckService.createDeck({
      name: form.name,
      description: form.description,
      source: form.source,
      cefrLevel: form.cefrLevel,
      words,
      isPublic: form.isPublic,
    })
    router.push('/decks')
  } catch (e) {
    error.value = e.response?.data?.message || 'Tạo thất bại'
  } finally {
    loading.value = false
  }
}
</script>
