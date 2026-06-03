<template>
  <header class="border-b-4 border-black bg-white sticky top-0 z-50">
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
        <nav class="hidden md:flex items-center gap-8 font-bold uppercase tracking-wider text-sm">
          <router-link to="/search" class="hover:text-primary-red transition-colors" active-class="text-primary-red">Tra từ</router-link>
          <router-link to="/decks" class="hover:text-green-500 transition-colors" active-class="text-green-500">Bộ từ vựng</router-link>
          <router-link to="/leaderboard" class="hover:text-primary-blue transition-colors" active-class="text-primary-blue">Xếp hạng</router-link>
          
          <template v-if="auth.isLoggedIn">
            <router-link to="/lessons" class="hover:text-primary-yellow transition-colors" active-class="text-primary-yellow">Bài học</router-link>
            <router-link to="/profile" class="hover:text-primary-red transition-colors" active-class="text-primary-red">Dashboard</router-link>
          </template>
        </nav>

        <!-- Actions -->
        <div class="hidden md:flex items-center gap-4">
          <template v-if="auth.isLoggedIn">
            <div class="flex items-center gap-3 mr-4 border-2 border-black px-3 py-1 bg-background shadow-[2px_2px_0px_0px_black]">
              <img :src="auth.user?.avatarUrl || 'https://api.dicebear.com/7.x/identicon/svg?seed=engflow'" class="w-8 h-8 rounded-full border-2 border-black grayscale hover:grayscale-0 transition-all" alt="" />
              <div class="flex flex-col">
                <span class="font-bold text-xs uppercase leading-tight">{{ auth.user?.fullName || auth.user?.username }}</span>
                <span class="font-bold text-[10px] text-primary-red">🔥 {{ auth.user?.currentStreak || 0 }} | ⭐ {{ auth.user?.totalPoints || 0 }}</span>
              </div>
            </div>
            <BauhausButton variant="outline" size="sm" @click="handleLogout">Thoát</BauhausButton>
          </template>
          <template v-else>
            <BauhausButton variant="ghost" size="sm" @click="router.push('/login')">Đăng nhập</BauhausButton>
            <BauhausButton variant="primary" size="sm" @click="router.push('/register')">Đăng ký</BauhausButton>
          </template>
        </div>

        <!-- Mobile menu button -->
        <button class="md:hidden border-2 border-black p-2 bg-primary-yellow shadow-[4px_4px_0px_0px_black] active:translate-x-[2px] active:translate-y-[2px] active:shadow-none" aria-label="Open menu" @click="mobileMenuOpen = !mobileMenuOpen" :aria-expanded="mobileMenuOpen">
          <MenuIcon class="w-6 h-6" />
        </button>
      </div>

      <!-- Mobile Navigation Panel -->
      <div v-if="mobileMenuOpen" class="md:hidden border-t-4 border-black bg-white py-4 px-4 space-y-3">
        <router-link to="/search" class="block font-bold uppercase tracking-wider text-sm py-2 px-3 border-2 border-black bg-background hover:bg-primary-red hover:text-white transition-colors" active-class="bg-primary-red text-white" @click="mobileMenuOpen = false">Tra từ</router-link>
        <router-link to="/decks" class="block font-bold uppercase tracking-wider text-sm py-2 px-3 border-2 border-black bg-background hover:bg-green-500 hover:text-white transition-colors" active-class="bg-green-500 text-white" @click="mobileMenuOpen = false">Bộ từ vựng</router-link>
        <router-link to="/leaderboard" class="block font-bold uppercase tracking-wider text-sm py-2 px-3 border-2 border-black bg-background hover:bg-primary-blue hover:text-white transition-colors" active-class="bg-primary-blue text-white" @click="mobileMenuOpen = false">Xếp hạng</router-link>
        <template v-if="auth.isLoggedIn">
          <router-link to="/lessons" class="block font-bold uppercase tracking-wider text-sm py-2 px-3 border-2 border-black bg-background hover:bg-primary-yellow transition-colors" active-class="bg-primary-yellow" @click="mobileMenuOpen = false">Bài học</router-link>
          <router-link to="/profile" class="block font-bold uppercase tracking-wider text-sm py-2 px-3 border-2 border-black bg-background hover:bg-primary-red hover:text-white transition-colors" active-class="bg-primary-red text-white" @click="mobileMenuOpen = false">Dashboard</router-link>
        </template>
        <div class="border-t-2 border-black pt-3">
          <template v-if="auth.isLoggedIn">
            <div class="flex items-center gap-3 mb-3 border-2 border-black px-3 py-2 bg-background">
              <img :src="auth.user?.avatarUrl || 'https://api.dicebear.com/7.x/identicon/svg?seed=engflow'" class="w-8 h-8 rounded-full border-2 border-black grayscale" alt="" />
              <div class="flex flex-col">
                <span class="font-bold text-xs uppercase leading-tight">{{ auth.user?.fullName || auth.user?.username }}</span>
                <span class="font-bold text-[10px] text-primary-red">🔥 {{ auth.user?.currentStreak || 0 }} | ⭐ {{ auth.user?.totalPoints || 0 }}</span>
              </div>
            </div>
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
import { Menu as MenuIcon } from 'lucide-vue-next'
import BauhausButton from '@/components/bauhaus/BauhausButton.vue'

import { ref } from 'vue'

const auth = useAuthStore()
const router = useRouter()
const mobileMenuOpen = ref(false)

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
