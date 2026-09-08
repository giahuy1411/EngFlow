<template>
  <div class="bg-background min-h-screen">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl mb-8 relative overflow-hidden">
        <div class="absolute top-4 right-4 w-12 h-12 bg-tertiary/20 border-2 border-foreground/10 rounded-full pointer-events-none"></div>
        <div class="absolute bottom-4 left-1/3 w-8 h-8 bg-secondary/10 border-2 border-foreground/10 rounded-md rotate-12 pointer-events-none"></div>
        <div class="flex flex-col md:flex-row items-center gap-8 relative z-10">
          <div class="relative">
            <img :src="user.avatarUrl || 'https://api.dicebear.com/7.x/thumbs/svg?seed=default'" class="w-28 h-28 border-2 border-foreground object-cover rounded-md shadow-pop-sm" alt="avatar" />
            <div class="absolute -bottom-2 -right-2 w-7 h-7 bg-quaternary border-2 border-foreground rounded-full"></div>
          </div>
          <div class="flex-1 text-center md:text-left">
            <UserPageHeader eyebrow="Hồ sơ học tập" :divided="false" root-class="justify-items-center md:justify-items-start">
              <template #title><span>{{ user.fullName || user.username }}</span></template>
              <template #accent>@{{ user.username }}</template>
            </UserPageHeader>
          </div>
          <div class="flex gap-4">
            <div class="text-center"><p class="font-black text-2xl text-accent">{{ user.currentStreak || 0 }}</p><p class="font-bold text-[10px] uppercase tracking-widest text-muted-foreground">Streak</p></div>
            <div class="text-center"><p class="font-black text-2xl text-secondary">{{ user.totalPoints || 0 }}</p><p class="font-bold text-[10px] uppercase tracking-widest text-muted-foreground">Điểm</p></div>
            <div class="text-center"><p class="font-black text-2xl text-foreground">{{ user.level || 'N/A' }}</p><p class="font-bold text-[10px] uppercase tracking-widest text-muted-foreground">Cấp độ</p></div>
          </div>
        </div>
      </div>
      <section class="mb-8 border-2 border-foreground bg-card p-6 shadow-pop-xl" aria-labelledby="subscription-title">
        <div class="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div><p class="text-xs font-black uppercase tracking-widest text-muted-foreground">Gói thành viên</p><h2 id="subscription-title" class="mt-1 text-2xl font-black">{{ subscriptionLabel }}</h2><p class="mt-2 text-sm text-muted-foreground">{{ subscriptionDescription }}</p></div>
          <router-link v-if="!premiumStore.isPremium && !auth.isAdmin" to="/premium" class="inline-flex shrink-0 items-center justify-center rounded-full border-2 border-foreground bg-accent px-5 py-3 font-black text-white shadow-pop transition-transform hover:-translate-y-0.5">Nâng cấp Premium</router-link>
          <router-link v-else to="/premium" class="inline-flex shrink-0 items-center justify-center rounded-full border-2 border-foreground bg-tertiary px-5 py-3 font-black shadow-pop transition-transform hover:-translate-y-0.5">Quản lý gói</router-link>
        </div>
      </section>
      <div class="grid md:grid-cols-2 gap-6 mb-8">
        <div class="bg-card border-2 border-foreground rounded-md p-6 shadow-pop-lg hover:-translate-y-1 transition-all duration-300">
          <div class="flex items-center gap-3 mb-3"><div class="w-10 h-10 bg-accent/10 border-2 border-foreground rounded-full flex items-center justify-center"><BookOpen class="w-5 h-5 text-accent" /></div><span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Bài học</span></div>
          <p class="font-black text-3xl">{{ stats.completedLessons }}<span class="text-lg font-bold text-muted-foreground">/{{ stats.totalLessons }}</span></p>
          <div class="mt-3 h-2.5 border-2 border-foreground bg-background rounded-none" role="progressbar" :aria-valuenow="stats.completedLessons" :aria-valuemin="0" :aria-valuemax="stats.totalLessons" aria-label="Tiến độ hoàn thành bài học">
            <div class="h-full bg-accent transition-all duration-700" :style="{ width: `${lessonProgressPercent}%` }"></div>
          </div>
        </div>
        <div class="bg-card border-2 border-foreground rounded-md p-6 shadow-pop-lg hover:-translate-y-1 transition-all duration-300"><div class="flex items-center gap-3 mb-3"><div class="w-10 h-10 bg-secondary/10 border-2 border-foreground rounded-full flex items-center justify-center"><Zap class="w-5 h-5 text-secondary" /></div><span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Streak</span></div><p class="font-black text-3xl">{{ currentStreak }}</p></div>
      </div>
      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl mb-8">
        <h2 class="font-black text-xl uppercase tracking-tight mb-6 flex items-center gap-3"><Flame class="w-6 h-6 text-tertiary" />Lịch học</h2>
        <StreakCalendar :history="streakData" :current-streak="currentStreak" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import { usePremiumStore } from '@/store/modules/premium'
import streakService from '@/services/streakService'
import dashboardService from '@/services/dashboardService'
import { BookOpen, Zap, Flame } from 'lucide-vue-next'
import StreakCalendar from '@/components/common/StreakCalendar.vue'
import UserPageHeader from '@/components/common/UserPageHeader.vue'

const auth = useAuthStore()
const premiumStore = usePremiumStore()
const user = ref(auth.user || {})
const stats = ref({ completedLessons: 0, totalLessons: 0 })
const currentStreak = ref(0)
const streakData = ref([])
const lessonProgressPercent = computed(() => {
  if (!stats.value.totalLessons) return 0
  const percent = Math.min(100, (stats.value.completedLessons / stats.value.totalLessons) * 100)
  return percent.toFixed(2)
})
const subscriptionLabel = computed(() => {
  if (auth.isAdmin) return 'Admin · Toàn quyền'
  return premiumStore.isPremium ? 'Premium đang hoạt động' : 'Gói miễn phí'
})
const subscriptionDescription = computed(() => {
  if (auth.isAdmin) return 'Tài khoản admin được mở toàn bộ tính năng.'
  if (premiumStore.isPremium) return premiumStore.premiumExpiry ? `Có hiệu lực đến ${new Date(premiumStore.premiumExpiry).toLocaleDateString('vi-VN')}.` : 'Đã mở khóa luyện nói và AI không giới hạn.'
  return 'AI miễn phí 5 lượt vĩnh viễn. Luyện nói cần Premium.'
})

onMounted(async () => {
  await premiumStore.checkStatus()
  if (auth.user) {
    auth.user.isPremium = premiumStore.isPremium
    auth.user.premiumExpiry = premiumStore.premiumExpiry
    localStorage.setItem('user', JSON.stringify(auth.user))
  }
  try {
    const streak = await streakService.getCurrentStreak()
    currentStreak.value = streak.currentStreak || 0
  } catch (e) { /* ignore */ }
  try {
    const dashStats = await dashboardService.getStats()
    stats.value = {
      completedLessons: dashStats.completedLessons || 0,
      totalLessons: dashStats.totalLessons || 0
    }
  } catch (e) { /* ignore */ }
  try {
    streakData.value = await streakService.getHistory(30)
  } catch (e) { /* ignore */ }
})
</script>
