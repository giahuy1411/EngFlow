<template>
  <main class="min-h-[100dvh] bg-background">
    <div class="mx-auto max-w-7xl space-y-8 px-4 py-10 lg:py-14">
      <UserPageHeader
        eyebrow="Vocabulary studio"
        title="Bộ từ vựng"
        subtitle="Tìm một chủ đề, ôn từng nhóm từ, rồi biến thời gian rảnh thành tiến bộ thật."
      >
        <template #accent>Decks</template>
        <template #actions>
          <router-link to="/decks/create" class="app-btn app-btn--featured app-btn--md">Tạo bộ từ</router-link>
          <router-link to="/ai-vocab-generator" class="app-btn app-btn--amber app-btn--md">Tạo bằng AI</router-link>
        </template>
      </UserPageHeader>

      <section class="grid gap-4 border-b-2 border-foreground pb-6 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-end" aria-label="Tìm kiếm bộ từ">
        <label class="block">
          <span class="mb-2 block text-xs font-black uppercase tracking-wider text-muted-foreground">Tìm bộ từ</span>
          <input
            id="deck-search-input"
            v-model="searchQuery"
            name="deck-search"
            type="search"
            placeholder="Ví dụ: travel, work, daily life"
            class="min-h-12 w-full border-2 border-foreground bg-card px-4 font-bold outline-none transition focus:ring-2 focus:ring-accent"
          />
        </label>
        <div class="flex border-2 border-foreground bg-card p-1">
          <button type="button" class="min-h-10 px-4 text-xs font-black uppercase tracking-wider transition" :class="activeTab === 'public' ? 'bg-accent-strong text-white' : 'text-muted-foreground hover:text-foreground'" @click="switchTab('public')">Cộng đồng</button>
          <button type="button" class="min-h-10 px-4 text-xs font-black uppercase tracking-wider transition" :class="activeTab === 'my' ? 'bg-accent-strong text-white' : 'text-muted-foreground hover:text-foreground'" @click="switchTab('my')">Bộ của tôi</button>
        </div>
      </section>

      <section v-if="loading" class="grid gap-5 sm:grid-cols-2 xl:grid-cols-3" role="status" aria-label="Đang tải bộ từ">
        <div v-for="index in 6" :key="index" class="h-64 animate-pulse border-2 border-foreground/10 bg-card/60"></div>
      </section>

      <section v-else-if="error" class="border-2 border-danger bg-danger/10 p-6 text-danger-ink" role="alert">
        <p class="font-black">Không tải được danh sách bộ từ.</p>
        <p class="mt-1 text-sm">{{ error }}</p>
        <AppButton class="mt-4" variant="secondary" size="sm" @click="loadPage">Thử lại</AppButton>
      </section>

      <section v-else-if="displayedDecks.length" class="space-y-8">
        <div class="flex items-end justify-between gap-4">
          <div>
            <p class="text-xs font-black uppercase tracking-[0.2em] text-muted-foreground">{{ activeTab === 'public' ? 'Được chia sẻ' : 'Không gian của bạn' }}</p>
            <h2 class="mt-2 text-3xl font-black tracking-tight">{{ activeTab === 'public' ? 'Chọn một chủ đề để bắt đầu' : 'Các bộ bạn đang xây dựng' }}</h2>
          </div>
          <span class="text-sm font-black tabular-nums text-muted-foreground">{{ totalElements }} bộ</span>
        </div>

        <div class="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
          <StickerCard
            v-for="deck in displayedDecks"
            :key="deck.id"
            as="router-link"
            :to="`/decks/${deck.id}`"
            interactive
            class="block text-inherit no-underline"
            :aria-label="`Mở bộ từ ${deck.name}`"
          >
            <template #icon>
              <div class="absolute -right-2 -top-2 h-5 w-5 rotate-12 border-2 border-foreground bg-secondary"></div>
            </template>
            <div class="flex flex-wrap gap-2">
              <span v-if="deck.source" class="border-2 border-foreground bg-tertiary px-2.5 py-1 text-[11px] font-black uppercase tracking-wider">{{ deck.source }}</span>
              <span v-if="deck.cefrLevel" class="border-2 border-foreground bg-card px-2.5 py-1 text-[11px] font-black uppercase tracking-wider">{{ deck.cefrLevel }}</span>
            </div>
            <h3 class="mt-5 line-clamp-2 text-2xl font-black tracking-tight text-foreground">{{ deck.name }}</h3>
            <p v-if="deck.description" class="mt-3 line-clamp-3 text-sm leading-relaxed text-muted-foreground" v-html="sanitizeText(deck.description)"></p>
            <div class="mt-7 flex items-center justify-between border-t-2 border-foreground pt-4">
              <span class="text-xs font-black uppercase tracking-wider text-muted-foreground">{{ deck.wordCount ?? 0 }} từ</span>
              <span class="text-sm font-black uppercase tracking-wider text-accent-ink">Mở bộ &rarr;</span>
            </div>
          </StickerCard>
        </div>

        <Pagination :current-page="currentPage" :total-pages="totalPages" :total-items="totalElements" :page-size="pageSize" item-label="bộ từ vựng" @page-change="changePage" />
      </section>

      <section v-else class="border-2 border-dashed border-foreground bg-card p-12 text-center">
        <p class="text-2xl font-black">{{ activeTab === 'public' ? 'Chưa có bộ từ công khai' : 'Bạn chưa có bộ từ nào' }}</p>
        <p class="mx-auto mt-2 max-w-xl text-sm leading-relaxed text-muted-foreground">{{ activeTab === 'public' ? 'Thử lại sau hoặc tìm một chủ đề khác.' : 'Tạo bộ đầu tiên bằng tay hoặc để AI gợi ý cấu trúc từ vựng.' }}</p>
        <router-link v-if="activeTab === 'my'" to="/ai-vocab-generator" class="mt-6 inline-flex border-2 border-foreground bg-accent-strong px-5 py-3 text-xs font-black uppercase tracking-wider text-white">Tạo bộ đầu tiên</router-link>
      </section>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import deckService from '@/services/deckService'
import Pagination from '@/components/common/Pagination.vue'
import { StickerCard, AppButton } from '@/components/ui'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import { sanitizeText } from '@/utils/markdown'

const activeTab = ref('public')
const publicDecks = ref([])
const myDecks = ref([])
const loading = ref(true)
const error = ref('')
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = 9
const totalPagesPublic = ref(1)
const totalPagesMy = ref(1)
const totalElementsPublic = ref(0)
const totalElementsMy = ref(0)
const displayedDecks = computed(() => activeTab.value === 'public' ? publicDecks.value : myDecks.value)
const totalPages = computed(() => activeTab.value === 'public' ? totalPagesPublic.value : totalPagesMy.value)
const totalElements = computed(() => activeTab.value === 'public' ? totalElementsPublic.value : totalElementsMy.value)
let searchTimer

onMounted(loadPage)
onBeforeUnmount(() => clearTimeout(searchTimer))

async function loadPage() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: currentPage.value - 1, size: pageSize }
    if (searchQuery.value.trim()) params.q = searchQuery.value.trim()
    if (activeTab.value === 'public') {
      const response = await deckService.getPublicDecks(params)
      publicDecks.value = response.content || []
      totalPagesPublic.value = response.totalPages || 1
      totalElementsPublic.value = response.totalElements || 0
    } else {
      const response = await deckService.getMyDecks(params)
      myDecks.value = response.content || []
      totalPagesMy.value = response.totalPages || 1
      totalElementsMy.value = response.totalElements || 0
    }
  } catch (cause) {
    error.value = cause.response?.data?.detail || 'Không tải được bộ từ.'
  } finally {
    loading.value = false
  }
}

watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadPage()
  }, 350)
})

function switchTab(tab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  currentPage.value = 1
  loadPage()
}

function changePage(page) {
  currentPage.value = page
  loadPage()
}
</script>