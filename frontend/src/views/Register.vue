<template>
    <div class="app-auth">
    <div class="app-auth__deco" aria-hidden="true">
      <span class="app-auth__shape app-auth__shape--secondary" />
      <span class="app-auth__shape app-auth__shape--tertiary" />
      <span class="app-auth__shape app-auth__shape--accent" />
    </div>
    <div class="app-auth__card">
        <!-- Header -->
        <div class="app-auth__header">
          <div class="inline-flex items-center justify-center w-14 h-14 bg-secondary rounded-full border-2 border-foreground mb-4 shadow-pop-sm animate-pop-in">
            <svg class="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
            </svg>
          </div>
          <h1 class="font-black text-3xl uppercase tracking-tight">Đăng ký</h1>
          <p class="font-medium text-sm text-muted-foreground mt-2">Bắt đầu hành trình học ngay!</p>
        </div>

        <!-- Form -->
        <form @submit.prevent="handleRegister" class="app-auth__form" novalidate>
          <FormField id="reg-username" label="Tên đăng nhập" required>
            <template #default="{ id, describedBy }">
              <AppInput :id="id" v-model="username" type="text" placeholder="yourname" autocomplete="username"
                :aria-describedby="describedBy" required />
            </template>
          </FormField>

          <FormField id="reg-email" label="Email" required>
            <template #default="{ id, describedBy }">
              <AppInput :id="id" v-model="email" type="email" placeholder="your@email.com" autocomplete="email"
                :aria-describedby="describedBy" required />
            </template>
          </FormField>

          <FormField id="reg-password" label="Mật khẩu" required>
            <template #default="{ id, describedBy }">
              <AppInput :id="id" v-model="password" type="password" placeholder="••••••••" autocomplete="new-password"
                :aria-describedby="describedBy" required minlength="6" />
            </template>
          </FormField>

          <FormField id="reg-confirm" label="Xác nhận mật khẩu" required>
            <template #default="{ id, describedBy }">
              <AppInput :id="id" v-model="confirmPassword" type="password" placeholder="••••••••" autocomplete="new-password"
                :aria-describedby="describedBy" required minlength="6" />
            </template>
          </FormField>

          <p v-if="error" class="font-bold text-xs uppercase tracking-wider text-secondary text-center" role="alert">{{ error }}</p>

          <AppButton type="submit" variant="primary" size="lg" class="w-full" :loading="loading">
            Tạo tài khoản
          </AppButton>
        </form>

        <!-- Divider -->
        <div class="flex items-center gap-3 my-6">
          <div class="flex-1 h-0.5 bg-border"></div>
          <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Hoặc</span>
          <div class="flex-1 h-0.5 bg-border"></div>
        </div>

        <p class="text-center font-bold text-sm text-muted-foreground">
          Đã có tài khoản?
          <router-link to="/login" class="text-accent hover:underline">Đăng nhập</router-link>
        </p>
      </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import { useRouter } from 'vue-router'
import { AppButton, AppInput, FormField } from '@/components/ui'

const auth = useAuthStore()
const router = useRouter()
const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')

async function handleRegister() {
  error.value = ''
  if (password.value !== confirmPassword.value) {
    error.value = 'Mật khẩu không khớp'
    return
  }
  loading.value = true
  try {
    await auth.register({ username: username.value, email: email.value, password: password.value })
    router.push('/lessons')
  } catch (e) {
    error.value = e.response?.data?.message || 'Đăng ký thất bại'
  } finally {
    loading.value = false
  }
}
</script>
