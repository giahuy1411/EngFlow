<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-accent border-2 border-foreground flex items-center justify-center rotate-6 rounded-md">
          <Users class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tight">Người dùng</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-muted-foreground">Quản lý tài khoản</p>
        </div>
      </div>
    </div>

    <!-- Search -->
    <div class="flex justify-between items-center gap-4">
      <div class="relative w-full max-w-md">
        <input
          id="user-search"
          name="user-search"
          v-model="searchQuery"
          @input="onSearchInput"
          type="search"
          placeholder="Tìm theo tên, email..."
          class="w-full border-2 border-foreground bg-white px-4 py-2.5 pr-10 font-medium rounded-md shadow-pop-sm focus:ring-2 focus:ring-accent outline-none"
        />
      </div>
    </div>

    <!-- Table Container (audit-v7 F68: overflow-x-auto thay hidden → bảng 6 cột scroll được ở 375px) -->
    <div class="border-2 border-foreground bg-card shadow-pop-lg rounded-md overflow-x-auto">
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
            <th class="p-4 border-r-2 border-foreground text-center w-28">Premium</th>
            <th class="p-4 text-center w-36">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading" class="border-b-2 border-foreground">
            <td colspan="6" class="p-12 text-center">
              <div class="flex items-center justify-center gap-3">
                <div class="w-5 h-5 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
                <span class="font-bold text-sm tracking-wider">Đang tải...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="users.length === 0" class="border-b-2 border-foreground">
            <td colspan="6" class="p-12 text-center">
              <div class="font-bold text-sm tracking-wider text-muted-foreground">Chưa có người dùng</div>
            </td>
          </tr>
          <tr v-for="user in paginatedUsers" :key="user.id"
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
            <td class="p-4 border-r-2 border-foreground text-center">
              <span v-if="user.premiumExpiry" class="inline-block px-3 py-1 border-2 border-foreground text-xs font-black tracking-wider rounded-md shadow-pop-sm bg-tertiary text-foreground">
                Hết {{ formatDate(user.premiumExpiry) }}
              </span>
              <span v-else class="inline-block px-3 py-1 border-2 border-foreground text-xs font-black tracking-wider rounded-md shadow-pop-sm bg-muted text-muted-foreground">
                Không
              </span>
            </td>
            <td class="p-4 text-center">
              <div class="flex flex-col gap-1.5 items-center">
                <AppButton @click="toggleUser(user.id)"
                  :disabled="togglingUserId === user.id"
                  size="sm"
                  class="w-full"
                  :variant="user.isActive ? 'primary' : 'pink'"
                >{{ togglingUserId === user.id ? '...' : (user.isActive ? 'Khóa' : 'Mở') }}</AppButton>
                <AppButton v-if="user.premiumExpiry" @click="revokePremium(user.id)"
                  :disabled="premiumUserId === user.id"
                  variant="danger"
                  size="sm"
                  class="w-full"
                >{{ premiumUserId === user.id ? '...' : 'Huỷ Premium' }}</AppButton>
                <AppButton v-else @click="activatePremium(user.id)"
                  :disabled="premiumUserId === user.id"
                  variant="amber"
                  size="sm"
                  class="w-full"
                >{{ premiumUserId === user.id ? '...' : 'Cấp Premium' }}</AppButton>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="flex flex-wrap items-center justify-between gap-4 bg-accent/10 border-t-2 border-foreground px-6 py-3">
        <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">Tổng số: {{ totalElements }}</span>
        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          :total-items="totalElements"
          :page-size="pageSize"
          :show-summary="false"
          item-label="người dùng"
          class="!bg-transparent !border-t-0 !shadow-none !p-0"
          @page-change="changePage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminService } from '@/services/adminService'
import Pagination from '@/components/common/Pagination.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { Users } from 'lucide-vue-next'

const users = ref([])
const loading = ref(true)
const togglingUserId = ref(null)
const premiumUserId = ref(null)
const searchQuery = ref('')
const searchTimer = ref(null)

const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const totalElements = ref(0)

const paginatedUsers = computed(() => users.value)

onMounted(async () => {
  await loadUsers()
})

async function loadUsers() {
  loading.value = true
  try {
    const data = await adminService.getAllUsers({
      q: searchQuery.value.trim() || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    users.value = Array.isArray(data) ? data : data.content || []
    totalPages.value = data.totalPages || 1
    totalElements.value = data.totalElements || users.value.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  currentPage.value = p
  loadUsers()
}

function onSearchInput() {
  clearTimeout(searchTimer.value)
  searchTimer.value = setTimeout(() => {
    currentPage.value = 1
    loadUsers()
  }, 300)
}

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

async function activatePremium(userId) {
  premiumUserId.value = userId
  try {
    const updated = await adminService.toggleUserPremium(userId)
    const idx = users.value.findIndex(u => u.id === userId)
    if (idx !== -1) users.value[idx] = { ...users.value[idx], isPremium: updated.isPremium, premiumExpiry: updated.premiumExpiry }
  } catch (e) {
    console.error(e)
  } finally {
    premiumUserId.value = null
  }
}

async function revokePremium(userId) {
  premiumUserId.value = userId
  try {
    const updated = await adminService.revokeUserPremium(userId)
    const idx = users.value.findIndex(u => u.id === userId)
    if (idx !== -1) users.value[idx] = { ...users.value[idx], isPremium: updated.isPremium, premiumExpiry: updated.premiumExpiry }
  } catch (e) {
    console.error(e)
  } finally {
    premiumUserId.value = null
  }
}

function formatDate(value) {
  if (!value) return ''
  const d = new Date(value)
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
</script>
