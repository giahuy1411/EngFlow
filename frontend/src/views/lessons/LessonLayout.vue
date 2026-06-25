<template>
  <div class="bg-background min-h-screen border-b-4 border-black">
    <div v-if="loading" class="flex items-center justify-center min-h-[60vh]">
      <div class="animate-spin inline-block w-12 h-12 border-4 border-black border-t-transparent rounded-full"></div>
    </div>

    <div v-else-if="error" class="max-w-4xl mx-auto py-16 px-4">
      <div class="bg-red-100 border-4 border-red-500 text-red-700 p-6 rounded-xl font-bold text-lg">
        {{ error }}
      </div>
    </div>

    <template v-else-if="lesson">
      <section class="bg-primary-blue border-b-4 border-black py-12 px-4">
        <div class="max-w-6xl mx-auto">
          <button @click="$router.push('/lessons')" class="text-white/80 hover:text-white font-bold text-sm uppercase mb-4 inline-block">
            &larr; Back to Lessons
          </button>
          <h2 class="font-black text-4xl md:text-5xl uppercase text-white tracking-tighter mb-2">{{ lesson.title }}</h2>
          <p v-if="lesson.description" class="font-medium text-white/80 text-lg">{{ lesson.description }}</p>
        </div>
      </section>

      <div class="max-w-6xl mx-auto py-8 px-4">
        <SkillNav
          :active-skill="currentSkill"
          :lesson-id="lesson.id"
          @navigate="navigateToSkill"
        />

        <RouterView
          :lesson="lesson"
          :skills="lessonSkills"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import lessonService from '@/services/lessonService'
import SkillNav from '@/components/lessons/SkillNav.vue'

const route = useRoute()
const router = useRouter()

const lesson = ref(null)
const loading = ref(true)
const error = ref(null)

const lessonSkills = computed(() => lesson.value?.lessonSkills || [])

const currentSkill = computed(() => {
  const name = route.name || ''
  if (name === 'LessonOverview') return 'OVERVIEW'
  return name.replace('Lesson', '').toUpperCase()
})

function navigateToSkill(code) {
  const routes = {
    OVERVIEW: 'LessonOverview',
    VOCABULARY: 'LessonVocab',
    GRAMMAR: 'LessonGrammar',
    LISTENING: 'LessonListening',
    READING: 'LessonReading',
    WRITING: 'LessonWriting',
    SPEAKING: 'LessonSpeaking',
  }
  const target = routes[code]
  if (target) router.push({ name: target, params: { id: route.params.id } })
}

onMounted(async () => {
  try {
    lesson.value = await lessonService.getById(route.params.id)
  } catch (e) {
    error.value = 'Failed to load lesson details.'
  } finally {
    loading.value = false
  }
})
</script>
