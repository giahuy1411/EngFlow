<template>
  <div class="border-2 border-foreground rounded-md overflow-hidden bg-card shadow-pop-lg">
    <div class="p-5">
      <!-- Question -->
      <div class="geo-markdown mb-5 text-base" v-html="parseMarkdown(exercise.question)"></div>

      <!-- Matching grid: 2 columns -->
      <div class="grid grid-cols-2 gap-4">
        <!-- Left column: terms -->
        <div class="space-y-3">
          <div v-for="(item, i) in leftItems" :key="'l-' + i"
            @click="selectLeft(i)"
            class="p-3 border-2 rounded-md cursor-pointer font-bold text-sm transition-all duration-200 text-center"
            :class="leftClass(i)">
            {{ item }}
          </div>
        </div>

        <!-- Right column: definitions (shuffled) -->
        <div class="space-y-3">
          <div v-for="(item, i) in rightItems" :key="'r-' + i"
            @click="selectRight(i)"
            class="p-3 border-2 rounded-md cursor-pointer font-medium text-sm transition-all duration-200 text-center"
            :class="rightClass(i)">
            {{ item }}
          </div>
        </div>
      </div>

      <!-- Connection lines visual (shown after both selected) -->
      <div v-if="pendingLeft !== null && pendingRight !== null" class="mt-4 p-3 bg-secondary/10 border-2 border-secondary rounded-md">
        <p class="font-bold text-xs uppercase tracking-wider text-center">
          {{ leftItems[pendingLeft] }} ↔ {{ rightItems[pendingRight] }}
        </p>
      </div>

      <!-- audit-v5: malformed/empty MATCHING rows (seeded MC data mislabeled)
           would render two empty columns — show the question with a text input
           instead of a broken matching grid. -->
      <div v-if="unusable" class="space-y-3">
        <input v-model="fallbackAnswer" type="text"
          :aria-label="'Câu trả lời nối từ'"
          placeholder="Nhập câu trả lời..."
          :disabled="revealed"
          class="w-full border-2 border-foreground p-4 text-lg font-bold focus:outline-none focus:ring-4 focus:ring-tertiary transition-all rounded-md shadow-pop-sm" />
        <AppButton v-if="!revealed" @click="emitFallback" variant="pink" :disabled="!fallbackAnswer.trim()">
          Kiểm tra
        </AppButton>
      </div>

      <!-- Matched pairs -->
      <div v-else-if="matchedPairs.length > 0" class="mt-4 space-y-2">
        <p class="font-bold text-xs uppercase tracking-wider text-muted-foreground mb-2">Đã nối:</p>
        <div v-for="(pair, i) in matchedPairs" :key="'m-' + i"
          class="flex items-center gap-3 p-2.5 bg-quaternary/10 border-2 border-quaternary rounded-md">
          <span class="font-black text-xs text-foreground uppercase">{{ i + 1 }}</span>
          <span class="font-bold text-sm truncate">{{ leftItems[pair.left] }}</span>
          <span class="text-muted-foreground" aria-hidden="true">→</span>
          <span class="font-medium text-sm truncate">{{ rightItems[pair.right] }}</span>
          <button @click="removePair(i)" aria-label="Xóa cặp đã chọn" class="ml-auto w-6 h-6 flex items-center justify-center border-2 border-foreground rounded-md text-muted-foreground hover:bg-accent-strong hover:text-white transition-all font-black text-xs">&times;</button>
        </div>
      </div>

      <!-- Result badge (server-graded via `result` prop; correct pairs only known after /grade) -->
      <div v-if="revealed && !unusable" class="mt-4 p-4 rounded-md border-2" role="alert" aria-live="assertive"
        :class="!graded ? 'bg-secondary/10 border-secondary' : (result.isCorrect ? 'bg-quaternary/10 border-quaternary' : 'bg-accent/10 border-accent')">
        <div class="flex items-center gap-2 font-black text-sm uppercase mb-1">
          <span>{{ !graded ? 'Đang chấm…' : (result.isCorrect ? '✓ Đúng' : '✕ Sai') }}</span>
        </div>
        <p v-if="graded && result.correctAnswer" class="font-bold text-sm">Đáp án đúng: <span class="text-foreground font-black">{{ result.correctAnswer }}</span></p>
        <p v-if="exercise.explanation" class="mt-2 text-sm text-muted-foreground italic" v-html="sanitizeText(exercise.explanation)"></p>
      </div>
      <!-- Fallback result badge (unusable matching data) -->
      <div v-else-if="revealed && unusable" class="mt-4 p-4 rounded-md border-2" role="alert" aria-live="assertive"
        :class="!graded ? 'bg-secondary/10 border-secondary' : (result.isCorrect ? 'bg-quaternary/10 border-quaternary' : 'bg-accent/10 border-accent')">
        <div class="flex items-center gap-2 font-black text-sm uppercase mb-1">
          <span>{{ !graded ? 'Đang chấm…' : (result.isCorrect ? '✓ Đúng' : '✕ Sai') }}</span>
        </div>
        <p v-if="graded" class="font-bold text-sm">Đáp án: <span class="text-foreground font-black">{{ result.correctAnswer || 'Chưa có đáp án' }}</span></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { sanitizeText } from '@/utils/markdown'
import AppButton from '@/components/ui/AppButton.vue'

const props = defineProps({
  exercise: { type: Object, required: true },
  // audit-v5: kết quả chấm từ server (LessonExerciseTab gọi /grade rồi đẩy xuống)
  result: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['answer', 'reveal'])

const graded = computed(() => props.result?.graded === true)

// audit-v5: unusable matching data (empty or no "left|right" pairs) → text fallback
const unusable = computed(() => leftItems.value.length === 0)
const fallbackAnswer = ref('')

function emitFallback() {
  revealed.value = true
  emit('answer', fallbackAnswer.value)
  emit('reveal', fallbackAnswer.value)
}

// Parse MATCHING data
// options: ["word1|def1", "word2|def2", "word3|def3", "word4|def4"]
// After MCP processing: options = left items, _matchRight = right items (shuffled)
//
// audit-v10 F127: client KHONG giu `correctPairs` va KHONG doc `correctAnswer`.
// Server cham bang `options`; client chi can biet hai cot de hien thi, va chi
// gui len cap CHU nguoi hoc da noi.

const leftItems = ref([])
const rightItems = ref([])
const matchedPairs = ref([])
const pendingLeft = ref(null)
const pendingRight = ref(null)
const revealed = ref(false)

onMounted(() => {
  const ex = props.exercise

  // If MCP-processed format
  if (Array.isArray(ex._matchRight)) {
    leftItems.value = [...(ex.options || [])]
    rightItems.value = [...ex._matchRight]
  } else {
    // Parse options: could be array or JSON string
    let opts = ex.options
    if (typeof opts === 'string') {
      try { opts = JSON.parse(opts) } catch { opts = [] }
    }
    if (Array.isArray(opts) && opts.length > 0) {
      const pairs = []
      for (const opt of opts) {
        if (typeof opt === 'string' && opt.includes('|')) {
          const [left, right] = opt.split('|').map(s => s.trim())
          pairs.push({ left, right })
        }
      }
      if (pairs.length > 0) {
        leftItems.value = pairs.map(p => p.left)
        // Xao tron cot phai de nguoi hoc phai tu noi, khong doan theo thu tu.
        const rightTexts = pairs.map(p => p.right)
        const indices = [...Array(rightTexts.length).keys()]
        for (let i = indices.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1))
          ;[indices[i], indices[j]] = [indices[j], indices[i]]
        }
        rightItems.value = indices.map(i => rightTexts[i])
      }
    }
  }

  // audit-v10 F127: KHONG con parse `correctAnswer` o client.
  //
  // Hai ly do:
  //  1. No khong bao gio dung duoc. Doan cu lam `parseInt` tren tung ve cua moi
  //     cap va bo qua cap nao khong parse duoc. Do 331 row MATCHING published:
  //     330 row luu dap an dang CHU ("A=B,B=D" hoac "word1=be,..."), nen moi
  //     ve deu NaN, `correctPairs` rong, va code roi vao fallback "tuan tu" —
  //     tuc client TU DOAN dap an la 0=0,1=1,2=2. Doan sai.
  //  2. Dap an khong nen nam o client. Server cham bang `options` (xem
  //     ExerciseService.matchingPairsMatch), nen client khong can biet dap an —
  //     va khong duoc biet, vi do la ro ri dap an cho nguoi dang lam bai.
})

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}

function selectLeft(i) {
  if (revealed.value) return
  const alreadyMatched = matchedPairs.value.some(p => p.left === i)
  if (alreadyMatched) return
  pendingLeft.value = i
  tryMatch()
}

function selectRight(i) {
  if (revealed.value) return
  const alreadyMatched = matchedPairs.value.some(p => p.right === i)
  if (alreadyMatched) return
  pendingRight.value = i
  tryMatch()
}

function tryMatch() {
  if (pendingLeft.value === null || pendingRight.value === null) return
  matchedPairs.value.push({
    left: pendingLeft.value,
    right: pendingRight.value
  })
  pendingLeft.value = null
  pendingRight.value = null

  emit('answer', buildAnswer())
  // Nop khi da noi HET cap ben trai. Truoc day so voi `correctPairs.length`,
  // nhung do la do dai do client TU DOAN tu correctAnswer; gio lay truc tiep
  // so muc o cot trai — dung bang so cap can noi.
  if (matchedPairs.value.length === leftItems.value.length) {
    emit('reveal', buildAnswer())
  }
}

/**
 * audit-v10 F127 — gửi CHỮ, không gửi chỉ số.
 *
 * Trước đây hàm này gửi `${p.left}=${p.right}` với left/right là CHỈ SỐ VỊ TRÍ.
 * Không thể chấm được ở server, vì cột phải đã bị XÁO TRỘN ở onMounted: chỉ số
 * hiển thị không bằng chỉ số gốc, và server không có cách nào dựng lại phép
 * hoán vị đó. Đo trên 4 bài published: gửi chỉ số → correct=false cho cả 4, dù
 * người học nối đúng hết.
 *
 * Giờ gửi cặp CHỮ ("left=right"), khớp đúng định dạng server đọc từ `options`
 * (mỗi phần tử "left|right"). Server so khớp theo TẬP HỢP nên thứ tự nối
 * không ảnh hưởng kết quả.
 *
 * Lấy chữ trực tiếp từ mảng đang hiển thị: `leftItems[p.left]` và
 * `rightItems[p.right]` chính là hai nhãn người dùng nhìn thấy và bấm vào, nên
 * không cần dịch qua chỉ số gốc — chỉ số gốc không còn được dùng ở đâu.
 */
function buildAnswer() {
  return matchedPairs.value
    .map(p => `${leftItems.value[p.left]}=${rightItems.value[p.right]}`)
    .join(',')
}

function removePair(i) {
  matchedPairs.value.splice(i, 1)
}

function leftClass(i) {
  const isMatched = matchedPairs.value.some(p => p.left === i)
  const isPending = pendingLeft.value === i
  if (revealed.value) {
    // audit-v5: client không biết đáp án đúng (server chấm) — chỉ highlight đã nối
    const mp = matchedPairs.value.find(p => p.left === i)
    if (mp) return graded.value && props.result.isCorrect ? 'border-quaternary bg-quaternary/10' : 'border-accent bg-accent/10'
    return 'border-border opacity-50'
  }
  if (isMatched) return 'border-quaternary bg-quaternary/10'
  if (isPending) return 'border-accent bg-accent/20 ring-2 ring-accent'
  return 'border-foreground hover:bg-tertiary/10 hover:border-tertiary'
}

function rightClass(i) {
  const isMatched = matchedPairs.value.some(p => p.right === i)
  const isPending = pendingRight.value === i
  if (revealed.value) {
    const mp = matchedPairs.value.find(p => p.right === i)
    if (mp) return graded.value && props.result.isCorrect ? 'border-quaternary bg-quaternary/10' : 'border-accent bg-accent/10'
    return 'border-border opacity-50'
  }
  if (isMatched) return 'border-quaternary bg-quaternary/10'
  if (isPending) return 'border-accent bg-accent/20 ring-2 ring-accent'
  return 'border-foreground hover:bg-tertiary/10 hover:border-tertiary'
}
</script>

