<template>
  <main class="min-h-[100dvh] bg-background px-4 py-10 lg:py-14">
    <section class="mx-auto max-w-5xl space-y-8" aria-labelledby="recorder-title">
      <router-link :to="`/speaking/${promptId}`" class="inline-flex items-center border-b-2 border-foreground pb-1 text-xs font-black uppercase tracking-wider transition hover:text-accent">&larr; Quay lại đề bài</router-link>

      <div v-if="loading" class="h-80 animate-pulse border-2 border-foreground/10 bg-card/60" role="status" aria-label="Đang tải phòng luyện nói"></div>
      <div v-else-if="loadError" class="border-2 border-danger bg-danger/10 p-6 text-danger" role="alert">
        <p class="font-black">Không tải được phòng luyện nói.</p>
        <p class="mt-1 text-sm">{{ loadError }}</p>
        <AppButton id="retry-prompt-button" class="mt-4" variant="secondary" size="sm" @click="loadPrompt">Thử lại</AppButton>
      </div>

      <template v-else-if="prompt">
        <header class="border-2 border-foreground bg-card p-6 shadow-pop-sm md:p-8">
          <UserPageHeader
            eyebrow="Phòng ghi âm"
            :subtitle="prompt.prompt"
            :divided="false"
            root-class="mb-6"
          >
            <template #title><span id="recorder-title">{{ prompt.title }}</span></template>
            <template #accent>{{ modeLabel }}</template>
            <template #actions>
              <span class="text-sm font-black tabular-nums text-muted-foreground">Tối đa {{ prompt.maxDurationSeconds || 30 }}s</span>
            </template>
          </UserPageHeader>
          <blockquote v-if="prompt.mode === 'READ_ALOUD' && prompt.referenceText" class="mt-6 border-l-4 border-accent bg-muted/60 p-4 text-lg font-semibold leading-relaxed" v-html="sanitizeText(prompt.referenceText)"></blockquote>
        </header>

        <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
          <section class="border-2 border-foreground bg-card p-6 md:p-8" aria-labelledby="studio-title">
            <div class="flex flex-wrap items-end justify-between gap-4 border-b-2 border-foreground pb-5">
              <div>
                <p class="text-xs font-black uppercase tracking-[0.2em] text-muted-foreground">Recording studio</p>
                <h2 id="studio-title" class="mt-2 text-2xl font-black">Ghi âm câu trả lời</h2>
              </div>
              <span class="text-sm font-black tabular-nums text-muted-foreground">{{ formatTime(elapsedSeconds) }}</span>
            </div>

            <div v-if="!previewUrl" class="mt-6 border-2 border-dashed border-foreground bg-muted/60 p-8 text-center">
              <template v-if="isRecording">
                <div class="mx-auto mb-5 h-16 w-16 rounded-full border-8 border-danger/20 bg-danger animate-pulse"></div>
                <p class="text-xs font-black uppercase tracking-[0.2em] text-danger">Đang ghi âm</p>
                <p class="my-4 text-6xl font-black tabular-nums">{{ formatTime(elapsedSeconds) }}</p>
                <AppButton id="stop-recording-button" variant="tertiary" @click="stop">Dừng ghi</AppButton>
              </template>
              <template v-else>
                <div class="mx-auto flex h-20 w-20 items-center justify-center border-2 border-foreground bg-tertiary/30">
                  <span class="h-8 w-8 rounded-full bg-accent"></span>
                </div>
                <p class="mt-5 text-xl font-black">{{ isReady ? 'Micro đã sẵn sàng' : 'Cho phép micro để bắt đầu' }}</p>
                <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-muted-foreground">Ghi ở nơi yên tĩnh. Nói tự nhiên, không cần đọc quá nhanh.</p>
                <AppButton v-if="!isReady" id="enable-microphone-button" class="mt-6" variant="tertiary" @click="prepare()">Bật micro</AppButton>
                <AppButton v-else id="start-recording-button" class="mt-6" variant="primary" @click="start()">Bắt đầu ghi</AppButton>
              </template>
            </div>

            <div v-else class="mt-6 border-2 border-foreground bg-muted/60 p-5">
              <h3 class="font-black">Nghe lại trước khi nộp</h3>
              <audio :src="previewUrl" class="mt-4 w-full" controls />
              <div class="mt-5 flex flex-wrap gap-3">
                <AppButton id="rerecord-button" variant="secondary" :disabled="submitting" @click="recordAgain">Ghi lại</AppButton>
                <AppButton id="submit-recording-button" variant="primary" :disabled="submitting" @click="submitRecording">{{ submitting ? 'Đang gửi' : 'Gửi bài' }}</AppButton>
              </div>
            </div>

            <p v-if="recorderError || submitError" class="mt-4 border-l-4 border-danger bg-danger/10 p-3 text-sm font-bold text-danger" role="alert">{{ recorderError || submitError }}</p>
          </section>

          <aside class="space-y-4">
            <div class="border-2 border-foreground bg-tertiary/20 p-5 text-sm leading-relaxed">
              <h2 class="font-black">Quy trình chấm</h2>
              <ul class="mt-4 space-y-3 text-muted-foreground">
                <li><strong class="text-foreground">Lưu bản ghi:</strong> hệ thống giữ tệp an toàn.</li>
                <li><strong class="text-foreground">Giáo viên nghe:</strong> điểm theo thang 10.</li>
                <li><strong class="text-foreground">Nhận phản hồi:</strong> xem tại lịch sử bài nói.</li>
              </ul>
            </div>
          </aside>
        </div>

        <section v-if="result" class="border-2 border-foreground bg-card p-6" aria-labelledby="result-title" role="status">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="text-xs font-black uppercase tracking-wider text-success">Nộp bài thành công</p>
              <h2 id="result-title" class="mt-1 text-2xl font-black">{{ assessmentHeadline }}</h2>
              <p class="mt-2 max-w-2xl text-sm leading-relaxed text-muted-foreground">{{ assessmentDescription }}</p>
            </div>
            <span class="border-2 border-foreground bg-tertiary/20 px-3 py-2 text-xs font-black uppercase tracking-wider">{{ statusLabel }}</span>
          </div>

          <div v-if="assessing" class="mt-5 flex items-center gap-3 border-l-4 border-tertiary bg-muted/60 p-4 text-sm font-bold">
            <span class="h-4 w-4 animate-spin border-2 border-foreground border-t-transparent" aria-hidden="true"></span>
            AI đang chấm bài của bạn...
          </div>

          <div v-else-if="result.status === 'COMPLETED'" class="mt-5 grid gap-4 sm:grid-cols-3">
            <div v-for="metric in rubricMetrics" :key="metric.label" class="border-2 border-foreground bg-tertiary/20 p-4 text-center">
              <strong class="block text-3xl font-black tabular-nums">{{ metric.value }}/10</strong>
              <span class="text-xs font-black uppercase tracking-wider text-muted-foreground">{{ metric.label }}</span>
            </div>
            <p v-if="result.feedback" class="sm:col-span-3 border-l-4 border-accent bg-muted/60 p-4 text-sm leading-relaxed">{{ result.feedback }}</p>
            <p v-if="result.transcript" class="sm:col-span-3 text-xs leading-relaxed text-muted-foreground"><strong class="text-foreground">Transcript:</strong> {{ result.transcript }}</p>
          </div>

          <div v-else-if="result.status === 'FAILED'" class="mt-5 border-l-4 border-danger bg-danger/10 p-4 text-sm font-bold text-danger" role="alert">
            AI chưa chấm được bài này ({{ result.assessmentError || 'thiếu transcript' }}). Bài vẫn được gửi và sẽ có giáo viên chấm tay.
          </div>

          <router-link :to="`/speaking/${promptId}`" class="mt-5 inline-flex border-b-2 border-foreground pb-1 text-xs font-black uppercase tracking-wider">Xem lịch sử</router-link>
        </section>
      </template>
    </section>
  </main>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSpeakingRecorder } from '@/composables/useSpeakingRecorder'
import speakingService from '@/services/speakingService'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { sanitizeText } from '@/utils/markdown'

const route = useRoute()
const promptId = route.params.id
const prompt = ref(null)
const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const submitError = ref('')
const result = ref(null)
const assessing = ref(false)
const { blob, previewUrl, isRecording, elapsedSeconds, error: recorderError, isReady, prepare, start, stop, clearRecording } = useSpeakingRecorder(30)
const modeLabel = computed(() => prompt.value?.mode === 'READ_ALOUD' ? 'Đọc theo mẫu' : 'Nói tự do')

const statusLabel = computed(() => {
  const status = result.value?.status
  if (status === 'COMPLETED') return 'AI đã chấm'
  if (status === 'PROCESSING') return 'Đang chấm'
  if (status === 'FAILED') return 'Chờ giáo viên'
  return 'Đã gửi'
})

const assessmentHeadline = computed(() => {
  const status = result.value?.status
  if (status === 'COMPLETED') return 'AI đã chấm xong bài của bạn'
  if (status === 'PROCESSING') return 'Bài đang được AI chấm'
  return 'Bài đang chờ giáo viên chấm'
})

const assessmentDescription = computed(() => {
  const status = result.value?.status
  if (status === 'COMPLETED') return 'Điểm ngữ pháp, từ vựng, trôi chảy và nhận xét bên dưới do AI chấm nội dung (chưa phải điểm phát âm). Giáo viên vẫn có thể chấm lại.'
  if (status === 'FAILED') return 'AI không chấm được bài này nhưng bạn vẫn có thể rời trang. Giáo viên sẽ chấm tay.'
  return 'Bạn có thể rời trang. Điểm và nhận xét sẽ xuất hiện tại lịch sử của đề này.'
})

const rubricMetrics = computed(() => {
  const submission = result.value
  if (!submission) return []
  return [
    { label: 'Ngữ pháp', value: submission.scoreGrammar ?? '-' },
    { label: 'Từ vựng', value: submission.scoreVocabulary ?? '-' },
    { label: 'Trôi chảy', value: submission.scoreFluency ?? '-' }
  ]
})

loadPrompt()

async function loadPrompt() {
  loading.value = true
  loadError.value = ''
  try {
    prompt.value = await speakingService.getById(promptId)
  } catch (cause) {
    loadError.value = cause.response?.data?.detail || 'Không tải được đề luyện nói.'
  } finally {
    loading.value = false
  }
}

async function recordAgain() {
  clearRecording()
  result.value = null
  submitError.value = ''
  await prepare()
}

async function submitRecording() {
  if (!blob.value) return
  submitting.value = true
  submitError.value = ''
  const ext = blob.value.type.includes('ogg') ? 'ogg' : 'webm'
  const file = new File([blob.value], `speaking-${Date.now()}.${ext}`, { type: blob.value.type })
  try {
    result.value = await speakingService.uploadSubmission(promptId, file)
    await runAssessment(result.value.id)
  } catch (cause) {
    submitError.value = cause.response?.data?.detail || cause.response?.data?.message || 'Không thể nộp bản ghi.'
  } finally {
    submitting.value = false
  }
}

async function runAssessment(submissionId) {
  assessing.value = true
  try {
    result.value = await speakingService.assessSubmission(submissionId)
  } catch {
    // Assessment is best-effort: the submission stays queued for manual grading.
  } finally {
    assessing.value = false
  }
}

function formatTime(seconds) {
  const safeSeconds = Math.max(seconds || 0, 0)
  return `00:${String(safeSeconds).padStart(2, '0')}`
}
</script>