# audit-v19-full — review chéo đối kháng (T6.2)

**Ngày:** 2026-09-27 · **Reviewer:** subagent `general-purpose` độc lập · **Diff:** `AiVocabService.java`,
`AiVocabServiceLanguageGuardTest.java`, `sql/migrations/V005__exercises_order_index.sql`, `sweep/harness/ui-sweep.js`,
`focused-probe.js`.

## Reviewer tìm gì

| # | Mức | Vấn đề | Tôi đối chiếu lại code | Xử lý |
|---|---|---|---|---|
| 1 | MED | `enrichWord` guard ném trong `try` → bị `catch(Exception)` bọc → **500** | **Đúng ở bản cũ**; tôi đã sửa **trước khi** reviewer chạy (thêm `catch(BadRequestException) rethrow`) | ✅ ĐÃ FIX (live: `resilient`→400) |
| 2 | MED | **Quota bị trừ dù guard loại hết item** (`AiVocabController:68-71`) | **Đúng, chưa sửa** | ✅ **FIXED**: chỉ trừ quota khi `generated` không rỗng |
| 3 | LOW | `containsCjk` hẹp hơn "NO other language" (Hiragana/Hangul lọt) | Đúng (đo là Han nên chấp nhận) | Ghi nhận, không mở rộng (đúng phạm vi defect đo được) |
| 4 | LOW | test `lenientParseUnchanged` assert yếu | Đúng (parse không đổi, F-17-11 không regress) | Chấp nhận (parse không bị chạm) |
| 5 | LOW | prompt nhúng ký tự CJK, phụ thuộc UTF-8 source | Đúng; build OK | Chấp nhận |
| 6 | **MED** | **`/^inline/` + `td/li` miễn quá rộng** — control `inline-flex` trong `<td>` bị miễn oan | **Đúng, chưa sửa** | ✅ **FIXED**: thêm điều kiện **`height <= line-height`** (đúng định nghĩa WCAG "constrained by the line-height") |
| 7 | MED | mutation-test không phủ ca "control nhỏ TRONG `<p>`" | Đúng | ✅ **FIXED**: mutation-test mới inject 2 control cao 30px trong `<p>` → **bắt được** (smallCount=2) |
| 8 | LOW | số liệu evidence lệch (14 vs 12) | Đúng — 12 là raw của focused-probe | Sửa evidence |
| 9 | LOW | `smallCount` đổi ngữ nghĩa | Đúng (không consumer nào đọc) | Ghi nhận |

**Verdict reviewer:** *"No change is unsafe to keep"* — nhưng #1/#6/#2 cần sửa.

## Tôi đối chiếu lại (reviewer cũng có thể sai)

- **#1:** reviewer đọc bản **trước** khi tôi sửa `enrichWord` (tôi thêm `catch(BadRequestException)` ở bước trước).
  Xác nhận code hiện tại có rethrow → 400. Reviewer đúng về bản cũ, đã hết hiệu lực.
- **#6:** xác nhận **đúng** — `design-system.css` `.app-btn` là `inline-flex`; `AdminVideoLessons.vue:110` có
  `<router-link class="inline-flex">` trong `<td>`. Đã siết bằng `height <= line-height`.
- **#2:** xác nhận **đúng** — quota trừ vô điều kiện. Đã sửa.
- **#3:** chấp nhận giới hạn (defect đo được là Han; mở rộng Hiragana/Hangul là scope creep không có bằng chứng).

## Kết quả sau fix

- ui-sweep: **smallTargets=0** (không false positive) + mutation-test **bắt được** control thật trong `<p>` và flex row.
- `enrichWord`: CJK → **400** (không 500).
- Quota: chỉ trừ khi có item hợp lệ.
- Backend suite vẫn xanh (xem Phase 7).

**Kết luận:** reviewer bắt **3 vấn đề thật** (#1,#2,#6) + 1 lỗ hổng test (#7); tôi **đối chiếu code**, xác nhận,
**sửa tận gốc** cả 3 + mở rộng mutation-test. Đúng tinh thần "review chéo → đối chiếu → sửa".
