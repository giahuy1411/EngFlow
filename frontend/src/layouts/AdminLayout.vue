<template>
  <div class="flex h-screen bg-background overflow-hidden" style="font-family: 'Be Vietnam Pro', sans-serif">
    <!-- Sidebar -->
    <aside class="w-72 bg-foreground text-white flex flex-col flex-shrink-0 border-r-4 border-foreground relative">
      <div class="absolute top-4 right-4 w-3 h-3 rounded-full bg-primary-red"></div>
      <div class="absolute top-4 right-10 w-3 h-3 bg-primary-blue"></div>

      <!-- Logo -->
      <div class="h-24 flex items-center justify-center border-b-4 border-white relative overflow-hidden">
        <div class="absolute -left-6 -top-6 w-16 h-16 bg-primary-red/20 rounded-full"></div>
        <router-link to="/" class="flex items-center gap-3 group relative z-10">
          <div class="relative w-10 h-10">
            <div class="w-4 h-4 bg-primary-red border-2 border-white absolute top-0 left-0"></div>
            <div class="w-4 h-4 bg-primary-yellow border-2 border-white absolute bottom-0 right-0 rotate-12"></div>
            <div class="w-3 h-3 rounded-full bg-primary-blue border-2 border-white absolute top-1 right-0"></div>
          </div>
          <span class="font-black text-3xl uppercase tracking-tighter">EngFlow</span>
        </router-link>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 overflow-y-auto py-6 px-3 relative">
        <div class="absolute right-0 top-24 w-1 h-32 bg-primary-red"></div>
        <ul class="space-y-1">
          <li v-for="item in navItems" :key="item.to">
            <router-link
              :to="item.to"
              class="flex items-center gap-4 px-4 py-3 font-bold uppercase text-sm tracking-wider border-2 border-transparent transition-all duration-200 relative group"
              :active-class="item.activeClass"
            >
              <component :is="item.icon" class="w-5 h-5 shrink-0" />
              <span>{{ item.label }}</span>
              <div class="absolute right-2 w-2 h-2 border border-white/30 group-hover:bg-white/50 transition-colors"
                   :class="item.to === '/admin/dashboard' ? 'rounded-full' : 'rotate-45'">
              </div>
            </router-link>
          </li>
        </ul>
      </nav>

      <!-- User Footer -->
      <div class="p-5 border-t-4 border-white relative">
        <div class="absolute bottom-16 left-0 w-full h-1 bg-primary-yellow/50"></div>
        <div class="flex items-center gap-4">
          <div class="relative">
            <img :src="auth.user?.avatarUrl || 'https://api.dicebear.com/7.x/identicon/svg?seed=admin'"
                 class="w-11 h-11 rounded-full border-2 border-white" />
            <div class="absolute -bottom-1 -right-1 w-4 h-4 bg-primary-red border-2 border-white rounded-full"></div>
          </div>
          <div class="flex flex-col">
            <span class="font-bold text-sm uppercase leading-tight truncate w-36">{{ auth.user?.username }}</span>
            <span class="font-black text-[10px] text-primary-yellow tracking-widest uppercase">Administrator</span>
          </div>
        </div>
        <button @click="handleLogout"
                class="mt-4 w-full py-3 border-2 border-white font-bold uppercase text-sm tracking-wider transition-all duration-200
                       hover:bg-white hover:text-foreground active:translate-x-0.5 active:translate-y-0.5">
          <div class="flex items-center justify-center gap-2">
            <span class="w-2 h-2 bg-current"></span>
            Thoát
            <span class="w-2 h-2 bg-current"></span>
          </div>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 flex flex-col overflow-hidden">
      <!-- Header Bar -->
      <header class="h-24 border-b-4 border-foreground bg-white flex items-center justify-between px-10 shrink-0 relative">
        <div class="absolute left-0 top-0 w-1 h-full bg-primary-red"></div>
        <div class="flex items-center gap-4">
          <div class="w-3 h-3 bg-primary-red"></div>
          <h1 class="font-black text-3xl uppercase tracking-tighter">{{ pageTitle }}</h1>
        </div>
        <div class="flex items-center gap-3">
          <div class="w-3 h-3 rounded-full bg-primary-blue"></div>
          <div class="w-6 h-0.5 bg-foreground"></div>
          <span class="font-bold text-xs uppercase tracking-widest text-gray-500">Admin Panel</span>
        </div>
      </header>

      <!-- Scrollable Content -->
      <div class="flex-1 overflow-y-auto p-10 bg-background relative">
        <div class="absolute top-10 right-10 w-24 h-24 border-4 border-foreground/5 rounded-full pointer-events-none"></div>
        <div class="absolute bottom-10 left-10 w-16 h-16 border-4 border-foreground/5 rotate-45 pointer-events-none"></div>
        <router-view></router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import {
  LayoutDashboard, BookOpen, PenTool, Users, Trophy, CheckSquare
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const navItems = [
  { to: '/admin/dashboard', icon: LayoutDashboard, label: 'Tổng quan', activeClass: 'bg-primary-red border-foreground text-white shadow-[4px_4px_0px_0px_rgba(255,255,255,0.3)]' },
  { to: '/admin/lessons', icon: BookOpen, label: 'Bài học', activeClass: 'bg-primary-yellow border-foreground text-foreground shadow-[4px_4px_0px_0px_rgba(255,255,255,0.3)]' },
  { to: '/admin/exercises', icon: PenTool, label: 'Bài tập', activeClass: 'bg-primary-red border-foreground text-white shadow-[4px_4px_0px_0px_rgba(255,255,255,0.3)]' },
  { to: '/admin/submissions', icon: CheckSquare, label: 'Chấm bài', activeClass: 'bg-primary-yellow border-foreground text-foreground shadow-[4px_4px_0px_0px_rgba(255,255,255,0.3)]' },
  { to: '/admin/users', icon: Users, label: 'Người dùng', activeClass: 'bg-white border-foreground text-foreground shadow-[4px_4px_0px_0px_rgba(255,255,255,0.3)]' },
  { to: '/admin/achievements', icon: Trophy, label: 'Thành tích', activeClass: 'bg-primary-blue border-foreground text-white shadow-[4px_4px_0px_0px_rgba(255,255,255,0.3)]' }
]

const pageTitle = computed(() => ({
  AdminDashboard: 'Tổng quan',
  AdminLessons: 'Quản lý Bài học',
  AdminExercises: 'Quản lý Bài tập',
  AdminSubmissions: 'Chấm bài học viên',
  AdminUsers: 'Quản lý Người dùng',
  AdminAchievements: 'Quản lý Thành tích'
})[route.name] || 'Quản trị')

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>
