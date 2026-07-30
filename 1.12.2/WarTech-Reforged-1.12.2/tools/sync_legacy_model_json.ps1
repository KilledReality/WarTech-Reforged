$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$assetsRoot = Join-Path $projectRoot 'src\main\resources\assets\wartecmod'
$itemModelRoot = Join-Path $assetsRoot 'models\item'
$blockModelRoot = Join-Path $assetsRoot 'models\block'

$customItemNames = [Collections.Generic.HashSet[string]]::new(
    [StringComparer]::OrdinalIgnoreCase
)
@(
    'stormshadow', 'gerandrone', 'antiradiationmissile', 'kh555missile',
    'mq9reaperdrone', 'mq9payload', 'strategicbomb', 'tu95strategicbomber',
    'tacticalaircraft', 'su27tacticalaircraft', 'mobileradartruck',
    's400longrangeradar', 'airdefensecommandtruck', 'electronicwarfareunit',
    'mobileairdefensesystem', 'mobileartillery', 'itemkalibrmissile',
    'itemtomahawkmissile', 'itemcj10missile', 'itemiskandermissile',
    'itemmissileantiairtier1', 'itemmissileantiairtier2',
    'itemmissileantiairtier3', 'itemmissileasat'
) | ForEach-Object { [void]$customItemNames.Add($_) }

$customBlockNames = [Collections.Generic.HashSet[string]]::new(
    [StringComparer]::OrdinalIgnoreCase
)
@(
    'geranlauncher', 'patriotlauncher', 's400launcher',
    'strategicearlywarningradar', 'vlsexhaust',
    'ballisticmissilelauncher', 'launchtube'
) | ForEach-Object { [void]$customBlockNames.Add($_) }

$textureAliases = @{
    'itemcruisemissiletb' = 'itemcruisemissilefae'
    'itemwarheadtb' = 'itemwarheadfae'
    'sat_nuclear' = 'itemsatellitenuclear'
    'sat_emp' = 'legacy_hbm_sat_mapper'
    'itemtargetfinder' = 'legacy_hbm_radar_linker'
    'wartechiffconfigurator' = 'legacy_hbm_radar_linker'
    'mq9flares' = 'legacy_hbm_flare_supply'
    'wartecsalvagewrench' = 'legacy_hbm_wrench'
    'kineticbombardmentsatellite' = 'legacy_hbm_sat_laser'
    'pantsir30mmbelt' = 'legacy_hbm_ammo_container'
}

function Write-Json {
    param([string]$Path, [object]$Value)
    $json = $Value | ConvertTo-Json -Depth 8
    [IO.File]::WriteAllText($Path, $json + [Environment]::NewLine)
}

Get-ChildItem -LiteralPath $itemModelRoot -Filter '*.json' -File | ForEach-Object {
    $name = $_.BaseName.ToLowerInvariant()
    $baseName = $name.Split('_')[0]
    $custom = $customItemNames.Contains($name) -or
        $customItemNames.Contains($baseName) -or
        $customBlockNames.Contains($name) -or
        $name.StartsWith('decoblock')
    if ($custom) {
        Write-Json $_.FullName ([ordered]@{ parent = 'builtin/entity' })
        return
    }

    $texture = if ($textureAliases.ContainsKey($name)) {
        $textureAliases[$name]
    } else {
        $name
    }
    $texturePath = Join-Path $assetsRoot "textures\items\$texture.png"
    if (Test-Path -LiteralPath $texturePath) {
        Write-Json $_.FullName ([ordered]@{
            parent = 'item/generated'
            textures = [ordered]@{ layer0 = "wartecmod:items/$texture" }
        })
    }
}

$emptyModel = [ordered]@{ textures = [ordered]@{} }
Get-ChildItem -LiteralPath $blockModelRoot -Filter '*.json' -File | ForEach-Object {
    $name = $_.BaseName.ToLowerInvariant()
    if ($customBlockNames.Contains($name) -or $name.StartsWith('decoblock') -or
        $name -eq 'mobileturretproxy' -or $name -eq 'strategicradarstructure') {
        Write-Json $_.FullName $emptyModel
    }
}

$cubeMappings = @{
    'blockarmorsteel' = 'wartecmod:blocks/blockarmorsteel'
    'blockreinforcedwood' = 'wartecmod:blocks/legacy_hbm_mass_storage_side_wood'
    'airraidsirenrelay' = 'minecraft:blocks/redstone_block'
    'longrangecommunicationmast' = 'minecraft:blocks/iron_block'
    'longrangecommunicationmastsegment' = 'minecraft:blocks/iron_bars'
}
foreach ($pair in $cubeMappings.GetEnumerator()) {
    $path = Join-Path $blockModelRoot ($pair.Key + '.json')
    Write-Json $path ([ordered]@{
        parent = 'block/cube_all'
        textures = [ordered]@{ all = $pair.Value }
    })
}

Write-Host 'Rebound item and block JSON models to exact legacy resources.'
