<template>
  <div class="bg-background min-h-screen">
    <StreakBanner :streak="currentStreak" v-if="isLoggedIn" />
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- audit-v13 F-13-18: this route rendered no h1 at all (headings started at h3),
           so screen-reader and outline users got no page title. The lesson title is the
           natural h1. -->
      <h1 class="mb-4 text-3xl font-black uppercase tracking-tight">{{ lessonTitle || 'Bài học' }}</h1>
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
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import StreakBanner from './StreakBanner.vue'
import LessonContent from './LessonContent.vue'
import LessonExerciseTab from './LessonExerciseTab.vue'
import LessonPreview from './LessonPreview.vue'
import GuestCtaCard from '@/components/common/GuestCtaCard.vue'
import streakService from '@/services/streakService'
import lessonService from '@/services/lessonService'

const auth = useAuthStore()
const route = useRoute()
const isLoggedIn = computed(() => auth.isLoggedIn)
const currentStreak = ref(0)
// audit-v13 F-13-18: title for the page h1.
const lessonTitle = ref('')

onMounted(async () => {
  // Load the title for the h1 regardless of auth (the route is public).
  try {
    const lesson = await lessonService.getById(Number(route.params.id))
    lessonTitle.value = lesson?.title || ''
  } catch (e) { /* h1 falls back to "Bài học" */ }

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
