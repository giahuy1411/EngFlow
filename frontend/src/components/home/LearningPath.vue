<template>
  <PageSection
    title="Lộ trình học tập"
    subtitle="Từ số 0 đến giao tiếp tự tin — học theo thứ tự, mỗi chặng đều có số liệu thật."
  >
    <ol class="grid gap-6 md:grid-cols-2 lg:grid-cols-5 lg:gap-4">
      <li v-for="(step, i) in steps" :key="step.title" class="relative">
        <!-- Connector: mờ, chỉ hiện ở desktop, nối các bước -->
        <span
          v-if="i < steps.length - 1"
          aria-hidden="true"
          class="hidden lg:block absolute top-1/2 -right-2 w-4 border-t-2 border-dashed border-muted-foreground/40"
        />
        <StickerCard interactive class="h-full">
          <template #icon>
            <div class="app-features__icon" :class="step.iconBg">
              <component :is="step.icon" class="w-6 h-6" />
            </div>
          </template>
          <p class="font-black text-xs uppercase tracking-widest text-muted-foreground">
            Bước {{ i + 1 }}
          </p>
          <h3 class="font-black text-lg uppercase leading-snug mt-1 text-foreground">
            {{ step.title }}
          </h3>
          <p class="text-sm font-medium text-muted-foreground leading-relaxed mt-2">
            {{ step.desc }}
          </p>
          <p class="font-bold text-xs uppercase tracking-wider text-accent mt-3">
            {{ step.stat }}
          </p>
          <p v-if="step.badge" class="inline-flex items-center gap-1.5 mt-3 text-[11px] font-bold uppercase tracking-wider text-warning border border-foreground/20 bg-warning/10 px-2 py-0.5 rounded-md">
            <Lock class="w-3 h-3" /> {{ step.badge }}
          </p>
          <template #footer>
            <AppButton as="router-link" :to="step.to" variant="secondary" size="sm" class="mt-4">
              {{ step.cta }}
            </AppButton>
          </template>
        </StickerCard>
      </li>
    </ol>
  </PageSection>
</template>

<script setup>
import { BookOpen, Brain, Mic, Search, Trophy, Lock } from 'lucide-vue-next'
import PageSection from '@/components/layout/PageSection.vue'
import StickerCard from '@/components/ui/StickerCard.vue'
import AppButton from '@/components/ui/AppButton.vue'

// Nguồn số liệu: DB query 2026-08-30 (docs/spec-home-roadmap.md) + đếm route từ router.
// Không sinh số ngoài nguồn này. Nâng cấp API stats thật là việc sau MVP.
const steps = [
  {
    icon: BookOpen,
    iconBg: 'app-features__icon--accent',
    title: 'Học bài theo lộ trình',
    desc: 'Bài học theo 4 trình độ, 7 kỹ năng: ngữ pháp, từ vựng, nghe, nói, đọc, viết.',
    stat: '1.473 bài · 4 trình độ · 7 kỹ năng',
    to: '/lessons',
    cta: 'Bắt đầu học',
  },
  {
    icon: Brain,
    iconBg: 'app-features__icon--secondary',
    title: 'Luyện từ vựng',
    desc: 'Flashcard, quiz, ghép cặp, gõ từ — ôn từ vựng qua 6 kiểu game.',
    stat: '11 bộ thẻ công khai · 6 game',
    to: '/decks',
    cta: 'Vào luyện tập',
    badge: 'Game cần đăng nhập',
  },
  {
    icon: Mic,
    iconBg: 'app-features__icon--tertiary',
    title: 'Luyện nói cùng AI',
    desc: 'Ghi âm và để AI chấm phát âm, phản hồi từng câu nói.',
    stat: '107 bài nói',
    to: '/premium?redirect=%2Fspeaking',
    cta: 'Xem Premium',
    badge: 'Premium',
  },
  {
    icon: Search,
    iconBg: 'app-features__icon--accent',
    title: 'Tra từ nhanh',
    desc: 'Tra nghĩa, phiên âm và phát âm từ vựng ngay khi cần.',
    stat: 'Tra nghĩa · phát âm',
    to: '/search',
    cta: 'Tra từ ngay',
  },
  {
    icon: Trophy,
    iconBg: 'app-features__icon--secondary',
    title: 'Thi đua & giữ nhịp',
    desc: 'Giữ chuỗi ngày học, leo bảng xếp hạng cùng cộng đồng đang lớn dần.',
    stat: 'Streak · bảng vàng · 41 người học',
    to: '/leaderboard',
    cta: 'Xem bảng vàng',
  },
]
</script>
