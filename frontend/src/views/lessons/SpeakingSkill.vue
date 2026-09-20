<template>
  <div>
    <div class="border-2 border-foreground rounded-md p-6 shadow-pop-lg mb-6">
      <div class="geo-markdown text-lg font-bold mb-4" v-html="parseMarkdown(prompt)"></div>
    </div>
    <div>
      <label class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Ghi âm</label>
      <div class="flex items-center gap-4">
        <button @click="toggleRecording"
          :aria-label="isRecording ? 'Dừng ghi âm' : 'Bắt đầu ghi âm'"
          class="w-14 h-14 rounded-full border-2 border-foreground flex items-center justify-center transition-all"
          :class="isRecording ? 'bg-secondary-strong text-white shadow-pop' : 'bg-card hover:bg-tertiary/20'"
        >
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path v-if="!isRecording" stroke-linecap="round" stroke-linejoin="round" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"/>
            <rect v-else x="6" y="6" width="12" height="12" rx="2"/>
          </svg>
        </button>
        <span class="font-bold text-sm text-muted-foreground">{{ isRecording ? 'Đang ghi...' : 'Nhấn để ghi âm' }}</span>
      </div>
      <audio v-if="recordedUrl" :src="recordedUrl" controls class="w-full mt-4 geo-audio"></audio>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  prompt: { type: String, default: '' },
})

const isRecording = ref(false)
const recordedUrl = ref('')

function toggleRecording() {
  isRecording.value = !isRecording.value
  if (!isRecording.value) {
    // Stop recording logic
    recordedUrl.value = recordedUrl.value || ''
  }
}

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}
</script>
