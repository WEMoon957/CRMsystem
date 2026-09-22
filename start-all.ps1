# 一键启动：MySQL -> 后端 -> 前端
$base = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "=== [1/3] Starting MySQL ==="
& "$base\start-mysql.ps1"
Start-Sleep -Seconds 3

Write-Host "=== [2/3] Starting Backend ==="
& "$base\start-backend.ps1"

Write-Host "=== [3/3] Starting Frontend ==="
$ps = (Get-Command powershell.exe).Source
([wmiclass]'Win32_Process').Create("`"$ps`" -ExecutionPolicy Bypass -NoProfile -File `"$base\start-frontend.ps1`"") | Out-Null

Write-Host ""
Write-Host "All services launching..."
Write-Host "  Frontend: http://localhost:5173"
Write-Host "  Backend : http://localhost:8080/healthz"
Write-Host "  Login   : admin / Admin@123  (or sales01 / Sales@123)"
