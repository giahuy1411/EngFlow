<template>
  <div class="bg-background min-h-screen">
    <StreakBanner :streak="currentStreak" v-if="isLoggedIn" />
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Tab bar -->
      <div role="tablist" aria-label="Nội dung bài học" class="flex gap-4 border-b-2 border-border mb-6">
        <button v-for="tab in tabs" :key="tab.id" role="tab"
          :aria-selected="activeTab === tab.id" :aria-controls="'tabpanel-' + tab.id"
          :tabindex="activeTab === tab.id ? 0 : -1"
          @click="activeTab = tab.id" @keydown.left.right.prevent="switchTab"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="activeTab === tab.id ? 'border-accent text-accent-ink' : 'border-transparent text-muted-foreground hover:text-foreground'">
          {{ tab.label }}
        </button>
      </div>

      <!-- Tab panels -->
      <div v-if="activeTab === 'content'" :id="'tabpanel-content'" role="tabpanel" aria-label="Nội dung">
        <LessonContent />
      </div>
      <div v-else-if="activeTab === 'exercises'" :id="'tabpanel-exercises'" role="tabpanel" aria-label="Bài tập">
        <LessonExerciseTab v-if="isLoggedIn" />
        <GuestCtaCard v-else title="Đăng nhập để làm bài tập" />
      </div>
      <div v-else-if="activeTab === 'history'" :id="'tabpanel-history'" role="tabpanel" aria-label="Lịch sử">
        <LessonPreview v-if="isLoggedIn" />
        <GuestCtaCard v-else title="Đăng nhập để lưu lịch sử học" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import StreakBanner from './StreakBanner.vue'
import LessonContent from './LessonContent.vue'
import LessonExerciseTab from './LessonExerciseTab.vue'
import LessonPreview from './LessonPreview.vue'
import GuestCtaCard from '@/components/common/GuestCtaCard.vue'
import streakService from '@/services/streakService'

const auth = useAuthStore()
const isLoggedIn = computed(() => auth.isLoggedIn)
const currentStreak = ref(0)

onMounted(async () => {
  if (!isLoggedIn.value) return
  try {
    const streak = await streakService.getCurrentStreak()
    currentStreak.value = streak.currentStreak || 0
  } catch (e) { /* ignore — banner vẫn hiển thị 0 */ }
})

const tabs = [
  { id: 'content', label: 'Nội dung' },
  { id: 'exercises', label: 'Bài tập' },
  { id: 'history', label: 'Lịch sử' },
]
const activeTab = ref('content')

function switchTab(e) {
  const ids = tabs.map(t => t.id)
  const cur = ids.indexOf(activeTab.value)
  if (e.key === 'ArrowLeft') {
    activeTab.value = ids[(cur - 1 + ids.length) % ids.length]
  } else if (e.key === 'ArrowRight') {
    activeTab.value = ids[(cur + 1) % ids.length]
  }
}
</script>
