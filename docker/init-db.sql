-- init-db.sql: Tạo database english_learning nếu chưa tồn tại
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'english_learning')
BEGIN
    CREATE DATABASE english_learning;
END
GO

-- audit-v5 perf: cap SQL Server at 2GB. Host has 8GB shared with backend JVM,
-- Ollama (GPU+CPU offload) and Whisper; default max server memory is unlimited
-- and the buffer pool starves the other services.
EXEC sp_configure 'show advanced options', 1;
RECONFIGURE;
EXEC sp_configure 'max server memory (MB)', 2048;
RECONFIGURE;
GO
