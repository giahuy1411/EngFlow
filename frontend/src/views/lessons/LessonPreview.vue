<template>
  <div class="space-y-6">
    <h2 class="font-black text-2xl uppercase">Lịch sử làm bài</h2>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-16">
      <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
    </div>

    <!-- Empty -->
    <div v-else-if="attempts.length === 0" class="text-center py-16 bg-card border-2 border-foreground shadow-pop-lg rounded-md">
      <p class="font-black text-xl uppercase mb-2">Chưa có lịch sử</p>
      <p class="text-muted-foreground font-medium">Làm bài tập ở tab Nội dung để theo dõi tiến độ.</p>
    </div>

    <!-- Attempt list -->
    <div v-else class="space-y-4">
      <div v-for="a in attempts" :key="a.id"
        class="bg-card border-2 border-foreground shadow-pop-lg rounded-md overflow-hidden cursor-pointer transition-all hover:-translate-x-0.5 hover:-translate-y-0.5"
        @click="toggleExpand(a.id)">

        <!-- Summary row -->
        <div class="p-5 flex items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full border-2 border-foreground flex items-center justify-center font-black text-lg"
              :class="a.percentage >= 70 ? 'bg-quaternary text-white' : 'bg-accent-strong text-white'">
              {{ Math.round(a.percentage) }}%
            </div>
            <div>
              <p class="font-black text-sm uppercase">
                <span :class="a.percentage >= 70 ? 'text-success-ink' : 'text-danger'">
                  {{ a.percentage >= 70 ? 'Đạt' : 'Chưa đạt' }}
                </span>
              </p>
              <p class="font-bold text-xs text-muted-foreground">{{ a.score }} / {{ a.total }} câu đúng</p>
            </div>
          </div>
          <div class="text-right">
            <p class="font-bold text-xs text-muted-foreground">{{ formatDate(a.completedAt) }}</p>
            <svg class="w-5 h-5 mt-1 ml-auto transition-transform" :class="{ 'rotate-180': expanded[a.id] }"
              fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7"/>
            </svg>
          </div>
        </div>

        <!-- Expanded detail -->
        <div v-if="expanded[a.id]" class="border-t-2 border-foreground">
          <div v-if="loadingDetail[a.id]" class="flex justify-center py-8">
            <div class="w-8 h-8 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
          </div>
          <div v-else-if="detailCache[a.id]" class="divide-y-2 divide-foreground">
            <div v-for="(item, i) in detailCache[a.id].details" :key="i"
              class="p-4 flex items-start gap-3"
              :class="item.correct ? 'bg-quaternary/5' : 'bg-accent/5'">
              <span class="text-lg mt-0.5">{{ item.correct ? '✅' : '❌' }}</span>
              <div class="min-w-0 flex-1">
                <div class="geo-markdown text-sm mb-1" v-html="parseMarkdown(item.question)"></div>
                <p class="font-bold text-xs">Đáp án: <span class="text-success-ink">{{ item.correctAnswer }}</span></p>
                <p v-if="!item.correct" class="text-xs text-accent-ink">Bạn: {{ item.userAnswer }}</p>
                <p v-if="item.explanation" class="text-xs text-muted-foreground italic mt-1" v-html="sanitizeText(item.explanation)"></p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import lessonService from '@/services/lessonService'
import { sanitizeText } from '@/utils/markdown'

const route = useRoute()
const lessonId = Number(route.params.id)

const attempts = ref([])
const loading = ref(true)
const expanded = ref({})
const loadingDetail = ref({})
const detailCache = ref({})

function formatDate(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  const now = new Date()
  const diff = now - d
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins} phút trước`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} giờ trước`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days} ngày trước`
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function parseMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}

async function toggleExpand(id) {
  expanded.value[id] = !expanded.value[id]
  if (expanded.value[id] && !detailCache.value[id]) {
    loadingDetail.value[id] = true
    try {
      const detail = await lessonService.getAttemptDetail(lessonId, id)
      detailCache.value[id] = detail
    } catch (e) {
      console.error('Failed to load detail:', e)
    } finally {
      loadingDetail.value[id] = false
    }
  }
}

onMounted(async () => {
  try {
    const data = await lessonService.getAttempts(lessonId)
    attempts.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('Failed to load attempts:', e)
  } finally {
    loading.value = false
  }
})
</script>
