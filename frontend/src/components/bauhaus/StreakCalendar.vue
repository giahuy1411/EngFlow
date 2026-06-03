<template>
  <div class="streak-calendar bg-white border-4 border-black rounded-xl p-6 shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
    <div class="flex items-center justify-between mb-6">
      <h3 class="text-2xl font-black uppercase">Study Streak</h3>
      <div class="flex items-center space-x-2">
        <span class="text-3xl">🔥</span>
        <span class="text-3xl font-black text-orange-500">{{ currentStreak }}</span>
        <span class="text-lg font-bold text-gray-500">Days</span>
      </div>
    </div>
    
    <div class="grid grid-cols-7 gap-2">
      <div v-for="day in ['M','T','W','T','F','S','S']" :key="day" class="text-center font-bold text-gray-400 text-sm">
        {{ day }}
      </div>
      
      <div 
        v-for="(date, i) in calendarDays" 
        :key="i"
        class="aspect-square rounded-md border-2 border-black flex items-center justify-center transition-colors"
        :class="{
          'bg-orange-400 text-white font-bold': isStudied(date),
          'bg-gray-100': !isStudied(date) && isPast(date),
          'bg-white opacity-50': !isPast(date),
          'ring-2 ring-blue-500 ring-offset-2': isToday(date)
        }"
        :title="formatDate(date)"
      >
        <span v-if="isStudied(date)" class="text-xs">🔥</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentStreak: {
    type: Number,
    default: 0
  },
  history: {
    type: Array,
    default: () => []
  }
})

// Generate last 28 days for the grid (4 weeks)
const calendarDays = computed(() => {
  const days = []
  const today = new Date()
  
  // Find the Monday of the week that contains the start date (28 days ago)
  const startDate = new Date(today)
  startDate.setDate(today.getDate() - 27)
  
  const dayOfWeek = startDate.getDay() || 7 // 1 (Mon) to 7 (Sun)
  startDate.setDate(startDate.getDate() - (dayOfWeek - 1))
  
  for (let i = 0; i < 28; i++) {
    const d = new Date(startDate)
    d.setDate(startDate.getDate() + i)
    days.push(d)
  }
  
  return days
})

const isStudied = (date) => {
  const dateStr = date.toISOString().split('T')[0]
  return props.history.some(h => h?.studyDate?.startsWith(dateStr) && h.wordsStudied > 0)
}

const isPast = (date) => {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date <= today
}

const isToday = (date) => {
  const today = new Date()
  return date.getDate() === today.getDate() && 
         date.getMonth() === today.getMonth() && 
         date.getFullYear() === today.getFullYear()
}

const formatDate = (date) => {
  return date.toLocaleDateString()
}
</script>
