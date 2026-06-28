<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="bg-primary-blue border-4 border-black p-8 shadow-[6px_6px_0px_0px_black] text-white">
      <h2 class="font-black text-4xl uppercase tracking-tighter mb-2">Chấm điểm bài học</h2>
      <p class="font-bold text-sm uppercase bg-white text-black inline-block px-3 py-1 border-2 border-black">
        Quản lý bài nộp Viết & Nói của học viên
      </p>
    </div>

    <!-- Filter Tabs -->
    <div class="flex gap-4 border-b-4 border-black pb-4">
      <button 
        @click="filterStatus = 'PENDING'"
        class="px-6 py-3 border-4 border-black font-black uppercase text-sm tracking-wider transition-all duration-150"
        :class="filterStatus === 'PENDING' ? 'bg-primary-yellow shadow-[4px_4px_0px_0px_black] -translate-y-1' : 'bg-white hover:bg-gray-100'"
      >
        Chờ chấm điểm ({{ pendingSubmissions.length }})
      </button>
      <button 
        @click="filterStatus = 'GRADED'"
        class="px-6 py-3 border-4 border-black font-black uppercase text-sm tracking-wider transition-all duration-150"
        :class="filterStatus === 'GRADED' ? 'bg-primary-blue text-white shadow-[4px_4px_0px_0px_black] -translate-y-1' : 'bg-white hover:bg-gray-100'"
      >
        Đã chấm điểm ({{ gradedSubmissions.length }})
      </button>
    </div>

    <!-- Main Workspace -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      
      <!-- Submissions List (Left/Mid Column) -->
      <div class="lg:col-span-2 space-y-6">
        <div v-if="loading" class="text-center py-12 bg-white border-4 border-black border-dashed">
          <LoaderIcon class="w-10 h-10 animate-spin mx-auto text-primary-red mb-3" />
          <p class="font-bold uppercase tracking-wider text-sm">Đang tải danh sách...</p>
        </div>

        <div v-else-if="filteredSubmissions.length === 0" class="text-center py-12 bg-white border-4 border-black border-dashed">
          <p class="font-bold uppercase tracking-wider text-sm text-gray-500 mb-0">Không có bài nộp nào phù hợp.</p>
        </div>

        <div v-else class="space-y-4">
          <div 
            v-for="sub in filteredSubmissions" 
            :key="sub.id"
            class="bg-white border-4 border-black p-6 shadow-[6px_6px_0px_0px_black] hover:-translate-y-1 hover:shadow-[8px_8px_0px_0px_black] transition-all cursor-pointer relative"
            :class="{ 'border-primary-red': selectedSubmission && selectedSubmission.id === sub.id }"
            @click="selectSubmission(sub)"
          >
            <!-- Badge -->
            <span class="absolute top-4 right-4 border-2 border-black px-2.5 py-0.5 text-xs font-bold uppercase tracking-wider" :class="skillBadgeClass(sub.skillType)">
              {{ sub.skillType === 'WRITING' ? 'VIẾT' : 'NÓI' }}
            </span>

            <h4 class="font-black text-xl uppercase tracking-tighter mb-2">{{ sub.fullName || sub.username }}</h4>
            <p class="font-bold text-xs text-muted-foreground uppercase mb-4">Bài học: <span class="text-foreground font-black">{{ sub.lessonTitle }}</span></p>
            
            <div class="flex justify-between items-end border-t-2 border-black pt-4">
              <span class="font-mono text-xs text-gray-500 font-bold">{{ formatDate(sub.createdAt) }}</span>
              <span v-if="sub.status === 'GRADED'" class="font-black text-sm uppercase text-foreground bg-primary-yellow/20 border-2 border-black px-2 py-1">
                ĐÃ CHẤM: {{ sub.score }}/10
              </span>
              <span v-else class="font-black text-sm uppercase text-foreground bg-primary-red/10 border-2 border-black px-2 py-1">
                CHỜ CHẤM ĐIỂM
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Grading Detail / Form (Right Column) -->
      <div class="bg-white border-4 border-black p-6 shadow-[8px_8px_0px_0px_black] relative h-fit">
        <h3 class="font-black text-2xl uppercase tracking-tighter mb-6 border-b-4 border-black pb-3">Chi tiết chấm điểm</h3>

        <div v-if="!selectedSubmission" class="text-center py-12 text-gray-500 font-bold uppercase text-xs">
          Chọn một bài nộp bên trái để chấm điểm
        </div>

        <div v-else class="space-y-6">
          <div>
            <span class="text-[10px] font-black uppercase text-gray-500 tracking-widest block mb-1">Học viên</span>
            <p class="font-black text-lg uppercase mb-0">{{ selectedSubmission.fullName || selectedSubmission.username }}</p>
            <span class="text-xs text-gray-400 font-mono">@{{ selectedSubmission.username }}</span>
          </div>

          <div>
            <span class="text-[10px] font-black uppercase text-gray-500 tracking-widest block mb-1">Bài học & Kỹ năng</span>
            <p class="font-bold text-sm mb-1">{{ selectedSubmission.lessonTitle }}</p>
            <span class="border-2 border-black px-2 py-0.5 text-xs font-black uppercase" :class="skillBadgeClass(selectedSubmission.skillType)">
              {{ selectedSubmission.skillType }}
            </span>
          </div>

          <!-- Submitted Content Display -->
          <div class="border-4 border-black p-4 bg-gray-50">
            <span class="text-[10px] font-black uppercase text-gray-500 tracking-widest block mb-2">Nội dung học viên nộp</span>
            
            <!-- Writing Content -->
            <div v-if="selectedSubmission.skillType === 'WRITING'">
              <p class="whitespace-pre-wrap font-medium text-sm text-foreground mb-0 leading-relaxed max-h-[250px] overflow-y-auto pr-2">{{ selectedSubmission.submissionText }}</p>
            </div>

            <!-- Speaking Content (Audio Player) -->
            <div v-if="selectedSubmission.skillType === 'SPEAKING' && selectedSubmission.audioUrl" class="space-y-3">
              <div class="flex items-center gap-3 bg-white p-3 border-2 border-black shadow-[2px_2px_0px_0px_black]">
                <button 
                  @click="playSpeaking" 
                  class="w-10 h-10 flex items-center justify-center bg-primary-red border-2 border-black text-white font-black text-lg hover:scale-105 active:scale-95 transition-transform"
                >
                    <span v-if="playing" class="w-4 h-0.5 bg-white"></span>
                    <span v-else class="w-0 h-0 border-l-[10px] border-l-white border-t-[7px] border-t-transparent border-b-[7px] border-b-transparent ml-0.5"></span>
                </button>
                <div class="flex-1 min-w-0">
                  <div class="font-bold text-xs uppercase tracking-wider truncate mb-1">Bản ghi giọng nói</div>
                  <div class="w-full h-1.5 bg-gray-200 border border-black cursor-pointer relative" @click="seekAudio">
                    <div class="h-full bg-primary-yellow border-r border-black" :style="{ width: audioProgress + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Grading Form -->
          <form @submit.prevent="submitGrade" class="space-y-4 border-t-4 border-black pt-4">
            <div>
              <label class="block text-xs font-black uppercase tracking-wider mb-2">Điểm số (0.0 - 10.0)</label>
              <input 
                type="number" 
                step="0.1" 
                min="0" 
                max="10" 
                v-model.number="gradeScore"
                required
                class="w-full p-3 border-4 border-black font-black text-lg focus:outline-none focus:ring-0 shadow-[3px_3px_0px_0px_black]"
                placeholder="Nhập điểm..."
              />
            </div>

            <div>
              <label class="block text-xs font-black uppercase tracking-wider mb-2">Nhận xét của giáo viên</label>
              <textarea 
                rows="4" 
                v-model="gradeFeedback"
                class="w-full p-3 border-4 border-black font-medium text-sm focus:outline-none focus:ring-0 shadow-[3px_3px_0px_0px_black]"
                placeholder="Nhập nhận xét..."
              ></textarea>
            </div>

            <button 
              type="submit" 
              :disabled="grading"
              class="w-full py-4 bg-primary-red text-white border-4 border-black font-black uppercase tracking-widest text-sm hover:-translate-y-0.5 hover:shadow-[4px_4px_0px_0px_black] active:translate-y-0.5 active:shadow-none transition-all shadow-[2px_2px_0px_0px_black] disabled:opacity-50"
            >
              {{ grading ? 'ĐANG LƯU...' : 'XÁC NHẬN CHẤM ĐIỂM' }}
            </button>
          </form>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { submissionService } from '@/services/submissionService'
import { Loader2 as LoaderIcon } from 'lucide-vue-next'

const submissions = ref([])
const loading = ref(true)
const grading = ref(false)
const filterStatus = ref('PENDING')

const selectedSubmission = ref(null)
const gradeScore = ref(null)
const gradeFeedback = ref('')

// Audio control
const playing = ref(false)
const audioProgress = ref(0)
let audio = null

const pendingSubmissions = computed(() => {
  return submissions.value.filter(s => s.status === 'PENDING')
})

const gradedSubmissions = computed(() => {
  return submissions.value.filter(s => s.status === 'GRADED')
})

const filteredSubmissions = computed(() => {
  return filterStatus.value === 'PENDING' ? pendingSubmissions.value : gradedSubmissions.value
})

const fetchAllSubmissions = async () => {
  loading.value = true
  try {
    const data = await submissionService.adminGetSubmissions()
    submissions.value = data || []
  } catch (e) {
    console.error('Failed to get submissions:', e)
  } finally {
    loading.value = false
  }
}

const selectSubmission = (sub) => {
  stopAudio()
  selectedSubmission.value = sub
  gradeScore.value = sub.score !== null ? sub.score : null
  gradeFeedback.value = sub.feedback || ''
}

const submitGrade = async () => {
  if (!selectedSubmission.value) return
  grading.value = true
  try {
    const updated = await submissionService.adminGradeSubmission(
      selectedSubmission.value.id,
      gradeScore.value,
      gradeFeedback.value
    )
    
    // Update local list
    const idx = submissions.value.findIndex(s => s.id === selectedSubmission.value.id)
    if (idx !== -1) {
      submissions.value[idx] = updated
    }
    
    alert('Đã chấm điểm thành công!')
    selectedSubmission.value = null
  } catch (e) {
    alert('Không thể cập nhật điểm. Vui lòng thử lại.')
    console.error(e)
  } finally {
    grading.value = false
  }
}

const playSpeaking = () => {
  if (!selectedSubmission.value || !selectedSubmission.value.audioUrl) return
  
  if (audio) {
    if (playing.value) {
      audio.pause()
      playing.value = false
      return
    } else {
      audio.play()
      playing.value = true
      return
    }
  }

  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const fullUrl = selectedSubmission.value.audioUrl.startsWith('http') 
    ? selectedSubmission.value.audioUrl 
    : baseUrl + selectedSubmission.value.audioUrl

  audio = new Audio(fullUrl)
  audio.addEventListener('timeupdate', () => {
    if (audio.duration) {
      audioProgress.value = (audio.currentTime / audio.duration) * 100
    }
  })
  audio.addEventListener('ended', () => {
    playing.value = false
    audioProgress.value = 0
    audio = null
  })
  audio.play()
  playing.value = true
}

const seekAudio = (e) => {
  if (!audio || !audio.duration) return
  const rect = e.currentTarget.getBoundingClientRect()
  const pct = (e.clientX - rect.left) / rect.width
  audio.currentTime = pct * audio.duration
}

const stopAudio = () => {
  if (audio) {
    audio.pause()
    audio = null
  }
  playing.value = false
  audioProgress.value = 0
}

const skillBadgeClass = (skillType) => {
  return skillType === 'WRITING' ? 'bg-primary-blue/15 text-primary-blue' : 'bg-primary-red/15 text-primary-red'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

onMounted(() => {
  fetchAllSubmissions()
})
</script>

<style scoped>
</style>
