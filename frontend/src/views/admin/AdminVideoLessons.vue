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

    <!-- Form -->
    <div v-if="showForm" class="bg-white border-2 border-foreground p-6 shadow-pop rounded-md">
      <h3 class="font-black text-xl mb-4 uppercase tracking-wider">{{ editing ? 'Sửa bài học video' : 'Bài học video mới' }}</h3>
      <div class="grid md:grid-cols-2 gap-4 mb-4">
        <div>
          <label for="vl-title" class="block text-xs font-bold uppercase mb-1">Tiêu đề</label>
          <input id="vl-title" v-model="form.title" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded" required />
        </div>
        <div>
          <label for="vl-level" class="block text-xs font-bold uppercase mb-1">Trình độ</label>
          <select id="vl-level" v-model="form.level" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded">
            <option value="ELEMENTARY">Elementary (A1–A2)</option>
            <option value="PRE_INTERMEDIATE">Pre-Intermediate (B1)</option>
            <option value="INTERMEDIATE">Intermediate (B2)</option>
            <option value="UPPER_INTERMEDIATE">Upper-Intermediate (C1)</option>
          </select>
        </div>
        <div class="md:col-span-2">
          <label for="vl-url" class="block text-xs font-bold uppercase mb-1">Link YouTube</label>
          <input id="vl-url" v-model="form.youtubeUrl" placeholder="https://www.youtube.com/watch?v=..." class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded" required />
          <p v-if="previewVideoId" class="mt-1 text-xs font-bold text-success">Video ID: {{ previewVideoId }}</p>
        </div>
        <div class="md:col-span-2">
          <label for="vl-desc" class="block text-xs font-bold uppercase mb-1">Mô tả ngắn</label>
          <textarea id="vl-desc" v-model="form.description" rows="2" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded"></textarea>
        </div>
        <div>
          <label for="vl-category" class="block text-xs font-bold uppercase mb-1">Danh mục</label>
          <input id="vl-category" v-model="form.category" placeholder="Giao tiếp, IELTS..." class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded" />
        </div>
        <div class="flex items-end">
          <label class="flex items-center gap-2 text-sm font-bold"><input type="checkbox" v-model="form.isPublished" /> Công khai</label>
        </div>
        <div class="md:col-span-2">
          <label for="vl-transcript" class="block text-xs font-bold uppercase mb-1">Phụ đề (dán nội dung .srt / .vtt hoặc JSON)</label>
          <textarea id="vl-transcript" v-model="form.transcriptText" rows="8" spellcheck="false"
            placeholder="1&#10;00:00:01,000 --> 00:00:04,500&#10;Hello, dear English learners.&#10;&#10;2&#10;..."
            class="w-full border-2 border-border p-2 text-xs font-mono focus:border-foreground outline-none rounded"></textarea>
          <div class="flex items-center gap-3 mt-2">
            <input id="vl-file" type="file" accept=".srt,.vtt,.txt" class="text-xs font-bold" @change="onFilePicked" />
            <span v-if="parsedPreview.length" class="text-xs font-black text-success uppercase tracking-wider">Nhận {{ parsedPreview.length }} dòng</span>
          </div>
          <p v-if="transcriptError" class="mt-1 text-xs font-bold text-danger">{{ transcriptError }}</p>
        </div>
      </div>
      <div class="flex gap-3">
        <AppButton @click="save" :disabled="saving" :loading="saving" variant="tertiary">
          {{ saving ? 'Đang lưu...' : 'Lưu bài học' }}
        </AppButton>
        <AppButton @click="showForm = false" variant="secondary">Hủy</AppButton>
      </div>
    </div>

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
            <th class="text-center p-4 border-r-2 border-foreground">Công khai</th>
            <th class="text-center p-4">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="l in lessons" :key="l.id" class="border-b-2 border-foreground hover:bg-accent/5 transition-colors duration-150">
            <td class="p-4 border-r-2 border-foreground font-bold">{{ l.title }}</td>
            <td class="p-4 border-r-2 border-foreground">{{ levelLabel(l.level) }}</td>
            <td class="p-4 border-r-2 border-foreground text-center tabular-nums">{{ l.lineCount }}</td>
            <td class="p-4 border-r-2 border-foreground text-center">{{ l.isPublished ? 'Có' : 'Không' }}</td>
            <td class="p-4 text-center">
              <router-link :to="`/videos/${l.id}`" class="text-xs font-black uppercase tracking-wider underline mr-3">Xem</router-link>
              <AppButton @click="deleteLesson(l.id)" variant="danger" size="sm">Xóa</AppButton>
            </td>
          </tr>
          <tr v-if="lessons.length === 0">
            <td colspan="5" class="p-6 text-center text-muted-foreground font-bold uppercase text-xs">Chưa có bài học video nào</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ClapperboardIcon, PlusIcon } from 'lucide-vue-next'
import videoLessonService from '@/services/videoLessonService'
import { parseSubtitlePreview } from '@/utils/subtitlePreview'
import AppButton from '@/components/ui/AppButton.vue'
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

const form = ref(defaultForm())

function defaultForm() {
  return { title: '', description: '', youtubeUrl: '', level: 'ELEMENTARY', category: '', transcriptText: '', isPublished: true }
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

function openForm() {
  editing.value = null
  form.value = defaultForm()
  transcriptError.value = ''
  showForm.value = true
}

function onFilePicked(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => { form.value.transcriptText = String(reader.result || '') }
  reader.readAsText(file)
}

async function save() {
  transcriptError.value = ''
  if (!parsedPreview.value.length && !form.value.transcriptText.trim().startsWith('[')) {
    transcriptError.value = 'Phụ đề không hợp lệ — cần ít nhất 1 dòng SRT/VTT hoặc mảng JSON.'
    return
  }
  saving.value = true
  try {
    const meta = {
      title: form.value.title,
      description: form.value.description,
      youtubeUrl: form.value.youtubeUrl,
      level: form.value.level,
      category: form.value.category,
      transcript: parsedPreview.value.map(cue => ({ start: cue.start, end: cue.end, textEn: cue.text, textVi: null })),
      isPublished: form.value.isPublished
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
