<template>
  <div class="reading-questions space-y-6">
    <div
      v-for="(q, qi) in questions"
      :key="qi"
      class="bg-white border-2 border-black rounded-lg p-4"
    >
      <div class="font-bold mb-3">Question {{ qi + 1 }}</div>

      <div v-if="q.options.length" class="space-y-2">
        <button
          v-for="(opt, oi) in q.options"
          :key="oi"
          @click="selectOption(qi, oi)"
          class="w-full flex items-center gap-3 px-4 py-3 text-left border-2 border-black rounded-lg font-medium transition-all"
          :class="getOptionClass(qi, oi)"
          :disabled="submitted"
        >
          <span class="w-6 h-6 flex items-center justify-center border-2 border-black rounded-full font-bold text-xs">{{ optionLabels[oi] }}</span>
          <span>{{ opt }}</span>
        </button>
      </div>

      <div v-if="q.textInputs.length" class="space-y-3">
        <div v-for="(ti, tii) in q.textInputs" :key="tii" class="space-y-1">
          <input
            type="text"
            v-model="textAnswers[qi]"
            class="w-full px-4 py-2 border-2 border-black rounded-lg font-medium focus:outline-none focus:border-yellow-400"
            placeholder="Type your answer..."
            :disabled="submitted"
          />
        </div>
      </div>
    </div>

    <button
      v-if="!submitted"
      @click="submitAnswers"
      class="w-full py-3 bg-yellow-400 text-black font-black uppercase border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5 transition-all"
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
const results = ref({})

function selectOption(qi, oi) {
  if (submitted.value) return
  selected.value[qi] = oi
}

function getOptionClass(qi, oi) {
  if (!submitted.value) {
    return selected.value[qi] === oi
      ? 'bg-yellow-200 border-yellow-500'
      : 'bg-white hover:bg-yellow-50'
  }
  const expected = props.answerKey[qi]
  if (expected != null && expected.toUpperCase() === optionLabels[oi]) return 'bg-green-400 border-green-600 text-white'
  if (selected.value[qi] === oi) return 'bg-red-400 border-red-600 text-white'
  return 'bg-gray-100 opacity-40'
}

function submitAnswers() {
  submitted.value = true
}
</script>
