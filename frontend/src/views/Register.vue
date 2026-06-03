<template>
  <div class="flex-grow flex items-center justify-center px-4 py-12 bg-background" style="min-height: calc(100vh - 5rem);">
    <div class="w-full max-w-md">
      <!-- Success message -->
      <div v-if="success" class="bg-primary-blue text-white font-bold text-sm uppercase tracking-wider px-4 py-3 border-2 border-black shadow-hard-sm mb-6" role="alert">
        Đăng ký thành công! Đang chuyển hướng...
      </div>

      <!-- Error message -->
      <div v-if="error" class="bg-primary-red text-white font-bold text-sm uppercase tracking-wider px-4 py-3 border-2 border-black shadow-hard-sm mb-6" role="alert">
        {{ error }}
      </div>

      <div class="bg-white border-4 border-black shadow-hard-lg p-8 relative">
        <!-- Geometric corner decorations -->
        <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-blue border-2 border-black rotate-12"></div>
        <div class="absolute -bottom-3 -left-3 w-5 h-5 bg-primary-yellow border-2 border-black rounded-full"></div>

        <h1 class="font-black text-4xl md:text-5xl uppercase tracking-tight mb-8">
          Đăng<br />Ký
        </h1>

        <form @submit.prevent="handleRegister" novalidate class="space-y-5">
          <!-- Full name -->
          <div>
            <label for="reg-fullname" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Họ và tên
            </label>
            <input
              id="reg-fullname"
              v-model="form.fullName"
              type="text"
              autocomplete="name"
              class="w-full px-4 py-3 border-2 border-black bg-background font-sans text-base focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-red focus-visible:ring-offset-2"
              placeholder="Nguyễn Văn A"
              @input="error = null"
            />
          </div>

          <!-- Username -->
          <div>
            <label for="reg-username" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Tên đăng nhập
            </label>
            <input
              id="reg-username"
              v-model="form.username"
              type="text"
              autocomplete="username"
              :class="[
                'w-full px-4 py-3 border-2 border-black bg-background font-sans text-base',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-red focus-visible:ring-offset-2',
                errors.username ? 'bg-red-50 border-primary-red' : ''
              ]"
              placeholder="nguyenvana"
              @input="errors.username = ''"
              @blur="validateField('username')"
            />
            <p v-if="errors.username" class="mt-1 text-primary-red font-bold text-xs uppercase tracking-wider">{{ errors.username }}</p>
          </div>

          <!-- Email -->
          <div>
            <label for="reg-email" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Email
            </label>
            <input
              id="reg-email"
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

          <!-- Password -->
          <div>
            <label for="reg-password" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Mật khẩu
            </label>
            <input
              id="reg-password"
              v-model="form.password"
              type="password"
              autocomplete="new-password"
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

          <!-- Confirm Password -->
          <div>
            <label for="reg-confirm-password" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Xác nhận mật khẩu
            </label>
            <input
              id="reg-confirm-password"
              v-model="form.confirmPassword"
              type="password"
              autocomplete="new-password"
              :class="[
                'w-full px-4 py-3 border-2 border-black bg-background font-sans text-base',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-red focus-visible:ring-offset-2',
                errors.confirmPassword ? 'bg-red-50 border-primary-red' : ''
              ]"
              placeholder="••••••••"
              @input="errors.confirmPassword = ''"
              @blur="validateField('confirmPassword')"
            />
            <p v-if="errors.confirmPassword" class="mt-1 text-primary-red font-bold text-xs uppercase tracking-wider">{{ errors.confirmPassword }}</p>
          </div>

          <!-- Submit button -->
          <BauhausButton
            variant="secondary"
            shape="square"
            size="lg"
            type="submit"
            :disabled="loading"
            class="w-full justify-center text-lg"
          >
            <span v-if="loading" class="animate-spin mr-2 inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full"></span>
            {{ loading ? 'ĐANG XỬ LÝ...' : 'ĐĂNG KÝ' }}
          </BauhausButton>
        </form>

        <!-- Footer link -->
        <p class="mt-6 text-center font-bold text-sm uppercase tracking-wider text-foreground/60">
          Đã có tài khoản?
          <router-link to="/login" class="text-primary-blue hover:text-primary-blue/80 underline decoration-2 underline-offset-2 ml-1">
            Đăng nhập
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
const success = ref(false)
const error = ref(null)
const form = reactive({ username: '', email: '', password: '', confirmPassword: '', fullName: '' })
const errors = reactive({ username: '', email: '', password: '', confirmPassword: '' })

function validateField(field) {
  if (field === 'username') {
    errors.username = form.username.length < 3 ? 'Tên đăng nhập tối thiểu 3 ký tự' : ''
  }
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
    errors.password = form.password.length < 6 ? 'Mật khẩu tối thiểu 6 ký tự' : ''
  }
  if (field === 'confirmPassword') {
    errors.confirmPassword = form.confirmPassword !== form.password ? 'Mật khẩu xác nhận không khớp' : ''
  }
}

function validate() {
  let valid = true

  if (form.username.length < 3) {
    errors.username = 'Tên đăng nhập tối thiểu 3 ký tự'
    valid = false
  } else {
    errors.username = ''
  }

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
    errors.password = 'Mật khẩu tối thiểu 6 ký tự'
    valid = false
  } else {
    errors.password = ''
  }

  if (form.confirmPassword !== form.password) {
    errors.confirmPassword = 'Mật khẩu xác nhận không khớp'
    valid = false
  } else {
    errors.confirmPassword = ''
  }

  return valid
}

  async function handleRegister() {
    if (!validate()) return
    loading.value = true
    error.value = null
    try {
      const { confirmPassword, ...payload } = form
      await auth.register(payload)
    success.value = true
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  } catch (e) {
    error.value = e.response?.data?.error || 'Đăng ký không thành công. Vui lòng kiểm tra lại thông tin.'
  } finally {
    loading.value = false
  }
}
</script>
