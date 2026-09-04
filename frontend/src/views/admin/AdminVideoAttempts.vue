<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-accent border-2 border-foreground flex items-center justify-center rotate-6 rounded-md">
          <AudioLinesIcon class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Chấm Shadowing</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-muted-foreground">Bản ghi luyện nói theo video của học viên</p>
        </div>
      </div>
      <select id="va-status-filter" v-model="statusFilter" class="border-2 border-foreground bg-white px-4 py-2.5 text-sm font-bold rounded-md focus:outline-none focus:ring-2 focus:ring-accent shadow-pop-sm" @change="load">
        <option value="">Tất cả</option>
        <option value="SUBMITTED">Chưa chấm</option>
        <option value="GRADED">Đã chấm</option>
      </select>
    </div>

    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin w-8 h-8 border-2 border-foreground border-t-transparent rounded-full mx-auto"></div>
    </div>
    <div v-else-if="error" class="bg-danger/10 border-2 border-danger p-6 rounded">
      <p class="font-bold text-danger">{{ error }}</p>
      <button @click="load" class="mt-2 text-sm font-bold underline">Thử lại</button>
    </div>
    <div v-else-if="attempts.length === 0" class="bg-white border-2 border-foreground p-10 text-center rounded-md shadow-pop">
      <p class="font-bold uppercase tracking-wider text-muted-foreground text-sm">Không có bản ghi nào</p>
    </div>
    <div v-else class="space-y-4">
      <div v-for="a in attempts" :key="a.id" class="bg-white border-2 border-foreground rounded-md shadow-pop p-5">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p class="font-black uppercase tracking-wider">Video #{{ a.videoLessonId }} · Câu {{ a.lineIndex + 1 }}</p>
            <p class="text-xs font-bold text-muted-foreground mt-1">{{ a.submittedAt?.replace('T', ' ').slice(0, 16) }} · {{ a.status === 'GRADED' ? `Đã chấm ${a.score}/10` : 'Chưa chấm' }}</p>
          </div>
          <audio v-if="a.mediaUrl" :src="resolveMediaUrl(a.mediaUrl)" controls class="h-10 max-w-full" @loadedmetadata="fixWebmDuration" />
        </div>
        <div v-if="a.status !== 'GRADED'" class="mt-4 flex flex-wrap items-end gap-3 border-t-2 border-foreground/10 pt-4">
          <AppButton variant="primary" :disabled="aiGrading[a.id]" @click="aiGrade(a)">
            {{ aiGrading[a.id] ? 'AI đang chấm...' : '✨ AI chấm' }}
          </AppButton>
          <div>
            <label :for="`score-${a.id}`" class="block text-xs font-bold uppercase mb-1">Điểm (0–10)</label>
            <input :id="`score-${a.id}`" v-model.number="grading[a.id].score" type="number" min="0" max="10" step="0.5"
              class="w-24 border-2 border-border p-2 text-sm rounded-sm focus:border-accent focus:shadow-pop-accent outline-none" />
          </div>
          <div class="flex-1 min-w-[200px]">
            <label :for="`feedback-${a.id}`" class="block text-xs font-bold uppercase mb-1">Nhận xét</label>
            <input :id="`feedback-${a.id}`" v-model="grading[a.id].feedback" placeholder="Phát âm tốt ở..., cần chú ý ngữ điệu..."
              class="w-full border-2 border-border p-2 text-sm rounded-sm focus:border-accent focus:shadow-pop-accent outline-none" />
          </div>
          <AppButton variant="tertiary" :disabled="grading[a.id].saving" @click="submitGrade(a)">
            {{ grading[a.id].saving ? 'Đang lưu...' : 'Chấm' }}
          </AppButton>
        </div>
        <p v-else class="mt-3 text-sm font-medium text-muted-foreground border-t-2 border-foreground/10 pt-3">💬 {{ a.adminFeedback || 'Không có nhận xét' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { AudioLinesIcon } from 'lucide-vue-next'
import videoLessonService from '@/services/videoLessonService'
import AppButton from '@/components/ui/AppButton.vue'
import { useToast} from '@/composables/useToast'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const toast = useToast()
const attempts = ref([])
const loading = ref(true)
const error = ref(null)
const statusFilter = ref('SUBMITTED')
const grading = reactive({})
const aiGrading = reactive({})

onMounted(load)

async function load() {
  loading.value = true
  error.value = null
  try {
    const pageData = await videoLessonService.getAdminAttempts(statusFilter.value, 0, 50)
    attempts.value = pageData.content || []
    attempts.value.forEach(a => { if (!grading[a.id]) grading[a.id] = { score: null, feedback: '', saving: false } })
  } catch (err) {
    error.value = err.response?.data?.message || 'Không thể tải danh sách bản ghi'
  } finally {
    loading.value = false
  }
}

async function submitGrade(attempt) {
  const entry = grading[attempt.id]
  if (entry.score == null || entry.score < 0 || entry.score > 10) {
    toast.showError('Điểm phải từ 0 đến 10')
    return
  }
  entry.saving = true
  try {
    await videoLessonService.gradeAttempt(attempt.id, entry.score, entry.feedback)
    toast.showSuccess('Đã chấm điểm')
    await load()
  } catch (err) {
    toast.showError(err.response?.data?.detail || 'Chấm thất bại')
  } finally {
    entry.saving = false
  }
}

/**
 * AI grading runs Whisper + LLM synchronously (can take ~1-2 min on the local
 * GPU), so each attempt shows its own busy state and the result reloads the list.
 */
async function aiGrade(attempt) {
  if (aiGrading[attempt.id]) return
  aiGrading[attempt.id] = true
  try {
    await videoLessonService.aiGradeAttempt(attempt.id)
    toast.showSuccess('AI đã chấm xong')
    await load()
  } catch (err) {
    toast.showError(err.response?.data?.detail || err.response?.data?.message || 'AI chấm thất bại')
  } finally {
    aiGrading[attempt.id] = false
  }
}

/**
 * MediaRecorder webm has no duration in its header, so the native control shows
 * "∞". Seeking past the end forces the browser to compute it, then rewinding.
 */
function fixWebmDuration(event) {
  const el = event.target
  if (Number.isFinite(el.duration) && el.duration > 0) return
  const originalRate = el.playbackRate
  el.onended = () => {
    el.onended = null
    el.pause()
    el.currentTime = 0
    el.playbackRate = originalRate
  }
  el.currentTime = 1e10
  el.playbackRate = 16
  el.play().catch(() => {})
}
</script>
