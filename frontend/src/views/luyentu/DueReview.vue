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
        <p class="mt-4 font-bold text-sm uppercase tracking-wider text-muted-foreground">Đang tải từ đến hạn...</p>
      </div>

      <div v-else-if="error" class="bg-danger/10 border-2 border-danger rounded-md p-8 text-center shadow-pop-xl" role="alert">
        <p class="font-black text-lg text-danger">{{ error }}</p>
        <AppButton variant="secondary" size="sm" class="mt-4" @click="load">Thử lại</AppButton>
      </div>

      <!-- Nothing due: this is the normal, healthy state, not an error -->
      <div v-else-if="words.length === 0" class="bg-card border-2 border-dashed border-foreground rounded-md p-12 text-center shadow-pop-xl">
        <div class="inline-flex items-center justify-center w-20 h-20 bg-quaternary border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
          <Check class="w-10 h-10 text-white" />
        </div>
        <h1 class="font-black text-2xl uppercase tracking-tight">Không có từ nào đến hạn</h1>
        <p class="mt-2 text-muted-foreground font-medium">Bạn đã ôn hết rồi. Quay lại sau để tiếp tục.</p>
        <AppButton as="router-link" variant="secondary" size="sm" class="mt-6" :to="'/decks/' + deckId">Về bộ từ</AppButton>
      </div>

      <div v-else class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl text-center">
        <UserPageHeader
          eyebrow="Ôn từ đến hạn"
          :title="deckName"
          :subtitle="`${words.length} từ đến hạn ôn hôm nay.`"
          :divided="false"
          root-class="mb-8"
        >
          <template #accent>Due</template>
        </UserPageHeader>

        <!-- Progress -->
        <div class="max-w-md mx-auto mb-8">
          <div class="flex justify-between text-xs font-bold uppercase tracking-wider mb-2">
            <span class="text-muted-foreground">{{ reviewedCount }}/{{ words.length }}</span>
            <span class="text-accent-ink">{{ progressPercent }}%</span>
          </div>
          <div class="w-full h-2 border-2 border-foreground bg-muted rounded-full overflow-hidden">
            <div class="h-full bg-accent rounded-full transition-all duration-500" :style="{ width: `${progressPercent}%` }"></div>
          </div>
        </div>

        <!-- Card -->
        <div v-if="currentWord" class="relative mb-8">
          <div @click="flipCard"
            class="rounded-2xl border-2 border-foreground p-10 min-h-[280px] flex items-center justify-center cursor-pointer transition-all duration-500 relative shadow-pop-xl hover:shadow-pop-lg"
            :class="[cardBg, cardInk]"
          >
            <div class="absolute top-4 right-4 px-3 py-1 rounded-full bg-foreground/90 text-background text-[10px] font-black uppercase tracking-widest">
              {{ wordTypeLabel }}
            </div>

            <div class="absolute bottom-3 left-3 w-6 h-6 bg-foreground/20 border-2 border-foreground rounded-full"></div>
            <div class="absolute top-1/2 -translate-y-1/2 -left-2 w-4 h-4 bg-foreground/10 border-2 border-foreground rounded-full"></div>

            <div class="text-center">
              <p class="text-xs font-bold uppercase tracking-widest opacity-70 mb-2">{{ isFlipped ? 'Nghĩa' : 'Từ' }}</p>
              <h2 class="font-black text-4xl md:text-5xl uppercase tracking-tight drop-shadow-sm" v-html="sanitizeText(isFlipped ? currentWord.meaning : currentWord.word)"></h2>
              <p v-if="!isFlipped && currentWord.phonetic" class="font-bold text-lg opacity-80 mt-2">{{ currentWord.phonetic }}</p>
              <p v-if="isFlipped && currentWord.example" class="font-medium opacity-80 mt-4 max-w-lg mx-auto italic">"<span v-html="sanitizeText(currentWord.example)"></span>"</p>
            </div>

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

        <!-- Done -->
        <div v-else class="py-10">
          <div class="inline-flex items-center justify-center w-20 h-20 bg-quaternary border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
            <Check class="w-10 h-10 text-white" />
          </div>
          <h2 class="font-black text-3xl uppercase tracking-tight mb-3">Đã ôn xong {{ reviewedCount }} từ!</h2>
          <p class="text-muted-foreground font-medium mb-6">Tiến độ đã được lưu. Hẹn gặp lại ở lần ôn tới.</p>
          <AppButton as="router-link" variant="primary" :to="'/decks/' + deckId">Về bộ từ</AppButton>
        </div>

        <!-- Rating buttons — same three levels as the flashcard drill, mapped to SM-2 quality -->
        <div v-if="currentWord" class="flex items-center justify-center gap-4">
          <AppButton @click="rate('again')" variant="pink">Lại</AppButton>
          <AppButton @click="rate('good')" variant="primary">Tiếp theo</AppButton>
          <AppButton @click="rate('easy')" variant="emerald">Dễ</AppButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import deckService from '@/services/deckService'
import srsService from '@/services/srsService'
import { Check, Volume2 } from 'lucide-vue-next'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { sanitizeText } from '@/utils/markdown'

const route = useRoute()
const deckId = route.params.id

const deckName = ref('Ôn từ đến hạn')
const words = ref([])
const currentIndex = ref(0)
const reviewedCount = ref(0)
const isFlipped = ref(false)
const loading = ref(true)
const error = ref('')

const currentWord = computed(() => words.value[currentIndex.value])
const progressPercent = computed(() =>
  words.value.length ? Math.round((reviewedCount.value / words.value.length) * 100) : 0
)

// Same colour system as FlashcardGame, keyed on the part of speech.
const THEMES = {
  verb: { bg: 'bg-accent-strong', ink: 'text-white' },
  noun: { bg: 'bg-quaternary', ink: 'text-foreground' },
  adjective: { bg: 'bg-tertiary', ink: 'text-foreground' },
  adverb: { bg: 'bg-secondary-strong', ink: 'text-white' },
  preposition: { bg: 'bg-foreground', ink: 'text-background' },
  default: { bg: 'bg-accent', ink: 'text-white' }
}
const THEME_LABELS = {
  verb: 'Động từ', noun: 'Danh từ', adjective: 'Tính từ', adverb: 'Trạng từ',
  preposition: 'Giới từ', default: 'Từ vựng'
}
const wordTypeLabel = computed(() => {
  const type = (currentWord.value?.wordType || '').toLowerCase()
  return THEME_LABELS[type] || THEME_LABELS.default
})
const cardTheme = computed(() => THEMES[(currentWord.value?.wordType || '').toLowerCase()] || THEMES.default)
const cardBg = computed(() => cardTheme.value.bg)
const cardInk = computed(() => cardTheme.value.ink)

// "Lại" -> 1, "Tiếp theo" -> 4, "Dễ" -> 5 — the SM-2 quality scale.
const RATING_QUALITY = { again: 1, good: 4, easy: 5 }

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [due] = await Promise.all([
      srsService.getDueWords(deckId),
      // The deck name is cosmetic; a failure here must not block the drill.
      deckService.getDeckById(deckId).then(d => { if (d?.name) deckName.value = d.name }).catch(() => {})
    ])
    words.value = due
    currentIndex.value = 0
    reviewedCount.value = 0
    isFlipped.value = false
  } catch (e) {
    error.value = e.response?.data?.detail || e.response?.data?.message || 'Không tải được từ đến hạn.'
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

function rate(rating) {
  if (currentIndex.value >= words.value.length) return
  const vocabId = currentWord.value?.id
  // Fire-and-forget: a failed save must not block the session.
  if (vocabId) {
    srsService.review(vocabId, RATING_QUALITY[rating] ?? 4)
      .catch(err => console.warn('Không lưu được review:', err?.message || err))
  }
  reviewedCount.value++
  currentIndex.value++
  isFlipped.value = false
}
</script>
