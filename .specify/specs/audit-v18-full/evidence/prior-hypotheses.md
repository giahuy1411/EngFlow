# audit-v18-full — T0.9 falsify-first ledger

Kết luận các vòng trước là **giả thuyết để kiểm chứng**, KHÔNG phải sự thật. Mỗi mục ghi cách bác bỏ.

| # | Giả thuyết (từ v17) | Cách kiểm chứng phiên này | Trạng thái |
|---|---|---|---|
| H1 | Baseline backend = 520 test | Chạy `.\mvnw.cmd test`, đếm từ log | **BÁC BỎ** — đo được **537** |
| H2 | Baseline frontend = 192/1 (32 file) | Chạy `npx vitest run` | **BÁC BỎ** — đo được **194/1 (32)** |
| H3 | Bundle entry = 177.74 kB | Chạy `npx vite build` | Xác nhận — **177.75 kB** (gzip 67.69) |
| H4 | Parity `1470|43738|5|118|29|4|3|12|10`, STUDY_DAYS=4, PENDING=0, EX_ATTEMPTS=33 | `sqlrun.py p16-parity.sql` | Xác nhận khớp chính xác |
| H5 | `/api/ai/generate-vocab` 500 ngắt quãng (raw newline trong JSON) đã fix bằng lenient parse | Gọi ×N≥10 (Phase 1 T1.6) + Phase 7 | **đo phiên này** |
| H6 | Timeout AI vocab hardcode 30s đã thành `ai.vocab.timeout-seconds=120` | Đọc `AiVocabService` + gọi chậm | **đo phiên này** |
| H7 | Từ điển ngoài ~20s cold, ~ms warm (dictionaryapi.dev chậm từ VN) | Tra cold/warm qua MCP + `l2-*.py` | **đo phiên này** |
| H8 | Perf "không có win rõ" → không tối ưu (P5) | `perf-probe.js` before/after | **đo phiên này** |
| H9 | Font đã là Be Vietnam Pro duy nhất | grep word-boundary + design-v2 assert BVP LOADED | Xác nhận (0 family thứ hai) |
| H10 | UI đã đúng Playful Geometric (191 hard vs 5 soft shadow) | `design-v2.js` 0 badFont/legacy/drift | **đo phiên này** |
| H11 | `exercise_attempts` residue (F-17-07) chưa truy hết nguồn | Đếm marker scoped 2 tài khoản probe | Đo = 33 (khớp v17 cuối) |
| H12 | Playwright MCP UNBLOCKED (Brave) — dùng được | `browser_navigate` :5173 đầu phiên | **đo phiên này** |
| H13 | `/premium/checkout` mount → tạo payment row thật | MCP đi `/premium*` → đếm PENDING_PAYMENTS | **đo phiên này** |

**Kỷ luật:** mỗi kết luận cuối phải có probe thứ 2 độc lập; reviewer cũng có thể sai → đối chiếu lại code.
