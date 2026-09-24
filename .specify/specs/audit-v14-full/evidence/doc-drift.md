# audit-v14-full — T6.6 doc-drift (mỗi claim đo lại, không sửa theo trí nhớ)

**Ngày:** 2026-09-25 (+07) · **Nguồn đo:** `npx vitest run`, `find`, `INFORMATION_SCHEMA.TABLES`, grep.

| # | Tài liệu | Claim cũ | Đo thật (phiên này) | Kết luận |
|---|---|---|---|---|
| D-1 | `README.md:76,130` | "Vitest (193 tests / 31 files)" | **194 passed / 1 skipped (31 file)** | **DRIFT** — +1 test trên cây hiện tại |
| D-2 | `AGENTS.md:15` | frontend baseline 193 | **194** | **DRIFT** (cùng nguồn D-1) |
| D-3 | `README.md:101` | "47 .vue files" (dưới `views/`) | **48** | **DRIFT** — +1 view |
| D-4 | `AGENTS.md:57` | "9/367 bài LISTENING thiếu `audio_url`" | **0/358** | **DRIFT** — đã cải thiện (xem `db-audit.md`) |
| D-5 | `.specify/feature.json` | trỏ `specs/audit-v13-full` | v14 đang chạy | **DRIFT** — con trỏ phiên |
| D-6 | `README.md:127` | "Backend tests (523 tests)" | **523** | ✅ khớp |
| D-7 | `README.md:95` | "26 REST controllers" | **26** | ✅ khớp |
| D-8 | `README.md:177` | "21 tables" | **21** (22 trừ `sysdiagrams`) | ✅ khớp |
| D-9 | `README.md:11,64` | "Spring Boot 4.0.6, Java 25" | pom: `4.0.6`, `java.version=25` | ✅ khớp |

## Ghi chú

- D-1/D-2: con số frontend **tăng** (không phải drift do mất mát) — cây hiện có thêm 1 test so với
  thời điểm v13 chốt. Sửa doc cho khớp **đo của phiên này**.
- D-3: đếm `find frontend/src/views -name '*.vue' | wc -l` = 48. README ghi 47.
- D-4: đã ghi ở `db-audit.md` §T1.7; sửa AGENTS.md.
- D-5: `.specify/feature.json` là con trỏ phiên — cập nhật sang `audit-v14-full`.
- **Nguyên tắc:** chỉ sửa doc khi có số đo phiên này; không sửa theo trí nhớ.
