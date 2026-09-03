# Spec: audit-v5-full — Audit vòng 3: root-cause toàn bộ lỗi còn sót + Be Vietnam Pro tuyệt đối

**Feature dir:** `.specify/specs/audit-v5-full/` · **Ngày:** 2026-09-03 · **Baseline:** commit `f33b728` (main)
**Tiền đề:** audit-v4-full (REPORT.md) — 67/67 API pass, nhưng còn lỗ hổng CHƯA được phát hiện ở v4.

## 1. Vấn đề

Người dùng yêu cầu: (1) quét toàn bộ codebase + DB; (2) test backend bằng cách chạy **tất cả** API
kết hợp tương tác UI thật cho từng chức năng, soi kỹ CRUD + AI; (3) kiểm tra DB trong docker + tối ưu
hiệu năng; (4) kiểm tra frontend bằng **cả hai** chrome-devtools MCP và playwright MCP, đối chiếu
design-system "Playful Geometric"; (5) thay **100%** font bằng Be Vietnam Pro (kể cả JetBrains Mono);
(6) vá lỗ hổng của chính prompt yêu cầu; (7) chạy lại vòng loop-test toàn diện hơn, sửa **tận gốc**;
(8) báo cáo chi tiết tiếng Việt. Audit-v4 bỏ sót các lớp lỗi chỉ lộ ra khi chạy E2E thật:
cấu hình `.env` hỏng, `@PreAuthorize` trơ, dữ liệu seed không khớp contract frontend.

## 2. Phát hiện lỗ hổng mới (root-cause findings)

| # | Lỗ hổng | Root cause | Bằng chứng |
|---|---------|-----------|------------|
| F1 | SePay polling chết, 401 circuit-break | `SEPAY_API_TOKEN` trong `.env` bị nối chuỗi `DB_PASSWORD` (dài 91 ký tự thay vì 64) | `pollingEnabled=false` → `true` sau sửa |
| F2 | Mọi `@PreAuthorize` không có hiệu lực | Thiếu `@EnableMethodSecurity` trong `SecurityConfig` | URL rules che mất; test 403 pass giả |
| F3 | Free user upload speaking → 500 | `ResponseStatusException` không có handler → rơi vào generic 500 | Thêm handler → 403 đúng |
| F4 | Rubric AI sai model + URL chết | `.env` trỏ `qwen2.5:1.5b` (fail rubric VN theo đo lường) + `localhost:11434` bất khả thi từ container | `host.docker.internal` + `3b` |
| F5 | 55 class `bg-danger/10`, `text-warning`... resolve rỗng | tailwind.config thiếu token danger/warning/success | Thêm 3 màu |
| F6 | Font phụ JetBrains Mono + Plus Jakarta Sans còn sót | `design-system.css` pre block + `index.html` Google Fonts link | Xóa, chỉ còn BVP |
| F7 | Token drift `--geo-muted`/`--geo-border` | Giá trị cũ `#F5F0E6`/`#E5DECF` lệch design prompt | Về `#F1F5F9`/`#E2E8F0` |
| F8 | MATCHING render 2 cột trống | 148/481 dòng seed có options không đúng contract `left\|right` (68 rỗng + 80 malformed) | Fallback text input |
| F9 | FILL_BLANK/LISTENING hiển thị chữ A/B/C/D vô nghĩa | ~230 dòng seed options là placeholder `["A","B","C","D"]`, đáp án thật nằm ở `correct_answer` | Lọc placeholder |
| F10 | Flashcard dùng gradient off-palette (sky/emerald/fuchsia...) | Vi phạm nguyên tắc màu phẳng Playful Geometric | Về 4 token + fg |
| F11 | `show-sql=true` trong prod container | ~7.5k dòng log/giờ, overhead string concat mỗi query | Env-overridable, default false |
| F12 | N+1 trong LessonSnapshotService | Query blocks theo từng section | 1 query/lesson |
| F13 | SQL Server ăn hết RAM host (7.7GB) | Không có memory cap; env `MSSQL_MAX_MEMORY_PER_PROCESS_MB` image không nhận | `sp_configure` qua init-db.sql |
| F14 | `bg-pink-500` trong AdminLayout | Off-token | `bg-accent` |

## 3. Phạm vi

**In scope:** mọi mục ở §1–§2; E2E thật qua UI cho: speaking (mic), video shadowing (grade),
premium checkout (webhook funnel), deck games (6 mode), lesson exercises (5 type), admin CRUD
(7 trang + modal create/edit/delete), AI generate-async, dictionary, leaderboard, profile,
register/forgot-password/login-sai.
**Out of scope:** refactor kiến trúc lớn (JWT refresh, split service), viết lại data seed
(hàng chục nghìn dòng — chỉ guard phía UI + ghi nhận), CI/CD.

## 4. Yêu cầu kiểm chứng (DoD)

- [ ] Backend `mvnw test` xanh (baseline 221)
- [ ] Frontend `vitest` xanh (baseline 73/14 files) + `vite build` sạch
- [ ] API sweep 67/67 pass trên container rebuilt
- [ ] Computed font mọi trang public = chỉ "Be Vietnam Pro"
- [ ] Không còn class màu off-token nào resolve rỗng trong `frontend/src`
- [ ] E2E: mỗi chức năng §3 có bằng chứng runtime (snapshot/log/DB row)
- [ ] Perf: số liệu before/after cho lesson list, dictionary cache, log volume (P5)
- [ ] CLS đo được; nếu >0.1 ghi rõ nguyên nhân còn lại trong REPORT
- [ ] Commit Conventional Commits, author giahuy1411, `.env` không vào git

## 5. Rủi ro

- Guard UI che data xấu nhưng không sửa data → chấp nhận (out of scope seed rewrite), ghi chú REPORT.
- `min-height` footer có thể lệch trên mobile → screenshot 375px trước khi đóng.
