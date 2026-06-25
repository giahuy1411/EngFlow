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
          <button @click="goBack" class="text-white/80 hover:text-white font-bold text-sm uppercase mb-4 inline-block">
            &larr; {{ isOverview ? 'Lessons' : 'Overview' }}
          </button>
          <h2 class="font-black text-4xl md:text-5xl uppercase text-white tracking-tighter mb-2">{{ lesson.title }}</h2>
          <p v-if="lesson.description" class="font-medium text-white/80 text-lg">{{ lesson.description }}</p>
        </div>
      </section>

      <div class="max-w-6xl mx-auto py-8 px-4">
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

const route = useRoute()
const router = useRouter()

const lesson = ref(null)
const loading = ref(true)
const error = ref(null)

const lessonSkills = computed(() => lesson.value?.lessonSkills || [])
const isOverview = computed(() => route.name === 'LessonOverview')

function goBack() {
  if (isOverview.value) router.push('/lessons')
  else router.push(`/lessons/${lesson.value.id}`)
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
