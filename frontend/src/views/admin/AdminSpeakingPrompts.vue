<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-accent border-2 border-foreground flex items-center justify-center rotate-6 rounded-md">
          <Mic2Icon class="w-5 h-5 text-white" aria-hidden="true" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Đề Bài Speaking</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-muted-foreground">Quản lý đề bài & mẫu câu đọc nói</p>
        </div>
      </div>
      <AppButton @click="openPromptForm(null)" variant="primary">
        <PlusIcon class="w-4 h-4" aria-hidden="true" />
        Thêm đề bài
      </AppButton>
    </div>

    <!-- Actions & Filters -->
    <div class="flex flex-wrap items-center gap-3">
      <div class="relative flex-1 min-w-[200px] sm:max-w-xs">
        <SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" aria-hidden="true" />
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
      <button
        v-if="hasActiveFilters"
        type="button"
        class="text-xs font-bold uppercase tracking-wider text-muted-foreground underline underline-offset-4 hover:text-foreground transition-colors"
        @click="clearFilters"
      >
        Xóa bộ lọc
      </button>
    </div>

    <!-- Form modal -->
    <AppModal v-model="showForm" :title="editing ? 'Sửa đề bài' : 'Thêm đề bài mới'" size="lg">
      <form id="speaking-prompt-form" class="space-y-4" @submit.prevent="savePrompt">
        <div class="grid md:grid-cols-2 gap-4">
          <div>
            <label for="prompt-title" class="app-form-field__label">Tiêu đề <span class="app-form-field__required">*</span></label>
            <input
              id="prompt-title"
              v-model="form.title"
              :class="['app-input', { 'app-input--invalid': formErrors.title }]"
              @input="clearError('title')"
            />
            <p v-if="formErrors.title" class="app-form-field__error" role="alert">{{ formErrors.title }}</p>
          </div>
          <div>
            <label for="prompt-level" class="app-form-field__label">Level</label>
            <select id="prompt-level" v-model="form.level" class="app-input">
              <option value="">Chưa chọn cấp độ</option>
              <option value="A1">A1 - Beginner</option>
              <option value="A2">A2 - Elementary</option>
              <option value="B1">B1 - Intermediate</option>
              <option value="B2">B2 - Upper Intermediate</option>
              <option value="C1">C1 - Advanced</option>
            </select>
          </div>
          <div class="md:col-span-2">
            <label for="prompt-desc" class="app-form-field__label">Mô tả ngắn</label>
            <textarea id="prompt-desc" v-model="form.description" rows="2" class="app-input"></textarea>
          </div>
          <div>
            <label for="prompt-mode" class="app-form-field__label">Chế độ luyện tập</label>
            <select id="prompt-mode" v-model="form.mode" class="app-input">
              <option value="READ_ALOUD">Đọc theo mẫu</option>
              <option value="FREE_SPEAKING">Nói tự do</option>
            </select>
          </div>
          <div class="flex items-end">
            <AppButton
              type="button"
              class="w-full"
              :loading="generatingFull"
              @click="aiGenerateFull"
              variant="primary"
            >
              <SparklesIcon class="w-4 h-4" aria-hidden="true" />
              {{ generatingFull ? 'AI đang tạo đề...' : 'Tạo đề bằng AI' }}
            </AppButton>
          </div>
          <p class="md:col-span-2 text-xs font-medium text-muted-foreground -mt-2">
            Nhập chủ đề vào ô "Tiêu đề" (ví dụ: Ordering coffee, Job interview...) rồi bấm "Tạo đề bằng AI" — hệ thống tự điền mô tả, hướng dẫn và đoạn đọc mẫu theo trình độ.
          </p>
          <div class="md:col-span-2">
            <label for="prompt-body" class="app-form-field__label">Nội dung câu hỏi / Đề bài <span class="app-form-field__required">*</span></label>
            <textarea
              id="prompt-body"
              v-model="form.prompt"
              rows="3"
              :class="['app-input', { 'app-input--invalid': formErrors.prompt }]"
              @input="clearError('prompt')"
            ></textarea>
            <p v-if="formErrors.prompt" class="app-form-field__error" role="alert">{{ formErrors.prompt }}</p>
          </div>
          <div class="md:col-span-2">
            <label for="prompt-reference" class="app-form-field__label">Nội dung đọc mẫu (referenceText)<span v-if="form.mode === 'READ_ALOUD'" class="app-form-field__required">*</span></label>
            <textarea
              id="prompt-reference"
              v-model="form.referenceText"
              rows="3"
              :class="['app-input', { 'app-input--invalid': formErrors.referenceText }]"
              @input="clearError('referenceText')"
            ></textarea>
            <p v-if="formErrors.referenceText" class="app-form-field__error" role="alert">{{ formErrors.referenceText }}</p>
          </div>
          <div>
            <label for="prompt-duration" class="app-form-field__label">Thời lượng tối đa (giây)</label>
            <input id="prompt-duration" v-model.number="form.maxDurationSeconds" type="number" min="15" max="300" class="app-input" />
          </div>
          <div>
            <label for="prompt-attempts" class="app-form-field__label">Số lượt tối đa</label>
            <input id="prompt-attempts" v-model.number="form.attemptLimit" type="number" min="1" max="100" class="app-input" />
          </div>
        </div>
      </form>
      <template #footer>
        <AppButton variant="secondary" @click="closeForm">Hủy</AppButton>
        <AppButton type="submit" form="speaking-prompt-form" :loading="saving" variant="tertiary">Lưu đề bài</AppButton>
      </template>
    </AppModal>

    <!-- Loading skeleton -->
    <div v-if="loading" class="bg-white border-2 border-foreground overflow-hidden rounded-md shadow-pop-lg" role="status" aria-label="Đang tải danh sách đề bài">
      <div class="bg-foreground px-6 py-3">
        <div class="app-skeleton h-3 w-24 rounded"></div>
      </div>
      <div class="divide-y-2 divide-foreground">
        <div v-for="i in 5" :key="i" class="flex items-center gap-4 p-4">
          <div class="app-skeleton h-4 flex-1 rounded"></div>
          <div class="app-skeleton h-4 w-12 rounded"></div>
          <div class="app-skeleton h-6 w-20 rounded-md"></div>
          <div class="app-skeleton h-4 w-16 rounded"></div>
          <div class="app-skeleton h-8 w-28 rounded-full"></div>
        </div>
      </div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="app-state app-state--error" role="alert">
      <AlertTriangleIcon class="w-8 h-8 text-danger" aria-hidden="true" />
      <h3 class="app-state__title">Không tải được danh sách đề bài</h3>
      <p class="app-state__desc">{{ error }}</p>
      <div class="app-state__action">
        <AppButton variant="secondary" size="sm" @click="loadPrompts()">Thử lại</AppButton>
      </div>
    </div>

    <!-- Table -->
    <div v-else class="bg-white border-2 border-foreground overflow-hidden rounded-md shadow-pop-lg">
      <div class="bg-foreground border-b-2 border-foreground px-6 py-3 flex items-center gap-3">
        <div class="w-2 h-2 bg-tertiary rotate-45 rounded"></div>
        <span class="font-bold text-xs uppercase tracking-widest text-white/80">{{ filteredPrompts.length }} đề bài</span>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-sm min-w-[640px]">
          <thead class="bg-accent/20 border-b-2 border-foreground font-black uppercase text-xs tracking-wider">
            <tr>
              <th class="text-left p-4 border-r-2 border-foreground">Tiêu đề</th>
              <th class="text-left p-4 border-r-2 border-foreground">Level</th>
              <th class="text-left p-4 border-r-2 border-foreground">Chế độ</th>
              <th class="text-center p-4">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in paginatedPrompts" :key="p.id" class="border-b-2 border-foreground hover:bg-accent/5 transition-colors duration-150">
              <td class="p-4 border-r-2 border-foreground">
                <span class="font-bold">{{ p.title }}</span>
                <span v-if="p.description" class="block text-xs text-muted-foreground font-medium mt-0.5 line-clamp-1">{{ p.description }}</span>
              </td>
              <td class="p-4 border-r-2 border-foreground font-bold tabular-nums">{{ p.level || '-' }}</td>
              <td class="p-4 border-r-2 border-foreground">
                <span class="inline-block px-2 py-1 text-xs font-bold rounded-md border border-foreground" :class="p.mode === 'FREE_SPEAKING' ? 'bg-tertiary/30' : 'bg-secondary/30'">
                  {{ p.mode === 'FREE_SPEAKING' ? 'Nói tự do' : 'Đọc mẫu' }}
                </span>
              </td>
              <td class="p-4 text-center whitespace-nowrap">
                <AppButton @click="editPrompt(p)" variant="secondary" size="sm" class="mr-2">Sửa</AppButton>
                <AppButton @click="askDelete(p)" variant="danger" size="sm">Xóa</AppButton>
              </td>
            </tr>
            <tr v-if="filteredPrompts.length === 0">
              <td colspan="4" class="p-6">
                <AppEmptyState
                  :title="hasActiveFilters ? 'Không tìm thấy đề bài phù hợp' : 'Chưa có đề bài nào'"
                  :description="hasActiveFilters ? 'Thử đổi từ khóa hoặc bỏ bớt bộ lọc.' : 'Tạo đề bài đầu tiên để học viên bắt đầu luyện nói.'"
                >
                  <template #visual>
                    <FileTextIcon class="w-10 h-10 text-muted-foreground" aria-hidden="true" />
                  </template>
                  <template #action>
                    <AppButton v-if="hasActiveFilters" variant="secondary" size="sm" @click="clearFilters">Xóa bộ lọc</AppButton>
                    <AppButton v-else variant="primary" size="sm" @click="openPromptForm(null)">
                      <PlusIcon class="w-4 h-4" aria-hidden="true" />
                      Thêm đề bài
                    </AppButton>
                  </template>
                </AppEmptyState>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

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

    <!-- Delete confirmation -->
    <AppModal v-model="confirmDelete" title="Xóa đề bài" size="sm">
      <div class="flex items-start gap-3">
        <div class="w-10 h-10 shrink-0 bg-danger/10 border-2 border-danger rounded-md flex items-center justify-center">
          <AlertTriangleIcon class="w-5 h-5 text-danger" aria-hidden="true" />
        </div>
        <p class="text-sm font-medium leading-relaxed">
          Bạn có chắc muốn xóa đề bài <strong class="font-black">{{ promptToDelete?.title }}</strong>?
          Hành động này không thể hoàn tác.
        </p>
      </div>
      <template #footer>
        <AppButton variant="secondary" @click="confirmDelete = false">Hủy</AppButton>
        <AppButton variant="danger" :loading="deleting" @click="confirmDeletePrompt">Xóa đề bài</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Mic2Icon, PlusIcon, SearchIcon, AlertTriangleIcon, FileTextIcon, SparklesIcon } from 'lucide-vue-next'
import speakingService from '@/services/speakingService'
import api from '@/services/api'
import Pagination from '@/components/common/Pagination.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppEmptyState from '@/components/ui/AppEmptyState.vue'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const prompts = ref([])
const loading = ref(true)
const error = ref(null)
const showForm = ref(false)
const editing = ref(null)
const saving = ref(false)
const generatingFull = ref(false)

const confirmDelete = ref(false)
const promptToDelete = ref(null)
const deleting = ref(false)

const searchQuery = ref('')
const levelFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const form = ref(defaultForm())
const formErrors = ref({})

function defaultForm() {
  return {
    title: '', description: '', prompt: '', level: '',
    mode: 'READ_ALOUD', referenceText: '', maxDurationSeconds: 60, attemptLimit: 10
  }
}

const hasActiveFilters = computed(() => Boolean(searchQuery.value.trim() || levelFilter.value))

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

function clearFilters() {
  searchQuery.value = ''
  levelFilter.value = ''
  currentPage.value = 1
}

onMounted(loadPrompts)

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

function openPromptForm(p = null) {
  if (p) {
    editing.value = p.id
    // Only keep fields the trimmed form still shows.
    form.value = {
      title: p.title || '',
      description: p.description || '',
      prompt: p.prompt || '',
      level: p.level || '',
      mode: p.mode || 'READ_ALOUD',
      referenceText: p.referenceText || '',
      maxDurationSeconds: p.maxDurationSeconds || 60,
      attemptLimit: p.attemptLimit || 10
    }
  } else {
    editing.value = null
    form.value = defaultForm()
  }
  formErrors.value = {}
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editing.value = null
  formErrors.value = {}
}

function editPrompt(p) {
  openPromptForm(p)
}

function clearError(field) {
  if (formErrors.value[field]) {
    const next = { ...formErrors.value }
    delete next[field]
    formErrors.value = next
  }
}

function validateForm() {
  const errors = {}
  if (!form.value.title?.trim()) errors.title = 'Tiêu đề không được để trống'
  if (!form.value.prompt?.trim()) errors.prompt = 'Nội dung đề bài không được để trống'
  if (form.value.mode === 'READ_ALOUD' && !form.value.referenceText?.trim()) {
    errors.referenceText = 'Chế độ đọc mẫu cần có nội dung đọc mẫu'
  }
  formErrors.value = errors
  return Object.keys(errors).length === 0
}

async function savePrompt() {
  if (!validateForm()) return
  const isEdit = Boolean(editing.value)
  saving.value = true
  try {
    if (isEdit) {
      await speakingService.updatePrompt(editing.value, form.value)
    } else {
      await speakingService.createPrompt(form.value)
    }
    closeForm()
    await loadPrompts()
    toast.showSuccess(isEdit ? 'Đã cập nhật đề bài' : 'Đã tạo đề bài mới')
  } catch (err) {
    toast.showError(err.response?.data?.message || 'Lưu thất bại')
  } finally {
    saving.value = false
  }
}

function askDelete(p) {
  promptToDelete.value = p
  confirmDelete.value = true
}

async function confirmDeletePrompt() {
  const target = promptToDelete.value
  if (!target) return
  deleting.value = true
  try {
    await speakingService.deletePrompt(target.id)
    confirmDelete.value = false
    promptToDelete.value = null
    await loadPrompts()
    toast.showSuccess('Đã xóa đề bài')
  } catch (err) {
    toast.showError(err.response?.data?.message || 'Xóa thất bại')
  } finally {
    deleting.value = false
  }
}

/**
 * Full AI draft: uses the title box as the topic and fills description,
 * prompt, referenceText and level from one LLM call. Existing values are
 * kept — AI only fills blanks.
 */
async function aiGenerateFull() {
  const topic = form.value.title?.trim()
  if (!topic) {
    toast.showError('Nhập chủ đề vào ô Tiêu đề trước khi tạo bằng AI')
    return
  }
  if (generatingFull.value) return
  generatingFull.value = true
  try {
    // audit-v6 F23: local LLM can take 30-60s+ (model swap + generation);
    // the default 10s axios timeout aborted the request mid-call.
    const res = await api.post('/api/v1/admin/speaking-prompts/ai-generate-full', {
      topic,
      level: form.value.level || 'A2',
      mode: form.value.mode
    }, { timeout: 120000 })
    const draft = res.data || {}
    if (!form.value.title) form.value.title = draft.title || topic
    if (!form.value.description && draft.description) form.value.description = draft.description
    if (!form.value.prompt && draft.prompt) form.value.prompt = draft.prompt
    if (!form.value.referenceText && draft.referenceText) {
      form.value.referenceText = draft.referenceText
      clearError('referenceText')
    }
    if (!form.value.level && draft.level) form.value.level = draft.level
    toast.showSuccess('AI đã điền đề bài — kiểm tra lại trước khi lưu')
  } catch (err) {
    toast.showError(err.response?.data?.error || err.response?.data?.message || 'Tạo đề bằng AI thất bại')
  } finally {
    generatingFull.value = false
  }
}
</script>
