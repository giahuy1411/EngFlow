<template>
  <nav class="skill-nav">
    <button
      v-for="skill in skills"
      :key="skill.code"
      class="skill-nav__btn"
      :class="{ 'skill-nav__btn--active': skill.code === activeSkill }"
      @click="$emit('navigate', skill.code)"
    >
      {{ skill.label }}
    </button>
  </nav>
</template>

<script setup>
const props = defineProps({
  activeSkill: { type: String, required: true },
  lessonId: { type: [String, Number], required: true },
})

const emit = defineEmits(['navigate'])

const skills = [
  { code: 'VOCABULARY', label: 'Vocabulary' },
  { code: 'GRAMMAR', label: 'Grammar' },
  { code: 'LISTENING', label: 'Listening' },
  { code: 'READING', label: 'Reading' },
  { code: 'WRITING', label: 'Writing' },
  { code: 'SPEAKING', label: 'Speaking' },
]
</script>

<style scoped>
.skill-nav {
  display: flex;
  gap: 0;
  border: 3px solid var(--geo-fg, #1E293B);
  background: var(--geo-fg, #1E293B);
  margin-bottom: 2rem;
}
.skill-nav__btn {
  flex: 1;
  padding: 0.75rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border: 2px solid var(--geo-fg, #1E293B);
  background: var(--geo-card, #fff);
  color: var(--geo-fg, #1E293B);
  cursor: pointer;
  transition: all 0.15s;
  font-family: var(--geo-font, 'Be Vietnam Pro', sans-serif);
}
.skill-nav__btn:hover {
  background: var(--geo-tertiary, #FBBF24);
  border-color: var(--geo-tertiary, #FBBF24);
}
.skill-nav__btn--active {
  background: var(--geo-tertiary, #FBBF24);
  border-color: var(--geo-tertiary, #FBBF24);
  color: var(--geo-fg, #1E293B);
  box-shadow: inset 0 -3px 0 var(--geo-danger, #E11D48);
}
</style>
