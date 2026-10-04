param(
    [string]$AvdName = "Flashito"
)

$ErrorActionPreference = "Stop"

function Get-AndroidSdkRoot {
    $candidates = @(
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME,
        (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    throw "No se encontro Android SDK. Instala Android SDK Command-line Tools o define ANDROID_SDK_ROOT."
}

function Find-Tool([string]$root, [string]$name) {
    $tool = Get-ChildItem -LiteralPath $root -Filter $name -File -Recurse -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $tool) { throw "No se encontro $name dentro de $root." }
    return $tool.FullName
}

function Set-IniValue([string]$path, [string]$key, [string]$value) {
    $lines = @(Get-Content -LiteralPath $path -ErrorAction SilentlyContinue) |
        Where-Object { $_ -notmatch ('^' + [regex]::Escape($key) + '\s*=') }
    $lines += "$key=$value"
    Set-Content -LiteralPath $path -Value $lines -Encoding ASCII
}

$sdkRoot = Get-AndroidSdkRoot
$sdkManager = Find-Tool (Join-Path $sdkRoot "cmdline-tools") "sdkmanager.bat"
$avdManager = Find-Tool (Join-Path $sdkRoot "cmdline-tools") "avdmanager.bat"
$image = "system-images;android-34;default;x86_64"
$avdRoot = Join-Path $env:USERPROFILE ".android\avd"
$configPath = Join-Path $avdRoot "$AvdName.avd\config.ini"

if (Test-Path -LiteralPath $configPath) {
    Write-Host "El AVD '$AvdName' ya existe. No se modifico." -ForegroundColor Yellow
    exit 0
}

Write-Host "Instalando Android Emulator, platform-tools e imagen Android 14..."
& $sdkManager "emulator" "platform-tools" "platforms;android-34" $image
if ($LASTEXITCODE -ne 0) {
    throw "sdkmanager fallo. Acepta las licencias con: sdkmanager --licenses"
}

Write-Host "Creando AVD '$AvdName'..."
@("no") | & $avdManager create avd --name $AvdName --package $image --device "pixel_5" --force
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $configPath)) {
    throw "No se pudo crear el AVD '$AvdName'."
}

$settings = [ordered]@{
    "hw.cpu.ncore" = "4"
    "hw.ramSize" = "1536M"
    "hw.lcd.width" = "1080"
    "hw.lcd.height" = "2340"
    "hw.lcd.density" = "440"
    "disk.dataPartition.size" = "6442450944"
    "hw.gpu.enabled" = "yes"
    "hw.gpu.mode" = "auto"
    "hw.keyboard" = "no"
    "hw.camera.back" = "emulated"
    "hw.camera.front" = "none"
    "PlayStore.enabled" = "no"
    "fastboot.forceColdBoot" = "no"
    "fastboot.forceFastBoot" = "yes"
}

foreach ($entry in $settings.GetEnumerator()) {
    Set-IniValue $configPath $entry.Key $entry.Value
}

Write-Host "AVD '$AvdName' creado correctamente." -ForegroundColor Green
Write-Host "Inicialo con scripts\start-emulator.ps1"
