<template>
  <div class="flex flex-col min-h-screen bg-background">
    <!-- Skip to content link for keyboard users -->
    <a href="#main-content" class="skip-link">
      Bỏ qua điều hướng
    </a>

    <Header v-if="showNavigation" />

    <main id="main-content" class="flex-grow flex flex-col" tabindex="-1">
      <router-view></router-view>
    </main>

    <Footer v-if="showNavigation" />

    <!-- Toast Container -->
    <div class="fixed bottom-6 right-6 z-[9999] flex flex-col gap-3 max-w-sm w-full pointer-events-none">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="[
            'pointer-events-auto border-2 border-black p-4 font-bold uppercase tracking-wider text-sm shadow-pop',
            toastBackground(toast.type)
          ]"
          role="alert"
        >
          <div class="flex items-start justify-between gap-3">
            <span>{{ toast.message }}</span>
            <button @click="remove(toast.id)" class="flex-shrink-0 leading-none text-lg font-black opacity-70 hover:opacity-100 transition-opacity">&times;</button>
          </div>
        </div>
      </TransitionGroup>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const { toasts, remove, toastBackground } = useToast()

const showNavigation = computed(() => {
  const authRoutes = ['/login', '/register']
  const isAdminRoute = route.path.startsWith('/admin')
  return !authRoutes.includes(route.path) && !isAdminRoute
})
</script>

<style>
.skip-link {
  position: absolute;
  left: -9999px;
  z-index: 9999;
  padding: 0.5rem 1rem;
  background: #121212;
  color: #F0F0F0;
  font-family: 'Be Vietnam Pro', sans-serif;
  font-weight: 700;
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 0.1em;
  text-decoration: none;
}
.skip-link:focus {
  left: 0;
  top: 0;
}

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
