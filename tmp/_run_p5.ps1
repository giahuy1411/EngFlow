$ErrorActionPreference = 'Continue'
Set-Location C:\Users\ASUS\documents\laptrinh\engflow\sweep\v8
node p5.js 2>&1 | Select-String -Pattern "OK|FAIL|ASSERT|=== B" | Select-Object -Last 40
