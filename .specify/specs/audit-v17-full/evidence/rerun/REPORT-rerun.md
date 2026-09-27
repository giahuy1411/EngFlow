# audit-v17-full — RERUN: báo cáo (đóng các mục "Chưa làm")

**Ngày:** 2026-09-26 (+07) · **Nhánh:** `audit-v15-full` · **Tiền đề:** commit `c1504d9` (v17 vòng 1)
**Yêu cầu:** *"thực hiện lại toàn bộ các thao tác kế hoạch triển khai trước đó của v17 nhưng cần review chéo lại
thêm một lần nữa đi kèm với bản mở rộng thực hiện các việc chưa làm. đi sâu vào logic kiểm thử toàn bộ chức năng
thông qua ui/ux bằng mcp chrome devtool, playwright mcp"*

---

## 1. Tóm tắt

Chạy lại **toàn bộ** chuỗi v17 trên build hiện tại + **đóng cả 6 mục "Chưa làm"** + **thêm 1 vòng review chéo** +
**đi sâu UI/UX mọi chức năng**. Kết quả: **2 mục tưởng BLOCKED hoá ra test được** (SePay webhook ký thật; speaking
với audio TTS thật), **Playwright MCP unblock** (chứng minh), **4 finding harness FIXED**, **4 finding mới**.
Baseline: backend **520/0/0/11**, frontend **191/1 (32 file)**, API **143/0**, deep-probe **58/0**, design 0 drift,
CLS ≤ 0.00095, parity `1470|43738|5|118|29|4|3|12|10` — **0 regression**.

---

## 2. Đã làm (theo gap)

| Gap | v17 | Rerun | Bằng chứng |
|---|---|---|---|
| **G1 Playwright MCP** | BLOCKED | **UNBLOCKED (chứng minh)** | `--executable-path` → Brave; `browser_navigate` → `/lessons` OK → `g1-playwright-mcp.md` |
| **G2 F-17-06** | OPEN (kết luận SAI) | **CLOSED (probe SAI)** + chống tái diễn | text thật `ERR_UNSAFE_REDIRECT`; 4× tái hiện → 0; `isThirdPartyConsoleNoise` dùng chung → `g2-g3-g4-harness.md` |
| **G3 F-17-07** | OPEN | **FIXED (đã chứng minh)** | `cleanupExerciseAttempts` + marker + `assertClean`; guard tự bắt residue thật; test sống: insert→37→dọn→36; học viên thật không đụng |
| **G4 F-17-13** | OPEN | **FIXED** | tên ảnh từ `VER` → `v17-*.png` |
| **G5 F-17-05** | OPEN (đặc tính) | **FIXED phần local** | fast path **11–54 ms** thay ~20 000 ms; bỏ gọi proxy 2 lần; 3 probe (API + UI thật + fall-through); mutation-test → `g5-f17-05.md` |
| **G6 SePay** | BLOCKED | **VERIFIED** | valid sig → `{"success":true}` + `SUCCESS`; bad sig → `Invalid signature`; tự dọn → `g6-sepay.md` |
| **G7 Speaking** | BLOCKED | **VERIFIED** | TTS WAV thật → assess → `COMPLETED`, transcript 181 ký tự, **score 9.7**; tự dọn row+MinIO+study_days → `g7-speaking.md` |
| **G8 Perf** | "không win" | **giữ nguyên** | median 19.8 ms — tái xác nhận (P5) |

## 3. Đi sâu UI/UX mọi chức năng (Phase U')

`mcp-walkthrough-full.md`: admin CRUD (create→search→delete 204), deck CRUD (200/200/200), SRS (due=10, review 200),
5 chế độ game (200 + session), AI vocab (3 từ trong 6.2 s), tra từ fast path, speaking list+record, mid-session 401
(`/login?redirect=/`), **8/8 admin endpoint 200**, guard anon (401) / public (200) / 404, forgot-password 200.
**Residue dọn sạch** (study_days, exercise_attempts, payment, speaking row, MinIO object, lesson, deck).

## 4. Finding MỚI

| ID | Mức | Vấn đề | Trạng thái |
|---|---|---|---|
| F-17-14 | LOW (harness) | `assess()` ghi `study_days` mà probe không dọn | `FIXED` |
| F-17-15 | LOW (harness) | Probe G7 bỏ lại object MinIO (đọc sai field) | `FIXED` |
| F-17-16 | LOW (harness) | `_config.js` mặc định còn `audit-v15-full` | `OPEN` (ghi nhận) |
| F-17-17 | LOW (harness) | `focused-probe.js:188` hardcode `audit-v15-full` | `OPEN` (ghi nhận) |

## 5. Review chéo (vòng 2) — R3

Xem `review-v17-round2.md`. Reviewer bắt **7 defect thật trong chính fix của rerun** (đã đối chiếu + sửa hết):
**F-17-18** regex noise nuốt lỗi thật (MEDIUM) · **F-17-19** offline báo TIMEOUT sai · **F-17-20** local-first
làm mất audio/nghĩa 118 từ (MEDIUM, **đảo thiết kế**) · **F-17-21** baseline global → DIRTY giả · **F-17-22** G6
không hoàn nguyên premium · **F-17-23** negative control không assert · **F-17-24** G7 dùng  UTC sai giờ ·
**F-17-25** probe thiếu try/finally · **F-17-26** cosmetic. Thêm **8 test** mới + **mutation-test** chứng minh.

## 6. Còn lại (nói thật) — **ĐÃ ĐÓNG ở CLOSING ROUND 2026-09-27**

> Xem `CLOSING-ROUND.md`. Tất cả các mục dưới đây đã được đóng nốt; giữ lại đây để đối chiếu lịch sử.

- ~~**Playwright MCP**: cần restart session~~ → **ĐÓNG:** cấu hình env `PLAYWRIGHT_MCP_EXECUTABLE_PATH`
  (+ `--executable-path` trong plugin `.mcp.json`) đã **chứng minh** chạy (`C1 ENV ROUTE PASS: true`).
- ~~**F-17-05 phần "từ không có local"**: vẫn ~20 s~~ → **ĐÓNG:** tra từ nay **chỉ** dùng từ điển (bỏ local
  khỏi đường tra) + **trần chờ 6 s** (cold 19.99 s → warm 0.032 s; UI không còn treo).
- ~~**F-17-16/17**: 2 drift harness~~ → **FIXED** + thêm 2 static guard (`assert-harness` check 6/7).
- ~~**Speaking test dùng audio TTS**~~ → **ĐÓNG:** `g8-speaking-human-audio.py` chấm **giọng người thật**
  (recall 0.97, tự dọn, object gốc nguyên vẹn).
- ~~**SePay/speaking là test LOCAL**~~ → vẫn đúng và **được nói rõ**; thêm **assert replay window** + gỡ
  `blocked` lỗi thời ở `api-sweep` (145/0/0).

**Giới hạn vẫn còn (không giấu):** tra từ vẫn phụ thuộc upstream (trần chờ chỉ ngăn *treo*); file giọng người
**có sẵn local**, không phải phiên production; SePay dùng **secret local**, không phải tiền thật; tool Playwright
MCP **trong session này** cần restart để nạp lại cấu hình.
