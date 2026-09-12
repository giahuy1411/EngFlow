<template>
  <!-- audit-v5: inline font-family removed — body already inherits Be Vietnam Pro
       via --geo-font (main.css); the literal bypassed the token. -->
  <div class="flex h-screen bg-geo-bg overflow-hidden">
    <!-- audit-v7 F67: sidebar w-72 cứng + h-screen overflow-hidden làm admin
         không dùng được trên mobile (375px không còn chỗ cho content, không có
         cách mở menu). Nay là off-canvas drawer dưới lg, có overlay đóng lại. -->
    <div v-if="sidebarOpen" class="fixed inset-0 z-40 bg-foreground/50 lg:hidden" @click="sidebarOpen = false"></div>
    <aside
      class="w-72 bg-foreground text-white flex flex-col flex-shrink-0 border-r-2 border-foreground relative z-50 max-lg:fixed max-lg:inset-y-0 max-lg:left-0 max-lg:transition-transform max-lg:duration-200"
      :class="sidebarOpen ? 'max-lg:translate-x-0' : 'max-lg:-translate-x-full'"
      aria-label="Menu quản trị"
    >
      <!-- Decorative shapes -->
      <div class="absolute top-4 right-4 w-3 h-3 rounded-full bg-accent"></div>
      <div class="absolute top-4 right-10 w-3 h-3 bg-secondary"></div>
      <div class="absolute top-4 right-16 w-3 h-3 bg-tertiary rotate-12 rounded-sm"></div>

      <!-- Logo -->
      <div class="h-24 flex items-center justify-center border-b-2 border-white/20 relative overflow-hidden">
        <div class="absolute -left-6 -top-6 w-16 h-16 bg-accent/20 rounded-full"></div>
        <router-link to="/" class="flex items-center gap-3 group relative z-10">
          <div class="relative w-10 h-10">
            <div class="w-4 h-4 bg-accent border-2 border-white absolute top-0 left-0 rounded-md"></div>
            <div class="w-4 h-4 bg-tertiary border-2 border-white absolute bottom-0 right-0 rotate-12 rounded-full"></div>
            <div class="w-3 h-3 rounded-full bg-secondary border-2 border-white absolute top-1 right-0"></div>
          </div>
          <span class="font-black text-3xl uppercase tracking-tighter group-hover:text-tertiary transition-colors">EngFlow</span>
        </router-link>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 overflow-y-auto py-6 px-3 relative">
        <div class="absolute right-0 top-24 w-1 h-32 bg-accent"></div>
        <ul class="space-y-1">
          <li v-for="item in navItems" :key="item.to">
            <router-link
              :to="item.to"
              class="flex items-center gap-4 px-4 py-3 font-bold uppercase text-sm tracking-wider border-2 border-transparent transition-all duration-200 relative group rounded-md"
              :active-class="`${item.activeClass}`"
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

      <!-- Admin User Footer -->
      <div class="p-5 border-t-2 border-white/20 relative">
        <div class="absolute -top-1 left-0 w-full h-1 bg-tertiary/50"></div>
        <div class="flex items-center gap-4">
          <div class="relative">
            <img
              :src="auth.user?.avatarUrl || 'https://api.dicebear.com/7.x/identicon/svg?seed=admin'"
              class="w-11 h-11 rounded-md border-2 border-white shadow-pop-sm"
              alt="admin"
            />
            <div class="absolute -bottom-1 -right-1 w-4 h-4 bg-quaternary border-2 border-white rounded-full"></div>
          </div>
          <div class="flex flex-col">
            <span class="font-bold text-sm leading-tight max-w-36 truncate">{{ auth.user?.username || 'Admin' }}</span>
            <span class="font-black text-tertiary tracking-widest uppercase text-xs">Administrator</span>
          </div>
        </div>
        <button @click="handleLogout"
          class="mt-4 w-full py-3 border-2 border-white font-bold text-sm tracking-wider transition-all duration-200 hover:bg-secondary hover:text-white hover:border-secondary rounded-md active:translate-x-0.5 active:translate-y-0.5"
        >
          <div class="flex items-center justify-center gap-2">
            <span class="w-2 h-2 bg-current rounded-full"></span>
            <span class="w-2 h-2 bg-current rotate-45"></span>
            <span>Đăng xuất</span>
          </div>
        </button>
      </div>
    </aside>

    <!-- Main Content Area -->
    <main class="flex-1 flex flex-col overflow-hidden">
      <!-- Top Bar -->
      <header class="h-24 border-b-2 border-foreground bg-card flex items-center justify-between px-10 max-lg:px-4 shrink-0 relative">
        <div class="absolute left-0 top-0 w-1 h-full bg-accent"></div>
        <div class="flex items-center gap-4">
          <button
            type="button"
            class="lg:hidden w-10 h-10 border-2 border-foreground rounded-md flex items-center justify-center bg-card shadow-pop-sm"
            aria-label="Mở menu quản trị"
            :aria-expanded="sidebarOpen"
            @click="sidebarOpen = !sidebarOpen"
          >
            <Menu class="w-5 h-5" />
          </button>
          <div class="w-3 h-3 bg-tertiary rounded-blob"></div>
          <h1 class="font-black text-2xl tracking-tighter">{{ pageTitle }}</h1>
        </div>
        <div class="flex items-center gap-3">
          <div class="w-3 h-3 rounded-full bg-secondary"></div>
          <div class="w-6 h-0.5 bg-foreground/20"></div>
          <span class="font-bold text-xs tracking-widest text-muted-foreground">Admin Panel</span>
        </div>
      </header>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto p-10 max-lg:p-4 bg-geo-bg relative">
        <!-- Decorative Background Shapes -->
        <div class="absolute top-10 right-10 w-24 h-24 border-2 border-foreground/5 rounded-blob pointer-events-none"></div>
        <div class="absolute bottom-10 left-10 w-16 h-16 border-2 border-foreground/5 rotate-12 rounded-md pointer-events-none"></div>
        <router-view></router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import {
  LayoutDashboard, BookOpen, PenTool, Users, CheckSquare, Clapperboard, AudioLines, Mic, Menu
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// audit-v7 F67: off-canvas admin sidebar (mobile); đóng lại mỗi lần đổi trang.
const sidebarOpen = ref(false)
watch(() => route.fullPath, () => { sidebarOpen.value = false })

const navItems = [
  { to: '/admin/dashboard', icon: LayoutDashboard, label: 'Tổng quan', activeClass: 'bg-accent border-foreground text-white shadow-[4px_4px_0px_0px_white] border-2 rounded-md' },
  { to: '/admin/lessons', icon: BookOpen, label: 'Bài học', activeClass: 'bg-tertiary border-foreground text-foreground shadow-[4px_4px_0px_0px_white] border-2 rounded-md' },
  { to: '/admin/exercises', icon: PenTool, label: 'Bài tập', activeClass: 'bg-quaternary border-foreground text-foreground shadow-[4px_4px_0px_0px_white] border-2 rounded-md' },
  { to: '/admin/speaking-submissions', icon: CheckSquare, label: 'Chấm bài', activeClass: 'bg-secondary border-foreground text-white shadow-[4px_4px_0px_0px_white] border-2 rounded-md' },
  { to: '/admin/speaking-prompts', icon: Mic, label: 'Đề luyện nói', activeClass: 'bg-tertiary border-foreground text-foreground shadow-[4px_4px_0px_0px_white] border-2 rounded-md' },
  { to: '/admin/videos', icon: Clapperboard, label: 'Video học tập', activeClass: 'bg-quaternary border-foreground text-foreground shadow-[4px_4px_0px_0px_white] border-2 rounded-md' },
  { to: '/admin/video-attempts', icon: AudioLines, label: 'Chấm shadowing', activeClass: 'bg-secondary border-foreground text-white shadow-[4px_4px_0px_0px_white] border-2 rounded-md' },
  { to: '/admin/users', icon: Users, label: 'Người dùng', activeClass: 'bg-accent border-foreground text-white shadow-[4px_4px_0px_0px_white] border-2 rounded-md' },
]

const pageTitles = {
  AdminDashboard: 'Tổng quan',
  AdminLessons: 'Bài học',
  AdminExercises: 'Bài tập',
  AdminSpeakingSubmissions: 'Chấm bài',
  AdminSpeakingPrompts: 'Đề luyện nói',
  AdminVideoLessons: 'Video học tập',
  AdminVideoAttempts: 'Chấm shadowing',
  AdminLessonBuilder: 'Xây dựng bài học',
  AdminUsers: 'Người dùng',
}

const pageTitle = computed(() => pageTitles[route.name] || 'Admin')

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>
