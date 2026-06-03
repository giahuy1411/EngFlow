# 02 — Frontend Vue.js 3

---

## 1. Composition API

```
🔴 Dùng <script setup> — KHÔNG dùng Options API
🔴 Không mix Composition API và Options API trong cùng component
🟡 Tách logic phức tạp ra composables (src/composables/useXxx.js)
```

```vue
<!-- ✅ Đúng -->
<script setup>
import { ref, computed, onMounted } from 'vue'
</script>

<!-- ❌ Sai -->
<script>
export default {
  data() { return { ... } },
  methods: { ... }
}
</script>
```

---

## 2. Naming Convention

| Loại | Convention | Ví dụ |
|------|-----------|-------|
| Component / View file | PascalCase | `LessonCard.vue`, `LessonDetail.vue` |
| Composable file | camelCase, prefix `use` | `useLesson.js`, `useAuth.js` |
| Store module | camelCase, prefix `use` | `useLessonStore`, `useAuthStore` |
| Service file | camelCase, suffix `Service` | `lessonService.js` |
| CSS class (custom) | kebab-case | `.lesson-card`, `.progress-bar` |
| Prop | camelCase (JS) / kebab-case (template) | `lessonId` / `:lesson-id` |
| Emit | kebab-case | `update-progress`, `lesson-complete` |

```vue
<!-- ✅ Props và emits đúng -->
<script setup>
const props = defineProps({
  lessonId: { type: Number, required: true },
  isCompleted: { type: Boolean, default: false }
})
const emit = defineEmits(['update-progress', 'lesson-complete'])
</script>

<!-- ❌ Sai tên emit -->
const emit = defineEmits(['updateProgress', 'LessonComplete'])
```

---

## 3. HTTP Calls — Service Pattern

```
🔴 TUYỆT ĐỐI không gọi Axios trực tiếp trong component hoặc store
🔴 Mọi HTTP call phải đi qua file service tương ứng trong src/services/
🔴 api.js là điểm duy nhất cấu hình Axios instance (baseURL, interceptors)
```

```js
// ✅ Đúng — qua service
// src/services/lessonService.js
import api from './api'
export default {
  getAll: () => api.get('/api/lessons').then(r => r.data),
}

// store dùng service
import lessonService from '@/services/lessonService'
const items = await lessonService.getAll()

// ❌ Sai — gọi axios thẳng trong component
import axios from 'axios'
const { data } = await axios.get('http://localhost:8080/api/lessons')  // ❌
```

---

## 4. State Management — Pinia

```
🔴 Dùng Pinia — KHÔNG dùng Vuex
🔴 Mỗi domain có 1 store riêng (auth, lesson, exercise, progress)
🔴 Không để raw API data trực tiếp vào component state — phải qua store
🟡 Store chỉ chứa global state — local/ephemeral state dùng ref() trong component
```

```js
// ✅ Store structure chuẩn
export const useLessonStore = defineStore('lesson', () => {
  // State — ref()
  const items = ref([])
  const loading = ref(false)
  const error = ref(null)

  // Getters — computed()
  const publishedLessons = computed(() =>
    items.value.filter(l => l.isPublished)
  )

  // Actions — async functions
  async function fetchAll() {
    loading.value = true
    try {
      items.value = await lessonService.getAll()
    } catch (e) {
      error.value = e.response?.data?.error ?? 'Lỗi tải bài học'
    } finally {
      loading.value = false
    }
  }

  return { items, loading, error, publishedLessons, fetchAll }
})
```

---

## 5. Component Rules

```
🔴 Mỗi component làm đúng 1 việc (Single Responsibility)
🔴 Component không dài hơn 200 dòng — nếu dài hơn, tách thành sub-components
🔴 Luôn có loading state và error state khi fetch data
🟡 Không hardcode text tiếng Việt trong component — để riêng constants hoặc i18n sau
🟢 Thêm key cho v-for: dùng id thực, không dùng index
```

```vue
<!-- ✅ v-for với key thực -->
<LessonCard v-for="lesson in lessons" :key="lesson.id" :lesson="lesson" />

<!-- ❌ key là index -->
<LessonCard v-for="(lesson, index) in lessons" :key="index" />

<!-- ✅ Loading + Error state -->
<template>
  <div v-if="store.loading" class="text-center py-5">
    <div class="spinner-border text-primary"></div>
  </div>
  <div v-else-if="store.error" class="alert alert-danger">{{ store.error }}</div>
  <div v-else>
    <!-- content -->
  </div>
</template>
```

---

## 6. Thứ Tự Blocks Trong .vue File

```
🟡 Thứ tự nhất quán trong mọi component:
1. <template>
2. <script setup>
3. <style scoped>
```

```vue
<!-- ✅ Thứ tự chuẩn -->
<template>...</template>

<script setup>
// 1. Imports (vue, vue-router, pinia, services, components)
// 2. defineProps / defineEmits
// 3. Store instances
// 4. Reactive state (ref, reactive)
// 5. Computed
// 6. Methods
// 7. Lifecycle hooks (onMounted, onUnmounted)
</script>

<style scoped>
/* Chỉ CSS riêng của component */
/* Không override Bootstrap global ở đây */
</style>
```

---

## 7. Bootstrap 5 Usage

```
🔴 Dùng Bootstrap utility classes — hạn chế CSS tùy chỉnh
🟡 Không override Bootstrap variables trong component — làm trong custom-bootstrap.css
🟡 Responsive: mobile-first, dùng col-12 col-md-6 col-lg-4 pattern
🔴 Không dùng inline style (style="...") — dùng class hoặc :class binding
```

```vue
<!-- ✅ Đúng — Bootstrap classes + :class binding -->
<div :class="['card', { 'border-success': isCompleted, 'border-secondary': !isCompleted }]">

<!-- ❌ Sai — inline style -->
<div :style="{ border: isCompleted ? '1px solid green' : '1px solid gray' }">
```

---

## 8. Router Guards

```
🔴 Route cần auth phải có meta: { requiresAuth: true }
🔴 Admin routes phải có meta: { requiresAdmin: true }
🟡 Dùng lazy loading (import()) cho tất cả Views — không import trực tiếp
```

```js
// ✅ Lazy loading + meta
{
  path: '/lessons',
  component: () => import('@/views/Lessons.vue'),
  meta: { requiresAuth: true }
}

// ❌ Direct import
import Lessons from '@/views/Lessons.vue'
{ path: '/lessons', component: Lessons }
```

---

## 9. Environment Variables

```
🔴 Dùng VITE_ prefix cho biến môi trường Vite
🔴 Không hardcode URL/port trong code — dùng import.meta.env
🔴 File .env.local không được commit lên git
```

```js
// ✅
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// ❌
const baseURL = 'http://localhost:8080'  // hardcode
```

---

## 10. Code Hygiene

```
🔴 Không có console.log khi commit (dùng Vue DevTools thay thế)
🔴 Không có unused imports, unused variables (ESLint sẽ bắt)
🟡 Xóa comment code cũ — dùng git history thay thế
🟢 Viết comment cho logic phức tạp (spaced repetition algorithm, score calculation)
```
