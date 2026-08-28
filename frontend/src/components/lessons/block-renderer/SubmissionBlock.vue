<template>
  <div class="my-4 border-2 border-foreground rounded-md bg-card p-5 shadow-pop-sm">
    <p class="font-black mb-3">{{ data.prompt }}</p>

    <textarea v-if="data.submissionType === 'TEXT'" v-model="text" :disabled="submitted"
      class="w-full border-2 border-foreground rounded-md px-3 py-2 min-h-[120px] focus:outline-none focus:ring-2 focus:ring-accent"
      aria-label="Câu trả lời của bạn" placeholder="Nhập câu trả lời..."></textarea>

    <div v-else-if="data.submissionType === 'AUDIO'" class="space-y-3">
      <div v-if="!audioUrl" class="border-2 border-dashed border-border bg-muted/60 p-6 text-center rounded-md">
        <p class="font-bold text-sm text-muted-foreground">{{ isRecording ? `Đang ghi ${elapsed}s` : 'Ghi âm câu trả lời của bạn' }}</p>
        <button v-if="!isRecording" type="button" class="mt-3 px-5 py-2 bg-accent text-white border-2 border-foreground rounded-full font-black uppercase text-sm" @click="startRecording">
          Bắt đầu ghi
        </button>
        <button v-else type="button" class="mt-3 px-5 py-2 bg-danger text-white border-2 border-foreground rounded-full font-black uppercase text-sm" @click="stopRecording">
          Dừng ghi
        </button>
      </div>
      <div v-else class="border-2 border-foreground bg-muted/60 rounded-md p-4">
        <audio :src="audioUrl" controls class="w-full"></audio>
        <button type="button" class="mt-3 px-4 py-2 border-2 border-foreground rounded-full font-black uppercase text-xs" :disabled="submitted" @click="resetRecording">Ghi lại</button>
      </div>
      <p v-if="error" class="text-danger font-bold text-sm" role="alert">{{ error }}</p>
    </div>

    <button type="button" class="mt-4 px-5 py-2 bg-accent text-white border-2 border-foreground rounded-full font-black uppercase text-sm shadow-pop-sm"
      :disabled="submitted || !canSubmit" @click="submit">
      {{ submitted ? 'Đã nộp' : 'Nộp bài' }}
    </button>
    <p v-if="submitted" class="text-quaternary font-bold mt-2" role="status">Đã lưu bài nộp trên trình duyệt.</p>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const emit = defineEmits(['submitted'])

const text = ref('')
const submitted = ref(false)
const isRecording = ref(false)
const elapsed = ref(0)
const audioUrl = ref('')
const chunks = ref([])
const error = ref('')
let recorder = null
let stream = null
let timer = null

const canSubmit = computed(() => props.data.submissionType === 'AUDIO' ? Boolean(audioUrl.value) : Boolean(text.value.trim()))

async function startRecording() {
  error.value = ''
  if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
    error.value = 'Trình duyệt không hỗ trợ ghi âm.'
    return
  }
  try {
    resetRecording()
    stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    recorder = new MediaRecorder(stream)
    recorder.ondataavailable = event => { if (event.data.size) chunks.value.push(event.data) }
    recorder.onstop = finalizeRecording
    recorder.start(250)
    isRecording.value = true
    timer = window.setInterval(() => { elapsed.value += 1 }, 1000)
  } catch (cause) {
    error.value = cause.name === 'NotAllowedError' ? 'Quyền micro bị từ chối.' : 'Không thể mở micro.'
  }
}

function stopRecording() {
  if (recorder?.state && recorder.state !== 'inactive') recorder.stop()
  isRecording.value = false
  window.clearInterval(timer)
}

function finalizeRecording() {
  const blob = new Blob(chunks.value, { type: recorder?.mimeType || 'audio/webm' })
  if (blob.size) audioUrl.value = URL.createObjectURL(blob)
  stream?.getTracks().forEach(track => track.stop())
  stream = null
}

function resetRecording() {
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
  audioUrl.value = ''
  chunks.value = []
  elapsed.value = 0
  error.value = ''
}

function submit() {
  submitted.value = true
  emit('submitted', props.data.submissionType === 'AUDIO' ? { audioUrl: audioUrl.value } : { text: text.value })
}

onUnmounted(() => {
  window.clearInterval(timer)
  stream?.getTracks().forEach(track => track.stop())
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
})
</script>
