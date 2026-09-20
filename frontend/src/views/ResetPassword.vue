<template>
  <div class="app-auth">
    <div class="app-auth__deco" aria-hidden="true">
      <span class="app-auth__shape app-auth__shape--accent" />
      <span class="app-auth__shape app-auth__shape--secondary" />
      <span class="app-auth__shape app-auth__shape--tertiary" />
    </div>
    <div class="app-auth__card">
      <div class="app-auth__header">
        <div class="app-auth__icon-wrap app-auth__icon-wrap--secondary">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
        </div>
        <h1 class="app-auth__title">ĐẶT LẠI MẬT KHẨU</h1>
        <p class="app-auth__subtitle">Nhập mã OTP đã gửi tới email của bạn</p>
      </div>
      <form @submit.prevent="handleReset" class="app-auth__form" novalidate>
        <FormField id="reset-email" label="Email" required>
          <template #default="{ id, describedBy }">
            <AppInput :id="id" v-model="email" type="email" placeholder="your@email.com" autocomplete="email" :aria-describedby="describedBy" required />
          </template>
        </FormField>
        <FormField id="reset-otp" label="MÃ OTP" required>
          <template #default="{ id, describedBy }">
            <AppInput :id="id" v-model="otp" inputmode="numeric" maxlength="6" placeholder="6 số" :aria-describedby="describedBy" required />
          </template>
        </FormField>
        <FormField id="reset-password" label="MẬT KHẨU MỚI" required>
          <template #default="{ id, describedBy }">
            <AppInput :id="id" v-model="newPassword" type="password" placeholder="Tối thiểu 6 ký tự" autocomplete="new-password" :aria-describedby="describedBy" required />
          </template>
        </FormField>
        <div v-if="error" class="app-auth__alert app-alert app-alert--error" role="alert">
          <div class="app-alert__body"><span>{{ error }}</span></div>
        </div>
        <div v-if="success" class="app-auth__alert app-alert app-alert--success" role="status">
          <div class="app-alert__body"><span>{{ success }}</span></div>
        </div>
        <AppButton type="submit" variant="primary" size="lg" class="w-full" :loading="loading">Đặt lại mật khẩu</AppButton>
      </form>
      <p class="text-center font-bold text-sm text-muted-foreground mt-4">
        <router-link to="/login" class="text-accent-ink underline underline-offset-2 hover:no-underline">Quay lại đăng nhập</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import authService from '@/services/authService'
import { AppButton, AppInput, FormField } from '@/components/ui'

const router = useRouter()
const email = ref('')
const otp = ref('')
const newPassword = ref('')
const loading = ref(false)
const error = ref('')
const success = ref('')

async function handleReset() {
  error.value = ''
  success.value = ''
  if (!email.value || !otp.value || !newPassword.value) {
    error.value = 'Vui lòng điền đầy đủ thông tin'
    return
  }
  if (newPassword.value.length < 6) {
    error.value = 'Mật khẩu phải từ 6 ký tự trở lên'
    return
  }
  loading.value = true
  try {
    await authService.resetPassword(email.value, otp.value, newPassword.value)
    success.value = 'Đặt lại mật khẩu thành công! Đang chuyển đến trang đăng nhập...'
    setTimeout(() => router.push('/login'), 1500)
  } catch (e) {
    error.value = e.response?.data?.detail || e.response?.data?.message || 'Đặt lại mật khẩu thất bại. Kiểm tra lại mã OTP.'
  } finally {
    loading.value = false
  }
}
</script>
