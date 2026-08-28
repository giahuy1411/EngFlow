<template>
  <header
    class="user-page-header flex flex-col md:flex-row justify-between items-start md:items-end gap-8"
    :class="[rootClass, { 'border-b-2 border-foreground pb-8': divided }]"
  >
    <div class="relative">
      <div class="absolute -top-4 -left-4 w-8 h-8 bg-tertiary border-2 border-foreground rotate-12 rounded-sm" aria-hidden="true"></div>
      <div class="absolute -bottom-2 -right-2 w-6 h-6 bg-secondary border-2 border-foreground rounded-full" aria-hidden="true"></div>
      <h1 :id="titleId || undefined" class="font-black text-5xl md:text-6xl uppercase tracking-tight leading-none relative z-10">
        <slot name="title">
          <span v-if="titleParts.prefix">{{ titleParts.prefix }}&nbsp;</span><span class="text-accent">{{ titleParts.highlight }}</span>
        </slot>
      </h1>
      <p v-if="subtitle || eyebrow" class="font-bold text-sm uppercase tracking-wider text-muted-foreground mt-3">
        {{ subtitle || eyebrow }}
      </p>
    </div>

    <div v-if="$slots.actions" class="flex flex-wrap gap-2 md:justify-end">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  eyebrow: { type: String, default: '' },
  rootClass: { type: String, default: '' },
  titleId: { type: String, default: '' },
  divided: { type: Boolean, default: false },
})

const titleParts = computed(() => {
  const words = props.title.trim().split(/\s+/).filter(Boolean)
  if (words.length <= 1) return { prefix: '', highlight: props.title }
  return {
    prefix: words.slice(0, -1).join(' '),
    highlight: words.at(-1),
  }
})
</script>
