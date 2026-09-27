# audit-v18-full — findings

Quy ước: `F-18-NN`. Mỗi finding: mô tả · bằng chứng · root cause · fix · bằng chứng pass · test hồi quy ·
**probe thứ 2 độc lập**. Trạng thái: `FIXED` · `OPEN` · `BLOCKED` · `DEFERRED` · `CLOSED (probe SAI)` · `N-A`.

---

## F-18-01 — Harness hardcode revision Chromium → 2 probe fail launch — **LOW (harness)** — `FIXED`

**Bằng chứng:** `sweep/harness/danger-tint.js:48` và `sweep/harness/focused-probe.js:63` hardcode
`C:/Users/ASUS/AppData/Local/ms-playwright/chromium-1237/chrome-win64/chrome.exe`; bundle cài đặt là
**`chromium-1234`** (đo: `ls ms-playwright/` = `chromium-1234`, `chromium_headless_shell-1234`).
→ `browserType.launch: Failed to launch chromium because executable doesn't exist at ...chromium-1237...`
Cả 2 probe **EXIT=1**.

**Root cause:** revision Chromium hardcode ở nhiều probe; máy đổi revision → vỡ (lớp lỗi lặp lại giữa các vòng).

**Fix (tận gốc):** thêm **`resolveChromium()`** vào `sweep/v8/ui/lib.js` (đọc `playwright-core` registry →
quét `ms-playwright/chromium-*` → Brave fallback → null). `ui-sweep.js`, `danger-tint.js`, `focused-probe.js`
dùng chung. Sau review chéo, mở rộng fix sang **`f1302-a1-a11y.js`, `f1302-a1-blocks-live.js`,
`f1320-api-redirect-live.js`** (còn hardcode 1237, tuy guarded nên không vỡ — sửa cho sạch).

**Bằng chứng pass (probe 2 độc lập):**
- `ui-sweep.js` in `browser executable: ...chromium-1234...` → chạy 0 fail.
- `danger-tint.js` + `focused-probe.js` + 3 probe còn lại: **EXIT=0** (trước EXIT=1 / guarded).
- `grep -rn "chromium-1237" sweep/` → chỉ còn **1 comment** giải thích trong `resolveChromium()`.

**Test hồi quy:** `assert-harness.js` ALL CLEAN 7/7 (không có check cho revision, nhưng harness chạy thật là
probe mạnh nhất). Không cần unit test cho harness.

---

## F-18-02 — Demo doc mô tả trần chờ từ điển 6s CŨ — **LOW (docs)** — `FIXED`

**Bằng chứng:** `docs/demo-engflow-4-chuc-nang.md` §4.1/§4.3 viết *"app chờ tối đa 6 giây rồi báo thử lại"*.
Code (`frontend/src/services/vocabularyService.js:143-144`): `DICT_BUDGET_MS=6000` là **ngưỡng MỀM**
(chỉ báo "đang tra"), `DICT_TOTAL_MS=45000` mới là **trần cứng**; request **vẫn chạy** giữa 6s–45s. Hành vi này
đổi ở audit-v17 (remove-limits round) nhưng doc chưa theo.

**Root cause:** doc không cập nhật khi v17 gỡ "fail ở 6s".

**Fix:** sửa §4.1 + §4.3: "6 giây là ngưỡng mềm (hiện 'đang tra'), trần cứng 45 giây; dictionary là nguồn duy nhất".

**Bằng chứng pass (probe 2):** `sed` lại §4.1/§4.3 xác nhận đã sửa; `grep "chờ tối đa 6 giây" docs/` → 0 hit.

---

## F-18-03 — (RÚT LẠI) demo doc file:line `StudyActivityService` — **CLOSED (probe SAI)**

**Báo cáo ban đầu:** tưởng doc ghi `:200`/`:122` nhưng code ở `101/106`/`114`.
**Đối chiếu lại:** `grep -n "private int currentStreak(List<LocalDate>"` → **đúng dòng 200**;
`grep -n "public Map<Long, Integer> currentStreaks(Collection"` → **đúng dòng 122**. Grep đầu của tôi khớp
**overload khác** (`currentStreak(Long)` ở 106). → **Doc ĐÚNG**, finding **rút lại** (kỷ luật V4: kiểm chứng, không suy đoán).

---

## Không phải finding (ghi nhận)

| # | Quan sát | Kết luận |
|---|---|---|
| N1 | `smallTargets=115` trong ui-sweep | **Không phải vi phạm** — 52 inline `<a>` (WCAG 2.5.8 inline exception) + 21 checkbox trong `<label>` 80×16 (spacing exception). Đo trực tiếp bằng MCP xác nhận. |
| N2 | 1 console "error" ở `/videos/1` (PW) | **Third-party** — `youtube.com/.../base.js` "compute-pressure policy". Khớp `isThirdPartyConsoleNoise`. |
| N3 | AI `qwen2.5:1.5b` trả gloss tiếng Trung cho "ocean life" | **Hạn chế model nhỏ**, không phải bug code. |
| N4 | `exercise_attempts` 33 (không phải 36) | Scoped 2 tài khoản probe (F-17-21); 36 = cả bảng là SAI. |

## Tổng kết

**1 finding thật FIXED (F-18-01, harness)**, 1 doc-drift FIXED (F-18-02), 1 rút lại (F-18-03).
Vòng 2 = **0 finding mới** → hội tụ. **0 regression** (backend 537/0, frontend 194/1).
