# Phase 1–4 — kiểm chứng sau triển khai (2026-09-22)

Mọi số dưới đây **đo trong phiên này**; lệnh ghi kèm để chạy lại được.

## Cổng chung (sau cả 4 phase)

| Cổng | Kết quả | Lệnh |
|---|---|---|
| Backend | **523 / 0 fail / 0 error / 11 skipped, BUILD SUCCESS** | `.\mvnw.cmd -o test` |
| Frontend | **193 passed / 1 skipped (31 file)** | `npx vitest run` |
| Build | `index-2EnNbYih.js` **177.98 kB** (gzip 67.76) | `npx vite build` |
| Parity | `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` | `python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql` |
| `ck_study_policy_singleton` | còn (1); `hb_check_probe` = 0 | `tmp/v13/ck.sql` |
| Clean-clone | **0** import tracked→untracked | script quét 41 import |

## Phase 1 — `api.js` mang `redirect` khi hết hạn giữa phiên

`npm test`: 13/13 pass (`src/services/api.test.js`). **Mutation-check**: vô hiệu helper ⇒ **2 ca đỏ**
(`keeps the current route…`, `carries the destination on a mid-session 401`) → test không phải "xanh giả".

Live (`sweep/v13/f1320-api-redirect-live.js`) **4/4 PASS**:

| Ca | Kết quả |
|---|---|
| token hết hạn khi đang ở `/decks/10006` | → `/login?redirect=/decks/10006` |
| login với `?redirect=%2Fdecks%2F10006` | → về đúng `/decks/10006` |
| `?redirect=%2F%2Fevil.com` | → `/lessons` (không rời site) |

## Phase 2 — xoá 2 cột chết

- Backup TRƯỚC: `C:\Users\ASUS\engflow-backups\engflow_2026-09-22-predrop-deadcolumns.bak`
  (24.874 pages, `RESTORE VERIFYONLY` = *"The backup set on file 1 is valid"*).
- Drop: `dropped users.last_study_date`, `dropped users.current_streak`; chạy lại = **no-op** (idempotent);
  `remaining_legacy_cols=0`.
- Boot lại container: `Started EngflowApplication in 13.984 seconds`; Hibernate **không** thêm lại cột
  (`legacy_cols_after_boot=0`).
- Endpoint thật: `/api/auth/me` 200 · `/api/streak/current` 200 · `/api/streak/snapshot` 200 ·
  `/api/leaderboard` 200 · `/api/dashboard/stats` 200.
- `currentStreak = 2` khớp `study_days` (user 2: 2 ngày, mới nhất `2026-09-22`) — **DTO vẫn được nuôi
  từ nguồn thật**, không phải từ cột đã xoá.
- `recentUsers = 2` khớp `COUNT(DISTINCT user_id) FROM study_days` cửa sổ 7 ngày = **2**.

## Phase 3 — A1 renderer

`npm test`: 13/13 pass (`LessonBlocks.test.js`). **Mutation-check**: bỏ filter section ảo ⇒ **2 ca đỏ**
(`ignores the virtual section…`, `keeps a real section while ignoring a virtual one…`).

Live (`sweep/v13/f1302-a1-blocks-live.js`) **12/12 PASS**:

| Ca | Kết quả |
|---|---|
| 447: section biên soạn hiện | PASS |
| 447: TABLE render (header `Dai tu` — so `textContent`, vì `innerText` bị `text-transform: uppercase`) | PASS |
| 447: TABLE có 3 hàng | PASS |
| 447: **KHÔNG** lộ đáp án QUESTION (`She ___ to school every day.`) | PASS |
| 447: **KHÔNG** lộ SUBMISSION | PASS |
| 447: **đúng 1** section (không trùng nội dung) | PASS |
| 445 (không có block): renderer **vắng** | PASS |
| 446 / 567 / 11301 / 41881 / 91900: mỗi bài đúng 1 section | PASS ×5 |

Ảnh TRƯỚC: `evidence/f13-02-learner-cannot-see-blocks.png` · SAU: `evidence/f13-02-a1-blocks-after.png`
(đã xem: hiện "Tài liệu bổ sung cho bài học này" + bảng chia động từ; không lặp nội dung gốc).

## Phase 4 — ba đồng hồ (chỉ đọc)

`evidence/f13-09-three-clocks.md`. Đo lại **bác bỏ** chi tiết "15 DEFAULT GETDATE()": DB live có
`getdate_defaults_in_live_db = 0` (Flyway disabled → file migration chưa từng được áp). Số writer
thật = **11** `LocalDateTime.now()` (không phải 14). `AGENTS.md:57` đã cập nhật.

## Ghi chú — 2 "FAIL" đã điều tra và loại (không phải bug)

1. **`447: TABLE header 'Đại từ'`** — lần chạy đầu FAIL: probe so `innerText` (`"DAI TU"`) trong khi
   DOM là `"Dai tu"`; `innerText` phản ánh `text-transform: uppercase`. Sửa probe so `textContent`.
2. **`/api/admin/stats` → `currentStreak = None`** — probe đọc key không tồn tại: `AdminStatsDTO`
   **không có** field đó (chỉ `DashboardStatsDTO` mới có). Gọi đúng `/api/dashboard/stats` → `2`.
