# Clarify — audit-v6-full

**Ngày:** 2026-09-04 · **Số câu hỏi:** 0 (user không giám sát realtime — tự quyết theo mặc định hợp lý, ghi lại để user review)

| # | Câu hỏi ngầm định | Quyết định | Lý do |
|---|-------------------|------------|-------|
| C1 | "Toàn bộ API" có gồm mutation (POST/PUT/DELETE) gọi trực tiếp bằng script không? | **Không** — GET sweep bằng script; mutation verify qua **UI E2E** (playwright) | An toàn data; user yêu cầu "kết hợp tương tác giao diện" |
| C2 | Tối ưu DB có được tạo index thật không? | **Có**, nếu DB audit chứng minh lợi ích; đo trước/sau; rollback = DROP INDEX | Yêu cầu "tối ưu hiệu năng" là hành động, không chỉ báo cáo |
| C3 | Design system: có cho phép sửa CSS/component để đạt chuẩn không? | **Có**, nhưng chỉ lỗi vi phạm cụ thể (P1/P2), không redesign lại trang đã đạt | Tránh scope creep; audit-v5 đã close phần lớn |
| C4 | Font Be Vietnam Pro đã thay chưa? | **Rồi** (audit-v5, constitution P6) — đợt này chỉ **verify** + quét font hard-code sót | Bằng chứng: tailwind.config.js, index.html, design-system.css |
| C5 | Code chưa commit trên working tree có nằm trong phạm vi audit? | **Có** — ShadowingAiGradingService, SubtitleTranslationService, YouTubeTranscriptService, LlmChatClient + các file sửa | Đó là code mới nhất, chức năng AI mới |
| C6 | Sub-agent được chạy lệnh sửa (docker exec CREATE INDEX, git commit)? | **Không** — sub-agent chỉ đọc; main agent thực thi thay đổi sau khi tổng hợp | Kiểm soát tập trung, tránh xung đột |
