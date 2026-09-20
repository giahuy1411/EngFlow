# Phase 2 — API sweep trên container thật

**Ngày:** 2026-09-21 (+07) · **Target:** `http://localhost:8080` (container đang chạy)
**Probe:** `sweep/v12/api-sweep.js` → `evidence/api-sweep.json` · **Kết quả: 133 pass / 0 fail / 1 blocked / 3 N/A**

---

## Phương pháp, và hai cạm bẫy được thiết kế vòng qua

**Bucket rate-limit.** Bucket global 100/phút/IP. Harness **flush `rate_limit:*` trước mỗi batch** — không thì
nó tự tạo 429 giả (finding F109). Flush dùng `execFileSync` với argv, **không** `execSync` chuỗi nội suy, vì Redis
key là dữ liệu, không được chạm shell (security-guidance bắt ở v10).

**Chạy SERIAL, không fan-out.** Đo trực tiếp: chỉ có 2 bucket dùng được (`rate_limit:172.18.0.1:global` host và
`...:172.18.0.7:global` Vite proxy) vì `TRUSTED_PROXY_ENABLED` chưa set nên `X-Forwarded-For` không shard được.
Fan-out 6 luồng = tái hiện F109 có chủ ý.

**Kiểm HỢP ĐỒNG, không chỉ 2xx.** Hàm `contract()` khẳng định tên field **và** kiểu JS. Một 200 thiếu field sẽ FAIL
kèm danh sách key thực có.

**Phủ 12 controller v11 bỏ sót:** Game(6), Flashcard(2), Srs(3), Leaderboard(1), LessonSubmission(3),
LessonSnapshot(3), Progress(1), SpeakingPrompt(9), AdminExerciseSeed(1), LessonStructure(11),
AdminAnswerBackfill(2), Dashboard(1), MediaProxy(1).

---

## Kết quả theo vùng

| Vùng | Pass | Nội dung |
|---|---|---|
| **auth** | 13 | me(student/admin/anon), sai mk 401, mk ngắn 400, email sai 400, JWT rác 401, register trùng 400/409, forgot 200, reset 400, change-password 400/401, avatar 400 |
| **lessons** | 18 | list(contract, không lộ LOB), detail, exercises, `includeAnswers=false` strip đáp án, **anon `includeAnswers=true` KHÔNG lộ đáp án**, admin thấy đáp án, structure, content, grade anon 401, attempts auth/anon, **guard draft F88/F89/F115/F126 tái chứng minh**, draft admin 200 |
| **streak** | 5 | `/snapshot` đủ **7 field** + type, `/current`, `/history`, anon 401 |
| **search** | 8 | vocab search permitAll, 1 ký tự → 200+`[]` (đúng thiết kế), dictionary, anon `/api/vocabulary` 401, auth 200, admin exercise `q=`, anon 401/403, **`?sort=` đo lại** |
| **crud** | 17 | deck create/read/update/private-hidden/delete/404; lesson admin CRUD; anon 401/403; student 403; **C1 auth-shape 3 assert** |
| **game** | 13 | 5 mode (quiz/memory/typing/listening/mixed) đều 200 + `sessionId`; submit blank 400; submit session thật 200; anon 401 |
| **flashcard** | 3 | status 200, review 200, anon 401 |
| **srs** | 6 | due 200, stats 200, review q=3 200, q=9 400, thiếu vocabId 400, anon 401 |
| **misc** | 6 | leaderboard anon 200 (contract), progress auth 200 / anon 401, dashboard auth 200 / anon 401 |
| **submission** | 6 | lesson-submissions draft guard, my/lesson/skill 200, anon 401; snapshots admin 200 / student 403 / anon 401 |
| **speaking** | 9 | prompts public 200 (+q), video-prompts alias 200, admin prompts 200 / student 403 / anon 401, submissions auth 200, admin submissions 200 / student 403 |
| **video** | 7 | video-lessons anon 200, detail 200, my attempts auth 200, admin attempts 200 / student 403, admin lessons 200 / anon 401 |
| **admin** | 10 | **ROLE matrix 7 endpoint × 3 role, assert CẢ HAI CHIỀU** (401/403/200); ai/status 200 + anon 401/403; backfill status 200 |
| **ai** | 5 | generate-vocab anon 401/403, auth 200/429, enrich-word, save-vocab 200 + **header F145 `X-AI-Linked-To-Deck`** |
| **payment** | 7 | status 200, create-order 200 + `orderCode` ENG…, **webhook chữ ký sai → `success:false`**, stale timestamp → refused |

## 4 lỗi probe tự phát hiện (ghi lại — lỗi probe cũng là lỗi)

Mỗi cái bị **probe thứ hai độc lập** giết trước khi thành finding:

| Probe đầu báo | Probe thứ hai tìm ra | Verdict |
|---|---|---|
| "login sai mk trả 400, không phải 401" | Mk `"nope"` chỉ **4 ký tự** → vi phạm `@Size(min=6)` → **400 Validation Failed**. Dùng `"wrong-password"` → **401** đúng | **probe bug** |
| "`POST /api/flashcards/review` trả 404" | `vocab_id` **10006 không tồn tại** (MIN = 10017) → service ném `ResourceNotFoundException` → 404 đúng. Dùng 10017 → **200** | **probe bug** |
| "`POST /api/srs/review` trả 400" | `SrsController` đọc key **`vocabId`** (Map), không phải `vocabularyId`. Dùng `vocabId` → **200** | **probe bug** |
| "webhook chữ ký sai trả 200 = không chặn" | SePay ACK bằng **HTTP 200** và mang kết quả trong **body**: `{"success":false,"error":"Invalid signature"}`. Assert phải đọc body | **probe bug** |

Cả bốn sửa xong → **0 fail**. Đây đúng lớp lỗi v11 gặp 10 lần (P1–P9).

## Dọn dẹp & parity

Sweep có ghi (deck/lesson CRUD, vocabulary, payment order). Dọn **trong cùng run**, theo **ID/tiêu chí liệt kê**,
có `SET QUOTED_IDENTIFIER ON` và quét `Msg \d+`:

```
AUDIT_PAY_CANDIDATES=1
AUDIT_DECKS=0 AUDIT_VOCAB=0 AUDIT_LESSONS=0 AUDIT_PAY=0
cleanup produced 0 SQL errors (Msg scan)   PASS
cleanup left 0 residue                     PASS
```

**Parity sau sweep: `1471|43735|72|127|28|15|4|126|14|5`** — khớp trước sweep.

> **Một rò rỉ thật đã bắt và dọn:** lần chạy **đầu tiên** (trước khi tôi viết khối cleanup) tạo row `id=80261`
> `ENGCC5128F31E45` PENDING, đẩy payments 126 → 127. Đã xoá theo **id liệt kê** (`id=80261 AND status='PENDING'
> AND transaction_id IS NULL`), parity về lại 126. Cleanup trong harness sau đó được **siết** để assert
> `remaining === 0` theo *cửa sổ ngày của chính nó* thay vì so baseline cứng — đúng bài học F130 của v11.

## Biên giới được ghi rõ (không cap im lặng)

| Endpoint | Trạng thái | Lý do |
|---|---|---|
| `POST /api/webhook/sepay` chữ ký **hợp lệ** | **BLOCKED** | Mutate row payment thật = giao dịch thật. v11 đã chứng minh phía nhận (10/10) rồi revert |
| `POST /api/auth/avatar/upload` | **N/A** | Cần media thật (Cloudinary) — đã có bằng chứng AGENTS.md/F145 |
| `POST /api/admin/lessons/{id}/snapshots` + `/restore` | **N/A** | Ghi snapshot / khôi phục nội dung thật — không chạy để tránh mutate content |
| `POST /api/v1/speaking-submissions/upload` + `/assess` | **N/A** | Cần object MinIO thật — v11 có `speaking-record-test.js` (11/11) |

## `?sort=` — đo lại, xác nhận là product gap

```
?sort=title,asc   -> [41881, 81895, 91900, 91919, 91920]
?sort=title,desc  -> [41881, 81895, 91900, 91919, 91920]
```

**Thứ tự y hệt** ⇒ `?sort=` **không có tác dụng** — **xác nhận** v11. Phân loại **khoảng trống sản phẩm, không phải
lỗi**: UI không có nút sắp xếp nào, nên không người dùng nào bị lừa.

---

## Verdict

**Phase 2: PASS, và đây là phần việc lõi mới của v12.** Mọi vùng người dùng nêu — Auth, Lessons/Exercises, Streak,
Search/Sort, CRUD, AI — đều được chạy thật; **12 controller mà v11 chưa từng chạm nay đều có probe** với đúng role
và kiểm hợp đồng; role matrix assert **cả hai chiều**; 4 guard draft cũ (F88/F89/F115/F126) được **tái chứng minh
sống**, không giả định. Bốn "lỗi" đầu tiên đều bị **falsify là lỗi probe**. Một rò rỉ payment do lần chạy đầu đã
được bắt, dọn theo id liệt kê, và parity khôi phục chính xác.
