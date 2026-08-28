<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-accent border-2 border-foreground flex items-center justify-center rotate-6 rounded-md">
          <Mic2Icon class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Đề Bài Speaking</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-muted-foreground">Quản lý đề bài & mẫu câu đọc nói</p>
        </div>
      </div>
      <AppButton
        @click="openPromptForm(null)"
        variant="primary"
      >
        <PlusIcon class="w-4 h-4" />
        Thêm đề bài
      </AppButton>
    </div>

    <!-- Actions & Filters -->
    <div class="flex flex-wrap items-center gap-3">
      <div class="relative flex-1 min-w-[200px] sm:max-w-xs">
        <SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <input
          id="prompt-search"
          name="prompt-search"
          v-model="searchQuery"
          type="text"
          placeholder="Tìm tiêu đề, mô tả..."
          class="w-full border-2 border-foreground bg-white pl-10 pr-3 py-2.5 text-sm font-bold rounded-md focus:outline-none focus:ring-2 focus:ring-accent transition-all shadow-pop-sm"
        />
      </div>
      <select id="prompt-level-filter" name="prompt-level-filter" v-model="levelFilter" class="border-2 border-foreground bg-white px-4 py-2.5 text-sm font-bold rounded-md focus:outline-none focus:ring-2 focus:ring-accent transition-all shadow-pop-sm">
        <option value="">Tất cả cấp độ</option>
        <option value="A1">A1 - Beginner</option>
        <option value="A2">A2 - Elementary</option>
        <option value="B1">B1 - Intermediate</option>
        <option value="B2">B2 - Upper Intermediate</option>
        <option value="C1">C1 - Advanced</option>
      </select>
    </div>

    <!-- Form Modal/Card -->
    <div v-if="showForm" class="bg-white border-2 border-foreground p-6 shadow-pop rounded-md">
      <h3 class="font-black text-xl mb-4 uppercase tracking-wider">{{ editing ? 'Sửa đề bài' : 'Thêm đề bài mới' }}</h3>
      <div class="grid md:grid-cols-2 gap-4 mb-4">
        <div>
          <label for="prompt-title" class="block text-xs font-bold uppercase mb-1">Tiêu đề</label>
          <input id="prompt-title" v-model="form.title" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded" required />
        </div>
        <div>
          <label for="prompt-level" class="block text-xs font-bold uppercase mb-1">Level</label>
          <input id="prompt-level" v-model="form.level" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded" placeholder="A1/A2/B1/B2/C1" />
        </div>
        <div class="md:col-span-2">
          <label for="prompt-desc" class="block text-xs font-bold uppercase mb-1">Mô tả ngắn</label>
          <textarea id="prompt-desc" v-model="form.description" rows="2" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded"></textarea>
        </div>
        <div class="md:col-span-2">
          <label for="prompt-body" class="block text-xs font-bold uppercase mb-1">Nội dung câu hỏi / Đề bài</label>
          <textarea id="prompt-body" v-model="form.prompt" rows="3" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded" required></textarea>
        </div>
        <div>
          <label for="prompt-category" class="block text-xs font-bold uppercase mb-1">Danh mục</label>
          <input id="prompt-category" v-model="form.category" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded" />
        </div>
        <div>
          <label for="prompt-order" class="block text-xs font-bold uppercase mb-1">Thứ tự hiển thị</label>
          <input id="prompt-order" v-model.number="form.orderIndex" type="number" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded" />
        </div>
        <div>
          <label for="prompt-lesson" class="block text-xs font-bold uppercase mb-1">Bài học (Tùy chọn)</label>
          <select id="prompt-lesson" v-model="form.lessonId" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded">
            <option :value="null">Bài luyện độc lập</option>
            <option v-for="lesson in lessons" :key="lesson.id" :value="lesson.id">{{ lesson.title }}</option>
          </select>
        </div>
        <div>
          <label for="prompt-mode" class="block text-xs font-bold uppercase mb-1">Chế độ luyện tập</label>
          <select id="prompt-mode" v-model="form.mode" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded">
            <option value="READ_ALOUD">Đọc theo mẫu</option>
            <option value="FREE_SPEAKING">Nói tự do</option>
          </select>
        </div>
        <div class="md:col-span-2">
          <label for="prompt-reference" class="block text-xs font-bold uppercase mb-1">Nội dung đọc mẫu (referenceText)</label>
          <textarea id="prompt-reference" v-model="form.referenceText" rows="3" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded"></textarea>
          <AppButton
            v-if="form.mode === 'FREE_SPEAKING'"
            type="button"
            class="mt-2"
            :disabled="generating"
            @click="aiGenerate"
            variant="primary"
            size="sm"
          >
            {{ generating ? 'Đang tạo...' : 'Tạo nội dung bằng AI' }}
          </AppButton>
        </div>
        <div>
          <label for="prompt-duration" class="block text-xs font-bold uppercase mb-1">Thời lượng tối đa (giây)</label>
          <input id="prompt-duration" v-model.number="form.maxDurationSeconds" type="number" min="15" max="300" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded" />
        </div>
        <div>
          <label for="prompt-attempts" class="block text-xs font-bold uppercase mb-1">Số lượt tối đa</label>
          <input id="prompt-attempts" v-model.number="form.attemptLimit" type="number" min="1" max="100" class="w-full border-2 border-border p-2 text-sm focus:border-foreground outline-none rounded" />
        </div>
        <div class="flex items-center gap-6 md:col-span-2">
          <label class="flex items-center gap-2 text-sm font-bold"><input type="checkbox" v-model="form.isPremium" /> Premium</label>
          <label class="flex items-center gap-2 text-sm font-bold"><input type="checkbox" v-model="form.isPublished" /> Công khai</label>
        </div>
      </div>
      <div class="flex gap-3">
        <AppButton
          @click="savePrompt"
          :disabled="saving"
          :loading="saving"
          variant="tertiary"
        >
          {{ saving ? 'Đang lưu...' : 'Lưu đề bài' }}
        </AppButton>
        <AppButton
          @click="showForm = false"
          variant="secondary"
        >
          Hủy
        </AppButton>
      </div>
    </div>

    <!-- Table -->
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin w-8 h-8 border-2 border-foreground border-t-transparent rounded-full mx-auto"></div>
    </div>
    <div v-else-if="error" class="bg-danger/10 border-2 border-danger p-6 rounded">
      <p class="font-bold text-danger">{{ error }}</p>
      <button @click="loadPrompts()" class="mt-2 text-sm font-bold underline">Thử lại</button>
    </div>
    <div v-else class="bg-white border-2 border-foreground overflow-hidden rounded-md shadow-pop-lg">
      <div class="bg-foreground border-b-2 border-foreground px-6 py-3 flex items-center gap-3">
        <div class="w-2 h-2 bg-tertiary rotate-45 rounded"></div>
        <span class="font-bold text-xs uppercase tracking-widest text-white/60">{{ filteredPrompts.length }} đề bài</span>
      </div>
      <table class="w-full text-sm">
        <thead class="bg-accent/20 border-b-2 border-foreground font-black uppercase text-xs tracking-wider">
          <tr>
            <th class="text-left p-4 border-r-2 border-foreground">Tiêu đề</th>
            <th class="text-left p-4 border-r-2 border-foreground">Level</th>
            <th class="text-left p-4 border-r-2 border-foreground">Chế độ</th>
            <th class="text-center p-4 border-r-2 border-foreground">Premium</th>
            <th class="text-center p-4 border-r-2 border-foreground">Thứ tự</th>
            <th class="text-center p-4">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in paginatedPrompts" :key="p.id" class="border-b-2 border-foreground hover:bg-accent/5 transition-colors duration-150">
            <td class="p-4 border-r-2 border-foreground font-bold">{{ p.title }}</td>
            <td class="p-4 border-r-2 border-foreground">{{ p.level || '-' }}</td>
            <td class="p-4 border-r-2 border-foreground">
              <span class="px-2 py-1 text-xs font-bold rounded border border-foreground" :class="p.mode === 'FREE_SPEAKING' ? 'bg-tertiary/30' : 'bg-secondary/30'">
                {{ p.mode === 'FREE_SPEAKING' ? 'Nói tự do' : 'Đọc mẫu' }}
              </span>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center">{{ p.isPremium ? 'Có' : 'Không' }}</td>
            <td class="p-4 border-r-2 border-foreground text-center">{{ p.orderIndex ?? '-' }}</td>
            <td class="p-4 text-center">
              <AppButton @click="editPrompt(p)" variant="secondary" size="sm" class="mr-2">Sửa</AppButton>
              <AppButton @click="deletePrompt(p.id)" variant="danger" size="sm">Xóa</AppButton>
            </td>
          </tr>
          <tr v-if="filteredPrompts.length === 0">
            <td colspan="6" class="p-6 text-center text-muted-foreground font-bold uppercase text-xs">Không tìm thấy đề bài nào</td>
          </tr>
        </tbody>
      </table>

      <!-- Footer / Pagination -->
      <div class="flex flex-wrap items-center justify-between gap-4 border-t-2 border-foreground bg-accent/10 px-6 py-3">
        <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Tổng số: {{ filteredPrompts.length }}</span>
        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          :total-items="filteredPrompts.length"
          :page-size="pageSize"
          :show-summary="false"
          item-label="đề bài"
          class="!bg-transparent !border-t-0 !shadow-none !p-0"
          @page-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Mic2Icon, PlusIcon, SearchIcon } from 'lucide-vue-next'
import speakingService from '@/services/speakingService'
import lessonService from '@/services/lessonService'
import api from '@/services/api'
import Pagination from '@/components/common/Pagination.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const prompts = ref([])
const lessons = ref([])
const loading = ref(true)
const error = ref(null)
const showForm = ref(false)
const editing = ref(null)
const saving = ref(false)
const generating = ref(false)

const searchQuery = ref('')
const levelFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const form = ref(defaultForm())

function defaultForm() {
  return {
    title: '', description: '', prompt: '', level: '', category: '', isPremium: false,
    thumbnailUrl: '', orderIndex: 0, isPublished: true, lessonId: null,
    mode: 'READ_ALOUD', referenceText: '', referenceMediaUrl: '', maxDurationSeconds: 60, attemptLimit: 10
  }
}

const filteredPrompts = computed(() => {
  return prompts.value.filter(p => {
    const matchSearch = !searchQuery.value || p.title?.toLowerCase().includes(searchQuery.value.toLowerCase()) || p.description?.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchLevel = !levelFilter.value || p.level === levelFilter.value
    return matchSearch && matchLevel
  })
})

const totalPages = computed(() => {
  return Math.ceil(filteredPrompts.value.length / pageSize.value) || 1
})

const paginatedPrompts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredPrompts.value.slice(start, start + pageSize.value)
})

function handlePageChange(page) {
  currentPage.value = page
}

onMounted(async () => {
  await Promise.all([loadPrompts(), loadLessons()])
})

async function loadPrompts() {
  loading.value = true
  error.value = null
  try {
    const pageData = await speakingService.getAdminPrompts(0, 100)
    prompts.value = pageData.content || []
  } catch (err) {
    error.value = err.response?.data?.message || 'Không thể tải danh sách đề bài'
  } finally {
    loading.value = false
  }
}

async function loadLessons() {
  try {
    const res = await lessonService.getAll()
    lessons.value = Array.isArray(res) ? res : (res.content || res.data || [])
  } catch (err) {
    console.error('Failed to load lessons:', err)
  }
}

function openPromptForm(p = null) {
  if (p) {
    editing.value = p.id
    form.value = { ...p, lessonId: p.lessonId || p.lesson?.id || null }
  } else {
    editing.value = null
    form.value = defaultForm()
  }
  showForm.value = true
}

function editPrompt(p) {
  openPromptForm(p)
}

async function savePrompt() {
  saving.value = true
  try {
    if (editing.value) {
      await speakingService.updatePrompt(editing.value, form.value)
    } else {
      await speakingService.createPrompt(form.value)
    }
    showForm.value = false
    await loadPrompts()
    toast.showSuccess(editing.value ? 'Đã cập nhật đề bài' : 'Đã tạo đề bài mới')
  } catch (err) {
    toast.showError(err.response?.data?.message || 'Lưu thất bại')
  } finally {
    saving.value = false
  }
}

async function deletePrompt(id) {
  if (!confirm('Bạn có chắc muốn xóa đề bài này?')) return
  try {
    await speakingService.deletePrompt(id)
    await loadPrompts()
    toast.showSuccess('Đã xóa đề bài')
  } catch (err) {
    toast.showError(err.response?.data?.message || 'Xóa thất bại')
  }
}

async function aiGenerate() {
  generating.value = true
  try {
    const res = await api.post('/api/v1/admin/speaking-prompts/ai-generate', {
      topic: form.value.title || form.value.description || 'General English',
      level: form.value.level || 'A2'
    })
    if (res.data?.referenceText) {
      form.value.referenceText = res.data.referenceText
      if (!form.value.prompt) form.value.prompt = `Hãy nói về chủ đề: ${res.data.topic || form.value.title}`
    }
  } catch (err) {
    toast.showError(err.response?.data?.message || 'Tạo AI thất bại. Hãy kiểm tra dịch vụ AI.')
  } finally {
    generating.value = false
  }
}
</script>
