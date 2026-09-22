<template>
  <div class="bg-background min-h-screen py-16">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
      <UserPageHeader
        eyebrow="AI vocabulary"
        title="AI Generator"
        subtitle="Tự động sinh từ vựng theo chủ đề, cấp độ và lưu thẳng vào bộ học."
        root-class="mb-10"
      >
        <template #accent>5 free</template>
        <template #actions>
          <router-link to="/decks"
            class="inline-flex h-11 w-11 items-center justify-center rounded-full border-2 border-foreground bg-card shadow-pop-sm transition-all hover:-translate-y-0.5 hover:bg-tertiary/30 active:scale-[0.98]"
            aria-label="Quay lại bộ từ"
          >
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
          </router-link>
        </template>
      </UserPageHeader>

      <section class="mb-6 border-2 border-foreground bg-card p-5 shadow-pop-lg" aria-labelledby="ai-quota-title">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">Quyền sử dụng</p>
            <h2 id="ai-quota-title" class="mt-1 text-xl font-black">{{ unlimited ? 'Không giới hạn' : `${remainingQuota} / ${AI_LIMIT} lượt còn lại hôm nay` }}</h2>
            <p class="mt-1 text-sm text-muted-foreground">{{ unlimited ? 'Premium và admin được sinh từ không giới hạn.' : 'Tài khoản miễn phí có 5 lượt sinh từ mỗi ngày, tự reset sang ngày mới.' }}</p>
          </div>
          <router-link v-if="!unlimited" to="/premium" class="inline-flex shrink-0 items-center justify-center rounded-full border-2 border-foreground bg-tertiary px-5 py-2.5 text-sm font-black shadow-pop-sm transition-transform hover:-translate-y-0.5">Mở khóa không giới hạn</router-link>
        </div>
        <div v-if="!unlimited" class="mt-4 h-3 overflow-hidden rounded-full border-2 border-foreground bg-muted" aria-hidden="true"><div class="h-full bg-accent transition-all" :style="{ width: `${quotaPercent}%` }"></div></div>
      </section>

      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl">
        <form @submit.prevent="generate" class="space-y-6">
          <div>
            <label for="ai-topic" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Chủ đề</label>
            <input id="ai-topic" v-model="topic" placeholder="e.g. Environment, Technology, Travel..." required
              class="w-full bg-input border-2 border-border rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-none focus:border-accent focus:shadow-pop-accent focus:outline-none placeholder:text-muted-foreground"
            />
          </div>

          <div>
            <label for="ai-level" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">CEFR Level</label>
            <select id="ai-level" v-model="level"
              class="w-full bg-input border-2 border-border rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-none focus:border-accent focus:shadow-pop-accent focus:outline-none"
            >
              <option v-for="lv in ['A1','A2','B1','B2','C1','C2']" :key="lv" :value="lv">{{ lv }}</option>
            </select>
          </div>

          <div>
            <label for="ai-count" class="block font-bold uppercase tracking-wider text-xs mb-1.5 text-foreground">Số lượng từ</label>
            <input id="ai-count" v-model.number="count" type="number" min="3" max="20" required
              class="w-full bg-input border-2 border-border rounded-sm px-4 py-3 font-sans text-base text-foreground transition-all duration-300 ease-bounce shadow-none focus:border-accent focus:shadow-pop-accent focus:outline-none"
            />
          </div>

          <p v-if="error" role="alert" class="font-bold text-xs uppercase tracking-wider text-danger-ink">{{ error }}</p>
          <p v-if="quotaExhausted" role="alert" class="border-2 border-secondary bg-secondary/10 p-3 text-sm font-bold">Bạn đã dùng hết {{ AI_LIMIT }} lượt sinh từ miễn phí hôm nay. Nâng cấp Premium để tiếp tục.</p>

          <AppButton type="submit" :disabled="generating || quotaExhausted" :loading="generating" variant="primary" size="lg" class="w-full">
            <span v-if="generating">Đang sinh...</span>
            <span v-else>Sinh từ vựng</span>
          </AppButton>
        </form>

        <!-- Results -->
        <div v-if="generatedWords.length > 0" class="mt-10 space-y-4">
          <div class="flex items-center justify-between">
            <h2 class="font-black text-xl uppercase tracking-tight">Kết quả</h2>
            <AppButton
              @click="saveAll"
              :disabled="savingAll || allSaved"
              :loading="savingAll"
              variant="emerald"
            >
              <span v-if="savingAll">Đang lưu...</span>
              <span v-else-if="allSaved">Đã lưu tất cả</span>
              <span v-else>Lưu tất cả ({{ unsavedCount }})</span>
            </AppButton>
          </div>

          <!-- audit-v11 F145: chọn bộ từ đích. Không có lựa chọn này thì từ sinh ra bị lưu
               vào bảng toàn cục, không thuộc bộ nào và người dùng không bao giờ mở lại được. -->
          <div class="mb-4 border-2 border-foreground rounded-md p-4 shadow-pop-sm">
            <label for="ai-save-deck" class="block font-black text-xs uppercase tracking-wider mb-2">
              Lưu vào bộ từ
            </label>
            <select
              id="ai-save-deck"
              v-model="targetDeckId"
              class="w-full border-2 border-border rounded-md p-2 font-bold text-sm bg-white focus:border-accent focus:shadow-pop-accent outline-none"
            >
              <option :value="null">— Chưa chọn bộ từ —</option>
              <option v-for="d in myDecks" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
            <p v-if="!myDecks.length" class="mt-2 text-xs font-bold text-muted-foreground">
              Bạn chưa có bộ từ nào.
              <router-link to="/decks/create" class="text-accent-ink underline underline-offset-2">Tạo bộ từ</router-link>
              trước, nếu không các từ vừa sinh sẽ không thuộc bộ nào và bạn không mở lại được.
            </p>
            <p v-else-if="!targetDeckId" class="mt-2 text-xs font-bold text-danger-ink">
              Chưa chọn bộ từ — các từ sẽ được lưu nhưng <strong>không thuộc bộ nào</strong>.
            </p>
          </div>

          <p v-if="saveError" role="alert" class="font-bold text-xs uppercase tracking-wider text-danger-ink">{{ saveError }}</p>
          <p v-if="saveSuccess" role="status" aria-live="polite" class="font-bold text-xs uppercase tracking-wider text-success-ink">{{ saveSuccess }}</p>

          <div v-for="(w, i) in generatedWords" :key="i"
            class="border-2 border-foreground rounded-md p-5 shadow-pop-lg"
          >
            <div class="flex items-start justify-between">
              <div>
                <h3 class="font-black text-lg uppercase">{{ w.word }}</h3>
                <p class="font-bold text-xs text-muted-foreground">{{ w.pronunciation || '/' + w.word + '/' }}</p>
              </div>
              <div class="flex items-center gap-2">
                <span v-if="savedIndexes.has(i)" class="px-2 py-0.5 bg-success/10 border border-success rounded-full text-xs font-bold text-success-ink">
                  Đã lưu
                </span>
                <span class="px-2 py-0.5 bg-accent/10 border-2 border-foreground rounded-full text-xs font-bold">{{ w.wordType }}</span>
              </div>
            </div>
            <div class="flex-1" v-html="sanitizeText(w.meaning || w.definitionVi)"></div>
            <p v-if="w.exampleSentence" class="text-sm text-muted-foreground italic mt-1">"<span v-html="sanitizeText(w.exampleSentence)"></span>"</p>

            <div class="mt-3 flex justify-end">
              <AppButton
                v-if="!savedIndexes.has(i)"
                @click="saveOne(i)"
                :disabled="savingIndex === i"
                :loading="savingIndex === i"
                :aria-label="'Lưu từ ' + w.word"
                variant="primary"
                size="sm"
              >
                {{ savingIndex === i ? 'Đang lưu...' : 'Lưu' }}
              </AppButton>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import AppButton from '@/components/ui/AppButton.vue'
import aiService from '@/services/aiService'
import deckService from '@/services/deckService'
import { useToast } from '@/composables/useToast'
import { useAuthStore } from '@/store/modules/auth'
import UserPageHeader from '@/components/common/UserPageHeader.vue'
import { sanitizeText } from '@/utils/markdown'

const AI_LIMIT = 5
const auth = useAuthStore()
const toast = useToast()
const topic = ref('')
const level = ref('B1')
const count = ref(10)
const generating = ref(false)
const error = ref('')
const generatedWords = ref([])
const savedIndexes = ref(new Set())
const savingIndex = ref(null)
const savingAll = ref(false)
const saveError = ref('')
const saveSuccess = ref('')
// audit-v11 F145: bộ từ đích để từ sinh ra thực sự vào được nơi người dùng mở lại được.
const myDecks = ref([])
const targetDeckId = ref(null)

async function loadMyDecks() {
  try {
    const data = await deckService.getMyDecks({ page: 0, size: 100 })
    myDecks.value = data?.content || data?.data?.content || []
    // Mặc định chọn bộ đầu tiên để người dùng không vô tình lưu vào khoảng không.
    if (!targetDeckId.value && myDecks.value.length) targetDeckId.value = myDecks.value[0].id
  } catch {
    myDecks.value = []
  }
}

const allSaved = computed(() => generatedWords.value.length > 0 && savedIndexes.value.size === generatedWords.value.length)
const unsavedCount = computed(() => generatedWords.value.length - savedIndexes.value.size)
// Quyền và hạn mức lấy thẳng từ server (admin có toàn quyền → coi như unlimited).
const unlimited = computed(() => auth.hasPremiumAccess)
const remainingQuota = computed(() => {
  if (unlimited.value) return AI_LIMIT
  const r = auth.user?.aiGenerationsRemainingToday
  return typeof r === 'number' ? Math.max(Math.min(r, AI_LIMIT), 0) : AI_LIMIT
})
const usedQuota = computed(() => AI_LIMIT - remainingQuota.value)
const quotaPercent = computed(() => (usedQuota.value / AI_LIMIT) * 100)
const quotaExhausted = computed(() => !unlimited.value && remainingQuota.value === 0)

async function generate() {
  error.value = ''
  generating.value = true
  generatedWords.value = []
  savedIndexes.value = new Set()
  saveError.value = ''
  saveSuccess.value = ''
  try {
    const result = await aiService.generateVocab(topic.value, level.value, count.value)
    generatedWords.value = result || []
    await auth.fetchUser()
  } catch (e) {
    error.value = e.response?.data?.detail || e.response?.data?.message || 'Sinh từ thất bại'
  } finally {
    generating.value = false
  }
}

async function saveOne(index) {
  savingIndex.value = index
  saveError.value = ''
  saveSuccess.value = ''
  try {
    const word = generatedWords.value[index]
    await aiService.saveVocab([word], targetDeckId.value)
    const newSet = new Set(savedIndexes.value)
    newSet.add(index)
    savedIndexes.value = newSet
    toast.success?.('Đã lưu từ "' + word.word + '"')
  } catch (e) {
    saveError.value = e.response?.data?.error || 'Lưu từ thất bại'
    toast.error?.('Lưu từ thất bại')
  } finally {
    savingIndex.value = null
  }
}

async function saveAll() {
  savingAll.value = true
  saveError.value = ''
  saveSuccess.value = ''
  try {
    const unsaved = generatedWords.value.filter((_, i) => !savedIndexes.value.has(i))
    if (unsaved.length === 0) return
    await aiService.saveVocab(unsaved, targetDeckId.value)
    const newSet = new Set(savedIndexes.value)
    for (let i = 0; i < generatedWords.value.length; i++) newSet.add(i)
    savedIndexes.value = newSet
    saveSuccess.value = targetDeckId.value
      ? 'Đã lưu ' + unsaved.length + ' từ vào bộ từ đã chọn.'
      : 'Đã lưu ' + unsaved.length + ' từ, nhưng CHƯA vào bộ từ nào.'
    toast.success?.('Đã lưu ' + unsaved.length + ' từ vựng')
  } catch (e) {
    saveError.value = e.response?.data?.error || 'Lưu tất cả thất bại'
    toast.error?.('Lưu tất cả thất bại')
  } finally {
    savingAll.value = false
  }
}

// audit-v11 F145: nạp danh sách bộ từ của người dùng để chọn đích lưu ngay khi vào trang.
onMounted(loadMyDecks)

onUnmounted(() => {
  savingIndex.value = null
  savingAll.value = false
})
</script>
