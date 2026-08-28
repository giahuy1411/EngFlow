<template>
  <div class="my-4 border-2 border-foreground rounded-md bg-card p-5 shadow-pop-sm">
    <p class="font-black text-base mb-3">{{ data.questionText }}</p>
    <img v-if="data.imageUrl" :src="data.imageUrl" class="max-w-full rounded-md border-2 border-foreground mb-3" alt="" />

    <div v-if="data.questionType === 'MULTIPLE_CHOICE'" class="space-y-2">
      <label v-for="(opt, i) in parsedOptions" :key="i"
        class="flex items-center gap-3 p-3 border-2 rounded-md cursor-pointer transition-all"
        :class="selectedAnswer === opt ? 'bg-accent/10 border-accent' : 'border-foreground hover:bg-tertiary/10'">
        <input type="radio" :name="'q-' + blockId" :value="opt" v-model="selectedAnswer" class="geo-radio" />
        <span class="font-medium">{{ opt }}</span>
      </label>
    </div>

    <input v-else-if="data.questionType === 'FILL_IN_BLANK'" v-model="fillAnswer" type="text"
      class="w-full border-2 border-foreground rounded-md px-3 py-2 font-bold focus:outline-none focus:ring-2 focus:ring-accent"
      placeholder="Nhập câu trả lời..." />

    <div v-else-if="data.questionType === 'TRUE_FALSE'" class="flex flex-wrap gap-3 mt-2">
      <button type="button" @click="tfAnswer = 'true'"
        class="px-6 py-2 border-2 border-foreground rounded-full font-black uppercase"
        :class="tfAnswer === 'true' ? 'bg-quaternary text-white' : 'bg-card hover:bg-tertiary/10'">True</button>
      <button type="button" @click="tfAnswer = 'false'"
        class="px-6 py-2 border-2 border-foreground rounded-full font-black uppercase"
        :class="tfAnswer === 'false' ? 'bg-secondary text-white' : 'bg-card hover:bg-tertiary/10'">False</button>
    </div>

    <div v-else-if="data.questionType === 'MATCHING'" class="space-y-4">
      <div v-if="matchingPairs.length" class="grid gap-4 md:grid-cols-2">
        <div class="space-y-2">
          <button v-for="(item, i) in leftItems" :key="'left-' + i" type="button"
            class="w-full p-3 border-2 rounded-md font-bold transition-all"
            :class="leftClass(i)" :disabled="matchedLeft.has(i) || result !== null" @click="selectLeft(i)">
            {{ item.text }}
          </button>
        </div>
        <div class="space-y-2">
          <button v-for="(item, i) in rightItems" :key="'right-' + i" type="button"
            class="w-full p-3 border-2 rounded-md font-medium transition-all"
            :class="rightClass(i)" :disabled="matchedRight.has(i) || result !== null" @click="selectRight(i)">
            {{ item.text }}
          </button>
        </div>
      </div>
      <p v-else class="text-muted-foreground italic">Chưa có dữ liệu nối từ.</p>
      <div v-if="matchedPairs.length" class="space-y-2">
        <p class="font-bold text-xs uppercase text-muted-foreground">Đã nối</p>
        <div v-for="(pair, i) in matchedPairs" :key="i" class="flex items-center gap-2 border-2 border-foreground rounded-md bg-quaternary/10 p-2 text-sm">
          <span class="font-bold">{{ leftItems[pair.left].text }}</span>
          <span aria-hidden="true">→</span>
          <span>{{ rightItems[pair.right].text }}</span>
          <button type="button" class="ml-auto font-black" :disabled="result !== null" @click="removePair(i)">&times;</button>
        </div>
      </div>
    </div>

    <button v-if="showCheck" type="button" @click="checkAnswer"
      class="mt-4 px-5 py-2 bg-accent text-white border-2 border-foreground rounded-full font-black uppercase text-sm shadow-pop-sm">
      Kiểm tra
    </button>

    <div v-if="result !== null" class="mt-4 p-3 border-2 rounded-md font-bold" :class="result ? 'border-quaternary bg-quaternary/10 text-quaternary' : 'border-accent bg-accent/10 text-accent'" role="status">
      {{ result ? 'Chính xác!' : 'Sai rồi!' }}
      <span v-if="data.correctAnswer && !result && data.questionType !== 'MATCHING'" class="block text-sm text-foreground mt-1">Đáp án: {{ data.correctAnswer }}</span>
      <span v-if="data.explanation" class="block text-muted-foreground text-sm font-normal mt-1">{{ data.explanation }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  data: { type: Object, required: true },
  blockId: { type: Number, default: 0 }
})

const selectedAnswer = ref(null)
const fillAnswer = ref('')
const tfAnswer = ref(null)
const result = ref(null)
const pendingLeft = ref(null)
const pendingRight = ref(null)
const matchedPairs = ref([])

const parsedOptions = computed(() => parseArray(props.data.options))
const matchingPairs = computed(() => parsedOptions.value
  .map(option => {
    if (typeof option === 'string' && option.includes('|')) {
      const [left, right] = option.split('|').map(item => item.trim())
      return { left, right }
    }
    if (option?.left && option?.right) return option
    return null
  })
  .filter(Boolean)
)
const leftItems = computed(() => matchingPairs.value.map((pair, index) => ({ text: pair.left, pairId: index })))
const rightItems = computed(() => matchingPairs.value.map((pair, index) => ({ text: pair.right, pairId: index })).sort((a, b) => a.text.localeCompare(b.text)))
const matchedLeft = computed(() => new Set(matchedPairs.value.map(pair => pair.left)))
const matchedRight = computed(() => new Set(matchedPairs.value.map(pair => pair.right)))

const showCheck = computed(() => {
  if (props.data.questionType === 'MULTIPLE_CHOICE') return selectedAnswer.value !== null
  if (props.data.questionType === 'FILL_IN_BLANK') return fillAnswer.value.trim() !== ''
  if (props.data.questionType === 'TRUE_FALSE') return tfAnswer.value !== null
  if (props.data.questionType === 'MATCHING') return matchingPairs.value.length > 0 && matchedPairs.value.length === matchingPairs.value.length
  return false
})

function parseArray(raw) {
  if (Array.isArray(raw)) return raw
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : raw.split('\n').filter(Boolean)
    } catch {
      return raw.split('\n').filter(Boolean)
    }
  }
  return []
}

function normalize(value) {
  return String(value || '').trim().toLowerCase().replace(/\s+/g, ' ')
}

function selectLeft(index) {
  pendingLeft.value = index
  connectPending()
}

function selectRight(index) {
  pendingRight.value = index
  connectPending()
}

function connectPending() {
  if (pendingLeft.value === null || pendingRight.value === null) return
  matchedPairs.value.push({ left: pendingLeft.value, right: pendingRight.value })
  pendingLeft.value = null
  pendingRight.value = null
}

function removePair(index) {
  matchedPairs.value.splice(index, 1)
}

function leftClass(index) {
  if (matchedLeft.value.has(index)) return 'border-quaternary bg-quaternary/10'
  if (pendingLeft.value === index) return 'border-accent bg-accent/10'
  return 'border-foreground hover:bg-tertiary/10'
}

function rightClass(index) {
  if (matchedRight.value.has(index)) return 'border-quaternary bg-quaternary/10'
  if (pendingRight.value === index) return 'border-accent bg-accent/10'
  return 'border-foreground hover:bg-tertiary/10'
}

function checkAnswer() {
  if (props.data.questionType === 'MULTIPLE_CHOICE') {
    result.value = normalize(selectedAnswer.value) === normalize(props.data.correctAnswer)
    return
  }
  if (props.data.questionType === 'FILL_IN_BLANK') {
    result.value = normalize(fillAnswer.value) === normalize(props.data.correctAnswer)
    return
  }
  if (props.data.questionType === 'TRUE_FALSE') {
    result.value = normalize(tfAnswer.value) === normalize(props.data.correctAnswer)
    return
  }
  result.value = matchedPairs.value.every(pair => leftItems.value[pair.left]?.pairId === rightItems.value[pair.right]?.pairId)
}
</script>
