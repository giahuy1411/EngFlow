<template>
  <div class="my-4 p-4 border rounded-lg">
    <p class="font-medium mb-3">{{ data.questionText }}</p>
    <img v-if="data.imageUrl" :src="data.imageUrl" class="max-w-full rounded mb-3" />
    <div v-if="data.questionType === 'MULTIPLE_CHOICE'" class="space-y-2">
      <label v-for="(opt, i) in parsedOptions" :key="i"
        class="flex items-center gap-2 p-2 rounded cursor-pointer"
        :class="selectedAnswer === opt ? 'bg-blue-100 border border-blue-300' : 'bg-gray-50 hover:bg-gray-100'">
        <input type="radio" :name="'q-' + blockId" :value="opt" @change="selectedAnswer = opt" />
        {{ opt }}
      </label>
    </div>
    <div v-else-if="data.questionType === 'FILL_IN_BLANK'" class="mt-2">
      <input v-model="fillAnswer" type="text" class="border rounded px-3 py-2 w-full" placeholder="Nhập câu trả lời..." />
    </div>
    <div v-else-if="data.questionType === 'TRUE_FALSE'" class="flex gap-4 mt-2">
      <button @click="tfAnswer = 'True'"
        class="px-6 py-2 rounded" :class="tfAnswer === 'True' ? 'bg-green-500 text-white' : 'bg-gray-100'">True</button>
      <button @click="tfAnswer = 'False'"
        class="px-6 py-2 rounded" :class="tfAnswer === 'False' ? 'bg-red-500 text-white' : 'bg-gray-100'">False</button>
    </div>
    <div v-else-if="data.questionType === 'MATCHING'" class="mt-2">
      <p class="text-gray-500 italic">Tính năng matching đang phát triển...</p>
    </div>
    <button v-if="showCheck" @click="checkAnswer"
      class="mt-3 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
      Kiểm tra
    </button>
    <div v-if="result !== null" class="mt-2 font-medium" :class="result ? 'text-green-600' : 'text-red-600'">
      {{ result ? 'Chính xác!' : 'Sai rồi!' }}
      <span v-if="data.explanation" class="block text-gray-600 text-sm font-normal mt-1">{{ data.explanation }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  data: { type: Object, required: true },
  blockId: { type: Number }
});

const selectedAnswer = ref(null);
const fillAnswer = ref('');
const tfAnswer = ref(null);
const result = ref(null);

const parsedOptions = computed(() => {
  if (typeof props.data.options === 'string') {
    try { return JSON.parse(props.data.options); } catch { return []; }
  }
  return props.data.options || [];
});

const showCheck = computed(() => {
  if (props.data.questionType === 'MULTIPLE_CHOICE') return selectedAnswer.value !== null;
  if (props.data.questionType === 'FILL_IN_BLANK') return fillAnswer.value.trim() !== '';
  if (props.data.questionType === 'TRUE_FALSE') return tfAnswer.value !== null;
  return false;
});

function checkAnswer() {
  let correct = false;
  const ca = (props.data.correctAnswer || '').trim().toLowerCase();
  if (props.data.questionType === 'MULTIPLE_CHOICE') {
    correct = (selectedAnswer.value || '').trim().toLowerCase() === ca;
  } else if (props.data.questionType === 'FILL_IN_BLANK') {
    correct = fillAnswer.value.trim().toLowerCase() === ca;
  } else if (props.data.questionType === 'TRUE_FALSE') {
    correct = (tfAnswer.value || '').toLowerCase() === ca;
  }
  result.value = correct;
}
</script>
