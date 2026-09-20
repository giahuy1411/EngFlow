# Phase 1.4 — Đo Redis legacy `user:login_days:*` (C8)

## Số đo thật (2026-09-19 23:4x +07)

```
docker exec engflow-redis redis-cli --scan --pattern 'user:login_days:*'
→ 43 key

Tổng số ngày trong tất cả các set: 74
TTL của key mẫu (user:login_days:180528): 7,598,953 giây ≈ 88 ngày
Kiểu dữ liệu: set
Ví dụ nội dung user:login_days:180528 → {"2026-09-17"}   (SCARD = 1)
```

## Quyết định (theo C8)

**Giữ nguyên cơ chế đọc-legacy. KHÔNG copy sang bảng bền.**

Lý do, dựa trên số đo chứ không phải phỏng đoán:

1. **Không cấp bách.** TTL còn ~88 ngày. C8 nói chỉ copy nếu "đo được key sắp hết hạn" — 88 ngày không phải sắp hết hạn.
2. **Khối lượng nhỏ.** 43 key / 74 ngày. Nếu sau này cần copy thì chi phí thấp, không có rủi ro mất mát lớn.
3. **Đã có cơ chế đọc sẵn và đã kiểm chứng.** `StudyActivityService.snapshot()` đọc các key này vào `legacyAccessDays`, có xử lý lỗi Redis (bắt `RuntimeException` → `legacyHistoryAvailable = false`) và bỏ qua ngày không parse được.
4. **UI đã phân biệt rõ.** `StreakCalendar.vue` hiển thị nhãn riêng cho ngày trước cutover (`Lịch sử truy cập trước khi áp dụng`) và có dòng giải thích "Ngày học thật được tính từ {effectiveFrom}. Trước đó là lịch sử truy cập, không phải ngày hoàn thành học." — đúng yêu cầu của `tasks/streak-study/README.md`.
5. **Copy thêm một bảng nữa là thêm bề mặt dữ liệu phải bảo trì** mà chưa đo được lợi ích nào.

## Điều kiện để xem lại quyết định

Xem lại nếu: TTL tụt xuống dưới ~14 ngày, hoặc có yêu cầu hiển thị lịch sử truy cập cũ **sau khi** Redis đã xoá, hoặc số ngày legacy tăng lên đáng kể (hàng nghìn).

## Ghi chú

Con số 43 key / 74 ngày là **đo tại thời điểm này**. `StudyActivityService` đọc chúng theo `cutoff` của window (mặc định 30 ngày), nên phần lớn 74 ngày này có thể nằm ngoài cửa sổ hiển thị — điều đó không sao, chúng vẫn được giữ làm lịch sử.
