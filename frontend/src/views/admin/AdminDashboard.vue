<template>
  <div class="space-y-8">
    <PageHeader title="Dashboard" subtitle="Tổng quan hệ thống" :icon="LayoutDashboard" iconBg="bg-accent" iconColor="text-white" />

    <div v-if="loading" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" role="status" aria-label="Đang tải dashboard">
      <div v-for="index in 4" :key="index" class="h-32 animate-pulse border-2 border-foreground/10 bg-card/60"></div>
    </div>

    <div v-else-if="error" class="border-2 border-danger bg-danger/10 p-6 text-danger" role="alert">
      <p class="font-black">Không tải được dữ liệu dashboard.</p>
      <p class="mt-1 text-sm">{{ error }}</p>
      <AppButton class="mt-4" variant="secondary" size="sm" @click="loadStats">Thử lại</AppButton>
    </div>

    <template v-else>
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Người dùng" :value="stats.totalUsers || 0" :icon="Users" iconBg="bg-accent/10" iconColor="text-accent" />
        <StatCard label="Bài học" :value="stats.totalLessons || 0" :icon="BookOpen" iconBg="bg-secondary/10" iconColor="text-secondary" />
        <StatCard label="Bài tập" :value="stats.totalExercises || 0" :icon="FileText" iconBg="bg-tertiary/10" iconColor="text-tertiary" />
        <StatCard label="Bài nộp" :value="stats.totalSubmissions || 0" :icon="Award" iconBg="bg-quaternary/10" iconColor="text-quaternary" />
      </div>

      <div class="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <section class="border-2 border-foreground bg-card p-6 shadow-pop-sm" aria-labelledby="inventory-chart-title">
          <div class="flex flex-wrap items-end justify-between gap-4 border-b-2 border-foreground pb-5">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.2em] text-muted-foreground">Content inventory</p>
              <h2 id="inventory-chart-title" class="mt-2 text-2xl font-black">Khối lượng nội dung</h2>
            </div>
            <span class="text-xs font-black uppercase tracking-wider text-muted-foreground">Cập nhật theo dữ liệu hiện tại</span>
          </div>
          <div class="mt-7 space-y-5">
            <div v-for="item in contentBars" :key="item.label">
              <div class="mb-2 flex items-center justify-between gap-4 text-sm font-black">
                <span>{{ item.label }}</span>
                <span class="tabular-nums text-muted-foreground">{{ item.value }}</span>
              </div>
              <div class="h-3 border-2 border-foreground bg-foreground/5">
                <div class="h-full transition-all duration-500" :style="{ width: `${item.width}%`, backgroundColor: item.color }"></div>
              </div>
            </div>
          </div>
        </section>

        <section class="border-2 border-foreground bg-foreground p-6 text-white" aria-labelledby="activity-chart-title">
          <div class="flex items-end justify-between gap-4 border-b border-white/20 pb-5">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.2em] text-white/60">User health</p>
              <h2 id="activity-chart-title" class="mt-2 text-2xl font-black">Mức độ hoạt động</h2>
            </div>
            <span class="text-xs font-black uppercase tracking-wider text-white/60">7 ngày gần nhất</span>
          </div>
          <div class="mt-7 grid gap-6 sm:grid-cols-[12rem_1fr] sm:items-center">
            <div class="relative mx-auto h-44 w-44 rounded-full" :style="donutStyle">
              <div class="absolute inset-7 flex items-center justify-center rounded-full border-2 border-white/20 bg-foreground text-center">
                <div>
                  <p class="text-4xl font-black tabular-nums">{{ activeRate }}%</p>
                  <p class="text-[10px] font-black uppercase tracking-wider text-white/60">đang hoạt động</p>
                </div>
              </div>
            </div>
            <dl class="space-y-4 text-sm">
              <div class="flex items-center justify-between gap-4 border-b border-white/20 pb-3"><dt class="text-white/70">Người dùng hoạt động</dt><dd class="font-black tabular-nums">{{ stats.activeUsers || 0 }}</dd></div>
              <div class="flex items-center justify-between gap-4 border-b border-white/20 pb-3"><dt class="text-white/70">Học trong 7 ngày</dt><dd class="font-black tabular-nums">{{ stats.recentUsers || 0 }}</dd></div>
              <div class="flex items-center justify-between gap-4"><dt class="text-white/70">Chưa hoạt động</dt><dd class="font-black tabular-nums">{{ inactiveUsers }}</dd></div>
            </dl>
          </div>
        </section>
      </div>

      <section class="grid gap-4 md:grid-cols-3" aria-label="Chỉ số vận hành">
        <article class="border-t-4 border-accent bg-card p-5">
          <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Tỷ lệ hoạt động</p>
          <p class="mt-2 text-4xl font-black tabular-nums">{{ activeRate }}%</p>
          <p class="mt-2 text-sm leading-relaxed text-muted-foreground">Tỷ lệ người dùng đang có hoạt động gần đây.</p>
        </article>
        <article class="border-t-4 border-secondary bg-card p-5">
          <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Bài tập trên bài học</p>
          <p class="mt-2 text-4xl font-black tabular-nums">{{ exercisePerLesson }}</p>
          <p class="mt-2 text-sm leading-relaxed text-muted-foreground">Mật độ bài tập trung bình trên mỗi bài học.</p>
        </article>
        <article class="border-t-4 border-tertiary bg-card p-5">
          <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Bài nộp mỗi người</p>
          <p class="mt-2 text-4xl font-black tabular-nums">{{ submissionsPerUser }}</p>
          <p class="mt-2 text-sm leading-relaxed text-muted-foreground">Tín hiệu tham gia học tập từ toàn hệ thống.</p>
        </article>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { adminService } from '@/services/adminService'
import { LayoutDashboard, Users, BookOpen, FileText, Award } from 'lucide-vue-next'
import PageHeader from '@/components/layout/PageHeader.vue'
import StatCard from '@/components/layout/StatCard.vue'
import AppButton from '@/components/ui/AppButton.vue'

const stats = ref({})
const loading = ref(true)
const error = ref('')

const contentBars = computed(() => {
  const values = [
    { label: 'Bài học', value: Number(stats.value.totalLessons || 0), color: 'hsl(var(--accent))' },
    { label: 'Từ vựng', value: Number(stats.value.totalVocabulary || 0), color: 'hsl(var(--secondary))' },
    { label: 'Bài tập', value: Number(stats.value.totalExercises || 0), color: 'hsl(var(--tertiary))' },
    { label: 'Bài nộp', value: Number(stats.value.totalSubmissions || 0), color: 'hsl(var(--quaternary))' }
  ]
  const max = Math.max(...values.map(item => item.value), 1)
  return values.map(item => ({ ...item, width: item.value ? Math.max((item.value / max) * 100, 4) : 0 }))
})

const activeRate = computed(() => {
  const total = Number(stats.value.totalUsers || 0)
  if (!total) return 0
  return Math.min(Math.round((Number(stats.value.activeUsers || 0) / total) * 100), 100)
})
const inactiveUsers = computed(() => Math.max(Number(stats.value.totalUsers || 0) - Number(stats.value.activeUsers || 0), 0))
const exercisePerLesson = computed(() => {
  const lessons = Number(stats.value.totalLessons || 0)
  return lessons ? (Number(stats.value.totalExercises || 0) / lessons).toFixed(1) : '0.0'
})
const submissionsPerUser = computed(() => {
  const users = Number(stats.value.totalUsers || 0)
  return users ? (Number(stats.value.totalSubmissions || 0) / users).toFixed(1) : '0.0'
})
const donutStyle = computed(() => ({
  background: `conic-gradient(hsl(var(--accent)) 0 ${activeRate.value}%, hsl(var(--foreground) / 0.12) ${activeRate.value}% 100%)`
}))

onMounted(loadStats)

async function loadStats() {
  loading.value = true
  error.value = ''
  try {
    stats.value = await adminService.getDashboardStats()
  } catch (cause) {
    error.value = cause.response?.data?.detail || 'Không thể tải thống kê quản trị.'
  } finally {
    loading.value = false
  }
}
</script>