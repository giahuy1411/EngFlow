<template>
  <footer class="bg-foreground text-background py-8 border-t-4 border-black mt-auto">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row justify-between items-center">
      <div class="flex items-center gap-2 mb-4 md:mb-0">
        <div class="w-4 h-4 rounded-full bg-primary-red border-2 border-black"></div>
        <div class="w-4 h-4 rounded-none bg-primary-blue border-2 border-black"></div>
        <div class="w-4 h-4 bg-primary-yellow border-2 border-black clip-triangle"></div>
        <span class="font-black text-xl uppercase tracking-tighter ml-2">EngFlow</span>
      </div>
      <p class="font-bold text-sm uppercase tracking-wider text-muted">
        &copy; 2026 ENGFLOW. TỰ HỌC TIẾNG ANH.
      </p>
    </div>
  </footer>
</template>

<style scoped>
.clip-triangle {
  clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
}
</style>
