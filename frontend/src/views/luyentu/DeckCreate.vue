<template>
  <div class="max-w-7xl mx-auto p-8 relative min-h-[calc(100vh-5rem)]">
    <!-- Geometric Background Details -->
    <div class="fixed top-20 right-0 w-64 h-64 bg-primary-yellow rounded-full border-4 border-black translate-x-1/2 -z-10 opacity-20"></div>
    <div class="fixed bottom-0 left-0 w-48 h-48 bg-primary-blue border-4 border-black -translate-x-1/2 translate-y-1/2 rotate-45 -z-10 opacity-20"></div>

    <div class="flex justify-between items-center mb-12 border-b-4 border-black pb-4">
      <h1 class="font-black text-4xl md:text-5xl uppercase tracking-tighter">Tạo Bộ Từ Vựng</h1>
      <BauhausButton variant="outline" size="sm" @click="$router.push('/decks')">Quay lại</BauhausButton>
    </div>

    <!-- Error/Success states -->
    <div v-if="error" class="bg-primary-red text-white p-4 font-bold uppercase tracking-wider mb-8 border-2 border-black shadow-hard-sm">
      {{ error }}
    </div>
    
    <div v-if="success" class="bg-green-500 text-white p-4 font-bold uppercase tracking-wider mb-8 border-2 border-black shadow-hard-sm flex justify-between items-center">
      <span>{{ success }}</span>
      <BauhausButton variant="outline" size="sm" @click="goToNewDeck">Chơi ngay</BauhausButton>
    </div>

    <form @submit.prevent="handleSave" class="space-y-12">
      <!-- Deck Info -->
      <section class="bg-white border-4 border-black shadow-hard-lg p-8 relative">
        <div class="absolute -top-4 -left-4 w-8 h-8 bg-primary-red rounded-none border-4 border-black rotate-12"></div>
        
        <h2 class="font-black text-2xl uppercase tracking-wide mb-6 bg-black text-white inline-block px-4 py-1">Thông tin Bộ từ</h2>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="col-span-1 md:col-span-2">
            <label for="title" class="block font-bold uppercase tracking-wider mb-2">Tên bộ từ *</label>
            <input 
              id="title" 
              v-model="deckForm.title" 
              type="text" 
              required
              placeholder="VD: Từ vựng Toeic 500+"
              class="w-full border-2 border-black p-3 bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-blue font-bold text-lg"
            />
          </div>
          
          <div class="col-span-1 md:col-span-2">
            <label for="description" class="block font-bold uppercase tracking-wider mb-2">Mô tả</label>
            <textarea 
              id="description" 
              v-model="deckForm.description" 
              rows="2"
              placeholder="Nhập mô tả ngắn gọn..."
              class="w-full border-2 border-black p-3 bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-blue"
            ></textarea>
          </div>
          
          <div>
            <label for="level" class="block font-bold uppercase tracking-wider mb-2">Cấp độ</label>
            <select 
              id="level" 
              v-model="deckForm.level" 
              class="w-full border-2 border-black p-3 bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-blue font-bold appearance-none cursor-pointer"
            >
              <option value="ELEMENTARY">Cơ bản (Elementary)</option>
              <option value="INTERMEDIATE">Trung cấp (Intermediate)</option>
              <option value="UPPER_INTERMEDIATE">Nâng cao (Upper-Intermediate)</option>
            </select>
          </div>
          
          <div class="flex items-end pb-3">
            <label class="flex items-center gap-3 cursor-pointer group">
              <div class="relative w-8 h-8 flex items-center justify-center border-2 border-black bg-white group-hover:bg-gray-100">
                <input type="checkbox" v-model="deckForm.isPublic" class="opacity-0 absolute inset-0 cursor-pointer" />
                <div v-if="deckForm.isPublic" class="w-4 h-4 bg-primary-red"></div>
              </div>
              <span class="font-bold uppercase tracking-wider select-none">Chia sẻ công khai</span>
            </label>
          </div>
        </div>
      </section>

      <!-- Vocabulary List -->
      <section class="bg-white border-4 border-black shadow-hard-lg p-8 relative">
        <div class="absolute -top-4 -right-4 w-8 h-8 bg-primary-blue rounded-full border-4 border-black"></div>
        
        <div class="flex justify-between items-center mb-6">
          <h2 class="font-black text-2xl uppercase tracking-wide bg-black text-white inline-block px-4 py-1">Danh sách Từ vựng</h2>
          <span class="font-bold bg-primary-yellow border-2 border-black px-3 py-1">{{ words.length }} từ</span>
        </div>
        
        <div v-if="words.length === 0" class="text-center py-12 border-4 border-dashed border-gray-300 mb-6 bg-gray-50">
          <p class="font-bold text-gray-500 uppercase tracking-wider mb-4">Chưa có từ vựng nào.</p>
          <BauhausButton variant="primary" @click="addWord" type="button">Thêm từ đầu tiên</BauhausButton>
        </div>

        <div v-else class="space-y-6 mb-8">
          <div v-for="(word, index) in words" :key="index" class="border-2 border-black p-6 relative bg-gray-50 group hover:border-primary-blue transition-colors shadow-sm">
            <div class="absolute top-0 left-0 bg-black text-white font-black px-3 py-1 text-sm border-r-2 border-b-2 border-black">
              #{{ index + 1 }}
            </div>
            
            <button type="button" @click="removeWord(index)" class="absolute top-4 right-4 w-8 h-8 flex items-center justify-center border-2 border-black bg-white text-black hover:bg-primary-red hover:text-white transition-colors group-hover:shadow-hard-sm z-10" title="Xóa từ">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
            
            <div class="grid grid-cols-1 md:grid-cols-12 gap-4 mt-6">
              <div class="md:col-span-4">
                <label class="block font-bold text-xs uppercase tracking-wider mb-1">Từ tiếng Anh *</label>
                <input v-model="word.word" type="text" required class="w-full border-2 border-black p-2 focus:outline-none focus:border-primary-blue font-bold" placeholder="VD: Hello" />
              </div>
              <div class="md:col-span-3">
                <label class="block font-bold text-xs uppercase tracking-wider mb-1">Phiên âm</label>
                <input v-model="word.pronunciation" type="text" class="w-full border-2 border-black p-2 focus:outline-none focus:border-primary-blue" placeholder="VD: /həˈləʊ/" />
              </div>
              <div class="md:col-span-5">
                <label class="block font-bold text-xs uppercase tracking-wider mb-1">Từ loại</label>
                <input v-model="word.wordType" type="text" class="w-full border-2 border-black p-2 focus:outline-none focus:border-primary-blue" placeholder="VD: noun, verb, adj..." />
              </div>
              
              <div class="md:col-span-6">
                <label class="block font-bold text-xs uppercase tracking-wider mb-1">Nghĩa tiếng Việt *</label>
                <input v-model="word.meaning" type="text" required class="w-full border-2 border-black p-2 focus:outline-none focus:border-primary-blue" placeholder="VD: Xin chào" />
              </div>
              <div class="md:col-span-6">
                <label class="block font-bold text-xs uppercase tracking-wider mb-1">Câu ví dụ</label>
                <input v-model="word.exampleSentence" type="text" class="w-full border-2 border-black p-2 focus:outline-none focus:border-primary-blue" placeholder="VD: Hello, how are you?" />
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-center border-t-2 border-dashed border-gray-300 pt-6">
          <button type="button" @click="addWord" class="flex items-center gap-2 font-bold uppercase tracking-wider border-2 border-black px-6 py-3 bg-white hover:bg-gray-100 hover:-translate-y-1 transition-transform shadow-hard-sm">
            <div class="w-5 h-5 bg-primary-yellow border border-black flex items-center justify-center rounded-full text-lg leading-none">+</div>
            Thêm từ mới
          </button>
        </div>
      </section>

      <!-- Form Actions -->
      <div class="flex justify-end gap-4 pb-12">
        <BauhausButton variant="outline" size="lg" type="button" @click="$router.push('/decks')">Hủy bỏ</BauhausButton>
        <BauhausButton variant="primary" size="lg" type="submit" :disabled="loading || words.length === 0" class="min-w-[200px] justify-center">
          <span v-if="loading" class="animate-spin mr-2 inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full"></span>
          {{ loading ? 'ĐANG LƯU...' : 'LƯU BỘ TỪ' }}
        </BauhausButton>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import deckService from '@/services/deckService'
import vocabularyService from '@/services/vocabularyService'
import BauhausButton from '@/components/bauhaus/BauhausButton.vue'

const router = useRouter()

const loading = ref(false)
const error = ref(null)
const success = ref(null)
const newDeckId = ref(null)

const deckForm = reactive({
  title: '',
  description: '',
  level: 'ELEMENTARY',
  isPublic: false
})

const words = ref([
  { word: '', pronunciation: '', wordType: '', meaning: '', exampleSentence: '' }
])

function addWord() {
  words.value.push({ word: '', pronunciation: '', wordType: '', meaning: '', exampleSentence: '' })
}

function removeWord(index) {
  words.value.splice(index, 1)
}

function goToNewDeck() {
  if (newDeckId.value) {
    router.push(`/decks/${newDeckId.value}`)
  }
}

async function handleSave() {
  // Validate
  if (!deckForm.title.trim()) {
    error.value = 'Vui lòng nhập tên bộ từ'
    window.scrollTo(0, 0)
    return
  }

  const validWords = words.value.filter(w => w.word.trim() && w.meaning.trim())
  if (validWords.length === 0) {
    error.value = 'Vui lòng thêm ít nhất 1 từ vựng hoàn chỉnh (điền đủ từ và nghĩa)'
    return
  }

  loading.value = true
  error.value = null
  success.value = null

  try {
    // 1. Create deck (map form fields to API field names)
    const deckRes = await deckService.createDeck({
      name: deckForm.title,
      description: deckForm.description,
      cefrLevel: deckForm.level,
      isPublic: deckForm.isPublic
    })
    const deckId = deckRes.id
    newDeckId.value = deckId

    // 2. Create vocabularies and add to deck
    let addedCount = 0
    for (const vocabData of validWords) {
      try {
        // Create vocab via API (Note: In a real prod environment, we might want a bulk insert API)
        const vocabRes = await vocabularyService.create(vocabData)
        // Add to deck
        await deckService.addWordToDeck(deckId, vocabRes.id)
        addedCount++
      } catch (vocabErr) {
        console.error('Lỗi khi thêm từ:', vocabData.word, vocabErr)
      }
    }

    success.value = `Đã lưu thành công bộ từ với ${addedCount} từ vựng!`
    window.scrollTo(0, 0)
  } catch (err) {
    console.error('Save error', err)
    error.value = 'Có lỗi xảy ra khi lưu bộ từ. Vui lòng thử lại.'
    window.scrollTo(0, 0)
  } finally {
    loading.value = false
  }
}
</script>
