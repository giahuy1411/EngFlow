<template>
  <div class="min-h-screen bg-geo-bg flex items-center justify-center py-16 relative overflow-hidden">
    <!-- Decorative shapes -->
    <div class="absolute bottom-32 left-16 w-28 h-28 bg-secondary/10 rounded-full border-2 border-foreground/10"></div>
    <div class="absolute top-20 right-10 w-36 h-36 bg-tertiary/10 rounded-full border-2 border-foreground/10"></div>
    <div class="absolute bottom-1/3 left-1/4 w-16 h-16 bg-accent/10 rounded-md border-2 border-foreground/10 rotate-12"></div>

    <div class="w-full max-w-md px-4 relative z-10">
      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl">
        <!-- Header -->
        <div class="text-center mb-8">
          <div class="inline-flex items-center justify-center w-14 h-14 bg-secondary rounded-full border-2 border-foreground mb-4 shadow-pop-sm animate-pop-in">
            <svg class="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
            </svg>
          </div>
          <h1 class="font-black text-3xl uppercase tracking-tight">Đăng ký</h1>
          <p class="font-medium text-sm text-muted-foreground mt-2">Bắt đầu hành trình học ngay!</p>
        </div>

        <!-- Form -->
        <form @submit.prevent="handleRegister" class="space-y-4">
          <div>
            <label for="reg-username" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Tên đăng nhập</label>
            <input id="reg-username" v-model="username" type="text" placeholder="yourname" required
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            />
          </div>
          <div>
            <label for="reg-email" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Email</label>
            <input id="reg-email" v-model="email" type="email" placeholder="your@email.com" required
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            />
          </div>
          <div>
            <label for="reg-password" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Mật khẩu</label>
            <input id="reg-password" v-model="password" type="password" placeholder="••••••••" required minlength="6"
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            />
          </div>
          <div>
            <label for="reg-confirm" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Xác nhận mật khẩu</label>
            <input id="reg-confirm" v-model="confirmPassword" type="password" placeholder="••••••••" required minlength="6"
              class="w-full bg-input border-2 border-[#CBD5E1] rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            />
          </div>

          <p v-if="error" class="font-bold text-xs uppercase tracking-wider text-secondary text-center">{{ error }}</p>

          <button type="submit" :disabled="loading"
            class="w-full py-3.5 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 active:shadow-pop-active active:translate-x-0.5 active:translate-y-0.5 transition-all duration-300 ease-bounce disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            <span v-if="loading" class="inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
            <span v-else>Tạo tài khoản</span>
          </button>
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
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import { useRouter } from 'vue-router'

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
