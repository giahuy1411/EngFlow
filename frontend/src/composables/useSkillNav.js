import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const SKILL_ORDER = ['VOCABULARY', 'GRAMMAR', 'LISTENING', 'READING', 'WRITING', 'SPEAKING']

const SKILL_ROUTES = {
  VOCABULARY: 'LessonVocab',
  GRAMMAR: 'LessonGrammar',
  LISTENING: 'LessonListening',
  READING: 'LessonReading',
  WRITING: 'LessonWriting',
  SPEAKING: 'LessonSpeaking',
}

const SKILL_LABELS = {
  VOCABULARY: 'Vocabulary',
  GRAMMAR: 'Grammar',
  LISTENING: 'Listening',
  READING: 'Reading',
  WRITING: 'Writing',
  SPEAKING: 'Speaking',
}

export function useSkillNav(currentSkill) {
  const router = useRouter()
  const route = useRoute()
  const lessonId = computed(() => route.params.id)
  const currentIdx = computed(() => SKILL_ORDER.indexOf(currentSkill.value))
  const prevSkill = computed(() => currentIdx.value > 0 ? SKILL_ORDER[currentIdx.value - 1] : null)
  const nextSkill = computed(() => currentIdx.value < SKILL_ORDER.length - 1 ? SKILL_ORDER[currentIdx.value + 1] : null)
  const prevLabel = computed(() => prevSkill.value ? SKILL_LABELS[prevSkill.value] : '')
  const nextLabel = computed(() => nextSkill.value ? SKILL_LABELS[nextSkill.value] : '')

  function goToSkill(skill) {
    if (skill) router.push({ name: SKILL_ROUTES[skill], params: { id: lessonId.value } })
  }

  return { prevSkill, nextSkill, prevLabel, nextLabel, goToSkill }
}
