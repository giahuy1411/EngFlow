<template>
  <div class="space-y-8">
    <!-- LESSON CONTENT -->
    <div v-if="!loading && lesson?.content">
      <div class="lesson-html prose prose-lg max-w-none"
        v-html="sanitizeHtml(lesson.content)"
      ></div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-16">
      <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
    </div>

    <!-- Empty lesson -->
    <div v-if="!loading && !lesson" class="text-center py-16">
      <p class="text-xl font-bold text-muted-foreground">Bài học này chưa có nội dung.</p>
    </div>

    <!-- === EXERCISE CARDS === -->
    <div v-if="exercises.length > 0" class="mt-12 pt-8 border-t-2 border-gray-300">
      <h2 class="font-black text-2xl uppercase tracking-tight mb-2">Bài tập</h2>
      <p class="text-muted-foreground font-medium mb-8">Trả lời từng câu và kiểm tra đáp án.</p>

      <div v-for="(ex, idx) in exercises" :key="ex.id"
        class="bg-card border-2 border-foreground shadow-pop-lg rounded-md overflow-hidden mb-6 transition-all"
        :class="cardStates[ex.id]?.revealed ? (cardStates[ex.id]?.isCorrect ? 'ring-2 ring-quaternary' : 'ring-2 ring-accent') : ''">

        <!-- Card header -->
        <div class="flex items-center gap-3 px-6 pt-5 pb-2">
          <span class="font-black text-sm uppercase bg-foreground text-white px-3 py-1 rounded-full">{{ idx + 1 }}</span>
          <span class="px-3 py-1 bg-tertiary text-foreground font-bold text-xs uppercase tracking-wider rounded-full border-2 border-foreground">{{ ex.exerciseType }}</span>
          <span v-if="ex.difficulty === 'EASY'" class="text-quaternary font-bold text-xs uppercase">Dễ</span>
          <span v-else-if="ex.difficulty === 'MEDIUM'" class="text-tertiary font-bold text-xs uppercase">TB</span>
          <span v-else-if="ex.difficulty === 'HARD'" class="text-accent font-bold text-xs uppercase">Khó</span>
        </div>

        <div class="p-6 pt-3">
          <!-- Question -->
          <div class="geo-markdown mb-5" v-html="parseMarkdown(ex.question)"></div>

          <!-- Options (multiple choice) -->
          <div v-if="ex.options" class="space-y-3">
            <button v-for="(opt, oi) in parsedOptions(ex)" :key="oi"
              @click="selectAnswer(ex.id, opt, 'options')"
              class="w-full text-left p-4 border-2 font-medium transition-all rounded-md"
              :class="getCardOptionClass(ex.id, opt)">
              {{ opt }}
            </button>
          </div>

          <!-- Text input -->
          <input v-else v-model="textAnswers[ex.id]" type="text" placeholder="Nhập câu trả lời..."
            :disabled="cardStates[ex.id]?.revealed"
            class="w-full border-2 border-foreground p-4 text-lg font-bold focus:outline-none focus:ring-4 focus:ring-tertiary transition-all rounded-md shadow-pop-sm" />

          <!-- Result badge (after check) -->
          <div v-if="cardStates[ex.id]?.revealed" class="mt-4 p-4 rounded-md border-2"
            :class="cardStates[ex.id]?.isCorrect ? 'bg-quaternary/10 border-quaternary' : 'bg-accent/10 border-accent'">
            <div class="flex items-center gap-2 font-black text-sm uppercase mb-1">
              <span>{{ cardStates[ex.id]?.isCorrect ? '✅ Đúng' : '❌ Sai' }}</span>
            </div>
            <p class="font-bold text-sm text-foreground/70">Đáp án: <span class="text-quaternary">{{ ex.correctAnswer }}</span></p>
            <p v-if="ex.explanation" class="mt-2 text-sm text-foreground/60 italic">{{ ex.explanation }}</p>
          </div>

          <!-- Check button -->
          <button v-if="!cardStates[ex.id]?.revealed" @click="checkAnswer(ex)"
            class="mt-4 px-6 py-3 bg-secondary text-white font-black text-sm tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover active:shadow-pop-active transition-all"
            :disabled="!getUserAnswer(ex.id)">
            Kiểm tra
          </button>
        </div>
      </div>

      <!-- Submit button -->
      <button @click="submitAll"
        class="w-full px-8 py-4 bg-accent text-white font-black text-sm tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all duration-200"
        :disabled="submitting">
        {{ submitting ? 'Đang nộp...' : `Nộp bài (${exercises.length} câu)` }}
      </button>

      <!-- Submit success -->
      <div v-if="submitted" class="mt-4 p-4 bg-quaternary/10 border-2 border-quaternary rounded-md text-center">
        <p class="font-black text-sm uppercase">✅ Đã lưu kết quả! Xem tab Lịch sử.</p>
      </div>
    </div>

    <!-- No exercises -->
    <div v-if="!loading && loaded && exercises.length === 0 && lesson" class="text-center py-8">
      <p class="font-bold text-muted-foreground">Bài học này chưa có bài tập.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import lessonService from '@/services/lessonService'

const route = useRoute()
const lessonId = Number(route.params.id)

const lesson = ref(null)
const exercises = ref([])
const loading = ref(true)
const loaded = ref(false)
const textAnswers = ref({})
const optionAnswers = ref({})
const cardStates = ref({})  // { [exId]: { revealed, isCorrect } }
const submitting = ref(false)
const submitted = ref(false)

function sanitizeHtml(html) {
  if (!html) return ''
  return DOMPurify.sanitize(html, {
    ADD_TAGS: ['iframe'],
    ADD_ATTR: ['allow', 'allowfullscreen', 'frameborder', 'scrolling', 'target']
  })
}

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}

function parsedOptions(ex) {
  if (!ex.options) return null
  if (Array.isArray(ex.options)) return ex.options
  try { return JSON.parse(ex.options) }
  catch { return null }
}

function getUserAnswer(exId) {
  return textAnswers.value[exId] || optionAnswers.value[exId] || ''
}

function selectAnswer(exId, answer, type) {
  if (cardStates.value[exId]?.revealed) return
  if (type === 'options') {
    optionAnswers.value[exId] = answer
    textAnswers.value[exId] = ''
  }
}

function getCardOptionClass(exId, opt) {
  const selected = optionAnswers.value[exId] === opt
  const state = cardStates.value[exId]
  if (!state?.revealed) {
    return selected ? 'border-accent bg-accent/10' : 'border-foreground hover:bg-tertiary/10'
  }
  const isCorrectAnswer = opt.trim().toLowerCase() === exercises.value.find(e => e.id === exId)?.correctAnswer?.trim().toLowerCase()
  if (isCorrectAnswer) return 'border-quaternary bg-quaternary/10'
  if (selected && !state.isCorrect) return 'border-accent bg-accent/10'
  return 'border-gray-300 opacity-60'
}

function checkAnswer(ex) {
  const userAnswer = getUserAnswer(ex.id)
  if (!userAnswer) return
  const normalizedUser = userAnswer.trim().toLowerCase().replace(/\s+/g, ' ')
  const normalizedCorrect = (ex.correctAnswer || '').trim().toLowerCase().replace(/\s+/g, ' ')
  const isCorrect = normalizedUser === normalizedCorrect
  cardStates.value[ex.id] = { revealed: true, isCorrect }
}

async function submitAll() {
  submitting.value = true
  submitted.value = false
  try {
    const answers = exercises.value.map(ex => ({
      exerciseId: ex.id,
      userAnswer: getUserAnswer(ex.id)
    }))
    await lessonService.submitExercises(lessonId, answers)
    submitted.value = true
  } catch (e) {
    console.error('Submit failed:', e)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const [lessonData, exerciseData] = await Promise.all([
      lessonService.getById(lessonId),
      lessonService.getExercisesWithAnswers(lessonId)
    ])
    lesson.value = lessonData
    exercises.value = Array.isArray(exerciseData) ? exerciseData : []
    // Pre-initialize reactive objects for Vue 3 reactivity
    for (const ex of exercises.value) {
      cardStates.value[ex.id] = { revealed: false, isCorrect: false }
      textAnswers.value[ex.id] = ''
    }
  } catch (e) {
    console.error('Failed to load:', e)
  } finally {
    loading.value = false
    loaded.value = true
  }
})
</script>

<style scoped>
.lesson-html :deep(h1),
.lesson-html :deep(h2),
.lesson-html :deep(h3) {
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: -0.02em;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
}
.lesson-html :deep(p) {
  margin-bottom: 1rem;
  line-height: 1.7;
}
.lesson-html :deep(img) {
  max-width: 100%;
  border-radius: 0.5rem;
  border: 2px solid #1E293B;
  margin: 1.5rem auto;
  height: auto;
}
.lesson-html :deep(skill-html) {
  max-width: 100%;
  overflow-x: auto;
}
.lesson-html :deep(.entry-content) {
  line-height: 1.8;
}
.lesson-html :deep(.et_post_meta_wrapper) {
  margin-bottom: 1.5rem;
}
.lesson-html :deep(ins.adsbygoogle) {
  display: none !important;
}
.lesson-html :deep(script) {
  display: none !important;
}
.lesson-html :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1rem 0;
  border: 2px solid #1E293B;
}
.lesson-html :deep(td),
.lesson-html :deep(th) {
  border: 1px solid #1E293B;
  padding: 0.5rem;
  font-weight: 500;
}
.lesson-html :deep(th) {
  background: rgba(139, 92, 246, 0.1);
  font-weight: 900;
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 0.05em;
}
.lesson-html :deep(blockquote) {
  border-left: 4px solid #8B5CF6;
  padding-left: 1rem;
  margin: 1rem 0;
  font-style: italic;
  opacity: 0.8;
}
.lesson-html :deep(ul),
.lesson-html :deep(ol) {
  padding-left: 1.5rem;
  margin-bottom: 1rem;
}
.lesson-html :deep(li) {
  margin-bottom: 0.25rem;
  line-height: 1.6;
}
.lesson-html :deep(*) {
  color: inherit;
}
.lesson-html :deep(a) {
  color: #8B5CF6;
  text-decoration: underline;
}
.lesson-html :deep(.et_pb_text_inner) {
  max-width: 100%;
}
.lesson-html :deep(.entry-title) {
  font-size: 1.5rem;
  margin-bottom: 1rem;
}
</style>
