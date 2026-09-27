# audit-v18-full — Phase 1: API sweep

**Ngày:** 2026-09-27 · **Nguồn:** `sweep/v12/api-sweep.js --audit audit-v18-full`

## Kết quả

```
=== SUMMARY ===
pass=145 fail=0 blocked=0 n_a=2
  auth         pass=13 fail=0
  lessons      pass=18 fail=0
  streak       pass=5  fail=0
  search       pass=8  fail=0
  crud         pass=23 fail=0
  game         pass=13 fail=0
  flashcard    pass=7  fail=0
  srs          pass=6  fail=0
  misc         pass=6  fail=0
  submission   pass=6  fail=0
  speaking     pass=9  fail=0
  video        pass=7  fail=0
  admin        pass=10 fail=0
  ai           pass=5  fail=0
  payment      pass=9  fail=0
```

**145 pass / 0 fail / 0 blocked / 2 n/a** — khớp v17 (145/0/0). `deep-probe` **58/0/0**,
`search-sort` pass hết.

## Reconciliation với inventory

`endpoint-inventory.json`: `annotationCount=121 · expandedRows=137 · distinctMethodPaths=135 · controllers=25`.
`api-sweep` chạy **145 probe** (nhiều endpoint được probe nhiều lần: mỗi role, mỗi error path), phủ hết 15 area.
Endpoint không probe trực tiếp đều nằm trong 2 n/a (có lý do) hoặc là biến thể path đã probe.

## 2 probe N/A (có lý do, không bịa)

| Probe | Lý do | Bù bằng |
|---|---|---|
| `POST /api/auth/avatar/upload` | multipart media thật | `g8-speaking-human-audio.py` dùng file thật (Phase U/D) |
| `POST /api/v1/speaking-submissions/upload` + `/assess` | cần media thật (MinIO object) | `g7`/`g8` probes (Phase 6/7) |

## Rate-limit bucket

`flushBuckets()` chạy trước mỗi batch → **0 self-429** (không có 429 nào trong log).

## AI stress (T1.6) — vòng này thêm mới

Vì F-17-11 (500 ngắt quãng) chỉ lộ ở vòng 2, vòng này gọi AI **lặp ngay từ Phase 1**:
- `POST /api/ai/generate-vocab` ×10 → **10/10 = 200** (2.9–4.2 s/call)
- `POST /api/ai/enrich-word` ×5 → **5/5 = 200** (1.0–1.5 s/call)
- Dictionary cold/warm: `ubiquitous` cold **19 765 ms** → warm **86 ms** (khớp ~20 s v17)

→ **Không tái hiện 500/504.** Fix F-17-11 (lenient parse) + F-17-12 (timeout 120 s) còn hiệu lực.
Chi tiết: `evidence/ai-stress.md`.

## Parity sau Phase 1

`1470|43738|5|118|29|4|3|12|10` · STUDY_DAYS=4 · PENDING_PAYMENTS=0 · EXERCISE_ATTEMPTS=33 → **CLEAN** (không residue).

## Deep-probe breakdown

`mc-guard 18/0 · sort 2/0 · contract 13/0 · roles 25/0` — cleanup `DEEP_SD_REMAINING=0`, 0 SQL errors.
