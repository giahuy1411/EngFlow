$ErrorActionPreference = 'Continue'
Set-Location C:\Users\ASUS\documents\laptrinh\engflow

# Reset plan cache for clean measurement
python sweep\v8\sqlrun.py tmp\_qc_reset.sql 2>&1 | Out-Null
Start-Sleep -Seconds 2

# login
$resp = Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method Post -ContentType "application/json" -Body '{"email":"user@gmail.com","password":"123456"}'
$tok = $resp.token

# SINGLE leaderboard call
Invoke-RestMethod -Uri "http://localhost:8080/api/leaderboard?page=0&size=20" -Method Get -Headers @{ Authorization = "Bearer $tok" } | Out-Null

# Count queries touching study_days AFTER one leaderboard call
$after = python sweep\v8\sqlrun.py tmp\_qc_study.sql 2>&1 | Select-Object -Last 1
Write-Output "AFTER 1 leaderboard(size=20): $after"
