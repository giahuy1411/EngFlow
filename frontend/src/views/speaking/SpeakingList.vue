<template>
  <main class="min-h-[100dvh] bg-background">
    <div class="mx-auto max-w-6xl space-y-8 px-4 py-10 lg:py-14">
      <UserPageHeader
        eyebrow="Phòng luyện nói"
        title="Luyện nói"
        subtitle="Chọn một đề, ghi âm câu trả lời, nhận điểm và nhận xét từ giáo viên."
      >
        <template #accent>Speaking</template>
        <template #actions>
          <router-link
            to="/speaking/history"
            class="inline-flex min-h-11 items-center justify-center rounded-full border-2 border-foreground bg-card px-5 py-3 text-xs font-black uppercase tracking-wider shadow-pop-sm transition hover:-translate-y-0.5 hover:bg-foreground hover:text-white active:scale-[0.98]"
          >
            Xem lịch sử
          </router-link>
        </template>
      </UserPageHeader>

      <section class="grid gap-4 md:grid-cols-[minmax(0,1fr)_auto] md:items-end" aria-label="Tìm đề luyện nói">
        <label class="block">
          <span class="mb-2 block text-xs font-black uppercase tracking-wider text-muted-foreground">Tìm theo đề, kỹ năng hoặc cấp độ</span>
          <input
            v-model="searchQuery"
            type="search"
            placeholder="Ví dụ: daily routine"
            class="min-h-12 w-full border-2 border-foreground bg-card px-4 font-bold text-foreground shadow-pop-sm outline-none transition focus:ring-2 focus:ring-accent"
          />
        </label>
        <div class="border-2 border-foreground bg-tertiary/25 px-5 py-3 text-right">
          <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Đề luyện có sẵn</p>
          <p class="mt-1 text-2xl font-black tabular-nums">{{ totalElements }}</p>
        </div>
      </section>

      <section v-if="loading" class="grid gap-4 md:grid-cols-2 xl:grid-cols-3" aria-label="Đang tải đề luyện nói" role="status">
        <div v-for="index in 6" :key="index" class="h-64 animate-pulse border-2 border-foreground/10 bg-card/60"></div>
      </section>

      <section v-else-if="error" class="border-2 border-danger bg-danger/10 p-6 text-danger-ink" role="alert">
        <p class="font-black">Không tải được đề luyện nói.</p>
        <p class="mt-1 text-sm">{{ error }}</p>
        <AppButton id="retry-speaking-list-button" class="mt-4" variant="secondary" size="sm" @click="loadPrompts">Thử lại</AppButton>
      </section>

      <section v-else-if="!prompts.length" class="border-2 border-dashed border-foreground bg-card p-12 text-center">
        <p class="text-2xl font-black">Chưa có đề phù hợp</p>
        <p class="mt-2 text-sm text-muted-foreground">Thử từ khóa khác hoặc quay lại sau.</p>
      </section>

      <section v-else class="space-y-6">
        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <article v-for="prompt in prompts" :key="prompt.id" class="group flex min-h-64 flex-col border-2 border-foreground bg-card p-5 shadow-pop-sm transition hover:-translate-y-1 hover:shadow-pop">
            <div class="flex flex-wrap items-center gap-2 text-[11px] font-black uppercase tracking-wider">
              <span class="bg-foreground px-2.5 py-1 text-white">{{ prompt.mode === 'READ_ALOUD' ? 'Đọc theo mẫu' : 'Nói tự do' }}</span>
              <span class="border-2 border-foreground px-2.5 py-1">{{ prompt.level || 'Mọi cấp độ' }}</span>
            </div>
            <h2 class="mt-5 line-clamp-2 text-2xl font-black tracking-tight">{{ prompt.title }}</h2>
            <p class="mt-3 line-clamp-3 text-sm leading-relaxed text-muted-foreground" v-html="sanitizeText(prompt.description || prompt.prompt)"></p>
            <div class="mt-auto flex items-center justify-between gap-3 border-t-2 border-foreground pt-5">
              <span class="text-xs font-black uppercase tracking-wider text-muted-foreground">{{ prompt.lessonId ? 'Gắn với bài học' : 'Luyện độc lập' }}</span>
              <router-link :to="`/speaking/${prompt.id}`" class="border-2 border-foreground bg-accent-strong px-4 py-2 text-xs font-black uppercase text-white transition hover:-translate-y-0.5 active:scale-[0.98]">Luyện ngay</router-link>
            </div>
          </article>
        </div>

        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          :total-items="totalElements"
          :page-size="pageSize"
          item-label="đề luyện"
          @page-change="handlePageChange"
        />
      </section>
    </div>
  </main>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import speakingService from '@/services/speakingService'
import Pagination from '@/components/common/Pagination.vue'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { sanitizeText } from '@/utils/markdown'

const prompts = ref([])
const loading = ref(true)
const error = ref('')
const searchQuery = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const totalElements = ref(0)
const pageSize = ref(9)
let searchTimer

onMounted(loadPrompts)
onBeforeUnmount(() => clearTimeout(searchTimer))

watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadPrompts()
  }, 350)
})

async function loadPrompts() {
  loading.value = true
  error.value = ''
  try {
    const pageData = await speakingService.getPrompts(currentPage.value - 1, pageSize.value, searchQuery.value.trim())
    prompts.value = pageData.content || []
    totalPages.value = pageData.totalPages || 1
    totalElements.value = pageData.totalElements ?? prompts.value.length
  } catch (cause) {
    error.value = cause.response?.data?.detail || 'Không tải được danh sách luyện nói.'
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  currentPage.value = page
  loadPrompts()
}
</script>