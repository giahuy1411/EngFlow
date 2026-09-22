<template>
  <div class="bg-background min-h-screen py-16">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <UserPageHeader
        eyebrow="Vocabulary studio"
        title="Tạo bộ từ"
        subtitle="Tạo bộ từ vựng mới theo chủ đề, nguồn học và cấp độ CEFR."
        root-class="mb-10"
      >
        <template #accent>Create</template>
        <template #actions>
          <router-link to="/decks"
            class="inline-flex h-11 w-11 items-center justify-center rounded-full border-2 border-foreground bg-card shadow-pop-sm transition-all hover:-translate-y-0.5 hover:bg-tertiary/30 active:scale-[0.98]"
            aria-label="Quay lại bộ từ"
          >
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
          </router-link>
        </template>
      </UserPageHeader>

      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl">
        <form @submit.prevent="handleCreate" class="space-y-6">
          <!-- Name -->
          <div>
            <label for="deck-name" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Tên bộ từ</label>
            <input id="deck-name" v-model="form.name" placeholder="e.g. IELTS Vocabulary" required
              class="app-input"
            />
          </div>

          <!-- Description -->
          <div>
            <label for="deck-desc" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Mô tả</label>
            <textarea id="deck-desc" v-model="form.description" rows="3" placeholder="Mô tả ngắn về bộ từ..."
              class="app-input"
            ></textarea>
          </div>

          <!-- Source & Level -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label for="deck-source" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Nguồn</label>
              <input id="deck-source" v-model="form.source" placeholder="e.g. Cambridge"
                class="app-input"
              />
            </div>
            <div>
              <label for="deck-level" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">CEFR Level</label>
              <select id="deck-level" v-model="form.cefrLevel"
                class="w-full bg-input border-2 border-border rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-[4px_4px_0px_0px_transparent] focus:border-accent focus:shadow-pop-accent focus:outline-none"
              >
                <option value="">Chọn level</option>
                <option v-for="lv in ['A1','A2','B1','B2','C1','C2']" :key="lv" :value="lv">{{ lv }}</option>
              </select>
            </div>
          </div>

          <!-- Words -->
          <div>
            <label for="deck-words" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Từ vựng (mỗi dòng một từ)</label>
            <textarea id="deck-words" v-model="form.words" rows="8" placeholder="word1
word2
word3"
              class="app-input"
            ></textarea>
          </div>

          <!-- Public toggle -->
          <label class="flex items-center gap-3 cursor-pointer">
            <input type="checkbox" v-model="form.isPublic" class="w-5 h-5 border-2 border-foreground rounded-sm accent-accent" />
            <span class="font-bold text-sm uppercase tracking-wider">Công khai</span>
          </label>

          <p v-if="error" class="font-bold text-xs uppercase tracking-wider text-danger-ink">{{ error }}</p>

          <AppButton type="submit" variant="featured" size="lg" class="w-full" :loading="loading">
            Tạo bộ từ
          </AppButton>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import deckService from '@/services/deckService'
import { AppButton } from '@/components/ui'
import UserPageHeader from '@/components/common/UserPageHeader.vue'

const router = useRouter()
const loading = ref(false)
const error = ref('')

const form = reactive({
  name: '',
  description: '',
  source: '',
  cefrLevel: '',
  words: '',
  isPublic: false,
})

async function handleCreate() {
  error.value = ''
  loading.value = true
  try {
    const words = form.words.split('\n').filter(w => w.trim())
    await deckService.createDeck({
      name: form.name,
      description: form.description,
      source: form.source,
      cefrLevel: form.cefrLevel,
      words,
      isPublic: form.isPublic,
    })
    router.push('/decks')
  } catch (e) {
    error.value = e.response?.data?.message || 'Tạo thất bại'
  } finally {
    loading.value = false
  }
}
</script>
