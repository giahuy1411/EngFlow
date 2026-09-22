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
        <!-- audit-v13 F-13-20: carry this page's ?redirect= into login, or the destination
             the guard captured is lost the moment the user clicks through. -->
        Bạn cần <router-link :to="loginLink" class="text-accent-ink underline underline-offset-2">đăng nhập</router-link>
        trước khi mua gói Premium để quyền lợi được áp dụng đúng tài khoản.
      </div>

      <!-- audit-v11 F131: at md+ the featured card is scaled 1.1 (widening it ~17px per side)
           and the badge overhangs, so the grid carries px slack to absorb both. Measured:
           without it the document was +28px wide at 768-800px; px-8 clears it. -->
      <div class="grid md:grid-cols-2 gap-8 max-w-3xl mx-auto mb-12 md:px-8">
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

        <div class="app-plan app-plan--featured bg-white border-2 border-foreground p-8 flex flex-col relative">
          <!-- Prompt: "a massive yellow star badge 'MOST POPULAR' rotated 15deg" -->
          <div class="app-plan__badge" aria-hidden="true">★</div>
          <div class="app-plan__ribbon absolute -top-3 right-4 bg-tertiary text-foreground text-xs font-bold px-3 py-1 uppercase tracking-wider">Rẻ hơn cả trà sữa</div>
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import { useAuthStore } from '@/store/modules/auth'
import AppButton from '@/components/ui/AppButton.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// audit-v13 F-13-20: forward this page's redirect target into the login link.
const loginLink = computed(() => {
  const target = route.query.redirect
  return typeof target === 'string' && target ? `/login?redirect=${encodeURIComponent(target)}` : '/login'
})

function checkout(planType) {
  if (!authStore.isLoggedIn) {
    // audit-v13 F-13-20: keep the destination through the login detour.
    router.push(loginLink.value)
    return
  }
  // audit-v13 F-13-20: carry the destination into checkout too, so the post-payment
  // redirect can return the user to the page the premium guard bounced them off
  // (previously the checkout discarded it and hardcoded /speaking).
  const target = route.query.redirect
  router.push({
    path: '/premium/checkout',
    query: {
      plan: planType,
      ...(typeof target === 'string' && target ? { redirect: target } : {}),
    },
  })
}
</script>

<style scoped>
/*
  Prompt: "The middle card is scaled up (1.1) and has a massive yellow star
  badge 'MOST POPULAR' rotated 15deg."

  Two deliberate deviations from the literal text, both for correctness:
   - The badge carries a star, not the words "MOST POPULAR": this page's existing
     copy is Vietnamese ("Rẻ hơn cả trà sữa") and the UI-text convention (P7) is
     Vietnamese with English technical terms. The star is the decorative part the
     prompt actually specifies; a second English label would fight the ribbon.
   - The scale is applied from `md` up only. At 360-767px the two plans stack, and
     scaling one of a stacked pair overlaps its neighbour and pushes the page
     wider than the viewport.
*/
.app-plan--featured {
  border-color: var(--geo-secondary);
  box-shadow: var(--geo-shadow-featured);
}
@media (min-width: 768px) {
  .app-plan--featured {
    transform: scale(1.1);
    z-index: 1;
  }
}

/*
  audit-v11 F131 — horizontal overflow.
  Measured (documentElement.scrollWidth - clientWidth) on /premium:
      360–767px : +8px     (badge alone)
      768–800px : +28/29px (badge + the 1.1 scale kicking in at md)
      900px+    : 0
  The badge is `right: -1.25rem` AND `rotate(15deg)`; the rotation alone grows its
  bounding box by ~22% (56px square -> ~68px), so it overhangs further than the offset
  suggests. At md+ the featured card is also scaled 1.1, which widens it past its grid
  column by ~17px per side. The grid therefore needs real slack at md+.
*/
.app-plan__badge {
  position: absolute;
  top: -1.25rem;
  /* Pulled inside the card's own edge so it cannot widen the document on a phone. */
  right: -0.25rem;
  width: 3.5rem;
  height: 3.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  line-height: 1;
  background: var(--geo-tertiary);
  border: 2px solid var(--geo-fg);
  border-radius: var(--geo-radius-full);
  box-shadow: var(--geo-shadow-sm);
  transform: rotate(15deg);
}

/* From md up the grid carries the padding that the scaled card + overhanging badge need,
   so the badge can return to its full sticker offset. */
@media (min-width: 768px) {
  .app-plan__badge {
    right: -1.25rem;
  }
}

/* The ribbon already sits at the top edge; a slight tilt matches the sticker feel. */
.app-plan__ribbon {
  transform: rotate(-2deg);
}
</style>
