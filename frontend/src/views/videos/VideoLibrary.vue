<template>
  <div class="bg-background min-h-screen">
    <!-- Header bar (same pattern as Lessons.vue) -->
    <div class="bg-foreground border-b-2 border-foreground">
      <div class="max-w-7xl mx-auto px-6 py-3 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 bg-tertiary border-2 border-foreground flex items-center justify-center font-black text-sm rotate-12 rounded-sm">
              <span class="text-foreground">▶</span>
            </div>
            <span class="text-white font-bold text-xs uppercase tracking-wider">Học qua video</span>
          </div>
        </div>
        <router-link to="/lessons"
          class="text-white/80 hover:text-white font-bold text-[10px] uppercase tracking-widest border border-white/30 px-3 py-1 rounded-md hover:bg-white/10 transition-colors"
        >Bài học chữ</router-link>
      </div>
    </div>

    <section class="max-w-7xl mx-auto px-6 py-16">
      <div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-8 mb-12">
        <div class="relative">
          <div class="absolute -top-4 -left-4 w-8 h-8 bg-tertiary border-2 border-foreground rotate-12 rounded-sm"></div>
          <div class="absolute -bottom-2 -right-2 w-6 h-6 bg-secondary border-2 border-foreground rounded-full"></div>
          <h1 class="font-black text-5xl md:text-6xl uppercase tracking-tight leading-none relative z-10">
            Video <span class="text-accent">learning</span>
          </h1>
          <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground mt-3">
            Xem video thật · bấm vào từ để học · luyện nói theo từng câu
          </p>
        </div>
        <div class="flex flex-wrap gap-2" role="group" aria-label="Lọc theo trình độ">
          <button
            v-for="lv in levels" :key="lv.value"
            class="px-4 py-2 border-2 border-foreground rounded-md font-bold text-xs uppercase tracking-wider transition-all"
            :class="activeLevel === lv.value ? 'bg-foreground text-white shadow-pop-sm' : 'bg-card hover:bg-tertiary/30'"
            @click="selectLevel(lv.value)"
          >{{ lv.label }}</button>
        </div>
      </div>

      <div v-if="loading" class="flex flex-col items-center justify-center py-32 gap-4" role="status">
        <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
        <p class="font-bold text-sm uppercase tracking-widest text-muted-foreground">Đang tải...</p>
      </div>
      <div v-else-if="error" class="max-w-xl mx-auto text-center py-20" role="alert">
        <p class="font-bold text-lg text-accent">{{ error }}</p>
        <AppButton class="mt-4" variant="secondary" size="sm" @click="fetchPage">Thử lại</AppButton>
      </div>
      <template v-else>
        <div v-if="lessons.length === 0" class="text-center py-32">
          <p class="font-bold text-lg text-muted-foreground uppercase tracking-wider">Chưa có bài học video nào</p>
          <p class="font-medium text-sm text-muted-foreground mt-2">Quản trị viên có thể thêm video đầu tiên trong trang Admin.</p>
        </div>
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <router-link
            v-for="lesson in lessons" :key="lesson.id"
            :to="`/videos/${lesson.id}`"
            class="group bg-card border-2 border-foreground shadow-pop-xl hover:-translate-y-2 hover:shadow-pop-lg transition-all duration-300 flex flex-col relative overflow-hidden rounded-md"
          >
            <div class="h-2.5 w-full" :style="{ background: levelColor(lesson.level) }"></div>
            <div class="relative aspect-video overflow-hidden bg-muted border-b-2 border-foreground">
              <img
                v-if="!brokenThumbs.includes(lesson.id)"
                :src="`https://i.ytimg.com/vi/${lesson.youtubeVideoId}/hqdefault.jpg`"
                @error="brokenThumbs.push(lesson.id)"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" alt="" loading="lazy" />
              <div v-else class="w-full h-full flex items-center justify-center bg-foreground/5">
                <span class="text-4xl opacity-40">🎬</span>
              </div>
              <div class="absolute inset-0 flex items-center justify-center">
                <div class="w-14 h-14 bg-foreground/85 border-2 border-foreground rounded-full flex items-center justify-center shadow-pop">
                  <span class="text-white text-xl ml-1">▶</span>
                </div>
              </div>
              <div v-if="lesson.durationSeconds" class="absolute bottom-2 right-2 bg-card border-2 border-foreground px-2 py-0.5 shadow-pop-sm rounded-sm">
                <span class="font-bold text-xs">{{ formatDuration(lesson.durationSeconds) }}</span>
              </div>
            </div>
            <div class="p-5 flex flex-col flex-1 gap-3">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="w-2.5 h-2.5 border border-foreground rounded-full" :style="{ background: levelColor(lesson.level) }"></span>
                  <span class="text-xs font-bold uppercase tracking-wider" :style="{ color: levelColor(lesson.level) }">{{ levelLabel(lesson.level) }}</span>
                </div>
                <span class="text-xs font-bold text-muted-foreground uppercase tracking-wider">{{ lesson.lineCount }} câu</span>
              </div>
              <h3 class="font-black text-lg uppercase leading-snug text-foreground">{{ lesson.title }}</h3>
              <p v-if="lesson.description" class="text-sm font-medium text-muted-foreground leading-relaxed line-clamp-2 flex-1">{{ lesson.description }}</p>
              <AppButton class="w-full mt-auto" variant="tertiary" size="sm">Bắt đầu học</AppButton>
            </div>
          </router-link>
        </div>

        <Pagination v-if="totalPages > 1" :total-pages="totalPages" :current-page="currentPage" @page-changed="goToPage" />
      </template>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import videoLessonService from '@/services/videoLessonService'
import { AppButton } from '@/components/ui'
import Pagination from '@/components/common/Pagination.vue'
import { levelColor, levelLabel } from '@/utils/lessonLevels'

const router = useRouter()
const levels = [
  { value: '', label: 'Tất cả' },
  { value: 'ELEMENTARY', label: 'A1–A2' },
  { value: 'PRE_INTERMEDIATE', label: 'B1' },
  { value: 'INTERMEDIATE', label: 'B2' },
  { value: 'UPPER_INTERMEDIATE', label: 'C1+' }
]

const lessons = ref([])
// YouTube thumbnails 404 for some videos (private/removed); fall back to a
// neutral placeholder instead of a broken image icon.
const brokenThumbs = ref([])
const totalPages = ref(1)
const currentPage = ref(0)
const activeLevel = ref('')
const loading = ref(true)
const error = ref('')

onMounted(fetchPage)

async function fetchPage() {
  loading.value = true
  error.value = ''
  try {
    const data = await videoLessonService.list(currentPage.value, 12, activeLevel.value)
    lessons.value = data.content || []
    totalPages.value = data.totalPages || 1
  } catch (e) {
    error.value = e.response?.data?.detail || e.response?.data?.message || 'Không tải được danh sách video.'
  } finally {
    loading.value = false
  }
}

function selectLevel(value) {
  activeLevel.value = value
  currentPage.value = 0
  fetchPage()
}

function goToPage(page) {
  currentPage.value = page - 1
  fetchPage()
  router.push({ query: {} })
}

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}
</script>
