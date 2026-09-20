<template>
  <PageSection
    title="Lộ trình học tập"
    subtitle="Từ số 0 đến giao tiếp tự tin. Năm chặng, mỗi chặng đều có số liệu thật."
  >
    <div class="lp">
      <!-- Đường ray: nền nhạt + progress fill chạy theo scroll -->
      <div class="lp__rail" aria-hidden="true">
        <div class="lp__rail-fill" :style="{ transform: `scaleY(${progress})` }" />
      </div>

      <ol class="lp__list">
        <li
          v-for="(step, i) in steps"
          :key="step.title"
          ref="stationEls"
          class="lp__item"
          :class="{ 'lp__item--in': revealed.has(i) }"
        >
          <!-- Trạm trên đường ray: số thứ tự, đổi màu khi tới lượt -->
          <span class="lp__node" :class="{ 'lp__node--in': revealed.has(i) }" aria-hidden="true">
            {{ i + 1 }}
          </span>

          <StickerCard
            interactive
            :featured="i === 0"
            class="lp__card"
            :style="{ '--lp-tint': step.tint }"
          >
            <div class="lp__card-body">
              <div class="lp__icon" :class="step.iconBg">
                <component :is="step.icon" class="w-6 h-6" />
              </div>
              <div class="lp__content">
                <h3 class="lp__title">{{ step.title }}</h3>
                <p class="lp__desc">{{ step.desc }}</p>
                <p class="lp__stat">
                  <span class="lp__stat-dot" />
                  {{ step.stat }}
                </p>
                <p v-if="step.badge" class="lp__badge">
                  <Lock class="w-3 h-3" /> {{ step.badge }}
                </p>
              </div>
              <AppButton as="router-link" :to="step.to" variant="secondary" size="sm" class="lp__cta">
                {{ step.cta }}
              </AppButton>
            </div>
          </StickerCard>
        </li>
      </ol>
    </div>
  </PageSection>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
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
    tint: 'rgba(139, 92, 246, 0.08)',
    title: 'Học bài theo lộ trình',
    desc: 'Bài học theo 4 trình độ, 7 kỹ năng: ngữ pháp, từ vựng, nghe, nói, đọc, viết.',
    stat: '1.473 bài · 4 trình độ · 7 kỹ năng',
    to: '/lessons',
    cta: 'Bắt đầu học',
  },
  {
    icon: Brain,
    iconBg: 'app-features__icon--secondary',
    tint: 'rgba(244, 114, 182, 0.08)',
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
    tint: 'rgba(251, 191, 36, 0.10)',
    title: 'Luyện nói cùng AI',
    desc: 'Ghi âm và để AI chấm phát âm, phản hồi từng câu nói.',
    stat: '107 bài nói',
    to: '/premium?redirect=%2Fspeaking',
    cta: 'Xem Premium',
    badge: 'Premium',
  },
  {
    icon: Search,
    iconBg: 'lp-icon--quaternary',
    tint: 'rgba(52, 211, 153, 0.10)',
    title: 'Tra từ nhanh',
    desc: 'Tra nghĩa, phiên âm và phát âm từ vựng ngay khi cần.',
    stat: 'Tra nghĩa · phát âm',
    to: '/search',
    cta: 'Tra từ ngay',
  },
  {
    icon: Trophy,
    iconBg: 'app-features__icon--accent',
    tint: 'rgba(139, 92, 246, 0.08)',
    title: 'Thi đua & giữ nhịp',
    desc: 'Giữ chuỗi ngày học, leo bảng xếp hạng cùng cộng đồng đang lớn dần.',
    stat: 'Streak · bảng vàng · 41 người học',
    to: '/leaderboard',
    cta: 'Xem bảng vàng',
  },
]

// Scroll reveal: mỗi trạm "cắm" xuống khi vào viewport; đường ray fill theo tiến trình.
// IntersectionObserver (không dùng window scroll listener). Reduced motion: bỏ hết.
// Guard typeof: jsdom không có matchMedia/IntersectionObserver -> fallback hiện tức thì.
const stationEls = ref([])
const revealed = ref(new Set())
const progress = ref(0)
let observer = null

function updateProgress() {
  const els = stationEls.value
  if (!els.length) return
  // Fill tới node cuối cùng ĐÃ reveal liên tục (không tính node nhảy cóc chưa tới lượt)
  let last = 0
  while (last + 1 < els.length && revealed.value.has(last + 1)) last++
  progress.value = els.length > 1 ? last / (els.length - 1) : 1
}

onMounted(() => {
  const reduceMotion =
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduceMotion || typeof IntersectionObserver === 'undefined') {
    revealed.value = new Set(steps.map((_, i) => i))
    progress.value = 1
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      let changed = false
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        const idx = stationEls.value.indexOf(entry.target)
        if (idx > -1 && !revealed.value.has(idx)) {
          revealed.value = new Set([...revealed.value, idx])
          changed = true
        }
      }
      if (changed) updateProgress()
    },
    { threshold: 0.35 },
  )
  stationEls.value.forEach((el) => observer.observe(el))
})

onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.lp {
  position: relative;
}

/* --- Đường ray --- */
.lp__rail {
  position: absolute;
  top: 0.5rem;
  bottom: 0.5rem;
  left: 1.25rem;
  width: 4px;
  border-radius: 2px;
  background: var(--geo-border);
  overflow: hidden;
}
.lp__rail-fill {
  position: absolute;
  inset: 0;
  background: var(--geo-accent);
  transform-origin: top;
  transform: scaleY(0);
  transition: transform 600ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
@media (min-width: 768px) {
  .lp__rail {
    left: 1.75rem;
  }
}

/* --- Danh sách trạm --- */
.lp__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
.lp__item {
  position: relative;
  padding-left: 3.5rem;
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 500ms ease, transform 500ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
.lp__item--in {
  opacity: 1;
  transform: translateY(0);
}
@media (min-width: 768px) {
  .lp__item {
    padding-left: 4.5rem;
  }
}

/* --- Node trạm (đánh số, nằm trên ray) --- */
.lp__node {
  position: absolute;
  left: 0.125rem;
  top: 1.25rem;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  border: 2px solid var(--geo-fg);
  background: var(--geo-card);
  color: var(--geo-muted-fg);
  font-weight: 800;
  font-size: var(--geo-text-base);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--geo-shadow-xs);
  transition: background 300ms ease, color 300ms ease, transform 300ms cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 1;
}
.lp__node--in {
  background: var(--geo-accent);
  color: var(--geo-accent-fg);
  transform: scale(1.08);
}
@media (min-width: 768px) {
  .lp__node {
    left: 0.5rem;
    width: 3rem;
    height: 3rem;
    font-size: var(--geo-text-lg);
  }
}

/* --- Thẻ trạm --- */
.lp__card {
  background:
    linear-gradient(180deg, var(--lp-tint, transparent), var(--lp-tint, transparent)),
    var(--geo-card);
}
.lp__card-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem;
}
.lp__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 3.5rem;
  height: 3.5rem;
  border-radius: var(--geo-radius-md);
  border: 2px solid var(--geo-fg);
  color: var(--geo-fg);
  flex-shrink: 0;
}
.lp-icon--quaternary {
  background: var(--geo-quaternary);
  color: #fff;
}
.lp__content {
  flex: 1;
  min-width: 0;
}
.lp__title {
  font-weight: 900;
  font-size: var(--geo-text-xl);
  line-height: 1.2;
  letter-spacing: -0.01em;
  color: var(--geo-fg);
  text-wrap: balance;
}
.lp__desc {
  margin-top: 0.375rem;
  font-size: var(--geo-text-sm);
  font-weight: 500;
  line-height: 1.6;
  color: var(--geo-muted-fg);
  max-width: 52ch;
}
.lp__stat {
  margin-top: 0.75rem;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 800;
  font-size: var(--geo-text-sm);
  /* audit-v11 F132: text on a light surface -> ink variant (accent was 4.23:1). */
  color: var(--geo-accent-ink);
  font-variant-numeric: tabular-nums;
}
.lp__stat-dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 2px;
  border: 2px solid var(--geo-fg);
  background: var(--geo-tertiary);
  flex-shrink: 0;
}
.lp__badge {
  margin-top: 0.625rem;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  width: fit-content;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  /* audit-v11 F132: text on a light surface -> ink variant (warning was 2.15:1). */
  color: var(--geo-warning-ink);
  background: rgba(245, 158, 11, 0.12);
  border: 2px solid var(--geo-fg);
  border-radius: 6px;
  padding: 0.125rem 0.5rem;
}
.lp__cta {
  align-self: flex-start;
}

/* --- Desktop: thẻ nằm ngang, CTA thẳng hàng mép phải --- */
@media (min-width: 768px) {
  .lp__list {
    gap: 1.75rem;
  }
  .lp__card-body {
    flex-direction: row;
    align-items: center;
    gap: 1.5rem;
    padding: 1.5rem 1.75rem;
  }
  .lp__stat {
    margin-top: 0.5rem;
  }
  .lp__cta {
    margin-left: auto;
    flex-shrink: 0;
  }
}

/* --- Reduced motion: hiện tức thì, không fill --- */
@media (prefers-reduced-motion: reduce) {
  .lp__item {
    opacity: 1;
    transform: none;
    transition: none;
  }
  .lp__rail-fill {
    transition: none;
  }
  .lp__node {
    transition: none;
  }
}
</style>
