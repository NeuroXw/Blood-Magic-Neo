param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
$resources = Join-Path $projectRoot 'src/main/resources'
$manifest = Get-Content -Raw (Join-Path $projectRoot 'docs/stage3-content.json') | ConvertFrom-Json
$blockIds = @($manifest.blocks.id)
$itemIds = @($manifest.items.id) + $blockIds

function Assert-Resource([string]$relative) {
    $path = Join-Path $resources $relative
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing resource: $relative" }
    return $path
}

if (@($itemIds | Sort-Object -Unique).Count -ne $itemIds.Count) { throw 'Duplicate item IDs in stage1-content.json' }
if (@($blockIds | Sort-Object -Unique).Count -ne $blockIds.Count) { throw 'Duplicate block IDs in stage1-content.json' }

$jsonFiles = @(Get-ChildItem -LiteralPath $resources -Recurse -Filter '*.json')
foreach ($file in $jsonFiles) {
    $json = Get-Content -Raw -LiteralPath $file.FullName | ConvertFrom-Json -AsHashtable
    if ($file.FullName -match '[\\/]models[\\/]') {
        if ($json.ContainsKey('overrides')) { throw "Unmigrated model overrides: $($file.Name)" }
        if ($json.parent -like 'bloodmagic:*') {
            $null = Assert-Resource ("assets/bloodmagic/models/" + $json.parent.Substring(11) + '.json')
        }
        if ($json.ContainsKey('textures')) {
            foreach ($texture in $json.textures.Values) {
                if ($texture -like 'bloodmagic:*') {
                    $null = Assert-Resource ("assets/bloodmagic/textures/" + $texture.Substring(11) + '.png')
                }
            }
        }
    }
    if ($file.FullName -match '[\\/]blockstates[\\/]') {
        foreach ($match in [regex]::Matches((Get-Content -Raw -LiteralPath $file.FullName), '"model"\s*:\s*"bloodmagic:([^"]+)"')) {
            $null = Assert-Resource ("assets/bloodmagic/models/" + $match.Groups[1].Value + '.json')
        }
    }
}

$definitions = @(Get-ChildItem (Join-Path $resources 'assets/bloodmagic/items') -Filter '*.json')
if ($definitions.Count -ne $itemIds.Count) { throw 'Item definition count does not match the content manifest' }
foreach ($id in $itemIds) {
    $definition = Get-Content -Raw (Assert-Resource "assets/bloodmagic/items/$id.json") | ConvertFrom-Json
    if ($definition.model.type -ne 'minecraft:model' -or $definition.model.model -ne "bloodmagic:item/$id") {
        throw "Unexpected item definition: $id"
    }
    $null = Assert-Resource "assets/bloodmagic/models/item/$id.json"
}

foreach ($id in $blockIds) {
    $null = Assert-Resource "assets/bloodmagic/blockstates/$id.json"
    $loot = Get-Content -Raw (Assert-Resource "data/bloodmagic/loot_table/blocks/$id.json") | ConvertFrom-Json
    if ($loot.type -ne 'minecraft:block' -or $loot.pools[0].entries[0].name -ne "bloodmagic:$id") {
        throw "Unexpected block drop: $id"
    }
}
$pickaxe = Get-Content -Raw (Assert-Resource 'data/minecraft/tags/block/mineable/pickaxe.json') | ConvertFrom-Json
foreach ($id in $blockIds) {
    if ("bloodmagic:$id" -notin $pickaxe.values) { throw "Missing pickaxe tag: $id" }
}
foreach ($locale in @('en_us', 'zh_cn')) {
    $lang = Get-Content -Raw (Assert-Resource "assets/bloodmagic/lang/$locale.json") | ConvertFrom-Json -AsHashtable
    foreach ($entry in $manifest.items) {
        if (-not $lang["item.bloodmagic.$($entry.id)"]) { throw "Missing $locale item name: $($entry.id)" }
    }
    foreach ($id in $blockIds) {
        if (-not $lang["block.bloodmagic.$id"]) { throw "Missing $locale block name: $id" }
    }
    foreach ($key in @('itemGroup.bloodmagic.creativeTab', 'tooltip.bloodmagic.decoration.safe',
            'tooltip.bloodmagic.hellforgedparts', 'tooltip.bloodmagic.slate_vial',
            'tooltip.bloodmagic.stage2.orb', 'tooltip.bloodmagic.stage2.altar')) {
        if (-not $lang[$key]) { throw "Missing $locale translation: $key" }
    }
}
foreach ($legacy in @('data/bloodmagic/loot_tables', 'data/minecraft/tags/blocks')) {
    if (Test-Path (Join-Path $resources $legacy)) { throw "Legacy resource directory: $legacy" }
}
Write-Output "PASS: $($jsonFiles.Count) JSON files parse; $($itemIds.Count) item definitions; $($blockIds.Count) blockstates, drops and pickaxe tags; model/texture references and en_us/zh_cn translations."
