<template>
  <div class="lesson-overview">
    <div class="bg-white border-4 border-black rounded-xl shadow-[10px_10px_0px_0px_rgba(0,0,0,1)] overflow-hidden">
      <div class="bg-black text-white px-6 py-4">
        <h3 class="font-black text-2xl uppercase">Lesson Skills</h3>
      </div>

      <div class="p-6 sm:p-8 space-y-4">
        <div
          v-for="skill in skills"
          :key="skill.code"
          class="flex items-center justify-between border-2 border-black rounded-lg p-4 hover:bg-yellow-50 transition-colors cursor-pointer"
          @click="navigateToSkill(skill.code)"
        >
          <div class="flex items-center gap-4">
            <span class="w-10 h-10 flex items-center justify-center border-2 border-black rounded-full font-black text-sm bg-yellow-400">
              {{ skill.icon }}
            </span>
            <div>
              <div class="font-black uppercase text-lg">{{ skill.label }}</div>
              <div class="text-sm text-gray-500">{{ skill.description }}</div>
            </div>
          </div>
          <span class="text-2xl font-black">&rarr;</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const skills = [
  { code: 'VOCABULARY', label: 'Vocabulary', icon: 'V', description: 'Learn new words and phrases' },
  { code: 'GRAMMAR', label: 'Grammar', icon: 'G', description: 'Master grammar structures' },
  { code: 'LISTENING', label: 'Listening', icon: 'L', description: 'Practice listening comprehension' },
  { code: 'READING', label: 'Reading', icon: 'R', description: 'Improve reading skills' },
  { code: 'WRITING', label: 'Writing', icon: 'W', description: 'Develop writing ability' },
  { code: 'SPEAKING', label: 'Speaking', icon: 'S', description: 'Enhance speaking fluency' },
]

function navigateToSkill(code) {
  const routes = {
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
</script>
