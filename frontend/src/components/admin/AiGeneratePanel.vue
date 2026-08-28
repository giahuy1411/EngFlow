<template>
  <section
    class="bg-white border-2 border-foreground rounded-md shadow-pop p-6 space-y-4"
    aria-label="AI exercise generation panel"
  >
    <div class="flex items-center gap-3">
      <div class="w-8 h-8 bg-accent border-2 border-foreground flex items-center justify-center rounded-md">
        <Sparkles class="w-4 h-4 text-white" aria-hidden="true" />
      </div>
      <h3 class="font-black text-lg uppercase tracking-tighter">Sinh Bai Tap AI</h3>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div>
        <label for="ai-gen-lesson" class="block text-xs font-bold uppercase mb-1">Lesson</label>
        <select
          id="ai-gen-lesson"
          v-model="selectedLessonId"
          class="w-full border-2 border-foreground px-3 py-2 bg-white font-bold text-sm rounded-md focus:outline-none focus:ring-2 focus:ring-accent"
          :aria-invalid="!selectedLessonId && attempted"
        >
          <option :value="null">-- Chon Lesson --</option>
          <option v-for="l in lessons" :key="l.id" :value="l.id">{{ l.title }}</option>
        </select>
      </div>

      <div>
        <label for="ai-gen-type" class="block text-xs font-bold uppercase mb-1">Loai Bai Tap</label>
        <select
          id="ai-gen-type"
          v-model="selectedType"
          class="w-full border-2 border-foreground px-3 py-2 bg-white font-bold text-sm rounded-md focus:outline-none focus:ring-2 focus:ring-accent"
        >
          <option value="">Tat ca (mix)</option>
          <option value="MULTIPLE_CHOICE">Trac nghiem</option>
          <option value="FILL_BLANK">Dien khuyet</option>
          <option value="MATCHING">Ghep cap</option>
          <option value="TRANSLATION">Dich</option>
          <option value="LISTENING">Nghe</option>
        </select>
      </div>

      <div>
        <label for="ai-gen-count" class="block text-xs font-bold uppercase mb-1">So Luong</label>
        <input
          id="ai-gen-count"
          v-model.number="count"
          type="number"
          min="1"
          max="20"
          class="w-full border-2 border-foreground px-3 py-2 bg-white font-bold text-sm rounded-md focus:outline-none focus:ring-2 focus:ring-accent"
        />
      </div>
    </div>

    <div class="flex flex-wrap gap-3">
      <button
        @click="generate"
        :disabled="loading || !selectedLessonId"
        class="flex items-center gap-2 bg-accent border-2 border-foreground px-5 py-2.5 font-black uppercase text-sm text-white rounded-md shadow-pop hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        :aria-busy="loading"
      >
        <Loader2 v-if="loading" class="w-4 h-4 animate-spin" aria-hidden="true" />
        <Sparkles v-else class="w-4 h-4" aria-hidden="true" />
        {{ loading ? 'Dang sinh...' : 'Sinh bai tap AI' }}
      </button>

      <button
        @click="confirmBatch"
        :disabled="batchLoading"
        class="flex items-center gap-2 bg-foreground border-2 border-foreground px-5 py-2.5 font-black uppercase text-sm text-white rounded-md shadow-pop hover:-translate-y-0.5 disabled:opacity-50 transition-all"
      >
        <Layers class="w-4 h-4" aria-hidden="true" />
        Sinh batch tat ca
      </button>
    </div>

    <div
      v-if="batchStatus.running"
      role="status"
      aria-live="polite"
      class="bg-accent/10 border border-accent/20 rounded-md p-3 text-sm"
    >
      <p class="font-bold">Batch: {{ batchStatus.processed }}/{{ batchStatus.totalLessons }} lessons</p>
      <p class="text-xs text-muted-foreground">Generated: {{ batchStatus.generated }} | Errors: {{ batchStatus.errors }} | Current: {{ batchStatus.currentLesson }}</p>
      <div class="mt-2 h-2 bg-muted rounded-full overflow-hidden" role="progressbar" :aria-valuenow="batchProgressPercent" aria-valuemin="0" aria-valuemax="100">
        <div class="h-full bg-accent transition-all" :style="{ width: batchProgressPercent + '%' }"></div>
      </div>
    </div>

    <div
      v-if="error"
      role="alert"
      class="bg-danger/10 border border-danger/20 rounded-md p-3 text-sm text-danger"
    >
      {{ error }}
    </div>

    <div v-if="generatedExercises.length > 0" class="space-y-3">
      <div class="flex items-center justify-between">
        <h4 class="font-bold text-sm uppercase">Preview ({{ generatedExercises.length }} bai)</h4>
        <button
          @click="saveAll"
          :disabled="saving"
          class="bg-success border-2 border-foreground px-4 py-2 font-black uppercase text-xs text-white rounded-md shadow-pop hover:-translate-y-0.5 disabled:opacity-50 transition-all"
        >
          {{ saving ? 'Dang luu...' : 'Luu tat ca' }}
        </button>
      </div>

      <div class="border border-border rounded-md overflow-x-auto">
        <table class="w-full text-sm" role="table">
          <thead class="bg-muted font-bold uppercase text-xs" role="rowgroup">
            <tr role="row">
              <th class="text-left p-2" role="columnheader">Question</th>
              <th class="text-left p-2" role="columnheader">Type</th>
              <th class="text-left p-2" role="columnheader">Answer</th>
              <th class="text-left p-2" role="columnheader">Audio</th>
              <th class="p-2" role="columnheader"></th>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr
              v-for="(ex, i) in generatedExercises"
              :key="i"
              class="border-t hover:bg-muted/60"
              role="row"
            >
              <td class="p-2 max-w-xs truncate" role="cell">{{ ex.question }}</td>
              <td class="p-2 text-xs" role="cell">{{ ex.exerciseType }}</td>
              <td class="p-2 max-w-xs truncate font-bold" role="cell">{{ ex.correctAnswer }}</td>
              <td class="p-2" role="cell">
                <audio v-if="ex.audioUrl" :src="ex.audioUrl" controls class="h-6 w-32" aria-label="Audio preview"></audio>
                <span v-else class="text-muted-foreground text-xs" aria-label="No audio">-</span>
              </td>
              <td class="p-2" role="cell">
                <button
                  @click="removeExercise(i)"
                  class="text-danger text-xs font-bold"
                  :aria-label="'Xoa bai tap ' + (i + 1)"
                >
                  Xoa
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div
      v-if="successMessage"
      role="status"
      aria-live="polite"
      class="bg-success/10 border border-success/30 rounded-md p-3 text-sm text-success"
    >
      {{ successMessage }}
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import aiExerciseService from '@/services/aiExerciseService'
import lessonService from '@/services/lessonService'
import { useToast } from '@/composables/useToast'
import { Sparkles, Loader2, Layers } from 'lucide-vue-next'

const toast = useToast()
const lessons = ref([])
const selectedLessonId = ref(null)
const selectedType = ref('')
const count = ref(5)
const loading = ref(false)
const saving = ref(false)
const batchLoading = ref(false)
const attempted = ref(false)
const error = ref('')
const successMessage = ref('')
const generatedExercises = ref([])
const batchStatus = ref({ running: false, processed: 0, totalLessons: 0, generated: 0, errors: 0, currentLesson: '' })
let batchPollInterval = null

const batchProgressPercent = computed(() => {
  if (batchStatus.value.totalLessons === 0) return 0
  return Math.round((batchStatus.value.processed / batchStatus.value.totalLessons) * 100)
})

onMounted(async () => {
  try {
    const data = await lessonService.getAll()
    lessons.value = Array.isArray(data) ? data : (data.content || data.lessons || [])
  } catch (e) {
    console.error('Failed to load lessons:', e)
  }
})

onUnmounted(() => {
  if (batchPollInterval) clearInterval(batchPollInterval)
})

async function generate() {
  attempted.value = true
  if (!selectedLessonId.value) return
  loading.value = true
  error.value = ''
  successMessage.value = ''
  generatedExercises.value = []
  const startTime = Date.now()
  try {
    // Option 2: async generation → 202 + batchId, poll /status?batchId
    const batchRes = await aiExerciseService.generateExercisesAsync(selectedLessonId.value, selectedType.value, count.value)
    const batchId = batchRes.batchId
    if (!batchId) {
      error.value = 'AI sinh khong tra ve batchId. Dung sync.'
      return
    }
    // Poll progress per batchId
    const pollTimer = setInterval(async () => {
      try {
        const status = await aiExerciseService.getStatus(batchId)
        const elapsedSec = ((Date.now() - startTime) / 1000).toFixed(1)
        if (status.running) {
          successMessage.value = `Dang sinh... ${elapsedSec}s | ${status.processed}/${status.totalLessons} (generated: ${status.generated})`
        } else {
          clearInterval(pollTimer)
          loading.value = false
          successMessage.value = `Da sinh ${status.generated} bai tap trong ${elapsedSec}s. Review va luu.`
        }
      } catch (e) {
        console.error('Progress poll failed:', e)
      }
    }, 1500)
  } catch (e) {
    error.value = e.response?.data?.error || e.message || 'Loi sinh bai tap'
    loading.value = false
  } finally {
    // loading.value set false when poll completes
  }
}

async function saveAll() {
  saving.value = true
  error.value = ''
  try {
    await aiExerciseService.saveExercises(generatedExercises.value)
    successMessage.value = `Da luu ${generatedExercises.value.length} bai tap vao DB!`
    generatedExercises.value = []
  } catch (e) {
    error.value = e.response?.data?.error || 'Loi luu bai tap'
  } finally {
    saving.value = false
  }
}

function removeExercise(index) {
  generatedExercises.value.splice(index, 1)
}

async function confirmBatch() {
  if (!confirm('Sinh bai tap AI cho TAT CA lesson? Mat nhieu thoi gian.')) return
  batchLoading.value = true
  error.value = ''
  try {
    await aiExerciseService.generateBatch(false)
    startBatchPolling()
  } catch (e) {
    error.value = e.response?.data?.error || 'Loi batch generation'
    batchLoading.value = false
  }
}

function startBatchPolling() {
  batchPollInterval = setInterval(async () => {
    try {
      const status = await aiExerciseService.getBatchStatus()
      batchStatus.value = status
      if (!status.running) {
        clearInterval(batchPollInterval)
        batchPollInterval = null
        batchLoading.value = false
        successMessage.value = `Batch xong: ${status.generated} bai tap, ${status.errors} loi.`
      }
    } catch (e) {
      console.error('Poll failed:', e)
    }
  }, 2000)
}
</script>


