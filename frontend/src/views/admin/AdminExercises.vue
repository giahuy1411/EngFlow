<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-primary-red border-4 border-foreground flex items-center justify-center rotate-6">
          <PenToolIcon class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Bài Tập</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-gray-500">Quản lý bài tập trắc nghiệm</p>
        </div>
      </div>
      <button @click="openModal()"
              class="flex items-center gap-2 bg-primary-red border-4 border-foreground px-5 py-3 font-bold uppercase text-sm tracking-wider text-white
                     shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md
                     active:translate-x-1 active:translate-y-1 active:shadow-none transition-all duration-200">
        <PlusIcon class="w-4 h-4" />
        Thêm Bài Tập
      </button>
    </div>

    <!-- Search + Filter bar -->
    <div class="flex flex-wrap items-center gap-3">
      <div class="relative flex-1 min-w-[200px]">
        <SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input v-model="searchQuery" type="text" placeholder="Tìm bài tập..."
               class="w-full border-2 border-foreground pl-10 pr-3 py-2.5 bg-white font-bold text-sm
                      focus:outline-none focus:ring-2 focus:ring-primary-red transition-all" />
      </div>
      <select v-model="filterType"
              class="border-2 border-foreground px-3 py-2.5 bg-white font-bold uppercase text-xs
                     focus:outline-none focus:ring-2 focus:ring-primary-red transition-all appearance-none">
        <option value="">Tất cả loại</option>
        <option value="MULTIPLE_CHOICE">Trắc nghiệm</option>
        <option value="FILL_BLANK">Điền từ</option>
        <option value="LISTENING">Nghe</option>
        <option value="MATCHING">Nối từ</option>
        <option value="TRANSLATION">Dịch</option>
      </select>
      <select v-model="filterDifficulty"
              class="border-2 border-foreground px-3 py-2.5 bg-white font-bold uppercase text-xs
                     focus:outline-none focus:ring-2 focus:ring-primary-red transition-all appearance-none">
        <option value="">Tất cả độ khó</option>
        <option value="EASY">Dễ</option>
        <option value="MEDIUM">Trung bình</option>
        <option value="HARD">Khó</option>
      </select>
    </div>

    <div class="border-4 border-foreground bg-white shadow-hard-lg overflow-hidden">
      <div class="bg-foreground border-b-4 border-foreground px-6 py-3 flex items-center gap-3">
        <div class="w-2 h-2 bg-primary-red rotate-45"></div>
        <span class="font-bold text-xs uppercase tracking-widest text-white/60">{{ filteredExercises.length }} bài tập</span>
      </div>

      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-gray-100 border-b-4 border-foreground font-bold uppercase text-xs tracking-wider">
            <th class="p-4 border-r-2 border-foreground">Câu hỏi</th>
            <th class="p-4 border-r-2 border-foreground text-center w-28">Loại</th>
            <th class="p-4 border-r-2 border-foreground text-center w-24">Độ khó</th>
            <th class="p-4 border-r-2 border-foreground text-center w-24">Lesson</th>
            <th class="p-4 text-center w-28">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading" class="border-b-2 border-foreground">
            <td colspan="5" class="p-12 text-center">
              <div class="flex items-center justify-center gap-3">
                <div class="w-5 h-5 border-2 border-foreground border-t-primary-red rounded-full animate-spin"></div>
                <span class="font-bold uppercase text-sm tracking-wider">Đang tải...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="filteredExercises.length === 0" class="border-b-2 border-foreground">
            <td colspan="5" class="p-12 text-center font-bold uppercase text-sm tracking-wider text-gray-400">
              {{ exercises.length === 0 ? 'Chưa có bài tập' : 'Không tìm thấy bài tập phù hợp' }}
            </td>
          </tr>
          <tr v-for="ex in filteredExercises" :key="ex.id"
              class="border-b-2 border-foreground hover:bg-gray-50 transition-colors duration-150">
            <td class="p-4 border-r-2 border-foreground">
              <p class="font-bold">{{ ex.title }}</p>
              <p class="text-xs text-gray-500 font-medium truncate max-w-sm">{{ ex.question }}</p>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span class="inline-block px-3 py-1 border-2 border-foreground text-xs font-bold uppercase tracking-wider bg-primary-blue/20 whitespace-nowrap">
                {{ typeLabel(ex.exerciseType) }}
              </span>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span :class="['inline-block px-3 py-1 border-2 border-foreground text-xs font-bold uppercase tracking-wider min-w-[64px]',
                             ex.difficulty === 'EASY' ? 'bg-primary-blue text-white' : ex.difficulty === 'MEDIUM' ? 'bg-primary-yellow text-foreground' : 'bg-primary-red text-white']">
                {{ difficultyLabel(ex.difficulty) }}
              </span>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center font-black text-lg">{{ ex.lesson?.title || ex.lesson?.id || '—' }}</td>
            <td class="p-4 text-center">
              <div class="flex items-center justify-center gap-2">
                <button @click="openModal(ex)" aria-label="Sửa bài tập"
                         class="w-9 h-9 flex items-center justify-center bg-primary-blue text-white border-2 border-foreground
                                hover:-translate-y-0.5 hover:shadow-hard-sm transition-all
                                active:translate-x-0.5 active:translate-y-0.5 active:shadow-none">
                   <EditIcon class="w-4 h-4" />
                 </button>
                 <button @click="confirmDelete(ex)" aria-label="Xóa bài tập"
                         class="w-9 h-9 flex items-center justify-center bg-primary-red text-white border-2 border-foreground
                                hover:-translate-y-0.5 hover:shadow-hard-sm transition-all
                                active:translate-x-0.5 active:translate-y-0.5 active:shadow-none">
                   <TrashIcon class="w-4 h-4" />
                 </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="bg-gray-100 border-t-4 border-foreground px-6 py-3 flex justify-between items-center">
        <span class="font-bold text-xs uppercase tracking-wider text-gray-500">Tổng số: {{ exercises.length }}</span>
        <div class="flex gap-1">
          <div class="w-2 h-2 bg-primary-red rotate-45"></div>
          <div class="w-2 h-2 rounded-full bg-primary-yellow"></div>
          <div class="w-2 h-2 bg-primary-blue"></div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 backdrop-blur-sm p-4 pt-[5vh] overflow-y-auto"
         @click.self="closeModal">
      <div class="bg-white border-4 border-foreground w-full max-w-2xl shadow-hard-lg flex flex-col relative"
           style="overscroll-behavior: contain;">
        <div class="bg-primary-red border-b-4 border-foreground p-5 flex items-center justify-between sticky top-0 z-10">
          <div class="flex items-center gap-3">
            <div class="w-3 h-3 bg-white rotate-45"></div>
            <h3 class="font-black text-xl uppercase tracking-tighter text-white">{{ editingExercise ? 'Sửa Bài Tập' : 'Thêm Bài Tập Mới' }}</h3>
          </div>
          <button @click="closeModal"
                  class="w-8 h-8 flex items-center justify-center border-2 border-white bg-white/20 text-white font-black text-lg
                         hover:bg-white hover:text-primary-red transition-colors">&times;</button>
        </div>

        <div class="p-6 overflow-y-auto" style="overscroll-behavior: contain;">
          <form @submit.prevent="saveExercise" class="space-y-5">
            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Tiêu đề <span class="text-primary-red">*</span></label>
              <input v-model="formData.title" type="text" required
                     class="w-full border-2 border-foreground p-3 bg-background font-bold focus:outline-none focus:ring-2 focus:ring-primary-red focus:bg-white transition-all" />
            </div>

            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Câu hỏi <span class="text-primary-red">*</span></label>
              <textarea v-model="formData.question" required rows="3"
                        class="w-full border-2 border-foreground p-3 bg-background focus:outline-none focus:ring-2 focus:ring-primary-red focus:bg-white transition-all resize-y min-h-[60px]"></textarea>
            </div>

            <div class="grid grid-cols-2 gap-5">
              <div>
                <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Loại <span class="text-primary-red">*</span></label>
                <select v-model="formData.exerciseType" required
                        class="w-full border-2 border-foreground p-3 bg-background font-bold uppercase text-sm focus:outline-none focus:ring-2 focus:ring-primary-red focus:bg-white transition-all appearance-none">
                  <option value="MULTIPLE_CHOICE">Trắc nghiệm</option>
                  <option value="FILL_BLANK">Điền từ</option>
                  <option value="LISTENING">Nghe</option>
                  <option value="MATCHING">Nối từ</option>
                  <option value="TRANSLATION">Dịch</option>
                </select>
              </div>
              <div>
                <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Độ khó <span class="text-primary-red">*</span></label>
                <select v-model="formData.difficulty" required
                        class="w-full border-2 border-foreground p-3 bg-background font-bold uppercase text-sm focus:outline-none focus:ring-2 focus:ring-primary-red focus:bg-white transition-all appearance-none">
                  <option value="EASY">Dễ</option>
                  <option value="MEDIUM">Trung bình</option>
                  <option value="HARD">Khó</option>
                </select>
              </div>
            </div>

            <!-- Lesson searchable dropdown -->
            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Bài học <span class="text-primary-red">*</span></label>
              <div class="relative">
                <input ref="lessonSearchRef"
                       v-model="lessonSearch" type="text" :placeholder="selectedLessonLabel || 'Tìm bài học...'"
                       @focus="showLessonDropdown = true"
                       @input="showLessonDropdown = true"
                       class="w-full border-2 border-foreground p-3 bg-background font-bold focus:outline-none focus:ring-2 focus:ring-primary-red focus:bg-white transition-all" />
                <button type="button" @click="clearLesson" v-if="formData.lessonId"
                        class="absolute right-2 top-1/2 -translate-y-1/2 w-6 h-6 flex items-center justify-center text-gray-400 hover:text-primary-red transition-colors">
                  &times;
                </button>
                <ul v-if="showLessonDropdown && filteredLessons.length" class="absolute z-20 top-full left-0 right-0 mt-1 border-2 border-foreground bg-white max-h-48 overflow-y-auto shadow-hard-sm">
                  <li v-for="lesson in filteredLessons" :key="lesson.id" @click="selectLesson(lesson)"
                      class="px-3 py-2.5 font-bold text-sm border-b-2 border-foreground/20 cursor-pointer
                             hover:bg-primary-red hover:text-white transition-colors last:border-b-0">
                    <span class="mr-2">{{ lesson.title }}</span>
                    <span class="text-xs text-gray-400 font-medium">{{ lesson.level }}</span>
                  </li>
                </ul>
              </div>
            </div>

            <!-- MULTIPLE_CHOICE: dynamic options -->
            <div v-if="formData.exerciseType === 'MULTIPLE_CHOICE'" class="border-2 border-primary-blue/30 bg-blue-50/50 p-4">
              <div class="flex items-center justify-between mb-3">
                <label class="font-bold uppercase text-xs tracking-wider text-primary-blue">Lựa chọn</label>
                <button type="button" @click="addOption"
                        class="text-xs font-bold uppercase border-2 border-primary-blue px-2 py-1 text-primary-blue
                               hover:bg-primary-blue hover:text-white transition-colors">
                  + Thêm
                </button>
              </div>
              <div v-for="(opt, i) in formData.options" :key="i" class="flex items-center gap-2 mb-2">
                <span class="font-black text-sm w-6 text-center uppercase text-gray-500">{{ optionLabels[i] }}</span>
                <input v-model="formData.options[i]" type="text" :placeholder="`Lựa chọn ${optionLabels[i]}`"
                       class="flex-1 border-2 border-foreground p-2.5 bg-white font-bold text-sm
                              focus:outline-none focus:ring-2 focus:ring-primary-red transition-all" />
                <button type="button" @click="removeOption(i)" v-if="formData.options.length > 2"
                        class="w-8 h-8 flex items-center justify-center border-2 border-foreground text-gray-400
                               hover:bg-primary-red hover:text-white hover:border-primary-red transition-colors font-black">
                  &times;
                </button>
                <div class="relative">
                  <input :id="'opt-radio-' + i" type="radio" name="correctOption" :value="opt"
                         v-model="formData.correctAnswer"
                         class="peer sr-only" />
                  <label :for="'opt-radio-' + i"
                         class="block w-8 h-8 border-2 border-foreground cursor-pointer
                                peer-checked:bg-green-500 peer-checked:border-green-500
                                peer-checked:text-white flex items-center justify-center
                                font-black text-xs transition-colors"
                         :title="opt === formData.correctAnswer ? 'Đáp án đúng' : 'Chọn làm đáp án'">
                    <CheckIcon v-if="opt === formData.correctAnswer" class="w-4 h-4" />
                  </label>
                </div>
              </div>
              <p class="text-xs text-gray-400 font-bold mt-2">Chọn đáp án đúng bằng cách click vào ô checkbox bên phải</p>
            </div>

            <!-- correctAnswer for non-MULTIPLE_CHOICE -->
            <div v-else>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Đáp án đúng <span class="text-primary-red">*</span></label>
              <input v-model="formData.correctAnswer" type="text" required
                     class="w-full border-2 border-foreground p-3 bg-background font-bold text-lg focus:outline-none focus:ring-2 focus:ring-primary-red focus:bg-white transition-all" />
            </div>

            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Giải thích</label>
              <textarea v-model="formData.explanation" rows="2"
                        class="w-full border-2 border-foreground p-3 bg-background focus:outline-none focus:ring-2 focus:ring-primary-red focus:bg-white transition-all resize-y"></textarea>
            </div>

            <div class="grid grid-cols-2 gap-5">
              <div>
                <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Điểm</label>
                <input v-model.number="formData.points" type="number" min="1"
                       class="w-full border-2 border-foreground p-3 bg-background font-bold focus:outline-none focus:ring-2 focus:ring-primary-red focus:bg-white transition-all" />
              </div>
            </div>

            <!-- LISTENING: audio field -->
            <div v-if="formData.exerciseType === 'LISTENING'" class="border-2 border-primary-blue/30 bg-primary-blue/5 p-4">
              <div class="flex items-center gap-2 mb-1">
                <HeadphonesIcon class="w-4 h-4 text-primary-blue" />
                <p class="font-bold uppercase text-xs tracking-wider text-primary-blue">Bài tập nghe</p>
              </div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Audio URL <span class="text-primary-red">*</span></label>
              <div class="flex gap-2">
                <input v-model="formData.audioUrl" type="url" placeholder="https://example.com/audio.mp3"
                       class="flex-1 border-2 border-foreground p-3 bg-white font-bold focus:outline-none focus:ring-2 focus:ring-primary-red focus:bg-white transition-all" />
                <label class="flex items-center gap-1.5 px-4 py-2.5 bg-primary-blue text-white border-2 border-foreground font-bold text-xs uppercase tracking-wider cursor-pointer
                              hover:-translate-y-0.5 hover:shadow-hard-sm active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all shrink-0">
                  <UploadIcon class="w-3.5 h-3.5" />
                  Upload
                  <input type="file" accept="audio/*" class="hidden" @change="uploadFile($event, 'audioUrl')" />
                </label>
              </div>
              <audio v-if="formData.audioUrl" :src="formData.audioUrl" controls class="mt-2 w-full h-10" />
            </div>

            <!-- Image -->
            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Hình ảnh</label>
              <div class="flex gap-2">
                <input v-model="formData.imageUrl" type="url" placeholder="https://example.com/image.jpg"
                       class="flex-1 border-2 border-foreground p-3 bg-background font-bold focus:outline-none focus:ring-2 focus:ring-primary-red focus:bg-white transition-all" />
                <label class="flex items-center gap-1.5 px-4 py-2.5 bg-primary-blue text-white border-2 border-foreground font-bold text-xs uppercase tracking-wider cursor-pointer
                              hover:-translate-y-0.5 hover:shadow-hard-sm active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all shrink-0">
                  <UploadIcon class="w-3.5 h-3.5" />
                  Upload
                  <input type="file" accept="image/*" class="hidden" @change="uploadFile($event, 'imageUrl')" />
                </label>
              </div>
              <img v-if="formData.imageUrl" :src="formData.imageUrl" class="mt-2 border-2 border-foreground max-h-40 object-contain bg-white"
                   @error="onImageError" />
            </div>

            <div v-if="formData.imageUrl && imageError" class="text-primary-red font-bold text-xs">Không thể tải hình ảnh từ URL này</div>

            <div class="flex justify-end gap-4 pt-6 border-t-4 border-foreground">
              <button type="button" @click="closeModal"
                      class="px-8 py-3 border-2 border-foreground font-bold uppercase text-sm tracking-wider hover:bg-gray-200 transition-colors">
                Hủy
              </button>
              <button type="submit" :disabled="saving"
                      class="px-8 py-3 bg-foreground text-white border-2 border-foreground font-bold uppercase text-sm tracking-wider
                             shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md
                             active:translate-x-0.5 active:translate-y-0.5 active:shadow-none
                             transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2">
                <div v-if="saving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                {{ saving ? 'Đang lưu...' : 'Lưu lại' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Delete confirmation modal -->
    <div v-if="deleteTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4" @click.self="deleteTarget = null">
      <div class="bg-white border-4 border-foreground w-full max-w-sm shadow-hard-lg">
        <div class="bg-primary-red border-b-4 border-foreground p-5 flex items-center gap-3">
          <div class="w-3 h-3 bg-white rotate-45"></div>
          <h3 class="font-black text-lg uppercase tracking-tighter text-white">Xác nhận xóa</h3>
        </div>
        <div class="p-6">
          <p class="font-bold mb-6">Bạn có chắc chắn muốn xóa bài tập <span class="text-primary-red">"{{ deleteTarget.title }}"</span>?</p>
          <div class="flex justify-end gap-4">
            <button @click="deleteTarget = null"
                    class="px-6 py-3 border-2 border-foreground font-bold uppercase text-sm tracking-wider hover:bg-gray-200 transition-colors">
              Hủy
            </button>
            <button @click="executeDelete" :disabled="deleting"
                    class="px-6 py-3 bg-primary-red text-white border-2 border-foreground font-bold uppercase text-sm tracking-wider
                           shadow-hard-sm hover:-translate-y-0.5 hover:shadow-hard-md
                           active:translate-x-0.5 active:translate-y-0.5 active:shadow-none
                           transition-all duration-200 disabled:opacity-50 flex items-center gap-2">
              <div v-if="deleting" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
              {{ deleting ? 'Đang xóa...' : 'Xóa' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { adminService } from '@/services/adminService'
import lessonStructureService from '@/services/lessonStructureService'
import { useToast } from '@/composables/useToast'
import {
  PenTool as PenToolIcon, Plus as PlusIcon, Edit2 as EditIcon, Trash2 as TrashIcon,
  Search as SearchIcon, Check as CheckIcon, Headphones as HeadphonesIcon,
  Upload as UploadIcon
} from 'lucide-vue-next'

const exercises = ref([])
const lessons = ref([])
const loading = ref(true)
const saving = ref(false)
const deleting = ref(false)
const showModal = ref(false)
const editingExercise = ref(null)
const deleteTarget = ref(null)
const imageError = ref(false)
const toast = useToast()

const searchQuery = ref('')
const filterType = ref('')
const filterDifficulty = ref('')

const lessonSearch = ref('')
const showLessonDropdown = ref(false)

const initialForm = {
  title: '',
  question: '',
  options: [],
  correctAnswer: '',
  explanation: '',
  exerciseType: 'MULTIPLE_CHOICE',
  difficulty: 'EASY',
  lessonId: null,
  points: 10,
  audioUrl: '',
  imageUrl: ''
}
const formData = ref({ ...initialForm })

const optionLabels = ['A', 'B', 'C', 'D', 'E', 'F']

const filteredLessons = computed(() => {
  const q = lessonSearch.value.toLowerCase().trim()
  if (!q) return lessons.value
  return lessons.value.filter(l =>
    l.title.toLowerCase().includes(q) ||
    (l.level || '').toLowerCase().includes(q)
  )
})

const selectedLessonLabel = computed(() => {
  if (!formData.value.lessonId) return ''
  const lesson = lessons.value.find(l => l.id === formData.value.lessonId)
  return lesson ? lesson.title : `ID: ${formData.value.lessonId}`
})

const filteredExercises = computed(() => {
  let result = exercises.value
  const q = searchQuery.value.toLowerCase().trim()
  if (q) {
    result = result.filter(ex =>
      (ex.title || '').toLowerCase().includes(q) ||
      (ex.question || '').toLowerCase().includes(q)
    )
  }
  if (filterType.value) {
    result = result.filter(ex => ex.exerciseType === filterType.value)
  }
  if (filterDifficulty.value) {
    result = result.filter(ex => ex.difficulty === filterDifficulty.value)
  }
  return result
})

function typeLabel(type) {
  const map = { MULTIPLE_CHOICE: 'Trắc nghiệm', FILL_BLANK: 'Điền từ', LISTENING: 'Nghe', MATCHING: 'Nối từ', TRANSLATION: 'Dịch' }
  return map[type] || type
}

function difficultyLabel(diff) {
  const map = { EASY: 'Dễ', MEDIUM: 'TB', HARD: 'Khó' }
  return map[diff] || diff
}

function addOption() {
  formData.value.options.push('')
}

function removeOption(i) {
  if (formData.value.options.length <= 2) return
  formData.value.options.splice(i, 1)
  const labels = optionLabels.slice(0, formData.value.options.length)
  if (formData.value.correctAnswer && !formData.value.options.includes(formData.value.correctAnswer)) {
    formData.value.correctAnswer = ''
  }
}

function selectLesson(lesson) {
  formData.value.lessonId = lesson.id
  lessonSearch.value = ''
  showLessonDropdown.value = false
}

function clearLesson() {
  formData.value.lessonId = null
  lessonSearch.value = ''
}

function onImageError() {
  imageError.value = true
}

async function uploadFile(event, field) {
  const file = event.target.files[0]
  if (!file) return
  try {
    const result = await lessonStructureService.uploadFile(file)
    formData.value[field] = result.url
    if (field === 'imageUrl') imageError.value = false
  } catch {
    toast.showError('Tải file thất bại')
  }
  event.target.value = ''
}

watch(() => formData.value.imageUrl, () => {
  imageError.value = false
})

watch(() => formData.value.exerciseType, (type) => {
  if (type !== 'MULTIPLE_CHOICE') {
    formData.value.options = []
  } else {
    formData.value.options = ['', '', '', '']
  }
  formData.value.correctAnswer = ''
  if (type !== 'LISTENING') {
    formData.value.audioUrl = ''
  }
})

watch(showModal, (val) => {
  if (!val) {
    editingExercise.value = null
    lessonSearch.value = ''
    showLessonDropdown.value = false
    imageError.value = false
  }
})

function parseOptions(raw) {
  if (Array.isArray(raw)) return raw.filter(o => o != null)
  if (typeof raw === 'string') {
    try { const p = JSON.parse(raw); return Array.isArray(p) ? p.filter(o => o != null) : [] }
    catch { return [] }
  }
  return []
}

const fetchExercises = async () => {
  loading.value = true
  try {
    const data = await adminService.getAllExercises()
    exercises.value = Array.isArray(data) ? data : data.content || []
  } catch { toast.showError('Không thể tải danh sách bài tập') }
  finally { loading.value = false }
}

const fetchLessons = async () => {
  try {
    const data = await adminService.getAllLessons()
    lessons.value = Array.isArray(data) ? data : data.content || []
  } catch { /* non-critical */ }
}

const openModal = (ex = null) => {
  editingExercise.value = ex
  if (ex) {
    const options = parseOptions(ex.options)
    formData.value = {
      title: ex.title || '',
      question: ex.question || '',
      options: options.length >= 2 ? options : ['', '', '', ''],
      correctAnswer: ex.correctAnswer || '',
      explanation: ex.explanation || '',
      exerciseType: ex.exerciseType || 'MULTIPLE_CHOICE',
      difficulty: ex.difficulty || 'EASY',
      lessonId: ex.lesson?.id ?? null,
      points: ex.points ?? 10,
      audioUrl: ex.audioUrl || '',
      imageUrl: ex.imageUrl || ''
    }
  } else {
    formData.value = {
      ...initialForm,
      options: ['', '', '', ''],
      exerciseType: 'MULTIPLE_CHOICE'
    }
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingExercise.value = null
}

const saveExercise = async () => {
  saving.value = true
  try {
    const payload = {
      ...formData.value,
      options: formData.value.exerciseType === 'MULTIPLE_CHOICE'
        ? formData.value.options.filter(o => o.trim() !== '')
        : formData.value.options
    }
    if (editingExercise.value) {
      await adminService.updateExercise(editingExercise.value.id, payload)
      toast.showSuccess('Đã cập nhật bài tập')
    } else {
      await adminService.createExercise(payload)
      toast.showSuccess('Đã tạo bài tập mới')
    }
    closeModal()
    await fetchExercises()
  } catch (e) {
    const msg = e?.response?.data?.message || 'Lỗi lưu bài tập'
    toast.showError(msg)
  } finally { saving.value = false }
}

const confirmDelete = (ex) => {
  deleteTarget.value = ex
}

const executeDelete = async () => {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await adminService.deleteExercise(deleteTarget.value.id)
    toast.showSuccess('Đã xóa bài tập')
    deleteTarget.value = null
    await fetchExercises()
  } catch { toast.showError('Lỗi xóa bài tập') }
  finally { deleting.value = false }
}

onMounted(() => {
  fetchExercises()
  fetchLessons()
})
</script>
