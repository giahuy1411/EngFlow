<template>
  <div class="px-4 py-12 max-w-7xl mx-auto">
    <div class="mb-12 text-center">
      <h1 class="font-black text-5xl md:text-6xl uppercase tracking-tight mb-4">
        Bảng <span class="text-primary-red">Xếp Hạng</span>
      </h1>
      <p class="font-medium text-base text-gray-600">Cùng đua top với các thành viên khác nhé!</p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex items-center justify-center py-16">
      <LoaderIcon class="w-10 h-10 animate-spin text-primary-red" />
    </div>

    <!-- Empty State -->
    <div v-else-if="leaderboard.length === 0" class="border-4 border-black border-dashed bg-white p-12 text-center">
      <p class="font-black text-2xl uppercase mb-4">Chưa có dữ liệu</p>
      <p class="text-base text-gray-500">Hãy tích lũy điểm để xuất hiện trên bảng xếp hạng!</p>
    </div>

    <!-- Leaderboard List -->
    <div v-else class="flex flex-col gap-4">
      <BauhausCard
        v-for="user in leaderboard"
        :key="user.userId"
        :decoration="false"
        :class="[
          '!p-4 flex items-center gap-4',
          isCurrentUser(user.userId) ? 'border-primary-blue bg-blue-50' : ''
        ]"
      >
        <!-- Rank -->
        <div class="flex-shrink-0">
          <span v-if="user.rank === 1" class="text-4xl">👑</span>
          <span v-else-if="user.rank === 2" class="text-4xl">🥈</span>
          <span v-else-if="user.rank === 3" class="text-4xl">🥉</span>
          <div v-else class="w-10 h-10 border-2 border-black bg-gray-100 flex items-center justify-center font-black text-lg">
            {{ user.rank }}
          </div>
        </div>

        <!-- Avatar + Name -->
        <div class="flex items-center gap-3 flex-1 min-w-0">
          <img :src="user.avatarUrl || 'https://api.dicebear.com/7.x/thumbs/svg?seed=default'" class="w-12 h-12 border-2 border-black object-cover flex-shrink-0" alt="avatar" />
          <div class="min-w-0">
            <div class="font-bold text-base uppercase truncate">{{ user.fullName || user.username }}</div>
            <div class="text-sm text-gray-500 flex items-center gap-1.5">
              @{{ user.username }}
              <span v-if="isCurrentUser(user.userId)" class="bg-primary-blue text-white text-xs font-bold px-2 py-0.5 border-2 border-black">
                Bạn
              </span>
            </div>
          </div>
        </div>

        <!-- Level -->
        <div class="flex-shrink-0 bg-gray-100 border-2 border-black px-2 py-0.5 text-xs font-bold uppercase">
          {{ user.currentLevel }}
        </div>

        <!-- Streak -->
        <div class="flex-shrink-0 font-bold whitespace-nowrap">
          🔥 {{ user.currentStreak }} ngày
        </div>

        <!-- Points -->
        <div class="flex-shrink-0 text-2xl font-black text-primary-red">
          {{ user.totalPoints }}
        </div>
      </BauhausCard>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import leaderboardService from '@/services/leaderboardService'
import { useAuthStore } from '@/store/modules/auth'
import { Loader2 as LoaderIcon } from 'lucide-vue-next'
import BauhausCard from '@/components/bauhaus/BauhausCard.vue'

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
