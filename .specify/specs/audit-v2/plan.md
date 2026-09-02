# Plan — Audit V2 (Deep Audit EngFlow)

Ngày: 2026-09-01 | Cha: audit-v1 plan | Constitution: đã PASS ở v1 (P1-P8 vẫn hiệu lực)

## Technical Context

- Hệ thống: đã proven ở v1 — 21 controllers, 39 services, 21 entities, SQL Server
  (sqlcmd qua docker exec, password regex từ .env), Ollama 1.5b/3b, Whisper :9002,
  frontend bind-mount (sửa host = hot-reload, không cần rebuild), backend rebuild
  `docker compose up -d --build backend`.
- Token: login admin@gmail.com/user@gmail.com / 123456 → `data.data.token || data.token`;
  token TTL ngắn → refresh trước mỗi cụm test.
- Tools: pwsh scripts (strip non-ASCII + BOM nếu có tiếng Việt), Node cho multipart,
  chrome-devtools-mcp (console/network/Lighthouse/evaluate), playwright-mcp (journey).

## Phases (kế hoạch thực thi)

### Phase A — Bản đồ hóa (không đoán)
1. Grep toàn bộ `@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PatchMapping` từ 21 controllers → bảng endpoint đầy đủ (method, path, params, role hint).
2. Trích routes từ `frontend/src/router` → bảng route đầy đủ (path, name, role/meta).

### Phase B — API smoke + negative matrix
3. Happy path mọi endpoint (dùng bảng Phase A làm checklist) — ghi status + shape.
4. Negative matrix: mỗi nhóm API ≥1 case 401/403/400/404.
5. CRUD vòng đời từng entity chính (create→read→update→delete, verify DB sau mỗi bước).

### Phase C — AI sâu
6. Sinh thật từng exercise type (5 loại) — schema verify từng loại.
7. Ai-vocab multi-topic; speaking full (WAV có nội dung thật → transcript thật → rubric
   3b → scoreTotal); batch nhỏ 2-3 lessons qua Redis progress.

### Phase D — DB deep-audit
8. Orphan toàn FK; data quality (rác/duplicates/JSON hỏng); DMV missing-index;
   query stats top; EXPLAIN nếu cần; tạo index chỉ khi DMV+EXPLAIN đồng ý (P5).

### Phase E — Frontend toàn route
9. Mọi route render + console=0 (2 vai: student/admin), design-system verify theo
   loại component, form validation, keyboard/focus, responsive 375/768, Lighthouse
   Home + 2 trang, bundle size.
10. Findings → fix (theo clarify) + test + verify runtime.

### Phase F — Converge + re-test + báo cáo
11. Re-run 197 + 73; speckit-analyze; speckit-converge (append nếu thiếu);
    dọn data test; taskstoissues nếu còn việc tồn; báo cáo tiếng Việt.

## Risks & Mitigations
- Token hết hạn giữa chừng → script login-refresh tiện dụng, kiểm tra 401-response
  trước khi kết luận lỗi endpoint.
- Ollama model swap chậm (3b cho rubric) → poll dài, không timeout vội.
- Whisper không transcript im lặng → dùng WAV có speech thật (TTS sẵn hoặc generate).
- jsdom mock SpeechSynthesis (AGENTS.md) — không break test khi chạm listening UI.
- UI state sau login tự redirect về /lessons → điều hướng bằng URL trực tiếp.

## Constitution Check (P1-P8)
P1 giữ baseline xanh sau mỗi thay đổi — yes, Phase F + mỗi fix.
P5 tối ưu có bằng chứng — DMV + EXPLAIN là bằng chứng bắt buộc.
P6 font Be Vietnam Pro — verify không đổi token.
P8 runtime evidence — mọi kết luận kèm số liệu.
