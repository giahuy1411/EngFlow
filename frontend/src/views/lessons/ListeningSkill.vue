<template>
  <div>
    <div v-if="audioUrl" class="mb-6">
      <audio :src="audioUrl" controls class="w-full border-2 border-foreground bg-foreground/5 geo-audio rounded-md"></audio>
    </div>
    <div class="space-y-4">
      <div v-for="(ex, idx) in exercises" :key="idx"
        class="border-2 border-foreground rounded-md p-6 shadow-pop-lg"
      >
        <div v-if="ex.audioUrl" class="mb-4">
          <audio :src="ex.audioUrl" controls class="w-full geo-audio"></audio>
        </div>
        <div class="geo-markdown text-lg font-bold mb-4" v-html="parseMarkdown(ex.question)"></div>
        <div v-if="ex.options" class="space-y-2">
          <label v-for="(opt, oi) in ex.options" :key="oi"
            class="flex items-center gap-3 p-3 border-2 border-foreground rounded-md cursor-pointer transition-all"
            :class="answers[idx] === oi ? 'bg-accent/10 border-accent' : 'hover:bg-tertiary/10'"
          >
            <input type="radio" :name="'list-q-' + idx" :value="oi" v-model="answers[idx]" class="geo-radio" />
            <span class="font-medium">{{ opt }}</span>
          </label>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * ListeningSkill — khối nội dung kỹ năng Nghe của một bài học.
 *
 * Component THUẦN TRÌNH BÀY: nhận props, KHÔNG gọi API.
 *   - props.audioUrl  : audio chung của cả bài (thẻ <audio> đầu trang), có thể rỗng.
 *   - props.exercises : mảng câu hỏi; mỗi câu có thể kèm `ex.audioUrl` riêng.
 * Template chỉ render thẻ <audio> khi có URL tương ứng (v-if).
 *
 * ⚠️ FALLBACK GIỌNG TRÌNH DUYỆT KHÔNG NẰM Ở FILE NÀY:
 *   Khi bài LISTENING thiếu audio, cơ chế đọc bằng giọng máy của trình duyệt nằm ở
 *   `frontend/src/views/lessons/LessonExerciseTab.vue`, dùng
 *   `frontend/src/utils/speech.js` (isSpeechAvailable / blankOutForSpeech /
 *   stripLeadingNumber / speakEnglish) — `blankOutForSpeech` che `____` thành `...`
 *   để không lộ đáp án fill-blank. Grep xác nhận speech.js chỉ được import ở
 *   LessonExerciseTab.vue và ListeningGame.vue, KHÔNG ở ListeningSkill.vue.
 *   (ListeningSkill chỉ là UI tĩnh; nếu được tái dùng làm nơi phát audio thì phải
 *   tự bổ sung fallback, hiện tại chưa có.)
 *
 * An toàn XSS: Markdown luôn qua DOMPurify.sanitize trước khi v-html.
 *
 * CHƯA CHẮC: grep toàn `frontend/src` không thấy nơi import component này
 * (chỉ LessonLayout.vue được import ở views/lessons) — có thể không còn dùng.
 */
import { ref, onMounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  audioUrl: { type: String, default: '' },
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

