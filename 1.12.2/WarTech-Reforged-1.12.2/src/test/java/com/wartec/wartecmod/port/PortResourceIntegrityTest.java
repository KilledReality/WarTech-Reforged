package com.wartec.wartecmod.port;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wartec.wartecmod.port.content.PortItem;
import com.wartec.wartecmod.port.content.VariantItem;
import com.wartec.wartecmod.port.content.WarTechContent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.block.Block;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PortResourceIntegrityTest {
    private static final Path ASSET_ROOT = Paths.get(
        "src", "main", "resources", "assets", PortItem.MOD_ID
    );

    private static Set<String> englishKeys;
    private static Set<String> russianKeys;

    @BeforeClass
    public static void loadResources() throws IOException {
        Bootstrap.register();
        englishKeys = loadLangKeys(ASSET_ROOT.resolve("lang/en_us.lang"));
        russianKeys = loadLangKeys(ASSET_ROOT.resolve("lang/ru_ru.lang"));
    }

    @Test
    public void everyCatalogItemHasResolvableLegacyModelAndTranslation()
        throws IOException {
        for (Item item : WarTechContent.getItems()) {
            assertNotNull(item.getRegistryName());
            String registryPath = item.getRegistryName().getResourcePath();

            if (item instanceof VariantItem
                && ((VariantItem) item).getVariantCount() > 1) {
                VariantItem variantItem = (VariantItem) item;
                for (int metadata = 0;
                     metadata < variantItem.getVariantCount();
                     metadata++) {
                    String variant = PortItem.safePath(
                        variantItem.getVariantName(metadata)
                    );
                    verifyItemModel(registryPath + "_" + variant);
                    verifyTranslation(item, metadata);
                }
            } else {
                verifyItemModel(registryPath);
                verifyTranslation(item, 0);
            }
        }
    }

    @Test
    public void everyBlockHasResolvableModelAndTranslation()
        throws IOException {
        for (Block block : WarTechContent.getBlocks()) {
            assertNotNull(block.getRegistryName());
            String registryPath = block.getRegistryName().getResourcePath();

            Path blockState = ASSET_ROOT.resolve(
                "blockstates/" + registryPath + ".json"
            );
            Path blockModel = ASSET_ROOT.resolve(
                "models/block/" + registryPath + ".json"
            );
            Path itemModel = ASSET_ROOT.resolve(
                "models/item/" + registryPath + ".json"
            );
            assertTrue("Missing blockstate " + blockState, Files.isRegularFile(blockState));
            assertTrue("Missing block model " + blockModel, Files.isRegularFile(blockModel));
            assertTrue("Missing block item model " + itemModel, Files.isRegularFile(itemModel));

            JsonObject model = parseObject(blockModel);
            if (model.has("textures")
                && model.getAsJsonObject("textures").has("all")) {
                String texture = model.getAsJsonObject("textures")
                    .get("all")
                    .getAsString();
                if (texture.startsWith(PortItem.MOD_ID + ":")) {
                    assertTrue(
                        "Missing block texture " + texture,
                        Files.isRegularFile(resolveTexture(texture))
                    );
                } else {
                    assertTrue(
                        "Shared block texture must be a Minecraft resource",
                        texture.startsWith("minecraft:")
                    );
                }
            }

            String key = block.getUnlocalizedName() + ".name";
            assertTrue("Missing en_us key " + key, englishKeys.contains(key));
            assertTrue("Missing ru_ru key " + key, russianKeys.contains(key));
        }
    }

    @Test
    public void exactLegacyObjAndTextureBindingsArePackaged() {
        for (String path : Arrays.asList(
            "models/geran/geran2.obj",
            "models/geran/geran_catapult.obj",
            "models/patriot/patriot_launcher.obj",
            "models/s400/s400_launcher.obj",
            "models/network/strategic_radar.obj",
            "models/radar/renault_trm_radar.obj",
            "models/strategic/tu95_bear.obj",
            "textures/models/geran/geran2.png",
            "textures/models/patriot/mim-104_d.png",
            "textures/models/s400/t_s400_truck_01_01_a.png",
            "textures/models/network/strategic_radar.png",
            "textures/items/designator_arty_range.png",
            "textures/items/legacy_hbm_radar_linker.png",
            "textures/items/legacy_hbm_flare_supply.png"
        )) {
            assertTrue(
                "Missing exact legacy renderer asset " + path,
                Files.isRegularFile(ASSET_ROOT.resolve(path))
            );
        }
        assertFalse(
            "Generated placeholder asset script must not return",
            Files.exists(Paths.get("tools", "generate_port_assets.py"))
        );
    }

    @Test
    public void everyOriginalBinaryAssetRemainsByteIdentical()
            throws IOException {
        Path originalJar = Paths.get("..", "..", "build",
                "WarTech-Reforged-1.6.0-dev66-iff-save.jar");
        assertTrue("Missing dev66 audit baseline " + originalJar,
                Files.isRegularFile(originalJar));
        int compared = 0;
        Set<String> normalizedPaths = new HashSet<>();
        try (ZipFile original = new ZipFile(originalJar.toFile())) {
            Enumeration<? extends ZipEntry> entries = original.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()
                        || !name.startsWith("assets/wartecmod/")
                        || !isBinaryLegacyAsset(name)) {
                    continue;
                }
                String relative = name.substring(
                        "assets/wartecmod/".length())
                        .toLowerCase(Locale.ROOT);
                assertTrue("Legacy resource path collision: " + relative,
                        normalizedPaths.add(relative));
                Path portAsset = ASSET_ROOT.resolve(relative);
                assertTrue("Missing exact dev66 asset " + relative,
                        Files.isRegularFile(portAsset));
                byte[] expected;
                try (InputStream stream = original.getInputStream(entry)) {
                    expected = readAllBytes(stream);
                }
                assertTrue("Changed dev66 asset " + relative,
                        Arrays.equals(expected,
                                Files.readAllBytes(portAsset)));
                ++compared;
            }
        }
        assertEquals("Unexpected dev66 binary resource surface",
                308, compared);
    }

    @Test
    public void soundsUseWarTechNamespaceAndExistingOggFiles()
        throws IOException {
        JsonObject sounds = parseObject(ASSET_ROOT.resolve("sounds.json"));
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry
            : sounds.entrySet()) {
            String eventName = entry.getKey();
            JsonObject event = entry.getValue().getAsJsonObject();
            String soundName = event.getAsJsonArray("sounds")
                .get(0)
                .getAsJsonObject()
                .get("name")
                .getAsString();
            assertTrue(
                "Sound must use the WarTech namespace: " + soundName,
                soundName.startsWith(PortItem.MOD_ID + ":")
            );
            String path = soundName.substring(soundName.indexOf(':') + 1);
            assertTrue(
                "Missing OGG for " + soundName,
                Files.isRegularFile(
                    ASSET_ROOT.resolve("sounds/" + path + ".ogg")
                )
            );
        }
    }

    @Test
    public void dev17ParityGuardsRemainInSource() throws IOException {
        String launcher = readSource(
                "gameplay/TileEntityWarTechMachine.java");
        assertTrue("Ballistic launch velocity must survive launcher setup",
                launcher.contains("!= MissileProfile.FlightClass.BALLISTIC"));
        assertTrue("Glide launch velocity must survive launcher setup",
                launcher.contains("!= MissileProfile.FlightClass.GLIDE"));

        String ordnance = readSource("entity/EntityWarTechOrdnance.java");
        assertTrue("Aircraft impacts must retain dev66 fire/block semantics",
                ordnance.contains("radius, causesFire, true"));

        String explosion = readSource("integration/LegacyVntExplosion.java");
        assertTrue("VNT allocator must retain dev66 resolution",
                explosion.contains("BLOCK_RESOLUTION = 48"));
        assertTrue("VNT entity damage must retain all seven cross nodes",
                explosion.contains("Vec3d[] nodes")
                        && explosion.contains("NODE_DISTANCE = 7.5D"));

        String satellite = readSource("satellite/SatelliteEmp.java");
        assertTrue("EMP satellite must retain the original EntityCloudTom cloud",
                satellite.contains("new EntityCloudTom(world, 1000)"));

        String renderer = readSource("client/LegacyRenderLibrary.java");
        assertTrue("1.12 GUI models must be measured before rendering",
                renderer.contains("fitItemToGuiSlot")
                        && renderer.contains("beginMeasurement"));
        assertTrue("Measured models must be normalized to one GUI slot",
                renderer.contains("0.82F / bounds.largestSize()"));
        assertTrue("TEISR geometry must be centered in the 1.12 item cube",
                renderer.contains("GL11.glTranslatef(0.5F, 0.5F, 0.5F)"));
        assertTrue("GUI OBJ textures must use the base texture unit instead of the lightmap or atlas",
                renderer.contains("OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit)")
                        && renderer.contains("GL13.GL_ACTIVE_TEXTURE"));
        assertTrue("GUI OBJ rendering must isolate and restore the inherited texture matrix",
                renderer.contains("GL11.glMatrixMode(GL11.GL_TEXTURE)")
                        && renderer.contains("int previousMatrixMode")
                        && renderer.contains("GL11.glLoadIdentity()"));
        assertTrue("Legacy rendering must not roll texture bindings back behind GlStateManager",
                renderer.contains("LEGACY_ATTRIB_MASK = 24833")
                        && !renderer.contains("glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)"));
        assertTrue("The bounds-only pass must never mutate Minecraft's texture cache",
                renderer.contains("LegacyObjModel.isMeasuring()"));
        String objModel = readSource("client/LegacyObjModel.java");
        assertTrue("OBJ measurement state must be exposed to the texture binder",
                objModel.contains("static boolean isMeasuring()")
                        && objModel.contains("return measurement != null"));
        assertTrue("Per-context transforms must remain active",
                renderer.contains("ItemCameraTransforms.TransformType"));

        assertTrue("Original alternate takeoff sound must be packaged",
                Files.isRegularFile(ASSET_ROOT.resolve(
                        "sounds/weapon/missile_takeoff_alt.ogg")));
    }

    @Test
    public void dev19FirstSevenParityGuardsRemainInSource()
            throws IOException {
        String missile = readSource("entity/EntityWarTechMissile.java");
        assertTrue("Cruise stage must remain synchronized",
                missile.contains("DataParameter<Integer> FLIGHT_STAGE"));
        assertTrue("Cruise deployment must transition to stage two",
                missile.contains("this.dataManager.set(FLIGHT_STAGE, 2)"));
        assertTrue("LRHW separation must remove its booster",
                missile.contains("this.dataManager.set(FLIGHT_STAGE, 0)"));

        String renderer = readSource("client/LegacyRenderLibrary.java");
        assertTrue("LRHW booster must retain the original translated mount",
                renderer.contains("GL11.glTranslated(0.0D, -8.46875D, 0.0D)"));
        assertTrue("Kalibr folded fins must remain stage dependent",
                renderer.contains("entity_kalibr_missile_fins_folded.obj"));

        String fixer = readSource("integration/LegacyNbtDataFixer.java");
        assertTrue("Legacy field migration must run as schema version three",
                fixer.contains("return 3;"));
        assertTrue("Legacy missile specifications must be restored",
                fixer.contains("WarTechMissileSpec"));
        assertTrue("Legacy inventory slot compounds must be migrated",
                fixer.contains("\"InventorySlot\" + slot"));

        String item = readSource("content/MissileItem.java");
        assertTrue("VLS items must expose the dev66 spawning contract",
                item.contains("implements com.wartec.wartecmod.items.IMissileSpawningItem"));

        String artillery = readSource(
                "entity/EntityWarTechArtilleryProjectile.java");
        assertTrue("Greg clusters must retain their original split effect",
                artillery.contains("artilleryClusterSplit"));
        assertTrue("Artillery impacts must use their no-SFX VNT profile",
                artillery.contains("artilleryExplosion("));
    }

    @Test
    public void dev20RemainingSixParityGuardsRemainInSource()
            throws IOException {
        String missile = readSource("entity/EntityWarTechMissile.java");
        assertTrue("Connected VLS exhaust must receive its own plume",
                missile.contains("vlsExhaustY + 11.0D")
                        && missile.contains("world, x, y, z, 2"));
        assertTrue("Fragmentation must retain its non-destructive blast",
                missile.contains("case FRAGMENTATION:")
                        && missile.contains(
                                "world, posX, posY, posZ, 20.0F, 2.0F, false"));
        assertTrue("Invalid specifications must be removed, not flown",
                missile.contains(
                        "getMissileSpecification() == LegacyMissileSpecification.INVALID")
                        && missile.contains("setDead();"));

        String specification =
                readSource("entity/LegacyMissileSpecification.java");
        assertTrue("Malformed ordinals must use the invalid sentinel",
                specification.contains(
                        "ordinal < values.length ? values[ordinal] : INVALID"));
        assertFalse("Malformed profiles must never become Storm Shadow",
                specification.contains(
                        "specification == null ? STORM_SHADOW"));

        String ground =
                readSource("entity/EntityWarTechGroundVehicle.java");
        assertTrue("Ground vehicles must retain the minecart.base loop",
                ground.contains("SoundEvents.ENTITY_MINECART_RIDING")
                        && ground.contains("ticksExisted % 24 == 0"));

        String aircraft = readSource("entity/EntityWarTechAircraft.java");
        assertTrue("Unguided MQ-9 stores must retain random.pop",
                aircraft.contains("SoundEvents.ENTITY_ITEM_PICKUP"));
        assertFalse("Chicken egg replacement must not return",
                aircraft.contains("SoundEvents.ENTITY_CHICKEN_EGG"));

        String recipes =
                readSource("integration/WarTechRecipeRegistration.java");
        assertTrue("Only the 24 valid dev66 assembler recipes may be counted",
                recipes.contains("ASSEMBLER_RECIPE_COUNT = 24"));
        assertTrue("Removed mechanism must use the NTM 3.0.3 equivalent",
                recipes.contains("legacyMechanism(2)"));
        assertTrue("Removed fine wire must use the copper wire form",
                recipes.contains("legacyFineCopperWire(6)"));

        String launcher =
                readSource("gameplay/TileEntityWarTechMachine.java");
        assertTrue("VLS exhaust lookup must retain the original 30-block BFS",
                launcher.contains("current[2] >= 30")
                        && launcher.contains("findConnectedVlsExhaust()"));
    }

    @Test
    public void dev21ModelAndLegacyRecoveryGuardsRemainInSource()
            throws IOException {
        String models = readSource("client/ClientModelEvents.java");
        assertTrue("Launcher metadata must resolve to the shared legacy model",
                models.contains("ignore(LegacyLauncherBlock.META)"));
        assertTrue("Decoration facing must resolve to the TESR-backed model",
                models.contains("ignore(LegacyDecorationBlock.FACING)"));

        String ordnance = readSource("entity/EntityWarTechOrdnance.java");
        assertTrue("Already-converted ordnance must infer missing family data",
                ordnance.contains("inferLegacyFamily(compound, type)"));
        assertTrue("Legacy AAM target IDs must remain recoverable",
                ordnance.contains("compound.getInteger(\"AirTargetId\")"));

        String aircraft = readSource("entity/EntityWarTechAircraft.java");
        assertTrue("Already-converted aircraft must retain old launch state",
                aircraft.contains(
                        "\"WarTechLaunchCooldown\", \"LaunchCooldown\""));
        assertTrue("Already-converted aircraft must retain old home yaw",
                aircraft.contains("\"WarTechHomeYaw\", \"HomeYaw\""));
    }

    @Test
    public void dev18FinalAuditGuardsRemainInSource() throws IOException {
        String renderer = readSource("client/LegacyRenderLibrary.java");
        int fallbackStart = renderer.indexOf(
                "private static void renderFallbackProfile");
        int fallbackEnd = renderer.indexOf(
                "private static void renderAdvancedMissile", fallbackStart);
        assertTrue(fallbackStart >= 0 && fallbackEnd > fallbackStart);
        String fallbackRenderer = renderer.substring(
                fallbackStart, fallbackEnd);
        assertFalse("Unknown visuals must never become Storm Shadow",
                fallbackRenderer.contains("storm_shadow"));
        assertTrue("Unknown visuals must report a hard renderer gap",
                renderer.contains("No dev66 renderer binding"));
        assertTrue("MQ-9 wreck attitude must match dev66",
                renderer.contains("entity.getLegacyState() == 6"));
        assertTrue("Tu-95 wreck attitude must match dev66",
                renderer.contains("entity.getLegacyState() == 8"));

        String migration = readSource("integration/LegacyNbtDataFixer.java");
        assertTrue("Entity NBT IDs need a Forge data fixer",
                migration.contains("FixTypes.ENTITY"));
        assertTrue("Tile NBT IDs need a Forge data fixer",
                migration.contains("FixTypes.BLOCK_ENTITY"));

        String missile = readSource("entity/EntityWarTechMissile.java");
        assertTrue("ASAT must call NTM satellite data directly",
                missile.contains("SatelliteSavedData.getData(world)"));
        assertFalse("ASAT must not silently swallow NTM failures",
                missile.contains("NTM Extended satellite internals are optional"));

        String explosion = readSource("integration/HbmExplosionCompat.java");
        assertTrue("Neutron contamination must call NTM directly",
                explosion.contains("ContaminationUtil.contaminate"));
        assertFalse("Neutron contamination must not silently degrade",
                explosion.contains("Class.forName(\"com.hbm.util.ContaminationUtil\")"));

        String recipes = readSource("integration/WarTechRecipeRegistration.java");
        assertTrue("Three U-238 ingots must produce two plates",
                recipes.contains("ITEM_PLATE_U238, 2, 30"));
        assertFalse("Broken strong interceptor output must not be redirected",
                recipes.contains(
                        "assembler(WarTechContent.ITEM_MISSILE_ANTI_BALLISTIC_NUCLEAR, 350"));

        String blocks = readSource("content/WarTechContent.java");
        assertTrue("Reinforced wood must keep dev66 default hardness",
                blocks.contains(
                        "BlockReinforcedWood\", Material.WOOD, WarTechCreativeTabs.BLOCKS, 0.0F, 5.0F"));
        assertTrue("Decoration blocks must use metadata-facing behavior",
                blocks.contains("new LegacyDecorationBlock"));

        String sounds = new String(Files.readAllBytes(
                ASSET_ROOT.resolve("sounds.json")), StandardCharsets.UTF_8);
        assertFalse("Do not invent an asset for dev66's unresolved rocket_launch",
                sounds.contains("rocket_launch"));
    }

    private static void verifyItemModel(String modelName) throws IOException {
        Path modelPath = ASSET_ROOT.resolve("models/item/" + modelName + ".json");
        assertTrue("Missing item model " + modelPath, Files.isRegularFile(modelPath));

        JsonObject model = parseObject(modelPath);
        String parent = model.get("parent").getAsString();
        if ("builtin/entity".equals(parent)) {
            return;
        }
        if ("item/generated".equals(parent)) {
            String texture = model.getAsJsonObject("textures")
                .get("layer0")
                .getAsString();
            assertTrue(
                "Missing item texture " + texture,
                Files.isRegularFile(resolveTexture(texture))
            );
            return;
        }
        assertTrue(
            "Unexpected item model parent " + parent,
            parent.startsWith(PortItem.MOD_ID + ":block/")
        );
        assertTrue(
            "Missing parent block model " + parent,
            Files.isRegularFile(ASSET_ROOT.resolve(
                "models/" + parent.substring(parent.indexOf(':') + 1) + ".json"
            ))
        );
    }

    private static void verifyTranslation(Item item, int metadata) {
        String key = item.getUnlocalizedName(new ItemStack(item, 1, metadata))
            + ".name";
        assertTrue("Missing en_us key " + key, englishKeys.contains(key));
        assertTrue("Missing ru_ru key " + key, russianKeys.contains(key));
    }

    private static Path resolveTexture(String resourceName) {
        String value = resourceName;
        int separator = value.indexOf(':');
        if (separator >= 0) {
            assertEquals(PortItem.MOD_ID, value.substring(0, separator));
            value = value.substring(separator + 1);
        }
        return ASSET_ROOT.resolve("textures/" + value + ".png");
    }

    private static boolean isBinaryLegacyAsset(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".obj")
                || lower.endsWith(".mtl") || lower.endsWith(".ogg");
    }

    private static byte[] readAllBytes(InputStream stream)
            throws IOException {
        java.io.ByteArrayOutputStream output =
                new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) >= 0) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static JsonObject parseObject(Path path) throws IOException {
        try (java.io.Reader reader = Files.newBufferedReader(
            path,
            StandardCharsets.UTF_8
        )) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    private static String readSource(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(
                "src", "main", "java", "com", "wartec", "wartecmod",
                "port", relativePath.replace('/', java.io.File.separatorChar))),
                StandardCharsets.UTF_8);
    }

    private static Set<String> loadLangKeys(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Set<String> keys = new HashSet<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            assertFalse("Malformed lang line: " + line, separator < 1);
            keys.add(line.substring(0, separator));
        }
        return keys;
    }
}
