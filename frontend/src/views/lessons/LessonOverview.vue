<template>
  <div class="space-y-8">
    <!-- LESSON CONTENT (raw HTML from seed) -->
    <div v-if="!loading && lesson?.content">
      <div class="lesson-html prose prose-lg max-w-none"
        v-html="sanitizeHtml(lesson.content)"
      ></div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-16">
      <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
    </div>

    <!-- Empty state -->
    <div v-if="!loading && !lesson" class="text-center py-16">
      <p class="text-xl font-bold text-muted-foreground">Bài học này chưa có nội dung.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import DOMPurify from 'dompurify'
import lessonService from '@/services/lessonService'

const route = useRoute()
const lessonId = Number(route.params.id)

const lesson = ref(null)
const loading = ref(true)

function sanitizeHtml(html) {
  if (!html) return ''
  return DOMPurify.sanitize(html, {
    ADD_TAGS: ['iframe'],
    ADD_ATTR: ['allow', 'allowfullscreen', 'frameborder', 'scrolling', 'target']
  })
}

onMounted(async () => {
  try {
    const data = await lessonService.getById(lessonId)
    lesson.value = data
  } catch (e) {
    console.error('Failed to load lesson:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* Styling for raw HTML content from english-practice.net */
.lesson-html :deep(h1),
.lesson-html :deep(h2),
.lesson-html :deep(h3) {
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: -0.02em;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
}
.lesson-html :deep(p) {
  margin-bottom: 1rem;
  line-height: 1.7;
}
.lesson-html :deep(img) {
  max-width: 100%;
  border-radius: 0.5rem;
  border: 2px solid #1E293B;
  margin: 1.5rem auto;
  height: auto;
}
.lesson-html :deep(.skill-html) {
  max-width: 100%;
  overflow-x: auto;
}
.lesson-html :deep(.entry-content) {
  line-height: 1.8;
}
.lesson-html :deep(.et_post_meta_wrapper) {
  margin-bottom: 1.5rem;
}
/* Hide ads from scraped content */
.lesson-html :deep(ins.adsbygoogle) {
  display: none !important;
}
.lesson-html :deep(script) {
  display: none !important;
}
/* Tables */
.lesson-html :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1rem 0;
  border: 2px solid #1E293B;
}
.lesson-html :deep(td),
.lesson-html :deep(th) {
  border: 1px solid #1E293B;
  padding: 0.5rem;
  font-weight: 500;
}
.lesson-html :deep(th) {
  background: rgba(139, 92, 246, 0.1);
  font-weight: 900;
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 0.05em;
}
.lesson-html :deep(blockquote) {
  border-left: 4px solid #8B5CF6;
  padding-left: 1rem;
  margin: 1rem 0;
  font-style: italic;
  opacity: 0.8;
}
.lesson-html :deep(ul),
.lesson-html :deep(ol) {
  padding-left: 1.5rem;
  margin-bottom: 1rem;
}
.lesson-html :deep(li) {
  margin-bottom: 0.25rem;
  line-height: 1.6;
}
/* Ensure all deep content has proper text color */
.lesson-html :deep(*) {
  color: inherit;
}
.lesson-html :deep(a) {
  color: #8B5CF6;
  text-decoration: underline;
}
/* Section headings for skill sections */
.lesson-html :deep(.et_pb_text_inner) {
  max-width: 100%;
}
/* Entry title should be normal text, not hidden */
.lesson-html :deep(.entry-title) {
  font-size: 1.5rem;
  margin-bottom: 1rem;
}
</style>
