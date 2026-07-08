<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-accent border-2 border-foreground flex items-center justify-center rounded-md">
          <LayoutDashboard class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tight">Dashboard</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-muted-foreground">Tổng quan hệ thống</p>
        </div>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-card border-2 border-foreground rounded-md p-5 shadow-pop-lg">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 bg-accent/10 border-2 border-foreground rounded-md flex items-center justify-center">
            <Users class="w-5 h-5 text-accent" />
          </div>
          <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Người dùng</span>
        </div>
        <p class="font-black text-3xl">{{ stats.totalUsers || 0 }}</p>
      </div>
      <div class="bg-card border-2 border-foreground rounded-md p-5 shadow-pop-lg">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 bg-secondary/10 border-2 border-foreground rounded-md flex items-center justify-center">
            <BookOpen class="w-5 h-5 text-secondary" />
          </div>
          <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Bài học</span>
        </div>
        <p class="font-black text-3xl">{{ stats.totalLessons || 0 }}</p>
      </div>
      <div class="bg-card border-2 border-foreground rounded-md p-5 shadow-pop-lg">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 bg-tertiary/10 border-2 border-foreground rounded-md flex items-center justify-center">
            <FileText class="w-5 h-5 text-tertiary" />
          </div>
          <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Bài tập</span>
        </div>
        <p class="font-black text-3xl">{{ stats.totalExercises || 0 }}</p>
      </div>
      <div class="bg-card border-2 border-foreground rounded-md p-5 shadow-pop-lg">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 bg-quaternary/10 border-2 border-foreground rounded-md flex items-center justify-center">
            <Award class="w-5 h-5 text-quaternary" />
          </div>
          <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Bài nộp</span>
        </div>
        <p class="font-black text-3xl">{{ stats.totalSubmissions || 0 }}</p>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-16">
      <div class="w-8 h-8 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminService } from '@/services/adminService'
import { LayoutDashboard, Users, BookOpen, FileText, Award } from 'lucide-vue-next'

const stats = ref({})
const loading = ref(true)

onMounted(async () => {
  try {
    stats.value = await adminService.getDashboardStats()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>
