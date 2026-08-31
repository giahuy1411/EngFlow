<template>
  <div>
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-16">
      <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
    </div>

    <div v-else-if="exercises.length === 0" class="bg-card border-2 border-foreground shadow-pop-lg p-12 text-center rounded-md">
      <p class="font-black text-xl uppercase">Chưa có bài tập</p>
    </div>

    <!-- Results screen -->
    <div v-else-if="showResults" class="bg-card border-2 border-foreground shadow-pop-lg rounded-md overflow-hidden">
      <div class="p-8 text-center">
        <div class="w-20 h-20 mx-auto mb-4 rounded-full border-4 border-foreground flex items-center justify-center"
          :class="results.percentage >= 70 ? 'bg-quaternary' : 'bg-accent'">
          <span class="font-black text-3xl text-white">{{ Math.round(results.percentage) }}%</span>
        </div>
        <h3 class="font-black text-2xl uppercase mb-2">
          {{ results.percentage >= 70 ? 'Hoàn thành!' : 'Cần cố gắng hơn' }}
        </h3>
        <p class="font-bold text-muted-foreground mb-6">{{ results.score }} / {{ results.total }} câu đúng</p>

        <div class="space-y-3 text-left mb-6 max-w-lg mx-auto">
          <div v-for="r in results.results" :key="r.exerciseId"
            class="flex items-center gap-3 p-3 border-2 border-foreground rounded-md"
            :class="r.correct ? 'bg-quaternary/10 border-quaternary' : 'bg-accent/10 border-accent'">
            <span class="text-lg" aria-hidden="true">{{ r.correct ? '✓' : '✕' }}</span>
            <div>
              <p class="font-bold text-sm">Đáp án đúng: <span class="text-quaternary">{{ r.correctAnswer }}</span></p>
              <p v-if="!r.correct" class="text-xs text-muted-foreground">Bạn trả lời: {{ r.userAnswer }}</p>
            </div>
          </div>
        </div>

        <AppButton @click="resetQuiz" variant="pink">
          Làm lại
        </AppButton>
      </div>
    </div>

    <!-- Exercise flow -->
    <div v-else>
      <div class="flex items-center gap-4 mb-6">
        <span class="font-bold text-sm uppercase bg-foreground text-white px-4 py-2 rounded-full">{{ currentIndex + 1 }} / {{ exercises.length }}</span>
        <span class="px-3 py-1 bg-tertiary text-foreground font-bold text-xs uppercase tracking-wider rounded-full border-2 border-foreground">{{ typeLabel(currentExercise.exerciseType) }}</span>
        <span v-if="currentExercise.difficulty === 'EASY'" class="text-quaternary font-bold text-xs uppercase">Dễ</span>
        <span v-else-if="currentExercise.difficulty === 'MEDIUM'" class="text-tertiary font-bold text-xs uppercase">TB</span>
        <span v-else class="text-accent font-bold text-xs uppercase">Khó</span>
      </div>

      <div class="bg-card border-2 border-foreground shadow-pop-lg rounded-md overflow-hidden">
        <div class="p-6">
          <img v-if="currentExercise.imageUrl" :src="currentExercise.imageUrl" class="w-full max-w-md mx-auto border-2 border-foreground rounded-md mb-4" alt="" />

          <div v-if="currentExercise.exerciseType === 'LISTENING' && currentExercise.audioUrl" class="mb-6 bg-secondary/10 border-2 border-foreground p-4 rounded-md">
            <p class="font-bold text-xs uppercase tracking-wider text-secondary mb-3">Nghe & trả lời</p>
            <audio :src="currentExercise.audioUrl" controls class="w-full"></audio>
          </div>
          <div v-else-if="currentExercise.exerciseType === 'LISTENING' && isSpeechAvailable()" class="mb-6 bg-secondary/10 border-2 border-foreground p-4 rounded-md flex items-center gap-3">
            <p class="font-bold text-xs uppercase tracking-wider text-secondary flex-1">Nghe & trả lời (giọng đọc máy)</p>
            <AppButton variant="secondary" :aria-label="'Phát âm câu nghe số ' + (currentIndex + 1)" @click="speakListening(currentExercise)">
              🔊 Nghe
            </AppButton>
          </div>

          <!-- MATCHING: dedicated component -->
          <MatchingExercise v-if="currentExercise.exerciseType === 'MATCHING'"
            :exercise="currentExercise"
            @answer="onMatchingAnswer"
            @reveal="onMatchingReveal"
          />

          <template v-else>
            <div class="geo-markdown mb-5" v-html="parseMarkdown(currentExercise.question)"></div>

            <!-- Options (multiple choice, listening) -->
            <div v-if="hasOptionChoices(currentExercise)" class="space-y-3">
              <button v-for="(opt, idx) in parsedOptionsFor(currentExercise)" :key="idx"
                @click="selectOption(idx)"
                class="w-full text-left p-4 border-2 font-medium transition-all rounded-md"
                :class="getOptionClass(idx)">
                {{ opt }}
              </button>
            </div>

            <!-- Text input (fill blank, translation) -->
            <input v-else v-model="userAnswer" type="text"
              :placeholder="getInputPlaceholder(currentExercise)"
              class="w-full border-2 border-foreground p-4 text-lg font-bold focus:outline-none focus:ring-4 focus:ring-tertiary transition-all rounded-md shadow-pop-sm" />
          </template>
        </div>

        <div class="px-6 pb-6 flex gap-3">
          <AppButton v-if="currentIndex > 0" @click="goTo(currentIndex - 1)" variant="secondary">
            Trước
          </AppButton>
          <AppButton @click="submitAll" :disabled="submitting" variant="primary" class="flex-1">
            {{ submitting ? 'Đang nộp...' : `Nộp tất cả (${exercises.length} câu)` }}
          </AppButton>
          <AppButton v-if="currentIndex < exercises.length - 1" @click="goTo(currentIndex + 1)" variant="pink">
            Tiếp
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { isSpeechAvailable, blankOutForSpeech, stripLeadingNumber, speakEnglish } from '@/utils/speech'
import lessonService from '@/services/lessonService'
import MatchingExercise from '@/components/lessons/MatchingExercise.vue'
import AppButton from '@/components/ui/AppButton.vue'

const route = useRoute()
const lessonId = Number(route.params.id)

const exercises = ref([])
const loading = ref(true)
const currentIndex = ref(0)
const selectedOption = ref(null)
const userAnswer = ref('')
const answersMap = ref({})
const showResults = ref(false)
const results = ref({})
const submitting = ref(false)

const currentExercise = computed(() => exercises.value[currentIndex.value] || {})

function typeLabel(type) {
  const map = { MULTIPLE_CHOICE: 'Trắc nghiệm', FILL_BLANK: 'Điền từ', LISTENING: 'Nghe', MATCHING: 'Nối từ', TRANSLATION: 'Dịch' }
  return map[type] || type
}

// LISTENING không có file audio → đọc bằng giọng máy, che chỗ trống để không lộ đáp án
function speakListening(ex) {
  return speakEnglish(blankOutForSpeech(stripLeadingNumber(ex.question)))
}

function parseOpts(ex) {
  const opts = ex.options
  if (!opts) return null
  if (Array.isArray(opts)) return opts
  try { return JSON.parse(opts) }
  catch { return null }
}

function parsedOptionsFor(ex) {
  return parseOpts(ex)
}

function hasOptionChoices(ex) {
  const choiceTypes = ['MULTIPLE_CHOICE', 'LISTENING']
  if (!choiceTypes.includes(ex.exerciseType)) return false
  const opts = parseOpts(ex)
  return Array.isArray(opts) && opts.length > 0
}

function getInputPlaceholder(ex) {
  const placeholders = { FILL_BLANK: 'Điền vào chỗ trống...', TRANSLATION: 'Nhập bản dịch...' }
  return placeholders[ex.exerciseType] || 'Nhập câu trả lời...'
}

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}

function selectOption(idx) {
  selectedOption.value = idx
  const opts = parseOpts(currentExercise.value)
  if (opts && opts[idx]) {
    answersMap.value[currentExercise.value.id] = opts[idx]
  }
}

function getOptionClass(idx) {
  const opts = parseOpts(currentExercise.value)
  if (!opts) return 'border-foreground hover:bg-tertiary/10'
  const selected = selectedOption.value === idx
  return selected ? 'border-accent bg-accent/10' : 'border-foreground hover:bg-tertiary/10'
}

function goTo(idx) {
  currentIndex.value = idx
  selectedOption.value = null
  userAnswer.value = ''
}

function onMatchingAnswer(answer) {
  answersMap.value[currentExercise.value.id] = answer
}

function onMatchingReveal(answer) {
  // Answer already stored in answersMap
}

async function submitAll() {
  submitting.value = true
  try {
    const answers = exercises.value.map(ex => ({
      exerciseId: ex.id,
      userAnswer: answersMap.value[ex.id] || ''
    }))
    const response = await lessonService.gradeExercises(lessonId, answers)
    results.value = response
    showResults.value = true
  } catch (e) {
    console.error('Grade failed:', e)
  } finally {
    submitting.value = false
  }
}

function resetQuiz() {
  currentIndex.value = 0
  selectedOption.value = null
  userAnswer.value = ''
  answersMap.value = {}
  showResults.value = false
  results.value = {}
}

onMounted(async () => {
  try {
    const data = await lessonService.getExercises(lessonId)
    exercises.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('Failed to load exercises:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
audio { border-radius: 8px; }
</style>

