# 启动后端（Spring Boot，日志写入 backend/logs/app.log）
$base = Split-Path -Parent $MyInvocation.MyCommand.Path
$java = "$base\tools\jdk21\jdk-21.0.12.1+1\bin\java.exe"
$jar = "$base\backend\target\crm-1.0.0.jar"

if (-not (Test-Path $jar)) {
    Write-Host "jar not found, building backend first..."
    $env:JAVA_HOME = "$base\tools\jdk21\jdk-21.0.12.1+1"
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    & "$base\tools\maven\apache-maven-3.9.16\bin\mvn.cmd" -s "$base\tools\maven-settings.xml" -f "$base\backend\pom.xml" -DskipTests package
    if ($LASTEXITCODE -ne 0) { Write-Host "build failed"; exit 1 }
}

$cmdline = "`"$java`" -jar `"$jar`""
$res = ([wmiclass]'Win32_Process').Create($cmdline)
if ($res.ReturnValue -eq 0) {
    Write-Host "Backend starting on http://localhost:8080 (logs: backend/logs/app.log)"
} else {
    Write-Host "Backend start failed (ReturnValue=$($res.ReturnValue))"
}
