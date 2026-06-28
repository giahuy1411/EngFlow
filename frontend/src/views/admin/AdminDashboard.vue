<template>
  <div class="space-y-8">
    <!-- Stats Grid -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 divide-y-4 sm:divide-y-0 sm:divide-x-4 divide-foreground border-4 border-foreground bg-white shadow-[8px_8px_0px_0px_black]">
      <div v-for="(stat, i) in stats" :key="stat.label"
           :class="['p-6 sm:p-8 relative', stat.bg]">
        <div class="flex items-center justify-between mb-4">
          <div class="text-3xl font-black uppercase tracking-tighter" :class="stat.textColor">{{ stat.value }}</div>
          <div :class="['w-10 h-10 border-2 border-foreground flex items-center justify-center',
                        i % 3 === 0 ? 'rounded-full' : i % 3 === 1 ? 'rotate-12' : '']">
            <component :is="stat.icon" class="w-5 h-5" :class="stat.textColor" />
          </div>
        </div>
        <p class="text-sm font-bold uppercase tracking-widest" :class="stat.labelColor">{{ stat.label }}</p>
        <div :class="['absolute top-2 right-2 w-2 h-2', i % 2 === 0 ? 'bg-primary-red rounded-full' : 'bg-primary-blue rotate-45']"></div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Recent Activity -->
      <div class="lg:col-span-2 bg-white border-4 border-foreground shadow-[8px_8px_0px_0px_black] p-6 sm:p-8 relative">
        <div class="absolute -top-1 -right-1 w-6 h-6 bg-primary-blue border-2 border-foreground rounded-full" style="transform:translate(50%,-50%)"></div>
        <h3 class="font-black text-xl uppercase tracking-tighter mb-6 flex items-center gap-3">
          <ActivityIcon class="w-6 h-6 text-primary-red" />
          Hoạt động gần đây
        </h3>
        <div class="divide-y-4 divide-foreground">
          <div v-for="i in 4" :key="i" class="flex items-center gap-4 py-4">
            <div class="w-3 h-3" :class="i % 2 === 0 ? 'bg-primary-blue rotate-45' : 'bg-primary-red rounded-full'"></div>
            <span class="font-bold uppercase text-sm tracking-wider">Hoạt động mới</span>
            <span class="ml-auto text-xs font-bold uppercase text-gray-500">{{ i * 5 }} phút trước</span>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="bg-white border-4 border-foreground shadow-[8px_8px_0px_0px_black] p-6 sm:p-8 relative">
        <div class="absolute -top-1 -right-1 w-6 h-6 bg-primary-yellow border-2 border-foreground rotate-12" style="transform:translate(50%,-50%)"></div>
        <h3 class="font-black text-xl uppercase tracking-tighter mb-6 flex items-center gap-3">
          <ZapIcon class="w-6 h-6 text-primary-yellow" />
          Truy cập nhanh
        </h3>
        <div class="grid grid-cols-2 gap-4">
          <button v-for="action in quickActions" :key="action.label"
                  @click="action.action"
                  class="flex flex-col items-center justify-center gap-2 p-4 border-2 border-foreground font-bold uppercase text-xs tracking-wider
                         hover:-translate-y-1 hover:shadow-[4px_4px_0px_0px_black] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none
                         transition-all duration-200"
                  :class="action.style">
            <component :is="action.icon" class="w-6 h-6" />
            <span>{{ action.label }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- System Info -->
    <div class="bg-foreground border-4 border-foreground p-4 flex items-center justify-between shadow-[6px_6px_0px_0px_black]">
      <div class="flex items-center gap-6 text-white">
        <span class="flex items-center gap-2 font-bold uppercase text-xs tracking-wider">
          <DatabaseIcon class="w-4 h-4" /> Database Online
        </span>
        <span class="flex items-center gap-2 font-bold uppercase text-xs tracking-wider">
          <ServerIcon class="w-4 h-4" /> Server Online
        </span>
      </div>
      <span class="font-bold text-xs uppercase tracking-wider text-gray-500">v1.0.0</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminService } from '@/services/adminService'
import {
  Users, BookOpen, Trophy, Activity as ActivityIcon,
  Zap as ZapIcon, Database as DatabaseIcon, Server as ServerIcon,
  PlusCircle, PenTool, Settings, BarChart3
} from 'lucide-vue-next'

const router = useRouter()

const stats = ref([
  { label: 'Người dùng', value: '—', icon: Users, bg: 'bg-primary-yellow/20 sm:bg-white', textColor: 'text-foreground', labelColor: 'text-foreground/70' },
  { label: 'Bài học', value: '—', icon: BookOpen, bg: 'bg-primary-red/20 sm:bg-white', textColor: 'text-primary-red', labelColor: 'text-primary-red/70' },
  { label: 'Thành tích', value: '—', icon: Trophy, bg: 'bg-foreground/10 sm:bg-white', textColor: 'text-foreground', labelColor: 'text-foreground/70' }
])

const quickActions = [
  { label: 'Bài học mới', icon: PlusCircle, action: () => router.push('/admin/lessons'), style: 'bg-primary-red/20 text-primary-red hover:bg-primary-red/30' },
  { label: 'Bài tập', icon: PenTool, action: () => router.push('/admin/exercises'), style: 'bg-primary-blue/20 text-primary-blue hover:bg-primary-blue/30' },
  { label: 'Cài đặt', icon: Settings, action: () => {}, style: 'bg-foreground/10 text-foreground/70 hover:bg-foreground/20' },
  { label: 'Báo cáo', icon: BarChart3, action: () => {}, style: 'bg-primary-yellow/20 text-foreground hover:bg-primary-yellow/30' }
]

onMounted(async () => {
  try {
    const data = await adminService.getDashboardStats()
    if (data) {
      stats.value[0].value = data.totalUsers ?? '—'
      stats.value[1].value = data.totalLessons ?? '—'
      stats.value[2].value = data.totalAchievements ?? '—'
    }
  } catch { /* keep defaults */ }
})
</script>
