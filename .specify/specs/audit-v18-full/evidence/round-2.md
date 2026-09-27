# audit-v18-full — Phase 7: vòng 2 (loop-until-dry)

**Điều kiện dừng:** 2 vòng liên tiếp 0 finding mới. Vòng 2 chạy **trên build cuối** (sau fix F-18-01 + mở rộng
fix sang 3 probe còn lại + dọn comment/marker).

## Vòng 2 — kết quả

| Probe | Vòng 1 | Vòng 2 | Δ |
|---|---|---|---|
| `api-sweep` | 145/0/0/2na | **145/0/0/2na** | 0 |
| `deep-probe` | 58/0 | **58/0** | 0 |
| `ui-sweep` | 0 guard/console/api/overflow | **0** (35 ô, parity CLEAN) | 0 |
| `design-v2` | 0 badFont/legacy/drift | **0** (7670 el, 655 lucide) | 0 |
| Backend suite | 537/0/0/11 | **537/0/0/11** | 0 |
| Frontend suite | 194/1 (32) | **194/1 (32)** | 0 |
| Parity | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` | **khớp** + STUDY_DAYS=4/PENDING=0/EX_ATT=33 | 0 |

## Finding mới vòng 2

**0 finding mới.** Vòng 2 hội tụ.

## Vòng 1 đã bắt gì (giá trị của vòng lặp)

Vòng 1 bắt **F-18-01** (harness hardcode `chromium-1237` → 2 probe launch fail). Vòng 2 **không** tái hiện
(đã fix). Ngoài ra vòng 1 ghi 2 doc-drift (F-18-02/03) sẽ sửa ở Phase 8 — không phải lỗi runtime.

## Case biên / adversarial (T7.2)

| Case | Kết quả |
|---|---|
| AI gọi lặp (kênh ngắt quãng F-17-11) | 10× generate-vocab + 5× enrich → **15/15 = 200** |
| Dictionary cold/warm | cold 19 765 ms → warm 86 ms |
| SePay replay (chữ ký hợp lệ nhưng timestamp cũ) | **bị từ chối** (g6 PASS) |
| SePay chữ ký sai | **bị từ chối** (g6 PASS) |
| Guard role 2 chiều (anon/student/admin) | routes-all 228 visit, **0 wrong landing** |
| Guard < 2 ký tự (UI) | **0 call** `/dictionary/` (CD+PW) |
| Chống lộ đáp án (student) | 6 câu, **0 non-null** `correctAnswer` |
| Speaking im lặng vs audio thật | `g7`/`g8` PASS (transcript thật, recall 0.95) |

## Kết luận

**2 vòng liên tiếp 0 finding mới** (vòng 2 sạch) → **đạt điều kiện dừng**. Không cần vòng 3.
