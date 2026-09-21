<template>
  <div class="bg-background min-h-screen py-12">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Back -->
      <router-link :to="'/decks/' + deckId"
        class="inline-flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent-ink transition-colors mb-6"
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
            <span class="text-accent-ink">{{ progressPercent }}%</span>
          </div>
          <div class="w-full h-2 border-2 border-foreground bg-muted rounded-full overflow-hidden">
            <div class="h-full bg-accent rounded-full transition-all duration-500" :style="{ width: `${progressPercent}%` }"></div>
          </div>
        </div>

        <!-- Card area -->
        <div v-if="currentWord" class="relative mb-8">
          <div @click="flipCard"
            class="rounded-2xl border-2 border-foreground p-10 min-h-[280px] flex items-center justify-center cursor-pointer transition-all duration-500 relative shadow-pop-xl hover:shadow-pop-lg"
            :class="[cardBg, cardInk]"
          >
            <!-- Theme badge -->
            <div class="absolute top-4 right-4 px-3 py-1 rounded-full bg-foreground/90 text-background text-[10px] font-black uppercase tracking-widest">
              {{ wordTypeLabel }}
            </div>

            <!-- Flip decoration -->
            <div class="absolute bottom-3 left-3 w-6 h-6 bg-foreground/20 border-2 border-foreground rounded-full"></div>
            <div class="absolute top-1/2 -translate-y-1/2 -left-2 w-4 h-4 bg-foreground/10 border-2 border-foreground rounded-full"></div>

            <div class="text-center">
              <p class="text-xs font-bold uppercase tracking-widest opacity-70 mb-2">{{ isFlipped ? 'Nghĩa' : 'Từ' }}</p>
              <h2 class="font-black text-4xl md:text-5xl uppercase tracking-tight drop-shadow-sm" v-html="sanitizeText(isFlipped ? currentWord.meaning : currentWord.word)"></h2>
              <p v-if="!isFlipped && currentWord.phonetic" class="font-bold text-lg opacity-80 mt-2">{{ currentWord.phonetic }}</p>
              <p v-if="isFlipped && currentWord.example" class="font-medium opacity-80 mt-4 max-w-lg mx-auto italic">"<span v-html="sanitizeText(currentWord.example)"></span>"</p>
            </div>

            <!-- Audio button — tiny circular icon-only overlay control; keep raw <button> to preserve absolute layout -->
            <button v-if="currentWord.audioUrl" @click.stop="playAudio(currentWord.audioUrl)" :aria-label="'Phát âm ' + currentWord.word"
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

        <!-- Navigation buttons — audit-v12 F153: two buttons only. The drill no longer rates
             the word, so there is nothing to send but the study day (see advance()). -->
        <div v-if="currentWord" class="flex items-center justify-center gap-4">
          <AppButton @click="goBack" variant="secondary" :disabled="currentIndex === 0">Quay lại</AppButton>
          <AppButton @click="advance" variant="primary">Tiếp theo</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import deckService from '@/services/deckService'
import flashcardService from '@/services/flashcardService'
import { Check, Volume2 } from 'lucide-vue-next'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { sanitizeText } from '@/utils/markdown'

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

// Color themes by word type — audit-v5: dùng đúng 4 màu token của Playful
// Geometric (accent #8B5CF6, secondary #F472B6, tertiary #FBBF24,
// quaternary #34D399) + nền fg. Design system KHÔNG dùng gradient cho
// surface nên bỏ hết gradient Tailwind off-palette; phân biệt loại từ bằng
// màu phẳng + border-2 + shadow-pop (đúng "hard pop" của hệ).
// cardInk = màu chữ trên nền thẻ: nền tối → trắng; nền sáng (tertiary) → fg.
const THEMES = {
  verb: { bg: 'bg-accent-strong', ink: 'text-white' },
  noun: { bg: 'bg-quaternary', ink: 'text-foreground' },
  adjective: { bg: 'bg-tertiary', ink: 'text-foreground' },
  adverb: { bg: 'bg-secondary-strong', ink: 'text-white' },
  preposition: { bg: 'bg-foreground', ink: 'text-background' },
  pronoun: { bg: 'bg-accent-strong', ink: 'text-white' },
  conjunction: { bg: 'bg-secondary', ink: 'text-white' },
  interjection: { bg: 'bg-tertiary', ink: 'text-foreground' },
  default: { bg: 'bg-accent', ink: 'text-white' },
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
const cardBg = computed(() => cardTheme.value.bg)
const cardInk = computed(() => cardTheme.value.ink)

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
    studyRecorded.value = false
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

// audit-v12 F153: the drill is a plain reader now — "Quay lại" (back) and "Tiếp theo"
// (continue). It no longer rates the word, so it sends no SM-2 quality and does not touch
// user_vocabulary_progress. It DOES still count as a study day for the streak, which is
// recorded once per session on the first "continue" (a learner may leave mid-deck, so
// waiting for completion would lose the day).
const studyRecorded = ref(false)

function goBack() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    isFlipped.value = false
  }
}

function advance() {
  if (currentIndex.value >= words.value.length) return

  if (!studyRecorded.value) {
    studyRecorded.value = true
    // Fire-and-forget: a failed record must not block the drill.
    flashcardService.recordStudy()
      .catch(err => console.warn('Không ghi được ngày học:', err?.message || err))
  }

  currentIndex.value++
  isFlipped.value = false
}

function restart() {
  currentIndex.value = 0
  isFlipped.value = false
  studyRecorded.value = false
}
</script>
