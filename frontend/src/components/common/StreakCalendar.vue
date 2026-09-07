<template>
  <div class="streak-calendar bg-white border-2 border-foreground rounded-xl p-6 shadow-pop-lg">
    <div class="flex items-center justify-between mb-6">
      <h3 class="text-2xl font-black uppercase">Chuỗi ngày học</h3>
      <div class="flex items-center space-x-2">
        <span class="text-3xl" aria-hidden="true">🔥</span>
        <span class="text-3xl font-black text-secondary">{{ currentStreak }}</span>
        <span class="text-lg font-bold text-muted-foreground">ngày</span>
      </div>
    </div>
    <div class="grid grid-cols-7 gap-2">
      <div v-for="day in ['T2','T3','T4','T5','T6','T7','CN']" :key="day" class="text-center font-bold text-muted-foreground text-sm">{{ day }}</div>
      <div v-for="(date, i) in calendarDays" :key="i" class="aspect-square border-2 border-foreground flex items-center justify-center transition-all duration-200" :class="{'bg-secondary text-foreground font-bold rounded-blob scale-105': isStudied(date), 'bg-muted rounded-md': !isStudied(date) && isPast(date), 'bg-white opacity-50 rounded-md': !isPast(date), 'ring-2 ring-accent ring-offset-2': isToday(date)}" :title="formatDate(date)">
        <span v-if="isStudied(date)" class="text-xs" aria-hidden="true">🔥</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentStreak: { type: Number, default: 0 },
  history: { type: Array, default: () => [] }
})

const calendarDays = computed(() => {
  const days = []
  const today = new Date()
  const dayOfWeek = today.getDay() || 7 // CN(0) → 7, khớp cột T2..CN
  // Hàng cuối của lưới luôn là TUẦN HIỆN TẠI: bắt đầu từ Thứ Hai của
  // 3 tuần trước → 28 ô = 4 hàng tuần chuẩn, hôm nay luôn nằm ở hàng cuối.
  const firstCell = new Date(today)
  firstCell.setDate(today.getDate() - (dayOfWeek - 1) - 21)
  for (let i = 0; i < 28; i++) {
    const d = new Date(firstCell)
    d.setDate(firstCell.getDate() + i)
    days.push(d)
  }
  return days
})

const isStudied = (date) => {
  const dateStr = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  return props.history.includes(dateStr)
}

const isPast = (date) => {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date <= today
}

const isToday = (date) => {
  const today = new Date()
  return date.getDate() === today.getDate() && date.getMonth() === today.getMonth() && date.getFullYear() === today.getFullYear()
}

const formatDate = (date) => {
  return date.toLocaleDateString('vi-VN')
}
</script>
