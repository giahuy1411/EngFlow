# Checklist — audit-v4-full

## Completeness
- [x] Mọi mục còn tồn của audit-v3 (§4) được nhắc tới trong spec (S4, S5 + ghi nhận S3-out)
- [x] Yêu cầu mới của user (lặp test, payment thật, DB perf, design verify) có section riêng
- [x] Negative-path API testing được liệt kê (thiếu ở v3)
- [x] Success criteria đo được (R1–R7)

## Consistency
- [x] Không mâu thuẫn constitution v1.0.1 (P1–P8) — đặc biệt P5 (đo trước/sau), P8 (bằng chứng runtime)
- [x] Không mâu thuẫn AGENTS.md (không seed achievements, không Flyway, commit convention)
- [x] Out-of-scope rõ ràng, không mâu thuẫn In-scope

## Testability
- [x] Mỗi R có cách đo cụ thể (test count, grep count, ms timing, screenshot)
- [x] Payment gate có điều kiện dừng rõ ràng

## Risk
- [x] Safeguard destructive action: không auto-accept dialog, data test có nhãn + dọn
- [x] DB change: dump bảng liên quan trước khi ALTER

**Kết luận: PASS — đủ điều kiện sang plan.**
