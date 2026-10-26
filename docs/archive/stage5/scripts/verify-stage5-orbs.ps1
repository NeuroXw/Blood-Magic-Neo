param([switch]$Restart)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
Push-Location $projectRoot
try {
    & ./scripts/verify-stage1-resources.ps1
    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::new((Join-Path $projectRoot 'src/main/resources/assets/bloodmagic/textures/gui/altar.png'))
    try {
        if ($bitmap.Width -ne 176 -or $bitmap.Height -ne 204) { throw 'Altar GUI texture size mismatch' }
    } finally { $bitmap.Dispose() }
    foreach ($locale in @('en_us', 'zh_cn')) {
        $lang = Get-Content -Raw "src/main/resources/assets/bloodmagic/lang/$locale.json" | ConvertFrom-Json -AsHashtable
        foreach ($key in @('tier','lp','idle','starved','crafting','progress','stored')) {
            if (-not $lang["gui.bloodmagic.altar.$key"]) { throw "Missing GUI translation: $locale/$key" }
        }
        $placeholders = @{'tooltip.bloodmagic.currentOwner'=1; 'tooltip.bloodmagic.orb.stats'=3;
            'tooltip.bloodmagic.sigil.cost'=1; 'message.bloodmagic.altar'=3;
            'message.bloodmagic.altar.progress'=4; 'message.bloodmagic.network'=3}
        foreach ($entry in $placeholders.GetEnumerator()) {
            if ([regex]::Matches($lang[$entry.Key], '%s').Count -ne $entry.Value) { throw "Invalid placeholders: $locale/$($entry.Key)" }
        }
    }
    $passes = if ($Restart) { 2 } else { 1 }
    foreach ($pass in 1..$passes) {
        $log = "build-stage5-verify-pass$pass.log"
        & ./build-local.ps1 -GradleTasks @('build','runServer','-Pstage5Smoke=true') *> $log
        if ($LASTEXITCODE -ne 0) { Get-Content $log -Tail 70; throw "Stage 5 server check failed: $log" }
        $result = Get-Content -Raw run/stage5-smoke/stage5-smoke-result.txt
        if ($result -notmatch '^STAGE5_SMOKE PASS:') { throw 'Missing fresh stage 5 PASS marker' }
        if ($pass -eq 2 -and $result -notmatch 'RESTART persistence verified') { throw 'Restart was not verified' }
        Write-Output $result.Trim()
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jar = [IO.Compression.ZipFile]::OpenRead((Join-Path $projectRoot 'build/libs/bloodmagic-neoforge-0.1.0-stage5.jar'))
    try {
        $names = @($jar.Entries.FullName)
        foreach ($entry in @('wayoftime/bloodmagic/client/render/BloodAltarRenderer.class',
                'wayoftime/bloodmagic/client/render/AltarRenderState.class',
                'wayoftime/bloodmagic/client/BindingTooltips.class',
                'wayoftime/bloodmagic/common/menu/AltarMenu.class',
                'wayoftime/bloodmagic/client/screen/AltarScreen.class',
                'wayoftime/bloodmagic/core/data/SoulNetworks.class',
                'wayoftime/bloodmagic/common/item/sigil/ItemSigilAir.class',
                'data/bloodmagic/damage_type/sacrifice.json',
                'assets/bloodmagic/textures/gui/altar.png')) {
            if ($entry -notin $names) { throw "Missing release entry: $entry" }
        }
        if ($names -match '^wayoftime/bloodmagic/test/') { throw 'Test classes leaked into release JAR' }
    } finally { $jar.Dispose() }
    Get-FileHash build/libs/bloodmagic-neoforge-0.1.0-stage5.jar -Algorithm SHA256 | Format-List
    Write-Output 'PASS: resources, GUI texture, translations, dedicated-server orbs/sigils and release contents. No GPU/client visual verification.'
} finally { Pop-Location }
