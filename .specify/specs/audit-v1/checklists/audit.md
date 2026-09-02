# Audit Requirements Quality Checklist — EngFlow V1

**Purpose:** Validate requirement quality của spec audit-v1 ("unit tests for English")
**Created:** 2026-09-01 | **Depth:** Standard | **Audience:** Reviewer

## Requirement Completeness

- [ ] CHK001 - Có yêu cầu định danh đầy đủ các nhóm endpoint cần smoke-test (auth/CRUD/AI/admin/payment)? [Completeness, Spec §R2]
- [ ] CHK002 - Có yêu cầu kiểm tra response shape cho từng API (`.data` vs plain)? [Completeness, Spec §R2.4]
- [ ] CHK003 - Danh sách trang UI cần verify được liệt kê cụ thể? [Completeness, Spec §R4.2]
- [ ] CHK004 - Yêu cầu a11y cơ bản (lang, focus-visible, skip-link, contrast) được liệt kê đủ 4 mục? [Completeness, Spec §R4.3]

## Requirement Clarity

- [ ] CHK005 - "Tối ưu hiệu năng" được định lượng bằng chỉ số đo trước/sao (p50, EXPLAIN, Lighthouse)? [Clarity, Spec §R3.5]
- [ ] CHK006 - Mức "Real 1 lần mỗi loại" của AI test được định nghĩa đủ hẹp để lặp lại? [Clarity, Clarifications]
- [ ] CHK007 - "Font Be Vietnam Pro toàn cục" có tiêu chí verify khách quan (computed style)? [Clarity, Spec §R4.1]

## Requirement Consistency

- [ ] CHK008 - Quy tắc "không tối ưu khi chưa đo" (P5) không mâu thuẫn với yêu cầu "tối ưu hiệu năng" (R3.4)? [Consistency, Spec §R3.4 vs Constitution P5]
- [ ] CHK009 - Giới hạn rebuild backend (downtime chấp nhận) nhất quán với "KHÔNG rebuild trừ khi sửa code"? [Consistency, Spec §Phạm vi]
- [ ] CHK010 - "Max 1 accent" của skill design vs hệ palette confetti của design system được ghi rõ ngoại lệ? [Consistency, Spec §R5.2]

## Acceptance Criteria Quality

- [ ] CHK011 - Baseline 196/73 test là tiêu chí pass/fail khách quan, đo được? [Measurability, Spec §AC1]
- [ ] CHK012 - "Console errors = 0" là chỉ số nhị phân kiểm chứng được qua MCP? [Measurability, Spec §AC4]

## Scenario Coverage

- [ ] CHK013 - Có yêu cầu xử lý khi AI model swap (~5.7s) hoặc sinh bài fail/partial? [Coverage, Edge Case]
- [ ] CHK014 - Có kịch bản khi DB query chậm phát hiện nhưng không đủ bằng chứng tối ưu? [Coverage, Exception Flow]
- [ ] CHK015 - Có yêu cầu rollback nếu thay đổi làm vỡ baseline? [Coverage, Recovery, Gap]

## Dependencies & Assumptions

- [ ] CHK016 - Giả định frontend container bind-mount (không cần rebuild frontend) được xác thực từ docker-compose? [Assumption, Spec §R4]
- [ ] CHK017 - Phụ thuộc Ollama host (host.docker.internal:11434) có health check trước khi test AI? [Dependency, Gap]
