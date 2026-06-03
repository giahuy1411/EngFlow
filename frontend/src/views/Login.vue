<template>
  <div class="flex-grow flex items-center justify-center px-4 py-12 bg-background" style="min-height: calc(100vh - 5rem);">
    <div class="w-full max-w-md">
      <!-- Error message -->
      <div v-if="error" class="bg-primary-red text-white font-bold text-sm uppercase tracking-wider px-4 py-3 border-2 border-black shadow-hard-sm mb-6" role="alert">
        {{ error }}
      </div>

      <div class="bg-white border-4 border-black shadow-hard-lg p-8 relative">
        <!-- Geometric corner decoration -->
        <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-yellow border-2 border-black rounded-full"></div>
        <div class="absolute -bottom-3 -left-3 w-4 h-4 bg-primary-blue border-2 border-black"></div>

        <h1 class="font-black text-4xl md:text-5xl uppercase tracking-tight mb-8">
          Đăng<br />Nhập
        </h1>

        <form @submit.prevent="handleLogin" novalidate class="space-y-6">
          <!-- Email field -->
          <div>
            <label for="login-email" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Email
            </label>
            <input
              id="login-email"
              v-model="form.email"
              type="email"
              autocomplete="email"
              :class="[
                'w-full px-4 py-3 border-2 border-black bg-background font-sans text-base',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-red focus-visible:ring-offset-2',
                errors.email ? 'bg-red-50 border-primary-red' : ''
              ]"
              placeholder="email@domain.com"
              @input="errors.email = ''"
              @blur="validateField('email')"
            />
            <p v-if="errors.email" class="mt-1 text-primary-red font-bold text-xs uppercase tracking-wider">{{ errors.email }}</p>
          </div>

          <!-- Password field -->
          <div>
            <label for="login-password" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Mật khẩu
            </label>
            <input
              id="login-password"
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              :class="[
                'w-full px-4 py-3 border-2 border-black bg-background font-sans text-base',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-red focus-visible:ring-offset-2',
                errors.password ? 'bg-red-50 border-primary-red' : ''
              ]"
              placeholder="••••••••"
              @input="errors.password = ''"
              @blur="validateField('password')"
            />
            <p v-if="errors.password" class="mt-1 text-primary-red font-bold text-xs uppercase tracking-wider">{{ errors.password }}</p>
          </div>

          <!-- Submit button -->
          <BauhausButton
            variant="primary"
            shape="square"
            size="lg"
            type="submit"
            :disabled="loading"
            class="w-full justify-center text-lg"
          >
            <span v-if="loading" class="animate-spin mr-2 inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full"></span>
            {{ loading ? 'ĐANG XỬ LÝ...' : 'ĐĂNG NHẬP' }}
          </BauhausButton>
        </form>

        <!-- Footer link -->
        <p class="mt-6 text-center font-bold text-sm uppercase tracking-wider text-foreground/60">
          Chưa có tài khoản?
          <router-link to="/register" class="text-primary-red hover:text-primary-red/80 underline decoration-2 underline-offset-2 ml-1">
            Đăng ký ngay
          </router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import BauhausButton from '@/components/bauhaus/BauhausButton.vue'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const error = ref(null)
const form = reactive({ email: '', password: '' })
const errors = reactive({ email: '', password: '' })

function validateField(field) {
  if (field === 'email') {
    if (!form.email) {
      errors.email = 'Email không được để trống'
    } else if (!/\S+@\S+\.\S+/.test(form.email)) {
      errors.email = 'Email không đúng định dạng'
    } else {
      errors.email = ''
    }
  }
  if (field === 'password') {
    if (form.password.length < 6) {
      errors.password = 'Mật khẩu phải chứa ít nhất 6 ký tự'
    } else {
      errors.password = ''
    }
  }
}

function validate() {
  let valid = true

  if (!form.email) {
    errors.email = 'Email không được để trống'
    valid = false
  } else if (!/\S+@\S+\.\S+/.test(form.email)) {
    errors.email = 'Email không đúng định dạng'
    valid = false
  } else {
    errors.email = ''
  }

  if (form.password.length < 6) {
    errors.password = 'Mật khẩu phải chứa ít nhất 6 ký tự'
    valid = false
  } else {
    errors.password = ''
  }

  return valid
}

async function handleLogin() {
  if (!validate()) return
  loading.value = true
  error.value = null
  try {
    await auth.login(form)
    router.push('/lessons')
  } catch (e) {
    error.value = e.response?.data?.error || 'Email hoặc mật khẩu không chính xác'
  } finally {
    loading.value = false
  }
}
</script>
