param(
    [string]$AvdName = "Flashito",
    [switch]$ColdBoot
)

$ErrorActionPreference = "Stop"
$sdkRoot = if ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} elseif ($env:ANDROID_HOME) {
    $env:ANDROID_HOME
} else {
    Join-Path $env:LOCALAPPDATA "Android\Sdk"
}

$emulator = Join-Path $sdkRoot "emulator\emulator.exe"
if (-not (Test-Path -LiteralPath $emulator)) {
    throw "No se encontro Android Emulator en $emulator."
}

$available = & $emulator -list-avds
if ($available -notcontains $AvdName) {
    throw "El AVD '$AvdName' no existe. Ejecuta primero scripts\setup-emulator.ps1."
}

$arguments = @("-avd", $AvdName)
if ($ColdBoot) { $arguments += "-no-snapshot-load" }

Start-Process -FilePath $emulator -ArgumentList $arguments
Write-Host "Iniciando el emulador '$AvdName'..."
