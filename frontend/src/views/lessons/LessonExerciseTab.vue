<template>
  <div class="space-y-6">
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-16">
      <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
    </div>

    <!-- No exercises -->
    <div v-else-if="exercises.length === 0" class="bg-card border-2 border-foreground shadow-pop-lg p-12 text-center rounded-md">
      <p class="font-black text-xl uppercase">Chưa có bài tập</p>
      <p class="text-muted-foreground font-medium mt-2">Bài học này chưa có bài tập để làm.</p>
    </div>

    <div v-else>
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tight">Bài tập</h2>
          <p class="text-muted-foreground font-medium">Trả lời từng câu và kiểm tra đáp án.</p>
        </div>
        <span class="font-bold text-sm bg-foreground text-white px-4 py-2 rounded-full">{{ exercises.length }} câu</span>
      </div>

      <!-- Cards -->
      <div v-for="(ex, idx) in exercises" :key="ex.id"
        class="bg-card border-2 border-foreground shadow-pop-lg rounded-md overflow-hidden mb-5 transition-all"
        :class="cardStates[ex.id]?.revealed ? (cardStates[ex.id]?.isCorrect ? 'ring-2 ring-quaternary' : 'ring-2 ring-accent') : ''">

        <!-- Card header -->
        <div class="flex items-center gap-3 px-6 pt-5 pb-2">
          <span class="font-black text-sm uppercase bg-foreground text-white px-3 py-1 rounded-full">{{ idx + 1 }}</span>
          <span class="px-3 py-1 bg-tertiary text-foreground font-bold text-xs uppercase tracking-wider rounded-full border-2 border-foreground">{{ typeLabel(ex.exerciseType) }}</span>
          <!-- audit-v6 F25: tertiary/quaternary as text fails contrast (~2:1) —
               use badge chips (colored bg + foreground text) instead. -->
          <span v-if="ex.difficulty === 'EASY'" class="px-2 py-0.5 bg-quaternary/20 text-foreground font-bold text-xs uppercase tracking-wider rounded-full border-2 border-quaternary">Dễ</span>
          <span v-else-if="ex.difficulty === 'MEDIUM'" class="px-2 py-0.5 bg-tertiary/20 text-foreground font-bold text-xs uppercase tracking-wider rounded-full border-2 border-tertiary">TB</span>
          <span v-else-if="ex.difficulty === 'HARD'" class="px-2 py-0.5 bg-accent/10 text-accent font-bold text-xs uppercase tracking-wider rounded-full border-2 border-accent">Khó</span>
        </div>

        <div class="p-6 pt-3">
          <!-- MATCHING: use dedicated component -->
          <MatchingExercise v-if="ex.exerciseType === 'MATCHING'"
            :exercise="ex"
            :result="cardStates[ex.id] || {}"
            @answer="onMatchingAnswer(ex.id, $event)"
            @reveal="onMatchingReveal(ex.id, $event)"
          />

          <template v-else>
            <!-- Question text (markdown) -->
            <div class="geo-markdown mb-5" v-html="parseMarkdown(ex.question)"></div>

            <!-- Image -->
            <img v-if="ex.imageUrl" :src="ex.imageUrl" class="max-w-full max-h-96 mx-auto border-2 border-foreground rounded-md mb-5" alt="" />

            <!-- Audio (for listening exercises): file audio hoặc giọng đọc máy của trình duyệt -->
            <div v-if="ex.exerciseType === 'LISTENING' && ex.audioUrl" class="mb-5 bg-secondary/10 border-2 border-foreground p-4 rounded-md">
              <p class="font-bold text-xs uppercase tracking-wider text-secondary mb-2">Nghe & trả lời</p>
              <audio :src="ex.audioUrl" controls class="w-full max-w-md"></audio>
            </div>
            <div v-else-if="ex.exerciseType === 'LISTENING' && isSpeechAvailable()" class="mb-5 bg-secondary/10 border-2 border-foreground p-4 rounded-md flex items-center gap-3">
              <p class="font-bold text-xs uppercase tracking-wider text-secondary flex-1">Nghe & trả lời (giọng đọc máy)</p>
              <AppButton variant="secondary" :aria-label="'Phát âm câu nghe số ' + (idx + 1)" @click="speakListening(ex)">
                🔊 Nghe
              </AppButton>
            </div>

            <!-- Options (multiple choice) -->
            <div v-if="hasOptionChoices(ex)" class="space-y-3">
              <button v-for="(opt, oi) in parsedOptions(ex)" :key="oi"
                @click="selectAnswer(ex.id, opt)"
                class="w-full text-left p-4 border-2 font-medium transition-all rounded-md"
                :class="getCardOptionClass(ex.id, opt)">
                {{ opt }}
              </button>
            </div>

            <!-- Text input (fill blank / translation) -->
            <input v-else v-model="textAnswers[ex.id]" type="text"
              :aria-label="'Câu trả lời cho câu ' + (idx + 1)"
              :placeholder="getInputPlaceholder(ex)"
              :disabled="cardStates[ex.id]?.revealed"
              class="w-full border-2 border-foreground p-4 text-lg font-bold focus:outline-none focus:ring-4 focus:ring-tertiary transition-all rounded-md shadow-pop-sm" />

            <!-- Result badge (after check) -->
            <div v-if="cardStates[ex.id]?.revealed" class="mt-4 p-4 rounded-md border-2" role="alert" aria-live="assertive"
              :class="cardStates[ex.id]?.isCorrect ? 'bg-quaternary/10 border-quaternary' : 'bg-accent/10 border-accent'">
              <div class="flex items-center gap-2 font-black text-sm uppercase mb-1">
                <span>{{ cardStates[ex.id]?.ungradeable ? '⚪ Không chấm được' : (cardStates[ex.id]?.isCorrect ? '✅ Đúng' : '❌ Sai') }}</span>
              </div>
              <p v-if="cardStates[ex.id]?.correctAnswer" class="font-bold text-sm">Đáp án: <span class="text-foreground font-black">{{ cardStates[ex.id].correctAnswer }}</span></p>
              <p v-else-if="cardStates[ex.id]?.ungradeable" class="text-sm text-muted-foreground">Bài tập này chưa có đáp án chuẩn nên không được tính điểm.</p>
              <p v-if="ex.explanation" class="mt-2 text-sm text-muted-foreground italic" v-html="sanitizeText(ex.explanation)"></p>
            </div>

            <!-- Check button -->
            <AppButton v-if="!cardStates[ex.id]?.revealed" @click="checkAnswer(ex)" class="mt-4" variant="pink" :disabled="!getUserAnswer(ex.id) || grading">
              Kiểm tra
            </AppButton>
          </template>
        </div>
      </div>

      <!-- Submit all button -->
      <AppButton @click="submitAll" variant="primary" class="w-full" :disabled="submitting">
        {{ submitting ? 'Đang nộp...' : `Nộp bài (${exercises.length} câu)` }}
      </AppButton>

      <!-- Submit success -->
      <div v-if="submitted" class="mt-4 p-4 bg-quaternary/10 border-2 border-quaternary rounded-md text-center">
        <p class="font-black text-sm uppercase">✅ Đã lưu kết quả! Xem tab Lịch sử.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { sanitizeText } from '@/utils/markdown'
import { isSpeechAvailable, blankOutForSpeech, stripLeadingNumber, speakEnglish } from '@/utils/speech'
import lessonService from '@/services/lessonService'
import MatchingExercise from '@/components/lessons/MatchingExercise.vue'
import AppButton from '@/components/ui/AppButton.vue'

const route = useRoute()
const lessonId = Number(route.params.id)

const exercises = ref([])
const loading = ref(true)
const textAnswers = ref({})
const optionAnswers = ref({})
const cardStates = ref({})
const submitting = ref(false)
const submitted = ref(false)
// audit-v5: đáp án thật không bao giờ nằm trên client (includeAnswers=403 cho
// user thường) — mọi check/submit phải chấm qua API /grade (server-side key).
const grading = ref(false)

function typeLabel(type) {
  const map = { MULTIPLE_CHOICE: 'Trắc nghiệm', FILL_BLANK: 'Điền từ', LISTENING: 'Nghe', MATCHING: 'Nối từ', TRANSLATION: 'Dịch' }
  return map[type] || type
}

// LISTENING không có file audio → đọc bằng giọng máy, che chỗ trống để không lộ đáp án
function speakListening(ex) {
  return speakEnglish(blankOutForSpeech(stripLeadingNumber(ex.question)))
}

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}

function parsedOptions(ex) {
  if (!ex.options) return null
  let opts = ex.options
  if (typeof opts === 'string') {
    if (opts === 'null') return null
    try { opts = JSON.parse(opts) }
    catch { return null }
  }
  if (!Array.isArray(opts) || opts.length === 0) return null
  // audit-v5: seeded rows carry placeholder options (["A","B","C","D"] or
  // ["A - noisy", ...]) with the real answer only in correctAnswer — rendering
  // bare letters is worse than the free-text input.
  const allPlaceholders = opts.every(o => /^[A-D](\s*-\s*.*)?$/i.test(String(o).trim()))
  return allPlaceholders ? null : opts
}

function hasOptionChoices(ex) {
  if (ex.exerciseType === 'MULTIPLE_CHOICE' || ex.exerciseType === 'LISTENING') {
    const opts = parsedOptions(ex)
    return Array.isArray(opts) && opts.length > 0
  }
  // audit-v5: FILL_BLANK/TRANSLATION with REAL word options (e.g.
  // ["not","don't","doesn't"]) render as tappable choices — the answer string
  // matches an option exactly, so grading is unchanged. Placeholder options are
  // filtered out inside parsedOptions and fall back to the text input.
  const opts = parsedOptions(ex)
  return Array.isArray(opts) && opts.length >= 2
}

function getInputPlaceholder(ex) {
  const placeholders = { FILL_BLANK: 'Điền vào chỗ trống...', TRANSLATION: 'Nhập bản dịch...' }
  return placeholders[ex.exerciseType] || 'Nhập câu trả lời...'
}

function getUserAnswer(exId) {
  return textAnswers.value[exId] || optionAnswers.value[exId] || ''
}

function selectAnswer(exId, answer) {
  if (cardStates.value[exId]?.revealed) return
  optionAnswers.value[exId] = answer
  textAnswers.value[exId] = ''
}

function getCardOptionClass(exId, opt) {
  const selected = optionAnswers.value[exId] === opt
  const state = cardStates.value[exId]
  if (!state?.revealed) {
    return selected ? 'border-accent bg-accent/10' : 'border-foreground hover:bg-tertiary/10'
  }
  // audit-v5: sau khi chấm, tô xanh đáp án server trả về (key thật), không so
  // trên client vì correctAnswer bị strip với user thường.
  if (state.correctAnswer && opt.trim().toLowerCase() === String(state.correctAnswer).trim().toLowerCase()) {
    return 'border-quaternary bg-quaternary/10'
  }
  if (selected && !state.isCorrect) return 'border-accent bg-accent/10'
  return 'border-border opacity-60'
}

async function checkAnswer(ex) {
  const userAnswer = getUserAnswer(ex.id)
  if (!userAnswer || grading.value) return
  grading.value = true
  try {
    const res = await lessonService.gradeExercises(lessonId, [
      { exerciseId: ex.id, userAnswer }
    ])
    const item = res?.results?.[0]
    if (item) {
      cardStates.value[ex.id] = {
        revealed: true,
        graded: true,
        isCorrect: !!item.correct,
        ungradeable: !!item.ungradeable,
        correctAnswer: item.correctAnswer || ''
      }
    }
  } catch (e) {
    console.error('Grade failed:', e)
  } finally {
    grading.value = false
  }
}

// MATCHING handlers
function onMatchingAnswer(exId, answer) {
  optionAnswers.value[exId] = answer
  // audit-v5: reset trạng thái đã chấm khi học viên đổi câu trả lời
  cardStates.value[exId] = { revealed: false, graded: false, isCorrect: false, correctAnswer: '' }
}

async function onMatchingReveal(exId, answer) {
  const ex = exercises.value.find(e => e.id === exId)
  if (!ex) return
  await checkAnswer(ex)
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
    const auth = useAuthStore()
    // includeAnswers=true trả 403 cho ROLE_USER; chỉ admin được fetch kèm đáp án.
    const data = auth.isAdmin
      ? await lessonService.getExercisesWithAnswers(lessonId)
      : await lessonService.getExercises(lessonId)
    exercises.value = Array.isArray(data) ? data : []
    for (const ex of exercises.value) {
      cardStates.value[ex.id] = { revealed: false, isCorrect: false }
      textAnswers.value[ex.id] = ''
    }
  } catch (e) {
    console.error('Failed to load exercises:', e)
  } finally {
    loading.value = false
  }
})
</script>
