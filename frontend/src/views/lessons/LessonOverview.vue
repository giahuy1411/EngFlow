<template>
  <div class="space-y-8">
    <div v-if="loading" class="flex justify-center py-16">
      <div class="w-10 h-10 border-2 border-foreground border-t-accent rounded-full animate-spin"></div>
    </div>
    <div v-if="!loading && !lesson" class="text-center py-16">
      <p class="text-xl font-bold text-muted-foreground">Bài học này chưa có nội dung.</p>
    </div>
    <!-- Legacy overview — now redirects to LessonContent -->
    <p class="text-muted-foreground italic">Nội dung bài học đã chuyển sang tab "Nội dung".</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import lessonService from '@/services/lessonService'

const route = useRoute()
const lessonId = Number(route.params.id)
const lesson = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    lesson.value = await lessonService.getById(lessonId)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>
