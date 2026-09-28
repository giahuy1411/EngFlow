<template>
  <header
    class="user-page-header flex flex-col md:flex-row justify-between items-start md:items-end gap-8"
    :class="[rootClass, { 'border-b-2 border-foreground pb-8': divided }]"
  >
    <div class="relative min-w-0">
      <div class="absolute -top-4 -left-4 w-8 h-8 bg-tertiary border-2 border-foreground rotate-12 rounded-sm" aria-hidden="true"></div>
      <div class="absolute -bottom-2 -right-2 w-6 h-6 bg-secondary border-2 border-foreground rounded-full" aria-hidden="true"></div>
      <h1 :id="titleId || undefined" class="font-black text-5xl md:text-6xl uppercase tracking-tight leading-none relative z-10 break-words">
        <slot name="title">
          <!-- Space phải nằm trong chuỗi runtime ('{{ prefix }}' + ' '): template compiler
               trim whitespace source (mất space), còn &nbsp; thì tạo token không wrap → tràn mobile. -->
          <span v-if="titleParts.prefix">{{ titleParts.prefix + ' ' }}</span><span class="text-accent-ink">{{ titleParts.highlight }}</span>
        </slot>
      </h1>
      <p v-if="subtitle || eyebrow" class="font-bold text-sm uppercase tracking-wider text-muted-foreground mt-3">
        {{ subtitle || eyebrow }}
      </p>
    </div>

    <div v-if="$slots.actions" class="flex flex-wrap gap-2 md:justify-end">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
/*
 * UserPageHeader — tiêu đề trang dùng chung cho các trang phía người dùng
 * (Profile, Luyện tập...). Gồm tiêu đề lớn + phụ đề, kèm hai hoạ tiết hình học
 * trang trí (aria-hidden) đặt lệch góc.
 *
 * Props:
 *   - title     (String): tiêu đề. Từ CUỐI CÙNG được tô màu accent (xem titleParts).
 *   - subtitle  (String): phụ đề; nếu rỗng thì fallback sang `eyebrow`.
 *   - eyebrow   (String): nhãn nhỏ thay thế khi không có subtitle.
 *   - rootClass (String): class Tailwind bổ sung gắn vào thẻ <header>.
 *   - titleId   (String): id gán cho <h1> để nơi khác aria-labelledby tới được.
 *   - divided   (Boolean): true thì thêm đường kẻ dưới (border-b) ngăn cách.
 *
 * Slots:
 *   - title   : thay thế toàn bộ nội dung <h1> (mặc định là prefix + từ highlight).
 *   - actions : khu vực nút hành động bên phải; chỉ render khi slot được truyền.
 *
 * Không emit. Luồng dữ liệu: titleParts tách `title` thành phần đầu (prefix) và
 * từ cuối (highlight) để tô màu accent cho từ cuối.
 */
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  eyebrow: { type: String, default: '' },
  rootClass: { type: String, default: '' },
  titleId: { type: String, default: '' },
  divided: { type: Boolean, default: false },
})

const titleParts = computed(() => {
  const words = props.title.trim().split(/\s+/).filter(Boolean)
  if (words.length <= 1) return { prefix: '', highlight: props.title }
  return {
    prefix: words.slice(0, -1).join(' '),
    highlight: words.at(-1),
  }
})
</script>
