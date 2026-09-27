# audit-v19-full — T1.6 AI stress + W1 BEFORE (đếm CJK)

**Model:** `qwen2.5:1.5b` (`OPENROUTER_MODEL` trong `.env`, base = Ollama local :11434).
**Mục đích:** (a) F-17-11 (500 ngắt quãng); (b) **W1** — đo tần suất gloss tiếng Trung **TRƯỚC fix**.

## Kết quả BEFORE — CJK **NGẮT QUÃNG** (không tất định)

| Batch | Số call | CJK | Ghi chú |
|---|---|---|---|
| 1 (10 chủ đề) | 10 | **~4** (thấy 鱼 "ocean life", 雨水 "weather", 一种起源… "music", 健康 "health") | bằng chứng thô trong `SAMPLE` |
| 2 (10 chủ đề) | 10 | 0 | — |
| 3 (15 chủ đề) | 15 | 0 | — |

→ **CJK xuất hiện ngắt quãng**, giống hệt cách F-17-11 (500) lộ ra: **một vòng không đủ**. Vì vậy:
- Đây là **bằng chứng thật** của W1 (gloss tiếng Trung xảy ra được), KHÔNG phải "không tái hiện".
- Nhưng **không thể** chứng minh "sửa prompt là đủ" bằng tần suất thấp như vậy → **bắt buộc phải có guard sau parse**
  (quyết định W1: prompt + guard tất định).
- Sau W1, kỳ vọng: **0 CJK trong MỌI call** (guard loại tất định), đo lại ×25.

## F-17-11 (500) — không tái hiện

25/25 call trả **200** (không 500/504) → fix lenient-parse + timeout 120s còn hiệu lực.

## Raw sample (batch 1, CJK)

```
ocean life   → meaning: "鱼"
weather      → meaning: "雨水"
music        → meaning: "一种起源于美国20世纪20年代的…"
health       → meaning: "健康"
```
