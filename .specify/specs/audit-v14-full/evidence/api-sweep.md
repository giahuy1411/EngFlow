# audit-v14-full — Phase 2: API sweep (toàn bộ 148 inventory row)

**Ngày:** 2026-09-25 (+07) · **Target:** backend `http://localhost:8080`
**Inventory:** `evidence/endpoint-inventory.json` — 132 annotation → **148 row / 26 controller**
**Harness:** `sweep/v14/api-sweep.js` (base) + `coverage-sweep.js` (phủ phần còn lại) + `deep-probe.js` + `search-sort.js` + `reconcile-inventory.js`

## Kết quả

| suite | pass | fail | blocked | n/a |
|---|---|---|---|---|
| `api-sweep.js` (toàn inventory) | **143** | **0** | 1 | 3 |
| `deep-probe.js` (6 chức năng + guard + roles) | **58** | **0** | 0 | 0 |
| `coverage-sweep.js` (148 row còn lại) | 51 2xx / 95 4xx / 13 BLOCKED | 0 | 13 | 0 |
| `reconcile-inventory.js` | **148/148 accounted, unaccounted=0, 26/26 controller 100%** | | | |

**Chạy với Ollama BẬT** (ban đầu Ollama tắt → 2 endpoint AI trả 500; xem §Sự cố môi trường).

## T2.2 — Reconciliation (D1/D10) — **ĐÓNG KHOẢNG TRỐNG v13**

| metric | value |
|---|---|
| inventoryRows | 148 |
| matchedRows | **148** |
| **unaccounted** | **0** |
| PROBED | 137 |
| BLOCKED (kèm lý do) | 11 |

v13 ghi "143 pass" nhưng đó là **số assertion**, không phải số endpoint: chỉ **72/148** inventory row
thực sự được gọi. v14 đo lại và **phủ hết 148**, mỗi row có disposition (PROBED/BLOCKED), 26/26
controller 100%. Đây là delta D1/D10.

## T2.4 Role matrix (2 chiều)

deep-probe §roles: **25/25 pass** — mọi path admin × 3 role, assert cả ALLOW và DENY.

## T2.5 Search/Sort (đo, không giả định)

| probe | kết quả |
|---|---|
| vocab search `ab` / `AB` / `Ab` | 200, cùng kết quả → **case-insensitive** |
| vocab search `rise` | 200 len=0 (không khớp substring của từ nào) |
| search 1 ký tự / rỗng | 200 `[]` (theo thiết kế) |
| lesson `q=grammar` | 200 total=466 |
| admin-ex `q=the` | 200 total=16588 |
| **`?sort=notacolumn`** | **400** `"Tham số sắp xếp không hợp lệ"` → **F-13-11 fix còn hiệu lực** |
| VOCAB `sort=word` asc vs desc | **khác nhau** (ascOrdered=true, descOrdered=true) → sort thật sự hoạt động |
| LESSONS/DECKS/ADMIN-EX `?sort=` | asc==desc (bị bỏ qua) → ghi nhận |

## T2.6 Guard F-13-01/12/13 (2 chiều)

| case | kết quả | kết luận |
|---|---|---|
| MC options `["a","b","c","d"]` → 400 | ✅ | F-13-01 giữ |
| MC options thật `["Apple","Banana"]` → 200 | ✅ | |
| MC `["A - Salad",...]` (hàng thật 651717) → 200 | ✅ | **F-13-12 giữ** |
| MC options 1 phần tử / `[]` / null / 2 chữ cái → 400 | ✅ | |
| LISTENING bare-letter → 200 | ✅ | **sửa assertion sai của v13**: guard MC-scoped; F-13-13 fix ở **render path** |
| FILL_BLANK options chữ cái → 200 | ✅ | guard đúng phạm vi |
| PUT MC hợp lệ → chữ cái 400; → options thật 200 | ✅ | update path giữ |

## T2.7 AI

| probe | kết quả |
|---|---|
| `POST /api/ai/generate-vocab` (student) | **200** — sinh `sandwich`, `cuisine` (Ollama `qwen2.5:1.5b`) |
| `POST /api/ai/enrich-word` | 200 (sau khi Ollama bật) |
| `POST /api/ai/save-vocab` | 200 + header `X-AI-Linked-To-Deck` (F145) |
| `GET /api/admin/exercises/ai/status` | 200 (processed 1470/1470) |
| `POST /api/admin/exercises/ai/validate` | 200 — `["a","b","c","d"]` **valid=false**, options thật **valid=true** (đóng lỗ bypass AI) |
| `generate-async` / `generate` / `generate-all` | **BLOCKED** — ghi exercise thật qua Ollama; dùng đường `validate` thay thế |

## T2.8 Payment (biên giới)

| probe | kết quả |
|---|---|
| create-order | 200 + orderCode `ENG...`, row dọn trong run |
| webhook **sai chữ ký** | 200 body `{"success":false,...}` (SePay ACK 200, outcome ở body) |
| webhook **stale timestamp** | từ chối |
| webhook **hợp lệ** | **BLOCKED by design** — mutate `payment_transactions` thật |

## Sự cố môi trường (không phải lỗi sản phẩm)

**Ollama không chạy** khi bắt đầu Phase 2 → `POST /api/ai/generate-vocab` và `/enrich-word` trả **500**
(`WebClientRequestException: Connection refused host.docker.internal:11434`). Đã **khởi động Ollama**
(`ollama serve`, models `qwen2.5:1.5b` + `3b` có sẵn) → cả hai trả **200**. Đây là **service outage**, theo
constitution phải là **BLOCKED**, không phải FAIL. Ghi lại kèm root cause.

**Ghi nhận chất lượng (F-14-02, LOW):** khi upstream AI chết, app trả **500** generic thay vì **503**.
Đây là hành vi thật, có thể sửa — ghi vào `findings.md`.

## Sự cố tự gây — F-14-01 (HIGH, đã fix)

`coverage-sweep.js` (script mới) fire **DELETE** vào ID thật → **xoá lesson 445 + 6 exercises + section 5
+ 5 blocks + 2 lesson_submissions**; đồng thời 2 POST tạo `lesson_snapshot` + `study_days`. Phát hiện bằng
re-assert parity; **khôi phục từ backup** (targeted restore); **fix harness** (destructive → ghost id;
mutating POST → BLOCKED; integrity check cuối script). Chi tiết: `incident-f14-01-data-loss.md`.

## Cleanup (in-run)

- `api-sweep.js`: `AUDIT_DECKS=0 AUDIT_VOCAB=0 AUDIT_LESSONS=0 AUDIT_PAY=0` (0 rác)
- `deep-probe.js`: `DEEP_LESSONS=0 DEEP_DECKS=0 DEEP_EX=0` (0 rác)
- `coverage-sweep.js`: `parityOk=true`, `study_days=4`
- Parity cuối Phase 2: `1470|43735|72|118|29|15|4|126|10|5` — **khớp baseline**
