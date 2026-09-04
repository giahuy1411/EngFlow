# Spec — audit-v6-full: Kiểm tra toàn diện + tối ưu + verify design system

**Ngày:** 2026-09-04 · **Người yêu cầu:** user · **Trạng thái:** implement đang chạy

## 1. Mục tiêu (từ yêu cầu gốc)

1. Quét & kiểm tra toàn bộ codebase + DB.
2. Kiểm tra backend bằng cách chạy **toàn bộ API**, kết hợp tương tác UI cho các chức năng tương ứng; soi kỹ chức năng chính, CRUD, AI.
3. Kiểm tra DB trong Docker + **tối ưu hiệu năng** dự án.
4. Kiểm tra frontend bằng **trình duyệt thật** (chrome-devtools-mcp / playwright-mcp), tương tác toàn bộ chức năng backend hiển thị trong UI/UX; **verify giao diện đồng bộ design system "Playful Geometric"** với font **Be Vietnam Pro** thay toàn bộ font gốc.
5. Báo cáo chi tiết: đã làm / chưa làm / đã fix + cách fix / skill đã nạp.

## 2. Lỗ hổng phát hiện trong prompt thiết kế (đã vá trước khi triển khai)

| # | Lỗ hổng | Cách vá |
|---|---------|---------|
| G1 | Prompt ghi "Lucide **React**" nhưng stack là **Vue** → không thể dùng lucide-react | Dùng `lucide-vue-next` đã có; giữ spec stroke 2.5px + bọc shape |
| G2 | Prompt dùng cú pháp Tailwind arbitrary (`bg-[url(...)]`, `ease-[cubic-bezier(...)]`) không nêu phiên bản Tailwind | Đã cấu hình token trong `tailwind.config.js` (bounce easing, keyframes) — không phụ thuộc arbitrary class |
| G3 | "Thay font Be Vietnam Pro" mâu thuẫn spec gốc (Outfit + Plus Jakarta Sans, 2 họ) → không nêu weight mapping | Chốt: 1 họ duy nhất BVP; heading 700/800, body 400/500 — **đã áp dụng từ audit-v5** (constitution P6), verify lại ở đợt này |
| G4 | Spec màu tertiary #FBBF24 / quaternary #34D399 làm **text** trên nền trắng không đạt contrast AAA (chỉ ~1.6–2.1:1) | Ghi rõ: 2 màu này chỉ dùng cho **shape/trang trí**, không dùng làm màu chữ nhỏ; text dùng fg/muted-fg |
| G5 | `prefers-reduced-motion` nêu chung chung, không chỉ định tắt những hiệu ứng nào | Chốt: tắt wiggle/pop-in/float/marquee + hover translate; giữ focus-visible |
| G6 | Prompt không nêu breakpoint cụ thể cho "mobile" | Dùng breakpoint Tailwind mặc định `md` (768px); shadow pop giảm còn 2px dưới `md` |
| G7 | Không có tiêu chí "verify đồng bộ" đo được | DoD bên dưới quy định checklist trang × tiêu chí pass/fail qua computed style thật |

## 3. Phạm vi

- **In:** backend API sweep (44+ GET + mutation qua UI), test suite 2 nền, DB audit + index, perf đo được, UI/UX walkthrough toàn bộ trang user + admin, design-system conformance, a11y cơ bản, CRUD + AI E2E.
- **Out:** đổi schema phá vỡ tương thích, thêm dependency mới, refactor lớn không phục vụ lỗi tìm thấy.

## 4. DoD (Definition of Done)

- [ ] Backend suite xanh (baseline 247 tests) + frontend 73/73 + build sạch.
- [ ] 100% GET endpoint trả 2xx với ID thật hoặc 404 đúng hành vi có chủ đích; không 5xx.
- [ ] CRUD qua UI: create→read→update→delete cho lesson, exercise, speaking-prompt, video-lesson; verify DB.
- [ ] AI endpoints hoạt động: ai-generate (async 202), assess speaking, ai-grade video attempt, translate-transcript, fetch-youtube.
- [ ] DB: báo cáo index + data-quality; áp dụng index mới nếu lợi ích rõ, đo trước/sau.
- [ ] UI: mọi trang chính computed font-family = "Be Vietnam Pro"; checklist design-system pass ≥ trang chính; 0 console error.
- [ ] Perf: hot endpoint < 300ms warm; báo cáo số liệu trước/sau.
- [ ] REPORT.md tiếng Việt đầy đủ + commit.

## 5. Ràng buộc

- Không commit `.env`/key/fixture; không viết Flyway migration (ddl-auto=update); schema đổi bằng SQL trực tiếp.
- Ollama local (qwen2.5:1.5b/3b), Whisper :9002, Supertonic :8001 — tôn trọng OLLAMA_MAX_LOADED_MODELS=1.
- Sub-agent chỉ đọc ở phase audit; ghi chỉ khi tới phase fix.
