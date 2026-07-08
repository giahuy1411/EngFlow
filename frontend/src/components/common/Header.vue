<template>
  <header class="bg-card border-b-2 border-foreground sticky top-0 z-50 shadow-pop-sm">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between h-16 md:h-20">
        <!-- Logo -->
        <router-link to="/" class="flex items-center gap-3 group shrink-0">
          <div class="relative w-10 h-10">
            <div class="w-4 h-4 bg-accent border-2 border-foreground absolute top-0 left-0 rounded-md"></div>
            <div class="w-4 h-4 bg-tertiary border-2 border-foreground absolute bottom-0 right-0 rotate-12 rounded-full"></div>
            <div class="w-3 h-3 rounded-full bg-secondary border-2 border-foreground absolute top-1 right-0"></div>
          </div>
          <span class="font-black text-2xl tracking-tighter group-hover:text-accent transition-colors">EngFlow</span>
        </router-link>

        <!-- Desktop Nav -->
        <nav class="hidden md:flex items-center gap-1">
          <router-link
            v-for="item in navItems" :key="item.to" :to="item.to"
            class="px-4 py-2 font-bold text-sm uppercase tracking-wider rounded-full border-2 border-transparent transition-all duration-200 hover:bg-tertiary/20 hover:border-foreground"
            :class="$route.path === item.to ? 'bg-accent text-white border-foreground shadow-pop-sm' : 'text-foreground'"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <!-- Right Side -->
        <div class="flex items-center gap-3">
          <!-- Auth buttons -->
          <template v-if="!auth.isLoggedIn">
            <router-link to="/login"
              class="hidden sm:inline-flex px-5 py-2 font-bold text-xs uppercase tracking-wider border-2 border-foreground rounded-full hover:bg-tertiary transition-all duration-200"
            >Đăng nhập</router-link>
            <router-link to="/register"
              class="hidden sm:inline-flex px-5 py-2 font-bold text-xs uppercase tracking-wider bg-accent text-white border-2 border-foreground rounded-full shadow-pop-sm hover:shadow-pop hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all duration-300 ease-bounce"
            >Đăng ký</router-link>
          </template>

          <!-- User Menu (desktop) -->
          <div v-else class="hidden md:flex items-center gap-4">
            <router-link to="/profile" class="flex items-center gap-2 group">
              <div class="relative">
                <img
                  :src="auth.user?.avatarUrl || 'https://api.dicebear.com/7.x/thumbs/svg?seed=default'"
                  class="w-9 h-9 rounded-full border-2 border-foreground object-cover"
                  alt="avatar"
                />
                <div class="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 bg-quaternary border-2 border-foreground rounded-full"></div>
              </div>
              <span class="font-bold text-sm max-w-[120px] truncate">{{ auth.user?.username }}</span>
            </router-link>
            <button @click="handleLogout"
              class="px-4 py-2 font-bold text-xs uppercase tracking-wider border-2 border-foreground rounded-full hover:bg-secondary hover:text-white transition-all duration-200"
            >Thoát</button>
          </div>

          <!-- Mobile Hamburger -->
          <button @click="mobileOpen = !mobileOpen"
            class="md:hidden w-11 h-11 flex items-center justify-center border-2 border-foreground rounded-full bg-card transition-all duration-200"
            :class="mobileOpen ? 'bg-accent text-white' : ''"
          >
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path v-if="!mobileOpen" stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Mobile Menu -->
    <div v-if="mobileOpen"
      class="md:hidden border-t-2 border-foreground bg-card animate-pop-in"
    >
      <nav class="px-4 py-4 space-y-1">
        <router-link
          v-for="item in navItems" :key="item.to" :to="item.to"
          @click="mobileOpen = false"
          class="block px-4 py-3 font-bold text-sm uppercase tracking-wider rounded-full border-2 border-transparent transition-all"
          :class="$route.path === item.to ? 'bg-accent text-white border-foreground' : 'hover:bg-tertiary/20 hover:border-foreground'"
        >{{ item.label }}</router-link>

        <div class="border-t-2 border-foreground/20 my-3"></div>

        <template v-if="!auth.isLoggedIn">
          <router-link to="/login" @click="mobileOpen = false"
            class="block px-4 py-3 font-bold text-sm uppercase tracking-wider text-center border-2 border-foreground rounded-full"
          >Đăng nhập</router-link>
          <router-link to="/register" @click="mobileOpen = false"
            class="block px-4 py-3 font-bold text-sm uppercase tracking-wider text-center bg-accent text-white border-2 border-foreground rounded-full shadow-pop-sm mt-2"
          >Đăng ký</router-link>
        </template>
        <template v-else>
          <router-link to="/profile" @click="mobileOpen = false"
            class="block px-4 py-3 font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full"
          >Hồ sơ</router-link>
          <button @click="handleLogout"
            class="w-full mt-2 px-4 py-3 font-bold text-sm uppercase tracking-wider border-2 border-foreground rounded-full hover:bg-secondary hover:text-white transition-all"
          >Thoát</button>
        </template>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()
const mobileOpen = ref(false)

const navItems = computed(() => {
  const items = [
    { to: '/', label: 'Trang chủ' },
    { to: '/lessons', label: 'Bài học' },
    { to: '/decks', label: 'Luyện tập' },
    { to: '/leaderboard', label: 'Xếp hạng' },
  ]
  if (auth.isAdmin) {
    items.push({ to: '/admin/dashboard', label: 'Quản trị' })
  }
  return items
})

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>
