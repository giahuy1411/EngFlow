# Phase 1–4 — kiểm chứng sau triển khai (2026-09-22)

Mọi số dưới đây **đo trong phiên này**; lệnh ghi kèm để chạy lại được.

## Cổng chung (sau cả 4 phase)

| Cổng | Kết quả | Lệnh |
|---|---|---|
| Backend | **523 / 0 fail / 0 error / 11 skipped, BUILD SUCCESS** | `.\mvnw.cmd -o test` |
| Frontend | **193 passed / 1 skipped (31 file)** | `npx vitest run` |
| Build | `index-*.js` **177.98 kB** (gzip 67.74–67.76; hash đổi mỗi build) | `npx vite build` |
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

## Tự bắt được 1 defect trong code của chính tôi (Phase 3)

**`.no-print` là no-op.** Lần đầu tôi đặt `class="... no-print"` lên section biên soạn, nhưng
`no-print` **chỉ** được định nghĩa trong `<style scoped>` của `LessonContent.vue` → scoped style
không áp ra ngoài component đó. Hệ quả: bấm "In tài liệu" thì section biên soạn **vẫn in**.

Sửa: `@media print` + selector riêng trong scope của `LessonBlocks.vue`.
Verify live: `emulateMedia({media:'print'})` → `display = "none"` (đã đo).
Kiểm luôn: không còn chỗ nào khác dùng `no-print` ngoài `LessonContent.vue`.

## Phase 2 — mọi endpoint tiêu thụ streak (kiểm live sau khi xoá cột)

Xoá cột chỉ an toàn nếu **mọi** DTO có field `currentStreak` vẫn được nuôi từ nguồn thật.
Đã gọi thật cả 5 đường (2026-09-22):

| Endpoint | Kết quả | Nguồn |
|---|---|---|
| `GET /api/auth/me` | `currentStreak = 2` | `UserService:322` → `streakService.getCurrentStreak` |
| `GET /api/dashboard/stats` | `currentStreak = 2` | `DashboardService:35` |
| `GET /api/leaderboard` | `student=2, administrator=1, auditadmin=0` | `LeaderboardService:48` (batch) |
| `GET /api/admin/users` | 3 row, `currentStreak = 0/0/0` | `AdminService:77` (batch `currentStreaks`) |
| `GET /api/streak/current` + `/snapshot` | 200 | `StudyActivityService` |

Kết luận: **0 endpoint hỏng** do xoá cột — tất cả đọc từ `study_days` như thiết kế.

## Phase 3 — a11y của section mới (constitution P7)

`sweep/v13/f1302-a1-a11y.js` → **8/8 PASS** trên lesson 447: 1 `h1`; thứ tự heading
`H2 → H3` (không nhảy cấp); `aria-labelledby` resolve; 0 `img` thiếu `alt`; TABLE có
`<thead>`+`<th>`; id không trùng; **contrast AA cho cả 31 text node, min ratio 6.26**.
Dùng lại `COMPOSITE_FN` của repo (composite alpha, bottom-up), không viết bản thứ hai.

## Phase 3 — bằng chứng TRỰC TIẾP cho bẫy trùng nội dung (đo qua API thật)

Gọi `GET /api/lessons/445/structure` (bài **không** có section nào được materialize):

```
sections: 1
  section id=None  <-- VIRTUAL (id null)  title='Nội dung bài học'  blocks=1
     block id=None  type=TEXT  dataLen=6764   <-- chính là lesson.content
```

Còn `GET /api/lessons/447/structure` (bài CÓ section thật):

```
section id=5 / 6 / 7  (id thật, khác null)   blocks id=1..9
```

⇒ Nếu render **mọi thứ** endpoint trả về, 445 (và ~1.462 bài tương tự) sẽ hiện nội dung
bài **HAI LẦN**. Guard `s.id != null` trong `LessonBlocks.vue` chặn đúng ca này — và live
probe đã xác nhận 445 render **0** section.

## Tự bắt được defect #2 — một test FALSE NEGATIVE (Phase 3)

Khi đối chiếu sanitize config của tôi với `LessonContent.vue`, thấy nó có `FORBID_CONTENTS`
còn bản của tôi thiếu. Tôi thêm option đó **và** viết test khẳng định nó — rồi **mutation-check
lại**: gỡ `FORBID_CONTENTS` ra thì test **vẫn pass** ⇒ test là **false negative** (nó xanh vì lý do
khác, không phải vì option tôi vừa thêm).

Đo trực tiếp DOMPurify với payload `<style>body{color:red}</style>`:

```
WITHOUT FORBID_CONTENTS: "<p>visible</p>"
WITH    FORBID_CONTENTS: "<p>visible</p>"   <-- y hệt
```

⇒ Với input này, DOMPurify bỏ **cả** element lẫn text dù chỉ có `FORBID_TAGS`. Vậy `FORBID_CONTENTS`
là **defence-in-depth / đồng bộ với component anh em**, không phải thứ làm test xanh.

Xử lý: giữ option (vô hại, khớp sibling) nhưng viết lại test để khẳng định **kết quả quan sát được**
(`<style>` và text của nó không còn trong output), và **mutation-check lại**: làm rỗng `FORBID_TAGS`
⇒ test **đỏ** (13 pass / 1 fail) ⇒ test có giá trị thật.

**Bài học ghi lại:** một test mới chỉ đáng tin sau khi chứng minh nó **đỏ khi bỏ fix**. Cả 3 fix
lớn trong phiên này (Phase 1, Phase 2, Phase 3) đều đã qua bước đó.
