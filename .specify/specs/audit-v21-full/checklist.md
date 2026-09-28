# audit-v21-full — checklist chất lượng requirements

> Checklist = "unit test cho việc viết yêu cầu" — kiểm tra yêu cầu có rõ/ràng/đủ, KHÔNG phải kiểm code.

## Chất lượng spec

- [x] Mỗi objective có số/tiêu chí đo được (O1: 44+33 file; O2: 121 endpoint; O3: 5 viewport × 3 role)
- [x] Requirement phân biệt MUST (R3, R4, R5) vs SHOULD
- [x] Out of scope liệt kê rõ (font, design system, push/PR, index LIKE)
- [x] Success criteria kiểm chứng được bằng command cụ thể
- [x] Không có "TBD"/"TODO" trong spec
- [x] Mọi tuyên bố "đã xong" (font/design system) có bằng chứng file:dòng

## Chất lượng kịch bản test

- [x] Mỗi vùng chính có happy + negative/authz + edge
- [x] Mỗi kịch bản nêu công cụ cụ thể (harness nào / MCP nào / sqlcmd)
- [x] Mỗi kịch bản nêu kỳ vọng có số + artifact làm bằng chứng
- [x] Chiều mới (security/Lighthouse/bundle/index) có kịch bản riêng
- [x] Tái sử dụng harness có sẵn, không phát minh trùng

## Chất lượng an toàn

- [x] Guard chống comment phá build (Javadoc trước annotation, full test mỗi lô)
- [x] Guard chống citation drift (doc_citation_remap.py)
- [x] Guard chống parity pollution (cleanup + assert trong cùng run)
- [x] Guard chống xoá nhầm (verify-tham-chiếu trước khi xoá + danh sách KEEP)
- [x] Không commit secret/PII/fixture

## Điều kiện tiên quyết

- [x] Runtime 8 container up
- [x] Baseline đo lại khớp (541/194/177.75kB/parity)
- [x] assert-harness ALL CLEAN (sau khi sửa default v21)
- [x] Prompt flaws đo lại có bằng chứng
