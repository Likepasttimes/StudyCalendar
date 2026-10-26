param(
    [switch]$SkipGradle,
    [string]$InnoCompilerPath = $env:INNO_SETUP_HOME
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildRoot = Join-Path $PSScriptRoot 'build'
$fatJar = Join-Path $buildRoot 'libs\LearningCalendarDesktop.jar'
$packageInput = Join-Path $buildRoot 'windows-package-input'
$packageOutput = Join-Path $buildRoot 'windows-package'
$iconDirectory = Join-Path $buildRoot 'windows-icon'
$iconPath = Join-Path $iconDirectory 'learning-calendar.ico'
$deliveryDirectory = Join-Path $projectRoot 'Windows安装包'
$deliveryZip = Join-Path $deliveryDirectory '学习日历-Windows-V1.3.0.zip'
$deliveryInstaller = Join-Path $deliveryDirectory '学习日历-Windows-V1.3.0-安装程序.exe'

if (-not (Test-Path -LiteralPath $deliveryDirectory)) {
    New-Item -ItemType Directory -Path $deliveryDirectory -Force | Out-Null
}

if (-not $SkipGradle) {
    & (Join-Path $projectRoot 'gradlew.bat') ':windowsApp:windowsFatJar' --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw 'Windows JAR 构建失败'
    }
}

if (-not (Test-Path -LiteralPath $fatJar)) {
    throw "找不到 Windows JAR：$fatJar"
}

$jpackage = if (
    $env:JAVA_HOME -and
    (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\jpackage.exe'))
) {
    Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
} else {
    (Get-Command jpackage -ErrorAction Stop).Source
}

function Remove-OwnedBuildDirectory {
    param([string]$Target)
    $resolvedBuild = [System.IO.Path]::GetFullPath($buildRoot)
    $resolvedTarget = [System.IO.Path]::GetFullPath($Target)
    if (
        $resolvedTarget.StartsWith(
            $resolvedBuild + [System.IO.Path]::DirectorySeparatorChar,
            [System.StringComparison]::OrdinalIgnoreCase
        ) -and
        (Test-Path -LiteralPath $resolvedTarget)
    ) {
        Remove-Item -LiteralPath $resolvedTarget -Recurse -Force
    }
}

Remove-OwnedBuildDirectory $packageInput
Remove-OwnedBuildDirectory $packageOutput
Remove-OwnedBuildDirectory $iconDirectory
New-Item -ItemType Directory -Path $packageInput -Force | Out-Null
New-Item -ItemType Directory -Path $packageOutput -Force | Out-Null
New-Item -ItemType Directory -Path $iconDirectory -Force | Out-Null
Copy-Item -LiteralPath $fatJar -Destination $packageInput

Add-Type -AssemblyName System.Drawing
$bitmap = New-Object System.Drawing.Bitmap 256, 256
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.Clear([System.Drawing.Color]::Transparent)

$sageBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml('#54785C'))
$warmBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml('#FFFDF8'))
$sageSoftBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml('#DCE9DC'))
$deepBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml('#183247'))

$backgroundPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$backgroundPath.AddArc(8, 8, 72, 72, 180, 90)
$backgroundPath.AddArc(176, 8, 72, 72, 270, 90)
$backgroundPath.AddArc(176, 176, 72, 72, 0, 90)
$backgroundPath.AddArc(8, 176, 72, 72, 90, 90)
$backgroundPath.CloseFigure()
$graphics.FillPath($sageBrush, $backgroundPath)

$calendarPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$calendarPath.AddArc(55, 57, 26, 26, 180, 90)
$calendarPath.AddArc(175, 57, 26, 26, 270, 90)
$calendarPath.AddArc(175, 177, 26, 26, 0, 90)
$calendarPath.AddArc(55, 177, 26, 26, 90, 90)
$calendarPath.CloseFigure()
$graphics.FillPath($warmBrush, $calendarPath)
$graphics.FillRectangle($sageSoftBrush, 56, 89, 144, 34)
$graphics.FillRectangle($deepBrush, 76, 47, 14, 31)
$graphics.FillRectangle($deepBrush, 166, 47, 14, 31)
foreach ($point in @(
    @(79, 140), @(113, 140), @(147, 140),
    @(79, 174), @(113, 174)
)) {
    $graphics.FillRectangle($deepBrush, $point[0], $point[1], 22, 20)
}

$pngPath = Join-Path $iconDirectory 'learning-calendar-256.png'
$bitmap.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$bitmap.Dispose()
$backgroundPath.Dispose()
$calendarPath.Dispose()
$sageBrush.Dispose()
$warmBrush.Dispose()
$sageSoftBrush.Dispose()
$deepBrush.Dispose()

$pngBytes = [System.IO.File]::ReadAllBytes($pngPath)
$iconStream = New-Object System.IO.MemoryStream
$writer = New-Object System.IO.BinaryWriter($iconStream)
$writer.Write([UInt16]0)
$writer.Write([UInt16]1)
$writer.Write([UInt16]1)
$writer.Write([Byte]0)
$writer.Write([Byte]0)
$writer.Write([Byte]0)
$writer.Write([Byte]0)
$writer.Write([UInt16]1)
$writer.Write([UInt16]32)
$writer.Write([UInt32]$pngBytes.Length)
$writer.Write([UInt32]22)
$writer.Write($pngBytes)
$writer.Flush()
[System.IO.File]::WriteAllBytes($iconPath, $iconStream.ToArray())
$writer.Dispose()
$iconStream.Dispose()

& $jpackage `
    --type app-image `
    --name '学习日历' `
    --app-version '1.3.0' `
    --vendor '学习日历' `
    --description '学习日历 Windows 版' `
    --input $packageInput `
    --dest $packageOutput `
    --main-jar 'LearningCalendarDesktop.jar' `
    --main-class 'com.learningcalendar.desktop.MainKt' `
    --icon $iconPath `
    --add-modules 'java.base,java.desktop,jdk.httpserver' `
    --java-options '-Dfile.encoding=UTF-8'
if ($LASTEXITCODE -ne 0) {
    throw 'Windows 应用打包失败'
}

if (Test-Path -LiteralPath $deliveryZip) {
    Remove-Item -LiteralPath $deliveryZip -Force
}
Compress-Archive `
    -Path (Join-Path $packageOutput '学习日历') `
    -DestinationPath $deliveryZip `
    -CompressionLevel Optimal

if (-not $InnoCompilerPath) {
    $innoCommand = Get-Command ISCC.exe -ErrorAction SilentlyContinue
    if ($innoCommand) {
        $InnoCompilerPath = Split-Path -Parent $innoCommand.Source
    }
}
$iscc = if (
    $InnoCompilerPath -and
    (Test-Path -LiteralPath $InnoCompilerPath -PathType Container)
) {
    Join-Path $InnoCompilerPath 'ISCC.exe'
} else {
    $InnoCompilerPath
}
if (-not $iscc -or -not (Test-Path -LiteralPath $iscc)) {
    throw '缺少 Inno Setup 的 ISCC.exe，请通过 -InnoCompilerPath 指定工具目录'
}

& $iscc `
    "/DAppSourceRoot=$(Join-Path $packageOutput '学习日历')" `
    "/DAppOutputDir=$deliveryDirectory" `
    '/DAppVersion=1.3.0' `
    "/DIconPath=$iconPath" `
    (Join-Path $PSScriptRoot 'installer.iss')
if ($LASTEXITCODE -ne 0) {
    throw 'Windows 安装程序打包失败'
}
if (-not (Test-Path -LiteralPath $deliveryInstaller)) {
    throw "找不到生成的 Windows 安装程序：$deliveryInstaller"
}

Write-Output "Windows 应用：$packageOutput\学习日历\学习日历.exe"
Write-Output "交付压缩包：$deliveryZip"
Write-Output "Windows 安装程序：$deliveryInstaller"
