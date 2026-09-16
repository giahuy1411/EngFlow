# Checklist — audit-v8-full

> Cập nhật **2026-09-16** (vòng audit-v8-full, Round 1). Số cũ của vòng 1–3 được ghi trong
> ngoặc khi thay đổi, để thấy rõ cái gì được **đo lại** chứ không phải chép lại.

## A. Backend
- [x] A1 enumerate toàn bộ endpoint mapping từ source (**144** unique mapping / 26 controller — sinh bằng `sweep/v8/endpoint_inventory.py`, tái lập được; số cũ "139/97" không có script sinh ra nên đã bỏ)
- [x] A2 probe từng endpoint × 3 vai trò (noauth/user/admin) với id thật — **144/144 covered, 0 uncovered** (`coverage_check.py`)
- [x] A3 CRUD roundtrip lesson/exercise/vocabulary/deck/speaking-prompt + validation + phân quyền
- [x] A4 streak: register → login → current/history, 401 noauth
- [x] A5 search/sort/filter: q, level, type, difficulty, size clamp, page âm, XSS payload
- [x] A6 grading: grade/submit/attempts/attempts-{id}, đáp án không lộ ở route public
- [x] A7 AI: generate-async/generate/generate-all/generate-batch(guard)/validate/status + vocab + prompt + rubric
- [x] A8 toàn bộ suite xanh (**378** backend / **89** frontend — trước vòng này là 369/85), không test giả; mỗi fix có regression test
- [x] A9 rate limit: burst thô → đúng ngưỡng 100 global / 10 AI / 15 upload / 10 order — **vòng này phát hiện 12 endpoint AI + 2 endpoint upload đi sai bucket** (F91/F92); nay đo bằng **key Redis thật** chứ không suy từ tên bucket
- [x] A10 **mới**: 16 endpoint từng chưa được probe (họ ALIAS `video-prompts`/`video-submissions` + write path `LessonController`) đã phủ bằng `p6.js` (52 probe / 0 FAIL)
- [x] A11 **mới**: F90 (nghi vấn bypass rate-limit do thứ tự filter) đã **bác bỏ bằng đo** — 0/17 surface `permitAll` không tốn bucket

## B. Database
- [x] B1 inventory + dung lượng per-table, không đếm sai do join fan-out
- [x] B2 FK integrity / orphan = 0; dữ liệu probe của audit = 0 — **đo cuối phiên: orphan ex/uvp_vocab/uvp_user/snap/sub = 0/0/0/0/0**, `zz_leftover=0`
- [x] B3 fragmentation đo bằng DMV; rebuild có số trước/sau
- [x] B4 index usage: tìm index thừa/thiếu, không xoá index tuỳ tiện
- [x] B5 top query theo logical reads; sửa query nóng, đo lại
- [x] B6 timezone convention naive-VN giữ nguyên; không "fix" sang UTC
- [x] B7 **mới**: parity sau mọi sweep tạo row — phát hiện **rò rỉ do harness** (`p3a` bị timeout 120 s giữa dòng nên không chạy bước cleanup) → 1 lesson + 4 exercise; đã dọn bằng `clean_r2.sql`, parity về **khớp baseline chính xác** (lessons 1471, exercises 43737, users 76, vocabulary 127, speaking_submissions 28, video_attempts 15, lesson_submissions 4, payment_transactions 126, decks 14, lesson_snapshots 5)

## C. Frontend/browser
- [x] C1 mọi route mount được, 0 console error, 0 API ≥400 ngoài dự kiến
- [x] C2 tương tác thật: login form, filter, tab bài học, grade, quiz/flashcard/SRS, dictionary, AI generator, logout
- [x] C3 font = Be Vietnam Pro 100% computed style — **vòng này đo lại mạnh hơn**: `document.fonts.check()` 400/700/900 = true (webfont **load thật**, không chỉ khai báo), **7 685 element → 0 element lệch font**, 0 hit `Outfit/Plus Jakarta Sans/Inter/Roboto/Poppins` trong cascade
- [x] C4 border 2px + hard shadow + radius theo token; không dùng token ma (hsl(var(--accent)))
- [x] C5 responsive: không tràn ngang **5 viewport 360/768/1280/1440/1920** (12 route × 5 = **60 tổ hợp**, 0 vượt ngưỡng nhiễu scrollbar 16 px)
- [x] C6 a11y: skip-link, focus-visible, prefers-reduced-motion, tap target ≥24px, alt ảnh

## D. Bảo mật
- [x] D1 chuỗi stored-XSS qua upload → reproduce trong browser thật → fix tận gốc 2 lớp → verify lại
- [x] D2 IDOR media proxy (ticket HMAC) còn hiệu lực
- [x] D3 role matrix 401/403 đúng trên toàn bộ admin surface
- [x] D4 enum/JSON rác → 400, không 500; ClassCastException từ input client = 0
- [x] D5 **mới**: hợp đồng lỗi — 10 response lỗi đo được 3/10 dùng shape cũ `{"error"}` (F94) → chuẩn hoá tại interceptor `api.js` (một chỗ) thay vì đổi 20 endpoint, vì **13 call-site frontend đang đọc `.error`**

## E. converge
- [x] E1 baseline mới ghi lại: backend **378**, frontend **89**, build OK (**176.80 kB** / gzip 67.39) — đã đồng bộ vào `AGENTS.md`
- [x] E2 cleanup: hàng/file do audit tạo đã xoá; DB về parity — **verify cuối phiên bằng truy vấn đếm + orphan**
- [x] E3 REPORT: đã làm / chưa làm / đã fix + cách fix / skill đã nạp — thêm **§2b** cho vòng audit-v8-full (F90–F94 + SWEEP-LEAK + GAP-COVERAGE-16 + CLAIM-COUNT + BASELINE-DRIFT + PLANB-VERIFY)
- [x] E4 **mới**: `git diff --check` sạch (0 trailing whitespace / conflict marker), không commit `.env`/secret/backup/PII
- [x] E5 **mới**: Plan B (Be Vietnam Pro) xác minh **ĐẠT** — bằng chứng 5 viewport ở `evidence/design-audit-r1.md`

## F. Trung thực học thuật (bắt buộc ghi rõ)
- [x] F1 **F90 là "không phải lỗ hổng"**, không phải "đã fix" — phân loại đúng để vòng sau không tưởng đã sửa code
- [x] F2 **F93 chỉ verified một phần**: đường 504 phủ bằng unit test + xác minh class trong container đang chạy; **chưa** ép được timeout sống (race phụ thuộc tải GPU) — đã ghi thẳng giới hạn này trong `findings.json` (`verificationBoundary`)
- [x] F3 mọi số trong REPORT đều truy được về log/harness cụ thể; không có số nào chép lại từ báo cáo cũ mà không đo
