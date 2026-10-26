param([switch]$Restart)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
Push-Location $projectRoot
try {
    & ./scripts/verify-stage1-resources.ps1
    $texture = Join-Path $projectRoot 'src/main/resources/assets/bloodmagic/textures/gui/altar.png'
    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::new($texture)
    try {
        if ($bitmap.Width -ne 176 -or $bitmap.Height -ne 204) { throw 'Altar GUI texture size mismatch' }
    } finally { $bitmap.Dispose() }
    foreach ($locale in @('en_us', 'zh_cn')) {
        $lang = Get-Content -Raw "src/main/resources/assets/bloodmagic/lang/$locale.json" | ConvertFrom-Json -AsHashtable
        foreach ($key in @('tier','lp','idle','starved','crafting','progress','stored')) {
            if (-not $lang["gui.bloodmagic.altar.$key"]) { throw "Missing GUI translation: $locale/$key" }
        }
        foreach ($key in @('progress','stored')) {
            if ([regex]::Matches($lang["gui.bloodmagic.altar.$key"], '%s').Count -ne 2) { throw "Invalid GUI placeholders: $locale/$key" }
        }
    }
    $passes = if ($Restart) { 2 } else { 1 }
    foreach ($pass in 1..$passes) {
        $log = "build-stage4-smoke-pass$pass.log"
        & ./build-local.ps1 -GradleTasks @('build','runServer','-Pstage4Smoke=true') *> $log
        if ($LASTEXITCODE -ne 0) { Get-Content $log -Tail 80; throw "Stage 4 server check failed: $log" }
        $result = Get-Content -Raw run/stage4-smoke/stage4-smoke-result.txt
        if ($result -notmatch '^STAGE4_SMOKE PASS:') { throw 'Missing fresh stage 4 PASS marker' }
        if ($pass -eq 2 -and $result -notmatch 'RESTART persistence verified') { throw 'Restart was not verified' }
        Write-Output $result.Trim()
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jar = [IO.Compression.ZipFile]::OpenRead((Join-Path $projectRoot 'build/libs/bloodmagic-neoforge-0.1.0-stage4.jar'))
    try {
        $names = @($jar.Entries.FullName)
        foreach ($entry in @('wayoftime/bloodmagic/common/menu/AltarMenu.class',
                'wayoftime/bloodmagic/client/screen/AltarScreen.class',
                'wayoftime/bloodmagic/client/BloodMagicClient.class',
                'assets/bloodmagic/textures/gui/altar.png')) {
            if ($entry -notin $names) { throw "Missing GUI release entry: $entry" }
        }
        if ($names -match '^wayoftime/bloodmagic/test/') { throw 'Test classes leaked into release JAR' }
    } finally { $jar.Dispose() }
    Write-Output 'PASS: GUI texture/translations, dedicated-server checks, and release contents. Graphical client rendering is not exercised.'
} finally { Pop-Location }
