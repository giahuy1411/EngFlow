<template>
  <div class="max-w-6xl mx-auto px-4 py-10" v-if="lesson">
    <div class="flex items-center gap-6 mb-6">
      <router-link to="/lessons" class="inline-flex items-center gap-2 font-sans font-bold text-sm uppercase tracking-wider text-foreground hover:text-primary-red">
        ← Quay lại danh sách bài học
      </router-link>
      
      <button @click="activeTab = 'practice'" class="bg-primary-red text-white border-2 border-black font-bold uppercase text-xs tracking-wider px-4 py-2 shadow-hard-sm hover:bg-primary-red/90 transition-all active:translate-x-[2px] active:translate-y-[2px] active:shadow-none">
        Chuyển đến Bài tập 🏆
      </button>
    </div>

    <!-- Header -->
    <BauhausCard decoration decorationColor="yellow" decorationShape="square" class="mb-6">
      <div class="flex justify-between items-start mb-4">
        <span class="bg-primary-red text-white font-bold text-xs uppercase tracking-wider px-4 py-1.5 border-2 border-black">{{ lesson.level }}</span>
        <span class="flex items-center gap-1 font-sans font-bold text-sm uppercase tracking-wider text-foreground">
          <ClockIcon class="w-4 h-4" />{{ lesson.durationMinutes }} phút
        </span>
      </div>
      <h3 class="font-bold text-3xl mb-2">{{ lesson.title }}</h3>
      <p class="font-sans text-foreground/70 mb-0">{{ lesson.description }}</p>
    </BauhausCard>

    <!-- Navigation Tabs -->
    <div class="flex flex-wrap gap-3 mb-6 border-b-2 border-black pb-2">
      <BauhausButton :variant="activeTab === 'vocab' ? 'primary' : 'outline'" size="sm" @click="activeTab = 'vocab'">
        ⚡ Từ vựng ({{ lesson.vocabularies?.length || 0 }})
      </BauhausButton>
      <BauhausButton :variant="activeTab === 'grammar' ? 'primary' : 'outline'" size="sm" @click="activeTab = 'grammar'">
        📝 Ngữ pháp
      </BauhausButton>
      <BauhausButton :variant="activeTab === 'practice' ? 'primary' : 'outline'" size="sm" @click="activeTab = 'practice'">
        🏆 Bài tập ({{ lesson.exercises?.length || 0 }})
      </BauhausButton>
    </div>

    <!-- Tab Contents -->
    <div>
      <!-- 1. Vocabulary Tab -->
      <div v-if="activeTab === 'vocab'" class="text-center">
        <div v-if="!lesson.vocabularies || lesson.vocabularies.length === 0" class="border-4 border-black border-dashed bg-white p-8 text-center">
          <p class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/70">Bài học này chưa có từ vựng.</p>
        </div>
        <div v-else class="max-w-2xl mx-auto">
          <!-- Flashcard -->
          <div class="flashcard-card mb-6" :class="{ 'flipped': isFlipped }" @click="isFlipped = !isFlipped" role="button" tabindex="0" @keydown.enter="isFlipped = !isFlipped" @keydown.space.prevent="isFlipped = !isFlipped">
            <div class="flashcard-inner">
              <!-- Front Side -->
              <div class="flashcard-front border-4 border-black shadow-hard-lg flex flex-col items-center justify-center text-center" :class="frontBgClass">
                <div v-if="vocabLoading" class="flex items-center justify-center w-full h-full">
                  <LoaderIcon class="w-8 h-8 animate-spin text-foreground" />
                </div>
                <div v-else-if="vocabDetail" :class="frontBgClass" class="w-full h-full flex flex-col items-center justify-center p-8 text-center bg-transparent">
                  <span class="bg-primary-blue text-white font-bold text-xs uppercase tracking-wider px-3 py-1 border-2 border-black mb-4 inline-block">{{ vocabDetail.wordType }}</span>
                  <h3 class="font-bold text-5xl mb-4">{{ vocabDetail.word }}</h3>
                  <BauhausButton variant="outline" size="sm" @click.stop="speakWord(vocabDetail.word)" title="Phát âm">
                    🔊 Phát âm
                  </BauhausButton>
                  <p class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/50 mt-6 mb-0">Nhấp vào thẻ để xem nghĩa</p>
                </div>
              </div>
              <!-- Back Side -->
              <div class="flashcard-back bg-primary-yellow border-4 border-black shadow-hard-lg flex flex-col items-center justify-center p-8 text-center">
                <div v-if="vocabLoading" class="flex items-center justify-center">
                  <LoaderIcon class="w-8 h-8 animate-spin text-foreground" />
                </div>
                <div v-else-if="vocabDetail">
                  <div class="flex items-center justify-center gap-3 mb-3">
                    <h3 class="font-bold text-3xl mb-0 text-foreground">{{ vocabDetail.pronunciation }}</h3>
                    <BauhausButton variant="ghost" size="sm" @click.stop="speakWord(vocabDetail.word)" title="Phát âm">
                      🔊
                    </BauhausButton>
                  </div>
                  <div class="font-sans font-bold text-lg mb-4 px-4 text-foreground/80 bauhaus-markdown" v-html="parseMarkdown(vocabDetail.meaning)"></div>
                  <div v-if="vocabDetail.exampleSentence" class="bg-white/50 border-2 border-black p-4 text-left w-full">
                    <span class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/70">Ví dụ:</span>
                    <div class="font-sans text-foreground mb-0 mt-1 bauhaus-markdown" v-html="parseMarkdown(vocabDetail.exampleSentence)"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Flashcard Navigation -->
          <div class="flex justify-between items-center mb-6">
            <BauhausButton variant="outline" size="md" :disabled="currentVocabIndex === 0" @click="prevVocab">
              ← Trước
            </BauhausButton>
            <div class="text-center">
              <div class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/70 mb-1">
                Từ {{ currentVocabIndex + 1 }} / {{ lesson.vocabularies.length }}
              </div>
              <span v-if="masteryLevel === 0" class="inline-block bg-foreground/20 text-foreground font-bold text-xs uppercase tracking-wider px-3 py-1 border-2 border-black">Mới</span>
              <span v-else-if="masteryLevel === 1" class="inline-block bg-primary-blue text-white font-bold text-xs uppercase tracking-wider px-3 py-1 border-2 border-black">Đang học</span>
              <span v-else-if="masteryLevel === 2" class="inline-block bg-primary-yellow text-black font-bold text-xs uppercase tracking-wider px-3 py-1 border-2 border-black">Gần thuộc</span>
              <span v-else-if="masteryLevel >= 3" class="inline-block bg-primary-red text-white font-bold text-xs uppercase tracking-wider px-3 py-1 border-2 border-black">Đã thuộc</span>
            </div>
            <BauhausButton variant="outline" size="md" :disabled="currentVocabIndex === lesson.vocabularies.length - 1" @click="nextVocab">
              Tiếp theo →
            </BauhausButton>
          </div>

          <!-- SRS Review Actions -->
          <div class="flex justify-center gap-4">
            <BauhausButton variant="outline" size="md" @click="reviewVocab(false)">
              ❌ Chưa thuộc
            </BauhausButton>
            <BauhausButton variant="primary" size="md" @click="reviewVocab(true)">
              ✅ Đã thuộc
            </BauhausButton>
          </div>
        </div>
      </div>

      <!-- 2. Grammar Tab -->
      <div v-if="activeTab === 'grammar'" class="space-y-6">
        <div v-if="!lesson.grammars || lesson.grammars.length === 0" class="border-4 border-black border-dashed bg-white p-8 text-center">
          <p class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/70">Bài học này chưa có phần ngữ pháp.</p>
        </div>
        <div v-else v-for="gram in lesson.grammars" :key="gram.id">
          <BauhausCard class="p-6">
            <h4 class="font-bold text-2xl mb-4">{{ gram.title }}</h4>
            <div class="bauhaus-markdown mb-6" v-html="parseMarkdown(gram.explanation)"></div>

            <div v-if="gram.formula" class="bg-black/5 border-2 border-black p-5 mb-6">
              <h5 class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/70 mb-2">Công thức:</h5>
              <div class="bauhaus-markdown mb-0" v-html="parseMarkdown('```\n' + gram.formula + '\n```')"></div>
            </div>

            <div v-if="gram.examples">
              <h5 class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/70 mb-3">Ví dụ minh họa:</h5>
              <div class="bauhaus-markdown" v-html="parseMarkdown(gram.examples)"></div>
            </div>
          </BauhausCard>
        </div>
      </div>

      <!-- 3. Practice Tab -->
      <div v-if="activeTab === 'practice'" class="space-y-6">
        <div v-if="!lesson.exercises || lesson.exercises.length === 0" class="border-4 border-black border-dashed bg-white p-8 text-center">
          <p class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/70">Bài học này chưa có câu hỏi thực hành.</p>
        </div>
        <div v-else class="max-w-3xl mx-auto space-y-6">
          <div v-for="(ex, idx) in lesson.exercises" :key="ex.id">
            <BauhausCard class="p-6">
              <div class="flex justify-between items-center mb-4">
                <span class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/70">Câu {{ idx + 1 }}</span>
                <span class="bg-foreground/10 text-foreground font-bold text-xs uppercase tracking-wider px-3 py-1 border-2 border-black">+{{ ex.points }} điểm</span>
              </div>

              <div class="bauhaus-markdown mb-5" v-html="parseMarkdown(ex.question)"></div>

              <!-- Audio Player -->
              <div v-if="ex.audioUrl" class="mb-5">
                <audio controls class="w-full border-2 border-black bg-black/5 bauhaus-audio">
                  <source :src="ex.audioUrl" type="audio/mpeg">
                  Trình duyệt của bạn không hỗ trợ thẻ audio.
                </audio>
              </div>

              <!-- Multiple Choice Input -->
              <div v-if="ex.exerciseType === 'MULTIPLE_CHOICE'" class="space-y-3 mb-5">
                <div v-for="(option, optIdx) in parseOptions(ex.options)" :key="optIdx"
                     class="border-2 border-black p-4 cursor-pointer transition-colors duration-200 hover:bg-gray-100"
                     :class="optionClass(ex.id, optIdx, ex.correctAnswer)">
                  <label class="flex items-center gap-3 font-sans font-medium text-foreground cursor-pointer w-full" :for="`opt-${ex.id}-${optIdx}`">
                    <input class="bauhaus-radio" type="radio"
                           :id="`opt-${ex.id}-${optIdx}`" :name="`ex-${ex.id}`" :value="String(optIdx)"
                           v-model="answers[ex.id]" :disabled="results[ex.id] !== undefined" />
                    {{ option }}
                  </label>
                </div>
              </div>

              <!-- Fill In Blank Input -->
              <div v-else-if="ex.exerciseType === 'FILL_IN_BLANK'" class="mb-5">
                <input v-model="answers[ex.id]" type="text" class="w-full border-2 border-black bg-background p-4 font-sans text-foreground placeholder:text-foreground/40 focus:outline-none focus:border-primary-red"
                       placeholder="Nhập câu trả lời tại đây..." :disabled="results[ex.id] !== undefined" />
              </div>

              <!-- Submit Button & Feedback -->
              <div>
                <button v-if="results[ex.id] === undefined"
                        class="inline-flex items-center justify-center font-bold uppercase tracking-wider border-2 border-black px-6 py-2.5 text-base bg-primary-red text-white shadow-hard-sm hover:bg-primary-red/90 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all duration-200 ease-out cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed bauhaus-btn-press"
                        :disabled="!answers[ex.id]" @click="submitAnswer(ex.id)">
                  Nộp câu trả lời
                </button>
                <div v-else class="w-full space-y-4">
                  <div class="flex items-center gap-3">
                    <span class="text-2xl">{{ results[ex.id] ? '✅' : '❌' }}</span>
                    <span class="font-bold" :class="results[ex.id] ? 'text-primary-red' : 'text-foreground'">
                      {{ results[ex.id] ? 'Chính xác!' : `Sai rồi! Đáp án đúng: ${displayCorrectAnswer(ex)}` }}
                    </span>
                  </div>
                  <div class="bg-black/5 border-2 border-black p-4">
                    <span class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/70">Giải thích:</span>
                    <div class="font-sans text-foreground/80 mb-0 mt-1 bauhaus-markdown" v-html="parseMarkdown(ex.explanation)"></div>
                  </div>
                </div>
              </div>
            </BauhausCard>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="flex flex-col items-center justify-center py-20">
    <LoaderIcon class="w-10 h-10 animate-spin text-foreground mb-4" />
    <p class="font-sans font-bold text-sm uppercase tracking-wider text-foreground/70">Đang tải nội dung bài học...</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLessonStore } from '@/store/modules/lesson'
import { useExerciseStore } from '@/store/modules/exercise'
import flashcardService from '@/services/flashcardService'
import { useToast } from '@/composables/useToast'
import BauhausButton from '@/components/bauhaus/BauhausButton.vue'
import BauhausCard from '@/components/bauhaus/BauhausCard.vue'
import { Loader2 as LoaderIcon, Clock as ClockIcon } from 'lucide-vue-next'
import { parseMarkdown } from '@/utils/markdown'

const route = useRoute()
const lessonStore = useLessonStore()
const exerciseStore = useExerciseStore()
const toast = useToast()

const activeTab = ref('vocab')
const isFlipped = ref(false)
const currentVocabIndex = ref(0)
const answers = ref({})
const results = ref({})
const masteryLevel = ref(0)

const colors = [
  'bg-white',
  'bg-pink-300',
  'bg-green-300',
  'bg-blue-300',
  'bg-orange-300',
  'bg-purple-300',
  'bg-red-300'
]
const frontBgClass = computed(() => colors[currentVocabIndex.value % colors.length])

const lesson = computed(() => lessonStore.currentLesson)
const currentVocab = computed(() => {
  if (lesson.value?.vocabularies?.length > 0) {
    return lesson.value.vocabularies[currentVocabIndex.value]
  }
  return null
})

import vocabularyService from '@/services/vocabularyService'

const vocabDetail = ref(null)
const vocabLoading = ref(false)

async function fetchCurrentVocabDetails() {
  if (!currentVocab.value) return

  vocabDetail.value = null
  vocabLoading.value = true

  try {
    const apiResults = await vocabularyService.search(currentVocab.value.word)
    if (apiResults && apiResults.length > 0) {
      vocabDetail.value = apiResults[0]
    } else {
      vocabDetail.value = {
        word: currentVocab.value.word,
        wordType: 'N/A',
        pronunciation: 'N/A',
        meaning: 'Không tìm thấy định nghĩa',
        exampleSentence: ''
      }
    }
  } catch (e) {
    console.error('Lỗi khi tải chi tiết từ vựng từ API:', e)
    vocabDetail.value = {
      word: currentVocab.value.word,
      wordType: 'N/A',
      pronunciation: 'N/A',
      meaning: '⚠️ Lỗi kết nối máy chủ từ điển',
      exampleSentence: ''
    }
  } finally {
    vocabLoading.value = false
  }
}

const loadLesson = async (lessonId) => {
  try {
    answers.value = {}
    results.value = {}
    currentVocabIndex.value = 0
    isFlipped.value = false
    activeTab.value = 'vocab'
    await lessonStore.fetchLessonById(lessonId)
  } catch (e) {
    console.error('Failed to load lesson details:', e)
  }
}

onMounted(() => loadLesson(route.params.id))

watch(() => route.params.id, (newId) => {
  if (newId) loadLesson(newId)
})

watch(currentVocab, async () => {
  await fetchMasteryLevel()
  await fetchCurrentVocabDetails()
})

async function fetchMasteryLevel() {
  if (currentVocab.value) {
    try {
      masteryLevel.value = await flashcardService.getStatus(currentVocab.value.id)
    } catch (e) {
      masteryLevel.value = 0
    }
  }
}

async function reviewVocab(isKnown) {
  if (currentVocab.value) {
    try {
      await flashcardService.reviewFlashcard(currentVocab.value.id, isKnown)
      await fetchMasteryLevel()
      if (isKnown) {
        nextVocab()
      }
    } catch (e) {
      console.error('Lỗi khi cập nhật tiến độ:', e)
    }
  }
}

function prevVocab() {
  if (currentVocabIndex.value > 0) {
    isFlipped.value = false
    currentVocabIndex.value--
  }
}

function nextVocab() {
  if (currentVocabIndex.value < lesson.value.vocabularies.length - 1) {
    isFlipped.value = false
    currentVocabIndex.value++
  }
}

function speakWord(word) {
  if (!word || !window.speechSynthesis) return
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(word)
  utterance.lang = 'en-US'
  utterance.rate = 0.9
  window.speechSynthesis.speak(utterance)
}

function parseOptions(optionsJson) {
  if (!optionsJson) return []
  try {
    const parsed = JSON.parse(optionsJson)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
}

function optionClass(exerciseId, optIdx, correctAns) {
  const userAns = answers.value[exerciseId]
  const isSubmitted = results.value[exerciseId] !== undefined

  if (!isSubmitted) return ''
  if (String(optIdx) === correctAns) return 'bg-green-200 border-green-600'
  if (String(optIdx) === userAns && userAns !== correctAns) return 'bg-red-200 border-red-600'
  return ''
}

function displayCorrectAnswer(ex) {
  if (ex.exerciseType === 'MULTIPLE_CHOICE') {
    const opts = parseOptions(ex.options)
    try {
      const idx = parseInt(ex.correctAnswer)
      return opts[idx]
    } catch (e) {
      return ex.correctAnswer
    }
  }
  return ex.correctAnswer
}

async function submitAnswer(exerciseId) {
  const userAns = answers.value[exerciseId]
  if (!userAns) return

  try {
    const res = await exerciseStore.submitExerciseAnswer({
      exerciseId: exerciseId,
      userAnswer: userAns
    })
    results.value[exerciseId] = res.isCorrect
  } catch (e) {
    toast.error(e.response?.data?.error || 'Có lỗi xảy ra khi nộp bài. Vui lòng thử lại.');
  }
}
</script>

<style scoped>
.flashcard-card {
  perspective: 1000px;
  cursor: pointer;
  min-height: 300px;
}

.flashcard-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.6s ease-in-out;
  transform-style: preserve-3d;
  min-height: 300px;
}

.flipped .flashcard-inner {
  transform: rotateY(180deg);
}

.flashcard-front,
.flashcard-back {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  min-height: 300px;
}

.flashcard-back {
  transform: rotateY(180deg);
}

input.bauhaus-radio {
  appearance: none;
  -webkit-appearance: none;
  width: 20px;
  height: 20px;
  border: 2px solid #121212;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  margin: 0;
  background: white;
}

input.bauhaus-radio:checked {
  background-color: #D02020;
  border-color: #121212;
  box-shadow: inset 0 0 0 3px white;
}

audio.bauhaus-audio,
audio[controls] {
  filter: grayscale(1) contrast(1.5);
}
</style>
