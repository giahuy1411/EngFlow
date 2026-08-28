<template>
  <section class="relative geo-section" :class="{ 'geo-section--flush': !padding }">
    <template v-if="decorative">
      <DecoShape variant="tertiary" size="lg" class="absolute -top-16 -left-16 opacity-40" />
      <DecoShape variant="accent" size="md" class="absolute top-1/4 right-8 opacity-30" />
      <DecoShape variant="secondary" size="sm" class="absolute bottom-8 left-1/3 opacity-30" />
    </template>
    <Container>
      <header v-if="hasHeading" class="relative z-10 mb-12 geo-section__header" :class="{ 'text-center': align === 'center' }">
        <div v-if="kicker" class="inline-flex items-center gap-2.5 mb-3">
          <span class="w-3.5 h-3.5 rounded-full bg-accent border-2 border-foreground inline-block" />
          <span class="font-bold text-xs uppercase tracking-widest text-muted-foreground">{{ kicker }}</span>
        </div>
        <h2 v-if="title" class="font-black text-3xl md:text-4xl tracking-tight leading-tight">
          {{ title }}
        </h2>
        <p v-if="subtitle" class="mt-2.5 font-medium text-muted-foreground max-w-2xl leading-relaxed" :class="{ 'mx-auto': align === 'center' }">
          {{ subtitle }}
        </p>
      </header>
      <div class="relative z-10">
        <slot />
      </div>
    </Container>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import Container from './Container.vue'
import DecoShape from '@/components/decor/DecoShape.vue'

const props = defineProps({
  decorative: { type: Boolean, default: false },
  padding:   { type: Boolean, default: true },
  align:     { type: String, default: 'left' },
  kicker:    { type: String, default: '' },
  title:     { type: String, default: '' },
  subtitle:  { type: String, default: '' },
})
const hasHeading = computed(() => props.kicker || props.title || props.subtitle)
</script>

<style scoped>
.geo-section {
  padding-top: 4rem;
  padding-bottom: 5rem;
}
.geo-section--flush {
  padding-top: 0;
  padding-bottom: 0;
}
.geo-section__header {
  margin-bottom: 3rem;
}
</style>
