<template>
  <div class="max-w-4xl mx-auto p-6">
    <h1 class="text-2xl font-bold mb-2">{{ lesson?.title }}</h1>
    <p class="text-gray-500 mb-6">{{ lesson?.description }}</p>
    <LessonBlockRenderer v-if="sections.length" :sections="sections" />
    <p v-else class="text-gray-400 italic">Bài học chưa có nội dung.</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import lessonService from '../../services/lessonService';
import lessonStructureService from '../../services/lessonStructureService';
import LessonBlockRenderer from '../../components/lessons/block-renderer/LessonBlockRenderer.vue';

const route = useRoute();
const lessonId = Number(route.params.id);
const lesson = ref(null);
const sections = ref([]);

onMounted(async () => {
  try {
    const lessons = await lessonService.getAll();
    lesson.value = lessons.find(l => l.id === lessonId);
  } catch {}
  try {
    sections.value = await lessonStructureService.getStructure(lessonId);
  } catch {}
});
</script>
