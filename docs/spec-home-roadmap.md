# Spec: Section "Lộ trình học tập" trên trang chủ EngFlow (v2 — sau adversarial review)

## Objective
Khách vãng lai mở trang chủ thấy ngay web có gì và học theo thứ tự nào, qua 1 section stepper ngang 5 bước, mỗi bước = 1 nhóm chức năng + số liệu thật. Định hướng: người xem là khách chưa đăng nhập.

Đã chốt với owner (4 lựa chọn): nội dung = **kết hợp chức năng + số liệu thật**; hiển thị = **stepper ngang 4-5 bước**; đối tượng = **khách vãng lai**; phạm vi = **MVP tĩnh** (không backend mới, số liệu chốt tĩnh từ DB, nâng cấp API sau).

## Tech Stack (từ package.json / pom.xml — đã đọc)
- Frontend: Vue 3.5 + Vite 5 + Pinia 2 + vue-router 4, Tailwind 3.4, lucide-vue-next, vitest + @vue/test-utils + jsdom
- Backend: Spring Boot (không đụng trong MVP này)

## Commands
- Test: `cd frontend && npx vitest run`
- Build: `cd frontend && npx vite build`
- Dev: docker `engflow-frontend` (Vite dev, mount ./frontend:/app, HMR tự chạy) — http://localhost:5173

## Project Structure (những gì sẽ chạm)
```
frontend/src/components/home/LearningPath.vue     → NEW: section stepper 5 bước
frontend/src/components/home/LearningPath.test.js → NEW: test render + link (mount với createRouter thật)
frontend/src/views/Home.vue                       → EDIT: thêm section + thay stats hardcode
```
Style: chỉ Tailwind utilities + token `--geo-*` **trong file component** — KHÔNG thêm vào `design-system.css` (giữ đúng 3 file). Lý do không dùng `AppStepper` có sẵn: nó là progress indicator không có link/stats, không phù hợp navigation — quyết định có chủ đích, không phải bỏ qua.

## Nội dung section (nguồn sự thật = DB query ngày 2026-08-30)
| Bước | Tên | Số liệu hiển thị | Link | Ghi chú khách |
|---|---|---|---|---|
| 1 | Học bài theo lộ trình | 1.473 bài · 4 trình độ · 7 kỹ năng | /lessons | mở tự do |
| 2 | Luyện từ vựng | 11 bộ thẻ công khai · 6 game | /decks | game cần đăng nhập → chú thích nhỏ |
| 3 | Luyện nói cùng AI | 107 bài · AI chấm phát âm | /premium?redirect=%2Fspeaking | **tính năng Premium** → dẫn tới trang Premium, badge "Premium" |
| 4 | Tra từ nhanh | tra nghĩa · phát âm | /search | mở tự do |
| 5 | Thi đua & giữ nhịp | streak · bảng vàng · 41 người học | /leaderboard | mở tự do |

Số liệu đã verify từ DB ngày 2026-08-30: lessons 1.473 (El 507 / Pre 378 / Int 317 / Upper 271); speaking 107; decks **11 công khai** (`WHERE is_public=1`, tổng 13 trừ 2 private — không dùng 13); games 6 (đếm từ router `/decks/:id/play/*` — nguồn = router, không phải DB, được phép theo contract); users **41 người học** (`WHERE is_admin=0`, tổng 46 trừ 5 admin).

Hero stats thay "50+ / 1K+ / 500+" bằng: **1.4K+ bài học · 43K+ câu hỏi · 4 trình độ** (43K = 43.727 từ DB; 1.4K = 1.473). Không hiển thị số người học ở hero (41 quá nhỏ so với đối thủ — số này chỉ nằm ở bước 5 như dấu "cộng đồng đang lớn dần", không phải con số bán hàng).

Ghi chú Premium (bước 2 game + bước 3): hiển thị icon khóa/badge nhỏ — trung thực với khách, không rình là miễn phí rồi đá vào trang Premium.

## Code Style
- Dùng lại: `PageSection`, `StickerCard`, `AppButton`, icon lucide (stroke 2.5 auto qua `.lucide`), token `--geo-*`. Không thêm dependency.
- Component functional, `<script setup>`, data là mảng const trong file (không fetch).

## Testing Strategy
- vitest: mount LearningPath với `createRouter` + `createWebHistory` thật (không stub router-link); assert 5 bước, đúng link từng bước; Home.vue: assert không còn chuỗi "500+".
- Runtime: mở http://localhost:5173 bằng browser thật — snapshot + screenshot + console sạch.

## Boundaries
- Always: số liệu phải khớp DB query 2026-08-30 hoặc đếm từ router; test pass; không đổi route/layout khác.
- Ask first: muốn thêm API stats thật (sau MVP), muốn đổi nội dung bước nào.
- Never: sinh số liệu không có nguồn DB/router; không để guest click vào route bị guard mà không có badge giải thích.

## Responsive (đã xử lý cả tablet — lỗi #7 của review)
- <768px: dồn 1 cột dọc.
- 768–1023px: grid 2 cột × 3 hàng (bước 5 chiếm full width) — tránh overflow ngang.
- ≥1024px: stepper ngang 5 cột, các bước nối bằng đường mờ.

## Testing Strategy bổ sung (lỗi #10)
- Test LearningPath dùng router thật; test Home.vue hero stats; cả 2 test mới chạy chung bộ `npx vitest run`.

## Success Criteria
- [ ] Home hiển thị section lộ trình 5 bước; khách (chưa login) click từng bước không bị đá về login ngoài ý muốn (bước 3 chủ đích vào /premium).
- [ ] Không cần scroll ngang ở 768px, 1024px và mobile.
- [ ] Mỗi bước điều hướng đúng route trong bảng trên.
- [ ] Không còn chuỗi "50+", "1K+", "500+" hardcode sai sự thật ở hero.
- [ ] Mọi số liệu hiển thị truy vết được về DB query 2026-08-30 hoặc router.
- [ ] `npx vitest run` pass (toàn bộ, gồm test mới); `vite build` pass.
- [ ] Console không có error mới khi load Home.

## Trade-off đã chấp nhận (từ adversarial review, không sửa)
- **T-1 (lỗi #5):** số liệu DB có thể ≠ số guest thấy trên trang đích (vd decks private bị filter). Chấp nhận: con số lấy từ nguồn chính thức trong DB, sai lệch nhỏ chấp nhận được ở MVP tĩnh; nâng cấp API stats thật sẽ tự sửa.
- **T-2 (lỗi #11):** bước 2 quảng bá game cần đăng nhập cho khách. Chấp nhận: badge "cần đăng nhập" đủ trung thực mà không phá "học vui".

## Đã sửa từ review v1 (7 actionable)
1. Bước 3 `/speaking` → `/premium?redirect=%2Fspeaking` + badge Premium (critical).
2. Ngày provenance 2026-09 (tương lai) → 2026-08-30 (hôm nay, đúng thực tế).
3. Mâu thuẫn 46 users: hero bỏ, bước 5 giữ với lý do rõ + số sửa thành 41 (người học thật, trừ 5 admin).
4. "13 bộ thẻ" → "11 bộ thẻ công khai" (theo `is_public=1`).
5. "6 game" ghi rõ nguồn = router (đếm route), không phải DB.
6. Bổ sung breakpoint tablet 768–1023px (2 cột).
7. Ghi rõ style nằm trong component, không đụng design-system.css; ghi rõ lý do không dùng AppStepper.
+ Test coverage: ghi rõ mount router thật + test Home.vue hero.

## Open Questions
- Không có blocker — nội dung/sắc từ chỉnh được sau khi thấy preview.
