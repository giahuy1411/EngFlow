# audit-v19-full — analyze (giữa kỳ)

## Chất lượng bằng chứng

**Mạnh:**
- **Đính chính 2 kết luận v18 SAI/THIẾU** bằng đo lại: (a) `smallTargets=115` không phải "toàn false positive" —
  harness cắt list giấu 12 button thật; (b) SePay "real-money boundary" không chính xác (có Test-mode miễn phí).
- **W1 (AI gloss) tái hiện thật** qua Playwright MCP (天气/气候) rồi fix tận gốc (prompt + guard) → 0 CJK ×15.
- **W3 đo được win thật**: index → logical reads 1341→86 (page), 1348→100 (count); endpoint 42.8→26.6ms.
  Đồng thời **bác bỏ giả thuyết projection** (1417 vs 1417 reads — không giảm).
- **Phase U: luồng trọng yếu × CẢ 2 engine khớp nhau** (gồm Premium lần đầu).
- Mọi fix có test + mutation-test; probe tự dọn (parity CLEAN).

**Yếu / giới hạn:**
- **W4 SePay chữ ký thật:** đang **PENDING** — chờ người dùng chuyển khoản thật (không tự động được).
- Video/Speaking/Leaderboard Phase U chỉ PW (bù `ui-sweep`/`routes-all`).
- CD home screenshot timeout (animation) — có PW + ui-sweep bù.

## Finding tới giờ

| ID | Mức | Vấn đề | Trạng thái |
|---|---|---|---|
| F-19-01 | MED | AI gloss CJK | **FIXED** (prompt + guard) |
| F-19-02 | LOW | enrichWord CJK → 500 | **FIXED** (→400) |
| F-19-03 | LOW | UI trống im lặng khi 0 item | **FIXED** |
| F-19-04 | LOW | smallTargets harness sai | **FIXED** |
| F-19-05 | MED | admin exercise full scan | **FIXED** (index) |
| W4 | — | SePay chữ ký thật | **PENDING** (chờ chuyển khoản) |

## Đối chiếu spec ↔ plan ↔ tasks ↔ evidence
O1 API ✅ · O2 UI ✅ · O2b Phase U (2 engine) ✅ · O3 DB ✅ · O4 demo claims ✅ · **O5-W1 ✅ · O6-W2 ✅ ·
O7-W3 ✅ · O8-W4 PENDING** · O9 chưa (2 vòng/docs/rác/report).
