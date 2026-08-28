<script setup>
/**
 * AppLogo — reusable EngFlow mark.
 * `href` empty -> static span; `href` set -> router-link home.
 */
defineProps({
  href: { type: String, default: '' },
})
</script>

<template>
  <router-link v-if="href" :to="href" class="app-logo" aria-label="EngFlow home">
    <span class="app-logo__mark" aria-hidden="true">
      <span class="app-logo__square" />
      <span class="app-logo__circle" />
      <span class="app-logo__dot" />
    </span>
    <span class="app-logo__word">EngFlow</span>
  </router-link>
  <span v-else class="app-logo" aria-label="EngFlow">
    <span class="app-logo__mark" aria-hidden="true">
      <span class="app-logo__square" />
      <span class="app-logo__circle" />
      <span class="app-logo__dot" />
    </span>
    <span class="app-logo__word">EngFlow</span>
  </span>
</template>