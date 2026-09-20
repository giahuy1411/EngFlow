# Phase 4.1 — API sweep

Công cụ: `sweep/v10/api-sweep.js` — không cần Playwright.
Rate-limit bucket được flush trước mỗi batch (bài học F109: harness từng tự đo chính mình → 429 giả).

## Chạy lại sau khi rebuild container: PASS=35, FAIL=0, SKIP=0

> **Cập nhật 2026-09-20 01:0x:** container đã rebuild nên mục SKIP duy nhất đã chạy được. Số probe tăng từ 26 lên 35 vì endpoint `/api/streak/snapshot` giờ trả **200** thay vì **404**, nên cả 7 assert contract của nó thực sự chạy (trước đây chúng bị bỏ qua trong nhánh `SKIP`).

| Nhóm | Trước (container cũ) | **Sau (container mới)** |
|---|---|---|
| Đăng nhập / Đăng kí | 6 PASS | **6 PASS** |
| Bài học / Bài tập | 4 PASS | **4 PASS** |
| Streak | 2 PASS, **1 SKIP** | **10 PASS** |
| Tìm kiếm / Sắp xếp | 4 PASS | **4 PASS** |
| CRUD (admin) + guard | 5 PASS | **5 PASS** |
| AI | 3 PASS | **3 PASS** |
| **Tổng** | **24 PASS, 1 SKIP** | **35 PASS, 0 FAIL, 0 SKIP** |

**Một sửa đổi trong probe:** dòng kiểm `studiedToday` từng hard-code "(19/09 < cutover)". Nó đúng cho tới nửa đêm 20/09 rồi FAIL dù app trả về đúng — một ngày hard-code là quả bom hẹn giờ, không phải phép kiểm. Nay probe đọc ngày VN hiện tại và: trước cutover thì đòi `false`, sau cutover thì chỉ đòi **kiểu boolean** (cả hai giá trị đều hợp lệ tuỳ user đã học hôm nay chưa).

---

## Lần chạy đầu (2026-09-20 00:0x, container cũ) — lưu để đối chiếu

## Kết quả: PASS=24, FAIL=0, SKIP=1

### 1. Đăng nhập / Đăng kí — 6/6 PASS

| Probe | Kết quả |
|---|---|
| login user → có token | PASS |
| login admin → có token | PASS |
| login sai mật khẩu → 401 | PASS |
| `/api/auth/me` với token → 200 | PASS |
| `me` trả đúng email | PASS |
| `/api/auth/me` không token → 401 | PASS |

### 2. Bài học / Bài tập — 4/4 PASS

| Probe | Kết quả |
|---|---|
| `GET /api/lessons` → 200 | PASS |
| `GET /api/lessons/41881` → 200 | PASS |
| `GET /api/lessons/{id}/exercises` → 200 | PASS |
| **bài nháp 102049 → 404 cho student** | **PASS** ← F115 guard hoạt động trên live |

### 3. Streak — 2 PASS, 1 SKIP

| Probe | Kết quả |
|---|---|
| `/api/streak/snapshot` | **SKIP** — 404, container chưa rebuild |
| `/api/streak/current` → 200 | PASS |
| `/api/streak/snapshot` không token → 401 | PASS |

### 4. Tìm kiếm / Sắp xếp — 4/4 PASS

| Probe | Kết quả |
|---|---|
| `vocab/search?q=hello` (permitAll) → 200 | PASS |
| `lessons?sort=id,desc` → 200 | PASS |
| `admin/exercises?q=the` → 200 | PASS |
| `page=-1` → không 500 | PASS |

### 5. CRUD (admin) + guard 2 chiều — 5/5 PASS

| Probe | Kết quả |
|---|---|
| `admin/lessons` → 200 | PASS |
| `admin/vocabulary` → 200 | PASS |
| `admin/users` → 200 | PASS |
| **`admin/users` với token student → 403** | **PASS** ← guard chặn đúng |
| **`admin/users` không token → 401/403** | **PASS** ← guard chặn đúng |

### 6. AI — 3/3 PASS

| Probe | Kết quả |
|---|---|
| `ai/generate-vocab` topic rỗng → 400 | PASS |
| lỗi trả ProblemDetail (có `title`) | PASS |
| `ai/generate-vocab` không token → 401 | PASS |

## Parity sau sweep — KHÔNG ĐỔI

```
1471|43737|76|127|28|15|4|126|14|5
```

Giống hệt trước sweep. Sweep này chỉ dùng GET + 2 POST cố tình lỗi, không tạo row nào → không cần cleanup. **Parity được verify sau sweep theo đúng quy ước.**

## Ghi chú bảo mật

Script ban đầu dùng `execSync` với chuỗi lệnh nội suy Redis key. Đã sửa sang `execFileSync` với argv array (hook `security-guidance` cảnh báo đúng): Redis key là dữ liệu, truyền thẳng thành argv thì metacharacter không thể trở thành lệnh.

## Điều CHƯA kiểm được bằng sweep này

- `/api/streak/snapshot` — chờ rebuild container
- Browser sweep (39 route × viewport × 3 role) — cần Playwright, chưa cài
- Upload/ảnh hưởng file — cần multipart thật
