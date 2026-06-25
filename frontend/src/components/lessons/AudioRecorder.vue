<template>
  <div class="audio-recorder">
    <div v-if="!supported" class="p-4 bg-red-100 border-2 border-red-400 rounded-lg text-red-700 font-bold">
      Audio recording not supported in your browser.
    </div>

    <div v-else class="space-y-4">
      <div v-if="state === 'idle'">
        <button @click="startRecording" class="w-full py-3 bg-red-500 text-white font-bold uppercase border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5 transition-all">
          Start Recording
        </button>
      </div>

      <div v-if="state === 'recording'" class="text-center space-y-3">
        <div class="flex items-center justify-center gap-3">
          <span class="w-4 h-4 bg-red-500 rounded-full animate-pulse"></span>
          <span class="font-bold text-lg uppercase text-red-600">Recording...</span>
        </div>
        <div class="font-mono text-2xl font-black">{{ formatTime(elapsed) }}</div>
        <button @click="stopRecording" class="w-full py-3 bg-black text-white font-bold uppercase border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5 transition-all">
          Stop Recording
        </button>
      </div>

      <div v-if="state === 'stopped' && audioUrl" class="space-y-3">
        <audio :src="audioUrl" controls class="w-full"></audio>
        <div class="flex gap-3">
          <button @click="uploadRecording" class="flex-1 py-3 bg-green-500 text-white font-bold uppercase border-4 border-black rounded-xl shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5 transition-all" :disabled="uploading">
            {{ uploading ? 'Uploading...' : 'Submit Recording' }}
          </button>
          <button @click="resetRecording" class="flex-1 py-3 bg-gray-200 text-black font-bold uppercase border-4 border-black rounded-xl hover:bg-gray-300 transition-all">
            Re-record
          </button>
        </div>
      </div>

      <div v-if="uploaded" class="p-4 bg-green-100 border-2 border-green-400 rounded-lg text-green-700 font-bold text-center">
        Recording submitted successfully!
      </div>

      <div v-if="error" class="p-4 bg-red-100 border-2 border-red-400 rounded-lg text-red-700 font-bold">
        {{ error }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { submissionService } from '@/services/submissionService'

const emit = defineEmits(['submitted'])

const supported = ref(typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia)
const state = ref('idle')
const mediaRecorder = ref(null)
const audioChunks = ref([])
const audioUrl = ref(null)
const elapsed = ref(0)
const uploading = ref(false)
const uploaded = ref(false)
const error = ref(null)
let timer = null
let stream = null

function formatTime(sec) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

async function startRecording() {
  try {
    error.value = null
    audioChunks.value = []
    audioUrl.value = null
    stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder.value = new MediaRecorder(stream)
    mediaRecorder.value.ondataavailable = (e) => audioChunks.value.push(e.data)
    mediaRecorder.value.onstop = () => {
      const blob = new Blob(audioChunks.value, { type: 'audio/webm' })
      audioUrl.value = URL.createObjectURL(blob)
    }
    mediaRecorder.value.start()
    state.value = 'recording'
    elapsed.value = 0
    timer = setInterval(() => elapsed.value++, 1000)
  } catch (e) {
    error.value = 'Microphone access denied.'
  }
}

function stopRecording() {
  if (mediaRecorder.value && mediaRecorder.value.state !== 'inactive') {
    mediaRecorder.value.stop()
  }
  if (stream) stream.getTracks().forEach(t => t.stop())
  clearInterval(timer)
  state.value = 'stopped'
}

async function uploadRecording() {
  if (!audioUrl.value) return
  uploading.value = true
  error.value = null
  try {
    const blob = new Blob(audioChunks.value, { type: 'audio/webm' })
    const file = new File([blob], 'recording.webm', { type: 'audio/webm' })
    await submissionService.uploadAudio(file)
    uploaded.value = true
    emit('submitted')
  } catch (e) {
    error.value = 'Upload failed. Please try again.'
  } finally {
    uploading.value = false
  }
}

function resetRecording() {
  audioUrl.value = null
  audioChunks.value = []
  state.value = 'idle'
  uploaded.value = false
  error.value = null
  clearInterval(timer)
}

onUnmounted(() => {
  clearInterval(timer)
  if (stream) stream.getTracks().forEach(t => t.stop())
})
</script>
