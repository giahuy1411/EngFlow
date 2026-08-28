<template>
  <div class="bg-background min-h-screen py-12">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Back -->
      <router-link :to="'/decks/' + deckId"
        class="inline-flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent transition-colors mb-6"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
        Quay lại
      </router-link>

      <div v-if="loading" class="bg-card border-2 border-foreground rounded-md p-12 text-center shadow-pop-xl" role="status">
        <div class="w-10 h-10 mx-auto border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
        <p class="mt-4 font-bold text-sm uppercase tracking-wider text-muted-foreground">Đang tải flashcard...</p>
      </div>

      <div v-else-if="error" class="bg-danger/10 border-2 border-danger rounded-md p-8 text-center shadow-pop-xl" role="alert">
        <p class="font-black text-lg text-danger">{{ error }}</p>
        <AppButton variant="secondary" size="sm" class="mt-4" @click="loadDeck">Thử lại</AppButton>
      </div>

      <div v-else-if="words.length === 0" class="bg-card border-2 border-dashed border-foreground rounded-md p-12 text-center shadow-pop-xl">
        <p class="font-black text-2xl uppercase">Chưa có từ vựng</p>
        <p class="mt-2 text-muted-foreground font-medium">Thêm từ vào bộ này trước khi luyện flashcard.</p>
      </div>

      <div v-else class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl text-center">
        <UserPageHeader
          eyebrow="Flashcard drill"
          :title="deck?.name || 'Flashcard'"
          :subtitle="`${words.length} từ trong phiên luyện này.`"
          :divided="false"
          root-class="mb-8"
        >
          <template #accent>Cards</template>
        </UserPageHeader>

        <!-- Progress bar -->
        <div class="max-w-md mx-auto mb-8">
          <div class="flex justify-between text-xs font-bold uppercase tracking-wider mb-2">
            <span class="text-muted-foreground">{{ displayIndex }}/{{ words.length }}</span>
            <span class="text-accent">{{ progressPercent }}%</span>
          </div>
          <div class="w-full h-2 border-2 border-foreground bg-muted rounded-full overflow-hidden">
            <div class="h-full bg-accent rounded-full transition-all duration-500" :style="{ width: `${progressPercent}%` }"></div>
          </div>
        </div>

        <!-- Card area -->
        <div v-if="currentWord" class="relative mb-8">
          <div @click="flipCard"
            class="rounded-2xl border-2 border-foreground p-10 min-h-[280px] flex items-center justify-center cursor-pointer transition-all duration-500 relative shadow-pop-xl hover:shadow-pop-lg"
            :class="cardTheme"
          >
            <!-- Theme badge -->
            <div class="absolute top-4 right-4 px-3 py-1 rounded-full bg-foreground/90 text-background text-[10px] font-black uppercase tracking-widest">
              {{ wordTypeLabel }}
            </div>

            <!-- Flip decoration -->
            <div class="absolute bottom-3 left-3 w-6 h-6 bg-foreground/20 border-2 border-foreground rounded-full"></div>
            <div class="absolute top-1/2 -translate-y-1/2 -left-2 w-4 h-4 bg-foreground/10 border-2 border-foreground rounded-full"></div>

            <div class="text-center">
              <p class="text-xs font-bold uppercase tracking-widest text-background/70 mb-2">{{ isFlipped ? 'Nghĩa' : 'Từ' }}</p>
              <h2 class="font-black text-4xl md:text-5xl uppercase tracking-tight text-background drop-shadow-sm">{{ isFlipped ? currentWord.meaning : currentWord.word }}</h2>
              <p v-if="!isFlipped && currentWord.phonetic" class="font-bold text-lg text-background/80 mt-2">{{ currentWord.phonetic }}</p>
              <p v-if="isFlipped && currentWord.example" class="font-medium text-background/80 mt-4 max-w-lg mx-auto italic">"{{ currentWord.example }}"</p>
            </div>

            <!-- Audio button — tiny circular icon-only overlay control; keep raw <button> to preserve absolute layout -->
            <button v-if="currentWord.audioUrl" @click.stop="playAudio(currentWord.audioUrl)"
              class="absolute top-4 left-4 w-10 h-10 bg-foreground/20 border-2 border-foreground rounded-full flex items-center justify-center text-background hover:bg-foreground/30 transition-all shadow-pop-sm"
            >
              <Volume2 class="w-5 h-5" />
            </button>
          </div>
          <p class="text-center mt-4">
            <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">
              {{ isFlipped ? 'Nhấn để xem từ' : 'Nhấn để lật thẻ' }}
            </span>
          </p>
        </div>

        <div v-else class="py-10">
          <div class="inline-flex items-center justify-center w-20 h-20 bg-quaternary border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
            <Check class="w-10 h-10 text-white" />
          </div>
          <h2 class="font-black text-3xl uppercase tracking-tight mb-3">Hoàn thành!</h2>
          <AppButton @click="restart" variant="primary">Luyện lại</AppButton>
        </div>

        <!-- Navigation buttons -->
        <div v-if="currentWord" class="flex items-center justify-center gap-4">
          <AppButton @click="markWord('again')" variant="pink">Lại</AppButton>
          <AppButton @click="markWord('good')" variant="primary">Tiếp theo</AppButton>
          <AppButton @click="markWord('easy')" variant="emerald">Dễ</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import deckService from '@/services/deckService'
import { Check, Volume2 } from 'lucide-vue-next'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'

const route = useRoute()
const deckId = route.params.id
const deck = ref(null)
const words = ref([])
const currentIndex = ref(0)
const isFlipped = ref(false)
const loading = ref(true)
const error = ref('')

const currentWord = computed(() => words.value[currentIndex.value])
const displayIndex = computed(() => Math.min(currentIndex.value + 1, words.value.length))
const progressPercent = computed(() => words.value.length ? Math.round((Math.min(currentIndex.value, words.value.length) / words.value.length) * 100) : 0)

// Color themes by word type — mỗi loại từ một gradient riêng để thẻ đỡ đơn điệu.
// Chữ dùng text-background (gần đen/trắng tùy theme) đảm bảo tương phản trên nền màu.
const THEMES = {
  verb: 'bg-gradient-to-br from-sky-500 via-sky-600 to-indigo-700',
  noun: 'bg-gradient-to-br from-emerald-500 via-emerald-600 to-teal-700',
  adjective: 'bg-gradient-to-br from-amber-400 via-orange-500 to-rose-600',
  adverb: 'bg-gradient-to-br from-fuchsia-500 via-purple-600 to-violet-700',
  preposition: 'bg-gradient-to-br from-cyan-500 via-sky-600 to-blue-700',
  pronoun: 'bg-gradient-to-br from-lime-500 via-green-600 to-emerald-700',
  conjunction: 'bg-gradient-to-br from-rose-400 via-pink-500 to-fuchsia-600',
  interjection: 'bg-gradient-to-br from-orange-400 via-amber-500 to-yellow-600',
  default: 'bg-gradient-to-br from-accent via-accent/90 to-indigo-600',
}
const THEME_LABELS = {
  verb: 'Động từ',
  noun: 'Danh từ',
  adjective: 'Tính từ',
  adverb: 'Trạng từ',
  preposition: 'Giới từ',
  pronoun: 'Đại từ',
  conjunction: 'Liên từ',
  interjection: 'Thán từ',
  default: 'Từ vựng',
}

const wordTypeLabel = computed(() => {
  const type = (currentWord.value?.wordType || currentWord.value?.type || '').toLowerCase()
  return THEME_LABELS[type] || THEME_LABELS.default
})

const cardTheme = computed(() => {
  const type = (currentWord.value?.wordType || currentWord.value?.type || '').toLowerCase()
  return THEMES[type] || THEMES.default
})

onMounted(loadDeck)

async function loadDeck() {
  loading.value = true
  error.value = ''
  try {
    const data = await deckService.getDeckById(deckId)
    deck.value = data
    words.value = data.words || []
    currentIndex.value = 0
    isFlipped.value = false
  } catch (e) {
    error.value = e.response?.data?.message || 'Không tải được bộ từ.'
  } finally {
    loading.value = false
  }
}

function flipCard() {
  isFlipped.value = !isFlipped.value
}

function playAudio(url) {
  new Audio(url).play()
}

function markWord(rating) {
  if (currentIndex.value < words.value.length) {
    currentIndex.value++
    isFlipped.value = false
  }
}

function restart() {
  currentIndex.value = 0
  isFlipped.value = false
}
</script>
