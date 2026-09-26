# audit-v15-full — review chéo (đối kháng)

**Ngày:** 2026-09-25 (+07) · Yêu cầu thường trực: mọi code AI sinh phải được review chéo.
Không có agent review độc lập trong phiên này → **tự review inline** theo checklist đối kháng
(tiền lệ: `audit-v14-full/evidence/review-f14.md` §6).

## Phạm vi review: 5 thay đổi rủi ro nhất

### RV1 — `AdminUploadController` (tách endpoint dùng chung)

**Câu hỏi đối kháng:** tách có làm mất guard nào không?

| Kiểm tra | Kết quả |
|---|---|
| Path-traversal guard trong `getResource` | **giữ nguyên** — live `..%2F..%2Fpom.xml` → **400** |
| `SafeUploadNames.contentTypeFor` + `forceDownload` | **giữ nguyên** (copy verbatim) |
| Phân quyền `/api/admin/**` | do `SecurityConfig`, **không** khai lại trong controller → anon **401**, student **403** |
| `GET /api/resources/**` permitAll | **giữ** — live fetch-back **200** |
| Test phủ | `AuditV8UploadXssTest` **9/9** + `RateLimitFilterTest` **14/14** |

**Verdict:** ✅ an toàn. Không nới quyền, không mất guard.

### RV2 — `LessonService.deleteLesson` (bỏ cascade Đường B)

**Câu hỏi:** xoá lesson còn nổ FK không? Có sót bảng con nào?

| Kiểm tra | Kết quả |
|---|---|
| 12 FK tới `users` + FK tới `lessons` | đã liệt kê; các bảng con còn lại vẫn có bước xoá |
| Live test | tạo lesson 114115 → DELETE **204** → GET **404** |
| Unit test | `LessonServicePaginationTest` 4/4 |
| Bảng đã DROP còn được tham chiếu? | `grep` = **0** trong `src/main` |

**Verdict:** ✅. `deleteLesson` không còn chạm bảng đã xoá.

### RV3 — Migrate 3 câu hỏi → `exercises`

**Câu hỏi:** dữ liệu có đúng hợp đồng `ExerciseService`/frontend không? Có lộ đáp án không?

| Kiểm tra | Kết quả |
|---|---|
| `options` format | JSON array **chuỗi** (khớp 43k hàng cũ) |
| `exerciseType` hợp lệ | đều ∈ enum; TRUE_FALSE → `MULTIPLE_CHOICE ["True","False"]` (enum không có TRUE_FALSE) |
| Grade đáp án ĐÚNG | **3/3** |
| Grade đáp án SAI | **0/3** (không phải "luôn đúng") |
| `correctAnswer` cho learner | **None** (ẩn) — tốt hơn đường `/structure` cũ |
| `order_index` | 6,7,8 nối sau 5 hàng cũ |
| Timestamp | **đã sửa** naive-VN (F-15-06) |

**Verdict:** ✅. Không lộ đáp án, chấm đúng cả hai chiều.

### RV4 — Script cleanup C1/C2

**Câu hỏi:** có thể xoá nhầm dữ liệu thật không?

| Kiểm tra | Kết quả |
|---|---|
| Assertion trước commit | `RAISERROR` nếu ≠ 67/114/12 |
| Guard chặn user thật | `IF EXISTS (... IN (2,3,70009,120010,150040)) RAISERROR` |
| Dry-run | `ROLLBACK` → DB vẫn `72\|126` |
| Backup + drill | `VERIFYONLY` valid + restore parity khớp |
| Sau commit | 5 user thật còn, 12 SUCCESS còn, **0 orphan** |
| `QUOTED_IDENTIFIER ON` | có (tránh `Msg 1934`) |

**Verdict:** ✅. Nhiều lớp chặn; không thể xoá user thật mà im lặng.

### RV5 — Gỡ frontend (route, nút, component)

**Câu hỏi:** gỡ có sót tham chiếu gây lỗi runtime không?

| Kiểm tra | Kết quả |
|---|---|
| `grep` token Đường B trong `frontend/src` | chỉ còn comment + `uploadService` |
| `/admin/exercises` (dùng `uploadService`) | render **OK**, 0 lỗi |
| `LessonLayout` (bỏ `<LessonBlocks/>`) | `LessonLayout.h1.test.js` 2/2 |
| `/admin/447/build` | redirect `/` |
| `AdminLessons` aria-labels | chỉ Sửa/Xóa — **0** nút "Xây dựng" |
| `LayersIcon` không dùng | **đã gỡ import** |

**Verdict:** ✅. Không còn tham chiếu treo.

## Phát hiện qua review chéo

| # | Phát hiện | Xử lý |
|---|---|---|
| RV-1 | `perf-probe` ghi vào `sweep/v13` (nhãn sai) | F-15-13 — sửa `__dirname` |
| RV-2 | `ui-sweep` so URL đầy đủ với path trần (27 fail giả) | F-15-11 |
| RV-3 | `ui-sweep` kỳ vọng premium anon → `/login` | F-15-12 |
| RV-4 | `focused-probe` đo "builder" trên trang chủ | F-15-14 |
| RV-5 | 2 probe v13 khẳng định section đã gỡ | F-15-15 |

## Kết luận

5/5 thay đổi rủi ro cao **đạt** review đối kháng. Review chéo phát hiện **5 lỗi probe** (đều là harness,
không phải sản phẩm) — nghĩa là bước này **có giá trị thật**, không phải hình thức.
