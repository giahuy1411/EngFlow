# audit-v13-full — checklist (unit test cho requirements)

Checklist kiểm **chất lượng của yêu cầu**, không phải kiểm implementation.

## R1 — Mọi con số là đo của phiên này

- [x] Baseline backend/frontend/build đo lại, ghi log riêng (`baseline-backend.log`, `baseline-frontend.log`, `baseline-build.log`)
- [x] Parity đo lại (`baseline.md`) — có ghi rõ chênh so v12 và **lý do** (exercises +1, speaking +1 do user test)
- [x] Endpoint inventory tái dựng từ source, ghi số thật (132/148/146/26)
- [x] Mọi phát hiện mới (F-13-01..06) trỏ tới artifact đo trong phiên
- [x] Mọi số trong `REPORT.md` có file evidence tương ứng (64 file evidence)

## R2 — API surface được CHẠY, không chỉ liệt kê

- [x] Mọi entry inventory có status PASS/FAIL/BLOCKED/N-A (143 pass / 0 fail / 1 blocked / 3 N/A)
- [x] Kiểm hợp đồng (tên field + kiểu) cho 6 chức năng chính
- [x] Role kiểm **cả hai chiều** (25/25 pass)
- [x] Guard mới của F-13-01 kiểm cả hai chiều: payload lỗi → 400, payload hợp lệ → 200

## R3 — UI được ĐỐI CHIẾU với API, không test rời

- [x] T4.2b: UI thật → bắt network `GET /api/lessons/91920/exercises => 200` → grade/submit 200 → DB row đổi
- [x] Các flow T4.1, T4.3–T4.7 có 3 tầng bằng chứng; T4.8 ghi rõ biên giới tiền thật
- [x] Không có ca "UI lỗi trong khi API khỏe" ngoài artifact do chính tôi gây ra (đã ghi)

## R4 — DB trong Docker audit như dữ liệu

- [x] Constraint kiểm **hiệu lực** — 2 lần độc lập (scratch DB + rollback txn thật), Msg 547/2627/2601/515
- [x] Orphan scan đếm NULL FK riêng (0 orphan / 25 FK)
- [x] Quét `Msg \d+` mọi batch
- [x] Parity re-assert sau mỗi lần ghi probe (đã làm 3 lần: 43736→43735, E2E cleanup)

## R5 — Design system verify + tìm lỗ hổng của chính prompt

- [x] Font: grep Outfit/Plus Jakarta = 0; `--geo-font` = Be Vietnam Pro; `document.fonts.check` (trong UI sweep)
- [x] Contrast đo live DOM, **composite alpha bottom-up**, trên **đúng nền từng node**
- [x] Prompt hole H1 (AAA sai) ghi lại kèm số đo + quy tắc `*-ink`/`*-strong` (`prompt-fixes.md`)
- [x] **Không** "sửa" chỗ đang đúng: sidebar amber 8.76:1 đo được → giữ nguyên

## R6 — Vòng 2 rộng hơn, lặp đến khi cạn

- [x] Round 2 chạy trên build cuối (`round-2.md`)
- [x] Case biên đối kháng — 12 case, tất cả đúng hành vi
- [x] Dừng sau 2 vòng liên tiếp 0 lỗi mới

## R7 — Kỷ luật bằng chứng, không cap im lặng

- [x] Mỗi finding F-13-01/02/03/04/05/06 có artifact riêng
- [x] Mọi giới hạn ghi rõ trong `REPORT.md` §3
- [x] Lỗi của probe ghi lại, không tính là finding (ví dụ: `exercise_attempts` dùng `attempt_id` không phải `exercise_id` — lỗi của probe đầu tiên của tôi)

## R8 — Chỉ dùng dữ liệu THẬT

- [x] F-13-01 repro bằng hàng `exercise_id=777434` thật, API thật, UI thật
- [x] E2E tạo bài thật rồi chấm thật, đọc hàng DB thật
- [x] Contrast đo computed style trên DOM thật
- [x] Test unit (vitest/JUnit) dùng mock — **được ghi rõ** là unit test, không dùng để tuyên bố hành vi E2E

## R9 — Review chéo mọi fix do AI sinh

- [x] Adversarial review F-13-01 đã dispatch (2 skeptic + agent chuyên trách)
- [x] Review chéo bắt 6 điểm F-13-01 (đã sửa cả 6) + workflow bắt F-13-12/13/20

## Bẫy thao tác (AGENTS.md)

- [x] Flush `rate_limit:*` trước mỗi batch HTTP
- [x] `SET QUOTED_IDENTIFIER ON` cho mọi batch DELETE
- [x] Seed cả token + user, assert `page.url()` (117 cell, 0 guardFail)
- [x] `alt=""` hợp lệ (`!el.hasAttribute('alt')`) — missingAlt 0
- [x] Tap-target AA = 24px (có áp ngoại lệ 2.5.8)
- [x] Overflow = `scrollWidth - clientWidth` (0 overflow)
- [x] `/premium/checkout` dọn row + re-assert parity (nhiều lần)
- [x] Xoá theo **ID liệt kê**, không `LIKE 'zz%'`
- [x] Backup trước DML hàng loạt (v13 chưa cần — chỉ xoá vài hàng probe đã liệt kê ID)
- [x] Timezone naive-VN, không dùng `SYSDATETIME()` làm mốc
