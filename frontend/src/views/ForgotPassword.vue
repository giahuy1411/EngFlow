<template>
  <div class="app-auth">
    <div class="app-auth__deco" aria-hidden="true">
      <span class="app-auth__shape app-auth__shape--accent" />
      <span class="app-auth__shape app-auth__shape--secondary" />
      <span class="app-auth__shape app-auth__shape--tertiary" />
    </div>
    <div class="app-auth__card">
      <div class="app-auth__header">
        <div class="app-auth__icon-wrap app-auth__icon-wrap--tertiary">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
          </svg>
        </div>
        <h2 class="app-auth__title">Quên mật khẩu</h2>
        <p class="app-auth__subtitle">Nhập email để nhận liên kết đặt lại</p>
      </div>
      <form @submit.prevent="handleForgot" class="app-auth__form" novalidate>
        <FormField id="forgot-email" label="Email" required>
          <template #default="{ id, describedBy }">
            <AppInput :id="id" v-model="email" type="email" placeholder="your@email.com" autocomplete="email" :aria-describedby="describedBy" required />
          </template>
        </FormField>
        <div v-if="error" class="app-auth__alert app-alert app-alert--error" role="alert">
          <div class="app-alert__body"><span>{{ error }}</span></div>
        </div>
        <div v-if="success" class="app-auth__alert app-alert app-alert--success" role="status">
          <div class="app-alert__body"><span>{{ success }}</span></div>
        </div>
        <AppButton type="submit" variant="primary" size="lg" class="w-full" :loading="loading">Gửi liên kết</AppButton>
      </form>
      <p class="text-center font-bold text-sm text-muted-foreground mt-4">
        <router-link to="/login" class="text-accent hover:underline">Quay lại đăng nhập</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import { AppButton, AppInput, FormField } from '@/components/ui'

const auth = useAuthStore()
const email = ref('')
const loading = ref(false)
const error = ref('')
const success = ref('')

async function handleForgot() {
  error.value = ''
  success.value = ''
  loading.value = true
  try {
    await auth.forgotPassword(email.value)
    success.value = 'Đã gửi liên kết đặt lại mật khẩu. Kiểm tra email của bạn.'
  } catch (e) {
    error.value = e.response?.data?.message || 'Gửi yêu cầu thất bại'
  } finally {
    loading.value = false
  }
}
</script>
