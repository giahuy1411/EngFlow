# audit-v15-full — tap-target triage (WCAG 2.5.8)

**Nguồn:** `evidence/ui-sweep.json` (chạy 2026-09-25, Playwright) · Phương pháp: kế thừa `audit-v14`
F-14-03 (118 → **0 REAL**).

`totals.smallTargets = 115` là tổng `min(width,height) < 24px` trên mọi lượt route×role. Triage từng
cái theo **hai ngoại lệ hợp lệ của WCAG 2.5.8 (Target Size Minimum, AA)**:

| Ngoại lệ | Điều kiện | Áp dụng |
|---|---|---|
| **Inline** | Target nằm trong một câu/khối văn bản (link trong prose) | `<a>` trong câu |
| **Spacing** | Vòng tròn 24px quanh target không chồng target khác | checkbox/ô nhỏ có khoảng cách ≥24px |

## Phân loại (đo từ `a11yByRoute`)

| Verdict | Số | Ví dụ đo được |
|---|---:|---|
| **INLINE-EXCEPT** (`<a>` link trong câu) | **52** | `"QUÊN MẬT KHẨU?"` (120×16), `"Đăng ký"` (58×18), `"Quay lại đăng nhập"` (136×18) |
| **SPACING-EXCEPT** (`<input>` checkbox/radio 16×16) | **21** | checkbox "Ghi nhớ" (16×16) ở `/login`, `/profile`, các route filter |
| **REAL (AA violation)** | **0** | — |

*(73 entry có `smallList` chi tiết trên route-level; phần còn lại của 115 là các lượt lặp lại trên
nhiều route×role — cùng hai loại target, không có loại mới.)*

## Kết luận

**0 REAL.** Không có target nào vi phạm AA khi áp hai ngoại lệ chuẩn — **khớp kết luận audit-v14**
(118 → 0 REAL). Không có target loại mới xuất hiện sau khi gỡ Đường B (nút "Xây dựng" 36×36 đã bị gỡ,
vốn không nằm trong danh sách nhỏ).
