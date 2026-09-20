<template>
  <div
    class="streak-calendar bg-white border-2 rounded-xl p-6 shadow-pop-lg transition-colors"
    :class="studiedToday ? 'border-foreground' : 'border-tertiary'"
  >
    <div class="flex flex-wrap gap-3 items-center justify-between mb-6">
      <h3 class="text-2xl font-black uppercase">Chuỗi ngày học</h3>
      <p class="flex items-center space-x-2 text-3xl font-black text-secondary">
        <span aria-hidden="true">🔥</span>
        <span>{{ currentStreak }}</span>
        <span class="text-lg font-bold text-muted-foreground">ngày</span>
        <span class="sr-only">chuỗi ngày học liên tiếp hiện tại</span>
      </p>
    </div>
    <p class="mt-4 text-sm">✓ Đã học · Không học: ngày đã qua · Viền: hôm nay · Ô nhạt: ngày chưa đến · Viền nét đứt: ngày học cũ trước khi cách tính mới bắt đầu.</p>
    <p v-if="effectiveFrom" class="mt-2 text-sm">Từ {{ formattedEffectiveFrom }} trở đi, bạn ghi được một ngày học khi <strong>hoàn thành bài tập</strong> — chỉ mở trang thì không tính.</p>
    <div
      v-if="!studiedToday"
      class="mb-6 flex flex-col gap-3 bg-tertiary/10 border-2 border-tertiary rounded-md p-4"
    >
      <p class="font-black text-sm uppercase tracking-wider text-foreground">
        Hôm nay chưa học
      </p>
      <RouterLink
        to="/lessons"
        class="inline-flex items-center justify-center border-2 border-foreground bg-tertiary text-foreground font-black text-sm uppercase tracking-wider px-4 py-2 shadow-pop-sm hover:-translate-y-0.5 transition-transform"
      >
        Học ngay
      </RouterLink>
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
            'bg-quaternary/25 text-foreground rounded-md border-dashed': isLegacy(date),
            'bg-muted rounded-md': !isStudied(date) && !isLegacy(date) && isPast(date) && !isToday(date),
            'bg-white opacity-50 rounded-md': !isPast(date),
            'ring-2 ring-accent ring-offset-2': isToday(date)
          }"
          :title="formatDate(date)"
          :data-date="isoDate(date)"
          role="gridcell"
          :aria-label="cellLabel(date)"
        >
          <span>{{ date.getDate() }}</span>
          <span v-if="isStudied(date)" class="text-xs" aria-hidden="true">✓</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentStreak: { type: Number, default: 0 },
  history: { type: Array, default: () => [] },
  legacyHistory: { type: Array, default: () => [] },
  effectiveFrom: { type: String, default: null },
  // "Hôm nay" do server trả (ISO yyyy-MM-dd, múi giờ backend). Truyền vào để
  // lịch khớp đúng ngày học, kể cả khi máy khách ở múi giờ khác VN.
  today: { type: String, default: null }
})

const WEEKDAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']

/**
 * Date neo cho toàn bộ lịch. Parse yyyy-MM-dd thủ công thành nửa đêm GIỜ ĐỊA
 * PHƯƠNG: new Date('2026-09-10') sẽ bị hiểu là nửa đêm UTC và lệch một ngày
 * trên máy khách múi UTC-x. Nếu server chưa gửi thì fallback đồng hồ máy.
 */
const anchorToday = computed(() => {
  const iso = props.today
  if (iso && /^\d{4}-\d{2}-\d{2}$/.test(iso)) {
    const [y, m, d] = iso.split('-').map(Number)
    return new Date(y, m - 1, d)
  }
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
})

// Tra cứu nhanh các ngày đã học, thay vì includes() trên array mỗi lần render.
const studiedSet = computed(() => new Set(props.history))

const studiedToday = computed(() => studiedSet.value.has(isoDate(anchorToday.value)))

const isoDate = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

const calendarWeeks = computed(() => {
  const today = anchorToday.value
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

/**
 * Ngày có hoạt động TRƯỚC cutover — lịch sử truy cập cũ, không phải ngày hoàn
 * thành học. Điều kiện phải khớp thứ tự ưu tiên của {@link cellLabel}: ở đó
 * nhánh legacy được kiểm TRƯỚC isStudied, nên nếu hai bên lệch nhau thì một ô
 * sẽ mang màu legacy nhưng aria-label lại đọc là "đã học".
 */
const isLegacy = (date) =>
  !!props.effectiveFrom
  && isoDate(date) < props.effectiveFrom
  && props.legacyHistory.includes(isoDate(date))

const isPast = (date) => date <= anchorToday.value

const isToday = (date) => isoDate(date) === isoDate(anchorToday.value)

const formatDate = (date) => date.toLocaleDateString('vi-VN')

/** Ngày cutover hiển thị theo vi-VN; parse thủ công để không lệch múi giờ. */
const formattedEffectiveFrom = computed(() => {
  const iso = props.effectiveFrom
  if (!iso || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) return iso
  const [y, m, d] = iso.split('-').map(Number)
  return formatDate(new Date(y, m - 1, d))
})

// Trang bị cho screen reader: trạng thái không được chỉ truyền đạt bằng màu (WCAG 1.4.1).
const cellLabel = (date) => {
  if (props.effectiveFrom && isoDate(date) < props.effectiveFrom) {
    return `${formatDate(date)}: ${props.legacyHistory.includes(isoDate(date)) ? 'Lịch sử truy cập trước khi áp dụng' : 'Chưa có dữ liệu học theo quy tắc mới'}`
  }
  const state = isStudied(date)
    ? 'đã học'
    : isToday(date)
      ? 'Chưa học hôm nay'
      : isPast(date)
        ? 'Không học'
        : 'chưa đến'
  const marker = isToday(date) ? ' (hôm nay)' : ''
  return `${formatDate(date)}: ${state}${marker}`
}
</script>
