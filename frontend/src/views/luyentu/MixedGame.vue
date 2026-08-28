<template>
  <div class="bg-background min-h-screen py-12">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <router-link :to="`/decks/${deckId}`"
        class="inline-flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent transition-colors mb-6"
      >
        <span aria-hidden="true">←</span> Quay lại
      </router-link>

      <div v-if="loading" class="bg-card border-2 border-foreground rounded-md p-12 text-center shadow-pop-xl" role="status">
        <div class="w-10 h-10 mx-auto border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
        <p class="mt-4 font-bold text-sm uppercase tracking-wider text-muted-foreground">Đang tạo bài luyện tổng hợp...</p>
      </div>

      <div v-else-if="error" class="bg-danger/10 border-2 border-danger rounded-md p-8 text-center shadow-pop-xl" role="alert">
        <p class="font-black text-lg text-danger">{{ error }}</p>
        <button class="mt-4 px-5 py-2 border-2 border-foreground rounded-full font-bold uppercase text-sm" @click="loadDeck">Thử lại</button>
      </div>

      <div v-else-if="questions.length === 0" class="bg-card border-2 border-dashed border-foreground rounded-md p-12 text-center shadow-pop-xl">
        <p class="font-black text-2xl uppercase">Chưa có từ vựng</p>
        <p class="mt-2 text-muted-foreground font-medium">Bộ từ cần có cả từ và nghĩa để tạo bài luyện tổng hợp.</p>
      </div>

      <div v-else class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl text-center">
        <UserPageHeader
          eyebrow="Mixed drill"
          title="Tổng hợp"
          subtitle="Đổi chiều câu hỏi sau mỗi lượt để luyện cả nhận diện nghĩa và nhớ từ."
          :divided="false"
          root-class="mb-8"
        >
          <template #accent>Mixed</template>
        </UserPageHeader>

        <template v-if="currentQuestion">
          <div class="max-w-md mx-auto mb-8">
            <div class="flex justify-between text-xs font-bold uppercase tracking-wider mb-2">
              <span class="text-muted-foreground">{{ currentIndex + 1 }}/{{ questions.length }}</span>
              <span class="text-accent">{{ score }}/{{ answeredCount }} đúng</span>
            </div>
            <div class="w-full h-2 border-2 border-foreground bg-muted rounded-full overflow-hidden">
              <div class="h-full bg-accent rounded-full transition-all duration-500" :style="{ width: `${(currentIndex / questions.length) * 100}%` }" />
            </div>
          </div>

          <p class="text-xs font-bold uppercase tracking-widest text-muted-foreground mb-3">{{ currentQuestion.direction }}</p>
          <h2 class="font-black text-4xl uppercase tracking-tight mb-8">{{ currentQuestion.prompt }}</h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-xl mx-auto">
            <button v-for="(option, index) in currentQuestion.options" :key="option"
              class="p-4 border-2 border-foreground rounded-md font-bold transition-all"
              :class="optionClass(index)"
              :disabled="answered"
              @click="selectAnswer(index)"
            >{{ option }}</button>
          </div>
          <button v-if="answered" class="mt-6 px-8 py-3 bg-tertiary text-foreground font-black text-sm border-2 border-foreground rounded-full shadow-pop" @click="nextQuestion">
            {{ currentIndex < questions.length - 1 ? 'Câu tiếp theo' : 'Xem kết quả' }}
          </button>
        </template>

        <div v-else class="py-8">
          <div class="inline-flex items-center justify-center w-20 h-20 bg-quaternary border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
            <Check class="w-10 h-10 text-white" />
          </div>
          <h2 class="font-black text-3xl uppercase tracking-tight mb-2">Hoàn thành!</h2>
          <p class="font-black text-5xl text-accent mb-5">{{ score }}/{{ questions.length }}</p>
          <p v-if="submitting" class="mb-6 font-bold text-sm uppercase tracking-wider text-muted-foreground">Đang lưu kết quả...</p>
          <p v-else-if="submitResult" class="mb-6 font-black text-lg text-quaternary">
            Đã lưu: {{ submitResult.correctAnswers }}/{{ submitResult.totalQuestions }} đúng · +{{ submitResult.correctAnswers }} điểm
          </p>
          <button class="px-8 py-3 bg-accent text-white font-black text-sm border-2 border-foreground rounded-full shadow-pop" @click="restart">Luyện lại</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Check } from 'lucide-vue-next'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import gameService from '@/services/gameService'

const route = useRoute()
const deckId = route.params.id
const sessionId = ref('')
const questions = ref([])
const currentIndex = ref(0)
const score = ref(0)
const answeredCount = ref(0)
const answered = ref(false)
const selectedIndex = ref(null)
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const submitResult = ref(null)

const currentQuestion = computed(() => questions.value[currentIndex.value])

onMounted(loadDeck)

async function loadDeck() {
  loading.value = true
  error.value = ''
  submitResult.value = null
  try {
    const data = await gameService.startMixed(deckId)
    sessionId.value = data.sessionId
    const items = data.data || []
    questions.value = items.map((item, index) => {
      const askForMeaning = index % 2 === 0
      const correctAnswer = askForMeaning ? item.definitionVi : item.word
      const others = items.filter(x => x.vocabId !== item.vocabId)
      const alternatives = others
        .map(x => askForMeaning ? x.definitionVi : x.word)
        .filter(Boolean)
        .sort(() => Math.random() - 0.5)
        .slice(0, 3)
      return {
        vocabId: item.vocabId,
        prompt: askForMeaning ? item.word : item.definitionVi,
        direction: askForMeaning ? 'Chọn nghĩa đúng' : 'Chọn từ tiếng Anh',
        correctAnswer,
        options: [correctAnswer, ...alternatives].sort(() => Math.random() - 0.5),
      }
    })
    restart()
  } catch (cause) {
    error.value = cause.response?.data?.message || 'Không tải được bộ từ.'
  } finally {
    loading.value = false
  }
}

function selectAnswer(index) {
  if (answered.value) return
  answered.value = true
  selectedIndex.value = index
  answeredCount.value += 1
  if (currentQuestion.value.options[index] === currentQuestion.value.correctAnswer) score.value += 1
}

function optionClass(index) {
  if (!answered.value) return 'bg-card hover:bg-tertiary/10'
  const option = currentQuestion.value.options[index]
  if (option === currentQuestion.value.correctAnswer) return 'bg-quaternary/20 border-quaternary text-quaternary'
  if (index === selectedIndex.value) return 'bg-secondary/20 border-secondary text-secondary'
  return 'opacity-50'
}

function nextQuestion() {
  currentIndex.value += 1
  answered.value = false
  selectedIndex.value = null
  if (currentIndex.value >= questions.value.length) {
    submitGame()
  }
}

async function submitGame() {
  submitting.value = true
  try {
    submitResult.value = await gameService.submit(sessionId.value, null, score.value)
  } catch (e) {
    // Silent fail
  } finally {
    submitting.value = false
  }
}

function restart() {
  currentIndex.value = 0
  score.value = 0
  answeredCount.value = 0
  answered.value = false
  selectedIndex.value = null
}
</script>
