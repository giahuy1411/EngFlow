<template>
  <div class="bg-geo-bg min-h-screen">
    <StreakBanner />
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <nav class="flex gap-4 border-b-2 border-gray-300 mb-6">
        <button @click="activeTab = 'overview'"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="activeTab === 'overview' ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-foreground'">
          Nội dung
        </button>
        <button @click="activeTab = 'history'"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="activeTab === 'history' ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-foreground'">
          Lịch sử
        </button>
      </nav>

      <LessonOverview v-if="activeTab === 'overview'" />
      <LessonHistory v-if="activeTab === 'history'" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import StreakBanner from './StreakBanner.vue'
import LessonOverview from './LessonOverview.vue'
import LessonHistory from './LessonPreview.vue'

const activeTab = ref('overview')
</script>
