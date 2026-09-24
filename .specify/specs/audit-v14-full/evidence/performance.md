# audit-v14-full — Phase 5: hiệu năng (đo TRƯỚC khi tối ưu — P5)

**Ngày:** 2026-09-25 (+07) · **Harness:** `sweep/v14/perf-probe.js`, `sweep/v14/cls-probe.js`
**Data:** `sweep/v14/perf-before.json`, `sweep/v14/cls-before.json`

## Điều kiện đo (đọc trước khi tin số — bài học v13)

**v13 đo perf trong khi stack ĐANG ĐỘNG:** backend bị rebuild+restart **2 lần** trong cửa sổ đo; cùng
endpoint `lesson list` cho median **47.1ms** (có tranh chấp) vs **14.4ms** (sạch) — lệch >3×. Đó là lý do
v14 thêm **D3: assert `stackStable`**.

**v14 đo trên stack TĨNH:**

```
stackStable=true (before=2026-09-24T16:25:54.752224533Z after=2026-09-24T16:25:54.752224533Z)
```

→ container `StartedAt` **không đổi** suốt cửa sổ đo. Số dưới đây đo trên **một** backend instance.

## Phương pháp

- Mỗi endpoint gọi **5 lần**, báo **median** (không phải mean — 1 GC pause không được kéo số headline).
- Response body đọc hết (`arrayBuffer()`) trước khi dừng đồng hồ.
- Flush `rate_limit:*` trước mỗi endpoint (global 100/phút/IP).
- 35 endpoint.

## Kết quả — 35 endpoint, chỉ **1** > 100ms

| # | median | endpoint |
|---|---|---|
| 1 | **200.9 ms** | `admin exercise search q=the` |
| 2 | 90.9 ms | `admin exercise list (no q)` |
| 3 | 28.7 ms | `admin stats` |
| 4 | 27.6 ms | `game session (memory)` |
| 5 | 25.1 ms | `leaderboard` |
| 6 | 24.7 ms | `admin lessons` |
| 7 | 23.1 ms | `lesson list (page 5)` |
| 8 | 22.8 ms | `game session (quiz)` |

## Quyết định tối ưu (P5 — chỉ tối ưu khi số biện minh)

| Quan sát | Quyết định |
|---|---|
| `admin exercise search q=the` = **200.9 ms** | **TỪ CHỐI tối ưu** — LIKE `%kw%` trên **43 735 hàng**, không index nào cứu leading-wildcard (đã biết từ v11/v13). Thêm index vô ích = vi phạm P5. |
| `admin exercise list (no q)` = 90.9 ms | Ghi nhận; < 100ms, chưa đo được lợi ích rõ. |
| 33 endpoint còn lại | Đều < 30ms — **không tối ưu**. |

→ **Không tối ưu gì trong v14.** Số không biện minh (đúng P5). Đây là kết quả **từ chối có lý do**, không
phải bỏ qua.

## CLS (Cumulative Layout Shift)

| Route | CLS median | Ngưỡng "good" (<0.1) |
|---|---|---|
| `/` | **0.00069** | ✅ tốt hơn 140× |
| `/lessons` | **0.00095** | ✅ tốt hơn 100× |
| `/login` | **0.00003** | ✅ |

## Lighthouse (chrome-devtools MCP)

Accessibility **100** trên `/`, `/lessons`, `/login` (xem `ui-chrome-devtools.md`).
**Giới hạn host:** Lighthouse không phát category Performance trên host này (như v13 ghi) → perf đo trực
tiếp bằng `perf-probe.js` thay thế. Ghi là giới hạn host, không phải lỗi app.

## Before/After

v14 **không sửa** gì cho hiệu năng → không có bảng before/after. `perf-after` = cùng số (không chạy lại vì
không có thay đổi; nếu chạy lại sẽ trùng). Ghi rõ để không ai tưởng có tối ưu bị bỏ sót.
