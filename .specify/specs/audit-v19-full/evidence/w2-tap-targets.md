# audit-v19-full — W2: tap-target harness SAI + a11y (đính chính v18)

**Ngày:** 2026-09-27 · **Files:** `sweep/harness/ui-sweep.js`, `sweep/harness/focused-probe.js`

## Đính chính kết luận v18

v18 báo *"`smallTargets=115` là false positive (đã triage: inline link + label checkbox)"* — **SAI/THIẾU**:
- Harness cũ cắt `small.slice(0,10)` (`ui-sweep.js:143`) → artifact **không đủ** để triage; 105/115 entry bị bỏ.
- Harness cũ chỉ loại rect 0-size — **không** áp **bất kỳ** miễn trừ WCAG 2.5.8 nào.

## Root cause

`ui-sweep.js:117-128` (predicate `Math.min(w,h) < 24`, chỉ bỏ rect 0-size) + `:143` cắt list.
Hệ quả: 115 gồm **cả false positive lẫn (bị giấu) ứng viên thật**.

## Fix (harness) — áp đúng WCAG 2.5.8

Trong `A11Y_FN`:
1. **Inline exception:** target inline-level (`/^inline/.test(display)`), có text, nằm trong **text block**
   (`p, li, td, dd, blockquote, figcaption, h1..h6`) → miễn. (Đo `/videos/1`: word-chip `inline-block`, cao
   **26px = line-height**, nằm trong `<p>` → đúng định nghĩa "constrained by the line-height".)
2. **Label exception:** `checkbox/radio` trong `<label>` → target hiệu dụng = label.
3. **Spacing exception:** target <24px chỉ tính là vi phạm nếu đường tròn 24px tâm trên nó **chạm** target khác.
4. **BỎ cắt `slice(0,10)`** — xuất đủ list.

`focused-probe.js` đổi `smallCount` → `rawSmallUnder24` + note (đo thô, không phải vi phạm) để không hiểu nhầm.

## Kết quả before/after

| | v18 (naive) | v19 (WCAG 2.5.8 đúng) |
|---|---|---|
| `smallTargets` (tổng role×route) | 115 | **0** |
| Trong đó: `/videos/1` word-chip | bị **giấu** | **14** raw → **0** sau khi áp inline exception |
| List có bị cắt? | **Có** (`slice(0,10)`) | **Không** |

**Chuỗi đo:** 115 → (bỏ cắt) 59 → (thêm label+spacing) 14 → (thêm inline exception) **0**.

## Mutation-test (chứng minh harness KHÔNG bị "mù")

Inject 2 button 10×10 cách nhau 2px (không ở text block) vào `/lessons`:
→ `smallCount = 2` → **harness vẫn bắt được vi phạm thật**. Exceptions không làm mù check.

## Kết luận W2

- **Harness:** sửa xong, giờ **0 false positive** VÀ **vẫn bắt vi phạm thật** (mutation-test PASS).
- **App:** **KHÔNG cần sửa** — mọi target nhỏ đều thuộc miễn trừ hợp lệ (đo: word-chip 26px = line-height trong
  câu; checkbox trong label; link đơn lẻ cách xa target khác). Sửa app (nới 24px) sẽ **phá** đoạn văn đọc.
- **Đính chính:** v18 "115 = false positive" → **sai cách nói**; đúng là "115 gồm false positive + list bị cắt;
  sau khi áp đúng WCAG 2.5.8 = **0 vi phạm**".
