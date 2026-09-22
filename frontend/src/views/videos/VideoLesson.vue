<template>
  <div class="bg-background min-h-screen">
    <div class="bg-foreground border-b-2 border-foreground">
      <div class="max-w-7xl mx-auto px-6 py-3 flex items-center justify-between gap-4">
        <router-link to="/videos"
          class="text-white/80 hover:text-white font-bold text-[10px] uppercase tracking-widest border border-white/30 px-3 py-1 rounded-md hover:bg-white/10 transition-colors">
          ← Thư viện video
        </router-link>
        <div class="flex items-center gap-3 min-w-0">
          <span class="text-white/60 font-bold text-[10px] uppercase tracking-widest hidden sm:inline">Tiến độ</span>
          <div class="w-32 h-2 border border-white/30 bg-white/10 rounded-full overflow-hidden">
            <div class="h-full bg-accent transition-all duration-500" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <span class="text-white font-bold text-xs tabular-nums">{{ completedLines.length }}/{{ transcript.length }}</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="max-w-7xl mx-auto px-6 py-24 flex flex-col items-center gap-4" role="status">
      <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
      <p class="font-bold text-sm uppercase tracking-widest text-muted-foreground">Đang tải bài học...</p>
    </div>

    <div v-else-if="loadError" class="max-w-3xl mx-auto px-6 py-24 text-center" role="alert">
      <p class="font-black text-xl">{{ loadError }}</p>
      <AppButton class="mt-6" variant="secondary" @click="load">Thử lại</AppButton>
    </div>

    <main v-else-if="lesson" class="max-w-7xl mx-auto px-4 sm:px-6 py-8">
      <div class="grid grid-cols-1 lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)] gap-8">
        <!-- LEFT: player + tabs -->
        <section>
          <h1 class="font-black text-2xl md:text-3xl uppercase leading-tight">{{ lesson.title }}</h1>
          <p class="font-bold text-xs uppercase tracking-wider text-muted-foreground mt-2">
            {{ levelLabel(lesson.level) }} · {{ transcript.length }} câu luyện
          </p>

          <div ref="playerHost" class="mt-6 border-2 border-foreground bg-black rounded-md overflow-hidden shadow-pop-xl aspect-video"></div>
          <p v-if="playerError && !playerReady" class="mt-3 border-l-4 border-danger bg-danger/10 p-3 text-sm font-bold text-danger-ink" role="alert">
            {{ playerError }}
          </p>

          <!-- Mode tabs -->
          <div class="mt-6 flex gap-2" role="tablist" aria-label="Chế độ luyện tập">
            <button
              v-for="tab in tabs" :key="tab.id"
              role="tab" :aria-selected="activeTab === tab.id"
              class="px-5 py-2.5 border-2 border-foreground rounded-md font-black text-xs uppercase tracking-wider transition-all"
              :class="activeTab === tab.id ? 'bg-accent-strong text-white shadow-pop-sm' : 'bg-card hover:bg-tertiary/30'"
              @click="activeTab = tab.id"
            >{{ tab.label }}</button>
          </div>

          <!-- Transcript mode -->
          <div v-if="activeTab === 'transcript'" class="mt-6 border-2 border-foreground bg-card rounded-md shadow-pop-xl">
            <div class="flex items-center justify-between px-5 py-3 border-b-2 border-foreground bg-muted/40">
              <span class="font-black text-xs uppercase tracking-widest">Phụ đề tương tác</span>
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" v-model="showTranslation" class="w-4 h-4 accent-accent" />
                <span class="font-bold text-xs uppercase tracking-wider">Dịch nghĩa</span>
              </label>
            </div>
            <ol class="max-h-[28rem] overflow-y-auto divide-y divide-foreground/10">
              <li v-for="(line, idx) in transcript" :key="idx"
                class="px-5 py-3 cursor-pointer transition-colors hover:bg-tertiary/20"
                :class="{ 'bg-accent/10 border-l-4 border-accent': activeLine === idx }"
                @click="seekLine(idx)">
                <div class="flex items-start gap-3">
                  <span class="text-[10px] font-black tabular-nums text-muted-foreground mt-1 w-10 shrink-0">{{ formatTime(line.start) }}</span>
                  <div class="min-w-0">
                    <p class="font-semibold leading-relaxed">
                      <template v-for="(word, wi) in splitWords(line.textEn)" :key="wi">
                        <button
                          class="hover:bg-tertiary/50 rounded px-0.5 transition-colors"
                          :class="{ 'font-black underline decoration-accent decoration-2 underline-offset-2': completedLines.includes(idx) }"
                          @click.stop="lookupWord(word, idx)"
                        >{{ word }}</button>{{ ' ' }}
                      </template>
                    </p>
                    <p v-if="showTranslation && line.textVi" class="text-sm text-muted-foreground mt-1 leading-relaxed">{{ line.textVi }}</p>
                  </div>
                </div>
              </li>
            </ol>
          </div>

          <!-- Shadowing mode -->
          <div v-else-if="activeTab === 'shadowing'" class="mt-6 border-2 border-foreground bg-card rounded-md shadow-pop-xl p-6">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="font-black text-xs uppercase tracking-widest text-muted-foreground">Luyện nói theo câu</p>
                <h2 class="font-black text-xl mt-1">Câu {{ shadowIndex + 1 }}/{{ transcript.length }}</h2>
              </div>
              <div class="flex gap-2">
                <AppButton variant="secondary" size="sm" :disabled="shadowIndex === 0" @click="gotoShadow(shadowIndex - 1)">← Trước</AppButton>
                <AppButton variant="secondary" size="sm" :disabled="shadowIndex >= transcript.length - 1" @click="gotoShadow(shadowIndex + 1)">Sau →</AppButton>
              </div>
            </div>

            <blockquote class="mt-5 border-l-4 border-accent bg-muted/60 p-4 text-lg font-semibold leading-relaxed">
              {{ shadowLine?.textEn }}
            </blockquote>
            <p v-if="showTranslation && shadowLine?.textVi" class="mt-2 text-sm text-muted-foreground">{{ shadowLine?.textVi }}</p>

            <div class="mt-6 flex flex-wrap gap-3">
              <AppButton variant="tertiary" @click="loopLine">🔁 Nghe lặp lại (×{{ loopCount }})</AppButton>
              <template v-if="!rec.isRecording.value && !rec.previewUrl.value">
                <AppButton v-if="!rec.isReady.value" variant="primary" @click="prepareMic">🎤 Bật micro</AppButton>
                <AppButton v-else variant="primary" @click="startRecord">● Bắt đầu ghi</AppButton>
              </template>
              <AppButton v-else-if="rec.isRecording.value" variant="featured" @click="rec.stop()">■ Dừng ({{ rec.elapsedSeconds.value }}s)</AppButton>
            </div>
            <p v-if="rec.error.value || actionError" class="mt-3 border-l-4 border-danger bg-danger/10 p-3 text-sm font-bold text-danger-ink" role="alert">
              {{ rec.error.value || actionError }}
            </p>

            <div v-if="rec.previewUrl.value && !attemptResult" class="mt-6 border-2 border-foreground bg-muted/60 p-5 rounded-md">
              <h3 class="font-black">Nghe lại trước khi nộp</h3>
              <audio :src="rec.previewUrl.value" class="mt-3 w-full" controls @loadedmetadata="fixWebmDuration" />
              <div class="mt-4 flex flex-wrap gap-3">
                <AppButton variant="secondary" @click="recordAgain">Ghi lại</AppButton>
                <AppButton variant="primary" :disabled="submitting" @click="submitAttempt">
                  {{ submitting ? 'Đang gửi...' : 'Nộp cho giáo viên chấm' }}
                </AppButton>
              </div>
            </div>

            <div v-if="attemptResult" class="mt-6 border-2 border-foreground bg-tertiary/20 p-5 rounded-md" role="status">
              <p class="font-black text-success-ink uppercase text-xs tracking-widest">Đã nộp câu {{ shadowIndex + 1 }}</p>
              <p class="mt-1 text-sm font-medium">Bài của bạn sẽ được giáo viên chấm (thang 10) và có nhận xét ngay tại đây.</p>
              <AppButton class="mt-4" variant="secondary" size="sm" @click="attemptResult = null; recordAgain()">Tiếp tục câu khác</AppButton>
            </div>

            <!-- Previously submitted attempt for this sentence, with teacher feedback -->
            <div v-else-if="shadowAttempt && !rec.previewUrl.value" class="mt-6 border-2 border-foreground p-5 rounded-md"
              :class="shadowAttempt.status === 'GRADED' ? 'bg-success/10 border-success' : 'bg-muted/60'">
              <div class="flex flex-wrap items-center justify-between gap-3">
                <p class="font-black uppercase text-xs tracking-widest"
                  :class="shadowAttempt.status === 'GRADED' ? 'text-success-ink' : 'text-muted-foreground'">
                  {{ shadowAttempt.status === 'GRADED' ? 'Đã chấm' : 'Đã nộp — đang chờ giáo viên' }}
                </p>
                <span v-if="shadowAttempt.status === 'GRADED'" class="font-black text-2xl tabular-nums">
                  {{ Number(shadowAttempt.score).toFixed(1) }}<span class="text-sm text-muted-foreground">/10</span>
                </span>
              </div>
              <p v-if="shadowAttempt.status === 'GRADED' && shadowAttempt.adminFeedback" class="mt-2 text-sm font-medium leading-relaxed">
                💬 {{ shadowAttempt.adminFeedback }}
              </p>
              <audio v-if="shadowAttempt.mediaUrl" :src="resolveMediaUrl(shadowAttempt.mediaUrl)" controls class="mt-3 w-full h-10" @loadedmetadata="fixWebmDuration" />
              <AppButton class="mt-4" variant="secondary" size="sm" @click="gotoShadow(shadowIndex + 1)"
                :disabled="shadowIndex >= transcript.length - 1">Làm câu tiếp theo →</AppButton>
            </div>
          </div>

          <!-- Quiz mode -->
          <div v-else-if="activeTab === 'quiz'" class="mt-6 border-2 border-foreground bg-card rounded-md shadow-pop-xl p-6">
            <p class="font-black text-xs uppercase tracking-widest text-muted-foreground">Kiểm tra nội dung video</p>
            <div v-if="quiz.length === 0" class="py-10 text-center">
              <p class="font-bold text-muted-foreground">Chưa có câu hỏi cho video này.</p>
            </div>
            <div v-else>
              <div v-for="(q, qi) in quiz" :key="qi" class="border-b-2 border-foreground/10 py-5 last:border-0">
                <p class="font-bold">{{ qi + 1 }}. {{ q.question }}</p>
                <div class="mt-3 grid gap-2">
                  <label v-for="(opt, oi) in q.options" :key="oi"
                    class="flex items-center gap-3 border-2 border-foreground rounded-md px-4 py-2 cursor-pointer transition-colors"
                    :class="quizAnswered[qi] === oi
                      ? (oi === q.correct ? 'bg-success/20 border-success' : 'bg-danger/20 border-danger')
                      : (quizAnswered[qi] != null && oi === q.correct ? 'bg-success/20' : 'hover:bg-tertiary/20')">
                    <input type="radio" :name="`quiz-${qi}`" :value="oi" :disabled="quizAnswered[qi] != null"
                      class="w-4 h-4 accent-accent" @change="answerQuiz(qi, oi)" />
                    <span class="font-medium text-sm">{{ opt }}</span>
                  </label>
                </div>
                <p v-if="quizAnswered[qi] != null" class="mt-2 text-xs font-black uppercase tracking-wider"
                  :class="quizAnswered[qi] === q.correct ? 'text-success-ink' : 'text-danger-ink'">
                  {{ quizAnswered[qi] === q.correct ? 'Chính xác!' : 'Chưa đúng — đáp án: ' + q.options[q.correct] }}
                </p>
              </div>
              <div class="mt-5 flex items-center justify-between border-t-2 border-foreground pt-4">
                <span class="font-black uppercase text-sm tracking-wider">Điểm: {{ quizScore }}/{{ quiz.length }}</span>
                <AppButton variant="secondary" size="sm" @click="resetQuiz">Làm lại</AppButton>
              </div>
            </div>
          </div>
        </section>

        <!-- RIGHT: word lookup panel -->
        <aside class="lg:sticky lg:top-6 self-start space-y-6">
          <div class="border-2 border-foreground bg-card rounded-md shadow-pop-xl p-6">
            <p class="font-black text-xs uppercase tracking-widest text-muted-foreground">Tra từ nhanh</p>
            <div v-if="!lookupWordInfo" class="py-8 text-center">
              <p class="font-medium text-sm text-muted-foreground leading-relaxed">
                Bấm vào bất kỳ từ nào trong phụ đề để xem nghĩa, phiên âm IPA và lưu vào bộ từ của bạn.
              </p>
            </div>
            <template v-else>
              <div class="mt-3 flex items-baseline gap-3 flex-wrap">
                <h2 class="font-black text-3xl uppercase">{{ lookupWordInfo.word }}</h2>
                <span v-if="lookupWordInfo.phonetic" class="font-bold text-accent-ink">{{ lookupWordInfo.phonetic }}</span>
                <button v-if="lookupWordInfo.audioUrl" @click="playAudio"
                  class="w-8 h-8 border-2 border-foreground rounded-full flex items-center justify-center hover:bg-tertiary/30" aria-label="Nghe phát âm">🔊</button>
              </div>
              <div v-if="lookupWordLoading" class="py-6 text-center text-sm font-bold text-muted-foreground">Đang tra...</div>
              <div v-else-if="lookupWordError" class="py-6 text-center text-sm font-bold text-danger-ink">{{ lookupWordError }}</div>
              <div v-else class="mt-4 space-y-4">
                <div v-for="(m, mi) in lookupWordInfo.meanings.slice(0, 3)" :key="mi">
                  <p class="text-xs font-black uppercase tracking-wider text-muted-foreground">{{ m.partOfSpeech }}</p>
                  <p class="font-medium text-sm mt-1 leading-relaxed">• {{ m.definition }}</p>
                  <p v-if="m.example" class="text-xs italic text-muted-foreground mt-1">"{{ m.example }}"</p>
                </div>
                <div v-if="auth.isLoggedIn" class="border-t-2 border-foreground/10 pt-4">
                  <label for="save-deck" class="block font-bold uppercase tracking-wider text-[10px] mb-1.5">Lưu vào bộ từ</label>
                  <div class="flex gap-2">
                    <select id="save-deck" v-model="selectedDeckId" class="flex-1 min-w-0 bg-input border-2 border-border rounded-sm px-3 py-2 text-sm font-bold">
                      <option :value="null">Chọn bộ từ...</option>
                      <option v-for="deck in decks" :key="deck.id" :value="deck.id">{{ deck.name }}</option>
                    </select>
                    <AppButton variant="tertiary" size="sm" :disabled="!selectedDeckId || savingWord" @click="saveWordToDeck">
                      {{ savingWord ? '...' : 'Lưu' }}
                    </AppButton>
                  </div>
                  <p v-if="saveWordMessage" class="mt-2 text-xs font-bold" :class="saveWordError ? 'text-danger-ink' : 'text-success-ink'">{{ saveWordMessage }}</p>
                </div>
                <p v-else class="text-xs font-bold text-muted-foreground">
                  <router-link to="/login" class="underline text-accent-ink">Đăng nhập</router-link> để lưu từ vào bộ ôn tập.
                </p>
              </div>
            </template>
          </div>

          <div class="border-2 border-foreground bg-tertiary/20 rounded-md p-5 text-sm leading-relaxed">
            <h2 class="font-black uppercase text-xs tracking-widest mb-3">Phương pháp Shadowing</h2>
            <p class="font-medium text-muted-foreground">
              Nghe trước — nói đuổi theo ngay sau người bản xứ, bắt chước cả ngữ điệu và nhịp điệu.
              Luyện {{ Math.max(5, Math.min(30, transcript.length)) }} phút mỗi ngày, thường xuyên quan trọng hơn nhiều.
            </p>
            <p class="font-medium text-muted-foreground mt-2">
              Nguồn: Kadota (2019), <em>Shadowing as a Practice in Second Language Acquisition</em> — Routledge.
            </p>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import videoLessonService from '@/services/videoLessonService'
import vocabularyService from '@/services/vocabularyService'
import deckService from '@/services/deckService'
import { useYouTubePlayer } from '@/composables/useYouTubePlayer'
import { useSpeakingRecorder } from '@/composables/useSpeakingRecorder'
import { useAuthStore } from '@/store/modules/auth'
import { AppButton } from '@/components/ui'
import { levelLabel } from '@/utils/lessonLevels'
// audit-v11 F144: shared, unit-tested duration probe (the local copy played audio at 16x).
import { fixWebmDuration } from '@/utils/webmDuration'

const route = useRoute()
const auth = useAuthStore()

const tabs = [
  { id: 'transcript', label: 'Phụ đề' },
  { id: 'shadowing', label: 'Shadowing' },
  { id: 'quiz', label: 'Quiz' }
]

const lesson = ref(null)
const transcript = ref([])
const completedLines = ref([])
const loading = ref(true)
const loadError = ref('')
const activeTab = ref('transcript')
const showTranslation = ref(true)
const activeLine = ref(-1)
const shadowIndex = ref(0)
const loopCount = ref(2)

const { error: playerError, ready: playerReady, currentTime, create, seekTo, pause } = useYouTubePlayer()
const playerHost = ref(null)
const rec = useSpeakingRecorder(30)

const submitting = ref(false)
const actionError = ref('')
const attemptResult = ref(null)
// Full attempt history keyed by line index, so a graded submission can show its
// score + teacher feedback right on the sentence card instead of only in the
// speaking history page.
const attemptsByLine = ref({})

const lookupWordInfo = ref(null)
const lookupWordLoading = ref(false)
const lookupWordError = ref('')
const decks = ref([])
const selectedDeckId = ref(null)
const savingWord = ref(false)
const saveWordMessage = ref('')
const saveWordError = ref(false)

const quiz = ref([])
const quizAnswered = ref({})
const quizScore = computed(() =>
  Object.entries(quizAnswered.value).filter(([qi, oi]) => quiz.value[qi] && oi === quiz.value[qi].correct).length)

const shadowLine = computed(() => transcript.value[shadowIndex.value])
const shadowAttempt = computed(() => attemptsByLine.value[shadowIndex.value] || null)
const progressPercent = computed(() =>
  transcript.value.length ? Math.round((completedLines.value.length / transcript.value.length) * 100) : 0)

watch(currentTime, (t) => {
  for (let i = 0; i < transcript.value.length; i++) {
    const line = transcript.value[i]
    if (t >= line.start && t < line.end) {
      if (activeLine.value !== i) activeLine.value = i
      return
    }
  }
})

load()

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    lesson.value = await videoLessonService.getById(route.params.id)
    transcript.value = lesson.value.transcript || []
    completedLines.value = lesson.value.completedLines || []
    await loadAttempts()
    buildQuiz()
    // The player host lives inside `v-else-if="lesson"`, which only renders once
    // the loading branch is gone. Clear the flag and wait for the DOM update
    // before handing the element to the YouTube API.
    loading.value = false
    await nextTick()
    await create(playerHost.value, lesson.value.youtubeVideoId)
  } catch (e) {
    loadError.value = e.response?.data?.detail || 'Không tải được bài học video.'
  } finally {
    loading.value = false
  }
}

/** Fetch this user's attempts for the current lesson (best-effort: guests have none). */
async function loadAttempts() {
  attemptsByLine.value = {}
  if (!auth.isLoggedIn) return
  try {
    const page = await videoLessonService.myAttempts(0, 100)
    for (const a of page.content || []) {
      if (a.videoLessonId !== lesson.value.id) continue
      const prev = attemptsByLine.value[a.lineIndex]
      // Keep the most informative record: graded beats pending, newer wins ties.
      if (!prev || (a.status === 'GRADED' && prev.status !== 'GRADED') ||
          (a.status === prev.status && a.id > prev.id)) {
        attemptsByLine.value[a.lineIndex] = a
      }
    }
  } catch { /* attempt history is optional context, never block the lesson */ }
}

function splitWords(text) {
  return (text || '').split(/\s+/).filter(Boolean)
}

function seekLine(idx) {
  activeLine.value = idx
  seekTo(transcript.value[idx].start)
}

function gotoShadow(idx) {
  if (idx < 0 || idx >= transcript.value.length) return
  shadowIndex.value = idx
  attemptResult.value = null
  rec.clearRecording()
  seekLine(idx)
}

function loopLine() {
  const line = shadowLine.value
  if (!line) return
  pause()
  let played = 0
  const playOnce = () => {
    seekTo(line.start)
    played += 1
    if (played < loopCount.value) {
      const check = window.setInterval(() => {
        if (currentTime.value >= line.end - 0.15) {
          window.clearInterval(check)
          pause()
          window.setTimeout(playOnce, 400)
        }
      }, 200)
    }
  }
  playOnce()
}

async function prepareMic() {
  await rec.prepare()
}

function startRecord() {
  pause()
  rec.start()
}

function recordAgain() {
  rec.clearRecording()
  actionError.value = ''
}

async function submitAttempt() {
  if (!rec.blob.value) return
  submitting.value = true
  actionError.value = ''
  const ext = rec.blob.value.type.includes('ogg') ? 'ogg' : 'webm'
  const file = new File([rec.blob.value], `shadow-${lesson.value.id}-line-${shadowIndex.value}.${ext}`, { type: rec.blob.value.type })
  try {
    attemptResult.value = await videoLessonService.submitAttempt(lesson.value.id, shadowIndex.value, file)
    attemptsByLine.value = { ...attemptsByLine.value, [shadowIndex.value]: attemptResult.value }
    if (!completedLines.value.includes(shadowIndex.value)) {
      completedLines.value = [...completedLines.value, shadowIndex.value]
    }
  } catch (e) {
    actionError.value = e.response?.data?.detail || e.response?.data?.message || 'Không nộp được bản ghi. Bạn đã đăng nhập chưa?'
  } finally {
    submitting.value = false
  }
}

async function lookupWord(rawWord, lineIdx) {
  const word = rawWord.replace(/^[^\p{L}']+|[^\p{L}']+$/gu, '').toLowerCase()
  if (!word) return
  lookupWordInfo.value = { word, phonetic: '', audioUrl: '', meanings: [] }
  lookupWordLoading.value = true
  lookupWordError.value = ''
  saveWordMessage.value = ''
  try {
    const results = await vocabularyService.search(word)
    const entry = results[0]
    if (!entry) {
      lookupWordError.value = `Không tìm thấy "${word}" trong từ điển.`
      return
    }
    lookupWordInfo.value = {
      word: entry.word,
      phonetic: entry.phonetic || entry.pronunciation || '',
      audioUrl: entry.audioUrl || '',
      meanings: (entry.meanings || []).flatMap(m =>
        (m.definitions || []).slice(0, 1).map(d => ({ partOfSpeech: m.partOfSpeech, definition: d.definition, example: d.example })))
    }
    if (auth.isLoggedIn && decks.value.length === 0) {
      const data = await deckService.getMyDecks({ size: 50 })
      decks.value = data.content || []
    }
  } catch (e) {
    lookupWordError.value = e.message === 'TIMEOUT' ? 'Tra từ quá thời gian chờ.' : 'Không tra được từ (mất mạng?).'
  } finally {
    lookupWordLoading.value = false
  }
}

function playAudio() {
  if (lookupWordInfo.value?.audioUrl) new Audio(lookupWordInfo.value.audioUrl).play().catch(() => {})
}

async function saveWordToDeck() {
  if (!selectedDeckId.value || !lookupWordInfo.value) return
  savingWord.value = true
  saveWordMessage.value = ''
  saveWordError.value = false
  try {
    // audit-v12 F147: ONE call. The server creates the word AND links it to the deck in a
    // single transaction, with an ownership check. Before this, the second call
    // (deckService.addWordToDeck) was a separate request that could fail and leave the word
    // orphaned in the shared dictionary — and nothing verified the deck was the caller's.
    await vocabularyService.create({
      word: lookupWordInfo.value.word,
      pronunciation: lookupWordInfo.value.phonetic,
      wordType: lookupWordInfo.value.meanings[0]?.partOfSpeech || '',
      meaning: lookupWordInfo.value.meanings[0]?.definition || lookupWordInfo.value.word,
      definitionEn: lookupWordInfo.value.meanings[0]?.definition || '',
      exampleSentence: lookupWordInfo.value.meanings[0]?.example || '',
      source: 'VIDEO_LESSON'
    }, selectedDeckId.value)
    saveWordMessage.value = `Đã lưu "${lookupWordInfo.value.word}" vào bộ từ.`
  } catch (e) {
    saveWordError.value = true
    saveWordMessage.value = e.response?.data?.message || 'Không lưu được từ.'
  } finally {
    savingWord.value = false
  }
}

function buildQuiz() {
  const lines = transcript.value
  if (lines.length < 4) { quiz.value = []; return }
  const step = Math.max(1, Math.floor(lines.length / 5))
  const items = []
  for (let i = 0; i < lines.length && items.length < 5; i += step) {
    const correct = lines[i].textEn
    const distractors = lines
      .map(l => l.textEn)
      .filter(t => t !== correct)
      .sort(() => Math.random() - 0.5)
      .slice(0, 3)
    const options = [correct, ...distractors].sort(() => Math.random() - 0.5)
    items.push({
      question: `Nghe và chọn đúng câu người nói đã nói (đoạn ${formatTime(lines[i].start)}):`,
      options,
      correct: options.indexOf(correct),
      lineStart: lines[i].start
    })
  }
  quiz.value = items
  quizAnswered.value = {}
}

function answerQuiz(qi, oi) {
  quizAnswered.value = { ...quizAnswered.value, [qi]: oi }
  const q = quiz.value[qi]
  if (q) seekTo(q.lineStart)
}

function resetQuiz() {
  quizAnswered.value = {}
  buildQuiz()
}

function formatTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

/**
 * Duration handling for MediaRecorder webm moved to `@/utils/webmDuration` (F144).
 * This copy had the SAME bug as the admin page: `playbackRate = 16` + `play()` made
 * the student's own recording audibly play at 16x. The module is unit-tested.
 */
</script>
