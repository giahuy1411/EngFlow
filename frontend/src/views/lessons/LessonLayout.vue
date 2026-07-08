<template>
  <div class="bg-geo-bg min-h-screen">
    <StreakBanner />
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import StreakBanner from './StreakBanner.vue'
</script>
