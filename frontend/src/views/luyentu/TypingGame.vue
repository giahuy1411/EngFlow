<template>
  <div class="bg-background min-h-screen py-16">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
      <UserPageHeader
        eyebrow="Typing drill"
        title="Gõ từ"
        subtitle="Gõ từ tiếng Anh dựa vào nghĩa và kiểm tra phản xạ chính tả."
        root-class="mb-10"
      >
        <template #accent>Typing</template>
      </UserPageHeader>

      <div v-if="loading" class="bg-card border-2 border-foreground rounded-md p-12 shadow-pop-xl" role="status">
        <div class="w-10 h-10 mx-auto border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
        <p class="mt-4 font-bold text-sm uppercase tracking-wider text-muted-foreground">Đang tải gõ từ...</p>
      </div>

      <div v-else-if="error" class="bg-danger/10 border-2 border-danger rounded-md p-8 shadow-pop-xl" role="alert">
        <p class="font-black text-lg text-danger">{{ error }}</p>
        <AppButton variant="secondary" size="sm" class="mt-4" @click="loadDeck">Thử lại</AppButton>
      </div>

      <div v-else-if="words.length === 0" class="bg-card border-2 border-dashed border-foreground rounded-md p-12 shadow-pop-xl">
        <p class="font-black text-2xl uppercase">Chưa có từ vựng</p>
        <p class="mt-2 text-muted-foreground font-medium">Thêm từ vào bộ này trước khi luyện gõ.</p>
      </div>

      <template v-else>

      <div v-if="currentWord" class="mb-8">
        <!-- Meaning prompt -->
        <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl mb-6">
          <p class="text-xs font-bold uppercase tracking-widest text-muted-foreground mb-2">Nghĩa</p>
          <h2 class="font-black text-3xl uppercase tracking-tight" v-html="sanitizeText(currentWord.meaning)"></h2>
        </div>

        <!-- Input -->
        <div class="max-w-md mx-auto">
          <input
            id="typing-answer-input"
            v-model="userInput"
            name="typing-answer"
            @keyup.enter="checkAnswer"
            placeholder="Gõ từ..."
            aria-label="Gõ từ tiếng Anh"
            class="w-full bg-input border-2 border-border rounded-sm px-4 py-3 font-sans text-lg text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground text-center uppercase tracking-wider font-black"
            autofocus
          />
          <AppButton @click="checkAnswer" variant="primary" size="lg" class="mt-4"
          :disabled="!userInput.trim()">Kiểm tra</AppButton>
          <p v-if="feedback" class="mt-4 font-bold text-sm uppercase tracking-wider" :class="lastAnswerCorrect ? 'text-success-ink' : 'text-danger'">
            {{ lastAnswerCorrect ? 'Đúng!' : ('Sai. Đáp án: ' + feedback) }}
          </p>
        </div>
      </div>

      <!-- Progress -->
      <div class="max-w-md mx-auto">
        <div class="flex justify-between text-xs font-bold uppercase tracking-wider mb-2 mt-8">
          <span class="text-muted-foreground">{{ displayIndex }}/{{ words.length }}</span>
          <span class="text-accent-ink">{{ correct }}/{{ total }} đúng</span>
        </div>
        <div class="w-full h-2 border-2 border-foreground bg-muted rounded-full overflow-hidden">
          <div class="h-full bg-accent rounded-full transition-all" :style="{ width: `${progressPercent}%` }"></div>
        </div>
      </div>

      <!-- Completed -->
      <div v-if="currentIndex >= words.length" class="py-12">
        <div class="inline-flex items-center justify-center w-20 h-20 bg-quaternary border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
          <Check class="w-10 h-10 text-white" />
        </div>
        <h2 class="font-black text-3xl uppercase tracking-tight mb-4">Hoàn thành!</h2>
        <p v-if="submitting" class="mb-6 font-bold text-sm uppercase tracking-wider text-muted-foreground">Đang lưu kết quả...</p>
        <p v-else-if="submitResult" class="mb-6 font-black text-lg text-success-ink">
          Đã lưu: {{ submitResult.correctAnswers }}/{{ submitResult.totalQuestions }} đúng · +{{ submitResult.correctAnswers }} điểm
        </p>
        <router-link :to="'/decks/' + deckId"
          class="inline-flex px-8 py-3.5 font-bold text-base bg-accent-strong text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all"
        >Quay lại</router-link>
      </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import gameService from '@/services/gameService'
import { Check } from 'lucide-vue-next'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { sanitizeText } from '@/utils/markdown'

const route = useRoute()
const deckId = route.params.id
const sessionId = ref('')
const words = ref([])
const currentIndex = ref(0)
const correct = ref(0)
const total = ref(0)
const userInput = ref('')
const feedback = ref('')
const lastAnswerCorrect = ref(false)
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const submitResult = ref(null)
const currentWord = computed(() => words.value[currentIndex.value])
const displayIndex = computed(() => Math.min(currentIndex.value + 1, words.value.length))
const progressPercent = computed(() => words.value.length ? Math.round((Math.min(currentIndex.value, words.value.length) / words.value.length) * 100) : 0)

onMounted(loadDeck)

async function loadDeck() {
  loading.value = true
  error.value = ''
  submitResult.value = null
  try {
    const data = await gameService.startTyping(deckId)
    sessionId.value = data.sessionId
    words.value = (data.data || []).map(item => ({
      vocabId: item.vocabId,
      word: item.word,
      meaning: item.definitionVi,
      phonetic: item.pronunciation,
      audioUrl: item.audioUrl,
    }))
    currentIndex.value = 0
    correct.value = 0
    total.value = 0
    userInput.value = ''
    feedback.value = ''
    lastAnswerCorrect.value = false
  } catch (e) {
    error.value = e.response?.data?.message || 'Không tải được bộ từ.'
  } finally {
    loading.value = false
  }
}

function checkAnswer() {
  if (!userInput.value.trim()) return
  total.value++
  if (userInput.value.trim().toLowerCase() === currentWord.value.word.toLowerCase()) {
    correct.value++
    feedback.value = currentWord.value.word
    lastAnswerCorrect.value = true
    setTimeout(nextWord, 800)
  } else {
    feedback.value = currentWord.value.word
    lastAnswerCorrect.value = false
    setTimeout(nextWord, 1200)
  }
}

function nextWord() {
  currentIndex.value++
  userInput.value = ''
  feedback.value = ''
  lastAnswerCorrect.value = false
  if (currentIndex.value >= words.value.length) {
    submitGame()
  }
  nextTick(() => document.querySelector('input')?.focus())
}

async function submitGame() {
  submitting.value = true
  try {
    submitResult.value = await gameService.submit(sessionId.value, null, correct.value)
  } catch (e) {
    // Silent fail
  } finally {
    submitting.value = false
  }
}
</script>
