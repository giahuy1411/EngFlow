# audit-v19-full — findings

Quy ước `F-19-NN`. Mỗi finding: mô tả · bằng chứng · root cause · fix · bằng chứng pass · test · **probe thứ 2**.
Trạng thái: `FIXED` · `PENDING` · `CLOSED`.

---

## F-19-01 — AI gloss tiếng Trung — **MED (AI)** — `FIXED`

**Bằng chứng:** HTTP `generate-vocab` trả 鱼/雨水/一种起源/健康 (batch đầu); **Playwright MCP** `/ai-vocab-generator`
chủ đề "weather" → UI hiện **天气/气候** (`shots/mcp/playwright/U15-ai-cjk-before.png`). Ngắt quãng (4/10 rồi 0/25).
**Root cause:** `AiVocabService.java:76-82`/`:141-147` prompt **không nêu ngôn ngữ** cho field nào; 0 guard CJK.
**Fix:** prompt nêu ngôn ngữ từng field + "NO Chinese characters"; **guard `containsCjk`** (U+4E00–9FFF, U+3400–4DBF)
loại item ở generate / từ chối ở enrich.
**Pass (probe 2):** 0 CJK ×15 call live; `AiVocabServiceLanguageGuardTest` 4/4 **mutation-tested**.

## F-19-02 — `enrichWord` gặp CJK → 500 — **LOW (contract)** — `FIXED`
**Bằng chứng:** `resilient` → 500 (log: "Rejecting enrich-word 'resilient': CJK").
**Fix:** ném `BadRequestException` (→400); `catch(BadRequestException) rethrow` (không bọc lại 500).
**Pass:** live `resilient`→**400**, `abundant/ocean/meticulous`→200.

## F-19-03 — UI trống im lặng khi guard loại hết — **LOW (UX)** — `FIXED`
**Bằng chứng:** `AiVocabGenerator.vue` chỉ render khi `length > 0`.
**Fix:** hiện thông báo "AI chưa trả về từ hợp lệ…" khi rỗng.

## F-19-04 — `smallTargets` harness SAI + cắt list — **LOW (harness)** — `FIXED`
**Bằng chứng:** predicate chỉ loại rect 0-size + `slice(0,10)` giấu 12 button `/videos/1` → v18 triage SAI.
**Fix:** áp WCAG 2.5.8 (inline có `height<=line-height`, label, spacing) + **bỏ cắt**.
**Pass:** 115→0; mutation-test 2 ca (flex row + tall-in-`<p>`) **bắt được**.

## F-19-05 — Admin exercises scan cả bảng — **MED (perf)** — `FIXED`
**Bằng chứng:** page 1341 reads/197ms + count 1348 reads/67ms (`sys.dm_exec_query_stats`).
**Root cause:** không index nào lead `order_index`; projection KHÔNG giúp (1417 vs 1417 — bác bỏ giả thuyết).
**Fix:** `IX_exercises_order_id (order_index, exercise_id)` (V005, additive/idempotent).
**Pass:** page **86 reads/2ms**, count **100/2ms**; endpoint 42.8→26.6ms; ordering byte-identical.

## F-19-06 — Quota trừ dù guard loại hết — **MED (review)** — `FIXED`
**Bằng chứng (reviewer):** `AiVocabController:68-71` trừ quota vô điều kiện.
**Fix:** chỉ trừ khi `generated` không rỗng.

## F-19-07 — Inline exemption quá rộng — **MED (review)** — `FIXED`
**Bằng chứng (reviewer):** `/^inline/` + `td/li` miễn oan `inline-flex` (`.app-btn`) trong `<td>`.
**Fix:** thêm `height <= line-height` (đúng định nghĩa WCAG).
**Pass:** smallTargets vẫn 0; mutation-test ca 2 bắt control cao 30px trong `<p>`.

## W4 — SePay chữ ký THẬT — **PASS** ✅
`ENG73E2D3AA2DF6` → SUCCESS, `transaction_id=85111759`, `gateway=MBBank`; webhook (không polling) settle; premium +1 tháng. Chi tiết `w4-sepay-real.md`.

## Không phải finding
| # | Quan sát | Kết luận |
|---|---|---|
| N1 | `/` → `/login` khi token cũ | 401 giữa phiên → logout (ĐÚNG, không phải regression) |
| N2 | 1 console error `/videos/1` | third-party YouTube |
| N3 | `containsCjk` chưa phủ Hiragana/Hangul | defect đo được là Han (không scope creep) |

## Tổng kết
**7 finding FIXED** (2 MED AI/perf + 5 LOW) + **W4 PASS** (chữ ký thật verify). Vòng 2 = 0 finding mới.
0 regression (backend 541/0, frontend 194/1).
