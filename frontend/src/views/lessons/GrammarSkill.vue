<template>
  <div>
    <!-- Grammar Skill Content -->
    <div class="geo-markdown" v-html="parseMarkdown(content)"></div>
    <div v-if="exercises.length > 0" class="mt-8 space-y-4">
      <div v-for="(ex, idx) in exercises" :key="idx"
        class="border-2 border-foreground rounded-md p-6 shadow-pop-lg"
      >
        <div class="geo-markdown text-lg font-bold mb-4" v-html="parseMarkdown(ex.question)"></div>
        <div v-if="ex.options" class="space-y-2">
          <label v-for="(opt, oi) in ex.options" :key="oi"
            class="flex items-center gap-3 p-3 border-2 border-foreground rounded-md cursor-pointer transition-all"
            :class="answers[idx] === oi ? 'bg-accent/10 border-accent' : 'hover:bg-tertiary/10'"
          >
            <input type="radio" :name="'gram-q-' + idx" :value="oi" v-model="answers[idx]" class="geo-radio" />
            <span class="font-medium">{{ opt }}</span>
          </label>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * GrammarSkill — khối nội dung kỹ năng Ngữ pháp (grammar) của một bài học.
 *
 * Component THUẦN TRÌNH BÀY (presentational): nhận dữ liệu qua props, KHÔNG gọi API.
 *   - props.content   : nội dung lý thuyết dạng Markdown → render qua parseMarkdown.
 *   - props.exercises : mảng câu hỏi trắc nghiệm ({ question, options[] }).
 *
 * Trạng thái `answers` là cục bộ (local) — chọn radio chỉ lưu trong component,
 * KHÔNG emit lên cha và KHÔNG nộp bài (không có nút submit/không gọi service).
 *
 * An toàn XSS: mọi Markdown đều qua DOMPurify.sanitize trước khi v-html
 * (nội dung có thể đến từ dữ liệu bài học/AI, phải coi là không tin cậy).
 *
 * CHƯA CHẮC: grep toàn `frontend/src` không thấy nơi import component này
 * (chỉ LessonLayout.vue được import ở views/lessons) — có thể là component
 * minh hoạ/không còn dùng; comment mô tả đúng hành vi code hiện tại.
 */
import { ref, onMounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  content: { type: String, default: '' },
  exercises: { type: Array, default: () => [] },
})

const answers = ref([])
onMounted(() => { answers.value = props.exercises.map(() => null) })

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}
</script>

<style scoped>
input.geo-radio {
  appearance: none;
  width: 20px; height: 20px;
  border: 2px solid var(--geo-fg, #1E293B);
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 300ms cubic-bezier(0.34, 1.56, 0.64, 1);
  flex-shrink: 0;
}
input.geo-radio:checked {
  border-color: var(--geo-accent, #8B5CF6);
  background: var(--geo-accent, #8B5CF6);
  box-shadow: inset 0 0 0 3px white;
}
</style>
