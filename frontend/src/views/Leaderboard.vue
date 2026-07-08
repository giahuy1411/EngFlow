<template>
  <div class="px-4 py-12 max-w-7xl mx-auto">
    <div class="mb-12 text-center relative">
      <div class="inline-flex items-center justify-center w-14 h-14 bg-tertiary border-2 border-foreground rounded-full mb-4 shadow-pop-sm">
        <Trophy class="w-7 h-7 text-foreground" />
      </div>
      <h1 class="font-black text-5xl md:text-6xl uppercase tracking-tight mb-4">
        Bảng <span class="text-accent">Xếp Hạng</span>
      </h1>
      <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground">Cùng đua top với các thành viên khác nhé!</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-16">
      <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
    </div>

    <!-- Empty -->
    <div v-else-if="leaderboard.length === 0" class="border-2 border-foreground border-dashed bg-card p-12 text-center rounded-md">
      <p class="font-black text-2xl uppercase mb-4">Chưa có dữ liệu</p>
      <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground">Hãy tích lũy điểm để xuất hiện trên bảng xếp hạng!</p>
    </div>

    <!-- List -->
    <div v-else class="flex flex-col gap-4">
      <div v-for="user in leaderboard" :key="user.userId"
        :class="[
          'bg-card border-2 border-foreground rounded-md p-4 flex items-center gap-4 shadow-pop transition-all',
          isCurrentUser(user.userId) ? '!border-accent bg-accent/5' : ''
        ]"
      >
        <!-- Rank -->
        <div class="flex-shrink-0">
          <span v-if="user.rank === 1"
            class="w-10 h-10 bg-tertiary border-2 border-foreground flex items-center justify-center font-black text-lg rotate-6 rounded-md"
          >1</span>
          <span v-else-if="user.rank === 2"
            class="w-10 h-10 bg-secondary border-2 border-foreground flex items-center justify-center font-black text-lg rotate-3 rounded-md"
          >2</span>
          <span v-else-if="user.rank === 3"
            class="w-10 h-10 bg-accent/20 border-2 border-foreground flex items-center justify-center font-black text-lg -rotate-3 rounded-md"
          >3</span>
          <div v-else
            class="w-10 h-10 border-2 border-foreground bg-geo-bg flex items-center justify-center font-black text-lg rounded-md"
          >{{ user.rank }}</div>
        </div>

        <!-- Avatar + Name -->
        <div class="flex items-center gap-3 flex-1 min-w-0">
          <img :src="user.avatarUrl || 'https://api.dicebear.com/7.x/thumbs/svg?seed=default'"
            class="w-12 h-12 border-2 border-foreground object-cover flex-shrink-0 rounded-md" alt="avatar" />
          <div class="min-w-0">
            <div class="font-bold text-base uppercase truncate">{{ user.fullName || user.username }}</div>
            <div class="text-sm font-bold text-muted-foreground flex items-center gap-1.5">
              @{{ user.username }}
              <span v-if="isCurrentUser(user.userId)"
                class="bg-accent text-white text-xs font-bold px-2 py-0.5 border-2 border-foreground rounded-md"
              >Bạn</span>
            </div>
          </div>
        </div>

        <!-- Level -->
        <div class="flex-shrink-0 bg-geo-bg border-2 border-foreground px-2 py-0.5 text-xs font-bold uppercase rounded-md">
          {{ user.currentLevel }}
        </div>

        <!-- Streak -->
        <div class="flex-shrink-0 font-bold uppercase tracking-wider bg-accent/10 border-2 border-foreground px-3 py-1 whitespace-nowrap rounded-md">
          <span class="text-accent">Lửa {{ user.currentStreak }} ngày</span>
        </div>

        <!-- Points -->
        <div class="flex-shrink-0 text-2xl font-black text-accent">{{ user.totalPoints }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import leaderboardService from '@/services/leaderboardService'
import { useAuthStore } from '@/store/modules/auth'
import { Trophy } from 'lucide-vue-next'

const leaderboard = ref([])
const loading = ref(true)
const authStore = useAuthStore()

onMounted(async () => {
  try {
    leaderboard.value = await leaderboardService.getLeaderboard(20)
  } catch (e) {
    console.error('Failed to load leaderboard', e)
  } finally {
    loading.value = false
  }
})

function isCurrentUser(userId) {
  return authStore.user?.id === userId
}
</script>
