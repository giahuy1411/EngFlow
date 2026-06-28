<template>
  <div class="space-y-8">
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin inline-block w-10 h-10 border-4 border-black border-t-transparent rounded-full"></div>
      <p class="font-bold uppercase mt-4 tracking-wider">Đang tải bài tập...</p>
    </div>

    <div v-else-if="exercises.length === 0" class="bg-white border-4 border-black shadow-hard-lg p-12 text-center">
      <p class="font-black text-xl uppercase">Chưa có bài tập cho bài học này</p>
      <p class="font-bold text-xs uppercase tracking-wider text-foreground/60 mt-2">Admin có thể thêm bài tập ở trang quản trị</p>
    </div>

    <template v-else>
      <div class="flex items-center justify-between mb-6">
        <h3 class="font-black text-2xl uppercase">Bài tập tương tác</h3>
        <span class="font-bold text-sm uppercase bg-black text-white px-4 py-2">{{ currentIndex + 1 }} / {{ exercises.length }}</span>
      </div>

      <div class="bg-white border-4 border-black shadow-hard-lg">
        <div class="bg-black text-white px-6 py-3 flex items-center gap-3 border-b-4 border-black">
          <span class="px-3 py-1 bg-yellow-400 text-black font-bold text-xs uppercase tracking-wider">{{ currentExercise.exerciseType }}</span>
          <span class="font-medium text-sm">{{ currentExercise.title }}</span>
          <span class="ml-auto font-bold text-xs uppercase tracking-wider flex items-center gap-2">
            <span v-if="currentExercise.difficulty === 'EASY'" class="text-green-400">Dễ</span>
            <span v-else-if="currentExercise.difficulty === 'MEDIUM'" class="text-yellow-400">TB</span>
            <span v-else class="text-red-400">Khó</span>
          </span>
        </div>

        <div class="p-6 sm:p-8">
          <div v-if="currentExercise.imageUrl" class="mb-6">
            <img :src="currentExercise.imageUrl" class="w-full max-w-md mx-auto border-4 border-black" alt="Exercise image" />
          </div>

          <div v-if="currentExercise.exerciseType === 'LISTENING' && currentExercise.audioUrl" class="mb-6 bg-primary-blue/5 border-2 border-black p-4">
            <p class="font-bold uppercase text-xs tracking-wider mb-3 text-primary-blue">Nghe và trả lời</p>
            <audio :src="currentExercise.audioUrl" controls class="w-full"></audio>
          </div>

          <p class="font-bold text-lg mb-6">{{ currentExercise.question }}</p>

          <div v-if="submitResult === null">
            <div v-if="currentExercise.exerciseType === 'MULTIPLE_CHOICE' && optionsList.length > 0" class="space-y-3">
              <button v-for="(opt, idx) in optionsList" :key="idx"
                      @click="selectedAnswer = String(idx)"
                      class="w-full text-left p-4 border-2 font-medium transition-all"
                      :class="selectedAnswer === String(idx)
                        ? 'border-primary-blue bg-primary-blue/10 shadow-hard-sm'
                        : 'border-black hover:bg-background hover:shadow-hard-sm'">
                {{ String.fromCharCode(65 + idx) }}. {{ opt }}
              </button>
            </div>

            <div v-else>
              <input v-model="textAnswer" type="text"
                     placeholder="Nhập câu trả lời..."
                     class="w-full border-4 border-black p-4 text-lg font-bold focus:outline-none focus:ring-4 focus:ring-yellow-400 transition-all" />
            </div>

            <button @click="submitAnswer"
                    :disabled="!canSubmit"
                    class="mt-6 px-8 py-4 bg-black text-white font-black uppercase text-sm tracking-wider border-4 border-black
                           shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] hover:-translate-y-1 hover:shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]
                           active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all duration-200
                           disabled:opacity-30 disabled:cursor-not-allowed">
              KIỂM TRA
            </button>
          </div>

          <div v-else class="space-y-6">
            <div class="p-6 border-4 font-bold text-lg"
                 :class="submitResult
                   ? 'bg-primary-blue/10 border-primary-blue text-primary-blue'
                   : 'bg-primary-red/10 border-primary-red text-primary-red'">
              <p class="uppercase tracking-wider text-sm mb-2">{{ submitResult ? 'Chính xác!' : 'Sai rồi!' }}</p>
              <p v-if="!submitResult && currentExercise.exerciseType === 'MULTIPLE_CHOICE' && optionsList.length > parseInt(currentExercise.correctAnswer)">
                Đáp án đúng: <span class="underline">{{ String.fromCharCode(65 + parseInt(currentExercise.correctAnswer)) }}. {{ optionsList[parseInt(currentExercise.correctAnswer)] }}</span>
              </p>
              <p v-if="!submitResult && currentExercise.exerciseType !== 'MULTIPLE_CHOICE'">
                Đáp án đúng: <span class="underline">{{ currentExercise.correctAnswer }}</span>
              </p>
            </div>
            <div v-if="currentExercise.explanation" class="p-4 bg-background border-2 border-black text-sm">
              <p class="font-bold uppercase text-xs tracking-wider mb-1">Giải thích:</p>
              <p>{{ currentExercise.explanation }}</p>
            </div>

            <button @click="nextExercise"
                    class="px-8 py-4 bg-black text-white font-black uppercase text-sm tracking-wider border-4 border-black
                           shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] hover:-translate-y-1 hover:shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]
                           active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all duration-200">
              {{ currentIndex < exercises.length - 1 ? 'Câu tiếp theo →' : 'Hoàn thành' }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import lessonService from '@/services/lessonService'
import exerciseService from '@/services/exerciseService'

const props = defineProps({
  lesson: { type: Object, required: true },
})

const exercises = ref([])
const loading = ref(true)
const currentIndex = ref(0)
const selectedAnswer = ref(null)
const textAnswer = ref('')
const submitResult = ref(null)
const submitting = ref(false)

const currentExercise = computed(() => exercises.value[currentIndex.value] || {})
const optionsList = computed(() => {
  try {
    const opts = currentExercise.value.options
    if (!opts) return []
    return JSON.parse(opts)
  } catch { return [] }
})
const canSubmit = computed(() => {
  if (submitResult.value !== null) return false
  if (currentExercise.value.exerciseType === 'MULTIPLE_CHOICE') return selectedAnswer.value !== null
  return textAnswer.value.trim().length > 0
})

watch(() => props.lesson?.id, async (id) => {
  if (!id) return
  loading.value = true
  try {
    exercises.value = await lessonService.getExercises(id)
    currentIndex.value = 0
    resetForm()
  } catch { exercises.value = [] }
  finally { loading.value = false }
}, { immediate: true })

function resetForm() {
  selectedAnswer.value = null
  textAnswer.value = ''
  submitResult.value = null
  submitting.value = false
}

async function submitAnswer() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    const answer = currentExercise.value.exerciseType === 'MULTIPLE_CHOICE' ? selectedAnswer.value : textAnswer.value.trim()
    const res = await exerciseService.submit({ exerciseId: currentExercise.value.id, userAnswer: answer })
    submitResult.value = res.isCorrect
  } catch {
    // If submission fails (e.g. exercise not configured for scoring), do local comparison
    if (currentExercise.value.exerciseType === 'MULTIPLE_CHOICE') {
      submitResult.value = selectedAnswer.value === currentExercise.value.correctAnswer
    } else {
      submitResult.value = textAnswer.value.trim().toLowerCase() === currentExercise.value.correctAnswer.toLowerCase()
    }
  }
  finally { submitting.value = false }
}

function nextExercise() {
  if (currentIndex.value < exercises.value.length - 1) {
    currentIndex.value++
    resetForm()
  }
}
</script>
