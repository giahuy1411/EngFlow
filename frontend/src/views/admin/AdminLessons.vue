<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-accent border-2 border-foreground flex items-center justify-center rotate-6 rounded-md">
          <BookOpenIcon class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Bài học</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-muted-foreground">Quản lý danh sách bài học</p>
        </div>
      </div>
      <AppButton variant="primary" @click="openModal()">
        <PlusIcon class="w-4 h-4" />
        Thêm Bài Học
      </AppButton>
    </div>

    <!-- Search + Level filter bar -->
    <div class="flex flex-wrap items-center gap-3">
      <div class="relative flex-1 min-w-[200px]">
        <SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <input id="lesson-search" name="lesson-search" v-model="searchQuery" type="search" placeholder="Tìm tiêu đề, mô tả..."
               class="w-full border-2 border-foreground pl-10 pr-3 py-2.5 bg-white font-bold text-sm rounded-md
                      focus:outline-none focus:ring-2 focus:ring-accent transition-all shadow-pop-sm" />
      </div>
      <select id="lesson-level" name="lesson-level" v-model="levelFilter"
              class="border-2 border-foreground px-3 py-2.5 bg-white font-bold uppercase text-xs rounded-md
                     focus:outline-none focus:ring-2 focus:ring-accent transition-all appearance-none shadow-pop-sm">
        <option value="">Tất cả cấp độ</option>
        <option v-for="lv in levels" :key="lv" :value="lv">{{ displayLevel(lv) }}</option>
      </select>
    </div>

    <div class="border-2 border-foreground bg-white shadow-pop-lg rounded-md overflow-hidden" style="border-top:0">
      <div class="bg-foreground border-b-2 border-foreground px-6 py-3 flex items-center gap-3">
        <div class="w-2 h-2 bg-tertiary rotate-45 rounded"></div>
        <span class="font-bold text-xs uppercase tracking-widest text-white/60">{{ totalElements }} bài học</span>
      </div>

      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-accent/20 border-b-2 border-foreground font-black uppercase text-xs tracking-wider">
            <th class="p-4 border-r-2 border-foreground">Tiêu đề</th>
            <th class="p-4 border-r-2 border-foreground">Mô tả</th>
            <th class="p-4 border-r-2 border-foreground">Nội dung</th>
            <th class="p-4 border-r-2 border-foreground text-center w-28">Cấp độ</th>
            <th class="p-4 border-r-2 border-foreground text-center w-28">Xuất bản</th>
            <th class="p-4 text-center w-28">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading" class="border-b-2 border-foreground">
            <td colspan="6" class="p-12 text-center">
              <div class="flex items-center justify-center gap-3">
                <div class="w-5 h-5 border-2 border-foreground border-t-tertiary rounded-full animate-spin"></div>
                <span class="font-bold uppercase text-sm tracking-wider">Đang tải...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="lessons.length === 0" class="border-b-2 border-foreground">
            <td colspan="6" class="p-12 text-center font-bold uppercase text-sm tracking-wider text-muted-foreground">Chưa có bài học</td>
          </tr>
          <tr v-for="lesson in lessons" :key="lesson.id"
              class="border-b-2 border-foreground hover:bg-accent/5 transition-colors duration-150">
            <td class="p-4 border-r-2 border-foreground font-bold">{{ lesson.title }}</td>
            <td class="p-4 border-r-2 border-foreground text-sm text-muted-foreground truncate max-w-xs">{{ lesson.description || '-' }}</td>
            <td class="p-4 border-r-2 border-foreground text-sm text-muted-foreground max-w-xs">
              <div v-if="lesson.description" class="truncate">{{ lesson.description.substring(0, 120) }}...</div>
              <span v-else class="text-muted-foreground/60 italic">Trống</span>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span class="inline-block px-2 py-1 border-2 border-foreground text-xs font-black uppercase tracking-wider"
                    :class="levelBadge(lesson.level)">{{ displayLevel(lesson.level) }}</span>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span :class="['inline-block px-3 py-1 border-2 border-foreground text-xs font-black uppercase tracking-wider rounded-md shadow-pop-sm',
                            lesson.isPublished ? 'bg-quaternary text-foreground' : 'bg-foreground/10 text-foreground/70']">
                {{ lesson.isPublished ? 'Có' : 'Không' }}
              </span>
            </td>
             <td class="p-4 text-center">
                <div class="flex items-center justify-center gap-2">
                   <!-- icon-only control: kept raw -->
                   <button @click="$router.push('/admin/' + lesson.id + '/build')" aria-label="Xây dựng bài học"
                           class="w-9 h-9 flex items-center justify-center bg-tertiary text-foreground border-2 border-foreground rounded-md shadow-pop-sm
                                  hover:-translate-y-0.5 hover:shadow-pop transition-all duration-200
                                  active:translate-x-0.5 active:translate-y-0.5 active:shadow-none"
                           title="Xây dựng nội dung">
                     <LayersIcon class="w-4 h-4" />
                  </button>
                  <!-- icon-only control: kept raw -->
                  <button @click="openModal(lesson)" aria-label="Sửa bài học"
                           class="w-9 h-9 flex items-center justify-center bg-secondary text-foreground border-2 border-foreground rounded-md shadow-pop-sm
                                  hover:-translate-y-0.5 hover:shadow-pop transition-all duration-200
                                  active:translate-x-0.5 active:translate-y-0.5 active:shadow-none">
                     <EditIcon class="w-4 h-4" />
                  </button>
                  <!-- icon-only control: kept raw -->
                  <button @click="deleteLesson(lesson.id)" aria-label="Xóa bài học"
                           class="w-9 h-9 flex items-center justify-center bg-accent text-white border-2 border-foreground rounded-md shadow-pop-sm
                                  hover:-translate-y-0.5 hover:shadow-pop transition-all duration-200
                                  active:translate-x-0.5 active:translate-y-0.5 active:shadow-none">
                     <TrashIcon class="w-4 h-4" />
                  </button>
                </div>
             </td>
          </tr>
        </tbody>
      </table>

      <div class="flex flex-wrap items-center justify-between gap-4 border-t-2 border-foreground bg-accent/10 px-6 py-3">
        <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Tổng số: {{ totalElements }}</span>
        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          :total-items="totalElements"
          :page-size="pageSize"
          :show-summary="false"
          item-label="bài học"
          class="!bg-transparent !border-0 !shadow-none !p-0"
          @page-change="changePage"
        />
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-foreground/60 backdrop-blur-sm p-4">
      <div class="bg-white border-2 border-foreground w-full max-w-lg shadow-pop-xl rounded-md flex flex-col max-h-[90vh] overflow-hidden">
        <div class="bg-tertiary border-b-2 border-foreground p-5 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-3 h-3 bg-foreground rotate-45 rounded"></div>
            <h3 class="font-black text-xl uppercase tracking-tighter text-foreground">
              {{ editingLesson ? 'Sửa Bài Học' : 'Thêm Bài Học Mới' }}
            </h3>
          </div>
          <!-- icon-only control: kept raw -->
          <button @click="closeModal"
                  class="w-8 h-8 flex items-center justify-center border-2 border-foreground bg-white text-foreground font-black text-lg rounded-md shadow-pop-sm
                         hover:bg-accent hover:text-white transition-colors">&times;</button>
        </div>

        <div class="p-6 overflow-y-auto">
          <form @submit.prevent="saveLesson" class="space-y-5">
            <div>
              <label for="form-lesson-title" class="block font-bold uppercase text-xs tracking-wider mb-1.5">Tiêu đề *</label>
              <input id="form-lesson-title" name="form-lesson-title" v-model="formData.title" type="text" required
                     class="w-full border-2 border-foreground rounded-md p-3 bg-background font-bold focus:outline-none focus:ring-2 focus:ring-accent focus:bg-white transition-all" />
            </div>
            <div>
              <label for="form-lesson-desc" class="block font-bold uppercase text-xs tracking-wider mb-1.5">Mô tả</label>
              <textarea id="form-lesson-desc" name="form-lesson-desc" v-model="formData.description" rows="3"
                        class="w-full border-2 border-foreground rounded-md p-3 bg-background focus:outline-none focus:ring-2 focus:ring-accent focus:bg-white transition-all"></textarea>
            </div>
            
            <div>
              <label for="form-lesson-content" class="block font-bold uppercase text-xs tracking-wider mb-1.5">
                Nội dung HTML (Legacy Content)
                <span class="text-[10px] lowercase text-muted-foreground font-medium ml-2">(Chỉ dùng cho bài học cũ/clone)</span>
              </label>
              <textarea id="form-lesson-content" name="form-lesson-content" v-model="formData.content" rows="6" placeholder="<p>Nhập mã HTML tại đây...</p>"
                        class="w-full border-2 border-foreground rounded-md p-3 bg-background focus:outline-none focus:ring-2 focus:ring-accent focus:bg-white transition-all font-mono text-sm"></textarea>
            </div>

            <div>
              <label for="form-lesson-level" class="block font-bold uppercase text-xs tracking-wider mb-1.5">Cấp độ *</label>
              <select id="form-lesson-level" name="form-lesson-level" v-model="formData.level" required
                      class="w-full border-2 border-foreground rounded-md p-3 bg-background font-bold uppercase text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:bg-white transition-all appearance-none">
                <option value="ELEMENTARY">Elementary</option>
                <option value="PRE_INTERMEDIATE">Pre-Intermediate</option>
                <option value="INTERMEDIATE">Intermediate</option>
                <option value="UPPER_INTERMEDIATE">Upper-Intermediate</option>
              </select>
            </div>

            <div class="flex items-center justify-between py-2">
              <label class="font-bold uppercase text-xs tracking-wider cursor-pointer">Xuất bản</label>
              <!-- icon-only control: kept raw (toggle switch) -->
              <button type="button" @click="formData.isPublished = !formData.isPublished"
                      class="relative w-12 h-7 border-2 border-foreground rounded-full transition-colors duration-200"
                      :class="formData.isPublished ? 'bg-quaternary' : 'bg-foreground/10'">
                <div :class="['absolute top-0.5 w-5 h-5 bg-white border-2 border-foreground rounded-full transition-transform duration-200',
                              formData.isPublished ? 'translate-x-5 left-0.5' : 'translate-x-0.5 left-0']"></div>
              </button>
            </div>

            <div class="flex justify-end gap-4 pt-6 border-t-2 border-foreground">
              <AppButton variant="secondary" type="button" @click="closeModal">
                Hủy
              </AppButton>
              <AppButton variant="emerald" type="submit" :disabled="saving">
                {{ saving ? 'Đang lưu...' : 'Lưu lại' }}
              </AppButton>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { adminService } from '@/services/adminService'
import Pagination from '@/components/common/Pagination.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { useToast } from '@/composables/useToast'
import { BookOpen as BookOpenIcon, Plus as PlusIcon, Edit2 as EditIcon, Trash2 as TrashIcon, Layers as LayersIcon, Search as SearchIcon } from 'lucide-vue-next'

const lessons = ref([])
const loading = ref(true)
const saving = ref(false)
const showModal = ref(false)
const editingLesson = ref(null)
const levelFilter = ref('')
const searchQuery = ref('')
const searchTimer = ref(null)
const currentPage = ref(1)
const pageSize = 20
const totalPages = ref(1)
const totalElements = ref(0)
const toast = useToast()

const levels = ['ELEMENTARY', 'PRE_INTERMEDIATE', 'INTERMEDIATE', 'UPPER_INTERMEDIATE']

const displayLevel = (lv) => ({
  ELEMENTARY: 'Elementary',
  PRE_INTERMEDIATE: 'Pre-Intermediate',
  INTERMEDIATE: 'Intermediate',
  UPPER_INTERMEDIATE: 'Upper-Intermediate'
}[lv] || lv)

const levelBadge = (lv) => ({
  ELEMENTARY: 'bg-secondary text-foreground rounded-md border-2 border-foreground',
  PRE_INTERMEDIATE: 'bg-tertiary text-foreground rounded-md border-2 border-foreground',
  INTERMEDIATE: 'bg-accent text-white rounded-md border-2 border-foreground',
  UPPER_INTERMEDIATE: 'bg-secondary text-white rounded-md border-2 border-foreground'
}[lv] || 'bg-foreground/10 border-2 border-foreground')

const initialForm = { title: '', description: '', content: '', level: 'ELEMENTARY', isPublished: false }
const formData = ref({ ...initialForm })

const fetchLessons = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchQuery.value.trim()) params.q = searchQuery.value.trim()
    if (levelFilter.value) params.level = levelFilter.value
    params.page = currentPage.value - 1
    params.size = pageSize
    const data = await adminService.getAllLessons(params)
    lessons.value = Array.isArray(data) ? data : data.content || []
    totalPages.value = data.totalPages || 1
    totalElements.value = data.totalElements || lessons.value.length
  }
  catch { toast.showError('Không thể tải danh sách bài học') }
  finally { loading.value = false }
}

function changePage(p) {
  currentPage.value = p
  fetchLessons()
}

watch(levelFilter, () => {
  currentPage.value = 1
  fetchLessons()
})

const onSearchInput = () => {
  clearTimeout(searchTimer.value)
  searchTimer.value = setTimeout(() => {
    currentPage.value = 1
    fetchLessons()
  }, 300)
}

const openModal = async (lesson = null) => {
  if (lesson) {
    try {
      const fullLesson = await adminService.getLesson(lesson.id)
      editingLesson.value = fullLesson
      formData.value = {
        title: fullLesson.title,
        description: fullLesson.description || '',
        content: fullLesson.content || '',
        level: fullLesson.level || 'ELEMENTARY',
        isPublished: fullLesson.isPublished ?? false
      }
    } catch {
      editingLesson.value = lesson
      formData.value = {
        title: lesson.title,
        description: lesson.description || '',
        content: lesson.content || '',
        level: lesson.level || 'ELEMENTARY',
        isPublished: lesson.isPublished ?? false
      }
    }
  } else {
    editingLesson.value = null
    formData.value = { ...initialForm }
  }
  showModal.value = true
}

const closeModal = () => { showModal.value = false; editingLesson.value = null }

const saveLesson = async () => {
  saving.value = true
  try {
    if (editingLesson.value) {
      await adminService.updateLesson(editingLesson.value.id, formData.value)
      toast.showSuccess('Đã cập nhật bài học')
    } else {
      await adminService.createLesson(formData.value)
      toast.showSuccess('Đã tạo bài học mới')
    }
    closeModal(); fetchLessons()
  } catch { toast.showError('Lỗi lưu bài học') }
  finally { saving.value = false }
}

const deleteLesson = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa bài học này?')) return
  try { await adminService.deleteLesson(id); toast.showSuccess('Đã xóa bài học'); fetchLessons() }
  catch { toast.showError('Lỗi xóa bài học') }
}

onMounted(() => fetchLessons())
</script>
