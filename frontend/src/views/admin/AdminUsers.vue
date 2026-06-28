<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-primary-red border-4 border-foreground flex items-center justify-center">
          <UsersIcon class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Người dùng</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-gray-500">Quản lý tài khoản</p>
        </div>
      </div>
    </div>

    <div class="border-4 border-foreground bg-white shadow-[8px_8px_0px_0px_black] overflow-hidden">
      <div class="bg-foreground border-b-4 border-foreground px-6 py-3 flex items-center gap-3">
        <div class="w-2 h-2 bg-primary-yellow rotate-45"></div>
        <span class="font-bold text-xs uppercase tracking-widest text-white/60">{{ users.length }} người dùng</span>
      </div>

      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-gray-100 border-b-4 border-foreground font-bold uppercase text-xs tracking-wider">
            <th class="p-4 border-r-2 border-foreground">Người dùng</th>
            <th class="p-4 border-r-2 border-foreground">Email</th>
            <th class="p-4 border-r-2 border-foreground text-center w-24">Vai trò</th>
            <th class="p-4 border-r-2 border-foreground text-center w-28">Trạng thái</th>
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
          <tr v-else-if="users.length === 0" class="border-b-2 border-foreground">
            <td colspan="5" class="p-12 text-center">
              <div class="font-bold uppercase text-sm tracking-wider text-gray-400">Chưa có người dùng</div>
            </td>
          </tr>
          <tr v-for="user in users" :key="user.id"
              class="border-b-2 border-foreground hover:bg-gray-50 transition-colors duration-150">
            <td class="p-4 border-r-2 border-foreground">
              <div class="flex items-center gap-3">
                <img :src="user.avatarUrl || 'https://api.dicebear.com/7.x/identicon/svg?seed=' + user.username"
                     class="w-10 h-10 border-2 border-foreground grayscale" />
                <span class="font-bold">{{ user.username }}</span>
              </div>
            </td>
            <td class="p-4 border-r-2 border-foreground text-sm font-medium text-gray-600">{{ user.email }}</td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span :class="['inline-block px-3 py-1 border-2 border-foreground text-xs font-bold uppercase tracking-wider',
                            user.isAdmin ? 'bg-primary-blue text-white' : 'bg-gray-200 text-foreground']">
                {{ user.isAdmin ? 'Admin' : 'User' }}
              </span>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span :class="['inline-block px-3 py-1 border-2 border-foreground text-xs font-bold uppercase tracking-wider',
                            user.isActive ? 'bg-primary-blue text-white' : 'bg-foreground/10 text-foreground/70']">
                {{ user.isActive ? 'Hoạt động' : 'Khóa' }}
              </span>
            </td>
            <td class="p-4 text-center">
              <button @click="toggleUserActive(user)"
                      :disabled="togglingUserId === user.id"
                      :class="['px-4 py-1.5 border-2 border-foreground font-bold uppercase text-xs tracking-wider transition-all duration-200',
                               'active:translate-x-0.5 active:translate-y-0.5',
                               togglingUserId === user.id ? 'opacity-50 cursor-not-allowed' : '',
                                user.isActive
                                  ? 'bg-primary-red text-white hover:bg-primary-red/90'
                                  : 'bg-primary-blue text-white hover:bg-primary-blue/90']">
                {{ togglingUserId === user.id ? '...' : (user.isActive ? 'Khóa' : 'Mở') }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="bg-gray-100 border-t-4 border-foreground px-6 py-3 flex justify-between items-center">
        <span class="font-bold text-xs uppercase tracking-wider text-gray-500">Tổng số: {{ users.length }}</span>
        <div class="flex gap-1">
          <div class="w-2 h-2 bg-primary-red rounded-full"></div>
          <div class="w-2 h-2 bg-primary-yellow rotate-45"></div>
          <div class="w-2 h-2 bg-primary-blue"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminService } from '@/services/adminService'
import { useToast } from '@/composables/useToast'
import { Users as UsersIcon } from 'lucide-vue-next'

const users = ref([])
const loading = ref(true)
const toast = useToast()
const togglingUserId = ref(null)

const fetchUsers = async () => {
  loading.value = true
  try {
    users.value = await adminService.getAllUsers()
  } catch {
    toast.showError('Không thể tải danh sách người dùng')
  } finally {
    loading.value = false
  }
}

const toggleUserActive = async (user) => {
  if (togglingUserId.value === user.id) return
  togglingUserId.value = user.id
  try {
    await adminService.toggleUserActive(user.id)
    user.isActive = !user.isActive
    toast.showSuccess(user.isActive ? 'Đã mở khóa người dùng' : 'Đã khóa người dùng')
  } catch {
    toast.showError('Lỗi thay đổi trạng thái')
  } finally {
    togglingUserId.value = null
  }
}

onMounted(() => fetchUsers())
</script>
