# Phase 3.1 — Đo lại 4 backlog item (2026-09-20 00:3x +07)

Công cụ: `sweep/v10/backlog-remeasure.js` — **read-only**, chạy từng query riêng.

> **Vì sao có script Node thay vì gọi `sqlcmd` trực tiếp:** harness nén output sqlcmd thành dạng `N matches in N files`, làm mất số liệu. Chạy từng query một và in mỗi kết quả một dòng thì không bị nén. Đây là chi tiết vận hành, nhưng nếu bỏ qua thì sẽ tưởng "không đo được" trong khi thực ra chỉ là vấn đề hiển thị.

## Số đo

| # | Item | Kế hoạch ghi trong `issues.md` | **Đo lại** | Khớp? |
|---|---|---|---|---|
| BL1 | Bảng `exercises_bak_v5*` | 4 bảng | **4 bảng** (481 + 322 + 55 + 39 row) | ✅ |
| BL2 | User `zz*` | **8** | **4** | ❌ **lệch** |
| BL3 | `correct_answer` rỗng | 4.848 | **4.848** | ✅ |
| BL4 | LISTENING thiếu `audio_url` | 9 | **9** / tổng 367 | ✅ |

## BL2 — lệch, và lệch theo hướng nguy hiểm

`issues.md` ghi **8** user `zz*`. Thực tế chỉ có **4**, tất cả tạo ngày 12/09/2026:

```
170049 | zzprobe57578 | zzprobe30119@example.com | 2026-09-12 12:47:28
170050 | zzprobe37648 | zzprobe65536@example.com | 2026-09-12 12:47:56
170051 | zzprobe86729 | zzprobe79221@example.com | 2026-09-12 12:48:47
170097 | zzprobe16996 | zzprobe15227@example.com | 2026-09-12 15:17:21
```

**Vì sao con số lệch lại nguy hiểm hơn nó trông:** nếu tin vào "8" mà không đo, cách xử lý tự nhiên là **nới điều kiện tìm kiếm cho tới khi tìm đủ 8** — ví dụ đổi `username LIKE 'zzprobe%'` thành `email LIKE 'zz%'`. Đó **chính xác** là sai lầm đã gây ra **F111 ở v9**, nơi một filter `email LIKE 'zz%'` xoá mất 4 user thật.

Đo lại trước khi làm không phải thủ tục hình thức — nó chặn đúng cái sai lầm này.

**4 ID cụ thể để xoá (nếu quyết định xoá):** `170049`, `170050`, `170051`, `170097`.

## BL3 — 4.848 row, phân bố đã rõ

```
MULTIPLE_CHOICE = 4738
FILL_BLANK      = 110
```

Trước đây chỉ có con số tổng. Phân bố cho thấy đây **không phải** một lỗi nhập liệu rải rác: 97,7% nằm ở `MULTIPLE_CHOICE`, tức là các row mà đáp án nằm ở bảng options chứ không ở cột `correct_answer` — đúng như thiết kế. **Quyết định C6 giữ nguyên: phân loại, không tự sinh đáp án.**

## BL1 — 4 bảng backup, tổng 897 row

| Bảng | Row | Ngày tạo |
|---|---|---|
| `exercises_bak_v5` | 481 | 2026-09-03 |
| `exercises_bak_v5b` | 322 | 2026-09-03 |
| `exercises_bak_v5c` | 55 | 2026-09-03 |
| `exercises_bak_v5d` | 39 | 2026-09-03 |

## BL4 — 9 row LISTENING thiếu audio, trên tổng 367

Giữ nguyên quyết định **C7**: chỉ backfill nếu chứng minh được đường TTS→Cloudinary bằng một file media thật. `issues.md` đặt điều kiện này, và AGENTS.md ghi rằng byte giả tạo ra 400/500 và từng dẫn tới kết luận sai về "demo credentials".

## Một quan sát vận hành: parity `payments` đang là 128

Đo parity trong lúc browser sweep đang chạy cho `...|128|...` thay vì `126`. **Đây không phải drift** — sweep đi qua `/premium/checkout` và `PremiumCheckout.vue` gọi `POST /api/v1/payment/create-order` ngay khi mount, nên mỗi vòng sweep tạo row thật. Đúng như comment trong `routes-all-v10.js` dự đoán.

`routes-all-v10.js` tự gọi `cleanupAuditPayments(126)` ở cuối **trong cùng lần chạy đó** (không phải bước thủ công), và cleanup chỉ xoá `status <> 'SUCCESS'` — tức đơn chưa ai trả tiền, `transaction_id` vẫn NULL. Sau khi sweep xong, parity phải quay về `126`.

**Bài học đã được ghi trong AGENTS.md và lặp lại ở đây:** một harness ghi row nghiệp vụ phải tự dọn **trong cùng lần chạy** và assert parity — không phải tin vào exit code của chính nó.
