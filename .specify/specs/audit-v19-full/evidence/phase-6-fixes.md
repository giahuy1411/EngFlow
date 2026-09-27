# audit-v19-full — Phase 6: fix + regression + review chéo

**Ngày:** 2026-09-27

## 6.1 Findings đã fix

| ID | Mức | Vấn đề | Fix | Bằng chứng |
|---|---|---|---|---|
| **F-19-01** | **MED (AI)** | `generate-vocab`/`enrich-word` trả gloss **tiếng Trung** (model 1.5b zh-centric); prompt không nêu ngôn ngữ; 0 guard CJK | Prompt nêu ngôn ngữ từng field + "NO Chinese characters"; **guard `containsCjk`** loại/từ chối item CJK | W1: 0 CJK ×15 live; test 4/4 mutation-tested |
| **F-19-02** | LOW (contract) | `enrichWord` gặp CJK → ném `IllegalStateException` → **500** | Ném `BadRequestException` (→**400**), catch giữ nguyên 400 | live: `resilient`→400, `abundant/ocean`→200 |
| **F-19-03** | LOW (UX) | `generate-vocab` loại hết item → UI hiện **trống im lặng** | UI hiện thông báo "AI chưa trả về từ hợp lệ…" khi `length===0` | `AiVocabGenerator.vue` |
| **F-19-04** | LOW (harness) | `smallTargets` naive + cắt `slice(0,10)` → v18 triage SAI (giấu 12 button `/videos/1`) | Áp WCAG 2.5.8 (inline/label/spacing exception) + **bỏ cắt** | 115→0; mutation-test PASS (vẫn bắt vi phạm thật) |
| **F-19-05** | MED (perf) | Admin exercises scan cả bảng (page 1341 + count 1348 reads) | `IX_exercises_order_id (order_index, exercise_id)` | page **1341→86** reads; endpoint 42.8→26.6ms |

## 6.2 Review chéo đối kháng

Xem `evidence/review-v19.md` (reviewer subagent độc lập). Reviewer soi: `containsCjk` false-positive? `enrichWord`
throw→500? V005 index dùng được + không đổi kết quả? ui-sweep exception có làm mù check? — tôi **đối chiếu lại code**.

## 6.3 Mutation-test

- **W1 guard:** tắt `containsCjk` (luôn false) → `AiVocabServiceLanguageGuardTest` **FAIL 1** → test thật sự bảo vệ;
  khôi phục → 4/4 PASS.
- **W2 harness:** inject 2 button 10×10 cạnh nhau → harness **bắt được** (smallCount=2) → exception không làm mù.

## 6.4 Suite xanh

Backend **541 / 0 / 0 / 11** (537 + 4 W1) · Frontend **194 / 1 (32)** — 0 regression.
