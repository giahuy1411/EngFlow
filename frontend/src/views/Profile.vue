<template>
  <div class="bg-geo-bg min-h-screen">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <!-- Profile Header -->
      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl mb-8 relative overflow-hidden">
        <div class="absolute top-4 right-4 w-12 h-12 bg-tertiary/20 border-2 border-foreground/10 rounded-full pointer-events-none"></div>
        <div class="absolute bottom-4 left-1/3 w-8 h-8 bg-secondary/10 border-2 border-foreground/10 rounded-md rotate-12 pointer-events-none"></div>

        <div class="flex flex-col md:flex-row items-center gap-8 relative z-10">
          <div class="relative">
            <img :src="user.avatarUrl || 'https://api.dicebear.com/7.x/thumbs/svg?seed=default'"
              class="w-28 h-28 border-2 border-foreground object-cover rounded-md shadow-pop-sm" alt="avatar" />
            <div class="absolute -bottom-2 -right-2 w-7 h-7 bg-quaternary border-2 border-foreground rounded-full"></div>
          </div>
          <div class="flex-1 text-center md:text-left">
            <h1 class="font-black text-3xl uppercase tracking-tight">{{ user.fullName || user.username }}</h1>
            <p class="font-bold text-sm text-muted-foreground mt-1">@{{ user.username }}</p>
          </div>
          <div class="flex gap-4">
            <div class="text-center">
              <p class="font-black text-2xl text-accent">{{ user.currentStreak || 0 }}</p>
              <p class="font-bold text-[10px] uppercase tracking-widest text-muted-foreground">Streak</p>
            </div>
            <div class="text-center">
              <p class="font-black text-2xl text-secondary">{{ user.totalPoints || 0 }}</p>
              <p class="font-bold text-[10px] uppercase tracking-widest text-muted-foreground">Điểm</p>
            </div>
            <div class="text-center">
              <p class="font-black text-2xl text-tertiary">{{ user.level || 'N/A' }}</p>
              <p class="font-bold text-[10px] uppercase tracking-widest text-muted-foreground">Cấp độ</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Stats Grid -->
      <div class="grid md:grid-cols-3 gap-6 mb-8">
        <div class="bg-card border-2 border-foreground rounded-md p-6 shadow-pop-lg hover:-translate-y-1 transition-all duration-300">
          <div class="flex items-center gap-3 mb-3">
            <div class="w-10 h-10 bg-accent/10 border-2 border-foreground rounded-full flex items-center justify-center">
              <BookOpen class="w-5 h-5 text-accent" />
            </div>
            <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Bài học</span>
          </div>
          <p class="font-black text-3xl">{{ lessonCount }}</p>
        </div>
        <div class="bg-card border-2 border-foreground rounded-md p-6 shadow-pop-lg hover:-translate-y-1 transition-all duration-300">
          <div class="flex items-center gap-3 mb-3">
            <div class="w-10 h-10 bg-secondary/10 border-2 border-foreground rounded-full flex items-center justify-center">
              <Zap class="w-5 h-5 text-secondary" />
            </div>
            <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Streak</span>
          </div>
          <p class="font-black text-3xl">{{ currentStreak }}</p>
        </div>
        <div class="bg-card border-2 border-foreground rounded-md p-6 shadow-pop-lg hover:-translate-y-1 transition-all duration-300">
          <div class="flex items-center gap-3 mb-3">
            <div class="w-10 h-10 bg-tertiary/10 border-2 border-foreground rounded-full flex items-center justify-center">
              <Award class="w-5 h-5 text-tertiary" />
            </div>
            <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Thành tích</span>
          </div>
          <p class="font-black text-3xl">{{ achievementCount }}</p>
        </div>
      </div>

      <!-- Streak Calendar -->
      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl mb-8">
        <h2 class="font-black text-xl uppercase tracking-tight mb-6 flex items-center gap-3">
          <Flame class="w-6 h-6 text-tertiary" />
          Lịch học
        </h2>
        <StreakCalendar :data="streakData" />
      </div>

      <!-- Achievements -->
      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl">
        <h2 class="font-black text-xl uppercase tracking-tight mb-6">Thành tích</h2>
        <div v-if="achievements.length === 0" class="text-center py-8">
          <p class="font-bold text-muted-foreground">Chưa có thành tích nào</p>
        </div>
        <div v-else class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div v-for="a in achievements" :key="a.id"
            class="border-2 border-foreground rounded-md p-4 text-center bg-geo-bg hover:shadow-pop-sm transition-all"
          >
            <div class="w-10 h-10 mx-auto mb-2 bg-accent/10 border-2 border-foreground rounded-full flex items-center justify-center">
              <Trophy class="w-5 h-5 text-accent" />
            </div>
            <p class="font-bold text-sm">{{ a.name }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import streakService from '@/services/streakService'
import { BookOpen, Zap, Award, Flame, Trophy } from 'lucide-vue-next'
import StreakCalendar from '@/components/bauhaus/StreakCalendar.vue'

const auth = useAuthStore()
const user = ref(auth.user || {})
const lessonCount = ref(0)
const currentStreak = ref(0)
const achievementCount = ref(0)
const streakData = ref([])
const achievements = ref([])

onMounted(async () => {
  try {
    const streak = await streakService.getCurrentStreak()
    currentStreak.value = streak.currentStreak || 0
  } catch (e) { /* ignore */ }
})
</script>
