@echo off
echo ====================================================
echo Starting EngFlow Services...
echo ====================================================

:: Step 1: Start Docker
echo [1/3] Starting Docker containers (SQL Server & Redis)...
docker-compose up -d

:: Step 2: Start Backend
echo [2/3] Starting Spring Boot Backend in a new window...
start "EngFlow Backend" cmd /c "mvnw.cmd spring-boot:run"

:: Step 3: Start Frontend
echo [3/3] Starting Vue Frontend with Host binding...
cd frontend
start "EngFlow Frontend" cmd /c "npm run dev -- --host"

echo ====================================================
echo Both services are starting up!
echo - Backend will be available at http://localhost:8080
echo - Frontend will be available at http://localhost:5173
echo ====================================================
pause
