<template>
  <footer class="bg-foreground text-background border-t-2 border-foreground mt-auto relative overflow-hidden">
    <!-- Decorative shapes -->
    <div class="absolute top-4 left-8 w-4 h-4 bg-accent/30 rounded-full border border-background/20"></div>
    <div class="absolute bottom-8 right-12 w-6 h-6 bg-tertiary/20 border border-background/20 rotate-12 rounded-md"></div>
    <div class="absolute top-1/2 right-1/4 w-3 h-3 bg-secondary/20 rounded-full"></div>

    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10">
      <div class="flex flex-col md:flex-row justify-between items-center gap-4">
        <div class="flex items-center gap-3">
          <div class="w-4 h-4 bg-accent border-2 border-background rounded-md"></div>
          <div class="w-4 h-4 bg-secondary border-2 border-background rounded-full"></div>
          <div class="w-4 h-4 bg-tertiary border-2 border-background rotate-12" style="clip-path: polygon(50% 0%, 0% 100%, 100% 100%);"></div>
          <span class="font-black text-xl tracking-tighter ml-2">EngFlow</span>
        </div>
        <p class="font-bold text-xs uppercase tracking-wider text-background/60">
          &copy; 2026 ENGFLOW. TỰ HỌC TIẾNG ANH.
        </p>
      </div>
    </div>
  </footer>
</template>
