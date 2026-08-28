<template>
  <div class="bg-background min-h-screen">
    <div class="max-w-2xl mx-auto px-4 py-8 space-y-8">
      <UserPageHeader
        eyebrow="Thanh toán Premium"
        :title="planType === 'YEAR' ? 'Gói năm' : 'Gói tháng'"
        :subtitle="`Gói ${planType === 'YEAR' ? 'năm' : 'tháng'}: ${amount.toLocaleString('vi-VN')}đ`"
        :divided="false"
      >
        <template #accent>Checkout</template>
      </UserPageHeader>

      <div class="bg-card border-2 border-foreground rounded-xl p-8 shadow-pop-xl">
        <div v-if="!orderCode" class="text-center py-8">
          <div class="animate-spin w-8 h-8 border-2 border-foreground border-t-transparent rounded-full mx-auto mb-4"></div>
          <p>Đang tạo đơn hàng...</p>
        </div>

        <template v-else>
          <div class="bg-muted/60 border-2 border-border p-6 mb-6 text-center rounded-xl">
            <p class="font-bold mb-2">Quét mã QR để thanh toán</p>
            <img :src="qrUrl" alt="Mã QR thanh toán" class="mx-auto w-64 h-64 border-2 border-foreground mb-4 rounded-md" />
            <div class="text-sm space-y-1">
              <p>Số tài khoản: <strong>{{ bankAccount }}</strong></p>
              <p>Nội dung CK: <strong class="text-secondary">{{ orderCode }}</strong></p>
              <p>Số tiền: <strong>{{ amount.toLocaleString('vi-VN') }}đ</strong></p>
            </div>
          </div>

          <AppButton @click="checkStatus" :disabled="checking" variant="tertiary" class="w-full">
            {{ checking ? 'Đang kiểm tra...' : 'Đã chuyển khoản? Kiểm tra' }}
          </AppButton>

          <div v-if="message" class="mt-4 p-3 text-center font-bold rounded-md"
            :class="messageType === 'success' ? 'bg-quaternary/10 text-foreground border-2 border-quaternary' : 'bg-secondary/10 text-secondary border-2 border-secondary'">
            {{ message }}
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePremiumStore } from '@/store/modules/premium'
import { useAuthStore } from '@/store/modules/auth'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import AppButton from '@/components/ui/AppButton.vue'

const route = useRoute()
const router = useRouter()
const premiumStore = usePremiumStore()
const authStore = useAuthStore()

const planType = ref(route.query.plan || 'MONTH')
const amount = ref(planType.value === 'YEAR' ? 20000 : 10000)
const orderCode = ref('')
const qrUrl = ref('')
const bankAccount = import.meta.env.VITE_SEPAY_BANK_ACCOUNT || '123456789'
const checking = ref(false)
const message = ref('')
const messageType = ref('')
let pollTimer = null

onMounted(async () => {
  try {
    const order = await premiumStore.createOrder(planType.value)
    orderCode.value = order.orderCode
    qrUrl.value = order.qrUrl
    amount.value = order.amount
    // Auto-poll premium status while the QR is on screen so the upgrade
    // activates without a manual refresh once the bank transfer lands.
    pollTimer = setInterval(pollOnce, 5000)
  } catch (e) {
    message.value = 'Tạo đơn hàng thất bại: ' + (e.response?.data?.error || e.message)
    messageType.value = 'error'
  }
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})

async function pollOnce() {
  try {
    await premiumStore.checkStatus()
    if (premiumStore.isPremium) {
      clearInterval(pollTimer)
      pollTimer = null
      message.value = 'Premium đã được kích hoạt'
      messageType.value = 'success'
      authStore.user.isPremium = true
      localStorage.setItem('user', JSON.stringify(authStore.user))
      setTimeout(() => router.push('/speaking'), 2000)
    }
  } catch { /* keep polling silently */ }
}

async function checkStatus() {
  checking.value = true
  message.value = ''
  try {
    await pollOnce()
    if (!premiumStore.isPremium) {
      // Webhook-only mode: do NOT display internal polling-disabled message to user
      message.value = 'Chưa ghi nhận giao dịch. Vui lòng thử lại sau vài phút.'
      messageType.value = 'error'
    }
  } catch (e) {
    message.value = 'Kiểm tra thất bại: ' + (e.response?.data?.error || e.message)
    messageType.value = 'error'
  } finally {
    checking.value = false
  }
}
</script>
