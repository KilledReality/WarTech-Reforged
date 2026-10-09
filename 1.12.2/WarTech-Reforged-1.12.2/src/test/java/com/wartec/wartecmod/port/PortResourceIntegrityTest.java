package com.wartec.wartecmod.port;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wartec.wartecmod.port.content.PortItem;
import com.wartec.wartecmod.port.content.VariantItem;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.content.UavPartItem;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
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
import javax.imageio.ImageIO;
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
    @Test public void uavBlueprintUsesBlankAndSavedUserIcons() throws IOException {
        JsonObject blank=new JsonParser().parse(new String(Files.readAllBytes(ASSET_ROOT.resolve("models/item/uavblueprint.json")),StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("wartecmod:items/cruise_parts/cruise_blueprint_blank",blank.getAsJsonObject("textures").get("layer0").getAsString());
        JsonObject override=blank.getAsJsonArray("overrides").get(0).getAsJsonObject();
        assertEquals(1,override.getAsJsonObject("predicate").get("wartecmod:saved").getAsInt());
        assertEquals("wartecmod:item/uavblueprint_saved",override.get("model").getAsString());
        JsonObject saved=new JsonParser().parse(new String(Files.readAllBytes(ASSET_ROOT.resolve("models/item/uavblueprint_saved.json")),StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("wartecmod:items/uav_parts/uav_blueprint_saved",saved.getAsJsonObject("textures").get("layer0").getAsString());
    }
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

            if (item instanceof UavPartItem) {
                UavPartItem partItem = (UavPartItem) item;
                for (int metadata = 0; metadata < partItem.getVariantCount();
                        ++metadata) {
                    String modelName = "uavmodule_" + PortItem.safePath(
                            partItem.getVariantName(metadata));
                    verifyItemModel(modelName);
                    String model = new String(Files.readAllBytes(ASSET_ROOT
                            .resolve("models/item/" + modelName + ".json")),
                            StandardCharsets.UTF_8);
                    assertFalse("Custom UAV parts must not reuse aircraft assets",
                            model.contains("f16_tactical_aircraft")
                                    || model.contains("su27_tactical_aircraft"));
                    assertTrue("Custom UAV parts need dedicated component art",
                            model.contains("wartecmod:items/uav_parts/")
                                    || metadata==com.wartec.wartecmod.port.uav.UavPartDefinition.RACK_CRUISE.ordinal()
                                        && model.contains("\"elements\"") && model.contains("clean_composite_skin"));
                }
                for (int metadata = 0;
                        metadata < partItem.getVariantCount(); ++metadata) {
                    verifyTranslation(item, metadata);
                }
            } else if (item instanceof VariantItem
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
    public void customUavPartIconsStayCenteredInsideInventorySlots()
            throws IOException {
        Path directory = ASSET_ROOT.resolve("textures/items/uav_parts");
        int checked = 0;
        try (java.nio.file.DirectoryStream<Path> icons =
                Files.newDirectoryStream(directory, "*.png")) {
            for (Path icon : icons) {
                BufferedImage image = ImageIO.read(icon.toFile());
                assertNotNull("Unreadable UAV icon " + icon, image);
                int expectedSize = 256;
                assertEquals("UAV icon width " + icon, expectedSize,
                        image.getWidth());
                assertEquals("UAV icon height " + icon, expectedSize,
                        image.getHeight());
                assertTrue("UAV icon must retain transparency " + icon,
                        image.getColorModel().hasAlpha());
                int minX = expectedSize;
                int minY = expectedSize;
                int maxX = -1;
                int maxY = -1;
                for (int y = 0; y < expectedSize; ++y) {
                    for (int x = 0; x < expectedSize; ++x) {
                        if ((image.getRGB(x, y) >>> 24) >= 20) {
                            minX = Math.min(minX, x);
                            minY = Math.min(minY, y);
                            maxX = Math.max(maxX, x);
                            maxY = Math.max(maxY, y);
                        }
                        if (x == 0 || y == 0 || x == expectedSize - 1
                                || y == expectedSize - 1) {
                            assertEquals("UAV canvas edge must be transparent "
                                    + icon, 0, image.getRGB(x, y) >>> 24);
                        }
                    }
                }
                assertTrue("Empty UAV icon " + icon, maxX >= minX);
                int margin = 8;
                assertTrue("UAV icon touches horizontal slot edge " + icon,
                        minX >= margin && maxX < expectedSize - margin);
                assertTrue("UAV icon touches vertical slot edge " + icon,
                        minY >= margin && maxY < expectedSize - margin);
                assertTrue("UAV silhouette is too small inside its canvas "
                        + icon, Math.max(maxX - minX, maxY - minY)
                                >= expectedSize * 2 / 3);
                int centerTolerance = 4;
                assertTrue("UAV icon is not horizontally centered " + icon,
                        Math.abs(minX + maxX - (expectedSize - 1))
                                <= centerTolerance);
                assertTrue("UAV icon is not vertically centered " + icon,
                        Math.abs(minY + maxY - (expectedSize - 1))
                                <= centerTolerance);
                ++checked;
            }
        }
        assertEquals("27 original components, ventral rack and saved UAV blueprint", 29, checked);
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
                byte[] actual=Files.readAllBytes(portAsset);
                if(relative.equals("textures/models/storm_shadow/storm_shadow.png")) {
                    verifyApprovedStormRetouch(expected,actual);
                } else {
                    assertTrue("Changed dev66 asset " + relative,Arrays.equals(expected,actual));
                }
                ++compared;
            }
        }
        assertEquals("Unexpected dev66 binary resource surface",
                308, compared);
    }
    private static void verifyApprovedStormRetouch(byte[] original,byte[] edited) throws IOException {
        BufferedImage before=ImageIO.read(new java.io.ByteArrayInputStream(original));
        BufferedImage after=ImageIO.read(new java.io.ByteArrayInputStream(edited));
        assertEquals(1024,before.getWidth());assertEquals(1024,before.getHeight());
        assertEquals(before.getWidth(),after.getWidth());assertEquals(before.getHeight(),after.getHeight());
        int[][] regions={{232,37,49,39},{23,707,50,30},{245,239,195,94},{466,270,52,53},
            {546,232,92,115},{187,491,300,73},{516,465,101,117},{543,823,137,164}};
        int changed=0;
        for(int y=0;y<1024;y++) for(int x=0;x<1024;x++) {
            int a=before.getRGB(x,y),b=after.getRGB(x,y);
            assertEquals("Storm alpha changed",a>>>24,b>>>24);
            if(a==b) continue;
            boolean allowed=false;
            for(int[] r:regions) if(x>=r[0] && x<r[0]+r[2] && y>=r[1] && y<r[1]+r[3]) allowed=true;
            assertTrue("Storm pixel changed outside approved decal regions: "+x+","+y,allowed);changed++;
        }
        assertTrue("Approved national/political decal removal is missing",changed>0);
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
        assertTrue("VNT allocation must not retain empty air positions",
                explosion.contains("affected.add(sample.toImmutable())")
                        && explosion.contains(
                                "state.getMaterial() != Material.AIR"));
        assertTrue("VNT debris must only replace blocks destroyed by the blast",
                explosion.contains("for (BlockPos pos : destroyed)"));
        assertTrue("VNT entity damage must retain all seven cross nodes",
                explosion.contains("Vec3d[] nodes")
                        && explosion.contains("NODE_DISTANCE = 7.5D"));

        String hbmEffects = readSource("integration/HbmExplosionCompat.java");
        assertTrue("Physical blast debris must use its bounded dev66 count",
                hbmEffects.contains("rubbleFunction(effectSize)"));
        assertTrue("Physical blast shrapnel must use its bounded dev66 count",
                hbmEffects.contains("shrapnelFunction(effectSize)"));

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
                                "world, posX, posY, posZ, 5.0F, 2.0F, false"));
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

        String customUav = readSource("entity/EntityCustomUav.java");
        int detonationStart = customUav.indexOf("private void detonateWarhead()");
        int detonationEnd = customUav.indexOf(
                "public boolean processInitialInteract", detonationStart);
        String detonation = customUav.substring(detonationStart, detonationEnd);
        assertTrue("Custom UAV detonation needs a one-shot re-entry guard",
                detonation.contains("isDead || detonationStarted"));
        assertTrue("Custom UAV must be removed before its blast applies damage",
                detonation.indexOf("setDead();")
                        < detonation.indexOf("HbmExplosionCompat."));
        assertTrue("One-way link loss must enter uncontrolled descent",
                customUav.contains("setLegacyState(LOST_CONTROL)"));
        assertTrue("Reusable UAV link loss must retain return-home behavior",
                customUav.contains("remoteAirborne ? RETURN : READY"));
        assertTrue("Kamikaze UAVs must detonate on block or entity contact",
                customUav.contains("collidedVertically")
                        && customUav.contains("findImpactEntity(0.35D)"));
        assertTrue("Kamikaze mission must be selected by the installed warhead",
                customUav.contains("private boolean isKamikaze()")
                        && customUav.contains("payload.isWarhead()"));
        assertTrue("Custom UAV network positions need client interpolation",
                customUav.contains("setPositionAndRotationDirect")
                        && customUav.contains("updateClientInterpolation()"));
        assertTrue("Custom UAVs must expose an atomic chain launch entrypoint",
                customUav.contains("boolean launchFromChain(EntityPlayer player)"));
        assertTrue("Custom UAV missions must execute programmed waypoints",
                customUav.contains("tickMissionOutbound()")
                        && customUav.contains("advanceMission()"));

        String modularRenderer = readSource("client/CustomUavRenderer.java");
        assertFalse("Modular UAVs must not reuse Geran or MQ-9 geometry",
                modularRenderer.contains("renderGeran")
                        || modularRenderer.contains("renderMq9")
                        || modularRenderer.contains("models/geran")
                        || modularRenderer.contains("models/mq9"));
        assertTrue("All three custom airframe families need distinct geometry",
                modularRenderer.contains("renderOneWay()")
                        && modularRenderer.contains("renderRecon()")
                        && modularRenderer.contains("renderStrike()"));
        assertTrue("Integrated propulsion and external stores must remain visible",
                modularRenderer.contains("renderIntegratedPropulsion(build, frame")
                        && modularRenderer.contains("renderExternalStores"));
        assertTrue("Approved UAV families need their distinct world scales",
                modularRenderer.contains("glScalef(0.70F, 0.74F, 0.72F)")
                        && modularRenderer.contains(
                                "glScalef(1.15F, 1.08F, 1.02F)")
                        && modularRenderer.contains(
                                "glScalef(1.40F, 1.24F, 1.32F)"));
        assertTrue("Custom UAVs must use the clean seamless material",
                modularRenderer.contains("CLEAN_COMPOSITE_SKIN")
                        && modularRenderer.contains(
                                "custom_uav/clean_composite_skin.png")
                        && !modularRenderer.contains(
                                "custom_uav/composite_skin.png"));
        assertTrue("Each UAV family needs a legible fitted inventory scale",
                modularRenderer.contains("? 0.19F")
                        && modularRenderer.contains("? 0.15F : 0.125F"));
        assertTrue("Avionics and internal warheads must not become cuboid furniture",
                !modularRenderer.contains("renderModules(build, frame")
                        && modularRenderer.contains(
                                "frame != UavAirframe.STRIKE"));
        assertTrue("Heavy strike stores must use the wide approved airframe",
                modularRenderer.contains("VehicleDimensions.uavWingY(entity.getAirframeType(),slot)")
                        && modularRenderer.contains("VehicleDimensions.uavStore(entity.getAirframeType(),type,slot)")
                        && !modularRenderer.contains("payloadScale = 0.46F"));

        assertTrue("Custom UAV collision bodies must match the new families",
                customUav.contains("setSize(1.35F*scale, 0.55F*scale)")
                        && customUav.contains("setSize(3.25F*scale, 1.30F*scale)")
                        && customUav.contains("setSize(4.75F*scale, 1.65F*scale)"));

        String remoteClient = readSource("client/RemoteControlClient.java");
        assertTrue("Large UAV nose cameras must clear their airframes",
                remoteClient.contains(
                        "if (vehicleType == 6) return 3.20D")
                        && remoteClient.contains(
                                "if (vehicleType == 7) return 5.85D"));
        assertTrue("Large UAV chase cameras must frame their full wingspans",
                remoteClient.contains(
                        "if (vehicleType == 6) return -17.0D")
                        && remoteClient.contains(
                                "if (vehicleType == 7) return -28.0D"));

        String chain = readSource("integration/UavChainDetonator.java");
        assertTrue("UAV chains must retain NTM block bindings",
                chain.contains("ItemMultiDetonator.getLocations(stack)"));
        int chainLaunchStart = chain.indexOf("private static void launchChain");
        int chainLaunchEnd = chain.indexOf(
                "private static NBTTagCompound getOrCreateTag",
                chainLaunchStart);
        String chainLaunch = chain.substring(chainLaunchStart, chainLaunchEnd);
        assertTrue("Successful links must be removed, failed launches must remain retryable",
                chainLaunch.contains("retained.appendTag(link.copy())")
                    && chainLaunch.contains("setTag(LINKS_KEY,retained)")
                    && !chainLaunch.contains("loadChunk("));

        String guide = readSource("content/UavGuideBookItem.java");
        assertFalse("Custom guide items cannot use vanilla's item-gated openBook",
                guide.contains("player.openBook"));
        String clientProxy = readSource("proxy/ClientProxy.java");
        assertTrue("Custom guide must open the real 1.12 book GUI",
                clientProxy.contains("new net.minecraft.client.gui.GuiScreenBook"));

        String assembledUav = readSource("content/AssembledUavItem.java");
        assertTrue("Assembled UAVs must require a raised launch point",
                assembledUav.contains("UAV_LAUNCH_POINT"));
        assertTrue("Deployment must reject routes incompatible with the full build",
                assembledUav.contains("mission.isValidFor(build)"));

        String missionStation = readSource(
                "gameplay/TileEntityUavMissionStation.java");
        assertTrue("Mission station must persist routes on assembled UAV items",
                missionStation.contains("mission.writeToStack(stack)"));
        String missionPacket = readSource("network/UavMissionEditMessage.java");
        assertTrue("Mission editing must be applied on the server thread",
                missionPacket.contains("getServerWorld().addScheduledTask"));

        String sounds = new String(Files.readAllBytes(
                ASSET_ROOT.resolve("sounds.json")), StandardCharsets.UTF_8);
        assertFalse("Do not invent an asset for dev66's unresolved rocket_launch",
                sounds.contains("rocket_launch"));
    }

    @Test
    public void customUavObserveBuildsBoundedPersistentReconIntel()
            throws IOException {
        String uav = readSource("entity/EntityCustomUav.java");
        assertTrue("OBSERVE must survey terrain and detect contacts",
                uav.contains("performReconObservation")
                        && uav.contains("surveyTerrain")
                        && uav.contains("scanReconContacts"));
        assertTrue("Recon survey must never force unloaded chunks",
                uav.contains("world.isBlockLoaded(samplePos)"));
        assertTrue("Recovered UAV items must retain their recon report",
                uav.contains("reconReport.writeToStack(recovery)"));

        String report = readSource("uav/UavReconReport.java");
        assertTrue("Terrain coverage must remain bounded",
                report.contains("MAX_CELLS = 768")
                        && report.contains("trimOldest(cells, MAX_CELLS)"));
        assertTrue("Stored contacts must remain bounded",
                report.contains("MAX_CONTACTS = 96")
                        && report.contains(
                                "trimOldest(contacts, MAX_CONTACTS)"));

        String station = readSource("client/GuiUavMissionStation.java");
        assertTrue("Mission Station must expose its recon map",
                station.contains("RECON MAP")
                        && station.contains("drawReconMap()"));
        assertTrue("Recon contacts must feed strike planning",
                station.contains("loadContactAt")
                        && station.contains("UavWaypointMode.STRIKE"));

        String reportViewer = readSource("client/GuiUavReconReport.java");
        assertTrue("Downloaded recon data needs a dedicated readable viewer",
                reportViewer.contains("SURVEY ")
                        && reportViewer.contains("HOSTILE ")
                        && reportViewer.contains("drawContactList()"));
        String reportItem = readSource("content/UavReconReportItem.java");
        assertTrue("Downloaded recon data must persist in a portable item",
                reportItem.contains("UavReconReport.fromStack(stack)")
                        && reportItem.contains("writeToStack(stack)")
                        && reportItem.contains("openUavReconReport"));
        assertTrue("The UAV must give the downloaded report to the player",
                uav.contains("UAV_RECON_REPORT.createReport(")
                        && uav.contains("addItemStackToInventory(report)"));
        assertTrue("Recon downloads must not fall back to chat spam",
                uav.contains("new UavReconReportMessage(")
                        && !uav.contains("Recon report: "));

        String tracking = readSource("network/MissileTrackingService.java");
        assertTrue("Live recon contacts must enter the command network",
                tracking.contains("reportReconContact")
                        && tracking.contains("reconSeen"));
    }

    @Test
    public void customUavVisualsUseDedicatedHighResolutionMaterials()
            throws IOException {
        Path skin = ASSET_ROOT.resolve(
                "textures/models/custom_uav/clean_composite_skin.png");
        BufferedImage skinImage = ImageIO.read(skin.toFile());
        assertNotNull("Unreadable custom UAV composite skin", skinImage);
        assertTrue("Custom UAV skin must retain useful surface detail",
                skinImage.getWidth() >= 256 && skinImage.getHeight() >= 256);
        Path tb2Skin = ASSET_ROOT.resolve(
                "textures/models/custom_uav/tb2_albedo.png");
        BufferedImage tb2SkinImage = ImageIO.read(tb2Skin.toFile());
        assertNotNull("Unreadable TB2 albedo", tb2SkinImage);
        assertTrue("TB2 albedo must retain the supplied UV detail",
                tb2SkinImage.getWidth() >= 2048
                        && tb2SkinImage.getHeight() >= 2048);

        for (String path : Arrays.asList(
                "textures/blocks/uav_fabricator_front.png",
                "textures/blocks/uav_mission_programmer_front.png",
                "textures/blocks/uav_launch_pad_top.png")) {
            BufferedImage image = ImageIO.read(
                    ASSET_ROOT.resolve(path).toFile());
            assertNotNull("Unreadable UAV infrastructure texture " + path,
                    image);
            assertTrue("UAV infrastructure texture is too small " + path,
                    image.getWidth() >= 128 && image.getHeight() >= 128);
        }

        String renderer = readSource("client/CustomUavRenderer.java");
        assertTrue("Custom UAV entities must bind their composite material",
                renderer.contains("CLEAN_COMPOSITE_SKIN")
                        && renderer.contains("bindTexture"));
        assertTrue("Custom UAV inventory previews must use measurable OBJ geometry",
                renderer.contains("new LegacyObjModel(\"models/custom_uav/one_way.obj\")")
                        && renderer.contains("new LegacyObjModel(\"models/custom_uav/recon.obj\")")
                        && renderer.contains("new LegacyObjModel(\"models/custom_uav/tb2.obj\")")
                        && renderer.contains("TB2_ALBEDO"));
        for (String model : Arrays.asList("one_way.obj", "recon.obj",
                "tb2.obj")) {
            Path path = ASSET_ROOT.resolve("models/custom_uav/" + model);
            assertTrue("Missing detailed custom UAV model " + model,
                    Files.isRegularFile(path));
            long vertices;
            long faces;
            try (java.util.stream.Stream<String> lines = Files.lines(path)) {
                vertices = lines.filter(line -> line.startsWith("v ")).count();
            }
            try (java.util.stream.Stream<String> lines = Files.lines(path)) {
                faces = lines.filter(line -> line.startsWith("f ")).count();
            }
            long minimumVertices = model.equals("tb2.obj") ? 3800 : 6000;
            long minimumFaces = model.equals("tb2.obj") ? 7000 : 12000;
            assertTrue("Custom UAV model is still visibly low polygon: " + model,
                    vertices >= minimumVertices && faces >= minimumFaces);
        }

        for (String icon : Arrays.asList("engine_economy.png",
                "engine_balanced.png", "engine_heavy.png")) {
            BufferedImage image = ImageIO.read(ASSET_ROOT.resolve(
                    "textures/items/uav_parts/" + icon).toFile());
            assertNotNull("Unreadable UAV engine icon " + icon, image);
            assertEquals(256, image.getWidth());
            assertEquals(256, image.getHeight());
            assertTrue("UAV engine icon must retain transparency " + icon,
                    image.getColorModel().hasAlpha());
        }
    }

    @Test
    public void customUavWorkstationsAndServicePanelKeepTypedContracts()
            throws IOException {
        String fabricator = readSource("gui/ContainerUavFabricator.java");
        assertTrue("Fabricator shift-click must target the matching part slot",
                fabricator.contains("moveOneToExactSlot")
                        && fabricator.contains("getSlot() == expected"));

        String station = readSource("gameplay/TileEntityUavMissionStation.java");
        assertTrue("Mission station must expose a dedicated designator slot",
                station.contains("DESIGNATOR_SLOT = 1")
                        && station.contains("DesignatorCompat.getTarget")
                        && station.contains("action == 3"));

        String stationGui = readSource("client/GuiUavMissionStation.java");
        assertTrue("Mission station must draw the actual container grid",
                stationGui.contains("for (Slot slot : inventorySlots.inventorySlots)"));
        assertTrue("Mission station needs an explicit designator confirmation",
                stationGui.contains("PROGRAM / OK"));

        String entity = readSource("entity/EntityCustomUav.java");
        assertTrue("Custom UAV must open the aircraft service GUI",
                entity.contains("WarTechGuiHandler.GUI_MQ9"));
        assertTrue("Recon data must be downloadable without dismantling",
                entity.contains("sendReconSummary")
                        && entity.contains("action == 6"));
        assertTrue("Loaded LTC must feed custom UAV countermeasures",
                entity.contains("consumeLoadedFlare")
                        && entity.contains("DEFENSE_FLARES"));
    }

    private static void verifyItemModel(String modelName) throws IOException {
        Path modelPath = ASSET_ROOT.resolve("models/item/" + modelName + ".json");
        assertTrue("Missing item model " + modelPath, Files.isRegularFile(modelPath));

        JsonObject model = parseObject(modelPath);
        if(model.has("elements")) {
            assertEquals("Only dedicated ventral rack uses native item geometry","uavmodule_rack_cruise",modelName);
            assertEquals(4,model.getAsJsonArray("elements").size());
            String metal=model.getAsJsonObject("textures").get("metal").getAsString();
            assertTrue(Files.isRegularFile(resolveTexture(metal)));
            for(com.google.gson.JsonElement element:model.getAsJsonArray("elements")) {
                JsonObject e=element.getAsJsonObject();
                assertEquals(3,e.getAsJsonArray("from").size());assertEquals(3,e.getAsJsonArray("to").size());
                for(int axis=0;axis<3;axis++) {
                    assertTrue(e.getAsJsonArray("from").get(axis).getAsDouble()>=0);
                    assertTrue(e.getAsJsonArray("to").get(axis).getAsDouble()<=16);
                    assertTrue(e.getAsJsonArray("to").get(axis).getAsDouble()>e.getAsJsonArray("from").get(axis).getAsDouble());
                }
                assertEquals(6,e.getAsJsonObject("faces").entrySet().size());
                for(java.util.Map.Entry<String,com.google.gson.JsonElement> face:e.getAsJsonObject("faces").entrySet())
                    assertEquals("#metal",face.getValue().getAsJsonObject().get("texture").getAsString());
            }
            return;
        }
        String parent = model.get("parent").getAsString();
        if ("builtin/entity".equals(parent)) {
            return;
        }
        if ("item/generated".equals(parent)) {
            String texture = model.getAsJsonObject("textures")
                .get("layer0")
                .getAsString();
            if (texture.startsWith("minecraft:")) {
                return;
            }
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
