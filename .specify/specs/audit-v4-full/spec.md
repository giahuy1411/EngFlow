# Spec: audit-v4-full — Kiểm thử toàn diện lần 2 + thanh toán thật + tối ưu DB

**Feature dir:** `.specify/specs/audit-v4-full/` · **Ngày:** 2026-09-03 · **Baseline:** commit `d4501a6` (main, clean)
**Tiền đề:** audit-v3 (`.specify/specs/audit-v3/REPORT.md`) — 7 bug đã sửa, 5 mục còn tồn.

## 1. Vấn đề

Audit-v3 đã PASS các chức năng chính nhưng còn tồn: (1) thanh toán premium chưa
test với chuyển khoản thật; (2) `payment.status` ~3,5s chưa tối ưu; (3) 89/449
listening thiếu audio; (4) 39 input động thiếu id/name (a11y); (5) dữ liệu `?`
cũ trong lesson_blocks; (6) chưa có vòng test lặp mở rộng để bắt regression
mới. Người dùng yêu cầu một đợt audit **toàn diện hơn audit-v3, lặp test nhiều
vòng, không lo thời gian**, và lần này **chủ động nhắc user chuyển khoản thật**.

## 2. Phạm vi (In scope)

### S1. Backend API — full sweep mở rộng (hơn audit-v3)
- Chạy `mvnw.cmd test` → đo baseline thực tế (kỳ vọng ≥221).
- Gọi HTTP thật trên :8080 **tất cả 26 controllers**, gồm cả nhóm chưa đụng ở v3:
  LessonSnapshot, LessonStructure, LessonSubmission, MediaProxy, Progress, SRS,
  Streak, AdminAnswerBackfill, AdminExerciseSeed, AdminAiExercise (202 async),
  AiVocab, Dashboard, Game (mỗi loại game), Flashcard, Deck CRUD đầy đủ,
  VideoLesson admin+public, Speaking prompt/submission, Leaderboard, Vocabulary,
  Payment (create/status/webhook-guard), Auth (register/forgot/reset/profile).
- Kiểm tra cả **negative paths**: 401 không token, 403 sai role, 400 body sai,
  404 id không tồn tại.

### S2. Frontend
- `npx vitest run` + `npx vite build`.
- Browser thật (chrome-devtools-mcp; cross-check playwright-mcp khi cần):
  toàn bộ view người dùng + admin, verify UI ↔ API ↔ DB khớp số.
- Lặp smoke nhiều vòng (≥2 vòng sau mọi đợt fix).

### S3. Design system "Playful Geometric" + Be Vietnam Pro
- **Vá design-prompt** (đã vá vào constitution P6, kỳ này rà lại thêm):
  thêm success criteria đo được, scope lock, stop condition "KHÔNG auto-accept
  dialog xác nhận trình duyệt — mọi DELETE phải dừng hỏi" (bài học sự cố lesson 444),
  cấm #F472B6 làm màu chữ trên nền trắng, khai báo light-mode only, quy ước
  z-index decoration không đè text.
- Grep verify: `Outfit`/`Plus Jakarta Sans` = 0; `Be Vietnam Pro` trong
  index.html + design-system.css; `--geo-*`, `shadow-pop*` dùng rộng;
  `prefers-reduced-motion` hiện diện.
- Screenshot các trang chính ở 375px + 1440px, kiểm tra không vỡ layout,
  decoration không che text.

### S4. DB + hiệu năng
- Quét schema: mọi cột text là NVARCHAR (kỳ vụ `?` — rà **toàn bộ** lesson_blocks,
  không chỉ 3 block cũ), orphan rows, index thiếu cho query nóng.
- Tối ưu `payment.status` (~3,5s → mục tiêu <500ms trong cửa sổ poll):
  đo trước/sau theo P5.
- Kiểm tra tổng thể: top query tốn resource, missing index, Redis hit-rate.

### S5. Premium payment — chuyển khoản THẬT (gate người dùng)
- Bật UI premium + checkout trong browser thật, tạo order mới.
- **DỪNG và nhắc user chuyển khoản bằng tay** (AskUserQuestion) với đúng nội dung
  chuyển khoản (số tiền, nội dung CK, ngân hàng).
- Sau khi user xác nhận đã CK: poll payment.status, xác minh upgrade premium
  thực sự ghi DB + UI反映 (badge Premium, tính năng mở khóa).
- Nếu SePay token vẫn 401 → chẩn đoán nguyên nhân gốc, hướng dẫn user cách cấp
  token/webhook đúng, và test tối đa được đường hiện có (QR đúng, order đúng,
  polling hành vi đúng khi tiền vào).

### S6. AI (local)
- Sinh bài trong app (admin UI) với guards hiện có; AiVocab; game quiz từ deck;
  speaking rubric (3b) nếu thời gian model cho phép.

## 3. Out of scope
- Không thêm tính năng mới, không đổi kiến trúc, không thêm dependency mới
  (trừ font Be Vietnam Pro đã có).
- Không re-seed 89 listening thiếu audio qua MCP (tốn giờ GPU; fallback speech
  đang hoạt động — chỉ ghi nhận lại trạng thái).
- Không đổi schema ngoài NVARCHAR fix/index mới được chỉ ra bởi bằng chứng.

## 4. Yêu cầu nghiệm thu (success criteria — đo được)
- R1: Backend test suite xanh, số test ghi đúng thực tế.
- R2: Mọi nhóm API chính PASS ở cả positive lẫn negative path; lỗi tìm thấy
  được sửa tận gốc (root cause, không vá bề mặt) + commit Conventional Commits.
- R3: Frontend test + build xanh; UI các chức năng chính verified trên browser
  thật ≥2 vòng, không console error mới.
- R4: Design grep đạt 100% tiêu chí S3; font hiển thị thực tế trên browser là
  Be Vietnam Pro (verify qua computed style).
- R5: `payment.status` đo được before/after; cải thiện có số liệu.
- R6: Payment gate: user được nhắc đúng thời điểm, kết quả chuyển khoản thật
  được ghi nhận (đã upgrade / chưa do token-webhook) — minh bạch cả hai trường hợp.
- R7: Báo cáo cuối `REPORT.md` theo mẫu audit-v3: đã làm / chưa làm / đã fix /
  fix thế nào / skills đã nạp.

## 5. Rủi ro & safeguards
- Automation KHÔNG được tự accept dialog xác nhận (xóa/overwrite) — dừng hỏi.
- Không xóa data thật; test data phải có tiền tố/tên nhận diện và được dọn sau.
- Mọi lệnh đổi DB phải chạy sau khi đã dump bảng liên quan (SELECT ra file).
