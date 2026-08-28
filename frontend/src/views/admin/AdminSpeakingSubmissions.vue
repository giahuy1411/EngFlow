<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-accent border-2 border-foreground flex items-center justify-center rotate-6 rounded-md">
          <AudioWaveformIcon class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Chấm Bài Speaking</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-muted-foreground">Chấm thủ công bài nộp ghi âm từ học viên</p>
        </div>
      </div>
    </div>

    <!-- Status Filter -->
    <div class="flex flex-wrap items-center gap-3">
      <div class="relative flex-1 min-w-[200px] sm:max-w-xs">
        <SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <input
          id="submission-search"
          name="submission-search"
          v-model="searchQuery"
          type="search"
          placeholder="Tìm bài nộp..."
          class="w-full border-2 border-foreground bg-white pl-10 pr-3 py-2.5 font-bold text-sm rounded-md shadow-pop-sm focus:outline-none focus:ring-2 focus:ring-accent"
        />
      </div>

      <select
        id="submission-status-filter"
        v-model="submissionStatus"
        @change="handleStatusFilterChange"
        class="border-2 border-foreground bg-white px-4 py-2.5 text-sm font-bold rounded-md shadow-pop-sm focus:outline-none focus:ring-2 focus:ring-accent"
      >
        <option value="">Tất cả bài nộp</option>
        <option value="SUBMITTED">Chờ chấm (SUBMITTED)</option>
        <option value="UNDER_REVIEW">Đang chấm (UNDER_REVIEW)</option>
        <option value="GRADED">Đã chấm (GRADED)</option>
      </select>

      <AppButton
        @click="loadSubmissions()"
        variant="secondary"
        size="sm"
      >
        🔄 Tải lại danh sách
      </AppButton>
    </div>

    <!-- Content List -->
    <div v-if="subLoading" class="text-center py-12" role="status">
      <div class="animate-spin w-8 h-8 border-2 border-foreground border-t-transparent rounded-full mx-auto"></div>
      <p class="mt-2 text-xs font-bold uppercase tracking-wider text-muted-foreground">Đang tải bài nộp...</p>
    </div>

    <div v-else-if="subError" class="bg-danger/10 border-2 border-danger p-6 rounded-md">
      <p class="font-bold text-danger">{{ subError }}</p>
      <button @click="loadSubmissions()" class="mt-2 text-sm font-bold underline">Thử lại</button>
    </div>
    <div v-else class="space-y-6">
      <div v-if="filteredSubmissions.length === 0" class="text-center py-12 bg-white border-2 border-dashed border-foreground rounded-md shadow-pop">
        <p class="font-bold uppercase tracking-wider text-sm text-muted-foreground">Chưa có bài nộp nào</p>
      </div>

      <div
        v-for="s in filteredSubmissions"
        :key="s.id"
        class="bg-white border-2 border-foreground p-6 shadow-pop rounded-md hover:-translate-y-0.5 transition-all space-y-4"
      >
        <!-- Header info -->
        <div class="flex flex-wrap items-start justify-between gap-4 border-b-2 border-border pb-4">
          <div>
            <span class="font-black text-xl text-foreground">{{ s.user?.fullName || s.user?.username || 'Học viên #' + s.userId }}</span>
            <span class="text-sm font-bold text-muted-foreground ml-3">Đề bài: {{ s.promptTitle || '#' + s.promptId }}</span>
            <div class="mt-2 flex items-center gap-2">
              <span class="px-2.5 py-1 text-xs font-black uppercase rounded border-2 border-foreground" :class="statusMeta(s.status).className">
                {{ statusMeta(s.status).label }}
              </span>
              <span class="text-xs font-bold text-muted-foreground">Nộp ngày: {{ formatDate(s.submittedAt) }}</span>
            </div>
          </div>

          <div class="text-right">
            <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground block mb-1">Điểm số</span>
            <span class="text-3xl font-black px-4 py-2 bg-tertiary/20 border-2 border-foreground rounded-md inline-block">
              {{ s.score == null ? '-' : s.score + '/10' }}
            </span>
          </div>
        </div>

        <!-- Grid body -->
        <div class="grid gap-6 lg:grid-cols-[1fr_360px]">
          <!-- Media Player & Feedback -->
          <div class="space-y-4">
            <div class="bg-muted/60 border-2 border-foreground p-4 rounded-md">
              <label class="block text-xs font-black uppercase tracking-wider mb-2">Bản ghi âm bài nói</label>
              <audio v-if="isAudio(s.videoUrl)" :src="s.videoUrl" controls class="w-full" preload="metadata" />
              <video v-else-if="s.videoUrl" :src="s.videoUrl" controls class="max-h-72 w-full border-2 border-foreground bg-foreground rounded" preload="metadata" />
              <p v-else class="border-2 border-dashed border-border p-6 text-center text-muted-foreground font-bold text-sm">Không có media ghi âm.</p>
            </div>

            <div v-if="s.adminFeedback" class="border-l-4 border-accent bg-accent/5 p-4 rounded text-sm">
              <strong class="font-black uppercase text-xs text-accent block mb-1">Nhận xét đã lưu cho học viên:</strong>
              <p class="font-medium text-foreground">{{ s.adminFeedback }}</p>
            </div>

            <div v-if="s.privateNote" class="border-l-4 border-border bg-muted p-4 rounded text-sm">
              <strong class="font-black uppercase text-xs text-muted-foreground block mb-1">Ghi chú nội bộ Admin:</strong>
              <p class="font-medium text-muted-foreground">{{ s.privateNote }}</p>
            </div>
          </div>

          <!-- Grading Form -->
          <form class="border-2 border-foreground bg-background p-5 rounded-md shadow-pop-sm space-y-4" @submit.prevent="grade(s)">
            <h3 class="font-black text-base uppercase tracking-wider border-b-2 border-foreground pb-2">Chấm điểm &amp; Nhận xét</h3>

            <div>
              <label class="block text-xs font-black uppercase tracking-wider mb-1" :for="`score-${s.id}`">Điểm số (0 - 10)</label>
              <input
                :id="`score-${s.id}`"
                v-model="gradeForms[s.id].score"
                type="number"
                min="0"
                max="10"
                step="0.1"
                required
                class="w-full border-2 border-foreground bg-white p-2.5 font-black text-lg rounded outline-none focus:ring-2 focus:ring-accent"
              />
            </div>

            <div>
              <label class="block text-xs font-black uppercase tracking-wider mb-1" :for="`feedback-${s.id}`">Nhận xét cho học viên</label>
              <textarea
                :id="`feedback-${s.id}`"
                v-model="gradeForms[s.id].feedback"
                rows="4"
                required
                maxlength="4000"
                class="w-full border-2 border-foreground bg-white p-2.5 text-sm font-medium rounded outline-none focus:ring-2 focus:ring-accent"
                placeholder="Nêu chi tiết ưu điểm, phát âm, từ vựng và phần cần khắc phục..."
              ></textarea>
            </div>

            <div>
              <label class="block text-xs font-black uppercase tracking-wider mb-1" :for="`private-note-${s.id}`">Ghi chú nội bộ (Chỉ Admin)</label>
              <textarea
                :id="`private-note-${s.id}`"
                v-model="gradeForms[s.id].privateNote"
                rows="2"
                maxlength="4000"
                class="w-full border-2 border-foreground bg-white p-2.5 text-sm font-medium rounded outline-none focus:ring-2 focus:ring-accent"
                placeholder="Ghi chú nội bộ quản trị viên..."
              ></textarea>
            </div>

            <p v-if="gradeForms[s.id].error" class="text-sm font-bold text-danger bg-danger/10 p-2 border border-danger rounded" role="alert">
              {{ gradeForms[s.id].error }}
            </p>

            <AppButton
              :id="`grade-${s.id}`"
              type="submit"
              :disabled="gradeForms[s.id].saving"
              :loading="gradeForms[s.id].saving"
              variant="tertiary"
              class="w-full"
            >
              {{ gradeForms[s.id].saving ? 'Đang lưu điểm...' : '💾 Lưu điểm & Nhận xét' }}
            </AppButton>
          </form>
        </div>
      </div>

      <!-- Footer / Pagination -->
      <div class="flex flex-wrap items-center justify-between gap-4 border-2 border-foreground bg-accent/10 px-6 py-3 rounded-md shadow-pop-lg">
        <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Tổng số: {{ totalElements }}</span>
        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          :total-items="totalElements"
          :page-size="pageSize"
          :show-summary="false"
          item-label="bài nộp"
          class="!bg-transparent !border-0 !shadow-none !p-0"
          @page-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { AudioWaveformIcon, SearchIcon } from 'lucide-vue-next'
import speakingService from '@/services/speakingService'
import Pagination from '@/components/common/Pagination.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const submissions = ref([])
const subLoading = ref(false)
const subError = ref(null)
const submissionStatus = ref('')
const searchQuery = ref('')
const gradeForms = ref({})

const currentPage = ref(1)
const totalPages = ref(1)
const totalElements = ref(0)
const pageSize = ref(10)

const filteredSubmissions = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return submissions.value
  return submissions.value.filter(s =>
    (s.user?.fullName && s.user.fullName.toLowerCase().includes(q)) ||
    (s.user?.username && s.user.username.toLowerCase().includes(q)) ||
    (s.user?.email && s.user.email.toLowerCase().includes(q)) ||
    (s.promptTitle && s.promptTitle.toLowerCase().includes(q)) ||
    (s.adminFeedback && s.adminFeedback.toLowerCase().includes(q))
  )
})

onMounted(async () => {
  await loadSubmissions()
})

async function loadSubmissions() {
  subLoading.value = true
  subError.value = null
  try {
    const pageData = await speakingService.getAdminSubmissions(
      currentPage.value - 1,
      pageSize.value,
      submissionStatus.value
    )
    submissions.value = pageData.content || []
    totalPages.value = pageData.totalPages || 1
    totalElements.value = pageData.totalElements ?? submissions.value.length

    gradeForms.value = {}
    for (const s of submissions.value) {
      gradeForms.value[s.id] = {
        score: s.score ?? 8.0,
        feedback: s.adminFeedback || '',
        privateNote: s.privateNote || '',
        saving: false,
        error: null
      }
    }
  } catch (err) {
    subError.value = err.response?.data?.message || 'Không thể tải danh sách bài nộp'
  } finally {
    subLoading.value = false
  }
}

function handleStatusFilterChange() {
  currentPage.value = 1
  loadSubmissions()
}

function handlePageChange(page) {
  currentPage.value = page
  loadSubmissions()
}

function isAudio(url) {
  if (!url) return false
  const lower = url.toLowerCase()
  return lower.endsWith('.webm') || lower.endsWith('.mp3') || lower.endsWith('.wav') || lower.endsWith('.ogg') || lower.includes('/media/')
}

function formatDate(iso) {
  if (!iso) return 'N/A'
  try {
    return new Date(iso).toLocaleString('vi-VN')
  } catch (e) {
    return iso
  }
}

function statusMeta(status) {
  switch (status) {
    case 'SUBMITTED':
      return { label: 'Chờ chấm', className: 'bg-tertiary text-foreground' }
    case 'UNDER_REVIEW':
      return { label: 'Đang chấm', className: 'bg-secondary text-foreground' }
    case 'GRADED':
      return { label: 'Đã chấm', className: 'bg-accent text-white' }
    default:
      return { label: status || 'Khác', className: 'bg-muted text-foreground' }
  }
}

async function grade(s) {
  const form = gradeForms.value[s.id]
  if (!form) return
  form.saving = true
  form.error = null
  try {
    await speakingService.gradeSubmission(s.id, {
      score: Number(form.score),
      feedback: form.feedback,
      privateNote: form.privateNote
    })
    toast.showSuccess('Đã lưu điểm và nhận xét thành công!')
    await loadSubmissions()
  } catch (err) {
    form.error = err.response?.data?.message || 'Chấm điểm thất bại'
  } finally {
    form.saving = false
  }
}
</script>
