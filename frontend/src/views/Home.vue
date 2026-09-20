<template>
  <div>
    <section class="app-hero">
      <!-- Prompt: "A massive yellow circle behind the text." -->
      <span class="app-hero__sun" aria-hidden="true" />
      <Container>
        <div class="app-hero__grid">
          <div class="app-hero__text">
            <DotBackground :gap="24" :tint="0.14" class="absolute inset-0 -z-10" />
            <h1 class="app-hero__title">
              Học Tiếng Anh<br />
              <span class="app-hero__title-accent">Vui Vẻ</span>
            </h1>
            <p class="app-hero__subtitle">
              Nền tảng tự học tiếng Anh toàn diện với bài học tương tác, luyện tập thông minh và cộng đồng sôi động.
            </p>
            <div class="app-hero__cta">
              <AppButton as="router-link" to="/register" size="lg">Bắt đầu ngay</AppButton>
              <AppButton as="router-link" to="/lessons" variant="secondary" size="lg">Khám phá</AppButton>
            </div>
            <div class="app-hero__stats">
              <!-- Số liệu thật từ DB (query 2026-08-30): lessons=1473, exercises=43727, levels=4 -->
              <div class="app-hero__stat">
                <p class="app-hero__stat-num app-hero__stat-num--accent">1.4K+</p>
                <p class="app-hero__stat-label">Bài học</p>
              </div>
              <div class="app-hero__stat">
                <p class="app-hero__stat-num app-hero__stat-num--secondary">43K+</p>
                <p class="app-hero__stat-label">Câu hỏi luyện tập</p>
              </div>
              <div class="app-hero__stat">
                <p class="app-hero__stat-num app-hero__stat-num--tertiary">4</p>
                <p class="app-hero__stat-label">Trình độ A1-B2</p>
              </div>
            </div>
          </div>
          <div class="app-hero__visual" aria-hidden="true">
            <span class="app-hero__shape app-hero__shape--sq" />
            <span class="app-hero__shape app-hero__shape--ci" />
            <span class="app-hero__shape app-hero__shape--tri" />
            <!-- Prompt: "The image itself has a blob mask." -->
            <span class="app-hero__shape app-hero__shape--blob" />
          </div>
        </div>
      </Container>
    </section>

    <LearningPath />

    <SquiggleDivider height="20px" />

    <PageSection title="Mọi thứ bạn cần" align="center">
      <div class="app-features__track">
        <!-- Prompt: "Each card is connected by a dashed SVG line drawn in the background." -->
        <svg class="app-features__connector" viewBox="0 0 1000 60" preserveAspectRatio="none" aria-hidden="true">
          <path d="M0 30 H1000" stroke="var(--geo-fg)" stroke-width="2" stroke-dasharray="10 10" fill="none" />
        </svg>
        <div class="app-features__grid">
          <StickerCard v-for="(feature, i) in features" :key="i" interactive>
            <template #icon>
              <div class="app-features__icon" :class="feature.iconBg">
                <component :is="feature.icon" class="w-6 h-6" />
              </div>
            </template>
            <h3 class="app-features__card-title">{{ feature.title }}</h3>
            <p class="app-features__card-desc">{{ feature.desc }}</p>
          </StickerCard>
        </div>
      </div>
    </PageSection>

    <SquiggleDivider color="var(--geo-accent)" height="20px" />

    <!-- Prompt: "Use infinite scrolling text for client logos or keywords." -->
    <section class="app-marquee" aria-label="Chủ đề luyện tập">
      <div class="app-marquee__track">
        <span v-for="(word, i) in marqueeWords" :key="`a-${i}`" class="app-marquee__item">{{ word }}</span>
        <span v-for="(word, i) in marqueeWords" :key="`b-${i}`" class="app-marquee__item" aria-hidden="true">{{ word }}</span>
      </div>
    </section>

    <section class="app-cta">
      <Container>
        <div class="app-cta__card">
          <h2 class="app-cta__title">Sẵn sàng bắt đầu?</h2>
          <p class="app-cta__subtitle">Hoàn toàn miễn phí.</p>
          <AppButton as="router-link" to="/register" size="lg" with-arrow>Đăng ký miễn phí</AppButton>
        </div>
      </Container>
    </section>
  </div>
</template>

<script setup>
import { BookOpen, Brain, Target, Sparkles } from 'lucide-vue-next'
import StickerCard from '@/components/ui/StickerCard.vue'
import AppButton from '@/components/ui/AppButton.vue'
import Container from '@/components/layout/Container.vue'
import PageSection from '@/components/layout/PageSection.vue'
import DotBackground from '@/components/decor/DotBackground.vue'
import SquiggleDivider from '@/components/decor/SquiggleDivider.vue'
import LearningPath from '@/components/home/LearningPath.vue'

const features = [
  { icon: BookOpen, iconBg: 'app-features__icon--accent', title: 'Bài học có cấu trúc', desc: 'Lộ trình học bài bản từ cơ bản đến nâng cao, phù hợp với mọi trình độ.' },
  { icon: Brain, iconBg: 'app-features__icon--secondary', title: 'Luyện tập thông minh', desc: 'Flashcard, trắc nghiệm, nghe nói đọc viết đa dạng giúp luyện.' },
  { icon: Target, iconBg: 'app-features__icon--tertiary', title: 'Theo dõi tiến độ', desc: 'Theo dõi streak, điểm số và thành tích để duy trì động lực mỗi ngày.' },
  { icon: Sparkles, iconBg: 'app-features__icon--accent', title: 'AI hỗ trợ', desc: 'Sinh bài tập, đánh giá phát âm và gợi ý từ vựng cá nhân hóa.' },
]

// Prompt: marquee carries "client logos or keywords". Real topics the app
// actually teaches, not filler — the list is duplicated in the template so the
// -50% translate loop is seamless.
const marqueeWords = [
  'Từ vựng', 'Ngữ pháp', 'Nghe hiểu', 'Phát âm', 'Viết luận',
  'Giao tiếp', 'Thì động từ', 'Cụm động từ', 'Thành ngữ', 'Luyện đề',
]
</script>