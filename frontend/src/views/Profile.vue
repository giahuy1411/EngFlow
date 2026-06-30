<template>
  <div class="bg-background min-h-screen pb-16">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      <!-- Loading State -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-32">
        <div class="relative w-16 h-16">
          <div class="absolute inset-0 border-4 border-black rounded-full animate-spin"></div>
          <div class="absolute inset-2 bg-primary-red border-2 border-black rounded-full"></div>
        </div>
        <p class="font-bold text-sm uppercase tracking-widest mt-6 text-foreground/60">Đang tải thông tin...</p>
      </div>

      <div v-else class="space-y-8">
        <!-- ====== HERO SECTION ====== -->
        <div class="bg-white border-4 border-black shadow-hard-lg relative overflow-hidden">
          <div class="absolute -right-16 -top-16 w-48 h-48 bg-primary-yellow border-4 border-black rounded-full opacity-30"></div>
          <div class="absolute -left-8 -bottom-8 w-32 h-32 bg-primary-blue border-4 border-black opacity-20"></div>

          <div class="relative z-10 p-6 sm:p-10 flex flex-col sm:flex-row items-start sm:items-center gap-6 sm:gap-10">
            <!-- Avatar -->
            <div class="relative flex-shrink-0 group">
              <div class="w-24 h-24 sm:w-28 sm:h-28 border-4 border-black overflow-hidden bg-white">
                <img :src="auth.user?.avatarUrl || 'https://api.dicebear.com/7.x/thumbs/svg?seed=default'"
                     class="w-full h-full object-cover grayscale hover:grayscale-0 transition-all duration-500" alt="avatar" />
              </div>
              <button @click="showAvatarModal = true"
                      class="absolute inset-0 flex items-center justify-center bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity border-4 border-black cursor-pointer">
                <span class="text-white font-black text-xs uppercase tracking-wider bg-primary-red px-2 py-1 border-2 border-white">Đổi</span>
              </button>
              <div class="absolute -top-2 -right-2 w-6 h-6 bg-primary-red border-2 border-black rotate-12"></div>
            </div>

            <!-- Info -->
            <div class="flex-1 min-w-0">
              <h1 class="text-4xl sm:text-5xl font-black uppercase tracking-tighter leading-none mb-2">
                {{ auth.user?.fullName || auth.user?.username }}
              </h1>
              <div class="flex flex-wrap items-center gap-x-5 gap-y-2 text-sm font-bold uppercase tracking-wider">
                <span class="text-foreground/60">{{ auth.user?.email }}</span>
                <span class="bg-primary-red text-white px-3 py-1 border-2 border-black inline-flex items-center gap-1">
                  <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
                  {{ auth.user?.currentLevel }}
                </span>
                <span class="bg-primary-yellow text-black px-3 py-1 border-2 border-black inline-flex items-center gap-1">
                  ⭐ {{ auth.user?.totalPoints || 0 }}
                </span>
              </div>
            </div>

            <!-- Streak only -->
            <div class="flex items-center gap-4 flex-shrink-0">
              <div class="bg-white border-2 border-black px-4 py-2 text-center shadow-hard-sm">
                <div class="text-xs font-bold uppercase tracking-wider text-foreground/60">Streak</div>
                <div class="text-2xl font-black text-primary-red">{{ currentStreak }}</div>
                <div class="text-[10px] font-bold uppercase tracking-wider text-foreground/40">ngày</div>
              </div>
            </div>
          </div>
        </div>

        <!-- ====== METRICS ROW ====== -->
        <div v-if="dashboardStats" class="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          <div class="bg-white border-4 border-black shadow-hard-md p-5 relative overflow-hidden">
            <div class="absolute top-0 right-0 w-12 h-12 bg-primary-blue clip-corner"></div>
            <div class="relative z-10">
              <div class="text-xs font-bold uppercase tracking-widest text-foreground/50 mb-1">Bài học</div>
              <div class="text-3xl sm:text-4xl font-black">{{ dashboardStats.completedLessons }}<span class="text-lg font-bold text-foreground/40">/{{ dashboardStats.totalLessons }}</span></div>
              <div class="mt-2 h-2 border border-black bg-white">
                <div class="h-full bg-primary-blue transition-all duration-700" :style="{ width: `${progressPercent(dashboardStats.completedLessons, dashboardStats.totalLessons)}%` }"></div>
              </div>
            </div>
          </div>

          <div class="bg-white border-4 border-black shadow-hard-md p-5 relative overflow-hidden">
            <div class="absolute top-0 right-0 w-12 h-12 bg-primary-red clip-corner"></div>
            <div class="relative z-10">
              <div class="text-xs font-bold uppercase tracking-widest text-foreground/50 mb-1">Bài tập đúng</div>
              <div class="text-3xl sm:text-4xl font-black">{{ dashboardStats.correctExercises }}<span class="text-lg font-bold text-foreground/40">/{{ dashboardStats.totalExercises }}</span></div>
              <div class="mt-2 h-2 border border-black bg-white">
                <div class="h-full bg-primary-red transition-all duration-700" :style="{ width: `${progressPercent(dashboardStats.correctExercises, dashboardStats.totalExercises)}%` }"></div>
              </div>
            </div>
          </div>

          <div class="bg-white border-4 border-black shadow-hard-md p-5 relative overflow-hidden">
            <div class="absolute top-0 right-0 w-12 h-12 bg-primary-yellow clip-corner"></div>
            <div class="relative z-10">
              <div class="text-xs font-bold uppercase tracking-widest text-foreground/50 mb-1">Streak hiện tại</div>
              <div class="text-3xl sm:text-4xl font-black flex items-center gap-2">{{ currentStreak }}</div>
              <div class="text-sm font-bold mt-1 text-foreground/60">{{ streakMessage }}</div>
            </div>
          </div>

          <div class="bg-white border-4 border-black shadow-hard-md p-5 relative overflow-hidden">
            <div class="absolute top-0 right-0 w-12 h-12 bg-black clip-corner"></div>
            <div class="relative z-10">
              <div class="text-xs font-bold uppercase tracking-widest text-foreground/50 mb-1">Số dư</div>
              <div class="text-3xl sm:text-4xl font-black text-primary-yellow">{{ balance }}</div>
              <div class="text-sm font-bold mt-1 text-foreground/60">xu</div>
            </div>
          </div>
        </div>

        <!-- ====== MAIN CONTENT GRID ====== -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <!-- LEFT COLUMN -->
          <div class="lg:col-span-2 space-y-8">
            <!-- Streak Calendar -->
            <StreakCalendar :currentStreak="currentStreak" :history="streakHistory" />

            <!-- Chart Section -->
            <div v-if="dashboardStats" class="bg-white border-4 border-black shadow-hard-lg relative p-6 sm:p-8">
              <div class="absolute -top-3 -right-3 w-8 h-8 bg-primary-yellow border-2 border-black rotate-45 z-10"></div>

              <div class="flex items-center justify-between mb-6">
                <h3 class="font-black text-xl sm:text-2xl uppercase tracking-tight">Điểm 7 ngày</h3>
                <div class="flex gap-2">
                  <span class="w-3 h-3 bg-primary-blue border border-black"></span>
                  <span class="text-xs font-bold uppercase tracking-wider text-foreground/50">Points</span>
                </div>
              </div>

              <div class="relative h-[260px]">
                <canvas ref="chartCanvas" width="900" height="260" style="width: 100%; height: 100%;"></canvas>
              </div>
            </div>
          </div>

          <!-- RIGHT COLUMN -->
          <div class="space-y-8">
            <!-- Quick Stats Card -->
            <div v-if="dashboardStats" class="bg-white border-4 border-black shadow-hard-lg p-6">
              <div class="flex items-center gap-3 mb-5">
                <div class="w-3 h-3 bg-primary-red border border-black"></div>
                <h3 class="font-black text-lg uppercase tracking-tight">Tổng quan</h3>
              </div>

              <div class="space-y-4 divide-y-2 divide-black">
                <div class="flex justify-between items-center pt-1">
                  <span class="font-bold text-sm uppercase tracking-wider text-foreground/60">Cấp độ</span>
                  <span class="font-black text-base bg-primary-red text-white px-3 py-1 border-2 border-black">{{ auth.user?.currentLevel }}</span>
                </div>
                <div class="flex justify-between items-center pt-3">
                  <span class="font-bold text-sm uppercase tracking-wider text-foreground/60">Tổng điểm</span>
                  <span class="font-black text-lg">{{ auth.user?.totalPoints || 0 }}</span>
                </div>
                <div class="flex justify-between items-center pt-3">
                  <span class="font-bold text-sm uppercase tracking-wider text-foreground/60">Điểm hôm nay</span>
                  <span class="font-black text-lg text-primary-red">{{ dashboardStats.todayPoints || 0 }}</span>
                </div>
                <div class="flex justify-between items-center pt-3">
                  <span class="font-bold text-sm uppercase tracking-wider text-foreground/60">Lần học gần nhất</span>
                  <span class="font-bold text-sm text-foreground/70">{{ auth.user?.lastStudyDate || 'Chưa có' }}</span>
                </div>
              </div>
            </div>

            <!-- Achievements -->
            <div class="bg-white border-4 border-black shadow-hard-lg relative p-6">
              <div class="absolute -top-3 -right-3 w-8 h-8 bg-primary-red border-2 border-black rotate-12 z-10"></div>

              <div class="flex items-center gap-3 mb-5">
                <div class="w-3 h-3 bg-primary-blue border border-black"></div>
                <h3 class="font-black text-lg uppercase tracking-tight">Huy hiệu</h3>
                <span v-if="progressSummary?.unlockedAchievements?.length" class="ml-auto text-xs font-bold bg-black text-white px-2 py-0.5">{{ progressSummary.unlockedAchievements.length }}</span>
              </div>

              <div v-if="!progressSummary?.unlockedAchievements?.length"
                   class="border-2 border-black border-dashed bg-background p-6 text-center">
    <div class="w-12 h-12 bg-primary-yellow border-2 border-black flex items-center justify-center mx-auto mb-3 rotate-12"><span class="font-black text-lg">★</span></div>
    <h5 class="font-bold text-sm uppercase mb-1">Chưa có huy hiệu</h5>
                <p class="text-xs font-bold text-foreground/50 mb-4">Làm bài tập để mở khóa!</p>
                <router-link to="/lessons"
                  class="inline-block px-5 py-2 bg-primary-red text-white font-black text-xs uppercase tracking-wider border-2 border-black shadow-hard-sm hover:translate-x-0.5 hover:translate-y-0.5 hover:shadow-none transition-all">
                  Học ngay
                </router-link>
              </div>

              <div v-else class="grid grid-cols-2 gap-3">
                <div v-for="ach in progressSummary.unlockedAchievements" :key="ach.id"
                     class="border-2 border-black p-3 bg-background/50 text-center group hover:bg-primary-yellow/20 transition-colors">
                  <img :src="ach.iconUrl" class="w-10 h-10 mx-auto mb-2 border border-black grayscale group-hover:grayscale-0 transition-all" alt="">
                  <h6 class="font-bold text-xs uppercase leading-tight">{{ ach.name }}</h6>
                  <span class="text-[10px] font-bold text-foreground/50 uppercase tracking-wider">{{ ach.badgeType }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== AVATAR MODAL ====== -->
    <div v-if="showAvatarModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div class="bg-white border-4 border-black w-full max-w-lg shadow-hard-xl">
        <div class="bg-foreground border-b-4 border-black px-6 py-4 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-3 h-3 bg-primary-yellow rotate-45"></div>
            <h3 class="font-black text-lg uppercase tracking-tight text-white">Chọn ảnh đại diện</h3>
          </div>
          <button @click="showAvatarModal = false"
                  class="w-8 h-8 flex items-center justify-center border-2 border-white text-white font-black hover:bg-white hover:text-foreground transition-colors">&times;</button>
        </div>

        <div class="p-6 space-y-6">
          <div>
            <label class="block font-bold text-xs uppercase tracking-wider text-foreground/60 mb-2">Tải ảnh từ máy tính</label>
            <div class="flex items-center justify-center w-full">
              <label class="flex flex-col items-center justify-center w-full h-32 border-2 border-black border-dashed rounded-lg cursor-pointer bg-background hover:bg-gray-50">
                <div class="flex flex-col items-center justify-center pt-5 pb-6">
                  <svg class="w-8 h-8 mb-4 text-gray-500" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 16">
                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 13h3a3 3 0 0 0 0-6h-.025A5.56 5.56 0 0 0 16 6.5 5.5 5.5 0 0 0 5.207 5.021C5.137 5.017 5.071 5 5 5a4 4 0 0 0 0 8h2.167M10 15V6m0 0L8 8m2-2 2 2"/>
                  </svg>
                  <p class="mb-2 text-sm text-gray-500"><span class="font-semibold">Click để chọn file</span> hoặc kéo thả vào đây</p>
                  <p class="text-xs text-gray-500">SVG, PNG, JPG or GIF (MAX. 2MB)</p>
                </div>
                <input ref="fileInput" type="file" class="hidden" accept="image/*" @change="handleFileChange" />
              </label>
            </div>
          </div>

          <div v-if="selectedFilePreview" class="flex items-center gap-4 p-4 border-2 border-black bg-background mt-6">
            <div class="w-16 h-16 border-2 border-black bg-white flex-shrink-0">
              <img :src="selectedFilePreview" class="w-full h-full object-cover" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-black text-sm uppercase tracking-wide truncate">Preview</p>
              <p class="font-bold text-[10px] text-foreground/50 uppercase tracking-wider truncate">{{ selectedFileName }}</p>
            </div>
          </div>
        </div>

        <div class="border-t-4 border-black px-6 py-4 flex justify-end gap-4">
          <button @click="showAvatarModal = false"
                  class="px-6 py-3 border-2 border-black font-bold uppercase text-sm tracking-wider hover:bg-gray-100 transition-colors">Hủy</button>
          <button @click="saveAvatar"
                  :disabled="avatarSaving"
                  class="px-6 py-3 bg-primary-red text-white font-bold uppercase text-sm tracking-wider border-2 border-black shadow-hard-sm
                         hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5 transition-all disabled:opacity-50">
            {{ avatarSaving ? 'Đang lưu...' : 'Lưu lại' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import { useProgressStore } from '@/store/modules/progress'
import dashboardService from '@/services/dashboardService'
import streakService from '@/services/streakService'
import authService from '@/services/authService'
import StreakCalendar from '@/components/bauhaus/StreakCalendar.vue'

const auth = useAuthStore()
const progressStore = useProgressStore()

const progressSummary = computed(() => progressStore.progressSummary)
const dashboardStats = ref(null)
const chartCanvas = ref(null)
const loading = ref(true)
const streakHistory = ref([])
const currentStreak = ref(0)
const showAvatarModal = ref(false)
const avatarSaving = ref(false)
const fileInput = ref(null)
const selectedFile = ref(null)
const selectedFilePreview = ref(null)
const selectedFileName = ref('')

const streakMessage = computed(() => {
  if (currentStreak.value === 0) return 'Bắt đầu học ngay!'
  if (currentStreak.value < 3) return 'Đang vào guồng!'
  if (currentStreak.value < 7) return 'Đang có đà!'
  if (currentStreak.value < 30) return 'Rất ấn tượng!'
  return 'Huyền thoại!'
})

function progressPercent(value, total) {
  return Math.min(100, Math.round((value / Math.max(total, 1)) * 100))
}

function handleFileChange(event) {
  const file = event.target.files[0]
  if (!file) return
  
  if (file.size > 2 * 1024 * 1024) {
    alert('File ảnh không được vượt quá 2MB')
    return
  }

  selectedFile.value = file
  selectedFileName.value = file.name
  selectedFilePreview.value = URL.createObjectURL(file)
}

function clearFileSelection() {
  selectedFile.value = null
  selectedFileName.value = ''
  if (selectedFilePreview.value) {
    URL.revokeObjectURL(selectedFilePreview.value)
    selectedFilePreview.value = null
  }
}

async function saveAvatar() {
  if (!selectedFile.value) return
  avatarSaving.value = true
  try {
    const data = await authService.uploadAvatarFile(selectedFile.value)
    
    if (auth.user) {
      auth.user.avatarUrl = data.avatarUrl
      localStorage.setItem('user', JSON.stringify(auth.user))
    }
    showAvatarModal.value = false
    clearFileSelection()
  } catch (e) {
    alert('Lỗi cập nhật ảnh đại diện')
  } finally {
    avatarSaving.value = false
  }
}

onMounted(async () => {
  try {
    await auth.fetchUser()
    await progressStore.fetchProgressSummary()
    const [stats, historyData, streakData] = await Promise.all([
      dashboardService.getStats(),
      streakService.getHistory(30),
      streakService.getCurrentStreak()
    ])
    dashboardStats.value = stats
    streakHistory.value = historyData
    currentStreak.value = streakData.currentStreak
  } catch (e) {
    console.error('Lỗi tải dữ liệu dashboard:', e)
    dashboardStats.value = null
  }

  try {
    await nextTick()
    drawChart()
  } finally {
    loading.value = false
  }
})

function drawChart() {
  if (!chartCanvas.value || !dashboardStats.value?.dailyPoints) return

  const canvas = chartCanvas.value
  const ctx = canvas.getContext('2d')
  const data = dashboardStats.value.dailyPoints
  const W = canvas.width
  const H = canvas.height

  ctx.clearRect(0, 0, W, H)

  const pad = { top: 20, right: 20, bottom: 36, left: 44 }
  const cw = W - pad.left - pad.right
  const ch = H - pad.top - pad.bottom

  const maxVal = Math.max(...data.map(d => d.points), 10)

  // Grid lines
  const ySteps = 4
  for (let i = 0; i <= ySteps; i++) {
    const y = H - pad.bottom - (i / ySteps) * ch
    ctx.beginPath()
    ctx.moveTo(pad.left, y)
    ctx.lineTo(W - pad.right, y)
    ctx.strokeStyle = 'rgba(18,18,18,0.08)'
    ctx.lineWidth = 1
    ctx.stroke()

    ctx.fillStyle = '#94a3b8'
    ctx.font = '11px "Be Vietnam Pro", sans-serif'
    ctx.textAlign = 'right'
    ctx.fillText(Math.round((maxVal / ySteps) * i).toString(), pad.left - 8, y + 4)
  }

  // Bars
  const barGap = cw / data.length
  const barWidth = Math.min(barGap * 0.55, 48)
  const r = 4

  data.forEach((item, i) => {
    const x = pad.left + barGap * i + (barGap - barWidth) / 2
    const barH = (item.points / maxVal) * ch
    const y = H - pad.bottom - barH

    // Bar shadow
    ctx.fillStyle = 'rgba(18,18,18,0.1)'
    ctx.beginPath()
    ctx.roundRect(x + 3, y + 3, barWidth, barH, r)
    ctx.fill()

    // Bar fill
    ctx.fillStyle = '#1040C0'
    ctx.beginPath()
    ctx.roundRect(x, y, barWidth, barH, r)
    ctx.fill()

    // Bar border
    ctx.strokeStyle = '#121212'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.roundRect(x, y, barWidth, barH, r)
    ctx.stroke()

    // X label
    ctx.fillStyle = '#121212'
    ctx.font = 'bold 10px "Be Vietnam Pro", sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(item.date, x + barWidth / 2, H - pad.bottom + 18)

    // Value on top
    if (item.points > 0) {
      ctx.fillStyle = '#121212'
      ctx.font = 'bold 11px "Be Vietnam Pro", sans-serif'
      ctx.fillText(item.points.toString(), x + barWidth / 2, y - 6)
    }
  })
}
</script>

<style scoped>
.clip-corner {
  clip-path: polygon(100% 0, 0 0, 100% 100%);
}
</style>
