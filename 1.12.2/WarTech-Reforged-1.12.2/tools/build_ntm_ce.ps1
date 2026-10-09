param(
    [string]$NtmJar = (Join-Path $env:APPDATA '.minecraft\mods\NTM-CE-1.12.2-2.6.1.0.jar'),
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MixinBooterJar = (Join-Path $env:APPDATA '.minecraft\mods\!mixinbooter-10.7.jar')
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$generatedRoot = Join-Path $projectRoot 'build\compat-ce-src'
$sourceRoot = Join-Path $projectRoot 'src\main\java'
$jdk = $JavaHome
if ([string]::IsNullOrWhiteSpace($jdk)) {
    $jdk = Join-Path (Split-Path -Parent $projectRoot) 'toolchain\jdk8\jdk8u492-b09'
}

if (-not (Test-Path -LiteralPath $NtmJar)) {
    throw "NTM Community Edition JAR not found: $NtmJar"
}
if (-not (Test-Path -LiteralPath (Join-Path $jdk 'bin\java.exe'))) {
    throw "JDK 8 not found: $jdk. Pass -JavaHome or set JAVA_HOME."
}
if (-not (Test-Path -LiteralPath $mixinBooterJar)) {
    throw "MixinBooter 10.7 JAR not found: $mixinBooterJar"
}

$resolvedBuild = [IO.Path]::GetFullPath((Join-Path $projectRoot 'build'))
$resolvedGenerated = [IO.Path]::GetFullPath($generatedRoot)
if (-not $resolvedGenerated.StartsWith($resolvedBuild,
        [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to recreate generated sources outside build/: $resolvedGenerated"
}
if (Test-Path -LiteralPath $resolvedGenerated) {
    Remove-Item -LiteralPath $resolvedGenerated -Recurse -Force
}
New-Item -ItemType Directory -Path $resolvedGenerated | Out-Null
Copy-Item -Path (Join-Path $sourceRoot '*') -Destination $resolvedGenerated -Recurse

Get-ChildItem -LiteralPath $resolvedGenerated -Recurse -Filter '*.java' |
    ForEach-Object {
        $text = Get-Content -LiteralPath $_.FullName -Raw
        $text = $text.Replace('api.hbm.entity', 'com.hbm.api.entity')
        $text = $text.Replace('api.hbm.energy.IBatteryItem',
                'com.hbm.api.energymk2.IBatteryItem')
        $text = $text.Replace('.getDischargeRate()', '.getDischargeRate(stack)')
        [IO.File]::WriteAllText($_.FullName, $text,
                (New-Object Text.UTF8Encoding($false)))
    }

$poweredPath = Join-Path $resolvedGenerated 'com\wartec\wartecmod\port\integration\HbmPoweredTileEntity.java'
$taskEmpPath = Join-Path $resolvedGenerated 'com\wartec\wartecmod\port\integration\HbmExplosionCompat.java'
$taskEmpSource = Get-Content -LiteralPath $taskEmpPath -Raw
$taskEmpSource = $taskEmpSource.Replace('api.hbm.energy.IEnergyUser', 'com.hbm.api.energymk2.IEnergyReceiverMK2')
[IO.File]::WriteAllText($taskEmpPath, $taskEmpSource, (New-Object Text.UTF8Encoding($false)))
$powered = Get-Content -LiteralPath $poweredPath -Raw
$powered = $powered.Replace('import api.hbm.energy.IEnergyUser;',
        'import com.hbm.api.energymk2.IEnergyReceiverMK2;')
$powered = $powered.Replace('implements IEnergyUser, ITeamOwned',
        'implements IEnergyReceiverMK2, ITeamOwned')
$powered = $powered.Replace('public long transferPower(long offeredPower)',
        'public long transferPower(long offeredPower, boolean simulate)')
$powered = $powered.Replace('if (acceptedPower > 0L) {',
        'if (acceptedPower > 0L && !simulate) {')
$powered = $powered.Replace('updateStandardConnections(world, this);', @'
for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                trySubscribe(world, pos, direction);
            }
'@)
[IO.File]::WriteAllText($poweredPath, $powered,
        (New-Object Text.UTF8Encoding($false)))

$powerLinkPath = Join-Path $resolvedGenerated 'com\wartec\wartecmod\port\integration\HbmTilePowerLink.java'
$powerLink = Get-Content -LiteralPath $powerLinkPath -Raw
$powerLink = $powerLink.Replace('import api.hbm.energy.IEnergyConductor;',
        'import com.hbm.api.energymk2.IEnergyConductorMK2;')
$powerLink = $powerLink.Replace('import api.hbm.energy.IEnergyConnector;',
        'import com.hbm.api.energymk2.IEnergyReceiverMK2;')
$powerLink = $powerLink.Replace('IEnergyConnector receiver',
        'IEnergyReceiverMK2 receiver')
$powerLink = $powerLink.Replace('instanceof IEnergyConductor',
        'instanceof IEnergyConductorMK2')
[IO.File]::WriteAllText($powerLinkPath, $powerLink,
        (New-Object Text.UTF8Encoding($false)))

$explosionPath = Join-Path $resolvedGenerated 'com\wartec\wartecmod\port\integration\HbmExplosionCompat.java'
$explosion = Get-Content -LiteralPath $explosionPath -Raw
$explosion = $explosion.Replace('com.hbm.packet.AuxParticlePacketNT',
        'com.hbm.packet.toclient.AuxParticlePacketNT')
$explosion = $explosion.Replace(
        'ExplosionChaos.flameDeath(world, new BlockPos(x, y, z), radius);',
        'ExplosionChaos.flameDeath(world, null, new BlockPos(x, y, z), radius);')
$explosion = $explosion.Replace(
        'ExplosionChaos.burn(world, new BlockPos(x, y, z), radius);',
        'ExplosionChaos.burn(world, null, new BlockPos(x, y, z), radius);')
[IO.File]::WriteAllText($explosionPath, $explosion,
        (New-Object Text.UTF8Encoding($false)))

$missilePath = Join-Path $resolvedGenerated 'com\wartec\wartecmod\port\entity\EntityWarTechMissile.java'
$missile = Get-Content -LiteralPath $missilePath -Raw
$missile = [regex]::Replace($missile,
        'ExplosionLarge\.explode\(\s*world,',
        'ExplosionLarge.explode(world, this,')
$missile = [regex]::Replace($missile,
        'ExplosionLarge\.explodeFire\(\s*world,',
        'ExplosionLarge.explodeFire(world, this,')
[IO.File]::WriteAllText($missilePath, $missile,
        (New-Object Text.UTF8Encoding($false)))

$ballisticPath = Join-Path $resolvedGenerated 'com\wartec\wartecmod\port\content\BallisticLauncherBlock.java'
$ballistic = Get-Content -LiteralPath $ballisticPath -Raw
$ballistic = [regex]::Replace($ballistic,
        '(?s)    @Override\r?\n    public void explode\(World world, BlockPos pos\) \{.*?\r?\n    \}', @'
    @Override
    public IBomb.BombReturnCode explode(World world, BlockPos pos,
            net.minecraft.entity.Entity source) {
        if (world == null || world.isRemote) {
            return IBomb.BombReturnCode.UNDEFINED;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityWarTechMachine) {
            ((TileEntityWarTechMachine) tile).launchFromDetonator(
                source instanceof net.minecraft.entity.player.EntityPlayer ? (net.minecraft.entity.player.EntityPlayer)source : null);
            return IBomb.BombReturnCode.LAUNCHED;
        }
        return IBomb.BombReturnCode.ERROR_NO_BOMB;
    }
'@)
[IO.File]::WriteAllText($ballisticPath, $ballistic,
        (New-Object Text.UTF8Encoding($false)))

$legacyLauncherPath = Join-Path $resolvedGenerated 'com\wartec\wartecmod\port\content\LegacyLauncherBlock.java'
$legacyLauncher = Get-Content -LiteralPath $legacyLauncherPath -Raw
$legacyLauncher = [regex]::Replace($legacyLauncher,
        '(?s)    @Override\r?\n    public void explode\(World world, BlockPos pos\) \{.*?\r?\n    \}', @'
    @Override
    public IBomb.BombReturnCode explode(World world, BlockPos pos,
            net.minecraft.entity.Entity source) {
        if (world == null || world.isRemote) {
            return IBomb.BombReturnCode.UNDEFINED;
        }
        TileEntityWarTechMachine machine = findMachine(world, pos);
        if (machine != null) {
            machine.launchFromDetonator(
                source instanceof net.minecraft.entity.player.EntityPlayer ? (net.minecraft.entity.player.EntityPlayer)source : null);
            return IBomb.BombReturnCode.LAUNCHED;
        }
        return IBomb.BombReturnCode.ERROR_NO_BOMB;
    }
'@)
[IO.File]::WriteAllText($legacyLauncherPath, $legacyLauncher,
        (New-Object Text.UTF8Encoding($false)))

$recipePath = Join-Path $resolvedGenerated 'com\wartec\wartecmod\port\integration\WarTechRecipeRegistration.java'
$recipes = Get-Content -LiteralPath $recipePath -Raw
$recipes = [regex]::Replace($recipes,
        'import com\.hbm\.forgefluid\.ModForgeFluids;\r?\n',
        "import com.hbm.inventory.fluid.FluidType;`n" +
        "import com.hbm.inventory.fluid.Fluids;`n")
$recipes = [regex]::Replace($recipes,
        'import com\.hbm\.inventory\.AssemblerRecipes;\r?\n',
        "import com.hbm.inventory.recipes.AssemblyMachineRecipes;`n" +
        "import com.hbm.inventory.recipes.loader.GenericRecipe;`n")
$recipes = $recipes.Replace('import com.hbm.inventory.ShredderRecipes;',
        'import com.hbm.inventory.recipes.ShredderRecipes;')
$recipes = $recipes.Replace('ModBlocks.barricade', 'ModBlocks.barbed_wire')
$recipes = $recipes.Replace('ModForgeFluids.', 'Fluids.')
$recipes = $recipes.Replace('AssemblerRecipes.generateList();', '')
$recipes = $recipes.Replace('OreDictManager.CU.wire()',
        'OreDictManager.CU.wireFine()')
$recipes = [regex]::Replace($recipes,
        '(?s)    private static RecipesCommon\.NbtComparableStack fluidCells\(.*?\r?\n    \}', @'
    private static RecipesCommon.NbtComparableStack fluidCells(
        FluidType fluid,
        int count
    ) {
        return new RecipesCommon.NbtComparableStack(
                ItemCell.getFullCell(fluid, count));
    }
'@)
$recipes = [regex]::Replace($recipes,
        '(?s)    private static RecipesCommon\.NbtComparableStack fluidBarrel\(.*?\r?\n    \}', @'
    private static RecipesCommon.NbtComparableStack fluidBarrel(
            FluidType fluid) {
        return new RecipesCommon.NbtComparableStack(new ItemStack(
                ModItems.fluid_barrel_full, 1, fluid.getID()));
    }
'@)
$recipes = [regex]::Replace($recipes,
        '(?s)        AssemblerRecipes\.makeRecipe\(\r?\n            new RecipesCommon\.ComparableStack\(output, outputCount\),\r?\n            ingredients,\r?\n            duration\r?\n        \);', @'
        String path = output.getRegistryName() == null
                ? output.getClass().getSimpleName()
                : output.getRegistryName().getResourcePath();
        AssemblyMachineRecipes.INSTANCE.register(
                new GenericRecipe("wartec." + path)
                        .setup(duration, 100L)
                        .outputItems(new ItemStack(output, outputCount))
                        .inputItems(ingredients));
'@)
[IO.File]::WriteAllText($recipePath, $recipes,
        (New-Object Text.UTF8Encoding($false)))

$clientProxyPath = Join-Path $resolvedGenerated 'com\wartec\wartecmod\port\proxy\ClientProxy.java'
$clientProxy = Get-Content -LiteralPath $clientProxyPath -Raw
$clientProxy = [regex]::Replace($clientProxy,
        '(?s)                    com\.hbm\.main\.MainRegistry\.proxy\.spawnParticle\(\r?\n                            endX - dx \* inverse \* offset,\r?\n                            endY - dy \* inverse \* offset,\r?\n                            endZ - dz \* inverse \* offset,\r?\n                            "exKerosene", null\);', @'
                    minecraft.world.spawnParticle(
                            net.minecraft.util.EnumParticleTypes.SMOKE_LARGE,
                            endX - dx * inverse * offset,
                            endY - dy * inverse * offset,
                            endZ - dz * inverse * offset,
                            0.0D, 0.0D, 0.0D);
'@)
[IO.File]::WriteAllText($clientProxyPath, $clientProxy,
        (New-Object Text.UTF8Encoding($false)))

$satelliteNames = @('SatelliteEmp.java', 'SatelliteKinetic.java',
        'SatelliteNuclear.java')
foreach ($satelliteName in $satelliteNames) {
    $satellitePath = Join-Path $resolvedGenerated `
            "com\wartec\wartecmod\port\satellite\$satelliteName"
    $satellite = Get-Content -LiteralPath $satellitePath -Raw
    $satellite = $satellite.Replace(
            'import net.minecraft.entity.player.EntityPlayer;',
            "import net.minecraft.entity.player.EntityPlayer;`n" +
            'import net.minecraft.entity.player.EntityPlayerMP;')
    $satellite = $satellite.Replace(
            'public void onCoordAction(World world, EntityPlayer player,',
            'public void onCoordAction(World world, EntityPlayerMP player,')
    if ($satelliteName -eq 'SatelliteKinetic.java') {
        $satellite = $satellite.Replace(
                'public void onClick(World world, int x, int z) {`r`n' +
                '        releaseRod(world, findControllerPlayer(world), x, z);',
                'public void onClick(World world, EntityPlayerMP player, int x, int z) {`r`n' +
                '        releaseRod(world, player, x, z);')
        $satellite = $satellite.Replace(
                "public void onClick(World world, int x, int z) {`n" +
                '        releaseRod(world, findControllerPlayer(world), x, z);',
                "public void onClick(World world, EntityPlayerMP player, int x, int z) {`n" +
                '        releaseRod(world, player, x, z);')
    }
    $lastSatelliteBrace = $satellite.LastIndexOf('}')
    $colorMethod = @'
    @Override
    public float[] getColor() {
        return new float[] {0.25F, 0.80F, 1.00F};
    }

'@
    $satellite = $satellite.Insert($lastSatelliteBrace, $colorMethod)
    [IO.File]::WriteAllText($satellitePath, $satellite,
            (New-Object Text.UTF8Encoding($false)))
}

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
Push-Location $projectRoot
try {
    & .\gradlew.bat test build --no-daemon --console=plain `
        "-PntmJar=$NtmJar" `
        "-PmixinBooterJar=$mixinBooterJar" `
        "-PsourceRoot=$resolvedGenerated" `
        '-PntmEdition=Community Edition 2.6.1.0' `
        '-ParchiveBaseName=WarTech-Reforged-1.12.2-NTM-CE'
    if ($LASTEXITCODE -ne 0) {
        throw "NTM CE build failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
