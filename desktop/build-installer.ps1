# =========================================================
# Windows 桌面安装包一键构建
# 流程：前端构建 → 后端打包 → jlink 裁剪 JRE → electron-builder 产出 NSIS 安装包
# 用法：
#   powershell -ExecutionPolicy Bypass -File .\desktop\build-installer.ps1
#   可加 -SkipTests 跳过后端测试
# 产出：desktop\dist\SaaS CRM Setup 1.0.0.exe
# =========================================================
param(
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$desktopDir = $PSScriptRoot
$root = Split-Path $desktopDir -Parent

function Write-Step($msg) { Write-Host "`n===== $msg =====" -ForegroundColor Cyan }
function Assert-Ok($what) { if ($LASTEXITCODE -ne 0) { throw "$what 失败（退出码 $LASTEXITCODE）" } }

# ---------- 1. 前端构建 ----------
Write-Step "1/4 前端构建"
Push-Location (Join-Path $root 'frontend')
if (Test-Path (Join-Path $root 'frontend\package-lock.json')) { npm ci } else { npm install }
Assert-Ok "前端依赖安装"
npm run build
Assert-Ok "前端构建"
Pop-Location

# ---------- 2. 后端打包 ----------
Write-Step "2/4 后端打包"
$env:PATH = "$root\tools\jdk21\bin;$root\tools\maven\bin;$env:PATH"
Push-Location (Join-Path $root 'backend')
$mvnArgs = @('-q', 'package')
if ($SkipTests) { $mvnArgs += '-DskipTests' }
& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) { throw "后端打包失败" }
Pop-Location
$jar = Join-Path $root 'backend\target\crm.jar'
if (-not (Test-Path $jar)) { throw "后端产物不存在: $jar" }

# ---------- 3. jlink 裁剪 JRE（仅首次或强制重建）----------
Write-Step "3/4 准备内嵌 JRE"
$jreDir = Join-Path $desktopDir 'runtime\jre'
$jreMarker = Join-Path $jreDir 'windows-x64.marker'
if ((Test-Path $jreDir) -and (Test-Path $jreMarker)) {
    Write-Host "已存在 $jreDir，跳过 jlink（删除 desktop\runtime 可重建）"
} else {
    # 无标记的 JRE 可能来自其他平台，强制重建，防止把非 Windows JRE 打进安装包
    if (Test-Path $jreDir) { Remove-Item -Recurse -Force $jreDir }
    $jlink = Join-Path $root 'tools\jdk21\bin\jlink.exe'
    if (-not (Test-Path $jlink)) { throw "未找到 JDK: $jlink（请将便携 JDK21 置于 tools\jdk21）" }
    & $jlink `
        --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.crypto.mscapi,jdk.unsupported,jdk.zipfs `
        --strip-debug --no-man-pages --no-header-files --compress=2 `
        --output $jreDir
    if ($LASTEXITCODE -ne 0) { throw "jlink 裁剪失败" }
    New-Item -Path $jreMarker -ItemType File -Force | Out-Null
}

# ---------- 4. electron-builder 打包 ----------
Write-Step "4/4 electron-builder 产出 NSIS 安装包"
# 国内网络如下载 Electron/NSIS 缓慢或失败，取消下面两行注释后重跑：
# $env:ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/"
# $env:ELECTRON_BUILDER_BINARIES_MIRROR="https://npmmirror.com/mirrors/electron-builder-binaries/"
Push-Location $desktopDir
if (Test-Path (Join-Path $desktopDir 'package-lock.json')) { npm ci } else { npm install }
Assert-Ok "桌面端依赖安装"
npx electron-builder --win nsis
if ($LASTEXITCODE -ne 0) { throw "electron-builder 打包失败" }
Pop-Location

Write-Step "完成"
Get-ChildItem (Join-Path $desktopDir 'dist') -Filter *.exe | ForEach-Object { Write-Host $_.FullName -ForegroundColor Green }
