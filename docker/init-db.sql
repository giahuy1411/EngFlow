-- init-db.sql: Tạo database english_learning nếu chưa tồn tại
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'english_learning')
BEGIN
    CREATE DATABASE english_learning;
END
GO
