# audit-v21-full — Findings

> Mọi finding kèm bằng chứng đo được. Không suy đoán.

| ID | Mức | Mô tả | Bằng chứng | Trạng thái |
|---|---|---|---|---|
| **F-21-03** | **HIGH** | **Comment AI sinh làm VỠ component Vue.** Agent thêm comment HTML ngay sau `<template>` ở 3 file → template có **2 root node** (comment + element) ⇒ Vue coi là **fragment** ⇒ `wrapper.attributes()` trả `undefined` ⇒ `decor-props.test.js` **7 test FAIL**. Đây là hậu quả nghiêm trọng thật mà quy trình "test sau mỗi lô" bắt được. | `npx vitest run` → **7 failed**; A/B: giữ comment = 7 fail, bỏ comment = 3 fail (3 còn lại là DecoConfetti) | **FIXED** — bỏ comment ở cấp gốc template (chuyển nội dung vào JSDoc `<script setup>`); 194 passed. Đồng thời **gia cố `comment_only.py`**: với `.vue`, KHÔNG strip comment HTML (comment template là cấu trúc, không phải chú thích) ⇒ biến thể lỗi này bị bắt là code-changed. |
| **F-21-01** | MED | Default audit round còn `audit-v20-full` ở 2 nơi → probe ghi evidence sai thư mục (lớp F-20-02b). | `assert-harness.js` check 6+8 FAIL trước → PASS sau | **FIXED** |
| **F-21-02** | LOW | `harness-restore.md` thiếu trong `audit-v21-full/evidence/` → check 5 FAIL. | check 5 FAIL → PASS | **FIXED** |
| **F-21-04** | LOW | **Công cụ `comment_only.py` che string literal** ⇒ thay đổi CHỈ ở string (URL, tên role, regex) bị coi là comment-only. Bằng chứng thật: `_config.js`/`api-sweep.js` đổi `"audit-v20-full"→"audit-v21-full"` mà vẫn báo comment-only. Do security review (F-1) phát hiện. | `--files _config.js api-sweep.js` → `comment-only: 2/2` (SAI) | **FIXED** — bỏ mask string (giữ nguyên nội dung) ⇒ nay `0/2` (đúng). Thêm lọc binary (không crash PNG) + ép stdout UTF-8 (không crash cp1252). Mutation-test xác nhận. |
| **F-21-05** | INFO | `comment_only.py` báo `speech.js` là code-changed — **false positive**: regex `` /`[^`]*`/g `` chứa backtick làm parser template-string desync. Kiểm `git diff` thủ công: dòng code **byte-identical**, chỉ comment cuối dòng đổi. | `git diff HEAD~2 -- speech.js` → chỉ khác phần `// …` | **CLOSED (tool quirk)** — đã ghi; không sửa regex (rủi ro cao, lợi ích thấp). |

## Ghi chú về bản chất F-21-03 (bài học)

Comment HTML ở **cấp gốc** của `<template>` **không phải** chú thích vô hại — nó là một
node thật trong cây render. Với Vue 3, `<template>` có ≥2 node cấp gốc = fragment; mọi
assertion kiểu `wrapper.attributes()` / `wrapper.classes()` trả `undefined` vì không có
root element duy nhất. **Quy tắc cho các vòng sau:**
- Comment giải thích template đặt **trước** `<template>` (ngoài block) hoặc trong `<script setup>`.
- Nếu buộc phải comment trong template, đặt **bên trong** element gốc, không ngang cấp với nó.
- `comment_only.py` nay coi comment template là code ⇒ thay đổi cấu trúc này sẽ bị bắt.

## Phase 9 — `/simplify` + review code AI sinh (4 agent song song)

| ID | Mức | Vấn đề | Fix | Bằng chứng |
|---|---|---|---|---|
| **F-21-06** | MED | **Nguồn gốc của lớp lỗi "default vòng audit cũ" (F-17-16 → F-20-02b → F-21-01):** `sweep/v12/api-sweep.js` tự khai báo `arg()` + default riêng thay vì dùng `_config.js` → mỗi vòng phải bump HAI nơi; `assert-harness` check 6/8 chỉ **phát hiện sau**, không ngăn được. | **Fix tận gốc:** `api-sweep.js` nay `require("../harness/_config.js")` lấy `OUT` — chỉ còn MỘT chỗ để bump. | `assert-harness` ALL CLEAN 8/8 sau fix; `node --check` OK; api-sweep vẫn chạy |
| **F-21-07** | MED | **Cùng lớp F-21-03 còn SÓT:** `frontend/src/layouts/AdminLayout.vue:2` có comment HTML ở cấp gốc `<template>` → fragment (chưa vỡ vì không test nào mount nó gọi `.attributes()`). | Dời comment ra **trên** `<template>`. | `awk` xác nhận single-root; MCP: `/admin/dashboard` render đúng sidebar |
| **F-21-08** | LOW | `comment_only.py` còn 3 nhánh quote trùng lặp + 2 danh sách extension song song (`kind_of` vs `TEXT_EXT`) dễ lệch; docstring nói "mask string" (stale sau F-21-04). | Gộp 3 nhánh thành 1; thay bằng bảng `EXT_KIND` duy nhất; sửa docstring; text block giữ nguyên cho nhất quán. | mutation-test vẫn bắt string change; syntax OK |
| **F-21-09** | INFO | `comment_only.py` spawn `git show` mỗi file (69 file ≈ 1.9s). | **SKIP** — công cụ chạy tay, ~2s không đáng tối ưu (ghi nhận `git cat-file --batch` là bản nhanh hơn nếu sau này cần). | — |
| **F-21-10** | INFO | `_db-audit-v21.sql` là bản sao thứ 3 của query FK-index (đã có ở `audit-v16/v17-full/evidence/`) và là bản thu hẹp của orphan-FK generic. | **SKIP** — file là artifact một lần của vòng, đúng chỗ; hoist vào harness chung là việc ngoài scope vòng này. | — |

**Security review (agent độc lập):** 0 CRITICAL/HIGH/MEDIUM. 79 file code **comment-only** (harness + spot-check 4 file bảo mật: MediaProxyController, MediaSigner, CustomUserDetailsService, decor/*.vue). Authz 2 chiều, upload-XSS (`SafeUploadNames` + `contentTypeFor`/`forceDownload`), AI-output sanitize (`v-html` đều qua DOMPurify), secret (0 commit), rate-limit (`:order` khớp route thật) — tất cả CLEAN. Finding duy nhất là **F-21-04** (đã fix).
