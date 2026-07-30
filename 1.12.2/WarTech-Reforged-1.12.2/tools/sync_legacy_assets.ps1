param(
    [string]$WarTechJar,
    [string]$ClassicHbmJar
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$workspaceRoot = (Resolve-Path (Join-Path $projectRoot '..\..')).Path
$assetsRoot = Join-Path $projectRoot 'src\main\resources\assets\wartecmod'

if (-not $WarTechJar) {
    $WarTechJar = Join-Path $workspaceRoot 'build\WarTech-Reforged-1.6.0-dev66-iff-save.jar'
}
if (-not $ClassicHbmJar) {
    $ClassicHbmJar = Join-Path $workspaceRoot 'HBM-NTM-.1.0.27_X5751.jar'
}

$assetsRoot = (Resolve-Path $assetsRoot).Path
if (-not $assetsRoot.StartsWith($projectRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Asset target escaped project root: $assetsRoot"
}

foreach ($folderName in @('items', 'blocks')) {
    $folder = Join-Path $assetsRoot "textures\$folderName"
    Get-ChildItem -LiteralPath $folder -Filter '*.png' -File | ForEach-Object {
        Remove-Item -LiteralPath $_.FullName
    }
}

function Copy-ZipEntry {
    param(
        [System.IO.Compression.ZipArchive]$Archive,
        [string]$EntryPath,
        [string]$TargetRelativePath
    )
    $entry = $Archive.GetEntry($EntryPath)
    if (-not $entry) {
        throw "Missing source asset: $EntryPath"
    }
    $target = Join-Path $assetsRoot $TargetRelativePath.ToLowerInvariant()
    $directory = Split-Path -Parent $target
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    $source = $entry.Open()
    try {
        $output = [IO.File]::Create($target)
        try {
            $source.CopyTo($output)
        } finally {
            $output.Dispose()
        }
    } finally {
        $source.Dispose()
    }
}

$warTech = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $WarTechJar))
try {
    foreach ($entry in $warTech.Entries) {
        if ($entry.FullName -match '^assets/wartecmod/models/.+\.(obj|mtl)$' -or
            $entry.FullName -match '^assets/wartecmod/textures/(items|blocks|models)/.+\.png$') {
            $relative = $entry.FullName.Substring('assets/wartecmod/'.Length)
            Copy-ZipEntry $warTech $entry.FullName $relative
        }
    }
} finally {
    $warTech.Dispose()
}

$classicHbm = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $ClassicHbmJar))
try {
    $externalAssets = @{
        'assets/hbm/models/turrets/turret_arty.obj' = 'models/legacy_hbm/turret_arty.obj'
        'assets/hbm/models/turrets/turret_himars.obj' = 'models/legacy_hbm/turret_himars.obj'
        'assets/hbm/textures/models/turrets/arty.png' = 'textures/models/legacy_hbm/arty.png'
        'assets/hbm/textures/models/turrets/himars.png' = 'textures/models/legacy_hbm/himars.png'
        'assets/hbm/textures/items/radar_linker.png' = 'textures/items/legacy_hbm_radar_linker.png'
        'assets/hbm/textures/items/wrench.png' = 'textures/items/legacy_hbm_wrench.png'
        'assets/hbm/textures/items/sat_laser.png' = 'textures/items/legacy_hbm_sat_laser.png'
        'assets/hbm/textures/items/sat_mapper.png' = 'textures/items/legacy_hbm_sat_mapper.png'
        'assets/hbm/textures/items/ammo_container.png' = 'textures/items/legacy_hbm_ammo_container.png'
        'assets/hbm/textures/items/ammo_standard.g26_flare_supply.png' = 'textures/items/legacy_hbm_flare_supply.png'
        'assets/hbm/textures/items/ingot_tungsten.png' = 'textures/items/legacy_hbm_ingot_tungsten.png'
        'assets/hbm/textures/blocks/mass_storage_side_wood.png' = 'textures/blocks/legacy_hbm_mass_storage_side_wood.png'
    }
    foreach ($pair in $externalAssets.GetEnumerator()) {
        Copy-ZipEntry $classicHbm $pair.Key $pair.Value
    }
} finally {
    $classicHbm.Dispose()
}

Write-Host 'Synced exact WarTech dev66 and classic HBM renderer assets.'
