param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath
)

$ErrorActionPreference = "Stop"
$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
$sdkRoot = if ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} elseif ($env:ANDROID_HOME) {
    $env:ANDROID_HOME
} else {
    Join-Path $env:LOCALAPPDATA "Android\Sdk"
}

$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adb)) {
    throw "No se encontro ADB en $adb."
}

$devices = & $adb devices
if (($devices | Select-String "\tdevice$").Count -eq 0) {
    throw "No hay ningún emulador o dispositivo Android conectado."
}

& $adb install -r $resolvedApk
if ($LASTEXITCODE -ne 0) { throw "No se pudo instalar el APK." }
Write-Host "APK instalado correctamente." -ForegroundColor Green
