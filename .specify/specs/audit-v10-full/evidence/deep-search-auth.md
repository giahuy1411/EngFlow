# Phase 4.8 + 4.9 — Deep check Đăng nhập/Đăng kí và Tìm kiếm/Sắp xếp (2026-09-20 01:1x +07)

## 4.8 — Đăng nhập / Đăng kí: **19 PASS, 0 FAIL**

| Nhóm | Kiểm | Kết quả |
|---|---|---|
| Đăng kí | hợp lệ, trùng email, thiếu username, thiếu email, email sai định dạng, mật khẩu < 6, username < 3 | **7/7 PASS** |
| Đăng nhập | đúng, sai mật khẩu, email không tồn tại, body rỗng | **4/4 PASS** |
| Token | `/me` có token, không token, token rác, token bị sửa 4 ký tự | **4/4 PASS** |
| Không lộ thông tin | `/me` và `/login` không chứa `passwordHash`/`password` | **3/3 PASS** |

User kịch bản `zzauth…@example.com` đã được dọn theo ID cụ thể; tổng users về đúng **72**.

**Chính sách mật khẩu đo được:** chỉ có ràng buộc **độ dài ≥ 6**. Không có yêu cầu chữ hoa/thường/số/ký tự đặc biệt. Đây là ghi nhận, **không phải lỗi** — chưa từng có yêu cầu nào về độ mạnh mật khẩu trong dự án.

## 4.9 — Tìm kiếm / Sắp xếp: **25 PASS, 0 FAIL**

### Phân trang — sạch

`page=0&size=5` trả đúng 5 phần tử và **không trùng** với `page=1`. Các tham số biên (`page=-1`, `size=-5`, `size=100000`) đều **không gây 500** — chúng được kẹp về khoảng hợp lệ.

### Tìm kiếm — sạch

`q` rỗng, `q` không có kết quả, `q` chứa `%_'\"--`, `q` dài 500 ký tự, và `q` kiểu SQL injection (`' OR 1=1--`) đều **không gây 500** và không rò dữ liệu.

### Sắp xếp — một khoảng trống sản phẩm, không phải lỗi

**Đo được: không controller nào nhận `sort` từ client.**

```
LessonController:38          Sort.by("orderIndex").ascending().and(Sort.by("id"))
DeckController:34,50         Sort.by("name").ascending().and(Sort.by("id"))
AdminExerciseController:39   Sort.by("orderIndex").ascending().and(Sort.by("id"))
```

Và **frontend không bao giờ gửi `sort`**:
```
Lessons.vue:173   const params = { page: currentPage.value - 1, size: pageSize }
                  // chỉ thêm `level` và `q`
```

Nên `?sort=id,desc` là **tham số URL bị bỏ qua im lặng**. Bản đầu của probe giả định asc/desc phải khác nhau và báo **3 FAIL** — đó là **giả định sai của probe**, không phải lỗi app. Đã sửa probe để kiểm đúng thứ có thật.

**Thứ thật sự cần kiểm — và đã kiểm:** thứ tự hard-code phải **ổn định** giữa các lần gọi. Nếu không, phân trang sẽ bỏ sót hoặc lặp row. Đo: 2 lần gọi cùng tham số trả về **cùng thứ tự** → PASS.

**Ghi nhận trung thực:** "sắp xếp" trong dự án này **chưa phải một tính năng**. Nếu sau này muốn có, phải thêm `@PageableDefault` hoặc đọc `sort` từ request — đó là **thay đổi sản phẩm**, không phải sửa lỗi. Ghi lại để vòng sau không "phát hiện" nó như một defect, và cũng không sửa nó như một defect.

### Bài học phương pháp

Một probe viết theo **điều mình tưởng hệ thống nên làm** sẽ báo lỗi ở nơi hệ thống chỉ đang làm khác. Ba FAIL ở đây trông như một lỗi sắp xếp nghiêm trọng; đọc source cho thấy tham số chưa từng được nối. **Đọc source trước khi tin kết quả probe.**
