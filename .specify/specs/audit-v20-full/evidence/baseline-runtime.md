# audit-v20 — Phase 0: runtime baseline (đo thật)

**Ngày đo:** 2026-09-28 (giờ VN, +07)
**Người/harness:** Claude Code, đo trực tiếp trên máy này.

## Trạng thái trước khi dựng lại

| Container | Trạng thái đo được |
|---|---|
| `engflow-sqlserver` | `Exited (255)` |
| `engflow-redis` | `Exited (255)` |
| `engflow-backend` | crash-loop: `HibernateException: Unable to determine Dialect without JDBC metadata` |
| `engflow-frontend` | Up (200) |
| `engflow-minio`, `engflow-whisper`, `engflow-tts`, `engflow-tailscale` | Up |

→ Đúng như plan dự đoán: backend không boot được vì DB chết.

## Hành động

```
docker compose up -d sqlserver redis      # -> Running, sqlserver healthy sau ~49s
docker compose up -d --build backend      # -> Started EngflowApplication in 12.542 seconds
```

## Kết quả sau dựng lại

| Kiểm tra | Kết quả | Ghi chú |
|---|---|---|
| `engflow-sqlserver` | **Up (healthy)** | |
| `engflow-redis` | **Up** | |
| Backend boot | **`Started EngflowApplication in 12.542 seconds`** | 0 ERROR trong lượt boot hiện tại |
| `GET /api/health` | **401** | Đúng — path này nằm sau `anyRequest().authenticated()`, không phải public. KHÔNG phải lỗi. |
| `POST /api/auth/login` (`user@gmail.com`/`123456`) | **HTTP 200**, có `token` | Response **flat** (không bọc `.data`) — khớp gotcha AGENTS.md dòng 55 |
| Login shape | `token` ở top-level | `data` = null → dùng `d.token` |

## 4 dòng "Internal server error" trong log — ĐÃ ĐIỀU TRA

Log backend chứa 4 dòng `GlobalExceptionHandler: Internal server error`. **Truy nguồn:**
- Timestamp **00:41:49** — thuộc **lượt boot TRƯỚC**, không phải lượt hiện tại (14:50:09).
- Nguyên nhân: `AsyncRequestNotUsableException: ServletOutputStream failed to write: java.io.IOException: Broken pipe`
  trên `ResourceHttpMessageConverter` — client ngắt kết nối giữa lúc stream media.
- **Kết luận: benign** (client disconnect), KHÔNG phải lỗi ứng dụng. Lượt boot hiện tại: 0 ERROR.

## Parity DB (đo thật, `sweep/v8/p16-parity.sql`)

```
lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks
1470|43738|5|118|29|4|3|13|10
STUDY_DAYS=4
PENDING_PAYMENTS=0
EXERCISE_ATTEMPTS=33
```

→ **payments = 13**, KHÔNG phải 12.
→ **F-20-01 xác nhận:** `sweep/v8/ui/lib.js:110` hardcode `PARITY_BASELINE = "...|12|10"` → **baseline cũ**.
   Nguyên nhân đúng: giao dịch SePay THẬT `ENG73E2D3AA2DF6` (quyết định V9 giữ row). Không phải rác.
   `STUDY_DAYS=4`, `PENDING_PAYMENTS=0`, `EXERCISE_ATTEMPTS=33` khớp baseline → không có rác lọt.

## Khoảng trống comment backend (đo thật, không ước lượng)

| Chỉ số | Số đo |
|---|---|
| File Java trong `src/main` | 196 |
| File thiếu Javadoc **cấp class** | **2** (`security/PremiumRequired.java`, `service/AiAnswerBackfillService.java`) |
| Public method thiếu Javadoc | **74**, trải trên **20 file** |
| — trong file đã sửa (MOD) | 12 |
| — trong file chưa đụng | 62 (17 file) |

→ Kế hoạch ước "~57 file" là **SAI (cao hơn thực tế)**; con số thật nhỏ hơn nhiều. Ghi lại để báo cáo trung thực.
