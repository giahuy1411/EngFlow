# audit-v21-full — Hiệu năng frontend

**Đo:** 2026-09-28 · chrome-devtools MCP + `vite build` · không throttling, desktop 1440.

## 1. Bundle size (trước vs sau khi comment)

| Mốc | Entry JS | gzip |
|---|---|---|
| Baseline (trước vòng này) | `index-DZ76lATv.js` **177.75 kB** | 67.69 kB |
| Sau comment toàn bộ | `index-DPhMJOxD.js` **177.75 kB** | **67.68 kB** |

→ **Không đổi** (chênh 0.01 kB gzip là do hash/minify). Comment không vào bundle ⇒ xác nhận
các thay đổi là comment-only ở tầng build. Chunk `markdown-*.js` = 64.94 kB (DOMPurify +
marked, đã lazy — không nằm trong entry).

## 2. Core Web Vitals (`/lessons`, chrome-devtools performance trace)

| Chỉ số | Đo được | Ngưỡng "tốt" | ✓ |
|---|---|---|---|
| **LCP** | **434 ms** | < 2500 ms | ✓ |
| **CLS** | **0.00** | < 0.1 | ✓ |
| TTFB | 18 ms | < 800 ms | ✓ |
| Load delay | 353 ms | — | (font/asset) |
| Load duration | 6 ms | — | |
| Render delay | 57 ms | — | |

### CLS qua harness (`cls-probe.js`, 3 lần median)

| Trang | CLS median | armed |
|---|---|---|
| `/` | **0.00069** | false |
| `/lessons` | **0.00095** | true |
| `/login` | **0.00003** | true |

→ Tất cả ≪ 0.1. Không có layout shift đáng kể.

## 3. Lighthouse

| Trang | A11y | Best Practices | SEO |
|---|---|---|---|
| `/` | 100 | 100 | 100 |
| `/lessons` | 100 | 100 | 100 |

## 4. API latency (perf-probe, 34 endpoint, median)

Xem `db-audit.md §7`. Không endpoint nào > 200 ms; cao nhất `admin exercise search q=the` = 155.5 ms
(LIKE `%kw%` trên 43 738 row — không có index nào cứu leading-wildcard, đã biết).

## 5. Kết luận (P5: chỉ tối ưu khi có số)

- **Không có hồi quy hiệu năng.** Bundle đứng yên, LCP 434 ms, CLS ~0, Lighthouse 100/100/100.
- **Không cần tối ưu mới** — không có số nào cho thấy điểm nghẽn. Không thêm index (DMV 0 gợi ý),
  không đổi kiến trúc bundle (DOMPurify đã lazy đúng).
- Việc "tối ưu" duy nhất đã làm trong vòng = **giữ nguyên** baseline xanh (không hồi quy).
