<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-accent border-2 border-foreground flex items-center justify-center rotate-6 rounded-md">
          <ClapperboardIcon class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Bài Học Video</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-muted-foreground">Thêm video YouTube + phụ đề để học viên luyện shadowing</p>
        </div>
      </div>
      <AppButton @click="openForm(null)" variant="primary">
        <PlusIcon class="w-4 h-4" />
        Thêm video
      </AppButton>
    </div>

    <!-- Form modal -->
    <AppModal v-model="showForm" :title="editing ? 'Sửa bài học video' : 'Bài học video mới'" size="lg">
      <form id="video-lesson-form" class="space-y-4" @submit.prevent="save">
        <div class="grid md:grid-cols-2 gap-4">
          <div>
            <label for="vl-title" class="app-form-field__label">Tiêu đề <span class="app-form-field__required">*</span></label>
            <input id="vl-title" v-model="form.title" class="app-input" required />
          </div>
          <div>
            <label for="vl-level" class="app-form-field__label">Trình độ</label>
            <select id="vl-level" v-model="form.level" class="app-input">
              <option value="ELEMENTARY">Elementary (A1–A2)</option>
              <option value="PRE_INTERMEDIATE">Pre-Intermediate (B1)</option>
              <option value="INTERMEDIATE">Intermediate (B2)</option>
              <option value="UPPER_INTERMEDIATE">Upper-Intermediate (C1)</option>
            </select>
          </div>
          <div class="md:col-span-2">
            <label for="vl-url" class="app-form-field__label">Link YouTube <span class="app-form-field__required">*</span></label>
            <div class="flex gap-2">
              <input id="vl-url" v-model="form.youtubeUrl" placeholder="https://www.youtube.com/watch?v=..." class="app-input flex-1" required />
              <AppButton type="button" variant="primary" size="sm" :disabled="!previewVideoId || fetching" :loading="fetching" @click="fetchSubtitles">
                <SparklesIcon class="w-4 h-4" aria-hidden="true" />
                AI tạo phụ đề
              </AppButton>
            </div>
            <p v-if="previewVideoId" class="mt-1 text-xs font-bold text-success">Video ID: {{ previewVideoId }}</p>
            <p class="mt-1 text-xs font-medium text-muted-foreground">Dán link rồi bấm "AI tạo phụ đề" — hệ thống lấy phụ đề sẵn có của video (kể cả tự động) và dịch sang tiếng Việt, đồng thời điền tiêu đề + mô tả.</p>
          </div>
          <div class="md:col-span-2">
            <label for="vl-desc" class="app-form-field__label">Mô tả ngắn</label>
            <textarea id="vl-desc" v-model="form.description" rows="2" class="app-input"></textarea>
          </div>
          <div class="md:col-span-2">
            <label for="vl-transcript" class="app-form-field__label">Phụ đề (dán nội dung .srt / .vtt hoặc JSON) <span class="app-form-field__required">*</span></label>
            <textarea id="vl-transcript" v-model="form.transcriptText" rows="8" spellcheck="false"
              placeholder="Bấm &quot;AI tạo phụ đề&quot; để tự điền, hoặc dán SRT/VTT tại đây..."
              :class="['app-input font-mono text-xs', { 'app-input--invalid': transcriptError }]"
              @input="onTranscriptEdited"></textarea>
            <div class="flex flex-wrap items-center gap-3 mt-2">
              <input id="vl-file" type="file" accept=".srt,.vtt,.txt" class="text-xs font-bold" @change="onFilePicked" />
              <span v-if="parsedPreview.length" class="text-xs font-black text-success uppercase tracking-wider">Nhận {{ parsedPreview.length }} dòng</span>
              <AppButton v-if="parsedPreview.length" type="button" variant="secondary" size="sm" :disabled="translating" :loading="translating" @click="translateSubtitles">
                <LanguagesIcon class="w-4 h-4" aria-hidden="true" />
                {{ translating ? 'AI đang dịch...' : 'AI dịch sang tiếng Việt' }}
              </AppButton>
            </div>
            <p v-if="transcriptError" class="app-form-field__error" role="alert">{{ transcriptError }}</p>
          </div>
        </div>
      </form>
      <template #footer>
        <AppButton variant="secondary" @click="showForm = false">Hủy</AppButton>
        <AppButton type="submit" form="video-lesson-form" :disabled="saving" :loading="saving" variant="tertiary">
          {{ saving ? 'Đang lưu...' : 'Lưu bài học' }}
        </AppButton>
      </template>
    </AppModal>

    <!-- Table -->
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin w-8 h-8 border-2 border-foreground border-t-transparent rounded-full mx-auto"></div>
    </div>
    <div v-else-if="error" class="bg-danger/10 border-2 border-danger p-6 rounded">
      <p class="font-bold text-danger">{{ error }}</p>
      <button @click="loadLessons" class="mt-2 text-sm font-bold underline">Thử lại</button>
    </div>
    <div v-else class="bg-white border-2 border-foreground overflow-hidden rounded-md shadow-pop-lg">
      <div class="bg-foreground border-b-2 border-foreground px-6 py-3 flex items-center gap-3">
        <div class="w-2 h-2 bg-tertiary rotate-45 rounded"></div>
        <span class="font-bold text-xs uppercase tracking-widest text-white/60">{{ lessons.length }} bài học video</span>
      </div>
      <table class="w-full text-sm">
        <thead class="bg-accent/20 border-b-2 border-foreground font-black uppercase text-xs tracking-wider">
          <tr>
            <th class="text-left p-4 border-r-2 border-foreground">Tiêu đề</th>
            <th class="text-left p-4 border-r-2 border-foreground">Level</th>
            <th class="text-center p-4 border-r-2 border-foreground">Số câu</th>
            <th class="text-center p-4">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="l in lessons" :key="l.id" class="border-b-2 border-foreground hover:bg-accent/5 transition-colors duration-150">
            <td class="p-4 border-r-2 border-foreground font-bold">{{ l.title }}</td>
            <td class="p-4 border-r-2 border-foreground">{{ levelLabel(l.level) }}</td>
            <td class="p-4 border-r-2 border-foreground text-center tabular-nums">{{ l.lineCount }}</td>
            <td class="p-4 text-center">
              <router-link :to="`/videos/${l.id}`" class="text-xs font-black uppercase tracking-wider underline mr-3">Xem</router-link>
              <AppButton @click="deleteLesson(l.id)" variant="danger" size="sm">Xóa</AppButton>
            </td>
          </tr>
          <tr v-if="lessons.length === 0">
            <td colspan="4" class="p-6 text-center text-muted-foreground font-bold uppercase text-xs">Chưa có bài học video nào</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ClapperboardIcon, PlusIcon, SparklesIcon, LanguagesIcon } from 'lucide-vue-next'
import videoLessonService from '@/services/videoLessonService'
import { parseSubtitlePreview } from '@/utils/subtitlePreview'
import AppButton from '@/components/ui/AppButton.vue'
import AppModal from '@/components/ui/AppModal.vue'
import { useToast } from '@/composables/useToast'
import { levelLabel } from '@/utils/lessonLevels'

const toast = useToast()

const lessons = ref([])
const loading = ref(true)
const error = ref(null)
const showForm = ref(false)
const editing = ref(null)
const saving = ref(false)
const transcriptError = ref('')
const translating = ref(false)
const fetching = ref(false)
// AI-translated Vietnamese lines, keyed by cue index; merged into the payload on save.
const translatedLines = ref({})

const form = ref(defaultForm())

function defaultForm() {
  return { title: '', description: '', youtubeUrl: '', level: 'ELEMENTARY', transcriptText: '' }
}

const parsedPreview = computed(() => {
  if (!form.value.transcriptText.trim()) return []
  try {
    return parseSubtitlePreview(form.value.transcriptText)
  } catch {
    return []
  }
})

const previewVideoId = computed(() => {
  const m = (form.value.youtubeUrl || '').match(/(?:v=|embed\/|shorts\/|youtu\.be\/)([A-Za-z0-9_-]{11})/)
  return m ? m[1] : ''
})

onMounted(loadLessons)

async function loadLessons() {
  loading.value = true
  error.value = null
  try {
    const pageData = await videoLessonService.getAllAdmin(0, 100)
    lessons.value = pageData.content || []
  } catch (err) {
    error.value = err.response?.data?.message || 'Không thể tải danh sách bài học video'
  } finally {
    loading.value = false
  }
}

function openForm(lesson = null) {
  editing.value = lesson ? lesson.id : null
  // audit-v6 F21: keep transcript empty in the box — an empty box now means
  // "giữ phụ đề cũ" (backend keeps current transcript on update).
  form.value = lesson
    ? { title: lesson.title, description: lesson.description || '', youtubeUrl: lesson.youtubeUrl || '', level: lesson.level || 'ELEMENTARY', transcriptText: '' }
    : defaultForm()
  transcriptError.value = ''
  translatedLines.value = {}
  showForm.value = true
}

function onFilePicked(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    form.value.transcriptText = String(reader.result || '')
    translatedLines.value = {}
  }
  reader.readAsText(file)
}

function onTranscriptEdited() {
  transcriptError.value = ''
}

/**
 * Fetches the video's existing captions (auto or human) and translates them
 * to Vietnamese in one backend call; also pre-fills title and description.
 */
async function fetchSubtitles() {
  if (!previewVideoId.value || fetching.value) return
  fetching.value = true
  try {
    const result = await videoLessonService.fetchYoutubeTranscript(form.value.youtubeUrl)
    const lines = (result.lines || []).map(l => ({ start: l.start, end: l.end, textEn: l.textEn, textVi: l.textVi }))
    if (!lines.length) {
      toast.showError('Video này không có phụ đề để lấy — hãy dán file .srt')
      return
    }
    form.value.transcriptText = JSON.stringify(lines, null, 0)
    // Backend already translated; key the Vietnamese by cue index directly.
    const map = {}
    let filled = 0
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].textVi) {
        map[i] = lines[i].textVi
        filled++
      }
    }
    translatedLines.value = map
    if (!form.value.title && result.title) form.value.title = result.title
    if (!form.value.description && result.description) {
      form.value.description = result.description.slice(0, 300)
    }
    transcriptError.value = ''
    toast.showSuccess(filled > 0
      ? `Đã lấy ${lines.length} dòng phụ đề (dịch ${filled}/${lines.length} câu tiếng Việt)`
      : `Đã lấy ${lines.length} dòng phụ đề`)
  } catch (err) {
    toast.showError(err.response?.data?.detail || err.response?.data?.error || err.response?.data?.message || 'Không lấy được phụ đề — thử dán file .srt tay')
  } finally {
    fetching.value = false
  }
}

/**
 * Sends the parsed EN cues to the local LLM for Vietnamese translation and
 * stores the result keyed by cue index; merged into the transcript on save.
 */
async function translateSubtitles() {
  if (!parsedPreview.value.length || translating.value) return
  translating.value = true
  try {
    const lines = parsedPreview.value.map(cue => ({ start: cue.start, end: cue.end, textEn: cue.text, textVi: null }))
    const translated = await videoLessonService.translateTranscript(lines)
    const map = {}
    let filled = 0
    for (let i = 0; i < translated.length; i++) {
      if (translated[i]?.textVi) {
        map[i] = translated[i].textVi
        filled++
      }
    }
    translatedLines.value = map
    if (filled > 0) {
      toast.showSuccess(`AI đã dịch ${filled}/${parsedPreview.value.length} dòng`)
    } else {
      toast.showError('AI chưa dịch được dòng nào — thử lại sau')
    }
  } catch (err) {
    toast.showError(err.response?.data?.detail || 'Dịch phụ đề thất bại')
  } finally {
    translating.value = false
  }
}

async function save() {
  transcriptError.value = ''
  const isJson = form.value.transcriptText.trim().startsWith('[')
  // audit-v6 F21: on EDIT, an empty transcript box = keep the existing one
  // (backend null/empty-guard). Only CREATE requires ≥1 valid cue.
  if (!editing.value && !parsedPreview.value.length && !isJson) {
    transcriptError.value = 'Phụ đề không hợp lệ — cần ít nhất 1 dòng SRT/VTT hoặc mảng JSON.'
    return
  }
  saving.value = true
  try {
    // For pasted SRT/VTT the EN text comes from the parser; for AI-fetched or
    // pasted JSON the transcriptText is already an array with textVi included.
    let transcript = null
    if (isJson) {
      transcript = JSON.parse(form.value.transcriptText)
    } else if (parsedPreview.value.length) {
      transcript = parsedPreview.value.map((cue, i) => ({ start: cue.start, end: cue.end, textEn: cue.text, textVi: translatedLines.value[i] || null }))
    }
    const meta = {
      title: form.value.title,
      description: form.value.description,
      youtubeUrl: form.value.youtubeUrl,
      level: form.value.level,
      transcript
    }
    if (editing.value) {
      await videoLessonService.update(editing.value, meta)
    } else {
      await videoLessonService.create(meta)
    }
    showForm.value = false
    await loadLessons()
    toast.showSuccess('Đã lưu bài học video')
  } catch (err) {
    toast.showError(err.response?.data?.detail || err.response?.data?.message || 'Lưu thất bại')
  } finally {
    saving.value = false
  }
}

async function deleteLesson(id) {
  if (!confirm('Bạn có chắc muốn xóa bài học video này?')) return
  try {
    await videoLessonService.remove(id)
    await loadLessons()
    toast.showSuccess('Đã xóa bài học video')
  } catch (err) {
    toast.showError(err.response?.data?.message || 'Xóa thất bại')
  }
}
</script>
