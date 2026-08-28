-- =============================================
-- data.sql - Dữ liệu mẫu cho EngFlow
-- Chỉ INSERT nếu bảng rỗng (tránh duplicate khi restart)
-- =============================================

-- Seed Users (Password: 123456)
IF NOT EXISTS (SELECT 1 FROM users)
BEGIN
    INSERT INTO users (username, email, password_hash, full_name, avatar_url, is_admin, current_level, total_points, is_active)
    VALUES 
    ('student', 'user@gmail.com', '$2a$10$8.tX9s86n.H5lE8pB8dJbeK6G2wT/P9nC/NpyBwRjUjJ2B53/1BvG', N'Học Viên Mẫu', 'https://api.dicebear.com/7.x/adventurer/svg?seed=student', 0, 'ELEMENTARY', 0, 1),
    ('administrator', 'admin@gmail.com', '$2a$10$8.tX9s86n.H5lE8pB8dJbeK6G2wT/P9nC/NpyBwRjUjJ2B53/1BvG', N'Quản Trị Viên', 'https://api.dicebear.com/7.x/adventurer/svg?seed=admin', 1, 'UPPER_INTERMEDIATE', 0, 1);
END;
