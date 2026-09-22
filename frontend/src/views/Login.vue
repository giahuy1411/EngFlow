<template>
  <div class="app-auth">
    <div class="app-auth__deco" aria-hidden="true">
      <span class="app-auth__shape app-auth__shape--accent" />
      <span class="app-auth__shape app-auth__shape--tertiary" />
      <span class="app-auth__shape app-auth__shape--secondary" />
    </div>
    <div class="app-auth__card">
      <div class="app-auth__header">
        <div class="app-auth__icon-wrap app-auth__icon-wrap--accent">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
          </svg>
        </div>
        <h1 class="app-auth__title">Đăng nhập</h1>
        <p class="app-auth__subtitle">Chào mừng bạn trở lại!</p>
      </div>
      <form @submit.prevent="handleLogin" class="app-auth__form" novalidate>
        <FormField id="login-email" label="Email" required>
          <template #default="{ id, describedBy }">
            <AppInput :id="id" v-model="email" type="email" placeholder="your@email.com" autocomplete="email" :aria-describedby="describedBy" required />
          </template>
        </FormField>
        <FormField id="login-password" label="Mật khẩu" required>
          <template #default="{ id, describedBy }">
            <AppInput :id="id" v-model="password" type="password" placeholder="••••••••" autocomplete="current-password" :aria-describedby="describedBy" required />
          </template>
        </FormField>
        <div class="flex items-center justify-between">
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="remember" class="w-4 h-4 border-2 border-foreground rounded-sm accent-accent" />
            <span class="font-bold text-xs uppercase tracking-wider">Ghi nhớ</span>
          </label>
          <router-link to="/forgot-password" class="font-bold text-xs uppercase tracking-wider text-accent-ink underline underline-offset-2 hover:no-underline">Quên mật khẩu?</router-link>
        </div>
        <div v-if="error" class="app-auth__alert app-alert app-alert--error" role="alert">
          <div class="app-alert__body"><span>{{ error }}</span></div>
        </div>
        <AppButton type="submit" variant="featured" size="lg" class="w-full" :loading="loading">Đăng nhập</AppButton>
      </form>
      <div class="app-auth__divider">
        <div class="flex-1 h-0.5 bg-border" /><span>Hoặc</span><div class="flex-1 h-0.5 bg-border" />
      </div>
      <p class="text-center font-bold text-sm text-muted-foreground">
        Chưa có tài khoản? <router-link to="/register" class="text-accent-ink underline underline-offset-2 hover:no-underline">Đăng ký</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import { useRoute, useRouter } from 'vue-router'
import { AppButton, AppInput, FormField } from '@/components/ui'
import { safeRedirect } from '@/utils/safeRedirect'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const email = ref('')
const password = ref('')
const remember = ref(false)
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await auth.login({ email: email.value, password: password.value, remember: remember.value })
    // audit-v13 F-13-20: return to the page the guard bounced the user off, when there is
    // one. safeRedirect() rejects anything that is not an internal path, so a hostile
    // ?redirect=//evil.com cannot turn this into an open redirect.
    router.replace(safeRedirect(route.query.redirect))
  } catch (e) {
    error.value = e.response?.data?.message || 'Đăng nhập thất bại'
  } finally {
    loading.value = false
  }
}
</script>
