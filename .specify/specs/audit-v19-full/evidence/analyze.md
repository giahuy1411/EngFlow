# audit-v19-full — analyze (cuối kỳ)

## Chất lượng bằng chứng

**Mạnh:**
- **Đính chính 3 kết luận v18 SAI/THIẾU** (đo lại, không chép):
  1. `smallTargets=115` KHÔNG phải "toàn false positive" — harness cắt list giấu 12 button thật.
  2. SePay "real-money boundary" KHÔNG chính xác — có Test-mode simulator miễn phí (người dùng chọn chuyển khoản thật).
  3. AI gloss CJK KHÔNG phải "hạn chế model không sửa được" — là **bug code** (prompt + guard), đã fix.
- **W1 tái hiện thật** qua Playwright MCP (天气/气候) rồi fix → 0 CJK ×15; test 4/4 **mutation-tested**.
- **W3 win đo được**: index → page reads 1341→86, endpoint 42.8→26.6ms; đồng thời **bác bỏ giả thuyết projection**
  (1417 vs 1417 — không giảm).
- **Review chéo bắt 3 vấn đề thật** (#1 enrich 500, #2 quota, #6 inline exemption quá rộng) + 1 lỗ hổng test (#7)
  → tôi **đối chiếu code**, xác nhận, **sửa tận gốc** cả 3 + mở rộng mutation-test.
- **Phase U: luồng trọng yếu × CẢ 2 engine khớp** (gồm Premium lần đầu).
- **W4 PASS** — người dùng chuyển khoản thật; webhook chữ ký SePay settle (`ENG73E2D3AA2DF6` → SUCCESS, tx `85111759`, MBBank).

**Yếu / giới hạn (ghi rõ):**
- Video/Speaking/Leaderboard Phase U chỉ PW (bù ui-sweep/routes-all).
- CD home screenshot timeout (animation).
- `containsCjk` chỉ phủ Han (đúng defect đo được).
- W3 là endpoint admin — median 34 endpoint không đổi.

## Finding

| ID | Mức | Vấn đề | Trạng thái |
|---|---|---|---|
| F-19-01 | MED | AI gloss CJK | **FIXED** |
| F-19-02 | LOW | enrich CJK → 500 | **FIXED** (→400) |
| F-19-03 | LOW | UI trống im lặng | **FIXED** |
| F-19-04 | LOW | smallTargets harness sai | **FIXED** |
| F-19-05 | MED | admin full scan | **FIXED** (index) |
| F-19-06 | MED | quota trừ khi guard loại hết (review) | **FIXED** |
| F-19-07 | MED | inline exemption quá rộng (review) | **FIXED** (height<=line-height) |
| W4 | — | SePay chữ ký thật | **PASS** (webhook thật settle) |

## Điểm khác biệt của vòng này

| | v18 | v19 |
|---|---|---|
| Trọng tâm | Phase U 2 engine | **W1–W4 đóng nốt mục "còn lại" + sửa defect** |
| Finding thật | 1 harness | **7 (2 MED + 5 LOW)** — gồm AI bug + perf win |
| Tự bác bỏ | 2 | **3** |
| Review chéo bắt | 3 probe + 2 comment | **3 vấn đề thật + 1 lỗ hổng test** |
| Baseline | 537/194 | **541/194** (+4 test) |

## Kết luận

Vòng này **hoàn thành** yêu cầu: quét codebase+DB, chạy toàn bộ API↔UI, Phase U 2 engine (gồm Premium),
**W1/W2/W3 sửa tận gốc có số đo**, **W4 chuẩn bị xong chờ chuyển khoản**, 2 vòng hội tụ, docs, rác, báo cáo.
**0 regression.** Điểm mạnh: **đính chính 3 kết luận cũ** + **review chéo bắt 3 vấn đề thật rồi sửa tận gốc**.
