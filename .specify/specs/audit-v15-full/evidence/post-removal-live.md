# Phase 4 — verify trang bài học sau khi gỡ Đường B

**Ngày:** 2026-09-25 (+07) · Trình duyệt thật (Playwright MCP) + API thật + DB thật.

## T4.1 `/lessons/447` tab "Nội dung" — không còn "Nội dung biên soạn"

| Đo | Kết quả |
|---|---|
| `h1` | `ENGLISH GRAMMAR EXERCISES FOR A1 – PRESENT SIMPLE (AFFIRMATIVE)` |
| Tabs | `NỘI DUNG` · `BÀI TẬP` · `LỊCH SỬ` |
| `hasAuthoredSection` (`"Nội dung biên soạn"` ∨ `"Tài liệu bổ sung cho bài học này"`) | **`false`** ✅ |
| `#tabpanel-content` độ dài text | **3955** ký tự (nội dung scrape còn nguyên) |

→ Khối "từ đoạn này đi xuống" người dùng yêu cầu gỡ **đã biến mất**; nội dung scrape ở lại.

## T4.2 Tab "Bài tập" — 3 câu hỏi đã chuyển vẫn làm được

| Đo | Kết quả |
|---|---|
| Số câu | **`8 câu`** ✅ |
| Câu 6 "She ___ to school every day." | ✅ render, có options `go/goes/going/went` |
| Câu 7 "They ___ (play) football on Sundays." | ✅ render |
| Câu 8 "The writer wakes up at 6 AM." | ✅ render, có options `True/False` |

## T4.3 Tab "Lịch sử" — vẫn chạy, chuỗi stale đã sửa

| Đo | Kết quả |
|---|---|
| Panel render | ✅ "LỊCH SỬ LÀM BÀI" / "CHƯA CÓ LỊCH SỬ" |
| `hasStaleString` (`"tab Nội dung"`) | **`false`** ✅ |
| `hasCorrectedString` (`"tab Bài tập"`) | **`true`** ✅ |

## T4.4 Admin — không còn nút "Xây dựng"

| Probe | Kết quả |
|---|---|
| `/admin/447/build` (trực tiếp) | **redirect `/`** (catch-all), không có "Xây Dựng Bài Học" ✅ |
| `/admin/lessons` `aria-label` các nút | chỉ `Mở menu quản trị`, `Sửa bài học`, `Xóa bài học` — **không** có "Xây dựng" ✅ |
| `svg.lucide-layers` | **không có** (icon Layers đã gỡ) ✅ |
| `/admin/exercises` | render OK, **không lỗi** (F-15-03 đã sửa import) ✅ |

## T4.5 F-15-01 — endpoint rò rỉ đáp án đã biến mất

| Probe | Kết quả |
|---|---|
| anon `GET /api/lessons/447/structure` | **404** ✅ |
| admin `GET /api/admin/lessons/447/structure` | **404** ✅ |
| admin `POST /api/admin/upload` (endpoint tách ra) | **200** + URL |
| anon `GET {uploaded url}` | **200** (permitAll giữ nguyên) |
| `GET /api/resources/..%2F..%2Fpom.xml` | **400** (guard path-traversal còn) |
| **So sánh** anon `GET /api/lessons/447/exercises` | 8 item, **0 `correctAnswer`** — đường mới an toàn hơn |

## T3.10 Boot container sau gỡ

`Started EngflowApplication in 34.315 seconds`; **0** dòng log tham chiếu `lesson_section`/`lesson_block`/
`lesson_snapshot`; `GET /api/lessons/447` → 200.

## Kết luận Phase 4

**PASS toàn bộ.** Trang bài học = chỉ nội dung scrape + bài tập (tách hẳn); Đường B không còn dấu vết.
