# Phase 3.1 — Đo lại 4 mục backlog (2026-09-19 23:5x +07)

Theo C5: **đo lại → điều tra lệch → backup → hành động → verify → parity**. Đây là bước đo.

## Kết quả đo

```
BAK_TABLES=exercises_bak_v5,exercises_bak_v5b,exercises_bak_v5c,exercises_bak_v5d
ZZ_USERS=4
EMPTY_ANSWER=4848
LISTENING_NO_AUDIO=9
```

## So với `issues.md` (audit-v9)

| Mục | issues.md | Đo lại | Khớp? |
|---|---|---|---|
| Bảng `exercises_bak_v5*` | 4 bảng | **4 bảng** | ✅ |
| User `zz*@example.com` | 8 | **4** | ❌ **LỆCH** |
| Exercise `correct_answer` rỗng | 4.848 | **4.848** | ✅ |
| LISTENING thiếu `audio_url` | 9 | **9** | ✅ |

## Điều tra mục lệch: user `zz*` là 4, không phải 8

Danh sách đầy đủ (đo trực tiếp, liệt kê ID cụ thể — **không** dùng `LIKE` trần để hành động):

| user_id | email | created_at |
|---|---|---|
| 170049 | zzprobe30119@example.com | 2026-09-12 12:47:28 |
| 170050 | zzprobe65536@example.com | 2026-09-12 12:47:56 |
| 170051 | zzprobe79221@example.com | 2026-09-12 12:48:47 |
| 170097 | zzprobe15227@example.com | 2026-09-12 15:17:21 |

**Kết luận:** 4 user còn lại đều là `zzprobe*` tạo trong khoảng 12:47–15:17 ngày 12/09. Con số 8 trong `issues.md` **đã cũ**.

**Giả thuyết có khả năng nhất:** 4 user còn lại đã bị xoá ở một lần cleanup trước đó. `issues.md` ghi rõ một bản nháp `v9_cleanup_sweep.py` từng xoá 4 user bằng `email LIKE 'zz%'` rồi phải restore. Có thể lần cleanup hợp lệ sau đó đã xoá 4 trong 8, và tài liệu chưa cập nhật.

**Không kết luận chắc chắn** vì chưa đối chiếu được log cleanup. Điều chắc chắn: **hiện tại chỉ có 4**, và chúng có ID cụ thể.

## Ý nghĩa cho kế hoạch

1. **Nếu tin số cũ (8)** thì sẽ đi tìm 4 user không tồn tại, hoặc tệ hơn — nới rộng điều kiện tìm kiếm để "đủ 8", đúng kiểu sai lầm đã gây ra F111 ở v9 (xoá nhầm 4 user baseline).
2. **Baseline parity sẽ đổi ít hơn dự kiến.** Kế hoạch dự đoán parity mới là `1471|43737|68|...` (76 − 8). Nhưng chỉ có 4 user để xoá → parity mới sẽ là `1471|43737|72|...` (76 − 4), **nếu** quyết định xoá.
3. **Chưa hành động gì.** Mục này vẫn chờ: backup verify + danh sách ID cụ thể + quyết định của chủ dự án.

## Trạng thái 4 mục

| Mục | Đo lại | Sẵn sàng hành động? |
|---|---|---|
| 4 bảng `bak_v5*` | ✅ khớp | Cần: backup + kiểm tra không có FK/reference trỏ tới |
| 4 user `zz*` | ⚠️ lệch | Cần: backup + xác nhận chủ dự án + **4 ID cụ thể đã liệt kê** |
| 4.848 empty answer | ✅ khớp | Là quyết định nội dung (C6: phân loại, **không** sinh key) |
| 9 LISTENING thiếu audio | ✅ khớp | Cần: chứng minh đường TTS→Cloudinary bằng file media thật |
