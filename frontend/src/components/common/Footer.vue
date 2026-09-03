<template>
  <footer class="app-footer" v-if="showNav">
    <div class="app-footer__deco" aria-hidden="true">
      <span class="app-footer__shape app-footer__shape--accent" />
      <span class="app-footer__shape app-footer__shape--tertiary" />
      <span class="app-footer__shape app-footer__shape--secondary" />
    </div>
    <div class="app-footer__inner">
      <div class="app-footer__brand">
        <AppLogo :href="null" />
        <p class="app-footer__tagline">© 2026 ENGFLOW. TỰ HỌC TIẾNG ANH.</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppLogo from '@/components/common/AppLogo.vue'

// audit-v5 CLS fix: shell always mounted (App.vue); visibility decided here.
const route = useRoute()
const showNav = computed(() => {
  const authRoutes = ['/login', '/register']
  return !authRoutes.includes(route.path) && !route.path.startsWith('/admin')
})
</script>