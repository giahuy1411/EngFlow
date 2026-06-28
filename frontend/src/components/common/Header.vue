<template>
  <header :class="[
    'sticky z-50 transition-transform duration-300 left-0 right-0 top-0 border-b-4 border-black bg-white',
    isScrollingDown ? '-translate-y-full' : 'translate-y-0'
  ]">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex justify-between items-center h-20">
        <!-- Geometric Logo -->
        <router-link to="/" class="flex items-center gap-2 group focus:outline-none focus-visible:ring-2 focus-visible:ring-black">
          <div class="flex items-center">
            <div class="w-6 h-6 rounded-full bg-primary-red border-2 border-black -mr-2 relative z-10 group-hover:-translate-y-1 transition-transform"></div>
            <div class="w-6 h-6 rounded-none bg-primary-blue border-2 border-black -mr-2 relative z-20 group-hover:-translate-y-1 transition-transform delay-75"></div>
            <div class="w-6 h-6 bg-primary-yellow border-2 border-black relative z-30 group-hover:-translate-y-1 transition-transform delay-150 clip-triangle"></div>
          </div>
          <span class="font-black text-2xl uppercase tracking-tighter ml-2">EngFlow</span>
        </router-link>

        <!-- Desktop Navigation -->
        <nav class="hidden lg:flex items-center gap-8 font-bold uppercase tracking-wider text-sm mx-8 flex-1 justify-center">
          <router-link to="/search" class="relative py-1 text-foreground hover:text-primary-red transition-colors after:absolute after:-bottom-1 after:left-0 after:h-[3px] after:bg-primary-red after:w-0 hover:after:w-full after:transition-all after:duration-300" active-class="text-primary-red after:w-full">Tra từ</router-link>
          <router-link to="/decks" class="relative py-1 text-foreground hover:text-primary-blue transition-colors after:absolute after:-bottom-1 after:left-0 after:h-[3px] after:bg-primary-blue after:w-0 hover:after:w-full after:transition-all after:duration-300" active-class="text-primary-blue after:w-full">Bộ từ vựng</router-link>
          <router-link to="/leaderboard" class="relative py-1 text-foreground hover:text-primary-yellow transition-colors after:absolute after:-bottom-1 after:left-0 after:h-[3px] after:bg-primary-yellow after:w-0 hover:after:w-full after:transition-all after:duration-300" active-class="text-primary-yellow after:w-full">Xếp hạng</router-link>
          
          <template v-if="auth.isLoggedIn">
            <router-link to="/lessons" class="relative py-1 text-foreground hover:text-primary-red transition-colors after:absolute after:-bottom-1 after:left-0 after:h-[3px] after:bg-primary-red after:w-0 hover:after:w-full after:transition-all after:duration-300" active-class="text-primary-red after:w-full">Bài học</router-link>
            <router-link v-if="auth.isAdmin" to="/admin/dashboard" class="relative py-1 text-foreground hover:text-primary-yellow transition-colors after:absolute after:-bottom-1 after:left-0 after:h-[3px] after:bg-primary-yellow after:w-0 hover:after:w-full after:transition-all after:duration-300" active-class="text-primary-yellow after:w-full">Admin</router-link>
          </template>
        </nav>

        <!-- Actions -->
        <div class="hidden lg:flex items-center gap-4 shrink-0">
          <template v-if="auth.isLoggedIn">
            <!-- User Dropdown & Stats -->
            <div class="relative group">
              <button @click="router.push('/profile')" class="flex items-center gap-3 border-2 border-black bg-background px-3 py-1.5 shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all focus:outline-none">
                <div class="flex flex-col items-end justify-center">
                  <span class="font-black text-[11px] uppercase text-foreground leading-none mb-1.5 group-hover:text-primary-red transition-colors">{{ auth.user?.fullName || auth.user?.username }}</span>
                  <div class="flex items-center gap-1.5">
                    <span class="font-bold text-[10px] bg-primary-red text-white px-1.5 py-0.5 uppercase tracking-wider border border-black">🔥 {{ auth.user?.currentStreak || 0 }}</span>
                    <span class="font-bold text-[10px] bg-primary-blue text-white px-1.5 py-0.5 uppercase tracking-wider border border-black">💎 {{ auth.user?.totalPoints || 0 }}</span>
                  </div>
                </div>
                <div class="w-10 h-10 border-2 border-black bg-white p-0.5 flex-shrink-0">
                  <img :src="auth.user?.avatarUrl || 'https://api.dicebear.com/7.x/identicon/svg?seed=engflow'" class="w-full h-full object-cover grayscale group-hover:grayscale-0 transition-all" alt="Avatar" />
                </div>
              </button>
              
              <!-- Dropdown Menu -->
              <div class="absolute right-0 top-full mt-3 w-48 bg-white border-4 border-black shadow-hard-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 transform origin-top-right scale-95 group-hover:scale-100 z-[60] flex flex-col">
                <div class="absolute -top-3 right-5 w-4 h-4 bg-white border-t-4 border-l-4 border-black rotate-45 z-[-1]"></div>
                
                <button @click="handleLogout" class="w-full text-left px-4 py-3 font-bold uppercase text-sm hover:bg-primary-red hover:text-white transition-colors flex items-center gap-3">
                  <LogOutIcon class="w-4 h-4" /> Đăng xuất
                </button>
              </div>
            </div>
          </template>
          <template v-else>
            <BauhausButton variant="outline" size="sm" @click="router.push('/login')">Đăng nhập</BauhausButton>
            <BauhausButton variant="primary" size="sm" @click="router.push('/register')">Đăng ký</BauhausButton>
          </template>
        </div>

        <!-- Mobile menu button -->
        <button class="lg:hidden border-2 border-black p-2 bg-primary-yellow shadow-[4px_4px_0px_0px_black] active:translate-x-[2px] active:translate-y-[2px] active:shadow-none" aria-label="Open menu" @click="mobileMenuOpen = !mobileMenuOpen" :aria-expanded="mobileMenuOpen">
          <MenuIcon class="w-6 h-6" />
        </button>
      </div>

      <!-- Mobile Navigation Panel -->
      <div v-if="mobileMenuOpen" class="lg:hidden border-t-4 border-black bg-white py-4 px-6 space-y-4">
        <router-link to="/search" class="block font-bold uppercase tracking-wider text-lg text-foreground hover:text-primary-red transition-colors" active-class="text-primary-red" @click="mobileMenuOpen = false">Tra từ</router-link>
        <router-link to="/decks" class="block font-bold uppercase tracking-wider text-lg text-foreground hover:text-primary-blue transition-colors" active-class="text-primary-blue" @click="mobileMenuOpen = false">Bộ từ vựng</router-link>
        <router-link to="/leaderboard" class="block font-bold uppercase tracking-wider text-lg text-foreground hover:text-primary-yellow transition-colors" active-class="text-primary-yellow" @click="mobileMenuOpen = false">Xếp hạng</router-link>
        <template v-if="auth.isLoggedIn">
          <router-link to="/lessons" class="block font-bold uppercase tracking-wider text-lg text-foreground hover:text-primary-red transition-colors" active-class="text-primary-red" @click="mobileMenuOpen = false">Bài học</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/dashboard" class="block font-bold uppercase tracking-wider text-lg text-foreground hover:text-primary-yellow transition-colors" active-class="text-primary-yellow" @click="mobileMenuOpen = false">Admin</router-link>
        </template>
        <div class="border-t-2 border-black pt-3">
          <template v-if="auth.isLoggedIn">
            <button @click="router.push('/profile'); mobileMenuOpen = false" class="w-full text-left flex items-center gap-3 mb-3 border-2 border-black px-3 py-2 bg-background hover:bg-gray-100 transition-colors">
              <img :src="auth.user?.avatarUrl || 'https://api.dicebear.com/7.x/identicon/svg?seed=engflow'" class="w-8 h-8 border-2 border-black grayscale" alt="" />
              <div class="flex flex-col">
                <span class="font-bold text-xs uppercase leading-tight">{{ auth.user?.fullName || auth.user?.username }}</span>
                <span class="font-bold text-[10px] text-primary-red">{{ auth.user?.currentStreak || 0 }} | {{ auth.user?.totalPoints || 0 }}</span>
              </div>
            </button>
            <BauhausButton variant="outline" size="sm" class="w-full justify-center" @click="handleLogout">Thoát</BauhausButton>
          </template>
          <template v-else>
            <BauhausButton variant="ghost" size="sm" class="w-full justify-center mb-2" @click="router.push('/login'); mobileMenuOpen = false">Đăng nhập</BauhausButton>
            <BauhausButton variant="primary" size="sm" class="w-full justify-center" @click="router.push('/register'); mobileMenuOpen = false">Đăng ký</BauhausButton>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useAuthStore } from '@/store/modules/auth'
import { useRouter } from 'vue-router'
import { Menu as MenuIcon, LogOut as LogOutIcon } from 'lucide-vue-next'
import BauhausButton from '@/components/bauhaus/BauhausButton.vue'

import { ref, onMounted, onUnmounted } from 'vue'

const auth = useAuthStore()
const router = useRouter()
const mobileMenuOpen = ref(false)

const isScrollingDown = ref(false)
const lastScrollY = ref(0)

const handleScroll = () => {
  const currentScrollY = window.scrollY
  if (currentScrollY > 60 && currentScrollY > lastScrollY.value) {
    isScrollingDown.value = true
  } else {
    isScrollingDown.value = false
  }
  lastScrollY.value = currentScrollY
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.clip-triangle {
  clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
}
</style>
