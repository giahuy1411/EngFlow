<template>
  <div class="flex items-center gap-4">
    <div v-if="icon" class="w-10 h-10 rounded-md flex items-center justify-center border-2 border-foreground"
      :class="iconBg">
      <component :is="icon" class="w-5 h-5" :class="iconColor || 'text-foreground'" />
    </div>
    <div>
      <h2 class="font-black text-2xl uppercase tracking-tight">{{ title }}</h2>
      <p v-if="subtitle" class="font-bold text-xs uppercase tracking-widest text-muted-foreground">{{ subtitle }}</p>
    </div>
  </div>
</template>

<script setup>
/*
 * PageHeader — khối tiêu đề cấp mục trong trang: một icon vuông (tuỳ chọn) đặt
 * cạnh tiêu đề và phụ đề. Là component layout, không gọi API và không biết routing.
 *
 * Props:
 *   - title     (String, bắt buộc): tiêu đề chính.
 *   - subtitle  (String): dòng phụ đề nhỏ phía dưới, rỗng thì ẩn.
 *   - icon      (Object | Function | String): component icon truyền vào và render
 *               bằng <component :is="icon">; không truyền thì ẩn cả ô icon.
 *   - iconBg    (String): class Tailwind nền cho ô icon, mặc định 'bg-accent'.
 *   - iconColor (String): class màu cho icon, mặc định 'text-white'.
 *
 * Không slot, không emit. (Khác UserPageHeader ở chỗ có icon và phụ đề cố định.)
 */
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: [Object, Function, String], default: undefined },
  iconBg: { type: String, default: 'bg-accent' },
  iconColor: { type: String, default: 'text-white' },
})
</script>
