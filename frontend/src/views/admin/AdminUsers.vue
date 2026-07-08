<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-accent border-2 border-foreground flex items-center justify-center rounded-md">
          <Users class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tight">Người dùng</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-muted-foreground">Quản lý tài khoản</p>
        </div>
      </div>
    </div>

    <!-- Table Container -->
    <div class="border-2 border-foreground bg-card shadow-pop-lg rounded-md overflow-hidden">
      <div class="bg-foreground border-b-2 border-foreground px-6 py-3 flex items-center gap-3">
        <div class="w-2 h-2 bg-tertiary rotate-45 rounded-sm"></div>
        <span class="font-bold text-xs uppercase tracking-widest text-white/60">{{ users.length }} người dùng</span>
      </div>
      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-muted/30 border-b-2 border-foreground font-black text-xs tracking-wider">
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
                <div class="w-5 h-5 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
                <span class="font-bold text-sm tracking-wider">Đang tải...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="users.length === 0" class="border-b-2 border-foreground">
            <td colspan="5" class="p-12 text-center">
              <div class="font-bold text-sm tracking-wider text-muted-foreground">Chưa có người dùng</div>
            </td>
          </tr>
          <tr v-for="user in users" :key="user.id"
            class="border-b-2 border-foreground hover:bg-accent/5 transition-colors duration-150"
          >
            <td class="p-4 border-r-2 border-foreground">
              <div class="flex items-center gap-3">
                <img :src="user.avatarUrl || 'https://api.dicebear.com/7.x/identicon/svg?seed=' + user.username"
                  class="w-10 h-10 border-2 border-foreground rounded-md shadow-pop-sm" alt="" />
                <span class="font-bold">{{ user.username }}</span>
              </div>
            </td>
            <td class="p-4 border-r-2 border-foreground text-sm font-medium text-muted-foreground">{{ user.email }}</td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span :class="['inline-block px-3 py-1 border-2 border-foreground text-xs font-black tracking-wider rounded-md shadow-pop-sm',
                user.isAdmin ? 'bg-secondary text-foreground' : 'bg-card text-foreground']">
                {{ user.isAdmin ? 'Admin' : 'User' }}
              </span>
            </td>
            <td class="p-4 border-r-2 border-foreground text-center">
              <span :class="['inline-block px-3 py-1 border-2 border-foreground text-xs font-black tracking-wider rounded-md shadow-pop-sm',
                user.isActive ? 'bg-quaternary text-foreground' : 'bg-muted text-muted-foreground']">
                {{ user.isActive ? 'Hoạt động' : 'Đã khóa' }}
              </span>
            </td>
            <td class="p-4 text-center">
              <button @click="toggleUser(user.id)"
                :disabled="togglingUserId === user.id"
                class="px-4 py-1.5 border-2 border-foreground font-black text-xs tracking-wider transition-all duration-200 rounded-md shadow-pop-sm active:translate-x-0.5 active:translate-y-0.5"
                :class="[
                  togglingUserId === user.id ? 'opacity-50 cursor-not-allowed' : '',
                  user.isActive
                    ? 'bg-accent text-white hover:bg-accent/90'
                    : 'bg-secondary text-white hover:bg-secondary/90'
                ]"
              >{{ togglingUserId === user.id ? '...' : (user.isActive ? 'Khóa' : 'Mở') }}</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="bg-muted/20 border-t-2 border-foreground px-6 py-3 flex justify-between items-center">
        <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Tổng: {{ users.length }}</span>
        <div class="flex gap-1">
          <div class="w-2 h-2 bg-accent rounded-full"></div>
          <div class="w-2 h-2 bg-tertiary rotate-45 rounded-sm"></div>
          <div class="w-2 h-2 bg-secondary rounded-full"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminService } from '@/services/adminService'
import { Users } from 'lucide-vue-next'

const users = ref([])
const loading = ref(true)
const togglingUserId = ref(null)

onMounted(async () => {
  try {
    users.value = await adminService.getAllUsers()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

async function toggleUser(userId) {
  togglingUserId.value = userId
  try {
    const updated = await adminService.toggleUserActive(userId)
    const idx = users.value.findIndex(u => u.id === userId)
    if (idx !== -1) users.value[idx] = { ...users.value[idx], isActive: updated.isActive }
  } catch (e) {
    console.error(e)
  } finally {
    togglingUserId.value = null
  }
}
</script>
