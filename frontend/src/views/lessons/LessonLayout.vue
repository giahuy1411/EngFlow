<template>
  <div class="bg-geo-bg min-h-screen">
    <StreakBanner />
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Tab Navigation -->
      <nav class="flex gap-4 border-b-2 border-gray-300 mb-6">
        <router-link :to="`/lessons/${lessonId}`"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="$route.name === 'LessonOverview' ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-foreground'">
          Tổng quan
        </router-link>
        <router-link :to="`/lessons/${lessonId}/exercises`"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="$route.name === 'LessonExercises' ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-foreground'">
          Bài tập
        </router-link>
        <router-link :to="`/lessons/${lessonId}/preview`"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="$route.name === 'LessonPreview' ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-foreground'">
          Xem trước
        </router-link>
      </nav>

      <router-view />
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import StreakBanner from './StreakBanner.vue'

const route = useRoute()
const lessonId = route.params.id
</script>
