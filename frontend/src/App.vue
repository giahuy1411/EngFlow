<template>
  <div class="flex flex-col min-h-screen bg-background">
    <!-- Skip to content link for keyboard users -->
    <a href="#main-content" class="skip-link">
      Bỏ qua điều hướng
    </a>

    <!-- audit-v5 CLS fix: Header/Footer shells are ALWAYS mounted so their
         reserved heights (min-height in app-layout.css) exist from the first
         paint; each component hides itself on auth/admin routes. -->
    <Header />

    <main id="main-content" class="flex-grow flex flex-col" tabindex="-1">
      <router-view></router-view>
    </main>

    <Footer />

    <!-- Toast Container -->
    <div class="fixed bottom-6 right-6 z-[9999] flex flex-col gap-3 max-w-sm w-full pointer-events-none">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="[
            'pointer-events-auto border-2 border-foreground p-4 font-bold uppercase tracking-wider text-sm shadow-pop',
            toastBackground(toast.type)
          ]"
          role="alert"
        >
          <div class="flex items-start justify-between gap-3">
            <span>{{ toast.message }}</span>
            <button @click="remove(toast.id)" aria-label="Đóng thông báo" class="flex-shrink-0 leading-none text-lg font-black opacity-70 hover:opacity-100 transition-opacity">&times;</button>
          </div>
        </div>
      </TransitionGroup>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import { useToast } from '@/composables/useToast'
import { useAuthStore } from '@/store/modules/auth'

const { toasts, remove, toastBackground } = useToast()
const auth = useAuthStore()

// Streak semantics: mọi lượt truy cập web của user đã đăng nhập đều tính là
// một ngày học. fetchUser() gọi /api/auth/me → UserService.getProfile →
// StreakService.recordAccess. Fire-and-forget, fail-soft (store tự logout khi 401).
onMounted(() => {
  if (auth.isLoggedIn) auth.fetchUser()
})
</script>

<style>
/* audit-v5: removed the duplicate .skip-link block here (hardcoded #121212/#F0F0F0
   + literal font) — the token-based definition in assets/main.css is the single
   source of truth. */

main:focus {
  outline: none;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(100%) scale(0.9);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(100%) scale(0.9);
}
</style>
