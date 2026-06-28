<template>
  <div class="flex-grow flex items-center justify-center px-4 py-12 bg-background">
    <div class="w-full max-w-md">
      <!-- Error message -->
      <div v-if="error" class="bg-primary-red text-white font-bold text-sm uppercase tracking-wider px-4 py-3 border-2 border-black shadow-hard-sm mb-6" role="alert">
        {{ error }}
      </div>
      
      <!-- Success message -->
      <div v-if="success" class="bg-primary-blue text-white font-bold text-sm uppercase tracking-wider px-4 py-3 border-2 border-black shadow-hard-sm mb-6" role="alert">
        {{ success }}
      </div>

      <div class="bg-white border-4 border-black shadow-hard-lg p-8 relative">
        <!-- Geometric corner decoration -->
        <div class="absolute -top-3 -right-3 w-6 h-6 bg-primary-blue border-2 border-black rounded-none"></div>
        <div class="absolute -bottom-3 -left-3 w-4 h-4 bg-primary-red border-2 border-black rounded-full"></div>

        <h1 class="font-black text-4xl md:text-5xl uppercase tracking-tight mb-2">
          Quên<br />Mật Khẩu
        </h1>
        <p class="font-bold text-sm uppercase tracking-wider text-gray-500 mb-8">
          {{ step === 1 ? 'Nhập email để nhận mã xác nhận' : 'Tạo mật khẩu mới' }}
        </p>

        <!-- Step 1: Nhập Email -->
        <form v-if="step === 1" @submit.prevent="handleSendOtp" novalidate class="space-y-6">
          <div>
            <label for="forgot-email" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Email của bạn
            </label>
            <input
              id="forgot-email"
              v-model="email"
              type="email"
              :class="[
                'w-full px-4 py-3 border-2 border-black bg-background font-sans text-base',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-blue focus-visible:ring-offset-2',
                errors.email ? 'bg-red-50 border-primary-red' : ''
              ]"
              placeholder="email@domain.com"
              @input="errors.email = ''"
            />
            <p v-if="errors.email" class="mt-1 text-primary-red font-bold text-xs uppercase tracking-wider">{{ errors.email }}</p>
          </div>

          <BauhausButton
            variant="secondary"
            shape="square"
            size="lg"
            type="submit"
            :disabled="loading"
            class="w-full justify-center text-lg"
          >
            <span v-if="loading" class="animate-spin mr-2 inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full"></span>
            {{ loading ? 'ĐANG GỬI...' : 'GỬI MÃ OTP' }}
          </BauhausButton>
        </form>

        <!-- Step 2: Nhập OTP + Mật khẩu mới -->
        <form v-else @submit.prevent="handleResetPassword" novalidate class="space-y-6">
          <div>
            <label for="reset-otp" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Mã OTP (6 chữ số)
            </label>
            <input
              id="reset-otp"
              v-model="form.otp"
              type="text"
              maxlength="6"
              :class="[
                'w-full px-4 py-3 border-2 border-black bg-background font-sans text-base tracking-widest text-center font-bold',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-blue focus-visible:ring-offset-2',
                errors.otp ? 'bg-red-50 border-primary-red' : ''
              ]"
              placeholder="000000"
              @input="errors.otp = ''"
            />
            <p v-if="errors.otp" class="mt-1 text-primary-red font-bold text-xs uppercase tracking-wider">{{ errors.otp }}</p>
          </div>

          <div>
            <label for="reset-password" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Mật khẩu mới
            </label>
            <input
              id="reset-password"
              v-model="form.newPassword"
              type="password"
              :class="[
                'w-full px-4 py-3 border-2 border-black bg-background font-sans text-base',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-blue focus-visible:ring-offset-2',
                errors.newPassword ? 'bg-red-50 border-primary-red' : ''
              ]"
              placeholder="••••••••"
              @input="errors.newPassword = ''"
            />
            <p v-if="errors.newPassword" class="mt-1 text-primary-red font-bold text-xs uppercase tracking-wider">{{ errors.newPassword }}</p>
          </div>
          
          <div>
            <label for="reset-confirm-password" class="block font-bold text-sm uppercase tracking-wider mb-2">
              Xác nhận mật khẩu
            </label>
            <input
              id="reset-confirm-password"
              v-model="form.confirmPassword"
              type="password"
              :class="[
                'w-full px-4 py-3 border-2 border-black bg-background font-sans text-base',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-blue focus-visible:ring-offset-2',
                errors.confirmPassword ? 'bg-red-50 border-primary-red' : ''
              ]"
              placeholder="••••••••"
              @input="errors.confirmPassword = ''"
            />
            <p v-if="errors.confirmPassword" class="mt-1 text-primary-red font-bold text-xs uppercase tracking-wider">{{ errors.confirmPassword }}</p>
          </div>

          <BauhausButton
            variant="secondary"
            shape="square"
            size="lg"
            type="submit"
            :disabled="loading"
            class="w-full justify-center text-lg"
          >
            <span v-if="loading" class="animate-spin mr-2 inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full"></span>
            {{ loading ? 'ĐANG XỬ LÝ...' : 'ĐẶT LẠI MẬT KHẨU' }}
          </BauhausButton>
        </form>

        <!-- Footer link -->
        <p class="mt-6 text-center font-bold text-sm uppercase tracking-wider text-foreground/60">
          <router-link to="/login" class="text-primary-blue hover:text-primary-blue/80 underline decoration-2 underline-offset-2">
            Quay lại đăng nhập
          </router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import authService from '@/services/authService'
import BauhausButton from '@/components/bauhaus/BauhausButton.vue'

const router = useRouter()

const step = ref(1)
const loading = ref(false)
const error = ref(null)
const success = ref(null)

const email = ref('')
const form = reactive({ otp: '', newPassword: '', confirmPassword: '' })
const errors = reactive({ email: '', otp: '', newPassword: '', confirmPassword: '' })

async function handleSendOtp() {
  if (!email.value) {
    errors.email = 'Email không được để trống'
    return
  } else if (!/\S+@\S+\.\S+/.test(email.value)) {
    errors.email = 'Email không đúng định dạng'
    return
  }

  loading.value = true
  error.value = null
  success.value = null
  
  try {
    const res = await authService.forgotPassword(email.value)
    success.value = res.message || 'Mã OTP đã được gửi. Vui lòng kiểm tra email.'
    step.value = 2
  } catch (e) {
    error.value = e.response?.data?.error || 'Có lỗi xảy ra, vui lòng thử lại sau.'
  } finally {
    loading.value = false
  }
}

async function handleResetPassword() {
  let valid = true
  
  if (!form.otp || form.otp.length !== 6) {
    errors.otp = 'Mã OTP phải gồm 6 chữ số'
    valid = false
  }
  
  if (!form.newPassword || form.newPassword.length < 6) {
    errors.newPassword = 'Mật khẩu phải chứa ít nhất 6 ký tự'
    valid = false
  }
  
  if (form.newPassword !== form.confirmPassword) {
    errors.confirmPassword = 'Mật khẩu xác nhận không khớp'
    valid = false
  }
  
  if (!valid) return
  
  loading.value = true
  error.value = null
  success.value = null
  
  try {
    const res = await authService.resetPassword({
      email: email.value,
      otp: form.otp,
      newPassword: form.newPassword
    })
    
    success.value = res.message || 'Đặt lại mật khẩu thành công!'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  } catch (e) {
    error.value = e.response?.data?.error || 'Mã OTP không hợp lệ hoặc đã hết hạn.'
  } finally {
    loading.value = false
  }
}
</script>
