param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$androidDeliveryDirectory = Join-Path $projectRoot 'Android安装包'
if (-not (Test-Path -LiteralPath $androidDeliveryDirectory)) {
    New-Item -ItemType Directory -Path $androidDeliveryDirectory -Force | Out-Null
}

if (-not $SkipBuild) {
    & (Join-Path $projectRoot 'gradlew.bat') ':app:assembleDebug' --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw 'Android Debug APK 构建失败'
    }
}

$buildFile = Join-Path $projectRoot 'app\build.gradle.kts'
$versionMatch = Select-String `
    -LiteralPath $buildFile `
    -Pattern 'versionName\s*=\s*"([^"]+)"'
if (-not $versionMatch) {
    throw '无法从 app/build.gradle.kts 读取 versionName'
}
$versionName = $versionMatch.Matches[0].Groups[1].Value
$sourceApk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path -LiteralPath $sourceApk)) {
    throw "找不到构建后的 APK：$sourceApk"
}

$deliveryApk = Join-Path `
    $androidDeliveryDirectory `
    "学习日历-Android-V$versionName-debug.apk"
Copy-Item -LiteralPath $sourceApk -Destination $deliveryApk -Force
$hash = (Get-FileHash -LiteralPath $deliveryApk -Algorithm SHA256).Hash

Write-Output "Android APK：$deliveryApk"
Write-Output "SHA256：$hash"
