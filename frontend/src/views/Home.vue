<template>
  <div class="bg-geo-bg min-h-screen">
    <!-- ===== HERO SECTION ===== -->
    <section class="relative overflow-hidden py-16 md:py-24">
      <!-- Large decorative circle -->
      <div class="absolute -top-32 -left-32 w-96 h-96 bg-tertiary/30 rounded-full border-2 border-foreground/10"></div>
      <div class="absolute top-1/4 right-10 w-32 h-32 bg-accent/10 rounded-full border-2 border-foreground/10"></div>
      <div class="absolute bottom-10 left-1/3 w-16 h-16 bg-secondary/20 rounded-full border-2 border-foreground/10"></div>

      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div class="grid md:grid-cols-2 gap-12 items-center">
          <!-- Text -->
          <div class="space-y-8">
            <div class="relative inline-block">
              <div class="absolute -top-3 -left-3 w-8 h-8 bg-accent rounded-full border-2 border-foreground"></div>
              <div class="absolute -bottom-1 -right-1 w-5 h-5 bg-tertiary rotate-12 rounded-sm border-2 border-foreground"></div>
              <h1 class="font-black text-5xl md:text-6xl lg:text-7xl uppercase tracking-tight leading-none relative z-10">
                Học Tiếng Anh<br/>
                <span class="text-accent">Vui Vẻ</span>
              </h1>
            </div>
            <p class="font-medium text-lg text-muted-foreground max-w-lg leading-relaxed">
              Nền tảng tự học tiếng Anh toàn diện với bài học tương tác, luyện tập thông minh và cộng đồng sôi động.
            </p>
            <div class="flex flex-wrap gap-4">
              <router-link to="/register"
                class="inline-flex items-center gap-2 px-8 py-4 font-bold text-base bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all duration-300 ease-bounce"
              >
                Bắt đầu ngay
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6"/></svg>
              </router-link>
              <router-link to="/lessons"
                class="inline-flex items-center gap-2 px-8 py-4 font-bold text-base border-2 border-foreground rounded-full hover:bg-tertiary transition-all duration-300"
              >
                Khám phá
              </router-link>
            </div>
            <!-- Stats row -->
            <div class="flex gap-8 pt-4">
              <div>
                <span class="font-black text-3xl text-accent">50+</span>
                <p class="font-bold text-xs uppercase tracking-wider text-muted-foreground mt-1">Bài học</p>
              </div>
              <div>
                <span class="font-black text-3xl text-secondary">1K+</span>
                <p class="font-bold text-xs uppercase tracking-wider text-muted-foreground mt-1">Từ vựng</p>
              </div>
              <div>
                <span class="font-black text-3xl text-tertiary">500+</span>
                <p class="font-bold text-xs uppercase tracking-wider text-muted-foreground mt-1">Người học</p>
              </div>
            </div>
          </div>

          <!-- Image with blob mask -->
          <div class="relative">
            <div class="geo-dot-grid-light absolute -inset-4 rounded-blob pointer-events-none"></div>
            <div class="relative rounded-blob overflow-hidden border-2 border-foreground shadow-pop-xl">
              <img
                src="https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600&q=80"
                alt="Học tiếng Anh"
                class="w-full h-auto object-cover grayscale hover:grayscale-0 transition-all duration-700"
              />
              <!-- Floating badge -->
              <div class="absolute -bottom-4 -left-4 bg-card border-2 border-foreground px-4 py-2 rounded-blob-speech shadow-pop-sm">
                <p class="font-bold text-sm uppercase tracking-wider">Miễn phí 100%</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== FEATURES SECTION ===== -->
    <section class="relative py-20 bg-muted">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- Section heading -->
        <div class="text-center mb-16 relative">
          <div class="inline-flex items-center gap-3 mb-4">
            <div class="w-4 h-4 bg-accent rounded-full border-2 border-foreground"></div>
            <span class="font-bold text-xs uppercase tracking-widest text-muted-foreground">Tính năng</span>
          </div>
          <h2 class="font-black text-4xl md:text-5xl uppercase tracking-tight">
            Mọi thứ bạn <span class="text-accent">cần</span>
          </h2>
        </div>

        <!-- Feature cards grid -->
        <div class="grid md:grid-cols-3 gap-8">
          <div v-for="(feature, i) in features" :key="i"
            class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl hover:-rotate-1 hover:scale-[1.02] transition-all duration-300 ease-bounce relative"
          >
            <div class="absolute -top-4 left-8 w-12 h-12 rounded-full border-2 border-foreground flex items-center justify-center text-white font-black"
              :class="feature.iconBg">
              <component :is="feature.icon" class="w-6 h-6" />
            </div>
            <div class="mt-8">
              <div class="w-8 h-1 mb-4" :class="feature.barColor"></div>
              <h3 class="font-black text-xl uppercase tracking-tight mb-3">{{ feature.title }}</h3>
              <p class="font-medium text-muted-foreground leading-relaxed">{{ feature.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== CTA SECTION ===== -->
    <section class="relative py-24 overflow-hidden">
      <div class="absolute top-10 right-20 w-48 h-48 bg-tertiary/10 rounded-full border-2 border-foreground/10"></div>
      <div class="absolute bottom-10 left-20 w-32 h-32 bg-secondary/10 rounded-md border-2 border-foreground/10 rotate-12"></div>
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10">
        <div class="bg-card border-2 border-foreground rounded-md p-12 shadow-pop-xl">
          <div class="inline-flex items-center justify-center w-16 h-16 bg-accent rounded-full border-2 border-foreground mb-6 shadow-pop-sm">
            <svg class="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/></svg>
          </div>
          <h2 class="font-black text-4xl md:text-5xl uppercase tracking-tight mb-6">
            Sẵn sàng <span class="text-accent">học thử</span>?
          </h2>
          <p class="font-medium text-lg text-muted-foreground max-w-lg mx-auto mb-8">
            Tham gia cộng đồng EngFlow và bắt đầu hành trình chinh phục tiếng Anh ngay hôm nay.
          </p>
          <router-link to="/register"
            class="inline-flex items-center gap-2 px-10 py-4 font-bold text-lg bg-accent text-white border-2 border-foreground rounded-full shadow-pop hover:shadow-pop-hover hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all duration-300 ease-bounce"
          >
            Đăng ký miễn phí
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6"/></svg>
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { BookOpen, Brain, Target } from 'lucide-vue-next'

const features = [
  {
    icon: BookOpen,
    iconBg: 'bg-accent',
    barColor: 'bg-accent',
    title: 'Bài học cấu trúc',
    desc: 'Lộ trình học bài bản từ cơ bản đến nâng cao, phù hợp với mọi trình độ.',
  },
  {
    icon: Brain,
    iconBg: 'bg-secondary',
    barColor: 'bg-secondary',
    title: 'Luyện tập thông minh',
    desc: 'Flashcard, trắc nghiệm, nghe nói đọc viết — đa dạng hình thức ôn luyện.',
  },
  {
    icon: Target,
    iconBg: 'bg-tertiary',
    barColor: 'bg-tertiary',
    title: 'Theo dõi tiến độ',
    desc: 'Theo dõi streak, điểm số và thành tích để duy trì động lực mỗi ngày.',
  },
]
</script>
