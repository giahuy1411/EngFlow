<template>
  <main class="min-h-[100dvh] bg-background">
    <div class="mx-auto max-w-5xl space-y-8 px-4 py-10 lg:py-14">
      <UserPageHeader
        eyebrow="Tiến trình cá nhân"
        title="Lịch sử bài nói"
        subtitle="Nghe lại bản ghi, xem điểm và đọc nhận xét sau mỗi lần luyện."
      >
        <template #accent>History</template>
        <template #actions>
          <router-link to="/speaking" class="inline-flex min-h-11 items-center rounded-full border-2 border-foreground bg-card px-5 py-3 text-xs font-black uppercase tracking-wider shadow-pop-sm transition hover:-translate-y-0.5 hover:bg-foreground hover:text-white active:scale-[0.98]">Luyện nói</router-link>
        </template>
      </UserPageHeader>

      <div v-if="loading" class="space-y-4" role="status" aria-label="Đang tải lịch sử bài nói">
        <div v-for="index in pageSize" :key="index" class="h-40 animate-pulse border-2 border-foreground/10 bg-card/60"></div>
      </div>

      <section v-else-if="error" class="border-2 border-danger bg-danger/10 p-6 text-danger" role="alert">
        <p class="font-black">Không tải được lịch sử.</p>
        <p class="mt-1 text-sm">{{ error }}</p>
        <AppButton id="retry-speaking-history-button" class="mt-4" variant="secondary" size="sm" @click="loadSubmissions">Thử lại</AppButton>
      </section>

      <section v-else-if="!submissions.length" class="border-2 border-dashed border-foreground bg-card p-12 text-center">
        <p class="text-2xl font-black">Chưa có bài làm nào</p>
        <p class="mt-2 text-sm text-muted-foreground">Hoàn thành một đề luyện nói để lịch sử xuất hiện tại đây.</p>
        <router-link to="/speaking" class="mt-6 inline-flex border-2 border-foreground bg-accent px-5 py-3 text-xs font-black uppercase tracking-wider text-white">Chọn đề luyện</router-link>
      </section>

      <section v-else class="space-y-5">
        <article v-for="submission in submissions" :key="submission.id" class="border-2 border-foreground bg-card p-5 shadow-pop-sm md:p-6">
          <div class="grid gap-5 md:grid-cols-[minmax(0,1fr)_9rem] md:items-start">
            <div class="min-w-0">
              <h2 class="line-clamp-2 text-xl font-black">{{ submission.promptTitle || `Bài luyện #${submission.promptId}` }}</h2>
              <div class="mt-2 flex flex-wrap gap-2 text-xs font-black uppercase tracking-wider">
                <span>{{ formatDate(submission.submittedAt) }}</span>
                <span :class="statusMeta(submission.status).className">{{ statusMeta(submission.status).label }}</span>
              </div>
            </div>
            <div class="border-2 border-foreground bg-tertiary/20 p-4 text-center">
              <strong class="block text-4xl font-black tabular-nums">{{ displayTotal(submission) }}</strong>
              <span class="text-xs font-black uppercase tracking-wider text-muted-foreground">{{ displayTotalLabel(submission) }}</span>
            </div>
          </div>

          <div v-if="submission.status === 'GRADED' && submission.adminFeedback" class="mt-5 border-l-4 border-accent bg-muted/60 p-4">
            <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Nhận xét giáo viên</p>
            <p class="mt-1 text-sm leading-relaxed">{{ submission.adminFeedback }}</p>
          </div>
          <div v-else-if="submission.status === 'COMPLETED'" class="mt-5 space-y-3">
            <div class="grid gap-3 sm:grid-cols-3">
              <div v-for="metric in rubricMetrics(submission)" :key="metric.label" class="border-2 border-foreground bg-tertiary/20 p-3 text-center">
                <strong class="block text-2xl font-black tabular-nums">{{ metric.value }}/10</strong>
                <span class="text-[11px] font-black uppercase tracking-wider text-muted-foreground">{{ metric.label }}</span>
              </div>
            </div>
            <p v-if="submission.feedback" class="border-l-4 border-accent bg-muted/60 p-4 text-sm leading-relaxed">{{ submission.feedback }}</p>
          </div>
          <p v-else class="mt-5 text-sm leading-relaxed text-muted-foreground">{{ statusMeta(submission.status).description }}</p>

          <AppButton v-if="resolveMediaUrl(submission.videoUrl)" :id="`toggle-submission-${submission.id}`" class="mt-5" variant="secondary" :aria-expanded="playing === submission.id" @click="playing = playing === submission.id ? null : submission.id">
            {{ playing === submission.id ? 'Ẩn bản ghi' : 'Mở bản ghi' }}
          </AppButton>
          <template v-if="playing === submission.id && resolveMediaUrl(submission.videoUrl)">
            <audio v-if="isAudio(submission)" :src="resolveMediaUrl(submission.videoUrl)" controls class="mt-4 w-full" preload="metadata" />
            <video v-else :src="resolveMediaUrl(submission.videoUrl)" controls class="mt-4 max-h-80 w-full bg-foreground" preload="metadata" />
          </template>
        </article>

        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          :total-items="totalElements"
          :page-size="pageSize"
          item-label="bài nộp"
          @page-change="handlePageChange"
        />
      </section>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import speakingService from '@/services/speakingService'
import Pagination from '@/components/common/Pagination.vue'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'

const submissions = ref([])
const loading = ref(true)
const error = ref('')
const playing = ref(null)
const currentPage = ref(1)
const totalPages = ref(1)
const totalElements = ref(0)
const pageSize = 8

onMounted(loadSubmissions)

async function loadSubmissions() {
  loading.value = true
  error.value = ''
  try {
    const pageData = await speakingService.getSubmissions(currentPage.value - 1, pageSize)
    submissions.value = pageData.content || []
    totalPages.value = pageData.totalPages || 1
    totalElements.value = pageData.totalElements ?? submissions.value.length
  } catch (cause) {
    error.value = cause.response?.data?.detail || 'Không tải được lịch sử bài nói.'
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  currentPage.value = page
  playing.value = null
  loadSubmissions()
}

function statusMeta(status) {
  if (status === 'GRADED') return { label: 'Đã chấm', className: 'text-success font-black', description: 'Giáo viên đã hoàn tất chấm bài.' }
  if (status === 'COMPLETED') return { label: 'AI đã chấm', className: 'text-success font-black', description: 'AI đã chấm nội dung bài nói. Giáo viên có thể chấm lại.' }
  if (status === 'PROCESSING') return { label: 'Đang chấm', className: 'text-foreground font-black', description: 'AI đang phân tích bài nói của bạn.' }
  if (status === 'UNDER_REVIEW') return { label: 'Đang chấm', className: 'text-foreground font-black', description: 'Giáo viên đang xem bài của bạn.' }
  if (status === 'FAILED') return { label: 'Chờ chấm', className: 'text-muted-foreground font-black', description: 'AI chưa chấm được, giáo viên sẽ chấm tay bài này.' }
  return { label: 'Chờ chấm', className: 'text-muted-foreground font-black', description: 'Bài đã gửi và đang chờ giáo viên chấm.' }
}

function rubricMetrics(submission) {
  return [
    { label: 'Ngữ pháp', value: submission.scoreGrammar ?? '-' },
    { label: 'Từ vựng', value: submission.scoreVocabulary ?? '-' },
    { label: 'Trôi chảy', value: submission.scoreFluency ?? '-' }
  ]
}

// Teacher score wins; otherwise show the AI average on the same 0-10 scale.
function displayTotal(submission) {
  if (submission.score != null) return submission.score
  if (submission.scoreTotal != null) return formatAiScore(submission.scoreTotal)
  return '-'
}

function displayTotalLabel(submission) {
  if (submission.score != null) return 'Điểm /10'
  if (submission.scoreTotal != null) return 'AI chấm /10'
  return 'Điểm /10'
}

function formatAiScore(value) {
  return Number.isInteger(value) ? String(value) : value.toFixed(1)
}

function isAudio(submission) {
  const mediaType = submission?.mediaType || ''
  if (mediaType.startsWith('audio/')) return true
  if (mediaType.startsWith('video/')) return false
  return /\.(ogg|mp3|wav|m4a|webm)(\?|$)/i.test(submission?.videoUrl || '')
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '-'
}
</script>