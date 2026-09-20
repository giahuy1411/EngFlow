# Phase 0.3 — Trạng thái deployed (đo 2026-09-19 23:5x +07)

## Kết luận: container backend đang chạy CODE CŨ

Bằng chứng cứng, không suy đoán:

```
node sweep/v10/probe-authed.js
logged in as user@gmail.com

/api/streak/snapshot -> 404
  body: {"detail":"Không tìm thấy tài nguyên yêu cầu.","instance":"/api/streak/snapshot","status":404,"title":"Not Found"}

/api/streak/current  -> 200
  body: {"currentStreak":1,"today":"2026-09-19"}
```

## Cách đọc kết quả này

| Endpoint | Kết quả | Ý nghĩa |
|---|---|---|
| `/api/streak/snapshot` | **404** | Route **không tồn tại** → code cũ chưa có endpoint mới |
| `/api/streak/current` | **200** | Route cũ vẫn chạy, trả `currentStreak` theo logic cũ |

**Bẫy đã tránh:** lần probe đầu không có token trả **401** cho cả 4 endpoint — kể cả `/actuator/health`. Nếu đọc 401 là "route tồn tại" thì đã kết luận sai. 401 chỉ nói security chain từ chối, **không** nói controller có tồn tại. Phải đăng nhập thật mới phân biệt được 404 vs 200. Ghi lại để vòng sau không mắc lại.

## Bằng chứng bổ sung

```
docker exec engflow-backend ls -la /app/app.jar
-rw-r--r-- 1 root root 146346826 Sep 18 01:27 /app/app.jar
```
JAR đóng ngày **18/09 01:27** — trước khi streak refactor hoàn tất.

```
docker images
engflow-backend:latest   45 hours ago
```

## Hệ quả

1. **`/api/streak/snapshot` chưa hoạt động trên live.** Không thể verify contract endpoint này qua HTTP cho tới khi rebuild.
2. **`StreakCalendar` mới + `Profile.vue` mới sẽ không hoạt động trên live** — chúng gọi `streakService.getSnapshot()` → 404 → rơi vào nhánh lỗi → hiện nút "Thử lại". Đây là hành vi **đúng theo thiết kế** (đã có test `Profile.test.js` phủ), nhưng nghĩa là **UI streak mới chưa chạy được cho tới khi rebuild**.
3. **`currentStreak: 1` từ `/api/streak/current` là số CŨ** (đếm ngày đăng nhập), **không phải** ngày học tập. Không được dùng nó làm bằng chứng cho streak mới.

## Việc cần làm

`docker compose up -d --build backend` — hiện chưa chạy được vì chưa có trong allowlist.

Sau khi rebuild, chạy lại `node sweep/v10/probe-authed.js` và kỳ vọng:
- `/api/streak/snapshot` → **200** với đủ 7 field: `today`, `currentStreak`, `studiedToday`, `effectiveFrom`, `studiedDays`, `legacyAccessDays`, `legacyHistoryAvailable`
- `effectiveFrom` phải là `2026-09-20`
- `studiedToday` phải là `false` (hôm nay 19/09 < cutover)
