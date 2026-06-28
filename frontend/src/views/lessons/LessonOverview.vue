<template>
  <div class="space-y-8" v-if="lesson">
    <template v-if="!showExercises">
      <!-- Video -->
      <div v-if="videoHtml" class="bg-white border-4 border-foreground shadow-hard-lg">
        <div class="bg-primary-red text-white px-5 py-2 flex items-center gap-2 border-b-4 border-foreground">
          <span class="w-3 h-3 bg-white rotate-45"></span>
          <span class="font-bold text-xs uppercase tracking-wider">Video</span>
        </div>
        <div class="p-4" v-html="videoHtml"></div>
      </div>

      <!-- Content -->
      <div v-if="staticHtml" class="bg-white border-4 border-foreground shadow-hard-lg">
        <div class="bg-foreground text-white px-6 py-4 border-b-4 border-foreground">
          <h3 class="font-black text-xl uppercase tracking-tight">Bài học</h3>
        </div>
        <div class="px-6 py-6 skill-html" v-html="staticHtml"></div>
      </div>

      <div v-else-if="lesson.content" class="bg-white border-4 border-foreground shadow-hard-lg">
        <div class="bg-foreground text-white px-6 py-4 border-b-4 border-foreground">
          <h3 class="font-black text-xl uppercase tracking-tight">Bài học</h3>
        </div>
        <div class="px-6 py-6 skill-html" v-html="lesson.content"></div>
      </div>
      <div v-else class="bg-white border-4 border-foreground shadow-hard-lg p-16 text-center">
        <p class="font-black text-xl uppercase text-gray-400">Không có nội dung</p>
      </div>
    </template>

    <!-- Exercises -->
    <div v-if="showExercises" id="lesson-exercises">
      <div class="flex items-center gap-3 mb-5">
        <div class="h-px flex-1 bg-foreground/20"></div>
        <span class="font-black text-sm uppercase tracking-widest text-gray-500">Luyện tập</span>
        <div class="h-px flex-1 bg-foreground/20"></div>
      </div>
      
      <!-- Generated Quiz from content -->
      <QuizEngine v-if="questions.length" :questions="questions" :lesson-id="lesson.id" skill-code="quiz" class="mb-8" />

      <!-- Database Exercises -->
      <div v-if="lesson.exercises && lesson.exercises.length" class="space-y-6">
        <div v-for="(ex, idx) in lesson.exercises" :key="ex.id" class="bg-white border-4 border-foreground shadow-hard-lg p-6 transition-all duration-300 hover:shadow-hard-xl">
          <div class="flex justify-between items-center mb-4 border-b-2 border-foreground pb-4">
            <span class="font-black text-lg uppercase tracking-tighter text-foreground">Câu {{ questions.length ? questions.length + idx + 1 : idx + 1 }}</span>
            <span class="bg-primary-yellow text-foreground font-black text-xs uppercase tracking-wider px-3 py-1 border-2 border-foreground">+{{ ex.points }} điểm</span>
          </div>

          <div class="bauhaus-markdown mb-5 text-lg font-bold" v-html="parseMarkdown(ex.question)"></div>

          <!-- Audio Player -->
          <div v-if="ex.audioUrl" class="mb-5">
            <audio controls class="w-full border-2 border-foreground bg-black/5 bauhaus-audio">
              <source :src="ex.audioUrl" type="audio/mpeg">
              Trình duyệt của bạn không hỗ trợ thẻ audio.
            </audio>
          </div>

          <!-- Multiple Choice Input -->
          <div v-if="ex.exerciseType === 'MULTIPLE_CHOICE'" class="space-y-3 mb-5">
            <div v-for="(option, optIdx) in parseOptions(ex.options)" :key="optIdx"
                 class="border-4 border-foreground p-4 cursor-pointer transition-all duration-200 hover:bg-yellow-50 hover:-translate-y-1 hover:shadow-hard-sm"
                 :class="optionClass(ex.id, optIdx, ex.correctAnswer)">
              <label class="flex items-center gap-3 font-sans font-bold text-foreground cursor-pointer w-full" :for="`opt-${ex.id}-${optIdx}`">
                <input class="bauhaus-radio" type="radio"
                       :id="`opt-${ex.id}-${optIdx}`" :name="`ex-${ex.id}`" :value="String(optIdx)"
                       v-model="answers[ex.id]" :disabled="results[ex.id] !== undefined" />
                {{ option }}
              </label>
            </div>
          </div>

          <!-- Fill In Blank Input -->
          <div v-else-if="ex.exerciseType === 'FILL_IN_BLANK'" class="mb-5">
            <input v-model="answers[ex.id]" type="text" class="w-full border-4 border-foreground bg-white p-4 font-bold text-lg text-foreground focus:outline-none focus:ring-4 focus:ring-primary-yellow transition-all"
                   placeholder="Nhập câu trả lời tại đây..." :disabled="results[ex.id] !== undefined" />
          </div>

          <!-- Submit Button & Feedback -->
          <div>
            <button v-if="results[ex.id] === undefined"
                    class="inline-flex items-center justify-center font-black uppercase tracking-wider border-4 border-foreground px-8 py-3 text-lg bg-primary-red text-white shadow-hard-md hover:bg-primary-red/90 active:translate-x-1 active:translate-y-1 active:shadow-none transition-all duration-200 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                    :disabled="!answers[ex.id]" @click="submitAnswer(ex.id)">
              Nộp câu trả lời
            </button>
            <div v-else class="w-full space-y-4 animate-fade-in-up">
              <div class="flex items-center gap-3 p-4 border-4" :class="results[ex.id] ? 'bg-green-100 border-green-500 text-green-700' : 'bg-red-100 border-red-500 text-red-700'">
                <span class="text-3xl">{{ results[ex.id] ? '✅' : '❌' }}</span>
                <span class="font-black text-xl uppercase tracking-wider">
                  {{ results[ex.id] ? 'Chính xác! Tuyệt vời!' : `Sai rồi! Đáp án đúng: ${displayCorrectAnswer(ex)}` }}
                </span>
              </div>
              <div v-if="ex.explanation" class="bg-gray-100 border-4 border-foreground p-6">
                <span class="inline-block bg-primary-blue text-white px-3 py-1 font-black text-xs uppercase tracking-widest mb-3 border-2 border-foreground">Giải thích chi tiết</span>
                <div class="bauhaus-markdown text-foreground font-medium" v-html="parseMarkdown(ex.explanation)"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { parseQuizContent } from '@/utils/skillParser'
import QuizEngine from '@/components/lessons/QuizEngine.vue'
import { useToast } from '@/composables/useToast'
import { useExerciseStore } from '@/store/modules/exercise'
import { parseMarkdown } from '@/utils/markdown'

const props = defineProps({
  lesson: { type: Object, required: true },
  skills: { type: Array, required: true },
  showExercises: { type: Boolean, default: false },
})

const exerciseStore = useExerciseStore()
const toast = useToast()

const answers = ref({})
const results = ref({})

const html = computed(() => props.lesson?.content || '')
const parsed = computed(() => html.value ? parseQuizContent(html.value) : { staticHtml: '', videoHtml: '', questions: [] })
const staticHtml = computed(() => parsed.value.staticHtml)
const videoHtml = computed(() => parsed.value.videoHtml)
const questions = computed(() => parsed.value.questions)

function parseOptions(optionsStr) {
  if (!optionsStr) return []
  try { return JSON.parse(optionsStr) } catch (e) { return [] }
}

function optionClass(exId, optIdx, correctAns) {
  const isSubmitted = results.value[exId] !== undefined
  const userAns = answers.value[exId]

  if (!isSubmitted) return ''
  if (String(optIdx) === correctAns) return 'bg-green-300 border-green-600 text-green-900 shadow-none translate-x-1 translate-y-1'
  if (String(optIdx) === userAns && userAns !== correctAns) return 'bg-red-300 border-red-600 text-red-900 shadow-none translate-x-1 translate-y-1'
  return ''
}

function displayCorrectAnswer(ex) {
  if (ex.exerciseType === 'MULTIPLE_CHOICE') {
    const opts = parseOptions(ex.options)
    try {
      const idx = parseInt(ex.correctAnswer)
      return opts[idx]
    } catch (e) {
      return ex.correctAnswer
    }
  }
  return ex.correctAnswer
}

async function submitAnswer(exerciseId) {
  const userAns = answers.value[exerciseId]
  if (!userAns) return

  try {
    const res = await exerciseStore.submitExerciseAnswer({
      exerciseId: exerciseId,
      userAnswer: userAns
    })
    results.value[exerciseId] = res.isCorrect
  } catch (e) {
    toast.error(e.response?.data?.error || 'Có lỗi xảy ra khi nộp bài. Vui lòng thử lại.')
  }
}
</script>

<style scoped>
input.bauhaus-radio {
  appearance: none;
  -webkit-appearance: none;
  width: 24px;
  height: 24px;
  border: 4px solid #121212;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  margin: 0;
  background: white;
  transition: all 0.2s;
}

input.bauhaus-radio:checked {
  background-color: #D02020;
  box-shadow: inset 0 0 0 4px white;
}

audio.bauhaus-audio,
audio[controls] {
  filter: grayscale(1) contrast(1.5);
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in-up {
  animation: fadeInUp 0.4s ease-out forwards;
}
</style>
