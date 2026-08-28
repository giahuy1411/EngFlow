<template>
  <main class="min-h-[100dvh] bg-background px-4 py-12">
    <div class="mx-auto max-w-6xl space-y-8">
      <UserPageHeader
        eyebrow="Bảng thành tích cộng đồng"
        title="Bảng xếp hạng"
        subtitle="Theo dõi điểm, streak và tiến bộ của những người học đang duy trì thói quen mỗi ngày."
      >
        <template #accent>Streak</template>
        <template #actions>
          <div class="rounded-xl border-2 border-foreground bg-tertiary/30 p-5 text-right shadow-pop-sm">
            <Trophy class="ml-auto h-8 w-8 text-accent" aria-hidden="true" />
            <p class="mt-3 text-xs font-black uppercase tracking-wider text-muted-foreground">Tổng người học</p>
            <p class="mt-1 text-3xl font-black tabular-nums">{{ totalElements }}</p>
          </div>
        </template>
      </UserPageHeader>

      <div v-if="loading" class="space-y-3" role="status" aria-label="Đang tải bảng xếp hạng">
        <div v-for="index in pageSize" :key="index" class="h-20 animate-pulse border-2 border-foreground/10 bg-card/60"></div>
      </div>

      <section v-else-if="error" class="border-2 border-danger bg-danger/10 p-6 text-danger" role="alert">
        <p class="font-black">Không tải được bảng xếp hạng.</p>
        <p class="mt-1 text-sm">{{ error }}</p>
        <button class="mt-4 border-2 border-foreground bg-card px-4 py-2 font-black uppercase text-xs" @click="loadLeaderboard">Thử lại</button>
      </section>

      <section v-else-if="leaderboard.length === 0" class="border-2 border-dashed border-foreground bg-card p-12 text-center">
        <p class="font-black text-2xl">Chưa có dữ liệu xếp hạng</p>
        <p class="mt-2 text-sm text-muted-foreground">Hãy hoàn thành bài học đầu tiên để ghi tên lên bảng.</p>
      </section>

      <section v-else class="space-y-4" aria-label="Danh sách xếp hạng">
        <article
          v-for="user in leaderboard"
          :key="user.userId"
          class="grid gap-4 border-2 border-foreground bg-card p-4 shadow-pop transition-transform hover:-translate-y-0.5 md:grid-cols-[4rem_minmax(0,1fr)_auto_auto_auto] md:items-center"
          :class="isCurrentUser(user.userId) ? 'border-accent bg-accent/5' : ''"
        >
          <div class="flex h-12 w-12 items-center justify-center border-2 border-foreground bg-tertiary font-black text-xl" :class="user.rank <= 3 ? 'rotate-3' : ''">
            {{ user.rank }}
          </div>
          <div class="flex min-w-0 items-center gap-3">
            <img :src="user.avatarUrl || 'https://api.dicebear.com/7.x/thumbs/svg?seed=engflow'" class="h-12 w-12 flex-shrink-0 border-2 border-foreground object-cover" alt="Ảnh đại diện" />
            <div class="min-w-0">
              <p class="truncate font-black">{{ user.fullName || user.username }}</p>
              <p class="truncate text-sm text-muted-foreground">@{{ user.username }} <span v-if="isCurrentUser(user.userId)" class="font-black text-accent">· Bạn</span></p>
            </div>
          </div>
          <div class="text-xs font-black uppercase tracking-wider text-muted-foreground">{{ user.currentLevel }}</div>
          <div class="border-l-2 border-foreground pl-4 text-sm font-black text-accent">{{ user.currentStreak }} ngày streak</div>
          <div class="text-right text-2xl font-black tabular-nums">{{ user.totalPoints }}</div>
        </article>

        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          :total-items="totalElements"
          :page-size="pageSize"
          item-label="người học"
          @page-change="changePage"
        />
      </section>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import leaderboardService from '@/services/leaderboardService'
import { useAuthStore } from '@/store/modules/auth'
import { Trophy } from 'lucide-vue-next'
import Pagination from '@/components/common/Pagination.vue'
import UserPageHeader from '@/components/common/UserPageHeader.vue'

const leaderboard = ref([])
const loading = ref(true)
const error = ref('')
const authStore = useAuthStore()
const currentPage = ref(1)
const pageSize = 20
const totalPages = ref(1)
const totalElements = ref(0)

onMounted(loadLeaderboard)

async function loadLeaderboard() {
  loading.value = true
  error.value = ''
  try {
    const data = await leaderboardService.getLeaderboard({ page: currentPage.value - 1, size: pageSize })
    leaderboard.value = data.content || []
    totalPages.value = data.totalPages || 1
    totalElements.value = data.totalElements || 0
  } catch (cause) {
    error.value = cause.response?.data?.detail || 'Hệ thống đang bận. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

function changePage(page) {
  currentPage.value = page
  loadLeaderboard()
}

function isCurrentUser(userId) {
  return authStore.user?.id === userId
}
</script>