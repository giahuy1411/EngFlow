# audit-v18-full — Phase 8: docs drift (đối chiếu thực tế, sửa cho khớp)

**Ngày:** 2026-09-27 · Mọi số **đo phiên này**.

## Bảng cập nhật

| File | Trước | Sau | Lý do |
|---|---|---|---|
| `README.md` | `178 tests / 29 files` (×2) | **`194 tests / 32 files`** | baseline v18 |
| `README.md` | `515 tests` | **`537 tests`** | baseline v18 |
| `AGENTS.md` | backend 520 | **537** | baseline v18; thêm cảnh báo `mvnw -q` nuốt dòng tổng kết |
| `AGENTS.md` | frontend 191/1 | **194/1 (32)** | baseline v18 |
| `AGENTS.md` | parity "(2026-09-25, sau audit-v15)" | **"(2026-09-27, sau audit-v18)"** | giá trị không đổi, cập nhật nhãn |
| `AGENTS.md` | harness default `audit-v17-full` | **`audit-v18-full`** | namespace vòng này |
| `AGENTS.md` | harness comment cũ | thêm **"KHÔNG hardcode revision Chromium → `H.resolveChromium()`"** | F-18-01 |
| `.specify/memory/constitution.md` | v1.0.2, P1 = 520/191 | **v1.0.3, P1 = 537/194** | PATCH bump |
| `.specify/feature.json` | `specs/audit-v17-full` | **`specs/audit-v18-full`** | con trỏ vòng |
| `.gitignore` | — | thêm whitelist `audit-v18-full/evidence/{*.log,*.json,*.md,*.txt,shots/**}` | REPORT trích log theo tên |
| `docs/demo-engflow-4-chuc-nang.md` §4.1 | "2 tầng: kho đệm → từ điển thẳng"; "chờ tối đa 6 giây rồi báo thử lại" | **"proxy → direct; dictionary là nguồn duy nhất; 6s MỀM, cứng 45s, vẫn chờ"** | F-18-02 (v17 đổi hành vi, doc chưa theo) |
| `docs/demo-engflow-4-chuc-nang.md` §4.3 | "app chờ tối đa 6 giây rồi báo thử lại" | **"chỉ hiện 'đang tra' ở 6 giây, vẫn chờ tới 45s"** | F-18-02 |

## Số đã kiểm chứng (không cần sửa)

| Claim | Giá trị | Nguồn |
|---|---|---|
| Controller | 25 | `find controller -name '*.java'` = 25 |
| Views `.vue` | 46 | `find views -name '*.vue'` = 46 |
| UI primitives | 17 | `find components/ui -name '*.vue'` = 17 |
| Bảng DB | 18 | README §Database Schema (khớp entity) |

## File:line demo doc — đã `sed`/`grep` đối chiếu

`UserService.java:141` ✅ · `:165` ✅ · `JwtTokenProvider.java:31` ✅ · `SecurityConfig.java:76` ✅ ·
`ExerciseService.java:232` ✅ · `:473` ✅ · `Login.vue:71` ✅ · `Lessons.vue:180` ✅ ·
`LessonExerciseTab.vue:295` ✅ · `StudyActivityService.java:200` ✅ · `:122` ✅ · `Profile.vue:84` ✅ ·
`VocabularyController` search ✅.
→ **Tất cả tồn tại và nói đúng.** (Finding F-18-03 "file:line lệch" bị **rút lại** — grep đầu của tôi khớp
overload khác.)

## Prompt doc

`prompt-rewritten-v18.md` tạo mới (kế thừa v17 + đo lại 4 lỗ hổng + ghi rõ "thay font = verify" + optional gap).
