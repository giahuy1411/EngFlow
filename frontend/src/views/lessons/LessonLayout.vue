<template>
  <div class="bg-geo-bg min-h-screen">
    <StreakBanner />
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <nav class="flex gap-4 border-b-2 border-gray-300 mb-6">
        <button @click="activeTab = 'overview'"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="activeTab === 'overview' ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-foreground'">
          Tổng quan
        </button>
        <button @click="activeTab = 'exercises'"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="activeTab === 'exercises' ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-foreground'">
          Bài tập
        </button>
        <button @click="activeTab = 'preview'"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="activeTab === 'preview' ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-foreground'">
          Xem trước
        </button>
      </nav>

      <LessonOverview v-if="activeTab === 'overview'" />
      <LessonExercise v-if="activeTab === 'exercises'" />
      <LessonPreview v-if="activeTab === 'preview'" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import StreakBanner from './StreakBanner.vue'
import LessonOverview from './LessonOverview.vue'
import LessonExercise from './LessonExercise.vue'
import LessonPreview from './LessonPreview.vue'

const route = useRoute()
const lessonId = route.params.id
const activeTab = ref('overview')
</script>
