<template>
  <div>
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-16">
      <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
    </div>

    <div v-else-if="exercises.length === 0" class="bg-card border-2 border-foreground shadow-pop-lg p-12 text-center rounded-md">
      <p class="font-black text-xl uppercase">Chưa có bài tập</p>
    </div>

    <div v-else>
      <!-- Progress -->
      <div class="flex items-center gap-4 mb-6">
        <span class="font-bold text-sm uppercase bg-foreground text-white px-4 py-2 rounded-full">{{ currentIndex + 1 }} / {{ exercises.length }}</span>
        <span class="px-3 py-1 bg-tertiary text-foreground font-bold text-xs uppercase tracking-wider rounded-full border-2 border-foreground">{{ currentExercise.exerciseType }}</span>
        <span v-if="currentExercise.difficulty === 'EASY'" class="text-quaternary font-bold text-xs uppercase">Dễ</span>
        <span v-else-if="currentExercise.difficulty === 'MEDIUM'" class="text-tertiary font-bold text-xs uppercase">TB</span>
        <span v-else class="text-accent font-bold text-xs uppercase">Khó</span>
      </div>

      <!-- Exercise Card -->
      <div class="bg-card border-2 border-foreground shadow-pop-lg rounded-md overflow-hidden">
        <div class="p-6">
          <!-- Image -->
          <img v-if="currentExercise.imageUrl" :src="currentExercise.imageUrl" class="w-full max-w-md mx-auto border-2 border-foreground rounded-md mb-4" alt="Exercise image" />

          <!-- Audio -->
          <div v-if="currentExercise.exerciseType === 'LISTENING' && currentExercise.audioUrl" class="mb-6 bg-secondary/10 border-2 border-foreground p-4 rounded-md">
            <p class="font-bold text-xs uppercase tracking-wider text-secondary mb-3">Nghe & trả lời</p>
            <audio :src="currentExercise.audioUrl" controls class="w-full geo-audio"></audio>
          </div>

          <!-- Question -->
          <div class="geo-markdown mb-5" v-html="parseMarkdown(currentExercise.question)"></div>

          <!-- Options -->
          <div v-if="currentExercise.options" class="space-y-3">
            <button v-for="(opt, idx) in currentExercise.options" :key="idx"
              @click="selectOption(idx)"
              class="w-full text-left p-4 border-2 font-medium transition-all rounded-md"
              :class="getOptionClass(idx)"
            >
              {{ opt }}
            </button>
          </div>

          <!-- Text input -->
          <input v-else v-model="userAnswer" type="text" placeholder="Nhập câu trả lời..."
            class="w-full border-2 border-foreground p-4 text-lg font-bold focus:outline-none focus:ring-4 focus:ring-tertiary transition-all rounded-md shadow-pop-sm"
          />
        </div>

        <div class="px-6 pb-6">
          <button @click="submitAnswer" :disabled="answered"
            class="px-8 py-4 bg-accent text-white font-black text-sm tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >{{ answered ? 'Đã trả lời' : 'Kiểm tra' }}</button>
        </div>

        <!-- Feedback -->
        <div v-if="answered && feedbackMessage"
          class="p-6 border-t-2 border-foreground font-bold text-lg"
          :class="isCorrect ? 'bg-quaternary/10 border-quaternary text-quaternary' : 'bg-accent/10 border-accent text-accent'"
        >{{ feedbackMessage }}</div>

        <!-- Explanation -->
        <div v-if="answered && currentExercise.explanation" class="p-4 bg-muted border-t-2 border-foreground text-sm rounded-md">
          <div class="geo-markdown" v-html="parseMarkdown(currentExercise.explanation)"></div>
        </div>

        <!-- Next -->
        <div v-if="answered" class="border-t-2 border-foreground p-4 text-center">
          <button @click="nextExercise" v-if="currentIndex < exercises.length - 1"
            class="px-8 py-3 bg-secondary text-white font-black text-sm tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover active:shadow-pop-active transition-all"
          >Câu tiếp theo →</button>
          <p v-else class="font-black text-sm uppercase text-quaternary">Hoàn thành!</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({ exercises: { type: Array, default: () => [] } })
const currentIndex = ref(0)
const selectedOption = ref(null)
const userAnswer = ref('')
const answered = ref(false)
const isCorrect = ref(false)
const feedbackMessage = ref('')

const currentExercise = computed(() => props.exercises[currentIndex.value])

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}

function selectOption(idx) {
  if (answered.value) return
  selectedOption.value = idx
}

function getOptionClass(idx) {
  if (!answered.value) return selectedOption.value === idx ? 'border-accent bg-accent/10' : 'border-foreground hover:bg-tertiary/10'
  if (idx === currentExercise.value.correctIndex) return 'border-quaternary bg-quaternary/10 text-quaternary'
  if (idx === selectedOption.value) return 'border-accent bg-accent/10 text-accent'
  return 'opacity-50 border-foreground'
}

function submitAnswer() {
  if (answered.value) return
  answered.value = true
  if (currentExercise.value.options) {
    isCorrect.value = selectedOption.value === currentExercise.value.correctIndex
  } else {
    isCorrect.value = userAnswer.value.trim().toLowerCase() === (currentExercise.value.correctAnswer || '').toLowerCase()
  }
  feedbackMessage.value = isCorrect.value ? 'Chính xác!' : 'Sai rồi!'
}

function nextExercise() {
  if (currentIndex.value < props.exercises.length - 1) {
    currentIndex.value++
    selectedOption.value = null
    userAnswer.value = ''
    answered.value = false
    isCorrect.value = false
    feedbackMessage.value = ''
  }
}
</script>

<style scoped>
audio.geo-audio { border-radius: 8px; }
</style>
