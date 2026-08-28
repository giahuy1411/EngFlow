<template>
  <div>
    <section class="app-hero">
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
              <div class="app-hero__stat">
                <p class="app-hero__stat-num app-hero__stat-num--accent">50+</p>
                <p class="app-hero__stat-label">Bài học</p>
              </div>
              <div class="app-hero__stat">
                <p class="app-hero__stat-num app-hero__stat-num--secondary">1K+</p>
                <p class="app-hero__stat-label">Từ vựng</p>
              </div>
              <div class="app-hero__stat">
                <p class="app-hero__stat-num app-hero__stat-num--tertiary">500+</p>
                <p class="app-hero__stat-label">Người học</p>
              </div>
            </div>
          </div>
          <div class="app-hero__visual" aria-hidden="true">
            <span class="app-hero__shape app-hero__shape--sq" />
            <span class="app-hero__shape app-hero__shape--ci" />
            <span class="app-hero__shape app-hero__shape--tri" />
          </div>
        </div>
      </Container>
    </section>

    <PageSection title="Mọi thứ bạn cần" align="center">
      <div class="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
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
    </PageSection>

    <section class="app-cta">
      <Container>
        <div class="app-cta__card">
          <h2 class="app-cta__title">Sẵn sàng bắt đầu?</h2>
          <p class="app-cta__subtitle">Hoàn toàn miễn phí.</p>
          <AppButton as="router-link" to="/register" size="lg">Đăng ký miễn phí</AppButton>
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

const features = [
  { icon: BookOpen, iconBg: 'app-features__icon--accent', title: 'Bài học có cấu trúc', desc: 'Lộ trình học bài bản từ cơ bản đến nâng cao, phù hợp với mọi trình độ.' },
  { icon: Brain, iconBg: 'app-features__icon--secondary', title: 'Luyện tập thông minh', desc: 'Flashcard, trắc nghiệm, nghe nói đọc viết đa dạng giúp luyện.' },
  { icon: Target, iconBg: 'app-features__icon--tertiary', title: 'Theo dõi tiến độ', desc: 'Theo dõi streak, điểm số và thành tích để duy trì động lực mỗi ngày.' },
  { icon: Sparkles, iconBg: 'app-features__icon--accent', title: 'AI hỗ trợ', desc: 'Sinh bài tập, đánh giá phát âm và gợi ý từ vựng cá nhân hóa.' },
]
</script>