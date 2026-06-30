-- =============================================
-- data.sql - Dữ liệu mẫu cho EngFlow
-- Chỉ INSERT nếu bảng rỗng (tránh duplicate khi restart)
-- =============================================

-- Seed Users (Password: 123456)
IF NOT EXISTS (SELECT 1 FROM users)
BEGIN
    INSERT INTO users (username, email, password_hash, full_name, avatar_url, is_admin, current_level, total_points, is_active)
    VALUES 
    ('student', 'user@gmail.com', '$2a$10$8.tX9s86n.H5lE8pB8dJbeK6G2wT/P9nC/NpyBwRjUjJ2B53/1BvG', N'Học Viên Mẫu', 'https://api.dicebear.com/7.x/adventurer/svg?seed=student', 0, 'BEGINNER', 0, 1),
    ('administrator', 'admin@gmail.com', '$2a$10$8.tX9s86n.H5lE8pB8dJbeK6G2wT/P9nC/NpyBwRjUjJ2B53/1BvG', N'Quản Trị Viên', 'https://api.dicebear.com/7.x/adventurer/svg?seed=admin', 1, 'ADVANCED', 0, 1);
END;

-- Seed Achievements
IF NOT EXISTS (SELECT 1 FROM achievements)
BEGIN
    SET IDENTITY_INSERT achievements ON;
    INSERT INTO achievements (achievement_id, name, description, icon_url, points_required, badge_type)
    VALUES 
    (1, N'Khởi đầu Thuận lợi', N'Hoàn thành bài học đầu tiên.', 'https://api.dicebear.com/7.x/identicon/svg?seed=first-step', 10, 'BRONZE'),
    (2, N'Chăm Chỉ', N'Tích lũy 30 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=diligence', 30, 'SILVER'),
    (3, N'Chuyên Gia Ngôn Ngữ', N'Tích lũy 50 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=expert', 50, 'GOLD'),
    (4, N'Người Kiên Trì', N'Tích lũy 100 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=persistence', 100, 'GOLD'),
    (5, N'Bậc Thầy Tiếng Anh', N'Tích lũy 200 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=master', 200, 'DIAMOND');
    SET IDENTITY_INSERT achievements OFF;
END;
