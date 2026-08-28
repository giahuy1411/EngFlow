<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-tertiary border-2 border-foreground flex items-center justify-center rotate-3 rounded-md">
          <TrophyIcon class="w-5 h-5 text-foreground" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Thành Tích</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-gray-500">Quản lý thành tích & huy hiệu</p>
        </div>
      </div>
      <AppButton variant="amber" @click="openModal()">
        <PlusIcon class="w-4 h-4" />
        Thêm Thành Tích
      </AppButton>
    </div>

    <div class="border-2 border-foreground bg-white shadow-pop-lg rounded-md overflow-hidden">
      <div class="bg-foreground border-b-2 border-foreground px-6 py-3 flex items-center gap-3">
        <div class="w-2 h-2 bg-tertiary rotate-45 rounded"></div>
        <span class="font-bold text-xs uppercase tracking-widest text-white/60">{{ achievements.length }} thành tích</span>
      </div>

      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-accent/20 border-b-2 border-foreground font-black uppercase text-xs tracking-wider">
            <th class="p-4 border-r-2 border-foreground w-16 text-center">Icon</th>
            <th class="p-4 border-r-2 border-foreground">Tên</th>
            <th class="p-4 border-r-2 border-foreground text-center w-28">Huy hiệu</th>
            <th class="p-4 border-r-2 border-foreground text-center w-28">Điểm yêu cầu</th>
            <th class="p-4 text-center w-28">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading" class="border-b-2 border-foreground">
            <td colspan="5" class="p-12 text-center">
              <div class="flex items-center justify-center gap-3">
                <div class="w-5 h-5 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
                <span class="font-bold uppercase text-sm tracking-wider">Đang tải...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="achievements.length === 0" class="border-b-2 border-foreground">
            <td colspan="5" class="p-12 text-center font-bold uppercase text-sm tracking-wider text-gray-400">Chưa có thành tích</td>
          </tr>
          <tr v-for="ach in achievements" :key="ach.id"
              class="border-b-2 border-foreground hover:bg-accent/5 transition-colors duration-150">
            <td class="p-4 border-r-2 border-foreground text-center">
              <div class="w-10 h-10 border-2 border-foreground bg-background flex items-center justify-center mx-auto rounded-full">
                <img v-if="ach.iconUrl" :src="ach.iconUrl" class="w-8 h-8 object-contain" />
                <span v-else class="font-black text-gray-400 text-lg">★</span>
              </div>
            </td>
            <td class="p-4 border-r-2 border-foreground">
              <p class="font-bold">{{ ach.name }}</p>
              <p class="text-xs text-gray-500 font-medium">{{ ach.description }}</p>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span class="inline-block px-3 py-1 border-2 border-foreground text-xs font-black uppercase tracking-wider bg-tertiary/30 rounded-md">
                {{ ach.badgeType || '—' }}
              </span>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center font-black text-lg">{{ ach.pointsRequired }}</td>
            <td class="p-4 text-center">
              <div class="flex items-center justify-center gap-2">
                                <!-- Icon-only micro-control (h-9 w-9): table row edit — kept as raw button -->
                <button @click="openModal(ach)" aria-label="Sửa thành tích"
                         class="w-9 h-9 flex items-center justify-center bg-secondary text-foreground border-2 border-foreground rounded-md shadow-pop-sm
                                hover:-translate-y-0.5 hover:shadow-pop transition-all
                                active:translate-x-0.5 active:translate-y-0.5 active:shadow-none">
                   <EditIcon class="w-4 h-4" />
                 </button>
                 <!-- Icon-only micro-control (h-9 w-9): table row delete — kept as raw button -->
                 <button @click="deleteAchievement(ach.id)" aria-label="Xóa thành tích"
                         class="w-9 h-9 flex items-center justify-center bg-accent text-white border-2 border-foreground rounded-md shadow-pop-sm
                                hover:-translate-y-0.5 hover:shadow-pop transition-all
                                active:translate-x-0.5 active:translate-y-0.5 active:shadow-none">
                   <TrashIcon class="w-4 h-4" />
                 </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="bg-accent/10 border-t-2 border-foreground px-6 py-3 flex justify-between items-center">
        <span class="font-bold text-xs uppercase tracking-wider text-gray-500">Tổng số: {{ achievements.length }}</span>
        <div class="flex gap-1">
          <div class="w-2 h-2 bg-tertiary rotate-45 rounded"></div>
          <div class="w-2 h-2 rounded-full bg-accent"></div>
          <div class="w-2 h-2 bg-foreground rounded"></div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-foreground/60 backdrop-blur-sm p-4">
      <div class="bg-white border-2 border-foreground w-full max-w-lg shadow-pop-xl rounded-md flex flex-col max-h-[90vh] overflow-hidden">
        <div class="bg-tertiary border-b-2 border-foreground p-5 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-3 h-3 bg-foreground rotate-45 rounded"></div>
            <h3 class="font-black text-xl uppercase tracking-tighter text-foreground">{{ editingAchievement ? 'Sửa Thành Tích' : 'Thêm Thành Tích Mới' }}</h3>
          </div>
          <!-- icon-only control: kept raw -->
          <button @click="closeModal"
                  class="w-8 h-8 flex items-center justify-center border-2 border-foreground bg-white text-foreground font-black text-lg rounded-md shadow-pop-sm
                         hover:bg-accent hover:text-white transition-colors">&times;</button>
        </div>

        <div class="p-6 overflow-y-auto">
          <form @submit.prevent="saveAchievement" class="space-y-5">
            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Tên *</label>
              <input v-model="formData.name" type="text" required
                     class="w-full border-2 border-foreground rounded-md p-3 bg-background font-bold focus:outline-none focus:ring-2 focus:ring-accent focus:bg-white transition-all" />
            </div>
            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Mô tả</label>
              <textarea v-model="formData.description" rows="2"
                        class="w-full border-2 border-foreground rounded-md p-3 bg-background focus:outline-none focus:ring-2 focus:ring-accent focus:bg-white transition-all"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-5">
              <div>
                <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Huy hiệu</label>
                <input v-model="formData.badgeType" type="text" placeholder="VD: BRONZE, SILVER"
                       class="w-full border-2 border-foreground rounded-md p-3 bg-background font-bold uppercase text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:bg-white transition-all" />
              </div>
              <div>
                <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Điểm yêu cầu *</label>
                <input v-model.number="formData.pointsRequired" type="number" required
                       class="w-full border-2 border-foreground rounded-md p-3 bg-background font-bold text-lg focus:outline-none focus:ring-2 focus:ring-accent focus:bg-white transition-all" />
              </div>
            </div>
            <div>
              <label class="block font-bold uppercase text-xs tracking-wider mb-1.5">Icon URL</label>
               <input v-model="formData.iconUrl" type="url"
                      class="w-full border-2 border-foreground rounded-md p-3 bg-background focus:outline-none focus:ring-2 focus:ring-accent focus:bg-white transition-all" />
            </div>
            <div class="flex justify-end gap-4 pt-6 border-t-2 border-foreground">
              <AppButton type="button" @click="closeModal" variant="secondary">
                Hủy
              </AppButton>
              <AppButton type="submit" :disabled="saving" :loading="saving" variant="emerald">
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
import AppButton from '@/components/ui/AppButton.vue'
import { ref, onMounted } from 'vue'
import { adminService } from '@/services/adminService'
import { useToast } from '@/composables/useToast'
import { Trophy as TrophyIcon, Plus as PlusIcon, Edit2 as EditIcon, Trash2 as TrashIcon } from 'lucide-vue-next'

const achievements = ref([])
const loading = ref(true)
const saving = ref(false)
const showModal = ref(false)
const editingAchievement = ref(null)
const toast = useToast()

const initialForm = { name: '', description: '', badgeType: '', iconUrl: '', pointsRequired: 100 }
const formData = ref({ ...initialForm })

const fetchAchievements = async () => {
  loading.value = true
  try { achievements.value = await adminService.getAllAchievements() }
  catch { toast.showError('Không thể tải danh sách thành tích') }
  finally { loading.value = false }
}

const openModal = (ach = null) => {
  editingAchievement.value = ach
  formData.value = ach ? { ...ach } : { ...initialForm }
  showModal.value = true
}

const closeModal = () => { showModal.value = false; editingAchievement.value = null }

const saveAchievement = async () => {
  saving.value = true
  try {
    if (editingAchievement.value) { await adminService.updateAchievement(editingAchievement.value.id, formData.value); toast.showSuccess('Đã cập nhật thành tích') }
    else { await adminService.createAchievement(formData.value); toast.showSuccess('Đã tạo thành tích mới') }
    closeModal(); fetchAchievements()
  } catch { toast.showError('Lỗi lưu thành tích') }
  finally { saving.value = false }
}

const deleteAchievement = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa thành tích này?')) return
  try { await adminService.deleteAchievement(id); toast.showSuccess('Đã xóa thành tích'); fetchAchievements() }
  catch { toast.showError('Lỗi xóa thành tích') }
}

onMounted(() => fetchAchievements())
</script>
