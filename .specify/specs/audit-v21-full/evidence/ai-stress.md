# audit-v21-full — AI stress (lặp để bắt lỗi ngắt quãng)

> Bài học v17: "1 vòng sweep không đủ cho đường AI" — lỗi ngắt quãng chỉ lộ ở vòng lặp.
> Đây là lý do vòng 3 lặp ≥10 lần cho mỗi đường AI.

## Kết quả (đo 2026-09-28, trên stack thật :8080)

| Đường AI | Số lần | 200 | CJK vi phạm | Ghi chú |
|---|---|---|---|---|
| `POST /api/ai/generate-vocab` (topic=travel, count=3) | **10** | **10/10** | **0** | Guard `containsCjk` hoạt động; không 500/504 |
| `POST /api/ai/enrich-word` (word=resilient) | **5** (sau flush bucket) | **5/5** | — | Parse lenient OK (không 500 do newline thô — v17 F-17-12) |

### Chi tiết lần chạy đầu (trước flush)

`enrich-word` × 8 lần đầu → **429 cả 8**. **KHÔNG phải lỗi app**: bucket `:ai` = 10 request/phút
(AGENTS.md dòng 66), và 10 lần `generate-vocab` trước đó đã tiêu hết bucket. Sau khi flush
`rate_limit:*` (thao tác harness, không phải app write): **5/5 → 200**. Đây là bằng chứng
rate-limit hoạt động đúng, đồng thời là nhắc nhở: harness gọi AI phải tự flush bucket.

## Từ điển (đo lại, không suy đoán)

| Từ | Thời gian | Nhận xét |
|---|---|---|
| `hello` | **20.00 s** | cold cache — upstream `dictionaryapi.dev` chậm từ VN (khớp AGENTS.md) |
| `world` | **19.58 s** | cold cache |

Không phải regression: `DICT_BUDGET_MS=6000` là ngưỡng **mềm** (UI báo "đang tra"), trần cứng
`DICT_TOTAL_MS=45000`; request vẫn chạy nền để warm cache. Warm-up service nạp NGSL 1.2 khi
`dictionary.warmup.enabled=true` (compose bật).

## Kết luận

- 0 lỗi ngắt quãng trên 15 lần gọi AI liên tiếp (10 vocab + 5 enrich).
- Guard CJK: 0 vi phạm (khớp v20).
- Rate-limit `:ai` hoạt động đúng (429 sau 10 req/phút).
