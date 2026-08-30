<template>
  <header class="app-navbar">
    <div class="app-navbar__inner">
      <AppLogo href="/" />
      <nav class="hidden xl:flex items-center gap-1" aria-label="Primary">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="app-navbar__link inline-flex items-center gap-1"
          :class="route.path === item.to ? 'app-navbar__link--active' : ''"
        >
          <span>{{ item.label }}</span>
          <Lock v-if="item.locked" class="app-navbar__lock" :size="14" :stroke-width="2.5" aria-hidden="true" />
        </router-link>
      </nav>
      <div class="flex items-center gap-2.5 ml-auto">
        <template v-if="!auth.isLoggedIn">
          <router-link to="/login" class="app-navbar__auth app-navbar__auth--ghost hidden sm:inline-flex">Đăng nhập</router-link>
          <router-link to="/register" class="app-navbar__auth app-navbar__auth--solid">Đăng ký</router-link>
        </template>
        <div v-else class="hidden xl:flex items-center gap-3">
          <router-link to="/profile" class="app-navbar__user">
            <span class="app-avatar app-avatar--sm">
              <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" :alt="auth.user.username || 'avatar'" />
              <template v-else>{{ (auth.user?.username || 'U').slice(0, 1) }}</template>
            </span>
            <span class="app-navbar__username">{{ auth.user?.username }}</span>
          </router-link>
          <AppButton variant="secondary" size="sm" @click="handleLogout">Thoát</AppButton>
        </div>
        <AppButton variant="ghost" size="sm" class="app-navbar__hamburger xl:hidden" :aria-expanded="mobileOpen" aria-label="Menu" @click="mobileOpen = !mobileOpen">
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path v-if="!mobileOpen" stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </AppButton>
      </div>
    </div>
    <div v-if="mobileOpen" class="app-navbar__mobile">
      <nav class="app-navbar__mobile-nav" aria-label="Mobile">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="app-navbar__mobile-link inline-flex items-center gap-1"
          :class="route.path === item.to ? 'app-navbar__mobile-link--active' : ''"
          @click="mobileOpen = false"
        >
          <span>{{ item.label }}</span>
          <Lock v-if="item.locked" class="app-navbar__lock" :size="14" :stroke-width="2.5" aria-hidden="true" />
        </router-link>
        <template v-if="!auth.isLoggedIn">
          <router-link to="/login" class="app-navbar__mobile-cta app-navbar__mobile-cta--ghost" @click="mobileOpen = false">Đăng nhập</router-link>
          <router-link to="/register" class="app-navbar__mobile-cta app-navbar__mobile-cta--solid" @click="mobileOpen = false">Đăng ký</router-link>
        </template>
        <template v-else>
          <router-link to="/profile" class="app-navbar__mobile-link" @click="mobileOpen = false">Hồ sơ</router-link>
          <AppButton variant="secondary" @click="handleLogout">Thoát</AppButton>
        </template>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock } from 'lucide-vue-next'
import { useAuthStore } from '@/store/modules/auth'
import AppButton from '@/components/ui/AppButton.vue'
import AppLogo from '@/components/common/AppLogo.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const mobileOpen = ref(false)

const navItems = computed(() => {
  const speakingLocked = auth.isLoggedIn && !auth.isAdmin && !auth.isPremium
  const items = [
    { to: '/', label: 'Trang chủ' },
    { to: '/lessons', label: 'Bài học' },
    { to: '/decks', label: 'Luyện tập' },
    { to: '/speaking', label: 'Luyện nói', locked: speakingLocked },
    { to: '/leaderboard', label: 'Xếp hạng' },
    { to: '/search', label: 'Tra từ' },
  ]
  if (auth.isLoggedIn && !auth.isPremium && !auth.isAdmin) {
    items.push({ to: '/premium', label: 'Premium' })
  }
  if (auth.isLoggedIn && auth.isAdmin) {
    items.push({ to: '/admin/dashboard', label: 'Quản trị' })
  }
  return items
})

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>
