# Plan — audit-v6-full

## Kiến trúc kiểm tra (phối hợp sub-agents)

```
Phase 0 (xong)  : baseline tests (247/247 BE, 73/73 FE) + GET sweep 53 endpoint + spec artifacts
                  + 3 sub-agent đọc song song: static-code / db-audit / design-gap
Phase 1         : tổng hợp phát hiện → phân loại P1/P2/P3 → quyết định fix
Phase 2         : DB perf: áp index được chứng minh + đo trước/sau (main agent thực thi)
Phase 3         : UI verify bằng browser thật (chrome-devtools + playwright):
                  - computed font = Be Vietnam Pro mọi trang
                  - design-system checklist (button/card/input/shadow/border)
                  - console 0 error; responsive 375px
Phase 4         : E2E chức năng ↔ API: CRUD (lesson/exercise/prompt/video) + AI
                  (ai-generate, assess, ai-grade, translate, fetch-youtube) + payment
Phase 5         : fix các lỗi P1/P2 tìm được (code + data), tái kiểm
Phase 6         : converge (soát spec↔tasks↔code) + REPORT.md + commit
```

## Nguyên tắc

- Sub-agent **chỉ đọc**; mọi ghi (SQL, code, commit) do main agent làm sau khi reconcile.
- Fix nhỏ, có bằng chứng tái kiểm; không redesign lại phần đã đạt ở audit-v5.
- Mỗi phát hiện gán ID F20+ (đánh số tiếp nối audit-v5 F1–F19).

## Rủi ro

| Rủi ro | Giảm thiểu |
|--------|-----------|
| AI endpoint chậm (Ollama model swap ~6s) | timeout 120s, chạy nền, không block UI sweep |
| Playwright headless từ chối mic | speaking E2E verify qua API + UI fail-soft message (đã biết từ v5) |
| Index mới làm chậm write | chỉ tạo index có query pattern thật + đo |
| Sub-agent trả rỗng (đã xảy ra v5) | timeout + main tự reconcile nếu cần |
