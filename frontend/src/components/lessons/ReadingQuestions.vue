<template>
  <div class="reading-questions space-y-6">
    <div
      v-for="(q, qi) in questions"
      :key="qi"
      class="bg-white border-2 border-foreground rounded-xl p-5 shadow-pop"
    >
      <div class="font-black text-sm uppercase tracking-wider mb-4 flex items-center gap-2">
        <span class="w-6 h-6 bg-tertiary border-2 border-foreground rounded flex items-center justify-center font-black text-xs">{{ qi + 1 }}</span>
        <span>Question</span>
      </div>

      <div v-if="q.options.length" class="space-y-2">
        <button
          v-for="(opt, oi) in q.options"
          :key="oi"
          @click="selectOption(qi, oi)"
          class="w-full flex items-center gap-3 px-4 py-3 text-left border-2 border-foreground rounded-lg font-medium transition-all duration-150"
          :class="getOptionClass(qi, oi)"
          :disabled="submitted"
        >
          <span class="w-7 h-7 flex-shrink-0 flex items-center justify-center border-2 border-foreground rounded-full font-bold text-xs" :class="getLabelClass(qi, oi)">{{ optionLabels[oi] }}</span>
          <span class="flex-1">{{ opt }}</span>
        </button>
      </div>

      <div v-if="q.textInputs.length" class="space-y-3 mt-2">
        <div v-for="(ti, tii) in q.textInputs" :key="tii" class="space-y-1">
          <label :for="`reading-answer-${qi}-${tii}`" class="font-bold text-xs uppercase text-muted-foreground">Answer</label>
          <input
            :id="`reading-answer-${qi}-${tii}`"
            type="text"
            v-model="textAnswers[qi]"
            class="w-full px-4 py-2.5 border-2 border-foreground rounded-lg font-medium focus:outline-none focus:border-yellow-400 focus:shadow-pop-sm transition-all"
            placeholder="Type your answer..."
            :disabled="submitted"
          />
        </div>
      </div>
    </div>

    <button
      v-if="!submitted"
      @click="submitAnswers"
      class="w-full py-3.5 bg-tertiary text-foreground font-black uppercase text-lg border-4 border-foreground rounded-xl shadow-pop hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all"
    >
      Submit Answers
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  questions: { type: Array, required: true },
  answerKey: { type: Object, default: () => ({}) },
})

const optionLabels = ['A', 'B', 'C', 'D', 'E', 'F']
const selected = ref({})
const textAnswers = ref({})
const submitted = ref(false)

function selectOption(qi, oi) {
  if (submitted.value) return
  selected.value[qi] = oi
}

function getOptionClass(qi, oi) {
  if (!submitted.value) {
    return selected.value[qi] === oi
      ? 'bg-tertiary/20 border-tertiary shadow-pop-sm'
      : 'bg-white hover:bg-tertiary/10 hover:-translate-y-0.5 hover:shadow-pop-sm'
  }
  const expected = props.answerKey[qi]
  if (expected != null && expected.toUpperCase() === optionLabels[oi]) return 'bg-success/10 border-green-500 text-green-800'
  if (selected.value[qi] === oi) return 'bg-danger/10 border-danger text-danger'
  return 'bg-muted/60 border-border text-muted-foreground'
}

function getLabelClass(qi, oi) {
  if (!submitted.value) {
    return selected.value[qi] === oi ? 'bg-tertiary border-tertiary' : 'bg-white'
  }
  const expected = props.answerKey[qi]
  if (expected != null && expected.toUpperCase() === optionLabels[oi]) return 'bg-success border-green-600 text-white'
  if (selected.value[qi] === oi) return 'bg-danger border-danger text-white'
  return 'bg-muted border-border text-muted-foreground'
}

function submitAnswers() {
  submitted.value = true
}
</script>





