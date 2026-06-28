<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-primary-yellow border-4 border-foreground flex items-center justify-center">
          <BookOpenIcon class="w-5 h-5 text-foreground" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Bài học</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-gray-500">Quản lý danh sách bài học</p>
        </div>
      </div>
      <button @click="openModal()"
              class="flex items-center gap-2 bg-primary-yellow border-4 border-foreground px-5 py-3 font-bold uppercase text-sm tracking-wider text-foreground
                     shadow-[4px_4px_0px_0px_black] hover:-translate-y-0.5 hover:shadow-[6px_6px_0px_0px_black]
                     active:translate-x-1 active:translate-y-1 active:shadow-none transition-all duration-200">
        <PlusIcon class="w-4 h-4" />
        Thêm Bài Học
      </button>
    </div>

    <!-- Level filter -->
    <div class="flex flex-wrap gap-2 px-6 py-3 bg-white border-4 border-foreground border-b-0 shadow-[8px_8px_0px_0px_black]">
      <button @click="levelFilter = ''"
              class="px-3 py-1 border-2 border-foreground font-bold uppercase text-xs tracking-wider transition-all"
              :class="levelFilter === '' ? 'bg-foreground text-white' : 'bg-white hover:bg-gray-100'">
        Tất cả
      </button>
      <button v-for="lv in levels" :key="lv"
              @click="levelFilter = lv"
              class="px-3 py-1 border-2 border-foreground font-bold uppercase text-xs tracking-wider transition-all"
              :class="levelFilter === lv ? 'bg-foreground text-white' : 'bg-white hover:bg-gray-100'">
        {{ displayLevel(lv) }}
      </button>
    </div>

    <div class="border-4 border-foreground bg-white shadow-[8px_8px_0px_0px_black] overflow-hidden" style="border-top:0">
      <div class="bg-foreground border-b-4 border-foreground px-6 py-3 flex items-center gap-3">
        <div class="w-2 h-2 bg-primary-yellow rotate-45"></div>
        <span class="font-bold text-xs uppercase tracking-widest text-white/60">{{ filteredLessons.length }} bài học</span>
      </div>

      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-gray-100 border-b-4 border-foreground font-bold uppercase text-xs tracking-wider">
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
                <div class="w-5 h-5 border-2 border-foreground border-t-primary-yellow rounded-full animate-spin"></div>
                <span class="font-bold uppercase text-sm tracking-wider">Đang tải...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="filteredLessons.length === 0" class="border-b-2 border-foreground">
            <td colspan="6" class="p-12 text-center font-bold uppercase text-sm tracking-wider text-gray-400">Chưa có bài học</td>
          </tr>
          <tr v-for="lesson in filteredLessons" :key="lesson.id"
              class="border-b-2 border-foreground hover:bg-gray-50 transition-colors duration-150">
            <td class="p-4 border-r-2 border-foreground font-bold">{{ lesson.title }}</td>
            <td class="p-4 border-r-2 border-foreground text-sm text-gray-500 truncate max-w-xs">{{ lesson.description || '—' }}</td>
            <td class="p-4 border-r-2 border-foreground text-sm text-gray-500 max-w-xs">
              <div v-if="lesson.content" class="truncate">{{ lesson.content.replace(/<[^>]*>/g, '').substring(0, 120) }}...</div>
              <span v-else class="text-gray-300 italic">Trống</span>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span class="inline-block px-2 py-1 border-2 border-foreground text-xs font-bold uppercase tracking-wider"
                    :class="levelBadge(lesson.level)">{{ displayLevel(lesson.level) }}</span>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span :class="['inline-block px-3 py-1 border-2 border-foreground text-xs font-bold uppercase tracking-wider',
                            lesson.isPublished ? 'bg-primary-blue text-white' : 'bg-foreground/10 text-foreground/70']">
                {{ lesson.isPublished ? 'Có' : 'Không' }}
              </span>
            </td>
             <td class="p-4 text-center">
               <div class="flex items-center justify-center gap-2">
                  <button @click="$router.push('/admin/' + lesson.id + '/build')" aria-label="Xây dựng bài học"
                          class="w-9 h-9 flex items-center justify-center bg-primary-yellow text-foreground border-2 border-foreground
                                 hover:-translate-y-0.5 hover:shadow-[3px_3px_0px_0px_black] transition-all duration-200
                                 active:translate-x-0.5 active:translate-y-0.5 active:shadow-none"
                          title="Xây dựng nội dung">
                    <LayersIcon class="w-4 h-4" />
                 </button>
                 <button @click="openModal(lesson)" aria-label="Sửa bài học"
                          class="w-9 h-9 flex items-center justify-center bg-primary-blue text-white border-2 border-foreground
                                 hover:-translate-y-0.5 hover:shadow-[3px_3px_0px_0px_black] transition-all duration-200
                                 active:translate-x-0.5 active:translate-y-0.5 active:shadow-none">
                    <EditIcon class="w-4 h-4" />
                 </button>
                 <button @click="deleteLesson(lesson.id)" aria-label="Xóa bài học"
                          class="w-9 h-9 flex items-center justify-center bg-primary-red text-white border-2 border-foreground
                                 hover:-translate-y-0.5 hover:shadow-[3px_3px_0px_0px_black] transition-all duration-200
                                 active:translate-x-0.5 active:translate-y-0.5 active:shadow-none">
                    <TrashIcon class="w-4 h-4" />
                 </button>
               </div>
             </td>
          </tr>
        </tbody>
      </table>

      <div class="bg-gray-100 border-t-4 border-foreground px-6 py-3 flex justify-between items-center">
        <span class="font-bold text-xs uppercase tracking-wider text-gray-500">Tổng số: {{ lessons.length }}</span>
        <div class="flex gap-1">
          <div class="w-2 h-2 bg-primary-yellow rotate-45"></div>
          <div class="w-2 h-2 rounded-full bg-primary-red"></div>
          <div class="w-2 h-2 bg-primary-blue"></div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
      <div class="bg-white border-4 border-foreground w-full max-w-lg shadow-[10px_10px_0px_0px_black] flex flex-col max-h-[90vh]">
        <div class="bg-primary-yellow border-b-4 border-foreground p-5 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-3 h-3 bg-foreground rotate-45"></div>
            <h3 class="font-black text-xl uppercase tracking-tighter text-foreground">
              {{ editingLesson ? 'Sửa Bài Học' : 'Thêm Bài Học Mới' }}
            </h3>
          </div>
          <button @click="closeModal"
                  class="w-8 h-8 flex items-center justify-center border-2 border-foreground bg-foreground/10 text-foreground font-black text-lg
                         hover:bg-foreground hover:text-white transition-colors">&times;</button>
        </div>

        <div class="p-6 overflow-y-auto">
          <form @submit.prevent="saveLesson" class="space-y-5">
            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Tiêu đề *</label>
              <input v-model="formData.title" type="text" required
                     class="w-full border-2 border-foreground p-3 bg-background font-bold focus:outline-none focus:ring-2 focus:ring-primary-blue focus:bg-white transition-all" />
            </div>
            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Mô tả</label>
              <textarea v-model="formData.description" rows="3"
                        class="w-full border-2 border-foreground p-3 bg-background focus:outline-none focus:ring-2 focus:ring-primary-blue focus:bg-white transition-all"></textarea>
            </div>
            
            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">
                Nội dung HTML (Legacy Content)
                <span class="text-[10px] lowercase text-gray-500 font-medium ml-2">(Chỉ dùng cho bài học cũ/clone)</span>
              </label>
              <textarea v-model="formData.content" rows="6" placeholder="<p>Nhập mã HTML tại đây...</p>"
                        class="w-full border-2 border-foreground p-3 bg-background focus:outline-none focus:ring-2 focus:ring-primary-blue focus:bg-white transition-all font-mono text-sm"></textarea>
            </div>

            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Cấp độ *</label>
              <select v-model="formData.level" required
                      class="w-full border-2 border-foreground p-3 bg-background font-bold uppercase text-sm focus:outline-none focus:ring-2 focus:ring-primary-blue focus:bg-white transition-all appearance-none">
                <option value="ELEMENTARY">Elementary</option>
                <option value="PRE_INTERMEDIATE">Pre-Intermediate</option>
                <option value="INTERMEDIATE">Intermediate</option>
                <option value="UPPER_INTERMEDIATE">Upper-Intermediate</option>
              </select>
            </div>

            <div class="flex items-center justify-between py-2">
              <label class="font-bold uppercase text-xs tracking-wider cursor-pointer">Xuất bản</label>
              <button type="button" @click="formData.isPublished = !formData.isPublished"
                      class="relative w-12 h-7 border-2 border-foreground transition-colors duration-200"
                      :class="formData.isPublished ? 'bg-primary-blue' : 'bg-foreground/10'">
                <div :class="['absolute top-0.5 w-5 h-5 bg-white border-2 border-foreground transition-transform duration-200',
                              formData.isPublished ? 'translate-x-6 left-0.5' : 'translate-x-0.5 left-0']"></div>
              </button>
            </div>

            <div class="flex justify-end gap-4 pt-6 border-t-4 border-foreground">
              <button type="button" @click="closeModal"
                      class="px-8 py-3 border-2 border-foreground font-bold uppercase text-sm tracking-wider hover:bg-gray-200 transition-colors">
                Hủy
              </button>
              <button type="submit" :disabled="saving"
                      class="px-8 py-3 bg-foreground text-white border-2 border-foreground font-bold uppercase text-sm tracking-wider
                             shadow-[4px_4px_0px_0px_black] hover:-translate-y-0.5 hover:shadow-[6px_6px_0px_0px_black]
                             active:translate-x-0.5 active:translate-y-0.5 active:shadow-none
                             transition-all duration-200 disabled:opacity-50">
                {{ saving ? 'Đang lưu...' : 'Lưu lại' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminService } from '@/services/adminService'
import { useToast } from '@/composables/useToast'
import { BookOpen as BookOpenIcon, Plus as PlusIcon, Edit2 as EditIcon, Trash2 as TrashIcon, Layers as LayersIcon } from 'lucide-vue-next'

const lessons = ref([])
const loading = ref(true)
const saving = ref(false)
const showModal = ref(false)
const editingLesson = ref(null)
const levelFilter = ref('')
const toast = useToast()

const levels = ['ELEMENTARY', 'PRE_INTERMEDIATE', 'INTERMEDIATE', 'UPPER_INTERMEDIATE']

const displayLevel = (lv) => ({
  ELEMENTARY: 'Elementary',
  PRE_INTERMEDIATE: 'Pre-Intermediate',
  INTERMEDIATE: 'Intermediate',
  UPPER_INTERMEDIATE: 'Upper-Intermediate'
}[lv] || lv)

const levelBadge = (lv) => ({
  ELEMENTARY: 'bg-primary-blue text-white',
  PRE_INTERMEDIATE: 'bg-primary-yellow text-foreground',
  INTERMEDIATE: 'bg-primary-red text-white',
  UPPER_INTERMEDIATE: 'bg-foreground text-white'
}[lv] || 'bg-foreground/10')

const filteredLessons = computed(() =>
  levelFilter.value ? lessons.value.filter(l => l.level === levelFilter.value) : lessons.value
)

const initialForm = { title: '', description: '', content: '', level: 'ELEMENTARY', isPublished: false }
const formData = ref({ ...initialForm })

const fetchLessons = async () => {
  loading.value = true
  try { lessons.value = await adminService.getAllLessons() }
  catch { toast.showError('Không thể tải danh sách bài học') }
  finally { loading.value = false }
}

const openModal = (lesson = null) => {
  editingLesson.value = lesson
  formData.value = lesson ? { 
    title: lesson.title, 
    description: lesson.description || '', 
    content: lesson.content || '', 
    level: lesson.level || 'ELEMENTARY', 
    isPublished: lesson.isPublished ?? false 
  } : { ...initialForm }
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
