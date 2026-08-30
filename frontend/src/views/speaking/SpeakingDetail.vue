<template>
  <main class="min-h-[100dvh] bg-background px-4 py-10 lg:py-14">
    <div class="mx-auto max-w-6xl space-y-8">
      <router-link to="/speaking" class="inline-flex items-center border-b-2 border-foreground pb-1 text-xs font-black uppercase tracking-wider transition hover:text-accent">&larr; Danh sách luyện nói</router-link>

      <div v-if="loading" class="grid gap-4 lg:grid-cols-[1.35fr_0.65fr]" role="status" aria-label="Đang tải đề luyện nói">
        <div class="h-[28rem] animate-pulse border-2 border-foreground/10 bg-card/60"></div>
        <div class="h-[28rem] animate-pulse border-2 border-foreground/10 bg-card/60"></div>
      </div>
      <div v-else-if="error" class="border-2 border-danger bg-danger/10 p-6 text-danger" role="alert">
        <p class="font-black">Không tải được đề luyện nói.</p>
        <p class="mt-1 text-sm">{{ error }}</p>
        <AppButton id="retry-detail-button" class="mt-4" variant="secondary" size="sm" @click="loadPage">Thử lại</AppButton>
      </div>

      <template v-else-if="prompt">
        <section class="grid overflow-hidden border-2 border-foreground bg-card lg:grid-cols-[1.35fr_0.65fr]" aria-labelledby="prompt-title">
          <div class="p-6 md:p-9">
            <UserPageHeader
              eyebrow="Bài luyện nói"
              :subtitle="prompt.description"
              :divided="false"
              root-class="mb-8"
            >
              <template #title><span id="prompt-title">{{ prompt.title }}</span></template>
              <template #accent>{{ prompt.mode === 'READ_ALOUD' ? 'Read aloud' : 'Free talk' }}</template>
              <template #actions>
                <div class="flex flex-wrap gap-2 text-[11px] font-black uppercase tracking-wider">
                  <span class="bg-foreground px-2.5 py-1 text-white">{{ prompt.mode === 'READ_ALOUD' ? 'Đọc theo mẫu' : 'Nói tự do' }}</span>
                  <span class="border-2 border-foreground px-2.5 py-1">{{ prompt.level || 'Mọi cấp độ' }}</span>
                  <span v-if="prompt.lessonId" class="bg-tertiary px-2.5 py-1">Theo bài học</span>
                </div>
              </template>
            </UserPageHeader>
            <div class="mt-8 border-l-4 border-accent bg-muted/60 p-5">
              <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Nhiệm vụ</p>
              <p class="mt-2 text-xl font-semibold leading-relaxed" v-html="sanitizeText(prompt.prompt)"></p>
            </div>
            <blockquote v-if="prompt.mode === 'READ_ALOUD' && prompt.referenceText" class="mt-4 border-2 border-foreground p-5">
              <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Đoạn đọc</p>
              <p class="mt-2 text-lg font-semibold leading-relaxed" v-html="sanitizeText(prompt.referenceText)"></p>
            </blockquote>
            <router-link id="start-speaking-button" :to="`/speaking/${prompt.id}/record`" class="mt-8 inline-flex min-h-12 items-center justify-center bg-accent px-6 py-3 text-xs font-black uppercase tracking-wider text-white transition hover:-translate-y-0.5 active:scale-[0.98]">Bắt đầu luyện</router-link>
          </div>

          <aside class="border-t-2 border-foreground bg-foreground p-6 text-white lg:border-l-2 lg:border-t-0">
            <p class="text-xs font-black uppercase tracking-[0.2em] text-white/60">Chuẩn bị trước khi nói</p>
            <h2 class="mt-3 text-2xl font-black">Tập trung vào ý, không học thuộc câu.</h2>
            <div v-if="prompt.referenceMediaUrl" class="mt-8 border-2 border-white/30 bg-white/5 p-4">
              <p class="text-xs font-black uppercase tracking-wider text-white/60">Tệp tham khảo</p>
              <audio v-if="isReferenceAudio(prompt.referenceMediaUrl)" :src="prompt.referenceMediaUrl" class="mt-4 w-full" controls preload="metadata" />
              <video v-else :src="prompt.referenceMediaUrl" class="mt-4 aspect-video w-full bg-black/30" controls preload="metadata" />
            </div>
            <div v-else class="mt-8 border-2 border-dashed border-white/30 p-5 text-sm leading-relaxed text-white/70">Đề này không có tệp mẫu. Dùng phần nhiệm vụ làm điểm tựa rồi nói bằng ngôn ngữ của bạn.</div>
            <ul class="mt-8 space-y-4 border-t border-white/20 pt-6 text-sm text-white/70">
              <li><strong class="text-white">01</strong><span class="ml-3">Ghi âm tối đa {{ prompt.maxDurationSeconds || 30 }} giây.</span></li>
              <li><strong class="text-white">02</strong><span class="ml-3">Giáo viên chấm trên thang 10.</span></li>
              <li><strong class="text-white">03</strong><span class="ml-3">Nhận xét xuất hiện trong lịch sử luyện tập.</span></li>
            </ul>
          </aside>
        </section>

        <section class="space-y-5" aria-labelledby="history-title">
          <div class="flex flex-wrap items-end justify-between gap-4 border-b-2 border-foreground pb-4">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.2em] text-muted-foreground">Tiến trình cá nhân</p>
              <h2 id="history-title" class="mt-2 text-3xl font-black tracking-tight">Các lần luyện gần đây</h2>
            </div>
            <span class="text-sm font-black tabular-nums text-muted-foreground">{{ historyTotalElements }} lần</span>
          </div>

          <div v-if="!submissions.length" class="border-2 border-dashed border-foreground bg-card p-10 text-center text-muted-foreground">Chưa có bài làm. Bắt đầu lần luyện đầu tiên.</div>
          <div v-else class="divide-y-2 divide-foreground border-2 border-foreground bg-card">
            <article v-for="submission in submissions" :key="submission.id" class="grid gap-5 p-5 md:grid-cols-[minmax(0,1fr)_9rem] md:items-start">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2 text-xs font-black uppercase tracking-wider">
                  <span>{{ formatDate(submission.submittedAt) }}</span>
                  <span :class="statusMeta(submission.status).className">{{ statusMeta(submission.status).label }}</span>
                </div>
                <audio v-if="isAudio(submission)" :src="submission.videoUrl" class="mt-4 w-full" controls preload="metadata" />
                <video v-else-if="submission.videoUrl" :src="submission.videoUrl" class="mt-4 max-h-64 w-full bg-foreground" controls preload="metadata" />
                <div v-if="submission.status === 'GRADED' && submission.adminFeedback" class="mt-4 border-l-4 border-accent bg-muted/60 p-4">
                  <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Nhận xét giáo viên</p>
                  <p class="mt-1 text-sm leading-relaxed">{{ submission.adminFeedback }}</p>
                </div>
                <p v-else class="mt-4 text-sm leading-relaxed text-muted-foreground">{{ statusMeta(submission.status).description }}</p>
              </div>
              <div class="border-2 border-foreground bg-tertiary/20 p-4 text-center">
                <strong class="block text-4xl font-black tabular-nums">{{ submission.score == null ? '-' : submission.score }}</strong>
                <span class="text-xs font-black uppercase tracking-wider text-muted-foreground">Điểm /10</span>
              </div>
            </article>
          </div>
          <Pagination
            v-if="historyTotalPages > 1"
            :current-page="historyPage"
            :total-pages="historyTotalPages"
            :total-items="historyTotalElements"
            :page-size="historyPageSize"
            item-label="bài luyện"
            @page-change="handleHistoryPageChange"
          />
        </section>
      </template>
    </div>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import speakingService from '@/services/speakingService'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import Pagination from '@/components/common/Pagination.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { sanitizeText } from '@/utils/markdown'

const route = useRoute()
const prompt = ref(null)
const submissions = ref([])
const loading = ref(true)
const error = ref('')
const historyPage = ref(1)
const historyPageSize = 5
const historyTotalPages = ref(1)
const historyTotalElements = ref(0)

loadPage()

async function loadPage() {
  loading.value = true
  error.value = ''
  try {
    const [promptData, historyPageData] = await Promise.all([
      speakingService.getById(route.params.id),
      speakingService.getPromptSubmissions(route.params.id, historyPage.value - 1, historyPageSize)
    ])
    prompt.value = promptData
    submissions.value = historyPageData.content || []
    historyTotalPages.value = historyPageData.totalPages || 1
    historyTotalElements.value = historyPageData.totalElements ?? submissions.value.length
  } catch (cause) {
    error.value = cause.response?.data?.detail || 'Không tải được bài luyện nói.'
  } finally {
    loading.value = false
  }
}

function handleHistoryPageChange(page) {
  historyPage.value = page
  loadPage()
}

function statusMeta(status) {
  if (status === 'GRADED') return { label: 'Đã chấm', className: 'text-success', description: 'Giáo viên đã hoàn tất chấm bài.' }
  if (status === 'UNDER_REVIEW') return { label: 'Đang chấm', className: 'text-amber-700', description: 'Giáo viên đang xem bài của bạn.' }
  return { label: 'Chờ chấm', className: 'text-muted-foreground', description: 'Bài đã được gửi và đang chờ giáo viên chấm.' }
}

function isAudio(submission) {
  const mediaType = submission?.mediaType || ''
  if (mediaType.startsWith('audio/')) return true
  if (mediaType.startsWith('video/')) return false
  return /\.(ogg|mp3|wav|m4a|webm)(\?|$)/i.test(submission?.videoUrl || '')
}

function isReferenceAudio(url = '') {
  return /\.(ogg|mp3|wav|m4a|webm)(\?|$)/i.test(url)
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '-'
}
</script>