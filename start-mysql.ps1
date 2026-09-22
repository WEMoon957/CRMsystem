# 启动 MySQL（首次自动初始化，之后幂等）
$base = Split-Path -Parent $MyInvocation.MyCommand.Path
$mysql = "$base\tools\mysql\mysql-8.0.28-winx64"
$dataDir = "$base\tools\mysql\data"

if (Get-Process -Name mysqld -ErrorAction SilentlyContinue) {
    Write-Host "MySQL already running (port 33061)."
    exit 0
}

if (-not (Test-Path $dataDir)) {
    Write-Host "First run: initializing MySQL data directory..."
    & "$mysql\bin\mysqld.exe" "--defaults-file=$mysql\my.ini" --initialize-insecure --console
    if ($LASTEXITCODE -ne 0) {
        Write-Host "MySQL init failed. Check VC++ runtime or logs under $dataDir"
        exit 1
    }
}

$cmdline = "`"$mysql\bin\mysqld.exe`" --defaults-file=`"$mysql\my.ini`""
$res = ([wmiclass]'Win32_Process').Create($cmdline)
if ($res.ReturnValue -eq 0) {
    Write-Host "MySQL started on 127.0.0.1:33061"
} else {
    Write-Host "MySQL start failed (ReturnValue=$($res.ReturnValue))"
}
