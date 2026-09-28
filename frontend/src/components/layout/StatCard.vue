<template>
  <div class="bg-card border-2 border-foreground rounded-md p-5 shadow-pop-lg">
    <div class="flex items-center gap-3 mb-3">
      <div class="w-10 h-10 rounded-md flex items-center justify-center border-2 border-foreground"
        :class="iconBg">
        <component :is="icon" class="w-5 h-5" :class="iconColor || 'text-foreground'" />
      </div>
      <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">{{ label }}</span>
    </div>
    <p class="font-black text-3xl">{{ value }}</p>
  </div>
</template>

<script setup>
/*
 * StatCard — thẻ hiển thị một chỉ số thống kê (số bài đã làm, streak, điểm...).
 * Gồm ô icon, nhãn nhỏ phía trên và giá trị lớn bên dưới.
 *
 * Props:
 *   - label     (String, bắt buộc): nhãn mô tả chỉ số.
 *   - value     (String | Number): giá trị hiển thị, mặc định 0.
 *   - icon      (Object | Function | String, bắt buộc): component icon, render
 *               bằng <component :is="icon">.
 *   - iconBg    (String): class nền ô icon, mặc định 'bg-accent/10'.
 *   - iconColor (String): class màu icon, mặc định 'text-accent-ink'.
 *
 * Không slot, không emit. Là component trình bày thuần — dữ liệu do component
 * cha truyền vào.
 */
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: 0 },
  icon: { type: [Object, Function, String], required: true },
  iconBg: { type: String, default: 'bg-accent/10' },
  iconColor: { type: String, default: 'text-accent-ink' },
})
</script>
