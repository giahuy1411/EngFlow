# audit-v21-full — Findings

> Mọi finding kèm bằng chứng đo được. Không suy đoán.

| ID | Mức | Mô tả | Bằng chứng | Trạng thái |
|---|---|---|---|---|
| **F-21-03** | **HIGH** | **Comment AI sinh làm VỠ component Vue.** Agent thêm comment HTML ngay sau `<template>` ở 3 file → template có **2 root node** (comment + element) ⇒ Vue coi là **fragment** ⇒ `wrapper.attributes()` trả `undefined` ⇒ `decor-props.test.js` **7 test FAIL**. Đây là hậu quả nghiêm trọng thật mà quy trình "test sau mỗi lô" bắt được. | `npx vitest run` → **7 failed**; A/B: giữ comment = 7 fail, bỏ comment = 3 fail (3 còn lại là DecoConfetti) | **FIXED** — bỏ comment ở cấp gốc template (chuyển nội dung vào JSDoc `<script setup>`); 194 passed. Đồng thời **gia cố `comment_only.py`**: với `.vue`, KHÔNG strip comment HTML (comment template là cấu trúc, không phải chú thích) ⇒ biến thể lỗi này bị bắt là code-changed. |
| **F-21-01** | MED | Default audit round còn `audit-v20-full` ở 2 nơi → probe ghi evidence sai thư mục (lớp F-20-02b). | `assert-harness.js` check 6+8 FAIL trước → PASS sau | **FIXED** |
| **F-21-02** | LOW | `harness-restore.md` thiếu trong `audit-v21-full/evidence/` → check 5 FAIL. | check 5 FAIL → PASS | **FIXED** |

## Ghi chú về bản chất F-21-03 (bài học)

Comment HTML ở **cấp gốc** của `<template>` **không phải** chú thích vô hại — nó là một
node thật trong cây render. Với Vue 3, `<template>` có ≥2 node cấp gốc = fragment; mọi
assertion kiểu `wrapper.attributes()` / `wrapper.classes()` trả `undefined` vì không có
root element duy nhất. **Quy tắc cho các vòng sau:**
- Comment giải thích template đặt **trước** `<template>` (ngoài block) hoặc trong `<script setup>`.
- Nếu buộc phải comment trong template, đặt **bên trong** element gốc, không ngang cấp với nó.
- `comment_only.py` nay coi comment template là code ⇒ thay đổi cấu trúc này sẽ bị bắt.
