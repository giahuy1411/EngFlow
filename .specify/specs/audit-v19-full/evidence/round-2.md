# audit-v19-full — Phase 7: vòng 2 (loop-until-dry)

**Điều kiện dừng:** 2 vòng liên tiếp 0 finding mới. Vòng 2 chạy trên build cuối (sau fix W1/W2/W3 + review).

## Vòng 2 — kết quả

| Probe | Vòng 1 | Vòng 2 | Δ |
|---|---|---|---|
| `api-sweep` | 145/0/0/2na | **145/0/0/2na** | 0 |
| `deep-probe` | 58/0 | **58/0** | 0 |
| `search-sort` | pass | **pass** | 0 |
| `ui-sweep` | 0/0/0/0 (smallTargets 0) | **0/0/0/0 (smallTargets 0)** | 0 |
| `design-v2` | 0 badFont/legacy/drift | **0** | 0 |
| Backend suite | 537/0/0/11 | **541/0/0/11** (+4 W1) | +4 test |
| Frontend suite | 194/1 (32) | **194/1 (32)** | 0 |
| Parity | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` | **khớp** + PENDING=0 | 0 |

## Finding mới vòng 2

**0 finding mới** (ngoài các fix đã làm ở vòng 1). Vòng 2 hội tụ.

## Vòng 1 đã bắt gì (giá trị vòng lặp)

- **W1 (F-19-01):** AI gloss CJK — tái hiện qua Playwright MCP (天气/气候) → fix prompt+guard → vòng 2 **0 CJK**.
- **W2 (F-19-04):** harness smallTargets sai → fix → vòng 2 **smallTargets=0** (không false positive, vẫn bắt thật).
- **W3 (F-19-05):** admin full scan → index → vòng 2 endpoint nhanh hơn.
- **Review chéo** bắt thêm 3 vấn đề (#1/#2/#6) + 1 lỗ hổng test (#7) → đã sửa.

## Case biên / adversarial

| Case | Kết quả |
|---|---|
| AI gọi lặp (kênh ngắt quãng F-17-11) | 25+ call → 0 500/504; CJK guard 0 lọt |
| `enrich-word` CJK | → **400** (không 500) |
| Quota khi guard loại hết | **không trừ** (review #2) |
| Guard role 2 chiều | routes-all 228 visit, 0 wrong landing |
| Dictionary cold/warm | (v18: cold 19.8s → warm 86ms) |
| SePay replay/bad sig | g6 PASS (rejected) |

## Kết luận

**Vòng 2 = 0 finding mới** → **đạt điều kiện dừng**. Không cần vòng 3.
