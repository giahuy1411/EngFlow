import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import { safeRedirect } from '@/utils/safeRedirect'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPassword.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/ResetPassword.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/lessons',
    name: 'Lessons',
    component: () => import('@/views/Lessons.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/lessons/:id',
    name: 'LessonDetail',
    component: () => import('@/views/lessons/LessonLayout.vue'),
    meta: { requiresAuth: false }
  },
  // Video learning — Premium gated như Luyện nói (admin bypass qua guard)
  {
    path: '/videos',
    name: 'VideoLibrary',
    component: () => import('@/views/videos/VideoLibrary.vue'),
    meta: { requiresAuth: true, requiresPremium: true }
  },
  {
    path: '/videos/:id',
    name: 'VideoLesson',
    component: () => import('@/views/videos/VideoLesson.vue'),
    meta: { requiresAuth: true, requiresPremium: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/SearchVocabulary.vue')
  },
  {
    path: '/leaderboard',
    name: 'Leaderboard',
    component: () => import('@/views/Leaderboard.vue')
  },
  {
    path: '/decks',
    name: 'Decks',
    component: () => import('@/views/luyentu/Decks.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/decks/:id',
    name: 'DeckDetail',
    component: () => import('@/views/luyentu/DeckDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/decks/:id/play/flashcard',
    name: 'FlashcardGame',
    component: () => import('@/views/luyentu/FlashcardGame.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/decks/:id/play/quiz',
    name: 'QuizGame',
    component: () => import('@/views/luyentu/QuizGame.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/decks/:id/play/memory',
    name: 'MemoryMatchGame',
    component: () => import('@/views/luyentu/MemoryMatchGame.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/decks/:id/play/typing',
    name: 'TypingGame',
    component: () => import('@/views/luyentu/TypingGame.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/decks/:id/play/listening',
    name: 'ListeningGame',
    component: () => import('@/views/luyentu/ListeningGame.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/decks/:id/play/mixed',
    name: 'MixedGame',
    component: () => import('@/views/luyentu/MixedGame.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ai-vocab-generator',
    name: 'AiVocabGenerator',
    component: () => import('@/views/luyentu/AiVocabGenerator.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/decks/create',
    name: 'DeckCreate',
    component: () => import('@/views/luyentu/DeckCreate.vue'),
    meta: { requiresAuth: true }
  },
  // Speaking routes — Premium gated (admin bypasses via isAdmin check below)
  {
    path: '/speaking',
    name: 'Speaking',
    component: () => import('@/views/speaking/SpeakingList.vue'),
    meta: { requiresAuth: true, requiresPremium: true }
  },
  {
    path: '/speaking/history',
    name: 'SpeakingHistory',
    component: () => import('@/views/speaking/SubmissionHistory.vue'),
    meta: { requiresAuth: true, requiresPremium: true }
  },
  {
    path: '/speaking/:id',
    name: 'SpeakingDetail',
    component: () => import('@/views/speaking/SpeakingDetail.vue'),
    meta: { requiresAuth: true, requiresPremium: true }
  },
  {
    path: '/speaking/:id/record',
    name: 'SpeakingRecord',
    component: () => import('@/views/speaking/SpeakingRecord.vue'),
    meta: { requiresAuth: true, requiresPremium: true }
  },
  // Premium routes
  {
    path: '/premium',
    name: 'PremiumPage',
    component: () => import('@/views/premium/PremiumPage.vue')
  },
  {
    path: '/premium/checkout',
    name: 'PremiumCheckout',
    component: () => import('@/views/premium/PremiumCheckout.vue'),
    meta: { requiresAuth: true }
  },
  // Admin routes
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/AdminDashboard.vue') },
      { path: 'lessons', name: 'AdminLessons', component: () => import('@/views/admin/AdminLessons.vue') },
      { path: 'exercises', name: 'AdminExercises', component: () => import('@/views/admin/AdminExercises.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/AdminUsers.vue') },
      { path: 'speaking-prompts', name: 'AdminSpeakingPrompts', component: () => import('@/views/admin/AdminSpeakingPrompts.vue') },
      { path: 'speaking-submissions', name: 'AdminSpeakingSubmissions', component: () => import('@/views/admin/AdminSpeakingSubmissions.vue') },
      { path: 'videos', name: 'AdminVideoLessons', component: () => import('@/views/admin/AdminVideoLessons.vue') },
      { path: 'video-attempts', name: 'AdminVideoAttempts', component: () => import('@/views/admin/AdminVideoAttempts.vue') }
    ]
  },
  // Redirect any other path to home
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()

  if (to.meta.requiresPremium && !auth.isAdmin && !auth.isPremium) {
    next('/premium?redirect=' + encodeURIComponent(to.fullPath))
    return
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    // audit-v13 F-13-20: carry the destination so Login can return the user here.
    next('/login?redirect=' + encodeURIComponent(to.fullPath))
    return
  }

  if (to.meta.guestOnly && auth.isLoggedIn) {
    // audit-v13 F-13-20: a logged-in user opening /login?redirect=/profile should still
    // reach /profile — honour the destination instead of always dropping it. Guard against
    // a self-referential target (e.g. /login?redirect=/login) so the bounce can never loop.
    const target = safeRedirect(to.query.redirect)
    next(target === to.path ? '/lessons' : target)
    return
  }

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    next('/')
    return
  }

  next()
})

export default router
