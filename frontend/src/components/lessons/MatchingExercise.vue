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

      <!-- Matched pairs -->
      <div v-if="matchedPairs.length > 0" class="mt-4 space-y-2">
        <p class="font-bold text-xs uppercase tracking-wider text-muted-foreground mb-2">Đã nối:</p>
        <div v-for="(pair, i) in matchedPairs" :key="'m-' + i"
          class="flex items-center gap-3 p-2.5 bg-quaternary/10 border-2 border-quaternary rounded-md">
          <span class="font-black text-xs text-quaternary uppercase">{{ i + 1 }}</span>
          <span class="font-bold text-sm truncate">{{ leftItems[pair.left] }}</span>
          <span class="text-muted-foreground" aria-hidden="true">→</span>
          <span class="font-medium text-sm truncate">{{ rightItems[pair.right] }}</span>
          <button @click="removePair(i)" class="ml-auto w-6 h-6 flex items-center justify-center border-2 border-foreground rounded-md text-muted-foreground hover:bg-accent hover:text-white transition-all font-black text-xs">&times;</button>
        </div>
      </div>

      <!-- Result badge -->
      <div v-if="revealed" class="mt-4 p-4 rounded-md border-2" role="alert" aria-live="assertive"
        :class="isCorrect ? 'bg-quaternary/10 border-quaternary' : 'bg-accent/10 border-accent'">
        <div class="flex items-center gap-2 font-black text-sm uppercase mb-1">
          <span>{{ isCorrect ? '✓ Đúng' : '✕ Sai' }}</span>
        </div>
        <p class="font-bold text-sm">Đáp án đúng:</p>
        <div v-for="(pair, i) in correctPairs" :key="'c-' + i" class="flex gap-2 text-sm mt-1">
          <span class="font-bold">{{ leftItems[pair.left] }}</span>
          <span class="text-muted-foreground" aria-hidden="true">→</span>
          <span class="font-medium">{{ rightItems[pair.right] }}</span>
        </div>
        <p v-if="exercise.explanation" class="mt-2 text-sm text-muted-foreground italic">{{ exercise.explanation }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  exercise: { type: Object, required: true }
})

const emit = defineEmits(['answer', 'reveal'])

// Parse MATCHING data
// options: ["word1|def1", "word2|def2", "word3|def3", "word4|def4"]
// correctAnswer: "0=0,1=1,2=2,3=3" (leftIndex=rightIndex)
// After MCP processing: options = left items, _matchRight = right items (shuffled)

const leftItems = ref([])
const rightItems = ref([])
const correctPairs = ref([])
const matchedPairs = ref([])
const pendingLeft = ref(null)
const pendingRight = ref(null)
const rightIndexMap = ref([]) // maps displayed-right-index to original-right-text-index
const revealed = ref(false)

const userPairs = computed(() => {
  const map = {}
  for (const p of matchedPairs.value) {
    map[p.left] = p.right
  }
  return map
})

const isCorrect = computed(() => {
  if (matchedPairs.value.length !== correctPairs.value.length) return false
  for (const mp of matchedPairs.value) {
    const leftVal = leftItems.value[mp.left]
    const rightVal = rightItems.value[mp.right]
    const expected = correctPairs.value.find(cp => cp.left === leftVal)
    if (!expected || expected.right !== rightVal) return false
  }
  return true
})

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
        // Build rightItems shuffled + pairMap for VALUE-based matching
        const rightTexts = pairs.map(p => p.right)
        const indices = [...Array(rightTexts.length).keys()]
        for (let i = indices.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1))
          ;[indices[i], indices[j]] = [indices[j], indices[i]]
        }
        rightItems.value = indices.map(i => rightTexts[i])
        // Store which original right-index each displayed-right maps to
        rightIndexMap.value = indices
        // Build VALUE-based correct pairs: { leftValue: leftIndex, rightValue: rightIndex }
        for (let i = 0; i < pairs.length; i++) {
          correctPairs.value.push({ left: pairs[i].left, right: pairs[i].right })
        }
      }
    }
  }

  // Parse correctAnswer
  if (ex.correctAnswer) {
    const str = String(ex.correctAnswer)
    const parts = str.split(',')
    for (const part of parts) {
      const [l, r] = part.split('=').map(s => parseInt(s.trim()))
      if (!isNaN(l) && !isNaN(r)) {
        correctPairs.value.push({ left: l, right: r })
      }
    }
  }

  // Fallback: sequential
  if (correctPairs.value.length === 0 && leftItems.value.length > 0) {
    for (let i = 0; i < leftItems.value.length; i++) {
      correctPairs.value.push({ left: i, right: i })
    }
  }
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

  // Build answer string
  const pairs = matchedPairs.value.map(p => `${p.left}=${p.right}`).join(',')
  emit('answer', pairs)

  if (matchedPairs.value.length === correctPairs.value.length) {
    emit('reveal', pairs)
  }
}

function removePair(i) {
  matchedPairs.value.splice(i, 1)
}

function leftClass(i) {
  const isMatched = matchedPairs.value.some(p => p.left === i)
  const isPending = pendingLeft.value === i
  if (revealed.value) {
    const cp = correctPairs.value.find(p => p.left === i)
    const mp = matchedPairs.value.find(p => p.left === i)
    const correct = cp && mp && cp.right === mp.right
    if (correct) return 'border-quaternary bg-quaternary/10'
    if (mp) return 'border-accent bg-accent/10'
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
    const cp = correctPairs.value.find(p => p.right === i)
    const mp = matchedPairs.value.find(p => p.right === i)
    const correct = cp && mp && cp.right === mp.right
    if (correct) return 'border-quaternary bg-quaternary/10'
    if (mp) return 'border-accent bg-accent/10'
    return 'border-border opacity-50'
  }
  if (isMatched) return 'border-quaternary bg-quaternary/10'
  if (isPending) return 'border-accent bg-accent/20 ring-2 ring-accent'
  return 'border-foreground hover:bg-tertiary/10 hover:border-tertiary'
}
</script>

