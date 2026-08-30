# ADR-001: Khắc Phục Lỗi Bể Giao Diện UI/UX Do Thiếu Import CSS Layout

## Status
Accepted

## Date
2026-08-30

## Context & Problem Statement
Giao diện ứng dụng web EngFlow (`http://127.0.0.1:5173/`) gặp sự cố nghiêm trọng về hiển thị (bể UI/UX nặng, các khối nội dung xếp chồng không có định dạng, font chữ không thu phóng, thanh điều hướng navbar không có kiểu dáng, footer và hero section bị mất toàn bộ bố cục). 

Thông qua công cụ kiểm tra trình duyệt tự động và phân tích nguồn gốc (Chrome DevTools Protocol trên cổng 9222 & mã nguồn tại `C:\Users\ASUS\Documents\LAPTRINH\engflow\frontend`), chúng tôi tiến hành chẩn đoán nguyên nhân gốc rễ và đưa ra giải pháp khắc phục triệt để.

---

## Technical Investigation & Root Cause Analysis

### 1. Phân Tích Cấu Trúc CSS Trình Diễn (Playful Geometric Design System)
Mã nguồn frontend sử dụng hệ thống thiết kế Playful Geometric với các tệp CSS được chia tách trong `src/assets/`:
- `main.css`: Chứa chỉ thị `@tailwind base; @tailwind components; @tailwind utilities;`.
- `design-system.css`: Chứa khai báo biến CSS custom properties (`--geo-*`) và tokens base.
- `app-logo.css`: Chứa quy tắc CSS cho logo (`.app-logo`, `.app-logo__mark`, ...).
- `app-layout.css` (17.7 KB): Chứa **toàn bộ quy tắc định kiểu khung và bố cục trang web** như `.app-navbar`, `.app-hero`, `.app-hero__title`, `.app-hero__grid`, `.app-features`, `.app-cta`, `.app-footer`, `.app-admin`, `.app-auth`...

### 2. Nguyên Nhân Gốc Rễ (Root Cause)
Tệp điểm đầu của ứng dụng (`src/main.js`) ban đầu chỉ chứa khai báo:
```javascript
import './assets/main.css'
import './assets/design-system.css'
```
**Tệp `src/main.js` hoàn toàn thiếu (omitted) câu lệnh import 2 tệp `app-logo.css` và `app-layout.css`.**

Do đó, mặc dù mã HTML có chứa đầy đủ các class giao diện như `class="app-navbar"`, `class="app-hero__title"`, ở môi trường runtime trình duyệt **hoàn toàn không có bất kỳ CSS rule nào cho các class này**. Trình duyệt phải dùng kiểu dáng mặc định (`font-size: 16px`, `display: block`, `position: static`), khiến giao diện rơi vào trạng thái bể UI/UX hoàn toàn.

---

## Decision & Implementation

Chúng tôi quyết định bổ sung import trực tiếp 2 tệp `app-logo.css` và `app-layout.css` vào entry point chính của ứng dụng (`src/main.js`).

### Thay đổi chi tiết trong Mã nguồn (`C:\Users\ASUS\Documents\LAPTRINH\engflow\frontend\src\main.js`):
```javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// Asset imports - Enforce complete design system & layout stylesheet loading
import './assets/main.css'
import './assets/design-system.css'
import './assets/app-logo.css'
import './assets/app-layout.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

app.mount('#app')
```

### Khai báo bổ sung trong (`C:\Users\ASUS\Documents\LAPTRINH\engflow\frontend\src\assets\main.css`):
> **Đã rút gọn (2026-08-30):** hai dòng `@import './app-logo.css'; @import './app-layout.css';` ban đầu khai báo tại đây đã bị xóa vì trùng lặp với import trong `src/main.js` (CSS bị nạp 2 lần). Chỉ giữ duy nhất đường nạp qua `main.js`.

---

## Runtime Verification & Empirical Results

Việc kiểm chứng được thực hiện trực tiếp thông qua Chrome DevTools Protocol (CDP) kết nối tới trình duyệt đang chạy tại `http://127.0.0.1:5173/`. Kết quả so sánh thuộc tính Computed Style trước và sau khi sửa lỗi:

| Phần tử Giao diện | Thuộc tính (Property) | Trạng thái Trước khi sửa (Broken) | Trạng thái Sau khi sửa (Fixed) | Kết quả kiểm chứng |
| :--- | :--- | :--- | :--- | :--- |
| **Navbar (`header.app-navbar`)** | `position` | `static` | `sticky` | ✅ Khung navbar cố định trên đỉnh trang |
| | `borderBottom` | `0px solid ...` | `2px solid rgb(30, 41, 59)` | ✅ Viền đen định hình chuẩn thiết kế |
| | `boxShadow` | `none` | `rgba(139, 92, 246, 0.35) 0px 4px 0px 0px` | ✅ Hiệu ứng đổ bóng Playful Geometric chuẩn |
| **Logo (`.app-logo`)** | `display` | `inline` | `flex` | ✅ Logo căn chỉnh hàng ngang chuẩn |
| | `fontWeight` | `400` | `800` | ✅ Font đậm đúng thiết kế logo |
| **Hero Title (`.app-hero__title`)**| `fontSize` | `16px` | **`72px`** | ✅ Tiêu đề lớn hiển thị đúng tỉ lệ Playful |
| | `fontWeight` | `400` | **`900`** | ✅ Font siêu đậm 900 nổi bật |

---

## Consequences & Recommendations

### Tích cực (Positive):
1. **Khôi phục 100% Giao diện UI/UX:** Toàn bộ Navbar, Hero section, Feature grid, CTA, Auth form, Admin shell và Footer đã nhận đủ kiểu dáng và bố cục thiết kế.
2. **Kiến trúc Import rõ ràng:** Mọi stylesheet phục vụ layout đều được tập trung khai báo tại `src/main.js` giúp tránh việc quên stylesheet khi refactor.

### Nguyên tắc bảo trì tương lai (Future Guidelines):
- **Karpathy Simplicity Guideline:** Giữ các file CSS module hóa nhưng phải luôn đảm bảo entry point `main.js` import đầy đủ các phụ thuộc CSS.
- **Automated Visual Testing:** Nên bổ sung test tự động kiểm tra sự tồn tại của computed style chính (ví dụ font-size của `.app-hero__title`) trong bộ test E2E để phát hiện sớm các sự cố mất CSS.
