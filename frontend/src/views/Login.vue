<template>
  <div class="min-h-screen bg-geo-bg flex items-center justify-center py-16 relative overflow-hidden">
    <!-- Decorative shapes -->
    <div class="absolute top-20 left-10 w-24 h-24 bg-accent/10 rounded-full border-2 border-foreground/10"></div>
    <div class="absolute bottom-32 right-16 w-32 h-32 bg-tertiary/10 rounded-full border-2 border-foreground/10"></div>
    <div class="absolute top-1/3 right-1/4 w-12 h-12 bg-secondary/10 rounded-md border-2 border-foreground/10 rotate-12"></div>

    <div class="w-full max-w-md px-4 relative z-10">
      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl">
        <!-- Header -->
        <div class="text-center mb-8">
          <div class="inline-flex items-center justify-center w-14 h-14 bg-accent rounded-full border-2 border-foreground mb-4 shadow-pop-sm animate-pop-in">
            <svg class="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
            </svg>
          </div>
          <h1 class="font-black text-3xl uppercase tracking-tight">Đăng nhập</h1>
          <p class="font-medium text-sm text-muted-foreground mt-2">Chào mừng bạn trở lại!</p>
        </div>

        <!-- Form -->
        <form @submit.prevent="handleLogin" class="space-y-5">
          <div>
            <label for="login-email" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Email</label>
            <input id="login-email" v-model="email" type="email" placeholder="your@email.com" required
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            />
          </div>
          <div>
            <label for="login-password" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Mật khẩu</label>
            <input id="login-password" v-model="password" type="password" placeholder="••••••••" required
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            />
          </div>

          <div class="flex items-center justify-between text-sm">
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" v-model="remember" class="w-4 h-4 border-2 border-foreground rounded-sm accent-accent" />
              <span class="font-bold text-xs uppercase tracking-wider">Ghi nhớ</span>
            </label>
            <router-link to="/forgot-password" class="font-bold text-xs uppercase tracking-wider text-accent hover:underline">
              Quên mật khẩu?
            </router-link>
          </div>

          <p v-if="error" class="font-bold text-xs uppercase tracking-wider text-secondary text-center">{{ error }}</p>

          <button type="submit" :disabled="loading"
            class="w-full py-3.5 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5 transition-all duration-300 ease-bounce disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            <span v-if="loading" class="inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
            <span v-else>Đăng nhập</span>
          </button>
        </form>

        <!-- Divider -->
        <div class="flex items-center gap-3 my-6">
          <div class="flex-1 h-0.5 bg-border"></div>
          <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Hoặc</span>
          <div class="flex-1 h-0.5 bg-border"></div>
        </div>

        <p class="text-center font-bold text-sm text-muted-foreground">
          Chưa có tài khoản?
          <router-link to="/register" class="text-accent hover:underline">Đăng ký</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
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
    router.push('/lessons')
  } catch (e) {
    error.value = e.response?.data?.message || 'Đăng nhập thất bại'
  } finally {
    loading.value = false
  }
}
</script>
