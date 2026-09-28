import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// Thứ tự import CSS quan trọng: `main.css` (reset + biến nền) phải nạp TRƯỚC để
// `design-system.css` ghi đè token, rồi tới các stylesheet thành phần
// (`app-logo.css`, `app-layout.css`). Đổi thứ tự có thể làm mất/ghi đè style.
import './assets/main.css'
import './assets/design-system.css'
import './assets/app-logo.css'
import './assets/app-layout.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

app.mount('#app')

// GOTCHA: cố ý KHÔNG gọi `authStore.fetchUser()` lúc boot.
//
// `store/modules/auth.js` khởi tạo `isAdmin`/`isPremium` NGAY từ `localStorage.user`
// (đồng bộ, trước khi mount). Nếu main.js gọi `/me` ở đây thì `user` chỉ có sau một
// vòng mạng — mà router guard chạy trước đó, nên harness headless seed chỉ `token`
// (không kịp có `user`) sẽ thấy `isAdmin === undefined` và bị đá `/admin/*` về `/`.
// Vì vậy hydrate `user` được để cho App.vue (`onMounted` → `if (auth.isLoggedIn)
// auth.fetchUser()`) chạy SAU khi app đã mount. Xem AGENTS.md mục
// "HARNESS-TOKEN-ONLY-SEED".
