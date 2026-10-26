param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
$resources = Join-Path $projectRoot 'src/main/resources'
$manifest = Get-Content -Raw (Join-Path $projectRoot 'docs/stage6-content.json') | ConvertFrom-Json
$blockIds = @($manifest.blocks.id)
$blockItemIds = @($manifest.blocks | Where-Object { $_.hasItem -ne $false } | ForEach-Object { $_.id })
$itemIds = @($manifest.items.id) + $blockItemIds

function Assert-Resource([string]$relative) {
    $path = Join-Path $resources $relative
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing resource: $relative" }
    return $path
}

if (@($itemIds | Sort-Object -Unique).Count -ne $itemIds.Count) { throw 'Duplicate item IDs in stage6-content.json' }
if (@($blockIds | Sort-Object -Unique).Count -ne $blockIds.Count) { throw 'Duplicate block IDs in stage6-content.json' }

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
        if ($json.loader -eq 'neoforge:obj') {
            if ($json.model -notlike 'bloodmagic:*.obj') { throw "Unexpected OBJ location: $($json.model)" }
            $objPath = Assert-Resource ('assets/bloodmagic/' + $json.model.Substring(11))
            $obj = Get-Content -Raw -LiteralPath $objPath
            foreach ($library in [regex]::Matches($obj, '(?m)^mtllib\s+(\S+)')) {
                $mtlPath = Join-Path (Split-Path $objPath) $library.Groups[1].Value
                if (-not (Test-Path -LiteralPath $mtlPath)) { throw "Missing OBJ material library: $mtlPath" }
                foreach ($map in [regex]::Matches((Get-Content -Raw -LiteralPath $mtlPath), '(?m)^map_Kd\s+(\S+)')) {
                    # 26.1 ObjGeometry resolves a model texture slot, not a direct texture identifier.
                    $slot = $map.Groups[1].Value.TrimStart('#')
                    if (-not $json.textures.ContainsKey($slot)) { throw "Unresolved OBJ texture slot '$slot': $mtlPath" }
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
    if ($id -notin $blockItemIds) {
        if (Test-Path (Join-Path $resources "data/bloodmagic/loot_table/blocks/$id.json")) { throw "Unexpected drop for itemless block: $id" }
        continue
    }
    $loot = Get-Content -Raw (Assert-Resource "data/bloodmagic/loot_table/blocks/$id.json") | ConvertFrom-Json
    if ($loot.type -ne 'minecraft:block' -or $loot.pools[0].entries[0].name -ne "bloodmagic:$id") {
        throw "Unexpected block drop: $id"
    }
}
$pickaxe = Get-Content -Raw (Assert-Resource 'data/minecraft/tags/block/mineable/pickaxe.json') | ConvertFrom-Json
foreach ($id in $blockItemIds) {
    if ("bloodmagic:$id" -notin $pickaxe.values) { throw "Missing pickaxe tag: $id" }
}
$tableLoot = Get-Content -Raw (Assert-Resource 'data/bloodmagic/loot_table/blocks/alchemytable.json') | ConvertFrom-Json
if ('foot' -notin @($tableLoot.pools[0].conditions.properties.part)) { throw 'Table loot must require the inventory half' }
$null = Assert-Resource 'assets/bloodmagic/textures/mob_effect/soul_snare.png'
foreach ($recipe in $manifest.recipes) { $null = Assert-Resource "data/bloodmagic/recipe/$recipe" }
foreach ($file in Get-ChildItem (Join-Path $resources 'data/bloodmagic/recipe/array') -Filter '*.json') {
    $recipe = Get-Content -Raw $file.FullName | ConvertFrom-Json
    if ($recipe.texture -like 'bloodmagic:*') { $null = Assert-Resource ("assets/bloodmagic/" + $recipe.texture.Substring(11)) }
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
            'tooltip.bloodmagic.orb.desc', 'tooltip.bloodmagic.stage2.altar')) {
        if (-not $lang[$key]) { throw "Missing $locale translation: $key" }
    }
}
foreach ($legacy in @('data/bloodmagic/loot_tables', 'data/minecraft/tags/blocks')) {
    if (Test-Path (Join-Path $resources $legacy)) { throw "Legacy resource directory: $legacy" }
}

# The altar must retain the actual upstream mesh and pixels, not a cube or a missing sprite.
# Pinned source: WayofTime/BloodMagic 35f0188665e648d981095b6933cc75b0ca96eca5.
$altarAssets = @{
    'assets/bloodmagic/models/block/blockaltar.obj' = '3299AD3F908A193E24821DCC3738AF81174EB88868E103DC05FBCDDC8F2AAD84'
    'assets/bloodmagic/textures/block/altar.png' = 'B86D5E0B03D5AC9722E37AFA1FE53C26784EEB5AEFE0C5947C4188DC8E8B16A8'
}
foreach ($asset in $altarAssets.GetEnumerator()) {
    if ((Get-FileHash -LiteralPath (Assert-Resource $asset.Key) -Algorithm SHA256).Hash -ne $asset.Value) {
        throw "Altar asset differs from the pinned upstream original: $($asset.Key)"
    }
}
$altarModel = Get-Content -Raw (Assert-Resource 'assets/bloodmagic/models/block/bloodaltar.json') | ConvertFrom-Json
if ($altarModel.loader -ne 'neoforge:obj' -or $altarModel.model -ne 'bloodmagic:models/block/blockaltar.obj' -or
        $altarModel.textures.default -ne 'bloodmagic:block/altar' -or $altarModel.flip_v -ne $true) {
    throw 'Altar JSON must load the upstream mesh with the atlas texture and original UV orientation'
}
$altarState = Get-Content -Raw (Assert-Resource 'assets/bloodmagic/blockstates/altar.json') | ConvertFrom-Json -AsHashtable
$altarItem = Get-Content -Raw (Assert-Resource 'assets/bloodmagic/models/item/altar.json') | ConvertFrom-Json
if ($altarState.variants[''].model -ne 'bloodmagic:block/bloodaltar' -or $altarItem.parent -ne 'bloodmagic:block/bloodaltar') {
    throw 'Placed and inventory altars must use the same upstream 3D model'
}
Write-Output "PASS: $($jsonFiles.Count) JSON files parse; $($itemIds.Count) item definitions; $($blockIds.Count) blockstates, $($blockItemIds.Count) drops/tool tags plus itemless array; model/texture references and en_us/zh_cn translations."
