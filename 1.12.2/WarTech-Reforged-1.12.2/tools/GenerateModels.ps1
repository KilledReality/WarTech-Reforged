param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$contentFile = Join-Path $ProjectRoot "src\main\java\com\wartec\wartecmod\port\content\WarTechContent.java"
$assetRoot = Join-Path $ProjectRoot "src\main\resources\assets\wartecmod"
$source = Get-Content -LiteralPath $contentFile -Raw -Encoding UTF8
$parts = $source -split "// Legacy blocks\.", 2
if ($parts.Count -ne 2) {
    throw "Could not split item and block declarations"
}

function Get-LegacyNames([string]$text, [string[]]$helpers) {
    $helperPattern = ($helpers | ForEach-Object { [Regex]::Escape($_) }) -join "|"
    $matches = [Regex]::Matches($text, "(?:$helperPattern)\(\s*`"([^`"]+)`"")
    return $matches | ForEach-Object { $_.Groups[1].Value.ToLowerInvariant() } | Sort-Object -Unique
}

function Write-AsciiJson([string]$path, [string]$json) {
    $parent = Split-Path -Parent $path
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    [IO.File]::WriteAllText($path, $json + [Environment]::NewLine, [Text.Encoding]::ASCII)
}

$itemNames = Get-LegacyNames $parts[0] @("simple", "food", "missile", "deployable", "intent")
$blockNames = Get-LegacyNames $parts[1] @("deco", "block", "flag")
$itemModelRoot = Join-Path $assetRoot "models\item"
$blockModelRoot = Join-Path $assetRoot "models\block"
$blockStateRoot = Join-Path $assetRoot "blockstates"

foreach ($name in $itemNames) {
    $texture = if (Test-Path -LiteralPath (Join-Path $assetRoot "textures\items\$name.png")) {
        "wartecmod:items/$name"
    } else {
        "wartecmod:items/itemguidancesystemtier1"
    }
    $json = "{`n  `"parent`": `"item/generated`",`n  `"textures`": {`n    `"layer0`": `"$texture`"`n  }`n}"
    Write-AsciiJson (Join-Path $itemModelRoot "$name.json") $json
}

foreach ($name in $blockNames) {
    $texture = if (Test-Path -LiteralPath (Join-Path $assetRoot "textures\blocks\$name.png")) {
        "wartecmod:blocks/$name"
    } else {
        "wartecmod:blocks/blockarmorsteel"
    }
    $blockJson = "{`n  `"parent`": `"block/cube_all`",`n  `"textures`": {`n    `"all`": `"$texture`"`n  }`n}"
    $itemJson = "{`n  `"parent`": `"wartecmod:block/$name`"`n}"
    $stateJson = "{`n  `"variants`": {`n    `"normal`": { `"model`": `"wartecmod:$name`" }`n  }`n}"
    Write-AsciiJson (Join-Path $blockModelRoot "$name.json") $blockJson
    Write-AsciiJson (Join-Path $itemModelRoot "$name.json") $itemJson
    Write-AsciiJson (Join-Path $blockStateRoot "$name.json") $stateJson
}

Write-Output "Generated $($itemNames.Count) item models and $($blockNames.Count) block model sets."
