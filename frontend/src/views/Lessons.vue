<template>
  <div class="bg-background min-h-screen border-b-4 border-black">
    <!-- Header Section -->
    <section class="bg-primary-blue border-b-4 border-black py-16 px-4">
      <div class="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-start md:items-end gap-8">
        <div>
          <h2 class="font-black text-5xl md:text-7xl uppercase text-white tracking-tighter mb-4 shadow-black text-shadow-hard">CHƯƠNG TRÌNH HỌC</h2>
          <p class="font-bold text-xl uppercase bg-white text-black inline-block px-4 py-2 border-2 border-black shadow-[4px_4px_0px_0px_black]">Lựa chọn bài học phù hợp</p>
        </div>

        <!-- Filter Buttons -->
        <div class="flex flex-wrap gap-4">
          <BauhausButton 
            v-for="level in levels" 
            :key="level.value"
            :variant="selectedLevel === level.value ? 'yellow' : 'outline'"
            size="sm"
            @click="selectedLevel = level.value"
          >
            {{ level.label }}
          </BauhausButton>
        </div>
      </div>
    </section>

    <div class="max-w-7xl mx-auto py-16 px-4">
      <!-- Loading and Error State -->
      <div v-if="store.loading" class="text-center py-20">
        <LoaderIcon class="w-12 h-12 animate-spin mx-auto text-primary-red mb-4" />
        <p class="font-bold uppercase tracking-widest text-xl">Đang tải...</p>
      </div>
      <div v-else-if="store.error" class="bg-primary-red text-white p-6 border-4 border-black shadow-[8px_8px_0px_0px_black] font-bold text-lg uppercase">
        {{ store.error }}
      </div>

      <!-- Lessons Grid -->
      <div v-else>
        <div v-if="filteredLessons.length === 0" class="text-center py-20 border-4 border-black border-dashed bg-white">
          <p class="font-bold text-xl uppercase">Không tìm thấy bài học nào phù hợp.</p>
        </div>
        
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <BauhausCard 
            v-for="(lesson, index) in filteredLessons" 
            :key="lesson.id"
            :decorationColor="['red', 'blue', 'yellow'][index % 3]"
            :decorationShape="['circle', 'square', 'triangle'][index % 3]"
            class="flex flex-col h-full !p-0 overflow-hidden group"
          >
            <!-- Thumbnail -->
            <div class="w-full aspect-video bg-black border-b-4 border-black overflow-hidden relative">
              <img :src="lesson.thumbnailUrl || 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=500'" 
                   class="w-full h-full object-cover grayscale group-hover:grayscale-0 transition-all duration-300" alt="lesson thumbnail" />
              <!-- Level Badge -->
              <span class="absolute top-4 left-4 border-2 border-black px-3 py-1 font-bold text-sm uppercase tracking-widest shadow-[2px_2px_0px_0px_black]" :class="levelBadgeClass(lesson.level)">
                {{ lesson.level }}
              </span>
            </div>

            <!-- Card Body -->
            <div class="p-6 flex flex-col flex-grow">
              <span class="font-bold text-xs uppercase tracking-widest text-primary-red mb-2">{{ lesson.category }}</span>
              <h5 class="font-black text-2xl uppercase leading-tight mb-3">{{ lesson.title }}</h5>
              <p class="font-medium text-muted-foreground mb-6 line-clamp-3">{{ lesson.description }}</p>
              
              <!-- Progress Bar -->
              <div class="mb-6 mt-auto">
                <div class="flex justify-between font-bold text-sm uppercase mb-2">
                  <span>Tiến độ</span>
                  <span>{{ lesson.completionPercentage || 0 }}%</span>
                </div>
                <div class="w-full h-4 border-2 border-black bg-white">
                  <div class="h-full bg-primary-blue transition-all" :style="{ width: `${lesson.completionPercentage || 0}%` }"></div>
                </div>
              </div>

              <!-- Footer Details -->
              <div class="flex justify-between items-center pt-4 border-t-2 border-black border-dashed">
                <span class="font-bold text-sm uppercase flex items-center gap-1">
                  <ClockIcon class="w-4 h-4" /> {{ lesson.durationMinutes }} phút
                </span>
                <BauhausButton 
                  :variant="lesson.isCompleted ? 'yellow' : 'primary'" 
                  size="sm"
                  @click="$router.push(`/lessons/${lesson.id}`)"
                >
                  {{ lesson.isCompleted ? 'ĐÃ XONG' : 'HỌC NGAY' }}
                </BauhausButton>
              </div>
            </div>
          </BauhausCard>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useLessonStore } from '@/store/modules/lesson'
import BauhausButton from '@/components/bauhaus/BauhausButton.vue'
import BauhausCard from '@/components/bauhaus/BauhausCard.vue'
import { Loader2 as LoaderIcon, Clock as ClockIcon } from 'lucide-vue-next'

const store = useLessonStore()
const selectedLevel = ref('ALL')

const levels = [
  { label: 'TẤT CẢ', value: 'ALL' },
  { label: 'BEGINNER', value: 'BEGINNER' },
  { label: 'INTERMEDIATE', value: 'INTERMEDIATE' },
  { label: 'ADVANCED', value: 'ADVANCED' }
]

onMounted(() => {
  store.fetchLessons()
})

const filteredLessons = computed(() => {
  if (selectedLevel.value === 'ALL') return store.lessons
  return store.lessons.filter(l => l.level === selectedLevel.value)
})

function levelBadgeClass(level) {
  switch (level) {
    case 'BEGINNER': return 'bg-primary-yellow text-black'
    case 'INTERMEDIATE': return 'bg-primary-blue text-white'
    case 'ADVANCED': return 'bg-primary-red text-white'
    default: return 'bg-white text-black'
  }
}
</script>

<style scoped>
.text-shadow-hard {
  text-shadow: 4px 4px 0px rgba(18, 18, 18, 1);
}
</style>
