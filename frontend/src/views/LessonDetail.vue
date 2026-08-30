<template>
  <div class="bg-background min-h-screen py-12">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Breadcrumb -->
      <div class="flex items-center gap-2 mb-8">
        <router-link to="/lessons" class="font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent transition-colors">Bài học</router-link>
        <svg class="w-4 h-4 text-muted-foreground" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7"/></svg>
        <span class="font-bold text-sm uppercase tracking-wider text-accent">{{ lesson?.title || 'Chi tiết' }}</span>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-32">
        <div class="w-12 h-12 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
      </div>

      <template v-else-if="lesson">
        <!-- Lesson Header Card -->
        <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl mb-8 relative overflow-hidden">
          <div class="absolute top-4 right-4 w-16 h-16 bg-tertiary/10 border-2 border-foreground/10 rounded-full pointer-events-none"></div>
          <div class="absolute bottom-4 left-1/4 w-10 h-10 bg-secondary/10 border-2 border-foreground/10 rounded-md rotate-12 pointer-events-none"></div>

          <div class="flex flex-col md:flex-row gap-8 relative z-10">
            <div class="w-full md:w-64 h-40 rounded-md overflow-hidden border-2 border-foreground flex-shrink-0">
              <img :src="lesson.thumbnailUrl || 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=500'" class="w-full h-full object-cover" alt="" />
            </div>
            <div class="flex-1">
              <div class="flex items-center gap-3 mb-2">
                <span class="w-3 h-3 rounded-full" :class="levelDotClass"></span>
                <span class="font-bold text-xs uppercase tracking-wider" :class="levelTextClass">{{ lesson.level }}</span>
                <span class="text-xs font-bold text-muted-foreground uppercase tracking-wider">{{ lesson.durationMinutes }} phút</span>
              </div>
              <h1 class="font-black text-3xl md:text-4xl uppercase tracking-tight mb-3">{{ lesson.title }}</h1>
              <p class="font-medium text-muted-foreground leading-relaxed" v-html="sanitizeText(lesson.description)"></p>
            </div>
          </div>
        </div>

        <!-- Tabs -->
        <div class="flex flex-wrap gap-2 mb-8">
          <AppButton v-for="tab in tabs" :key="tab.key"
            @click="activeTab = tab.key"
            :variant="activeTab === tab.key ? 'primary' : 'secondary'"
            size="sm"
          >{{ tab.label }}</AppButton>
        </div>

        <!-- Tab Content -->
        <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl min-h-[300px]">
          <!-- Overview Tab -->
          <div v-if="activeTab === 'overview'" class="geo-markdown" v-html="parseMarkdown(lesson.content)"></div>

          <!-- Vocabulary Tab -->
          <div v-if="activeTab === 'vocabulary'" class="space-y-4">
            <div v-for="(vocab, i) in lesson.vocabularies || []" :key="i"
              class="border-2 border-foreground rounded-md p-5 shadow-pop-lg hover:shadow-pop-xl transition-all"
            >
              <div class="flex items-start justify-between">
                <div>
                  <h3 class="font-black text-xl uppercase tracking-tight">{{ vocab.word }}</h3>
                  <p class="font-bold text-sm text-muted-foreground">{{ vocab.phonetic || '' }}</p>
                </div>
                <div class="flex items-center gap-2">
                  <button v-if="vocab.audioUrl" @click="playAudio(vocab.audioUrl)"
                    class="w-9 h-9 bg-accent border-2 border-foreground rounded-full flex items-center justify-center text-white hover:bg-accent/90 transition-all shadow-pop-sm"
                  ><Volume2 class="w-4 h-4" /></button>
                </div>
              </div>
              <div class="geo-markdown font-bold text-lg mb-4 px-4 text-foreground/80 mt-2" v-html="parseMarkdown(vocab.meaning)"></div>
              <div class="geo-markdown text-foreground mb-0 mt-1" v-html="parseMarkdown(vocab.exampleSentence)"></div>
            </div>
          </div>

          <!-- Grammar Tab -->
          <div v-if="activeTab === 'grammar'" class="space-y-6">
            <div v-for="(gram, i) in lesson.grammars || []" :key="i"
              class="border-2 border-foreground rounded-md p-5 shadow-pop-lg"
            >
              <h3 class="font-black text-xl uppercase tracking-tight mb-3">{{ gram.title || 'Ngữ pháp' }}</h3>
              <div class="geo-markdown mb-6" v-html="parseMarkdown(gram.explanation)"></div>
              <div v-if="gram.formula" class="geo-markdown mb-0" v-html="parseMarkdown('```\n' + gram.formula + '\n```')"></div>
              <div v-if="gram.examples" class="geo-markdown" v-html="parseMarkdown(gram.examples)"></div>
            </div>
          </div>

          <!-- Exercises Tab -->
          <div v-if="activeTab === 'exercises'" class="space-y-4">
            <div v-for="(ex, i) in lesson.exercises || []" :key="i"
              class="border-2 border-foreground rounded-md p-5 shadow-pop-lg"
            >
              <div class="geo-markdown mb-5" v-html="parseMarkdown(ex.question)"></div>
              <audio v-if="ex.audioUrl" :src="ex.audioUrl" controls class="w-full border-2 border-foreground bg-black/5 geo-audio rounded-md mb-4"></audio>
              <div v-if="ex.options" class="space-y-2">
                <label v-for="(opt, oi) in ex.options" :key="oi"
                  class="flex items-center gap-3 p-3 border-2 border-foreground rounded-md cursor-pointer transition-all hover:bg-tertiary/10"
                >
                  <input type="radio" :name="'ex-' + i" class="geo-radio" />
                  <span class="font-medium">{{ opt }}</span>
                </label>
              </div>
              <div v-if="ex.explanation" class="geo-markdown text-foreground/80 mb-0 mt-1" v-html="parseMarkdown(ex.explanation)"></div>
            </div>
          </div>

          <!-- Reading Tab -->
          <div v-if="activeTab === 'reading'" class="geo-markdown" v-html="parseMarkdown(lesson.readingContent)"></div>

          <!-- Listening Tab -->
          <div v-if="activeTab === 'listening'" class="space-y-4">
            <audio v-if="lesson.listeningAudioUrl" :src="lesson.listeningAudioUrl" controls
              class="w-full border-2 border-foreground bg-black/5 geo-audio rounded-md"
            ></audio>
            <div class="geo-markdown mt-4" v-html="parseMarkdown(lesson.listeningContent)"></div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useLessonStore } from '@/store/modules/lesson'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { sanitizeText } from '@/utils/markdown'
import { Volume2 } from 'lucide-vue-next'
import AppButton from '@/components/ui/AppButton.vue'

const route = useRoute()
const store = useLessonStore()
const loading = ref(true)
const lesson = ref(null)
const activeTab = ref('overview')

onMounted(async () => {
  try {
    await store.fetchLessonDetail(route.params.id)
    lesson.value = store.currentLesson
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

const tabs = computed(() => {
  const t = [{ key: 'overview', label: 'Tổng quan' }]
  if (lesson.value?.vocabularies?.length) t.push({ key: 'vocabulary', label: 'Từ vựng' })
  if (lesson.value?.grammars?.length) t.push({ key: 'grammar', label: 'Ngữ pháp' })
  if (lesson.value?.exercises?.length) t.push({ key: 'exercises', label: 'Bài tập' })
  if (lesson.value?.readingContent) t.push({ key: 'reading', label: 'Đọc' })
  if (lesson.value?.listeningContent || lesson.value?.listeningAudioUrl) t.push({ key: 'listening', label: 'Nghe' })
  return t
})

const levelDotClass = computed(() => {
  const map = { ELEMENTARY: 'bg-accent', PRE_INTERMEDIATE: 'bg-tertiary', INTERMEDIATE: 'bg-secondary', UPPER_INTERMEDIATE: 'bg-quaternary' }
  return map[lesson.value?.level] || 'bg-muted-foreground'
})
const levelTextClass = computed(() => {
  const map = { ELEMENTARY: 'text-accent', PRE_INTERMEDIATE: 'text-tertiary', INTERMEDIATE: 'text-secondary', UPPER_INTERMEDIATE: 'text-quaternary' }
  return map[lesson.value?.level] || 'text-muted-foreground'
})

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}

function playAudio(url) {
  new Audio(url).play()
}
</script>

<style scoped>
input.geo-radio {
  appearance: none;
  width: 20px; height: 20px;
  border: 2px solid var(--geo-fg, #1E293B);
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 300ms cubic-bezier(0.34, 1.56, 0.64, 1);
  flex-shrink: 0;
}
input.geo-radio:checked {
  border-color: var(--geo-accent, #8B5CF6);
  background: var(--geo-accent, #8B5CF6);
  box-shadow: inset 0 0 0 3px white;
}
audio.geo-audio { border-radius: 8px; }
</style>
