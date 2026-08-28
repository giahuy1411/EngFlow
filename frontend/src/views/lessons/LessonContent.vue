<template>
  <div class="space-y-6">
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-16">
      <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="bg-card border-2 border-foreground shadow-pop-lg p-8 rounded-md text-center">
      <p class="font-black text-lg uppercase text-accent">Không thể tải nội dung</p>
      <p class="text-muted-foreground mt-2">{{ error }}</p>
    </div>

    <!-- Content -->
    <div v-else-if="cleanHtml" id="lesson-print-area">
      <!-- Toolbar -->
      <div class="flex flex-wrap gap-3 mb-6 no-print">
        <button @click="printContent"
          class="flex items-center gap-2 px-5 py-3 bg-secondary text-white font-black text-sm tracking-wider border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover active:shadow-pop-active transition-all">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z"/>
          </svg>
          In tài liệu
        </button>
      </div>

      <!-- Rendered clean HTML -->
      <div class="lesson-content lesson-html" v-html="sanitizeHtml(cleanHtml)"></div>

      <section v-if="speakingPrompts.length" class="no-print mt-8 border-2 border-foreground bg-card p-6 shadow-pop" aria-labelledby="lesson-speaking-title">
        <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Speaking Coach</p>
        <h2 id="lesson-speaking-title" class="mt-1 text-2xl font-black">Luyện nói cho bài học này</h2>
        <div class="mt-4 grid gap-3 md:grid-cols-2">
          <router-link v-for="item in speakingPrompts" :key="item.id" :id="`lesson-speaking-${item.id}`"
            :to="`/speaking/${item.id}`"
            class="border-2 border-foreground bg-background p-4 transition-all hover:-translate-y-0.5 hover:shadow-pop">
            <span class="text-xs font-black uppercase text-accent">{{ item.mode === 'READ_ALOUD' ? 'Đọc theo mẫu' : 'Nói tự do' }}</span>
            <strong class="mt-1 block text-lg">{{ item.title }}</strong>
            <span class="mt-2 block text-sm text-muted-foreground">Mở bài luyện &rarr;</span>
          </router-link>
        </div>
      </section>
    </div>

    <!-- Empty -->
    <div v-else class="bg-card border-2 border-foreground shadow-pop-lg p-12 text-center rounded-md">
      <p class="font-black text-xl uppercase">Bài học này chưa có nội dung.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import DOMPurify from 'dompurify'
import lessonService from '@/services/lessonService'
import speakingService from '@/services/speakingService'

const route = useRoute()
const lessonId = Number(route.params.id)
const cleanHtml = ref('')
const speakingPrompts = ref([])
const loading = ref(true)
const error = ref(null)

/**
 * Pre-process and sanitize lesson HTML content.
 * Strips escaped HTML ad text patterns before DOMPurify runs,
 * then sanitizes with a strict tag whitelist.
 */
function sanitizeHtml(html) {
  if (!html) return ''

  // Pre-process: strip escaped HTML ad text that appears as visible text
  let cleaned = html
    .replace(/&lt;iframe[^&]*?&gt;/gi, '')
    .replace(/&lt;\/iframe&gt;/gi, '')
    .replace(/&lt;ins[^&]*?&gt;/gi, '')
    .replace(/&lt;\/ins&gt;/gi, '')
    .replace(/&lt;script[^&]*?&gt;[\s\S]*?&lt;\/script&gt;/gi, '')
    .replace(/google_ads_frame\w*/gi, '')
    .replace(/adsbygoogle/gi, '')
    .replace(/pagead2\.googlesyndication[^\s<]*/gi, '')
    .replace(/ca-pub-\d+/gi, '')

  return DOMPurify.sanitize(cleaned, {
    ADD_TAGS: ['details', 'summary', 'audio', 'source'],
    ADD_ATTR: ['controls', 'src', 'open', 'preload'],
    FORBID_TAGS: ['iframe', 'ins', 'script', 'style', 'noscript'],
    FORBID_CONTENTS: ['iframe', 'script', 'style', 'noscript']
  })
}

/**
 * Print lesson content with all Answer sections auto-expanded.
 * Restores collapsed state after the print dialog closes.
 */
function printContent() {
  const details = document.querySelectorAll('#lesson-print-area details')
  const wasOpen = []
  details.forEach((d, i) => {
    wasOpen[i] = d.open
    d.open = true
  })

  window.print()

  // Restore original collapsed/expanded state after print dialog
  setTimeout(() => {
    details.forEach((d, i) => {
      d.open = wasOpen[i]
    })
  }, 500)
}

onMounted(async () => {
  try {
    const data = await lessonService.getCleanContent(lessonId)
    cleanHtml.value = data?.cleanHtml || ''
  } catch (cause) {
    console.error('Failed to load lesson content:', cause)
    error.value = cause.response?.data?.detail || cause.message || 'Lỗi kết nối máy chủ'
    loading.value = false
    return
  }

  try {
    const promptPage = await speakingService.getAll(0, 100)
    speakingPrompts.value = (promptPage.content || [])
      .filter(prompt => Number(prompt.lessonId) === lessonId)
  } catch (cause) {
    console.error('Failed to load speaking prompts:', cause)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* Print styles */
@media print {
  .no-print { display: none !important; }
  .lesson-content {
    font-size: 12pt;
    line-height: 1.5;
    color: #000;
    padding: 0;
  }
}
</style>
