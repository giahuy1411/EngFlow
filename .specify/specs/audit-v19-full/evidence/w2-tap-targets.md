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
| Trong đó: `/videos/1` word-chip | bị **giấu** | **12** raw (focused-probe) → **0** sau khi áp inline exception |
| List có bị cắt? | **Có** (`slice(0,10)`) | **Không** |

**Chuỗi đo:** 115 → (bỏ cắt list) 59 → (thêm label + spacing exception) **0**.

## Mutation-test (chứng minh harness KHÔNG bị "mù") — 2 ca

Reviewer v19 bắt lỗ hổng: test cũ chỉ inject control **ngoài** text block, không phủ ca "control nhỏ **TRONG** `<p>`".
Đã làm lại **2 ca**:
1. 2 button 10×10 cách 2px trong flex row (ngoài text block) → **bắt được** (smallCount≥2).
2. 2 button 10×10 **cao 30px** (taller than line-height 12px) cạnh nhau **trong `<p>`** → **bắt được**
   (smallCount=2) → chứng minh **inline exception KHÔNG quá rộng** (control không bị line-height ràng buộc vẫn tính).

→ Exceptions (inline/label/spacing) **không** làm mù check.

## Đính chính sau review (reviewer #6)

`/^inline/` + `td/li` ban đầu **miễn quá rộng** — control `inline-flex` (như `.app-btn`, `<router-link>` trong
`<td>` của `AdminVideoLessons.vue:110`) bị miễn oan. **Sửa:** thêm điều kiện **`height <= line-height`** — đúng
định nghĩa WCAG 2.5.8 "constrained by the line-height". Sau siết: `smallTargets` vẫn **0** (không false positive)
và mutation-test ca 2 chứng minh vẫn bắt control thật.

## Kết luận W2

- **Harness:** sửa xong, giờ **0 false positive** VÀ **vẫn bắt vi phạm thật** (mutation-test PASS).
- **App:** **KHÔNG cần sửa** — mọi target nhỏ đều thuộc miễn trừ hợp lệ (đo: word-chip 26px = line-height trong
  câu; checkbox trong label; link đơn lẻ cách xa target khác). Sửa app (nới 24px) sẽ **phá** đoạn văn đọc.
- **Đính chính:** v18 "115 = false positive" → **sai cách nói**; đúng là "115 gồm false positive + list bị cắt;
  sau khi áp đúng WCAG 2.5.8 = **0 vi phạm**".
