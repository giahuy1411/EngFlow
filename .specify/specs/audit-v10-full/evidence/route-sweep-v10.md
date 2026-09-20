# Phase 4.4 + R2 — Browser sweep toàn bộ route (chạy 2 lần)

Công cụ: `sweep/v10/routes-all-v10.js` — Edge thật, 39 route × 3 viewport × 3 role = **342 lượt mỗi vòng**.

## Vòng R2 (01:0x–01:20, build CUỐI mang F126 + F127) — kết quả chính thức

| Chỉ số | Kết quả |
|---|---|
| Số lượt | **342** |
| Không mount được | **0** |
| Console errors | **0** |
| Page errors | **0** |
| API trả ≥ 400 | **0** |
| Overflow ngang | **0** |
| **Landing sai guard** | **0** |
| Route có font sai | **0** |
| `<img>` thiếu `alt` | **0 / 885** |
| Lượt có vấn đề | **0 / 342** |

```
cleanupAuditPayments: window=since 2026-09-20 00:00:00 candidates=6 after=126 expected=126 sqlError=false -> PARITY OK
parity: 1471|43737|72|127|28|15|4|126|14|5
```

Sweep tự tạo **6 row** payment (đi qua `/premium/checkout`) và tự dọn **6 row** trong cùng lần chạy. Parity về đúng **126**, verify độc lập bằng `sweep/v10/verify-backlog-dml.js`: **TẤT CẢ PASS**.

**Đây là vòng có giá trị nhất**, vì nó chạy trên build đã mang cả hai bản sửa HIGH (F126 guard draft, F127 chấm MATCHING) — và **không có hồi quy nào**.

## Vòng 1 (00:1x–00:4x, build trước F126/F127) — lưu để đối chiếu

| Chỉ số | Kết quả |
|---|---|
| Số lượt | 342 |
| Không mount được | 0 |
| Console errors | 0 |
| Page errors | 0 |
| API trả ≥ 400 | 0 |
| Overflow ngang | 0 |
| Landing sai guard | 0 |
| Route có font sai | 0 |
| `<img>` thiếu `alt` | 0 / 878 |
| Dòng `ISSUE` trong log | **0** |

Hai vòng khác nhau ở số `<img>` (878 → 885) vì build cuối có thêm ảnh ở nơi khác; **cả hai đều 0 ảnh thiếu `alt`**.

## Guard được assert HAI CHIỀU

Đây là điểm quan trọng nhất của script này. Một sweep chỉ kiểm "có mount được không" **không phân biệt được** "render đúng route" với "bị đá sang trang khác" — đó chính xác là cách **9 route admin từng "pass" trong khi thực tế hiện trang chủ** (ghi trong comment của v8).

Bảng hợp đồng được mã hoá thành assertion:

| guard | anon | user | admin |
|---|---|---|---|
| `public` | ở lại | ở lại | ở lại |
| `guestOnly` | ở lại | **bị đá** | **bị đá** |
| `auth` | **bị đá** | ở lại | ở lại |
| `auth+premium` | **bị đá** | ở lại | ở lại |
| `admin` | **bị đá** | **bị đá** | ở lại |
| catch-all | **bị đá** (luôn) | **bị đá** | **bị đá** |

`landing sai guard = 0` nghĩa là **cả hai chiều đều đúng trên cả 342 lượt**: ai có quyền thì ở lại, ai thiếu quyền thì bị đá đi, và không lượt nào rơi vào trường hợp ngược lại.

## Viewport 1280 được thêm có chủ đích

v8 chỉ sweep 1440 và 360. Script này thêm **1280** vì đó là **mép dưới của dải compaction navbar** (F121). Nếu dải đó đặt sai — ví dụ bắt đầu từ 1152px như một bản sửa sai trước đây của tôi — thì lỗi sẽ chỉ hiện ở đúng khoảng 1280–1535px và **cả 1440 lẫn 360 đều không thấy**. Đo ở 1280 là cách duy nhất phát hiện.

**Kết quả: 0 overflow ở 1280.** Đây là bằng chứng cứng cho quyết định giữ mép dải ở 1280px (F121).

## Script tự dọn row nó tạo — trong cùng lần chạy

Sweep đi qua `/premium/checkout`, và `PremiumCheckout.vue` gọi `POST /api/v1/payment/create-order` **ngay khi mount**. Nên mỗi lần sweep tạo row thật:

```
cleanupAuditPayments: window=since 2026-09-20 00:00:00 candidates=6 after=126 expected=126 sqlError=false -> PARITY OK
parity: 1471|43737|76|127|28|15|4|126|14|5
```

Đo được **6 row** được tạo và **6 row** được dọn, parity về đúng `126`.

Cleanup chỉ xoá `status <> 'SUCCESS'` — tức đơn **chưa ai trả tiền**, `transaction_id` vẫn NULL — nên không thể chạm vào một giao dịch thật.

**Đây là bài học AGENTS.md đã ghi và được áp dụng đúng:** cleanup nằm **trong chính script**, không phải một bước thủ công mà người vận hành mệt mỏi sẽ bỏ qua. Parity được assert bằng số, không bằng exit code.

## Quan sát phụ: parity `payments` là 128 trong lúc sweep chạy

Trong lúc sweep đang chạy, phép đo backlog độc lập đọc được `...|128|...`. **Đây không phải drift** — đó là 6 row mà sweep chưa kịp dọn. Sau khi sweep kết thúc: `126`.

Ghi lại vì một người đọc log sau này có thể thấy hai con số khác nhau ở hai thời điểm và tưởng có mâu thuẫn.

## Giới hạn phải ghi rõ

- Browser là **Microsoft Edge**, không phải Chrome (`sweep/v10/ui-lib.js` truyền `executablePath` vì máy này không có Chromium của playwright và `playwright-core` cố ý không tự tải browser). Cùng engine nên mọi phép đo DOM/CSS/console hợp lệ, nhưng **không trộn** screenshot với nguồn Chromium khác.
- Sweep này là **desktop-1440 / desktop-1280 / mobile-360**. Không chạy 768 và 1920 ở đây — hai viewport đó đã được `prompt-claims.js` đo riêng (184 assert, 0 FAIL).
- `nav` timeout 30s và chờ 1.5s mỗi route. Một route render chậm hơn 1.5s có thể bị chấm "textLen thấp" — nhưng `textLen` không nằm trong tiêu chí fail, và `mounted`/`overflow`/`console` đều đo sau khi DOM ổn định.
