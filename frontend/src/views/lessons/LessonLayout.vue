<template>
  <div class="bg-geo-bg min-h-screen">
    <StreakBanner />
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div role="tablist" aria-label="Nội dung bài học" class="flex gap-4 border-b-2 border-gray-300 mb-6">
        <button role="tab" :aria-selected="activeTab === 'overview'" :aria-controls="'tabpanel-overview'"
          :tabindex="activeTab === 'overview' ? 0 : -1"
          @click="activeTab = 'overview'" @keydown.left.right.prevent="switchTab"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="activeTab === 'overview' ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-foreground'">
          Nội dung
        </button>
        <button role="tab" :aria-selected="activeTab === 'history'" :aria-controls="'tabpanel-history'"
          :tabindex="activeTab === 'history' ? 0 : -1"
          @click="activeTab = 'history'" @keydown.left.right.prevent="switchTab"
          class="px-4 py-2 font-black uppercase text-sm tracking-wider border-b-2 transition-colors"
          :class="activeTab === 'history' ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-foreground'">
          Lịch sử
        </button>
      </div>

      <div v-if="activeTab === 'overview'" id="tabpanel-overview" role="tabpanel" aria-label="Nội dung">
        <LessonOverview />
      </div>
      <div v-if="activeTab === 'history'" id="tabpanel-history" role="tabpanel" aria-label="Lịch sử">
        <LessonHistory />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import StreakBanner from './StreakBanner.vue'
import LessonOverview from './LessonOverview.vue'
import LessonHistory from './LessonPreview.vue'

const activeTab = ref('overview')

function switchTab(e) {
  const tabs = ['overview', 'history']
  const cur = tabs.indexOf(activeTab.value)
  if (e.key === 'ArrowLeft') {
    activeTab.value = tabs[(cur - 1 + tabs.length) % tabs.length]
  } else if (e.key === 'ArrowRight') {
    activeTab.value = tabs[(cur + 1) % tabs.length]
  }
}
</script>
