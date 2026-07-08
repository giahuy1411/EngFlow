<template>
  <div class="bg-foreground border-b-2 border-foreground">
    <div class="max-w-7xl mx-auto px-6 py-3 flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 bg-tertiary border-2 border-foreground flex items-center justify-center font-black text-sm rotate-12 rounded-sm">{{ streak }}</div>
          <span class="text-white font-bold text-xs uppercase tracking-wider">Ngày liên tiếp</span>
        </div>
      </div>
      <router-link to="/profile"
        class="text-white/80 hover:text-white font-bold text-[10px] uppercase tracking-widest border border-white/30 px-3 py-1 rounded-md hover:bg-white/10 transition-colors"
      >Xem chi tiết →</router-link>
    </div>
  </div>
</template>

<script setup>
defineProps({ streak: { type: Number, default: 0 } })
</script>
