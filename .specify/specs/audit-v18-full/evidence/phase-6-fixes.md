# audit-v18-full — Phase 6: fix + targeted probes + review chéo

**Ngày:** 2026-09-27

## 6.1 Findings đã fix

### F-18-01 — Harness hardcode chromium revision → 2 probe launch fail — **LOW (harness)** — `FIXED`

**Bằng chứng:** `danger-tint.js:48` và `focused-probe.js:63` hardcode
`chromium-1237/chrome-win64/chrome.exe`; bundle cài đặt là **`chromium-1234`** →
`browserType.launch: Failed to launch chromium because executable doesn't exist`. Cả 2 probe EXIT=1.

**Root cause:** revision Chromium hardcode ở nhiều probe; đổi revision → vỡ (lớp lỗi lặp lại giữa các vòng).

**Fix (tận gốc):** thêm **`resolveChromium()`** vào `sweep/v8/ui/lib.js` — đọc `playwright-core` registry →
quét `ms-playwright/chromium-*` → Brave fallback. `ui-sweep.js`, `danger-tint.js`, `focused-probe.js` dùng chung.

**Bằng chứng pass (probe 2 độc lập):**
- `ui-sweep.js` in `browser executable: ...chromium-1234...` và chạy lại 0 fail.
- `danger-tint.js` + `focused-probe.js` chạy lại **EXIT=0** (trước EXIT=1).

## 6.2 Targeted probes (đều tự dọn)

| Probe | Kết quả |
|---|---|
| `g6-sepay-signed.py` | **PASS** — valid HMAC → SUCCESS; bad sig → rejected; **stale/replay sig → rejected**; restore premium |
| `g7-speaking-real-audio.py` | **PASS** — TTS WAV thật → COMPLETED, transcript 181 ký tự, score 9.7; dọn row+MinIO+study_days |
| `g8-speaking-human-audio.py` | **PASS** — audio người thật → transcript recall **0.95**; fixture gốc nguyên vẹn |
| `l2-negative-cache-proof.py` | **PASS** — 404 cache (2nd không gọi upstream); 500 KHÔNG cache; positive cache |

## 6.3 ddl-auto=validate drill (T2.6)

`docker compose run --rm --no-deps -T -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate backend`:
`Started EngflowApplication in 14.737 seconds` — **0 ERROR, 0 schema-warning**. Entity ↔ DB khớp.
(WARN "Dictionary proxy failed for 'and'/'a'" là warmup timeout upstream — không phải lỗi schema.)
Container `engflow-backend-run-*` đã stop theo **tên** (không dùng ancestor filter — bẫy đã biết).

## 6.4 Review chéo đối kháng

Xem `evidence/review-v18.md`. Reviewer (subagent độc lập) soi fix F-18-01 + mọi thay đổi vòng này.

## 6.5 Suite xanh 0 regression

Backend **537/0/0/11** · Frontend **194/1 (32)** · build **177.75 kB** — giữ nguyên baseline (đo lại ở Phase 7).
