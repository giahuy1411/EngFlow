<template>
  <div class="streak-calendar bg-white border-2 border-foreground rounded-xl p-6 shadow-pop-lg">
    <div class="flex items-center justify-between mb-6">
      <h3 class="text-2xl font-black uppercase">Chuỗi ngày học</h3>
      <p class="flex items-center space-x-2 text-3xl font-black text-secondary">
        <span aria-hidden="true">🔥</span>
        <span>{{ currentStreak }}</span>
        <span class="text-lg font-bold text-muted-foreground">ngày</span>
        <span class="sr-only">chuỗi ngày học liên tiếp hiện tại</span>
      </p>
    </div>
    <!--
      Moi hang la mot the that (khong dung display:contents).
      display:contents bi Chrome loai kho accessibility tree, khien
      gridcell moc noi khong con grid/row chua -> ARIA khong hop le.
      flex-col + gap-2 va grid-cols-7 gap-2 cho ket qua spacing giong het ban grid goc.
    -->
    <div class="flex flex-col gap-2" role="grid" aria-label="Lịch học 4 tuần gần nhất">
      <div class="grid grid-cols-7 gap-2" role="row">
        <div
          v-for="day in WEEKDAYS"
          :key="day"
          class="text-center font-bold text-muted-foreground text-sm"
          role="columnheader"
        >{{ day }}</div>
      </div>
      <div v-for="(week, wi) in calendarWeeks" :key="wi" class="grid grid-cols-7 gap-2" role="row">
        <div
          v-for="date in week"
          :key="isoDate(date)"
          class="aspect-square border-2 border-foreground flex items-center justify-center transition-all duration-200"
          :class="{
            'bg-secondary text-foreground font-bold rounded-blob scale-105': isStudied(date),
            'bg-muted rounded-md': !isStudied(date) && isPast(date),
            'bg-white opacity-50 rounded-md': !isPast(date),
            'ring-2 ring-accent ring-offset-2': isToday(date)
          }"
          :title="formatDate(date)"
          role="gridcell"
          :aria-label="cellLabel(date)"
        >
          <span v-if="isStudied(date)" class="text-xs" aria-hidden="true">🔥</span>
        </div>
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

const WEEKDAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']

// Tra cuu nhanh cac ngay da hoc, thay vi includes() tren array moi lan render.
const studiedSet = computed(() => new Set(props.history))

const isoDate = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

const calendarWeeks = computed(() => {
  const today = new Date()
  const dayOfWeek = today.getDay() || 7 // CN(0) → 7, khớp cột T2..CN
  // Hàng cuối của lưới luôn là TUẦN HIỆN TẠI: bắt đầu từ Thứ Hai của
  // 3 tuần trước → 28 ô = 4 hàng tuần chuẩn, hôm nay luôn nằm ở hàng cuối.
  const firstCell = new Date(today)
  firstCell.setDate(today.getDate() - (dayOfWeek - 1) - 21)

  const weeks = []
  for (let w = 0; w < 4; w++) {
    const week = []
    for (let i = 0; i < 7; i++) {
      const d = new Date(firstCell)
      d.setDate(firstCell.getDate() + w * 7 + i)
      week.push(d)
    }
    weeks.push(week)
  }
  return weeks
})

const isStudied = (date) => studiedSet.value.has(isoDate(date))

const isPast = (date) => {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date <= today
}

const isToday = (date) => {
  const today = new Date()
  return date.getDate() === today.getDate() && date.getMonth() === today.getMonth() && date.getFullYear() === today.getFullYear()
}

const formatDate = (date) => date.toLocaleDateString('vi-VN')

// Trang bi cho screen reader: trang thai khong duoc chi truyen dat bang mau (WCAG 1.4.1).
const cellLabel = (date) => {
  const state = isStudied(date) ? 'đã học' : isPast(date) ? 'chưa học' : 'chưa đến'
  const marker = isToday(date) ? ' (hôm nay)' : ''
  return `${formatDate(date)}: ${state}${marker}`
}
</script>
