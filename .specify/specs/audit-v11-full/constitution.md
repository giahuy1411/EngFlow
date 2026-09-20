# Constitution compliance check — audit-v11-full

**Constitution:** `.specify/memory/constitution.md` v1.0.1 (ratified 2026-09-01, amended 2026-09-03)
**Checked:** 2026-09-20 (+07) · **Branch:** `audit-streak-review` · **Checkpoint:** `dae9667`

Every principle is checked against this audit's planned work. `AMEND` means the principle text is
stale and must be re-measured, not that the principle is wrong.

| # | Principle | Status | Evidence / action |
|---|---|---|---|
| **P1** | Baseline xanh là tiền đề — numbers must be re-measured each audit | **AMEND (numbers only)** | Constitution says backend **221**, frontend **73/14 files** (audit-v3 numbers). audit-v10 recorded **470** and **106/21**. v11 measures both from the run log and writes the real value back. The *rule* (green before and after; count from the run log, never `target/surefire-reports` XML) is upheld unchanged. |
| **P2** | Kiến trúc phân lớp bất biến (Controller → Service → Repository; `<script setup>`; services via `api`) | **COMPLIANT** | No architectural change is planned. v11 reads and tests these layers; it does not restructure them. |
| **P3** | Schema do Hibernate quản lý; Flyway disabled; đổi schema = SQL trực tiếp + entity + verify on `engflow-sqlserver` | **COMPLIANT** | Verified live: `study_policy`=1 row (`effective_from=2026-09-20`), `study_days`=0 rows, FK `fk_study_days_user`=1, UQ `uq_study_days_user_date`=1. No migration file is written by this audit. |
| **P4** | AI local là ràng buộc sản phẩm (Ollama 1.5b/3b, Whisper :9002) | **COMPLIANT** | AI checks probe the local Ollama/Whisper/TTS path only. No new cloud-AI dependency is proposed. |
| **P5** | Hiệu năng được đo, không bịa — mọi tuyên bố tối ưu kèm số trước/sau | **COMPLIANT — central to v11** | Every performance claim in the report carries before/after numbers (API latency, bundle size, query logical reads). No optimisation is applied without a prior measurement. |
| **P6** | Design System "Playful Geometric" + font **Be Vietnam Pro** duy nhất; cấm Outfit/Plus Jakarta | **COMPLIANT — and the font requirement is ALREADY MET** | Verified in source: `index.html` loads only `Be+Vietnam+Pro:wght@400;500;600;700;800;900`; `tailwind.config.js` maps `sans`/`heading`/`mono` → BVP; `--geo-font` = BVP; zero occurrences of Outfit/Plus Jakarta in `frontend/src`. **The user's instruction "replace all fonts with Be Vietnam Pro" requires no work — it is already done.** v11 verifies it holds, it does not re-do it. |
| **P7** | UI text tiếng Việt; a11y bắt buộc: focus-visible, skip-link, contrast AA, `prefers-reduced-motion` | **VIOLATION FOUND — this is a v11 finding** | **Contrast AA is not met** for the design system's own accent colours used as text. Measured live in the DOM at `/profile`: `text-secondary` (#F472B6) on white = **2.65:1** where WCAG 1.4.3 requires 3:1 (large) / 4.5:1 (small). Static audit of 11 real usages: 7 FAIL. Full detail in `findings.json`. |
| **P8** | Mỗi thay đổi có bằng chứng runtime — API thật trên :8080 + UI thật qua MCP | **COMPLIANT — and v11 closes a gap v10 had** | audit-v10's `REPORT.md` §2 records that **chrome-devtools MCP and Playwright MCP were unavailable** all session; it substituted `playwright-core` driving Edge/Brave. **In this session both MCPs are live** (verified: `list_pages` → OK, `browser_navigate` → OK). v11 therefore satisfies P8 more literally than v10 could. |

## Governance

- **Compliance check is per-session:** performed above, before any work.
- **Conflict resolution:** AGENTS.md governs operations; the constitution decides conflicts. No
  conflict arose: AGENTS.md's design-system notes and P6 agree.
- **Complexity budget:** v11 adds no abstraction, no dependency, no feature. It adds tests,
  measurements and evidence. The one code change it proposes (contrast tokens) is a token-value
  correction, not new machinery.

## Amendments required

**P1** — numbers only, after measurement: backend baseline `221 → <measured>`, frontend
`73/14 → <measured>`. The Sync Impact Report must record the old and new values. No principle text
changes. Bump **PATCH** (`1.0.1 → 1.0.2`), because the rule is unchanged and only the recorded
measurement moves.

**P6** — no amendment. The font requirement is already satisfied; v11 records the verification.
