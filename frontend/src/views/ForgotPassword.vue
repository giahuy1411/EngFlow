<template>
  <div class="min-h-screen bg-geo-bg flex items-center justify-center py-16 relative overflow-hidden">
    <div class="absolute top-32 left-20 w-20 h-20 bg-tertiary/10 rounded-full border-2 border-foreground/10"></div>
    <div class="absolute bottom-20 right-16 w-36 h-36 bg-accent/10 rounded-full border-2 border-foreground/10"></div>

    <div class="w-full max-w-md px-4 relative z-10">
      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl">
        <div class="text-center mb-8">
          <div class="inline-flex items-center justify-center w-14 h-14 bg-tertiary rounded-full border-2 border-foreground mb-4 shadow-pop-sm animate-pop-in">
            <svg class="w-7 h-7 text-foreground" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
            </svg>
          </div>
          <h1 class="font-black text-3xl uppercase tracking-tight">Quên mật khẩu</h1>
          <p class="font-medium text-sm text-muted-foreground mt-2">Nhập email để đặt lại mật khẩu</p>
        </div>

        <form @submit.prevent="handleForgot" class="space-y-5">
          <div>
            <label for="forgot-email" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Email</label>
            <input id="forgot-email" v-model="email" type="email" placeholder="your@email.com" required
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            />
          </div>

          <p v-if="message" class="font-bold text-xs text-center" :class="error ? 'text-secondary' : 'text-quaternary'">{{ message }}</p>

          <button type="submit" :disabled="loading"
            class="w-full py-3.5 font-bold text-base bg-tertiary text-foreground border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5 transition-all duration-300 ease-bounce disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            <span v-if="loading" class="inline-block w-5 h-5 border-2 border-foreground border-t-transparent rounded-full animate-spin"></span>
            <span v-else>Gửi yêu cầu</span>
          </button>
        </form>

        <div class="flex items-center gap-3 my-6">
          <div class="flex-1 h-0.5 bg-border"></div>
          <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Hoặc</span>
          <div class="flex-1 h-0.5 bg-border"></div>
        </div>

        <p class="text-center font-bold text-sm text-muted-foreground">
          <router-link to="/login" class="text-accent hover:underline">Quay lại đăng nhập</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import authService from '@/services/authService'

const email = ref('')
const loading = ref(false)
const message = ref('')
const error = ref(false)

async function handleForgot() {
  loading.value = true
  message.value = ''
  error.value = false
  try {
    await authService.forgotPassword(email.value)
    message.value = 'Yêu cầu đã được gửi! Kiểm tra email của bạn.'
  } catch (e) {
    error.value = true
    message.value = e.response?.data?.message || 'Gửi yêu cầu thất bại'
  } finally {
    loading.value = false
  }
}
</script>
