<template>
  <!--
    audit-v13 F-13-02 (A1). Renders the blocks an admin authored in the Lesson Builder.
    Before this, lesson_sections/lesson_blocks were written but never read by any learner
    view — the learner page showed only the scraped lesson.content, so 8 lessons' worth of
    authored material was invisible (lesson 447: 3 sections, measured 2026-09-22).

    Two deliberate rules:
      1. Render NOTHING unless the lesson has real, materialized sections. The structure
         endpoint synthesizes a read-only "virtual" TEXT section (id === null) from
         lesson.content when nothing is materialized — rendering it would duplicate the
         lesson body on the ~1,462 lessons that have no authored blocks.
      2. Only TEXT/IMAGE/AUDIO/TABLE are shown. QUESTION/SUBMISSION have no learner view and
         no grading path yet (A2/A3); they are skipped rather than half-rendered.
  -->
  <section v-if="visibleSections.length" class="mt-8 no-print" aria-labelledby="lesson-authored-title">
    <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Nội dung biên soạn</p>
    <h2 id="lesson-authored-title" class="mt-1 text-2xl font-black">Tài liệu bổ sung cho bài học này</h2>

    <div v-for="section in visibleSections" :key="section.key" class="mt-6 space-y-4">
      <h3 v-if="section.title" class="text-lg font-black">{{ section.title }}</h3>

      <template v-for="block in section.blocks" :key="block.key">
        <!-- TEXT -->
        <div v-if="block.type === 'TEXT' && block.text"
             class="lesson-content lesson-html border-2 border-foreground bg-card p-4 shadow-pop"
             v-html="sanitizeHtml(block.text)"></div>

        <!-- IMAGE -->
        <figure v-else-if="block.type === 'IMAGE' && block.imageUrl" class="border-2 border-foreground bg-card p-4 shadow-pop">
          <img :src="block.imageUrl" :alt="block.caption || 'Hình ảnh minh hoạ cho bài học'"
               class="mx-auto max-h-96 object-contain" />
          <figcaption v-if="block.caption" class="mt-2 text-center text-xs font-bold uppercase tracking-wider text-muted-foreground">
            {{ block.caption }}
          </figcaption>
        </figure>

        <!-- AUDIO -->
        <div v-else-if="block.type === 'AUDIO' && block.audioUrl" class="border-2 border-foreground bg-card p-4 shadow-pop">
          <audio :src="block.audioUrl" controls preload="metadata" class="w-full h-10"></audio>
          <p v-if="block.transcript" class="mt-3 text-sm text-muted-foreground">{{ block.transcript }}</p>
        </div>

        <!-- TABLE -->
        <div v-else-if="block.type === 'TABLE' && (block.headers.length || block.rows.length)"
             class="overflow-x-auto border-2 border-foreground bg-white shadow-pop">
          <table class="w-full text-sm">
            <thead v-if="block.headers.length" class="bg-foreground text-white">
              <tr>
                <th v-for="(h, i) in block.headers" :key="i"
                    class="px-3 py-2 text-left font-black uppercase tracking-wider">{{ h }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, ri) in block.rows" :key="ri" class="border-t-2 border-foreground/20">
                <td v-for="(cell, ci) in row" :key="ci" class="px-3 py-2">{{ cell }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import DOMPurify from 'dompurify'
import '../../utils/sanitize-a11y'
import lessonStructureService from '@/services/lessonStructureService'

const route = useRoute()
const sections = ref([])

const RENDERABLE = new Set(['TEXT', 'IMAGE', 'AUDIO', 'TABLE'])

function parse(raw) {
  if (raw == null) return {}
  if (typeof raw === 'object') return raw
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : { content: String(raw) }
  } catch {
    // Legacy rows (e.g. lesson 41881, 91900) store a raw HTML string in `data`,
    // not JSON. Treat it as TEXT content rather than dropping it.
    return { content: String(raw) }
  }
}

function asArray(v) {
  if (Array.isArray(v)) return v
  if (typeof v === 'string' && v.trim()) return v.split(',').map(s => s.trim()).filter(Boolean)
  return []
}

/**
 * Same whitelist LessonContent.vue uses. Kept local so this component cannot be the
 * one place that forgets to sanitize — block `data` is admin-authored HTML and is
 * rendered with v-html.
 */
function sanitizeHtml(html) {
  if (!html) return ''
  const cleaned = String(html)
    .replace(/&lt;script[^&]*?&gt;[\s\S]*?&lt;\/script&gt;/gi, '')
    .replace(/adsbygoogle/gi, '')
  return DOMPurify.sanitize(cleaned, {
    ADD_TAGS: ['details', 'summary', 'audio', 'source'],
    ADD_ATTR: ['controls', 'src', 'open', 'preload'],
    FORBID_TAGS: ['iframe', 'ins', 'script', 'style', 'noscript'],
  })
}

const visibleSections = computed(() =>
  sections.value
    .filter(s => s && s.id != null) // drop the synthesized virtual section
    .map(s => {
      const blocks = (s.blocks || [])
        .filter(b => RENDERABLE.has(b.blockType))
        .map(b => {
          const d = parse(b.data)
          return {
            key: 'b' + b.id,
            type: b.blockType,
            text: d.content ?? '',
            imageUrl: d.imageUrl ?? '',
            caption: d.caption ?? '',
            audioUrl: d.audioUrl ?? '',
            transcript: d.transcript ?? '',
            headers: asArray(d.headers),
            rows: Array.isArray(d.rows) ? d.rows : [],
          }
        })
        // a block with nothing to show is not a block
        .filter(b =>
          (b.type === 'TEXT' && b.text) ||
          (b.type === 'IMAGE' && b.imageUrl) ||
          (b.type === 'AUDIO' && b.audioUrl) ||
          (b.type === 'TABLE' && (b.headers.length || b.rows.length)))
      return { key: 's' + s.id, title: s.title, blocks }
    })
    .filter(s => s.blocks.length)
)

onMounted(async () => {
  try {
    const data = await lessonStructureService.getStructure(Number(route.params.id))
    sections.value = Array.isArray(data) ? data : []
  } catch {
    // Structure is supplementary; a failure must not break the lesson page.
    sections.value = []
  }
})
</script>
