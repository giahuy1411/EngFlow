# audit-v19-full — T0.9 falsify-first ledger

Kết luận các vòng trước là **giả thuyết để kiểm chứng**, KHÔNG phải sự thật.

| # | Giả thuyết (từ v18) | Cách kiểm chứng | Trạng thái |
|---|---|---|---|
| H1 | Baseline backend = 537 test | `.\mvnw.cmd test`, đếm từ log | Xác nhận — **537** |
| H2 | Baseline frontend = 194/1 (32) | `npx vitest run` | Xác nhận — **194/1** |
| H3 | Bundle = 177.75 kB | `npx vite build` | Xác nhận |
| H4 | Parity `1470\|43738\|5\|118\|29\|4\|3\|12\|10` | `sqlrun.py p16-parity.sql` | Xác nhận khớp |
| **H5** | **`smallTargets=115` là false positive (đã triage)** | Đo lại uncapped + phân loại theo WCAG | **BÁC BỎ** — bỏ sót **12 button word-chip** `/videos/1` (ứng viên THẬT) |
| **H6** | **SePay chữ ký thật BLOCKED (biên real-money)** | Khảo sát SePay docs + Funnel | **BÁC BỎ** — có Test-mode simulator miễn phí; Funnel :ts.net trả 200 |
| H7 | AI gloss tiếng Trung là "hạn chế model, không phải bug code" | Đọc prompt `AiVocabService` | **BÁC BỎ (một phần)** — prompt **không nêu ngôn ngữ**; có thể sửa prompt + guard |
| H8 | Perf "không có win rõ" | Đo `findAdminPage` (2 scan + hydrate + title query) | **đo phiên này** (W3) |
| H9 | Font đã là Be Vietnam Pro duy nhất | grep word-boundary + design-v2 | Xác nhận (0 code hit) |
| H10 | UI đã đúng Playful Geometric | `design-v2.js` 0 drift | **đo phiên này** |
| H11 | Playwright MCP dùng được | `browser_navigate` đầu phiên | **đo phiên này** |
| H12 | JWT filter DB-per-request là win tiềm năng | Đo 0.14ms/9.6ms = 1.5% | **BÁC BỎ** — không phải win |

**Kỷ luật:** mỗi kết luận cuối có probe thứ 2 độc lập; reviewer cũng có thể sai → đối chiếu lại code.
