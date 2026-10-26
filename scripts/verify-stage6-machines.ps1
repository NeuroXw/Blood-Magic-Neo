param([switch]$Restart)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
Push-Location $projectRoot
try {
    & ./scripts/verify-stage1-resources.ps1
    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::new((Join-Path $projectRoot 'src/main/resources/assets/bloodmagic/textures/gui/altar.png'))
    try { if ($bitmap.Width -ne 176 -or $bitmap.Height -ne 204) { throw 'Altar GUI texture size mismatch' } }
    finally { $bitmap.Dispose() }
    foreach ($locale in @('en_us','zh_cn')) {
        $lang = Get-Content -Raw "src/main/resources/assets/bloodmagic/lang/$locale.json" | ConvertFrom-Json -AsHashtable
        $placeholders = @{'tooltip.bloodmagic.currentOwner'=1; 'tooltip.bloodmagic.orb.stats'=3;
            'tooltip.bloodmagic.sigil.cost'=1; 'message.bloodmagic.altar'=3;
            'message.bloodmagic.altar.progress'=4; 'message.bloodmagic.network'=3;
            'tooltip.bloodmagic.demon_will'=2; 'gui.bloodmagic.machine.will'=1; 'gui.bloodmagic.machine.lp'=1;
            'message.bloodmagic.ritual.selected'=1}
        foreach ($entry in $placeholders.GetEnumerator()) {
            if ([regex]::Matches($lang[$entry.Key], '%s').Count -ne $entry.Value) { throw "Invalid placeholders: $locale/$($entry.Key)" }
        }
        foreach ($name in @('water','green_grove','crushing')) {
            if (-not $lang["ritual.bloodmagic.$name"]) { throw "Missing ritual name: $locale/$name" }
        }
    }
    $passes = if ($Restart) { 2 } else { 1 }
    foreach ($pass in 1..$passes) {
        $log = "build-stage6-verify-pass$pass.log"
        & ./build-local.ps1 -GradleTasks @('build','runServer','-Pstage6Smoke=true') *> $log
        if ($LASTEXITCODE -ne 0) { Get-Content $log -Tail 70; throw "Stage 6 server check failed: $log" }
        $result = Get-Content -Raw run/stage6-smoke/stage6-smoke-result.txt
        if ($result -notmatch '^STAGE6_SMOKE PASS:') { throw 'Missing fresh stage 6 PASS marker' }
        if ($pass -eq 2 -and $result -notmatch 'RESTART persistence verified') { throw 'Restart was not verified' }
        Write-Output $result.Trim()
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $versionLine = Get-Content (Join-Path $projectRoot 'gradle.properties') | Where-Object { $_ -match '^mod_version=' }
    $releaseVersion = ($versionLine -split '=', 2)[1].Trim()
    $jarPath = Join-Path $projectRoot "build/libs/BloodMagicNeo-$releaseVersion.jar"
    $jar = [IO.Compression.ZipFile]::OpenRead($jarPath)
    try {
        $names = @($jar.Entries.FullName)
        foreach ($entry in @('wayoftime/bloodmagic/client/render/AlchemyArrayRenderer.class',
                'wayoftime/bloodmagic/client/render/ArrayRenderState.class',
                'wayoftime/bloodmagic/client/screen/MachineScreen.class',
                'wayoftime/bloodmagic/common/menu/MachineMenu.class',
                'wayoftime/bloodmagic/common/block/entity/AlchemyArrayBlockEntity.class',
                'wayoftime/bloodmagic/common/block/entity/AlchemyTableBlockEntity.class',
                'wayoftime/bloodmagic/common/block/entity/HellfireForgeBlockEntity.class',
                'wayoftime/bloodmagic/common/block/entity/MasterRitualBlockEntity.class',
                'wayoftime/bloodmagic/will/DemonWill.class',
                'wayoftime/bloodmagic/common/entity/SoulSnare.class',
                'data/bloodmagic/recipe/blood_altar.json',
                'data/bloodmagic/recipe/blood_rune_blank.json',
                'data/bloodmagic/recipe/array/watersigil.json',
                'data/bloodmagic/recipe/soulforge/pettytartaricgem.json',
                'assets/bloodmagic/textures/mob_effect/soul_snare.png')) {
            if ($entry -notin $names) { throw "Missing release entry: $entry" }
        }
        if ($names -match '^wayoftime/bloodmagic/test/') { throw 'Test classes leaked into release JAR' }
    } finally { $jar.Dispose() }
    Get-FileHash -LiteralPath $jarPath -Algorithm SHA256 | Format-List
    Write-Output 'PASS: resources, translations, build, dedicated-server chains/machines/rituals and release contents. GPU/client visual verification remains outstanding.'
} finally { Pop-Location }
