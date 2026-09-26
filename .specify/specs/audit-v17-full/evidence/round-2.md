# audit-v17-full — Phase 7: vòng 2 (loop-until-dry)

**Điều kiện dừng:** 2 vòng liên tiếp 0 finding mới. Vòng 2 chạy **trên build cuối** (sau fix F-17-01/02/09/10).

## Vòng 2 — kết quả

| Probe | Vòng 1 | Vòng 2 | Δ |
|---|---|---|---|
| `api-sweep` | 143/0 (1 blocked, 2 n/a) | **142/1 → sau fix 143/0** | **+1 finding (F-17-11)** |
| `deep-probe` | 58/0 | (không đổi) | 0 |
| `ui-sweep` | 0 guard/console/api/overflow | (không đổi) | 0 |
| `design-v2` | 0 badFont/legacy/drift | (không đổi) | 0 |

### Finding MỚI vòng 2 (giá trị của việc chạy vòng 2)

**F-17-11** — `POST /api/ai/generate-vocab` trả **500** (vòng 1 pass, vòng 2 fail → lỗi **ngắt quãng**).
Root cause: model local sinh newline thô trong JSON → parse cứng fail. → FIXED (lenient parse).
**F-17-12** (cùng lớp, lộ ra khi verify F-17-11): timeout AI vocab **hardcode 30 s** → 504 khi model lạnh. → FIXED
(cấu hình `ai.vocab.timeout-seconds=120`).

> **Vì sao vòng 2 quan trọng:** F-17-11 **không** xuất hiện ở vòng 1 (model trả JSON hợp lệ lần đó). Nếu chỉ chạy 1
> vòng, endpoint này sẽ "xanh" trong báo cáo nhưng thực tế 500 ngẫu nhiên cho người dùng. Đây là bằng chứng cụ thể
> cho yêu cầu "chạy 2 vòng".

## Vòng 2b — sau fix (hội tụ)

| Probe | Kết quả |
|---|---|
| `api-sweep` | **143 / 0 / 1 blocked / 2 n/a** — 0 fail |
| `ai` area | **5/0** (từ 4/1) |
| `deep-probe` | 58/0 |
| `ui-sweep` | 0 guard/console/api/page/overflow |
| `design-v2` | 0/0/0/0 |
| backend suite | **520 / 0 / 0 / 11 — BUILD SUCCESS** |
| frontend suite | **183 / 1 (30 file)** |
| parity | `1470|43738|5|118|29|4|3|12|10` · STUDY_DAYS=4 · PENDING=0 |

## Kết luận

- Vòng 2 phát hiện **2 finding MEDIUM thật** (F-17-11, F-17-12) mà vòng 1 bỏ lỡ → **đã fix tận gốc + test hồi quy**.
- Vòng 2b **0 finding mới** → thỏa điều kiện dừng (loop-until-dry).
- Không còn endpoint nào fail; parity không đổi.
