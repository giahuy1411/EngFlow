<template>
  <div class="bg-card border-2 border-foreground rounded-md p-5 shadow-pop-lg">
    <div class="flex items-center gap-3 mb-3">
      <div class="w-10 h-10 rounded-md flex items-center justify-center border-2 border-foreground"
        :class="iconBg">
        <component :is="icon" class="w-5 h-5" :class="iconColor || 'text-foreground'" />
      </div>
      <span class="font-bold text-xs uppercase tracking-wider text-muted-foreground">{{ label }}</span>
    </div>
    <p class="font-black text-3xl">{{ value }}</p>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: 0 },
  icon: { type: [Object, Function, String], required: true },
  iconBg: { type: String, default: 'bg-accent/10' },
  iconColor: { type: String, default: 'text-accent-ink' },
})
</script>
