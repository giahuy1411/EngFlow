<template>
  <div class="flex items-center gap-4">
    <div v-if="icon" class="w-10 h-10 rounded-md flex items-center justify-center border-2 border-foreground"
      :class="iconBg">
      <component :is="icon" class="w-5 h-5" :class="iconColor || 'text-foreground'" />
    </div>
    <div>
      <h2 class="font-black text-2xl uppercase tracking-tight">{{ title }}</h2>
      <p v-if="subtitle" class="font-bold text-xs uppercase tracking-widest text-muted-foreground">{{ subtitle }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: [Object, Function, String], default: undefined },
  iconBg: { type: String, default: 'bg-accent' },
  iconColor: { type: String, default: 'text-white' },
})
</script>
