<template>
  <div class="bg-background min-h-screen">
    <div class="max-w-4xl mx-auto px-4 py-8">
      <UserPageHeader
        eyebrow="Nâng cấp trải nghiệm"
        title="EngFlow Premium"
        subtitle="Mở khóa tất cả tính năng: luyện nói, video và hơn thế nữa."
        root-class="mb-12"
      >
        <template #accent>Premium</template>
      </UserPageHeader>

      <div v-if="!authStore.isLoggedIn" role="note" class="mb-8 border-2 border-foreground bg-white p-4 text-center text-sm font-bold shadow-pop-sm max-w-3xl mx-auto">
        Bạn cần <router-link to="/login" class="text-accent underline underline-offset-2">đăng nhập</router-link>
        trước khi mua gói Premium để quyền lợi được áp dụng đúng tài khoản.
      </div>

      <div class="grid md:grid-cols-2 gap-8 max-w-3xl mx-auto mb-12">
        <div class="bg-white border-2 border-foreground p-8 flex flex-col">
          <div class="mb-6">
            <h2 class="text-2xl font-black mb-2">Gói tháng</h2>
            <div class="text-4xl font-black">10.000<span class="text-lg">đ</span></div>
            <p class="text-sm text-muted-foreground">/tháng</p>
          </div>
          <ul class="space-y-3 mb-8 flex-1">
            <li class="flex items-center gap-2 text-sm">Video Speaking không giới hạn</li>
            <li class="flex items-center gap-2 text-sm">AI chấm điểm phát âm</li>
            <li class="flex items-center gap-2 text-sm">Bài tập Premium</li>
            <li class="flex items-center gap-2 text-sm">Hủy bất cứ lúc nào</li>
          </ul>
          <AppButton @click="checkout('MONTH')" variant="tertiary" class="w-full">
            Đăng ký ngay
          </AppButton>
        </div>

        <div class="bg-white border-2 border-foreground p-8 flex flex-col relative">
          <div class="absolute -top-3 right-4 bg-tertiary text-foreground text-xs font-bold px-3 py-1 uppercase tracking-wider">Rẻ hơn cả trà sữa</div>
          <div class="mb-6">
            <h2 class="text-2xl font-black mb-2">Gói năm</h2>
            <div class="text-4xl font-black">20.000<span class="text-lg">đ</span></div>
            <p class="text-sm text-muted-foreground">/năm</p>
          </div>
          <ul class="space-y-3 mb-8 flex-1">
            <li class="flex items-center gap-2 text-sm">Tất cả quyền lợi gói tháng</li>
            <li class="flex items-center gap-2 text-sm">Ưu tiên tính năng mới</li>
            <li class="flex items-center gap-2 text-sm">Hỗ trợ ưu tiên</li>
            <li class="flex items-center gap-2 text-sm">Giá ưu đãi chỉ bằng 4 tháng</li>
          </ul>
          <AppButton @click="checkout('YEAR')" variant="tertiary" class="w-full">
            Đăng ký ngay
          </AppButton>
        </div>
      </div>

      <div class="text-center text-sm text-muted-foreground">
        <p>Sau khi thanh toán, Premium được kích hoạt trong vòng 1-5 phút.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import { useAuthStore } from '@/store/modules/auth'
import AppButton from '@/components/ui/AppButton.vue'

const router = useRouter()
const authStore = useAuthStore()

function checkout(planType) {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  router.push({ path: '/premium/checkout', query: { plan: planType } })
}
</script>
