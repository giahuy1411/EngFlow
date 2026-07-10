import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'

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
    path: '/lessons',
    name: 'Lessons',
    component: () => import('@/views/Lessons.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/lessons/:id',
    name: 'LessonDetail',
    component: () => import('@/views/lessons/LessonLayout.vue'),
    meta: { requiresAuth: true }
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
      { path: 'achievements', name: 'AdminAchievements', component: () => import('@/views/admin/AdminAchievements.vue') },
      { path: 'submissions', name: 'AdminSubmissions', component: () => import('@/views/admin/AdminSubmissions.vue') },
      { path: ':id/build', name: 'AdminLessonBuilder', component: () => import('@/views/admin/AdminLessonBuilder.vue'), props: true }
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

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    next('/login')
  } else if (to.meta.guestOnly && auth.isLoggedIn) {
    next('/lessons')
  } else if (to.meta.requiresAdmin && !auth.isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router
