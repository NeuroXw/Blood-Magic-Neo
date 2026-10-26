param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
$releaseVersion = ((Get-Content (Join-Path $projectRoot 'gradle.properties') | Where-Object { $_ -match '^mod_version=' }) -split '=', 2)[1].Trim()
$jarPath = Join-Path $projectRoot "build/libs/BloodMagicNeo-$releaseVersion.jar"
$classesRoot = Join-Path $projectRoot 'build/classes/java/main'
$resourcesRoot = Join-Path $projectRoot 'build/resources/main'

# Audit the actual archive against only the production build outputs. Smoke suites,
# development worlds, scripts and archived releases are intentionally kept outside it.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$expected = @{}
foreach ($root in @($classesRoot, $resourcesRoot)) {
    if (-not (Test-Path -LiteralPath $root)) { throw "Missing build output; run build first: $root" }
    foreach ($file in Get-ChildItem -LiteralPath $root -Recurse -File) {
        $relative = [IO.Path]::GetRelativePath($root, $file.FullName).Replace('\', '/')
        if ($expected.ContainsKey($relative)) { throw "Duplicate production entry: $relative" }
        $expected[$relative] = $file.FullName
    }
}
foreach ($name in @('LICENSE.md', 'NOTICE.md', 'TEMPLATE_LICENSE.txt')) {
    $expected["META-INF/$name"] = Join-Path $projectRoot $name
}

$jar = [IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $files = @($jar.Entries | Where-Object { -not $_.FullName.EndsWith('/') })
    $names = @($files.FullName)
    if (@($names | Sort-Object -Unique).Count -ne $names.Count) { throw 'Duplicate archive entries' }
    $forbidden = @($names | Where-Object {
        $_ -match '(?i)(^|/)(test|tests|stage\d+Smoke|run|logs|scripts|docs|\.gradle|__pycache__)(/|$)' -or
        $_ -match '(?i)(SmokeChecks|MenuTestPlayer|\.java$|\.log$|\.pyc$|\.ps1$|\.jar$)'
    })
    if ($forbidden.Count) { throw "Development/test content in release: $($forbidden -join ', ')" }
    foreach ($name in $expected.Keys) {
        if ($name -notin $names) { throw "Missing release entry: $name" }
    }
    foreach ($entry in $files) {
        $name = $entry.FullName
        if ($name -eq 'META-INF/MANIFEST.MF') { continue }
        if (-not $expected.ContainsKey($name)) { throw "Unexpected release entry: $name" }
        $stream = $entry.Open()
        try { $archiveHash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($stream)) }
        finally { $stream.Dispose() }
        $sourceHash = (Get-FileHash -LiteralPath $expected[$name] -Algorithm SHA256).Hash
        if ($archiveHash -ne $sourceHash) { throw "Release entry differs from production output: $name" }
        if ($name.EndsWith('.class')) {
            $stream = $entry.Open()
            $memory = [IO.MemoryStream]::new()
            try {
                $stream.CopyTo($memory)
                $classText = [Text.Encoding]::Latin1.GetString($memory.ToArray())
                if ($classText -match 'wayoftime/bloodmagic/test/|STAGE\d+_SMOKE') { throw "Test reference in production class: $name" }
            } finally { $stream.Dispose(); $memory.Dispose() }
        }
    }
    $reader = [IO.StreamReader]::new($jar.GetEntry('META-INF/neoforge.mods.toml').Open())
    try { $metadata = $reader.ReadToEnd() } finally { $reader.Dispose() }
    foreach ($pattern in @('(?m)^modId="bloodmagic"\s*$', ('(?m)^version="' + [regex]::Escape($releaseVersion) + '"\s*$'),
            '(?m)^displayName="Blood Magic: Neo"\s*$', '(?m)^license="CC-BY-4\.0"\s*$',
            '(?m)^logoFile="logo\.png"\s*$', '(?m)^logoBlur=false\s*$',
            '(?m)^authors="[^"\r\n]*WayofTime[^"\r\n]*NeuroXw[^"\r\n]*"\s*$')) {
        if ($metadata -notmatch $pattern) { throw "Release metadata mismatch: $pattern" }
    }
    if (-not $metadata.Contains('Unofficial port. Based on Blood Magic by WayofTime. Licensed under CC-BY-4.0.')) { throw 'Missing attribution' }
    if ($metadata.Contains('${')) { throw 'Unexpanded metadata placeholder' }
    if ('logo.png' -notin $names) { throw 'Missing mod logo' }
    $classCount = @($names | Where-Object { $_.EndsWith('.class') }).Count
    $recipeCount = @($names | Where-Object { $_ -like 'data/bloodmagic/recipe/*.json' }).Count
    $entryCount = $files.Count
} finally { $jar.Dispose() }

$hash = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash
$report = [ordered]@{
    result = 'PASS'
    checkedAt = [DateTimeOffset]::Now.ToString('o')
    jar = $jarPath
    bytes = (Get-Item -LiteralPath $jarPath).Length
    sha256 = $hash
    files = $entryCount
    productionClasses = $classCount
    recipes = $recipeCount
    metadata = "Blood Magic: Neo / $releaseVersion / WayofTime, NeuroXw / CC-BY-4.0"
    logo = 'logo.png'
    productionBytesMatch = $true
    testClassesOrReferences = 0
    unexpectedFiles = 0
    graphicalClientVerified = $false
}
$reportPath = Join-Path $projectRoot 'build/reports/stage7-release-audit.json'
$null = New-Item -ItemType Directory -Path (Split-Path $reportPath) -Force
$report | ConvertTo-Json | Set-Content -LiteralPath $reportPath -Encoding utf8
"$hash  $([IO.Path]::GetFileName($jarPath))" | Set-Content -LiteralPath "$jarPath.sha256" -Encoding ascii
$report | ConvertTo-Json
Write-Output 'PASS: release metadata, all production bytes, legal notices, and absence of development/test entries.'
