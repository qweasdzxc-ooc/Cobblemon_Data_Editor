# deploy.ps1 - 一键构建并部署到整合包 mods + 备份，自动清理旧版本
param(
    [string]$ModsDir = "E:\PCL\PCL2\.minecraft\versions\Lets go cobblemon master [6.0.2fix]\mods",
    [string]$BackupDir = "E:\PCL\自制修改mod"
)
$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$gp = Join-Path $root "gradle.properties"
$version = (Select-String -LiteralPath $gp -Pattern '^mod_version=').Line -replace '^mod_version=', ''

Push-Location $root
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-25"
& .\gradlew.bat build --console=plain
if ($LASTEXITCODE -ne 0) { Pop-Location; Write-Error "构建失败"; exit 1 }
Pop-Location

$jar = Join-Path $root "build\libs\cobblemoneditor-$version.jar"
if (-not (Test-Path -LiteralPath $jar)) { Write-Error "找不到产物: $jar"; exit 1 }

Get-ChildItem -LiteralPath $ModsDir -Filter "*CobblemonDataEditor-*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -ne "[自制]CobblemonDataEditor-$version.jar" } | ForEach-Object {
    try { Remove-Item -LiteralPath $_.FullName -Force -ErrorAction Stop; Write-Host "已删旧版: $($_.Name)" }
    catch { Write-Warning "删除失败(可能游戏占用): $($_.Name) —— 请先关游戏" }
}

Copy-Item -LiteralPath $jar -Destination (Join-Path $ModsDir "[自制]CobblemonDataEditor-$version.jar") -Force
Copy-Item -LiteralPath $jar -Destination (Join-Path $BackupDir "cobblemoneditor-$version.jar") -Force
Write-Host "已部署 [自制]CobblemonDataEditor-$version.jar"
