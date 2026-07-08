<template>
  <div class="bg-geo-bg min-h-screen">
    <!-- Streak Banner -->
    <div class="bg-foreground border-b-2 border-foreground">
      <div class="max-w-7xl mx-auto px-6 py-3 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 bg-tertiary border-2 border-foreground flex items-center justify-center font-black text-sm rotate-12 rounded-sm">
              {{ currentStreak }}
            </div>
            <span class="text-white font-bold text-xs uppercase tracking-wider">Ngày liên tiếp</span>
          </div>
          <div class="hidden sm:flex items-center gap-2">
            <div class="w-0.5 h-5 bg-white/20"></div>
            <span class="text-white/60 font-bold text-[10px] uppercase tracking-widest">{{ streakMessage }}</span>
          </div>
        </div>
        <router-link to="/profile"
          class="text-white/80 hover:text-white font-bold text-[10px] uppercase tracking-widest border border-white/30 px-3 py-1 rounded-md hover:bg-white/10 transition-colors"
        >Xem chi tiết →</router-link>
      </div>
    </div>

    <section class="max-w-7xl mx-auto px-6 py-16">
      <div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-8 mb-12">
        <div class="relative">
          <div class="absolute -top-4 -left-4 w-8 h-8 bg-tertiary border-2 border-foreground rotate-12 rounded-sm"></div>
          <div class="absolute -bottom-2 -right-2 w-6 h-6 bg-secondary border-2 border-foreground rounded-full"></div>
          <h1 class="font-black text-5xl md:text-6xl uppercase tracking-tight leading-none relative z-10">
            Bài <span class="text-accent">học</span>
          </h1>
          <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground mt-3">Chọn bài học phù hợp với trình độ của bạn</p>
        </div>

        <div class="flex flex-wrap gap-2">
          <button v-for="lv in levels" :key="lv.value"
            @click="selectedLevel = lv.value"
            class="px-4 py-2 border-2 border-foreground font-bold text-xs uppercase tracking-wider transition-all duration-200 rounded-md shadow-pop-sm"
            :class="selectedLevel === lv.value
              ? 'bg-accent text-white'
              : 'bg-card text-foreground hover:bg-tertiary/20'"
          >{{ lv.label }}</button>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="store.loading" class="flex flex-col items-center justify-center py-32 gap-4">
        <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
        <p class="font-bold text-sm uppercase tracking-widest text-muted-foreground">Đang tải...</p>
      </div>

      <!-- Error -->
      <div v-else-if="store.error" class="max-w-xl mx-auto text-center py-20">
        <div class="inline-flex items-center justify-center w-16 h-16 border-2 border-foreground bg-accent/10 mb-6 rounded-blob">
          <span class="text-3xl font-black text-accent">!</span>
        </div>
        <p class="font-bold text-lg text-accent">{{ store.error }}</p>
      </div>

      <div v-else>
        <!-- Empty -->
        <div v-if="filteredLessons.length === 0" class="text-center py-32">
          <div class="inline-flex items-center justify-center w-20 h-20 border-2 border-foreground bg-card mb-6 shadow-pop rounded-blob">
            <span class="text-4xl font-black text-muted-foreground">?</span>
          </div>
          <p class="font-bold text-lg text-muted-foreground uppercase tracking-wider">Không tìm thấy bài học nào</p>
        </div>

        <!-- Grid -->
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <article v-for="lesson in filteredLessons" :key="lesson.id"
            class="group bg-card border-2 border-foreground shadow-pop-xl hover:-translate-y-2 hover:shadow-pop-lg transition-all duration-300 flex flex-col relative overflow-hidden rounded-md"
          >
            <div class="h-2.5 w-full" :style="{ background: levelColor(lesson.level) }"></div>

            <div class="relative aspect-[16/9] overflow-hidden bg-muted border-b-2 border-foreground">
              <div class="absolute top-2 right-2 z-10 w-6 h-6 bg-tertiary border-2 border-foreground rotate-12 rounded-sm"></div>
              <img :src="lesson.thumbnailUrl || 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=500'"
                class="w-full h-full object-cover grayscale group-hover:grayscale-0 transition-all duration-700 group-hover:scale-105" alt="" />

              <div class="absolute bottom-2 right-2 flex items-center gap-2 bg-card border-2 border-foreground px-2 py-1 shadow-pop-sm rounded-sm">
                <svg class="w-6 h-6 -rotate-90" viewBox="0 0 32 32">
                  <circle cx="16" cy="16" r="14" fill="none" stroke="#e5e7eb" stroke-width="3" />
                  <circle cx="16" cy="16" r="14" fill="none" stroke-width="3"
                    :stroke="levelColor(lesson.level)"
                    :stroke-dasharray="88"
                    :stroke-dashoffset="88 - (88 * (lesson.completionPercentage || 0) / 100)" />
                </svg>
                <span class="font-bold text-xs">{{ lesson.completionPercentage || 0 }}%</span>
              </div>
            </div>

            <div class="p-5 flex flex-col flex-1 gap-3">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="w-2.5 h-2.5 border border-foreground rounded-full" :style="{ background: levelColor(lesson.level) }"></span>
                  <span class="text-xs font-bold uppercase tracking-wider" :style="{ color: levelColor(lesson.level) }">{{ levelLabel(lesson.level) }}</span>
                </div>
                <span class="text-xs font-bold text-muted-foreground uppercase tracking-wider">{{ lesson.durationMinutes }} phút</span>
              </div>

              <div>
                <span class="text-[10px] font-bold uppercase tracking-[0.15em] text-muted-foreground">{{ lesson.category }}</span>
                <h3 class="font-black text-lg uppercase leading-snug mt-1 text-foreground">{{ lesson.title }}</h3>
              </div>

              <p class="text-sm font-medium text-muted-foreground leading-relaxed line-clamp-2 flex-1">{{ lesson.description }}</p>

              <div class="space-y-2 pt-2 border-t-2 border-foreground">
                <div class="flex justify-between text-xs font-bold uppercase tracking-wider">
                  <span class="text-muted-foreground">Tiến độ</span>
                  <span>{{ lesson.completionPercentage || 0 }}%</span>
                </div>
                <div class="w-full h-2 border border-foreground bg-muted rounded-full overflow-hidden">
                  <div class="h-full transition-all duration-1000 rounded-full" :style="{ width: `${lesson.completionPercentage || 0}%`, background: levelColor(lesson.level) }"></div>
                </div>
              </div>

              <button @click="$router.push(`/lessons/${lesson.id}`)"
                class="w-full py-3 border-2 border-foreground font-bold text-xs uppercase tracking-wider transition-all duration-200 active:translate-x-0.5 active:translate-y-0.5 rounded-full shadow-pop"
                :class="lesson.isCompleted
                  ? 'bg-secondary text-white hover:bg-secondary/90'
                  : 'bg-accent text-white hover:bg-accent/90'"
              >{{ lesson.isCompleted ? 'Đã hoàn thành' : 'Bắt đầu học' }}</button>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useLessonStore } from '@/store/modules/lesson'
import streakService from '@/services/streakService'

const store = useLessonStore()
const selectedLevel = ref('ALL')
const currentStreak = ref(0)
const streakMessage = ref('')

onMounted(async () => {
  store.fetchLessons()
  try {
    const data = await streakService.getCurrentStreak()
    currentStreak.value = data.currentStreak
    if (data.currentStreak === 0) streakMessage.value = 'Bắt đầu học ngay!'
    else if (data.currentStreak < 3) streakMessage.value = 'Đang vào guồng!'
    else if (data.currentStreak < 7) streakMessage.value = 'Đang có đà!'
    else if (data.currentStreak < 30) streakMessage.value = 'Rất ấn tượng!'
    else streakMessage.value = 'Huyền thoại!'
  } catch (e) { /* ignore */ }
})

const levels = [
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Elementary', value: 'ELEMENTARY' },
  { label: 'Pre-Int.', value: 'PRE_INTERMEDIATE' },
  { label: 'Intermediate', value: 'INTERMEDIATE' },
  { label: 'Upper-Int.', value: 'UPPER_INTERMEDIATE' },
]

const filteredLessons = computed(() => {
  if (selectedLevel.value === 'ALL') return store.lessons
  return store.lessons.filter(l => l.level === selectedLevel.value)
})

function levelColor(level) {
  return ({
    ELEMENTARY: '#8B5CF6',
    PRE_INTERMEDIATE: '#FBBF24',
    INTERMEDIATE: '#F472B6',
    UPPER_INTERMEDIATE: '#34D399',
  })[level] || '#64748B'
}

function levelLabel(level) {
  return ({
    ELEMENTARY: 'Elementary',
    PRE_INTERMEDIATE: 'Pre-Intermediate',
    INTERMEDIATE: 'Intermediate',
    UPPER_INTERMEDIATE: 'Upper-Intermediate',
  })[level] || level
}
</script>
