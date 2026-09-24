# audit-v14-full — checklist (unit test cho requirements)

Checklist kiểm **chất lượng của yêu cầu**, không phải implementation. Trạng thái cập nhật khi phase xong.

## R1 — Mọi con số là đo của phiên này

- [ ] Baseline backend/frontend/build đo lại, log riêng (`baseline-backend.log`, `baseline-frontend.log`, `baseline-build.log`)
- [ ] Parity đo lại (`parity-before.txt`) — chênh so v13 phải ghi rõ **lý do**
- [ ] Endpoint inventory tái dựng từ source, ghi số thật
- [ ] Mọi số trong `REPORT.md` có file evidence tương ứng

## R2 — API surface được CHẠY + reconcile 148/148

- [ ] Mọi entry inventory có status PASS/FAIL/BLOCKED/N-A
- [ ] `endpoint-reconciliation.md` cho `unaccounted=0` (exit != 0 nếu khác)
- [ ] 26 controller đều 100% coverage
- [ ] Kiểm hợp đồng (tên field + kiểu) cho 6 chức năng chính
- [ ] Role kiểm **cả hai chiều**

## R3 — UI được ĐỐI CHIẾU với API, không test rời

- [ ] Mỗi chức năng: UI thật → network call → API contract → hàng DB đổi (assert **đẳng thức**)
- [ ] Không có ca "UI lỗi trong khi API khỏe" ngoài artifact tự gây (ghi rõ)

## R4 — DB trong Docker audit như dữ liệu

- [ ] Constraint kiểm **hiệu lực** — INSERT vi phạm bị chặn (scratch DB), rồi DROP
- [ ] Orphan scan đếm NULL FK **riêng dòng**
- [ ] Quét `Msg \d+` mọi batch
- [ ] Parity re-assert sau mỗi lần ghi probe

## R5 — Design system verify + tìm lỗ hổng prompt

- [ ] Font: grep Outfit/Plus Jakarta = 0; `--geo-font` = Be Vietnam Pro; `document.fonts.check` = true
- [ ] Contrast đo live DOM, **composite alpha bottom-up**
- [ ] Prompt hole ghi kèm số đo + quy tắc `*-ink`/`*-strong`
- [ ] **Không** "sửa" chỗ đang đúng

## R6 — Vòng 2 rộng hơn, lặp đến khi cạn

- [ ] Round 2 chạy trên build cuối
- [ ] Case biên đối kháng (L1–L8 + toàn bộ case biên)
- [ ] Dừng sau 2 vòng liên tiếp 0 lỗi mới

## R7 — Kỷ luật bằng chứng, không cap im lặng

- [ ] Mỗi finding có artifact riêng
- [ ] Mọi giới hạn ghi rõ trong `REPORT.md`
- [ ] Lỗi của probe ghi lại, không tính là finding

## R8 — Chỉ dữ liệu THẬT

- [ ] Repro bug dùng hàng DB thật, API thật, UI thật
- [ ] E2E tạo/chấm thật, đọc hàng DB thật
- [ ] Contrast đo computed style trên DOM thật
- [ ] Unit test dùng mock — **ghi rõ** là unit test, không dùng để tuyên bố hành vi E2E

## R9 — Review chéo mọi fix AI sinh

- [ ] Adversarial review mọi fix (≥2 skeptic cho finding HIGH)
- [ ] Review chéo mọi liên kết UI↔API↔DB mới

## R10 — Mỗi finding có probe thứ 2 độc lập

- [ ] `adversarial-verify.json`: 100% finding có ≥2 probe
- [ ] Probe bug bị falsify trước khi đóng finding

## R11 — Dọn rác + liệt kê

- [ ] `cleanup-manifest.md` liệt kê KEEP / DELETE / UNTOUCHED
- [ ] `git status --short` sạch sau dọn
- [ ] Parity = baseline sau dọn
- [ ] REPORT liệt kê verbatim file đã xoá

## R12 — Hiệu năng đo trên stack tĩnh

- [ ] `perf-before.json.stackStable=true`; container `StartedAt` before==after
- [ ] Không tối ưu mò; từ chối tối ưu kèm số (P5)

## Bẫy thao tác (AGENTS.md)

- [ ] Flush `rate_limit:*` trước mỗi batch HTTP
- [ ] `SET QUOTED_IDENTIFIER ON` cho mọi batch DELETE
- [ ] Seed cả token + user, assert `page.url()`
- [ ] `alt=""` hợp lệ (`!el.hasAttribute('alt')`)
- [ ] Tap-target AA = 24px (triage đầy đủ)
- [ ] Overflow = `scrollWidth - clientWidth` (trừ scrollbar)
- [ ] `/premium/checkout` dọn row + re-assert parity
- [ ] Xoá theo **ID liệt kê**, không `LIKE 'zz%'`
- [ ] Backup trước DML hàng loạt
- [ ] Timezone naive-VN, không dùng `SYSDATETIME()` làm mốc
