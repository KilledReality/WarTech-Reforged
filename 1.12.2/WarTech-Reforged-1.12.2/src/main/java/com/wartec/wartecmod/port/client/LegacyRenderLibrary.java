package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile;
import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import com.wartec.wartecmod.port.entity.EntityStrategicTel;
import com.wartec.wartecmod.port.entity.EntityStrategicMissile;
import com.wartec.wartecmod.port.entity.StrategicSystemProfile;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.entity.EntityCustomUav;
import com.wartec.wartecmod.port.uav.UavAirframe;
import com.wartec.wartecmod.port.uav.UavBuild;
import com.wartec.wartecmod.port.content.MissileItem;
import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

/**
 * Exact 1.7.10 model/texture bindings with their original presentation
 * transforms, adapted to the generic 1.12 entity families.
 */
final class LegacyRenderLibrary {
    // Original dev66 renderers use 24833 and deliberately exclude GL_TEXTURE_BIT.
    private static final int LEGACY_ATTRIB_MASK = 24833;
    private static final Map<String, LegacyObjModel> MODELS = new HashMap<>();
    private static final Map<String, LegacyObjModel.Bounds> GUI_BOUNDS =
            new java.util.LinkedHashMap<String, LegacyObjModel.Bounds>(64, .75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, LegacyObjModel.Bounds> entry) {
                    return size() > 256;
                }
            };
    static void reloadPreviews() { GUI_BOUNDS.clear(); LegacyObjModel.reloadAll(); }
    /** Prepare known inventory geometry during resource loading, never on opening a tab.
     * Uses the same render path/cache as real items, with framebuffer writes suppressed.
     * No world, entities, mission scans or new thumbnail textures are allocated. */
    static void warmItemPreviews() {
        int matrixMode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        // Never restore GL_TEXTURE_BIT behind Minecraft's texture-binding cache.
        GL11.glPushAttrib(LEGACY_ATTRIB_MASK | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();
        GL11.glColorMask(false,false,false,false);GL11.glDepthMask(false);
        Set<String> prepared=new HashSet<>();
        try {
            for(net.minecraft.item.Item item:net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.getValuesCollection()) {
                ResourceLocation id=item.getRegistryName();
                if(id==null || !WarTechReforged.MODID.equals(id.getResourceDomain())
                        || !isCustomItem(id.getResourcePath())) continue;
                net.minecraft.util.NonNullList<ItemStack> variants=net.minecraft.util.NonNullList.create();
                item.getSubItems(net.minecraft.creativetab.CreativeTabs.SEARCH,variants);
                for(ItemStack stack:variants) {
                    String key=ItemPreviewKey.of(id.getResourcePath(),stack.getMetadata(),stack.getTagCompound());
                    if(prepared.add(key)) renderItem(stack,ItemCameraTransforms.TransformType.GUI);
                }
            }
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();
            GL11.glPopAttrib();GL11.glMatrixMode(matrixMode);
        }
    }
    private static final Set<String> REPORTED_UNKNOWN_VISUALS = new HashSet<>();

    private static final String[] MQ9_BODY = {"body", "propeller_rotator"};
    private static final String[] MQ9_WINGS = {
        "far_left_flap", "middle_left_flap", "close_left_flap", "mainwing",
        "close_right_flap", "middle_right_flap", "far_right_flap", "tailwing",
        "tailwing_rudder", "left_vwing", "left_vwing_rudder", "right_vwing",
        "right_vwing_rudder"
    };
    private static final String[] MQ9_CAMERA = {"camera", "camera_holder"};
    private static final String[] MQ9_PYLONS = {
        "big_rocket_system_right", "big_rocket_system_left",
        "rocket_system_left", "rocket_system_right"
    };
    private static final String[] F16_BODY = {
        "lod0_lod0.001", "canopy01_canopy01.001",
        "glass_hud_glass_hud.001", "elevatorr01_elevatorr01.001",
        "elevatorl01_elevatorl01.001", "aileronr01_aileronr01.001",
        "aileronl01_aileronl01.001", "braker01_braker01.001",
        "braker02_braker02.001", "brakel01_brakel01.001",
        "brakel02_brakel02.001", "voletr01_voletr01.001",
        "voletl01_voletl01.001", "enginel01_enginel01.001",
        "rudderl01_rudderl01.001"
    };
    private static final String[] F16_PILOT = {"pilot_pilot.001"};
    private static final String[] F16_SEAT = {"eject_seat_eject_seat.001"};
    private static final String[] F16_GLASS = {
        "glass_canopy01_glass_canopy01.001", "glass01_glass01.001"
    };
    private static final String[] SU27_BODY = {
        "su27_body_su27_body.001", "su27_gear_su27_gear.001"
    };
    private static final String[] SU27_GLASS = {"su27_glass_su27_glass.001"};
    private static final String[] TU95_AIRFRAME = {
        "body", "airframe_wings", "airframe_gear", "airframe_details"
    };
    private static final String[] TU95_PROPELLERS = {
        "prop_outer_left_front", "prop_outer_left_rear",
        "prop_outer_right_front", "prop_outer_right_rear",
        "prop_inner_left_front", "prop_inner_left_rear",
        "prop_inner_right_front", "prop_inner_right_rear"
    };
    private static final String[] RADAR_TEXTURES = {
        "0006", "0012", "0014", "0016", "0018", "0019", "0021", "0025"
    };
    private static final String[][] RADAR_PARTS = {
        {"DrawCall_0239"},
        {"DrawCall_0240"},
        {"DrawCall_0241", "DrawCall_0270"},
        {"DrawCall_0242"},
        {"DrawCall_0277", "DrawCall_0471", "DrawCall_1113"},
        {"DrawCall_0470", "DrawCall_1112"},
        {"DrawCall_0464", "DrawCall_1128"}
    };
    private static final String[][] RADAR_DISH_PARTS = {
        {"DrawCall_0247", "DrawCall_0256", "DrawCall_0257", "DrawCall_0262", "DrawCall_0263"},
        {"DrawCall_0465", "DrawCall_0466", "DrawCall_0467", "DrawCall_0468",
            "DrawCall_0469", "DrawCall_1114", "DrawCall_1123", "DrawCall_1124",
            "DrawCall_1125", "DrawCall_1134"},
        {"DrawCall_0519"}
    };
    private static final String[] TOR_PARTS = {
        "tor_material_0",
        "tor_material_1_0", "tor_material_1_1", "tor_material_1_2",
        "tor_material_1_3", "tor_material_1_4", "tor_material_1_5",
        "tor_material_1_6", "tor_material_1_7", "tor_material_1_8",
        "tor_material_1_9", "tor_material_2", "tor_material_3", "tor_material_4"
    };
    private static final String[] TOR_TEXTURES = {
        "tor_0.png",
        "tor_1.png", "tor_1.png", "tor_1.png", "tor_1.png", "tor_1.png",
        "tor_1.png", "tor_1.png", "tor_1.png", "tor_1.png", "tor_1.png",
        "tor_2.png", "tor_0.png", "tor_3.png"
    };
    private static final String[] PANTSIR_STATIC = {
        "body", "canopy0", "canopy1", "canopy2", "canopy3", "canopy4",
        "hatch0", "wheel0", "wheel1", "wheel2", "wheel3", "wheel4", "wheel5"
    };
    private static final String[] PANTSIR_TURRET = {"weapon0", "weapon0_aux", "missiles"};

    private LegacyRenderLibrary() {
    }

    static void renderEntity(EntityWarTechBase entity, double x, double y,
            double z, float partialTicks, float remoteYaw,
            float remotePitch) {
        if (com.wartec.wartecmod.port.content.StrategicFeature.isDisabledEntity(entity)) return;
        GL11.glPushMatrix();
        GL11.glPushAttrib(LEGACY_ATTRIB_MASK);
        setup();
        GL11.glTranslated(x, y, z);
        String visual = resolveVisual(entity);
        int variant = entity.getVisualVariant();
        float yaw = Float.isNaN(remoteYaw)
                ? interpolate(entity.prevRotationYaw, entity.rotationYaw, partialTicks)
                : remoteYaw;
        float pitch = Float.isNaN(remotePitch)
                ? interpolate(entity.prevRotationPitch, entity.rotationPitch, partialTicks)
                : remotePitch;

        if (entity instanceof com.wartec.wartecmod.port.entity.EntityCustomCruise) {
            CruiseRenderer.renderEntity((com.wartec.wartecmod.port.entity.EntityCustomCruise) entity,yaw,pitch,partialTicks);
            GL11.glPopAttrib();GL11.glPopMatrix();return;
        }
        if (entity instanceof EntityCustomUav) {
            CustomUavRenderer.renderEntity((EntityCustomUav) entity,
                    yaw, pitch, partialTicks);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            return;
        }

        float worldScale=com.wartec.wartecmod.port.entity.VehicleDimensions.worldScale(entity.getProfile(),visual);
        GL11.glScalef(worldScale,worldScale,worldScale);
        if (visual.contains("storm_shadow")) {
            renderAdvancedMissile("models/storm_shadow/storm_shadow.obj",
                    "textures/models/storm_shadow/storm_shadow.png",
                    0.010F, -90.0F, 0.37F, 2.8F, 0.0F, false, false, yaw, pitch);
        } else if (visual.contains("geran_5")) {
            GL11.glRotatef(-yaw,0,1,0);GL11.glRotatef(pitch,1,0,0);
            renderSingle("models/geran/geran5.obj","textures/models/geran/geran5.png");
            if (entity instanceof com.wartec.wartecmod.port.entity.EntityWarTechMissile
                    && ((com.wartec.wartecmod.port.entity.EntityWarTechMissile)entity).isGeranJetRunning()) {
                CruiseExhaustRenderer.renderGeran5(entity.ticksExisted+partialTicks,
                        Math.sqrt(entity.motionX*entity.motionX+entity.motionY*entity.motionY+entity.motionZ*entity.motionZ));
            }
        } else if (visual.contains("geran")) {
            renderAdvancedMissile("models/geran/geran2.obj",
                    "textures/models/geran/geran2.png",
                    0.008F, 180.0F, 0.0F, -0.8F, -25.0F, true, true, yaw, pitch);
        } else if (visual.contains("anti_radiation") || visual.contains("anti_air")
                || visual.contains("anti_ballistic")) {
            renderAdvancedMissile("models/ew/agm88_harm.obj",
                    "textures/models/ew/agm88_harm.png",
                    1.55F, 180.0F, 0.0F, 0.0F, 0.0F, true, false, yaw, pitch);
        } else if (visual.contains("kh555")) {
            GL11.glTranslatef(0.0F, -0.35F, 0.0F);
            GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            renderSingle("models/strategic/kh555.obj",
                    "textures/models/strategic/kh555.png");
        } else if (visual.contains("mq9_reaper")) {
            GL11.glTranslatef(0.0F, 1.10F, 0.0F);
            GL11.glRotatef(-yaw - 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-pitch, 0.0F, 0.0F, 1.0F);
            if (entity.getLegacyState() == 6) {
                GL11.glRotatef(13.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(-11.0F, 0.0F, 0.0F, 1.0F);
            }
            renderMq9(entity, 900.0F, entity.ticksExisted + partialTicks);
        } else if (visual.contains("f16")) {
            GL11.glTranslatef(0.0F, 0.08F, 0.0F);
            GL11.glRotatef(-yaw + 90.0F, 0.0F, 1.0F, 0.0F);
            // Native F-16 nose is -X: opposite Z-rotation from the Su-27's +X nose.
            GL11.glRotatef(f16RenderPitch(entity, pitch)
                            - tacticalFlightPitchTrim(entity),
                    0.0F, 0.0F, 1.0F);
            applyTacticalWreckAttitude(entity);
            renderTactical(entity, false, false);
        } else if (visual.contains("su27")) {
            GL11.glTranslatef(0.0F, 0.08F, 0.0F);
            GL11.glRotatef(-yaw - 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-pitch, 0.0F, 0.0F, 1.0F);
            applyTacticalWreckAttitude(entity);
            renderTactical(entity, true, false);
        } else if (visual.contains("tu95")) {
            GL11.glTranslatef(0.0F, -0.25F, 0.0F);
            GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            if (entity.getLegacyState() == 8) {
                GL11.glRotatef(13.0F, 0.0F, 0.0F, 1.0F);
            }
            renderTu95(entity, 0.47F, partialTicks);
        } else if (visual.contains("artillery/greg_shell")) {
            GL11.glRotatef(yaw - 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch - 90.0F, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(2.5F, 5.0F, 2.5F);
            bind("textures/models/legacy_hbm/projectiles/grenade.png");
            model("models/legacy_hbm/projectiles/projectiles.obj")
                    .renderPart("Grenade");
        } else if (visual.contains("artillery/henry_rocket")) {
            GL11.glRotatef(yaw - 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch - 90.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            int ammoType = entity instanceof EntityWarTechArtilleryProjectile
                    ? ((EntityWarTechArtilleryProjectile) entity).getAmmoType()
                    : variant;
            bind(himarsProjectileTexture(ammoType));
            model("models/legacy_hbm/turret_himars.obj").renderPart(
                    ammoType == 1 || ammoType == 5
                            ? "RocketSingle" : "RocketStandard");
        } else if (visual.contains("kinetic")) {
            GL11.glRotatef(-yaw - 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-pitch, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(1.15F, 1.15F, 1.15F);
            renderSingle("models/orbital/kinetic_rod.obj",
                    "textures/items/legacy_hbm_ingot_tungsten.png");
        } else if (visual.contains("mq9_payload")) {
            GL11.glRotatef(-yaw + 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 0.0F, 0.0F, 1.0F);
            renderOrdnance(variant, 1.0F);
        } else if (visual.contains("strategic_bomb")
                || entity.getProfile() == WarTechEntityProfile.FAB_5000
                || entity.getProfile() == WarTechEntityProfile.KAB_3000) {
            GL11.glRotatef(-yaw + 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 0.0F, 0.0F, 1.0F);
            renderStrategicBomb(variant, 1.0F);
        } else if (visual.contains("strategic_flight")) {
            renderStrategicFlight(entity instanceof EntityStrategicMissile
                    ? (EntityStrategicMissile) entity : null,
                    variant, yaw, pitch);
        } else if (visual.contains("strategic_topol_m")
                || visual.contains("strategic_yars")
                || visual.contains("strategic_oreshnik")) {
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            EntityStrategicTel tel = entity instanceof EntityStrategicTel
                    ? (EntityStrategicTel) entity : null;
            float erection = tel == null ? 0.0F
                    : tel.getErectionProgress() / 100.0F;
            boolean loaded = tel == null || tel.isMissileLoaded();
            int launchTicks = tel == null ? 0 : tel.getLaunchTicks();
            renderStrategicTel(variant, erection, loaded, launchTicks);
        } else if (visual.contains("mobile_radar_truck")) {
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            renderRadarTruck(0.009F, false,
                    entity.isLegacyOperational()
                            ? (entity.ticksExisted + partialTicks) * 1.35F : 0.0F);
        } else if (visual.contains("s400_long_range_radar")
                || entity.getProfile() == WarTechEntityProfile.S400_RADAR) {
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            renderS400Radar(0.60F, false,
                    entity.isLegacyOperational()
                            ? (entity.ticksExisted + partialTicks) * 0.75F : 0.0F);
        } else if (visual.contains("air_defense_command_truck")) {
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            renderCommandTruck(0.75F, false);
        } else if (visual.contains("electronic_warfare")) {
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            renderElectronicWarfare(variant, false);
        } else if (visual.contains("mobile_air_defense")) {
            GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            EntityWarTechGroundVehicle vehicle =
                    entity instanceof EntityWarTechGroundVehicle
                            ? (EntityWarTechGroundVehicle) entity : null;
            float gunYaw = vehicle == null ? 0.0F
                    : vehicle.getRenderPantsirGunAimYaw(partialTicks);
            float gunPitch = vehicle == null ? 0.0F
                    : vehicle.getRenderPantsirGunAimPitch(partialTicks);
            boolean gunsFiring = vehicle != null
                    && vehicle.isPantsirGunsFiring();
            renderMobileAirDefense(variant, false,
                    entity.isDeployed()
                            ? (entity.ticksExisted + partialTicks) * 1.8F : 0.0F,
                    gunYaw);
            if (variant == 1 && gunsFiring) {
                renderPantsirGunTracers(gunYaw, gunPitch,
                        entity.ticksExisted + partialTicks);
            }
        } else if (visual.contains("mobile_artillery")) {
            GL11.glTranslatef(0.0F, 0.98F, 0.0F);
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            renderMobileArtilleryEntity(
                    entity instanceof EntityWarTechGroundVehicle
                            ? (EntityWarTechGroundVehicle) entity : null,
                    variant, yaw, partialTicks);
        } else if (visual.startsWith("missile/")) {
            int stage = entity instanceof EntityWarTechMissile
                    ? ((EntityWarTechMissile) entity).getFlightStage() : 1;
            renderLegacyMissile(
                    visual.substring("missile/".length()), yaw, pitch, stage);
        } else {
            renderFallbackProfile(entity.getProfile(), yaw, pitch);
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    static void renderSatelliteNuclear(net.minecraft.entity.Entity entity,
            double x, double y, double z, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        float yaw = entity.prevRotationYaw
                + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        float pitch = entity.prevRotationPitch
                + (entity.rotationPitch - entity.prevRotationPitch)
                        * partialTicks;
        GL11.glRotatef(yaw - 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(pitch, 0.0F, 0.0F, 1.0F);
        GL11.glScalef(1.5F, 1.5F, 1.5F);
        bind("textures/models/entity_sat_nuclear_missile_tex.png");
        model("models/entity_satellite_missile_nuclear.obj").renderAll();
        GL11.glPopMatrix();
    }

    static void renderItem(ItemStack stack,
            ItemCameraTransforms.TransformType type) {
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return;
        }
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            if (type == ItemCameraTransforms.TransformType.GUI) {
                fitItemToGuiSlot(stack, type);
            }
            renderItemRaw(stack, type);
        } finally {
            GL11.glPopMatrix();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPopMatrix();
            GL11.glMatrixMode(previousMatrixMode);
            OpenGlHelper.setActiveTexture(previousActiveTexture);
        }
    }

    private static void fitItemToGuiSlot(ItemStack stack,
            ItemCameraTransforms.TransformType type) {
        String key = ItemPreviewKey.of(stack.getItem().getRegistryName().getResourcePath(),
                stack.getMetadata(), stack.getTagCompound());
        LegacyObjModel.Bounds bounds = GUI_BOUNDS.get(key);
        if (bounds == null) {
            bounds = new LegacyObjModel.Bounds();
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            LegacyObjModel.beginMeasurement(bounds);
            try {
                renderItemRaw(stack, type);
            } finally {
                LegacyObjModel.endMeasurement();
                GL11.glPopMatrix();
            }
            GUI_BOUNDS.put(key, bounds);
        }
        if (!bounds.isValid() || bounds.largestSize() <= 1.0E-5F) {
            return;
        }
        float scale = 0.82F / bounds.largestSize();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(
                -bounds.centerX(), -bounds.centerY(), -bounds.centerZ());
    }

    private static void renderItemRaw(ItemStack stack,
            ItemCameraTransforms.TransformType type) {
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return;
        }
        String name = stack.getItem().getRegistryName().getResourcePath();
        int variant = stack.getMetadata();
        boolean inventory = type == ItemCameraTransforms.TransformType.GUI;
        GL11.glPushMatrix();
        GL11.glPushAttrib(LEGACY_ATTRIB_MASK);
        setup();

        if ("stormshadow".equals(name)) {
            renderAdvancedInventory("models/storm_shadow/storm_shadow.obj",
                    "textures/models/storm_shadow/storm_shadow.png",
                    0.010F, 0.28F, 135.0F, 0.37F, 2.8F, 0.0F,
                    false, inventory);
        } else if ("geran5drone".equals(name)) {
            renderAdvancedInventory("models/geran/geran5.obj","textures/models/geran/geran5.png",
                    1.0F,.25F,135.0F,0.0F,-.20F,0.0F,true,inventory);
        } else if ("gerandrone".equals(name)) {
            renderAdvancedInventory("models/geran/geran2.obj",
                    "textures/models/geran/geran2.png",
                    0.008F, 0.50F, 135.0F, 0.0F, -0.8F, -25.0F,
                    true, inventory);
        } else if ("antiradiationmissile".equals(name)) {
            renderAdvancedInventory("models/ew/agm88_harm.obj",
                    "textures/models/ew/agm88_harm.png",
                    1.55F, 0.21F, 135.0F, 0.0F, 0.0F, 0.0F,
                    true, inventory);
        } else if ("kh555missile".equals(name)) {
            if (inventory) {
                GL11.glTranslatef(0.0F, -0.1F, 0.0F);
                GL11.glScalef(1.38F, 1.38F, 1.38F);
            }
            GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.28F, 0.28F, 0.28F);
            renderSingle("models/strategic/kh555.obj", "textures/models/strategic/kh555.png");
        } else if ("mq9reaperdrone".equals(name)) {
            applySimpleInventoryTransform(type, -0.1F, 1.4F);
            GL11.glRotatef(64.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-38.0F, 0.0F, 1.0F, 0.0F);
            renderMq9(null, 82.0F, 0.0F);
        } else if ("assembleduav".equals(name)) {
            CustomUavRenderer.renderItem(UavBuild.fromStack(stack), type);
        } else if ("assembledcruise".equals(name)) {
            CruiseRenderer.renderItem(com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(stack),type);
        } else if ("cruisemodule".equals(name)) {
            com.wartec.wartecmod.port.cruise.CruisePartDefinition part=com.wartec.wartecmod.port.cruise.CruisePartDefinition.byMetadata(stack.getMetadata());
            if(part!=null && part.getSlot()==com.wartec.wartecmod.port.cruise.CruiseSlot.BODY)
                CruiseRenderer.renderItem(com.wartec.wartecmod.port.cruise.CruiseBuild.starter(part),type);
        } else if ("tacticalaircraft".equals(name) || "su27tacticalaircraft".equals(name)) {
            applySimpleInventoryTransform(type, -0.04F, 1.1F);
            GL11.glRotatef(62.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-36.0F, 0.0F, 1.0F, 0.0F);
            renderTactical(null, name.startsWith("su27"), true);
        } else if ("tu95strategicbomber".equals(name)) {
            applySimpleInventoryTransform(type, -0.08F, 1.15F);
            GL11.glRotatef(58.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-42.0F, 0.0F, 1.0F, 0.0F);
            renderTu95(null, 0.055F, 0.0F);
        } else if ("mq9payload".equals(name)) {
            GL11.glRotatef(18.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-34.0F, 0.0F, 0.0F, 1.0F);
            renderOrdnance(variant,
                    inventory ? inventoryOrdnanceScale(variant) : 1.12F);
        } else if ("strategicbomb".equals(name)) {
            GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-32.0F, 0.0F, 0.0F, 1.0F);
            renderStrategicBomb(variant, inventory ? 0.42F : 0.72F);
        } else if ("topolmtel".equals(name) || "yarstel".equals(name)
                || "oreshniktel".equals(name)) {
            applyVehicleItemTransform(type,
                    -0.30F, 0.75F,
                    0.65F, 0.35F, 0.15F, 0.11F, 0.15F);
            GL11.glRotatef(22.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
            renderStrategicTel("topolmtel".equals(name) ? 0
                    : "yarstel".equals(name) ? 1 : 2,
                    0.0F, true, 0);
        } else if ("mobileradartruck".equals(name)) {
            applyVehicleItemTransform(type,
                    -0.32F, 0.84F,
                    0.65F, 0.35F, 0.15F, 0.42F, 0.55F);
            GL11.glRotatef(22.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
            renderRadarTruck(0.00105F, true, 0.0F);
        } else if ("s400longrangeradar".equals(name)) {
            applyVehicleItemTransform(type,
                    -0.30F, 0.84F,
                    0.65F, 0.35F, 0.15F, 0.38F, 0.50F);
            GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            renderS400Radar(0.085F, true, 0.0F);
        } else if ("airdefensecommandtruck".equals(name)) {
            applyVehicleItemTransform(type,
                    -0.32F, 0.84F,
                    0.65F, 0.35F, 0.15F, 0.42F, 0.55F);
            GL11.glRotatef(22.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
            renderCommandTruck(0.12F, true);
        } else if ("electronicwarfareunit".equals(name)) {
            applyVehicleItemTransform(type,
                    -0.24F, 0.84F,
                    0.65F, 0.30F, 0.10F, 0.40F, 0.52F);
            GL11.glRotatef(22.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
            renderElectronicWarfare(variant, true);
        } else if ("mobileairdefensesystem".equals(name)) {
            applyVehicleItemTransform(type,
                    -0.18F, 0.84F,
                    0.65F, 0.30F, 0.10F, 0.34F, 0.46F);
            GL11.glRotatef(22.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.115F, 0.115F, 0.115F);
            renderMobileAirDefense(variant, true, 0.0F);
        } else if ("mobileartillery".equals(name)) {
            applyVehicleItemTransform(type,
                    -0.35F, 0.64F,
                    0.70F, 0.40F, 0.20F, 0.34F, 0.42F);
            GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.42F, 0.42F, 0.42F);
            renderMobileArtillery(variant, true);
        } else if ("itemkalibrmissile".equals(name) || "itemtomahawkmissile".equals(name)
                || "itemcj10missile".equals(name) || "itemiskandermissile".equals(name)) {
            renderLegacyMissileItem(name, type);
        } else if (name.startsWith("itemmissileantiairtier")) {
            applyLegacyModelAngles(inventory);
            GL11.glRotatef(-48.0F, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(0.18F, 0.18F, 0.18F);
            GL11.glTranslatef(0.0F, -4.0F, 0.0F);
            tintAntiAir(name);
            renderSingle("models/entity_missile_anti_air_tier1.obj",
                    "textures/models/entity_missile_anti_air_tier1.png");
        } else if ("itemmissileasat".equals(name)) {
            applyLegacyModelAngles(inventory);
            GL11.glRotatef(-48.0F, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(0.30F, 0.30F, 0.30F);
            GL11.glTranslatef(0.0F, -2.0F, 0.0F);
            renderSingle("models/entity_missile_micro.obj",
                    "textures/models/entity_missile_micro_gas.png");
        } else {
            renderBlockItem(name, type);
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    static boolean isCustomItem(String path) {
        return path.equals("stormshadow") || path.equals("gerandrone") || path.equals("geran5drone")
                || path.equals("antiradiationmissile") || path.equals("kh555missile")
                || path.equals("mq9reaperdrone") || path.equals("assembleduav") || path.equals("assembledcruise")
                || path.equals("mq9payload")
                || path.equals("strategicbomb") || path.equals("tu95strategicbomber")
                || path.equals("tacticalaircraft") || path.equals("su27tacticalaircraft")
                || path.equals("mobileradartruck") || path.equals("s400longrangeradar")
                || path.equals("airdefensecommandtruck") || path.equals("electronicwarfareunit")
                || path.equals("mobileairdefensesystem") || path.equals("mobileartillery")
                || path.equals("topolmtel") || path.equals("yarstel")
                || path.equals("oreshniktel")
                || path.equals("itemkalibrmissile") || path.equals("itemtomahawkmissile")
                || path.equals("itemcj10missile") || path.equals("itemiskandermissile")
                || path.startsWith("itemmissileantiairtier") || path.equals("itemmissileasat")
                || isCustomBlockItem(path);
    }

    static boolean isCustomBlockItem(String path) {
        return path.equals("geranlauncher") || path.equals("patriotlauncher")
                || path.equals("s400launcher") || path.equals("strategicearlywarningradar")
                || path.equals("vlsexhaust") || path.equals("ballisticmissilelauncher")
                || path.equals("launchtube") || path.startsWith("decoblock");
    }

    private static void renderFallbackProfile(WarTechEntityProfile profile, float yaw, float pitch) {
        if (profile == WarTechEntityProfile.COMMAND_TRUCK) {
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            renderCommandTruck(0.75F, false);
        } else if (profile == WarTechEntityProfile.RADAR_TRUCK) {
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            renderRadarTruck(0.009F, false, 0.0F);
        } else if (profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
            renderMobileAirDefense(0, false, 0.0F);
        } else if (profile == WarTechEntityProfile.MOBILE_ARTILLERY) {
            GL11.glTranslatef(0.0F, 0.98F, 0.0F);
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            renderMobileArtillery(0, false);
        } else if (profile == WarTechEntityProfile.ELECTRONIC_WARFARE) {
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            renderElectronicWarfare(0, false);
        } else if (profile == WarTechEntityProfile.STRATEGIC_TOPOL_M
                || profile == WarTechEntityProfile.STRATEGIC_YARS
                || profile == WarTechEntityProfile.STRATEGIC_ORESHNIK) {
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            renderStrategicTel(StrategicSystemProfile
                    .fromVehicleProfile(profile).ordinal(),
                    0.0F, true, 0);
        } else {
            reportUnknownVisual(profile);
        }
    }

    private static void reportUnknownVisual(WarTechEntityProfile profile) {
        String key = profile == null ? "<null>" : profile.name();
        synchronized (REPORTED_UNKNOWN_VISUALS) {
            if (!REPORTED_UNKNOWN_VISUALS.add(key)) {
                return;
            }
        }
        if (WarTechReforged.logger != null) {
            WarTechReforged.logger.error(
                    "No dev66 renderer binding for WarTech profile {}; "
                    + "entity intentionally left unrendered instead of "
                    + "substituting Storm Shadow", key);
        }
    }

    private static void renderAdvancedMissile(String model, String texture, float scale,
            float yawOffset, float offsetX, float offsetY, float offsetZ,
            boolean forwardAlongZ, boolean levelAlongZ, float yaw, float pitch) {
        GL11.glRotatef((levelAlongZ ? -yaw : yaw) + yawOffset, 0.0F, 1.0F, 0.0F);
        if (forwardAlongZ) {
            GL11.glRotatef((levelAlongZ ? 0.0F : 90.0F) - pitch, 1.0F, 0.0F, 0.0F);
        } else {
            GL11.glRotatef(pitch + 90.0F, 0.0F, 0.0F, 1.0F);
        }
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(offsetX, offsetY, offsetZ);
        renderSingle(model, texture);
    }

    private static void renderAdvancedInventory(String model, String texture, float worldScale,
            float inventoryScale, float inventoryYaw, float offsetX, float offsetY,
            float offsetZ, boolean forwardAlongZ, boolean inventory) {
        if (inventory) {
            GL11.glTranslatef(0.0F, -0.25F, 0.0F);
            GL11.glScalef(1.52F, 1.52F, 1.52F);
        }
        GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(inventoryYaw, 0.0F, 1.0F, 0.0F);
        if (!forwardAlongZ) {
            GL11.glRotatef(-48.0F, 0.0F, 0.0F, 1.0F);
        }
        float scale = worldScale * inventoryScale;
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(offsetX, offsetY, offsetZ);
        renderSingle(model, texture);
    }

    private static void applySimpleInventoryTransform(
            ItemCameraTransforms.TransformType type,
            float translateY, float scale) {
        if (type == ItemCameraTransforms.TransformType.GUI) {
            GL11.glTranslatef(0.0F, translateY, 0.0F);
            GL11.glScalef(scale, scale, scale);
        }
    }

    private static void applyVehicleItemTransform(
            ItemCameraTransforms.TransformType type,
            float inventoryY, float inventoryScale,
            float firstX, float firstY, float firstZ, float firstScale,
            float otherScale) {
        if (type == ItemCameraTransforms.TransformType.GUI) {
            GL11.glTranslatef(0.0F, inventoryY, 0.0F);
            GL11.glScalef(inventoryScale, inventoryScale, inventoryScale);
        } else if (type == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND
                || type == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND) {
            GL11.glTranslatef(firstX, firstY, firstZ);
            GL11.glScalef(firstScale, firstScale, firstScale);
        } else {
            GL11.glScalef(otherScale, otherScale, otherScale);
        }
    }

    private static void applyLegacyModelAngles(boolean inventory) {
        if (inventory) {
            GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void renderMq9(EntityWarTechBase entity, float scale,
            float animationTicks) {
        boolean showPylons = !(entity instanceof EntityCustomUav)
                || ((EntityCustomUav) entity).getAirframeType()
                        != UavAirframe.RECON;
        renderMq9(entity, scale, animationTicks, showPylons);
    }

    private static void renderMq9(EntityWarTechBase entity, float scale,
            float animationTicks, boolean showPylons) {
        LegacyObjModel model = model("models/mq9/mq9_reaper.obj");
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        bind("textures/models/mq9/mq9_body.png");
        renderParts(model, MQ9_BODY);
        bind("textures/models/mq9/mq9_wing.png");
        renderParts(model, MQ9_WINGS);
        bind("textures/models/mq9/mq9_camera.png");
        renderParts(model, MQ9_CAMERA);
        bind("textures/models/mq9/mq9_extras.png");
        model.renderPart("extras");
        if (showPylons && entity==null) {
            bind("textures/models/mq9/mq9_pylons.png");
            renderParts(model, MQ9_PYLONS);
        }
        bind("textures/models/mq9/mq9_body.png");
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.0038F, 0.0F, 0.0F);
        GL11.glRotatef(animationTicks * 36.0F, 1.0F, 0.0F, 0.0F);
        GL11.glTranslatef(0.0038F, 0.0F, 0.0F);
        model.renderPart("propeller");
        GL11.glPopMatrix();
        GL11.glPopMatrix();
        if (entity != null && showPylons) {
            GL11.glPushMatrix();GL11.glRotatef(90,0,1,0);
            for (int slot = 0; slot < 6; ++slot) {
                int code = entity.getLegacyPayloadCodeAt(slot);
                if (code <= 0) continue;
                renderConventionalAircraftStore(entity,slot,code,1.10);
            }
            GL11.glPopMatrix();
        }
    }

    private static void renderTactical(EntityWarTechBase entity, boolean su27,
            boolean inventory) {
        float scale = inventory ? 0.92F : su27 ? 12.5F : 10.5F;
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        if (su27) {
            LegacyObjModel model = model("models/tactical/su27_flanker.obj");
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            bind("textures/models/tactical/su27_body.png");
            renderParts(model, SU27_BODY);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            bind("textures/models/tactical/su27_glass.png");
            renderParts(model, SU27_GLASS);
        } else {
            LegacyObjModel model = model("models/tactical/f16_falcon.obj");
            bind("textures/models/tactical/f16_body.png");
            renderParts(model, F16_BODY);
            bind("textures/models/tactical/f16_pilot.png");
            renderParts(model, F16_PILOT);
            bind("textures/models/tactical/f16_seat.png");
            renderParts(model, F16_SEAT);
            bind("textures/models/tactical/f16_glass.png");
            renderParts(model, F16_GLASS);
        }
        GL11.glPopMatrix();
        if (entity != null) {
            renderTacticalPayloads(entity, su27);
        }
    }

    private static void renderTu95(EntityWarTechBase entity, float scale,
            float partialTicks) {
        LegacyObjModel model = model("models/strategic/tu95_bear.obj");
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        bind("textures/models/strategic/tu95_bear.png");
        renderParts(model, TU95_AIRFRAME);
        float[] propX = {-9.66278F, -9.66282F, 9.66278F, 9.66282F,
                -5.04754F, -5.04742F, 5.04754F, 5.04742F};
        float[] propY = {3.77985F, 3.77988F, 3.77985F, 3.77988F,
                3.77987F, 3.78024F, 3.77986F, 3.7802F};
        float[] propZ = {8.07354F, 8.97074F, 8.07354F, 8.97074F,
                11.1766F, 12.07379F, 11.1766F, 12.07379F};
        float[] direction = {1.0F, -1.0F, -1.0F, 1.0F,
                1.0F, -1.0F, -1.0F, 1.0F};
        float rate = entity == null ? 24.0F
                : entity.getLegacyState() == 6 ? 0.0F
                : entity.getLegacyState() != 0 ? 52.0F
                : entity.getLegacyPower() > 0 ? 12.0F : 0.0F;
        float angle = entity == null ? rate
                : (entity.ticksExisted + partialTicks) * rate;
        for (int index = 0; index < TU95_PROPELLERS.length; ++index) {
            GL11.glPushMatrix();
            GL11.glTranslatef(propX[index], propY[index], propZ[index]);
            GL11.glRotatef(angle * direction[index], 0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(-propX[index], -propY[index], -propZ[index]);
            model.renderPart(TU95_PROPELLERS[index]);
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
        if (entity != null) {
            renderTu95Payloads(entity);
        }
    }

    private static void renderOrdnance(int type, float presentationScale) {
        String[] models = {
            "agm114_hellfire.obj", "gbu12_paveway.obj", "mk82_bomb.obj",
            "hj10.obj", "agm65_maverick.obj", "kh29.obj", "kab500l.obj",
            "jdam.obj", "hj10.obj"
        };
        String[] folders = {
            "tactical", "tactical", "mq9", "tactical", "tactical",
            "tactical", "tactical", "tactical", "tactical"
        };
        String[] textures = {
            "agm114_hellfire.png", "gbu12_paveway.png", "mk82_bomb.png",
            "hj10.png", "agm65_maverick.png", "kh29.png", "kab500l.png",
            "jdam.png", "hj10.png"
        };
        float[] scales = {1.20F, 1.45F, 3.00F, 1.30F, 1.55F, 1.85F, 1.60F, 1.55F, 1.45F};
        int index = Math.max(0, Math.min(8, type));
        GL11.glPushMatrix();
        float scale = scales[index] * presentationScale;
        GL11.glScalef(scale, scale, scale);
        renderSingle("models/" + folders[index] + "/" + models[index],
                "textures/models/" + folders[index] + "/" + textures[index]);
        GL11.glPopMatrix();
    }

    static void renderPayloadCode(int code, float scale) {
        if (code >= 1 && code <= 9) {
            renderOrdnance(code - 1, scale);
        } else if (code == 10) {
            GL11.glScalef(0.45F * scale, 0.45F * scale, 0.45F * scale);
            GL11.glTranslatef(0,-.35F,0);
            renderSingle("models/strategic/kh555.obj",
                    "textures/models/strategic/kh555.png");
        } else if (code == 11 || code == 12) {
            renderStrategicBomb(code == 12 ? 1 : 0, 0.54F * scale);
        }
    }

    private static void renderTacticalPayloads(EntityWarTechBase entity,
            boolean su27) {
        double[] offset = su27
                ? new double[]{-3.0D, -2.0D, -1.0D, 1.0D, 2.0D, 3.0D}
                : new double[]{-2.18D, -1.08D, 1.08D, 2.18D};
        double[] modelX = su27
                ? new double[]{-3.2D, -1.62D, -0.55D, -0.55D, -1.62D, -3.2D}
                : new double[]{1.55D, 0.82D, 0.82D, 1.55D};
        double[] underside = su27
                ? new double[]{1.42D, 1.37D, 1.29D, 1.29D, 1.37D, 1.42D}
                : new double[]{0.92D, 0.92D, 0.92D, 0.92D};
        float[] tops = {0.059F, 0.125F, 0.0F, 0.073F, 0.102F,
                0.1F, 0.079F, 0.068F, 0.073F};
        float[] scales = {1.20F, 1.45F, 3.00F, 1.30F, 1.55F,
                1.85F, 1.60F, 1.55F, 1.45F};
        for (int slot = 0; slot < offset.length; ++slot) {
            int code = entity.getLegacyPayloadCodeAt(slot);
            if(code>=13 && entity instanceof EntityWarTechAircraft) {
                GL11.glPushMatrix();
                // Cancel the carrier's native OBJ front-axis rotation, NOT double it.
                GL11.glRotatef(com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.nativeCorrection(entity.getProfile()),0,1,0);
                renderCustomAircraftStore((EntityWarTechAircraft)entity,slot,.08);
                GL11.glPopMatrix();continue;
            }
            if (code < 1 || code > 9) continue;
            GL11.glPushMatrix();
            GL11.glRotatef(com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.nativeCorrection(entity.getProfile()),0,1,0);
            renderConventionalAircraftStore(entity,slot,code,.08);
            GL11.glPopMatrix();
        }
    }

    private static void renderTu95Payloads(EntityWarTechBase entity) {
        for (int slot = 0; slot < 6; ++slot) {
            int code = entity.getLegacyPayloadCodeAt(slot);
            if (code <= 0) continue;
            GL11.glPushMatrix();
            if(code>=13 && entity instanceof EntityWarTechAircraft) {
                renderCustomAircraftStore((EntityWarTechAircraft)entity,slot,-.25);
            } else if (code>=10 && code<=12) {
                double scale=com.wartec.wartecmod.port.entity.VehicleDimensions.scale(entity.getProfile());
                net.minecraft.util.math.Vec3d at=com.wartec.wartecmod.port.entity.VehicleDimensions.tuStore(code,slot);
                GL11.glScaled(1/scale,1/scale,1/scale);GL11.glTranslated(at.x,at.y+.25*scale,at.z);
                double top=com.wartec.wartecmod.port.entity.VehicleDimensions.tuStoreBodyTop(code);
                double attach=com.wartec.wartecmod.port.entity.VehicleDimensions.tuAttachY(code,slot);
                GL11.glPushMatrix();GL11.glTranslated(0,top,0);GL11.glScaled(1,attach-at.y-top,1);
                bind("textures/models/mq9/mq9_pylons.png");model("models/custom_uav/cruise_pylon.obj").renderAll();GL11.glPopMatrix();
                if(code!=10) GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                renderPayloadCode(code,code==10?1.10F/.45F:1.15F/.54F);
            }
            GL11.glPopMatrix();
        }
    }

    private static void renderCustomAircraftStore(EntityWarTechAircraft aircraft,int slot,double renderLift) {
        com.wartec.wartecmod.port.cruise.CruiseBuild build=aircraft.getCruiseStoreBuild(slot);
        net.minecraft.util.math.Vec3d at=com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.mount(aircraft.getProfile(),build,slot);
        double carrierScale=com.wartec.wartecmod.port.entity.VehicleDimensions.scale(aircraft.getProfile());
        // Coordinates are already WORLD dimensions. Cancel only the outer carrier scale,
        // retaining its yaw/pitch and the original body's ground-contact origin.
        GL11.glPushMatrix();GL11.glScaled(1/carrierScale,1/carrierScale,1/carrierScale);
        GL11.glTranslated(at.x,at.y-renderLift*carrierScale,at.z);
        double top=com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.storeBodyTop(build);
        double attach=com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.attachY(aircraft.getProfile(),build,slot);
        GL11.glPushMatrix();GL11.glTranslated(0,top,0);
        GL11.glScaled(1,Math.max(.08,attach-at.y-top),1);
        bind("textures/models/mq9/mq9_pylons.png");model("models/custom_uav/cruise_pylon.obj").renderAll();
        GL11.glPopMatrix();CruiseRenderer.renderMount(build);GL11.glPopMatrix();
    }
    private static void renderConventionalAircraftStore(EntityWarTechBase aircraft,int slot,int code,double lift) {
        double scale=com.wartec.wartecmod.port.entity.VehicleDimensions.scale(aircraft.getProfile());
        net.minecraft.util.math.Vec3d at=com.wartec.wartecmod.port.entity.AircraftStores.mount(aircraft.getProfile(),code-1,slot);
        double attach=com.wartec.wartecmod.port.entity.AircraftStores.anchor(aircraft.getProfile(),slot).y;
        double top=com.wartec.wartecmod.port.entity.AircraftStores.bodyTop(code-1);
        GL11.glPushMatrix();GL11.glScaled(1/scale,1/scale,1/scale);
        GL11.glTranslated(at.x,at.y-lift*scale,at.z);
        GL11.glPushMatrix();GL11.glTranslated(0,top,0);GL11.glScaled(1,attach-at.y-top,1);
        bind("textures/models/mq9/mq9_pylons.png");model("models/custom_uav/cruise_pylon.obj").renderAll();GL11.glPopMatrix();
        GL11.glRotatef(90,0,1,0);renderPayloadCode(code,1.15F);GL11.glPopMatrix();
    }

    private static void renderPylon(float x, float payloadTop, float underside,
            float z) {
        float x0 = x - 0.18F;
        float x1 = x + 0.18F;
        float y0 = payloadTop - 0.015F;
        float y1 = underside + 0.018F;
        float z0 = z - 0.055F;
        float z1 = z + 0.055F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.25F, 0.27F, 0.28F, 1.0F);
        GL11.glBegin(GL11.GL_QUADS);
        vertex(x0, y0, z0); vertex(x1, y0, z0); vertex(x1, y1, z0); vertex(x0, y1, z0);
        vertex(x1, y0, z1); vertex(x0, y0, z1); vertex(x0, y1, z1); vertex(x1, y1, z1);
        vertex(x0, y0, z1); vertex(x0, y0, z0); vertex(x0, y1, z0); vertex(x0, y1, z1);
        vertex(x1, y0, z0); vertex(x1, y0, z1); vertex(x1, y1, z1); vertex(x1, y1, z0);
        vertex(x0, y1, z0); vertex(x1, y1, z0); vertex(x1, y1, z1); vertex(x0, y1, z1);
        vertex(x0, y0, z1); vertex(x1, y0, z1); vertex(x1, y0, z0); vertex(x0, y0, z0);
        GL11.glEnd();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void vertex(float x, float y, float z) {
        GL11.glVertex3f(x, y, z);
    }

    private static float inventoryOrdnanceScale(int type) {
        switch (type) {
            case 1: return 1.08F;
            case 2: return 0.64F;
            case 3: return 1.20F;
            case 4: return 1.08F;
            case 5: return 0.92F;
            case 6: return 1.03F;
            case 7: return 1.04F;
            default: return 1.32F;
        }
    }

    private static void renderStrategicBomb(int variant, float presentationScale) {
        boolean kab = variant == 1;
        GL11.glPushMatrix();
        float scale = (kab ? 3.4F : 5.1F) * presentationScale;
        GL11.glScalef(scale, scale, scale);
        renderSingle(kab ? "models/tactical/kab500l.obj" : "models/mq9/mk82_bomb.obj",
                kab ? "textures/models/tactical/kab500l.png"
                        : "textures/models/mq9/mk82_bomb.png");
        GL11.glPopMatrix();
    }

    private static void renderRadarTruck(float scale, boolean inventory,
            float radarAngle) {
        LegacyObjModel model = model("models/radar/renault_trm_radar.obj");
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(58.943F, inventory ? -80.0F : -0.24F, 8.855F);
        for (int index = 0; index < RADAR_PARTS.length; index++) {
            bind("textures/models/radar/radar_" + RADAR_TEXTURES[index] + ".png");
            renderParts(model, RADAR_PARTS[index]);
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(8.0F, 0.0F, 140.0F);
        GL11.glRotatef(radarAngle, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-8.0F, 0.0F, -140.0F);
        bind("textures/models/radar/radar_0014.png");
        renderParts(model, RADAR_DISH_PARTS[0]);
        bind("textures/models/radar/radar_0021.png");
        renderParts(model, RADAR_DISH_PARTS[1]);
        bind("textures/models/radar/radar_0025.png");
        renderParts(model, RADAR_DISH_PARTS[2]);
        GL11.glPopMatrix();
    }

    private static void renderS400Radar(float scale, boolean inventory,
            float radarAngle) {
        LegacyObjModel model = model("models/network/s400_radar.obj");
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(0.636F, inventory ? -1.7F : 0.0F, 0.0F);
        bind("textures/models/network/s400_body1.png");
        model.renderPart("S400_Static_Body1_SM_Trioumf_Radar.001");
        bind("textures/models/network/s400_body2.png");
        model.renderPart("S400_Static_Body2_SM_Trioumf_Radar.002");
        bind("textures/models/network/s400_wheel.png");
        model.renderPart("S400_Static_Wheel_SM_Trioumf_Radar.004");
        bind("textures/models/network/s400_glass.png");
        model.renderPart("S400_Static_Glass_SM_Trioumf_Radar.005");
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0015F, 1.6997F, 0.0F);
        GL11.glRotatef(radarAngle, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-0.0015F, -1.6997F, 0.0F);
        bind("textures/models/network/s400_body2.png");
        model.renderPart("S400_Rotating_Body2_SM_Trioumf_Radar.003");
        GL11.glPopMatrix();
    }

    private static void renderCommandTruck(float scale, boolean inventory) {
        LegacyObjModel model = model("models/network/ural_command.obj");
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(0.0F, inventory ? -1.5F : 0.0F, -0.4746F);
        bind("textures/models/network/ural_command.png");
        model.renderPart("Command_Ural_ural4320");
        bind("textures/models/network/command_metal.png");
        model.renderPart("Command_Antennas_Cube");
    }

    private static void renderElectronicWarfare(int variant, boolean inventory) {
        int index = Math.max(0, Math.min(2, variant));
        String[] names = {"synytsia_jammer", "passive_esm_array", "radar_decoy"};
        if (inventory) {
            float scale = index == 1 ? 0.22F : 0.30F;
            GL11.glScalef(scale, scale, scale);
            GL11.glTranslatef(0.0F, -0.8F, 0.0F);
        }
        renderSingle("models/ew/" + names[index] + ".obj",
                "textures/models/ew/" + names[index] + ".png");
    }

    private static void renderMobileAirDefense(int variant, boolean inventory,
            float radarAngle) {
        renderMobileAirDefense(variant, inventory, radarAngle, 0.0F);
    }

    private static void renderMobileAirDefense(int variant, boolean inventory,
            float radarAngle, float gunYaw) {
        GL11.glPushMatrix();
        if (variant == 1) {
            LegacyObjModel model = model("models/shorad/pantsir_s2.obj");
            GL11.glScalef(0.65F, 0.65F, 0.65F);
            GL11.glTranslatef(0.0F, 0.35F, -1.70F);
            bind("textures/models/shorad/pantsir_s2.png");
            renderParts(model, PANTSIR_STATIC);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 3.03F, 0.56F);
            GL11.glRotatef(-gunYaw, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0.0F, -3.03F, -0.56F);
            renderParts(model, PANTSIR_TURRET);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 4.03F, 0.56F);
            GL11.glRotatef(radarAngle, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0.0F, -4.03F, -0.56F);
            model.renderPart("weapon0_radar");
            GL11.glPopMatrix();
            GL11.glPopMatrix();
        } else {
            LegacyObjModel model = model("models/shorad/tor_m1.obj");
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(0.72F, 0.72F, 0.72F);
            GL11.glTranslatef(0.885F, -12.943F, 2.420F);
            for (int index = 0; index < TOR_PARTS.length; index++) {
                bind("textures/models/shorad/" + TOR_TEXTURES[index]);
                model.renderPart(TOR_PARTS[index]);
            }
        }
        GL11.glPopMatrix();
    }

    private static void renderPantsirGunTracers(float gunYaw, float gunPitch,
            float animationTime) {
        double yaw = Math.toRadians(gunYaw);
        double pitch = Math.toRadians(gunPitch);
        double horizontal = Math.cos(pitch);
        double forwardX = -Math.sin(yaw) * horizontal;
        double forwardY = Math.sin(pitch);
        double forwardZ = Math.cos(yaw) * horizontal;
        double sideX = Math.cos(yaw);
        double sideZ = Math.sin(yaw);
        double phase = animationTime * 0.72D % 1.0D;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glLineWidth(2.6F);
        GL11.glColor4f(1.0F, 0.84F, 0.28F, 0.92F);
        GL11.glBegin(GL11.GL_LINES);
        for (int side = -1; side <= 1; side += 2) {
            double muzzleX = forwardX * 1.45D + sideX * side * 1.05D;
            double muzzleY = 3.22D;
            double muzzleZ = forwardZ * 1.45D + sideZ * side * 1.05D;
            for (int tracer = 0; tracer < 6; ++tracer) {
                double start = 5.0D + (tracer + phase) * 14.0D;
                double end = start + 5.5D;
                GL11.glVertex3d(muzzleX + forwardX * start,
                        muzzleY + forwardY * start,
                        muzzleZ + forwardZ * start);
                GL11.glVertex3d(muzzleX + forwardX * end,
                        muzzleY + forwardY * end,
                        muzzleZ + forwardZ * end);
            }
        }
        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    private static void renderMobileArtillery(int variant, boolean inventory) {
        bind("textures/models/mobile/hemtt.png");
        GL11.glPushMatrix();
        GL11.glScalef(0.00615F, 0.00615F, 0.00615F);
        GL11.glTranslatef(0.0F, inventory ? 158.8F : 0.0F, 34.5F);
        model("models/mobile/hemtt.obj").renderAll();
        GL11.glPopMatrix();
        if (variant == 1) {
            bind("textures/models/legacy_hbm/arty.png");
            GL11.glTranslatef(0.0F, inventory ? 1.02F : 0.04F, 1.45F);
            GL11.glScalef(0.48F, 0.48F, 0.48F);
            model("models/legacy_hbm/turret_arty.obj").renderAll();
        } else if (variant == 2) {
            bind("textures/models/legacy_hbm/himars.png");
            GL11.glTranslatef(0.0F, inventory ? 0.54F : -0.80F, 1.45F);
            GL11.glScalef(0.45F, 0.45F, 0.45F);
            model("models/legacy_hbm/turret_himars.obj").renderAll();
        }
    }

    private static void renderMobileArtilleryEntity(
            EntityWarTechGroundVehicle vehicle, int variant,
            float vehicleYaw, float partialTicks) {
        bind("textures/models/mobile/hemtt.png");
        GL11.glPushMatrix();
        GL11.glScalef(0.00615F, 0.00615F, 0.00615F);
        GL11.glTranslatef(0.0F, 0.0F, 34.5F);
        model("models/mobile/hemtt.obj").renderAll();
        GL11.glPopMatrix();
        if (variant != 0) {
            float turretYaw = vehicle == null ? vehicleYaw
                    : vehicle.getRenderArtilleryYaw(partialTicks);
            float turretPitch = vehicle == null ? 0.0F
                    : vehicle.getRenderArtilleryPitch(partialTicks);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, 1.45F);
            GL11.glRotatef(-(turretYaw - vehicleYaw),
                    0.0F, 1.0F, 0.0F);
            if (variant == 1) {
                GL11.glTranslatef(0.0F, 0.04F, 0.0F);
                bind("textures/models/legacy_hbm/arty.png");
                GL11.glScalef(0.48F, 0.48F, 0.48F);
                LegacyObjModel greg =
                        model("models/legacy_hbm/turret_arty.obj");
                greg.renderPart("Base");
                greg.renderPart("Carriage");
                GL11.glTranslatef(0.0F, 3.0F, 0.0F);
                GL11.glRotatef(turretPitch, 1.0F, 0.0F, 0.0F);
                GL11.glTranslatef(0.0F, -3.0F, 0.0F);
                greg.renderPart("Cannon");
                float recoil = vehicle == null ? 0.0F
                        : vehicle.getRenderArtilleryRecoil(partialTicks);
                GL11.glTranslatef(0.0F, 0.0F, recoil * 2.5F);
                greg.renderPart("Barrel");
            } else {
                GL11.glTranslatef(0.0F, -0.8F, 0.0F);
                bind("textures/models/legacy_hbm/himars.png");
                GL11.glScalef(0.45F, 0.45F, 0.45F);
                LegacyObjModel henry =
                        model("models/legacy_hbm/turret_himars.obj");
                henry.renderPart("Carriage");
                GL11.glTranslatef(0.0F, 2.25F, 2.0F);
                GL11.glRotatef(turretPitch, 1.0F, 0.0F, 0.0F);
                GL11.glTranslatef(0.0F, -2.25F, -2.0F);
                henry.renderPart("Launcher");
                float crane = vehicle == null ? 0.0F
                        : vehicle.getRenderArtilleryCrane(partialTicks);
                GL11.glTranslatef(0.0F, 0.0F, crane * -5.0F);
                henry.renderPart("Crane");
                int type = vehicle == null
                        ? -1 : vehicle.getArtilleryAmmoType();
                int count = vehicle == null
                        ? 0 : vehicle.getArtilleryAmmoCount();
                boolean single = type == 1 || type == 5;
                if (type >= 0) {
                    bind(himarsProjectileTexture(type));
                }
                henry.renderPart(single ? "TubeSingle" : "TubeStandard");
                if (single) {
                    if (count > 0) {
                        henry.renderPart("CapSingle");
                    }
                } else {
                    for (int index = 0;
                            index < Math.min(6, count); ++index) {
                        henry.renderPart("CapStandard" + (6 - index));
                    }
                }
            }
            GL11.glPopMatrix();
        }
        if (vehicle != null && vehicle.isDeployed()) {
            renderMobileArtilleryOutriggers();
        }
    }

    private static void renderMobileArtilleryOutriggers() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor3f(0.3F, 0.32F, 0.2F);
        GL11.glBegin(GL11.GL_QUADS);
        artilleryOutrigger(-1.0F, -2.15F, -1.58F);
        artilleryOutrigger(1.0F, 2.15F, -1.58F);
        artilleryOutrigger(-1.0F, -2.15F, 1.58F);
        artilleryOutrigger(1.0F, 2.15F, 1.58F);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void artilleryOutrigger(float start, float end, float z) {
        artilleryBox(Math.min(start, end), -0.58F, z - 0.09F,
                Math.max(start, end), -0.46F, z + 0.09F);
        artilleryBox(end - 0.1F, -0.96F, z - 0.1F,
                end + 0.1F, -0.46F, z + 0.1F);
        artilleryBox(end - 0.28F, -1.01F, z - 0.27F,
                end + 0.28F, -0.94F, z + 0.27F);
    }

    private static void artilleryBox(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        float[][] vertices = {
            {minX, minY, minZ}, {maxX, minY, minZ},
            {maxX, maxY, minZ}, {minX, maxY, minZ},
            {maxX, minY, maxZ}, {minX, minY, maxZ},
            {minX, maxY, maxZ}, {maxX, maxY, maxZ},
            {minX, minY, maxZ}, {minX, minY, minZ},
            {minX, maxY, minZ}, {minX, maxY, maxZ},
            {maxX, minY, minZ}, {maxX, minY, maxZ},
            {maxX, maxY, maxZ}, {maxX, maxY, minZ},
            {minX, maxY, minZ}, {maxX, maxY, minZ},
            {maxX, maxY, maxZ}, {minX, maxY, maxZ},
            {minX, minY, maxZ}, {maxX, minY, maxZ},
            {maxX, minY, minZ}, {minX, minY, minZ}
        };
        for (float[] vertex : vertices) {
            GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
        }
    }

    private static String himarsProjectileTexture(int type) {
        switch (type) {
            case 1:
                return "textures/models/legacy_hbm/projectiles/himars_single.png";
            case 2:
                return "textures/models/legacy_hbm/projectiles/himars_standard_he.png";
            case 3:
                return "textures/models/legacy_hbm/projectiles/himars_standard_wp.png";
            case 4:
                return "textures/models/legacy_hbm/projectiles/himars_standard_tb.png";
            case 5:
                return "textures/models/legacy_hbm/projectiles/himars_single_tb.png";
            case 6:
                return "textures/models/legacy_hbm/projectiles/himars_standard_mini_nuke.png";
            case 7:
                return "textures/models/legacy_hbm/projectiles/himars_standard_lava.png";
            default:
                return "textures/models/legacy_hbm/projectiles/himars_standard.png";
        }
    }

    private static void renderLegacyMissile(
            String id, float yaw, float pitch, int stage) {
        GL11.glRotatef(yaw - 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(pitch, 0.0F, 0.0F, 1.0F);
        if (id.startsWith("cruise_")) {
            renderCruiseMissile(id, stage);
        } else if ("kalibr".equals(id)) {
            renderKalibr(stage);
        } else if ("tomahawk".equals(id)) {
            renderCruiseMissileWithTexture(
                    "textures/models/entity_tomahawk_missile_tex.png", stage);
        } else if ("cj10".equals(id)) {
            renderSingle("models/entity_cj10_missile.obj",
                    "textures/models/entity_cj10_missile_tex.png");
        } else if ("iskander".equals(id)) {
            renderSingle("models/entity_iskander_missile.obj",
                    "textures/models/entity_iskander_missile_tex.png");
        } else if ("slbm".equals(id)) {
            renderSingle("models/entity_slbm_missile.obj",
                    "textures/models/entity_slbm_missile_tex.png");
        } else if (id.startsWith("micro_") || "asat".equals(id)) {
            renderSingle("models/entity_missile_micro.obj",
                    id.contains("neutron")
                            ? "textures/models/entity_missile_micro_neutron.png"
                            : "textures/models/entity_missile_micro_gas.png");
        } else if (id.startsWith("supersonic_")) {
            renderSupersonic(id.contains("hydrogen"), stage);
        } else if (id.startsWith("hypersonic_")) {
            renderHypersonic(id.contains("nuclear"), stage);
        } else if ("lrhw".equals(id)) {
            bind("textures/models/entity_lrhw_missile_tex.png");
            model("models/entity_lrhw_missile_glider.obj").renderAll();
            if (stage == 1) {
                GL11.glPushMatrix();
                GL11.glTranslated(0.0D, -8.46875D, 0.0D);
                model("models/entity_lrhw_missile_booster.obj").renderAll();
                model("models/entity_lrhw_missile_cone.obj").renderAll();
                GL11.glPopMatrix();
            }
        } else {
            renderSingle("models/entity_missile_anti_air_tier1.obj",
                    "textures/models/entity_missile_anti_air_tier1.png");
        }
    }

    private static void renderCruiseMissile(String id, int stage) {
        String texture = "textures/models/entity_cruise_missile_tex.png";
        if (id.contains("cluster")) texture = "textures/models/entity_cruise_missile_tex_cluster.png";
        else if (id.contains("buster")) texture = "textures/models/entity_cruise_missile_tex_buster.png";
        else if (id.contains("emp")) texture = "textures/models/entity_cruise_missile_tex_emp.png";
        else if (id.contains("thermobaric")) texture = "textures/models/entity_cruise_missile_tex_fae.png";
        else if (id.contains("nuclear")) texture = "textures/models/entity_cruise_missile_tex_nuclear.png";
        else if (id.contains("hydrogen")) texture = "textures/models/entity_cruise_missile_tex_h.png";
        renderCruiseMissileWithTexture(texture, stage);
    }

    private static void renderCruiseMissileWithTexture(
            String texture, int stage) {
        bind(texture);
        model("models/entity_cruise_missile_base.obj").renderAll();
        if (stage == 1) {
            model("models/entity_cruise_missile_booster.obj").renderAll();
            model("models/entity_cruise_missile_sealing.obj").renderAll();
        } else if (stage == 2) {
            model("models/entity_cruise_missile_turbofan.obj").renderAll();
            model("models/entity_cruise_missile_wings.obj").renderAll();
            model("models/entity_cruise_missile_fins.obj").renderAll();
        }
    }

    private static void renderKalibr(int stage) {
        bind("textures/models/entity_kalibr_missile_tex.png");
        model("models/entity_kalibr_missile_base.obj").renderAll();
        if (stage == 1) {
            model("models/entity_kalibr_missile_booster.obj").renderAll();
            model("models/entity_kalibr_missile_fins_folded.obj").renderAll();
            model("models/entity_kalibr_missile_valves.obj").renderAll();
        } else if (stage == 2) {
            model("models/entity_kalibr_missile_wings.obj").renderAll();
            model("models/entity_kalibr_missile_fins.obj").renderAll();
        }
    }

    private static void renderSupersonic(boolean hydrogen, int stage) {
        bind("textures/models/entity_supersonic_cruise_missile_ramjet_tex.png");
        model("models/entity_supersonic_cruise_missile_ramjet.obj").renderAll();
        bind(hydrogen
                ? "textures/models/entity_supersonic_cruise_missile_fuselage_h_tex.png"
                : "textures/models/entity_supersonic_cruise_missile_fuselage_tex.png");
        model("models/entity_supersonic_cruise_missile_fuselage.obj").renderAll();
        bind("textures/models/entity_supersonic_cruise_missile_engine_tex.png");
        model("models/entity_supersonic_cruise_missile_engine.obj").renderAll();
        if (stage == 1) {
            bind("textures/models/entity_supersonic_cruise_missile_booster_tex.png");
            model("models/entity_supersonic_cruise_missile_booster.obj").renderAll();
            bind("textures/models/entity_supersonic_cruise_missile_protection_tex.png");
            model("models/entity_supersonic_cruise_missile_protection.obj").renderAll();
        } else if (stage == 2) {
            bind("textures/models/entity_supersonic_cruise_missile_wings_large_tex.png");
            model("models/entity_supersonic_cruise_missile_wings_large.obj").renderAll();
            bind("textures/models/entity_supersonic_cruise_missile_wings_small_tex.png");
            model("models/entity_supersonic_cruise_missile_wings_small.obj").renderAll();
        }
    }

    private static void renderHypersonic(boolean nuclear, int stage) {
        bind(nuclear
                ? "textures/models/entity_hypersonic_cruise_missile_nuclear_tex.png"
                : "textures/models/entity_hypersonic_cruise_missile_tex.png");
        model("models/entity_hypersonic_cruise_missile_scramjet.obj").renderAll();
        if (stage == 1) {
            model("models/entity_hypersonic_cruise_missile_booster.obj").renderAll();
            model("models/entity_hypersonic_cruise_missile_fins.obj").renderAll();
        } else if (stage == 2) {
            model("models/entity_hypersonic_cruise_missile_wings.obj").renderAll();
        }
    }

    private static void renderLegacyMissileItem(String name,
            ItemCameraTransforms.TransformType type) {
        if (type == ItemCameraTransforms.TransformType.GUI) {
            GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-48.0F, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(0.18F, 0.18F, 0.18F);
        } else {
            GL11.glScalef(0.2F, 0.2F, 0.2F);
            if (type != ItemCameraTransforms.TransformType.GROUND
                    && type != ItemCameraTransforms.TransformType.FIXED) {
                GL11.glTranslatef(2.0F, 0.0F, 0.0F);
            }
        }
        if ("itemkalibrmissile".equals(name)) {
            renderSingle("models/entity_kalibr_missile.obj",
                    "textures/models/entity_kalibr_missile_tex.png");
        } else if ("itemtomahawkmissile".equals(name)) {
            renderSingle("models/entity_tomahawk_missile.obj",
                    "textures/models/entity_tomahawk_missile_tex.png");
        } else if ("itemcj10missile".equals(name)) {
            renderSingle("models/entity_cj10_missile.obj",
                    "textures/models/entity_cj10_missile_tex.png");
        } else {
            renderSingle("models/entity_iskander_missile.obj",
                    "textures/models/entity_iskander_missile_tex.png");
        }
    }

    private static void renderBlockItem(String name,
            ItemCameraTransforms.TransformType type) {
        boolean inventory = type == ItemCameraTransforms.TransformType.GUI;
        if ("vlsexhaust".equals(name)) {
            applyLegacyModelAngles(inventory);
            GL11.glScalef(0.12F, 0.12F, 0.12F);
            GL11.glTranslatef(0.0F, -5.65F, 0.0F);
            LegacyObjModel model = model("models/blocks/vls_exhaust.obj");
            bind("textures/models/blocks/vls_exhaust_tex.png");
            model.renderPart("base");
            bind("textures/models/blocks/launcher_cover_tex.png");
            model.renderPart("cover");
            model.renderPart("diff_cover.001");
        } else if ("ballisticmissilelauncher".equals(name)) {
            applyLegacyModelAngles(inventory);
            GL11.glScalef(0.9F, 0.9F, 0.9F);
            GL11.glTranslatef(0.0F, -0.5F, 0.0F);
            renderSingle("models/blocks/ballistic_missile_launcher.obj",
                    "textures/models/blocks/ballistic_missile_launcher.png");
        } else if ("geranlauncher".equals(name)) {
            applySimpleInventoryTransform(type, -0.25F, 0.9F);
            GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.28F, 0.28F, 0.28F);
            GL11.glTranslatef(0.0F, -0.5F, -0.4F);
            renderSingle("models/geran/geran_catapult.obj",
                    "textures/models/geran/geran_catapult.png");
        } else if ("patriotlauncher".equals(name)) {
            applySimpleInventoryTransform(type, -0.35F, 0.9F);
            GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.075F, 0.075F, 0.075F);
            GL11.glTranslatef(2.42275F, 0.047F, 0.0F);
            renderPatriot();
        } else if ("s400launcher".equals(name)) {
            applySimpleInventoryTransform(type, -0.35F, 0.9F);
            GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.0009F, 0.0009F, 0.0009F);
            GL11.glTranslatef(0.0F, 1.9531822F, 116.85965F);
            renderS400Launcher();
        } else if ("strategicearlywarningradar".equals(name)) {
            applySimpleInventoryTransform(type, -0.2F, 0.65F);
            GL11.glRotatef(24.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.028F, 0.028F, 0.028F);
            GL11.glTranslatef(0.0F, -10.0F, 0.0F);
            renderSingle("models/network/strategic_radar.obj",
                    "textures/models/network/strategic_radar.png");
        } else if ("launchtube".equals(name)) {
            GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.12F, 0.12F, 0.12F);
            GL11.glTranslatef(0.0F, -5.65F, 0.0F);
            renderLaunchTube(null, 0.0F);
        } else if (name.startsWith("decoblock")) {
            GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.28F, 0.28F, 0.28F);
            GL11.glTranslatef(0.0F, -0.5F, 0.0F);
            renderDeco(name);
        }
    }

    private static void renderPatriot() {
        LegacyObjModel model = model("models/patriot/patriot_launcher.obj");
        bind("textures/models/patriot/mim-104_d.png");
        model.renderPart("pac_3_laun");
        bind("textures/models/patriot/mim-104_tractor_d.png");
        model.renderPart("window");
        model.renderPart("pac_3_lau0");
    }

    private static void renderS400Launcher() {
        LegacyObjModel model = model("models/s400/s400_launcher.obj");
        bind("textures/models/s400/t_s400_rockets_01_01_a.png");
        model.renderPart("SM_S400_VarA_01_01_Rockets_4_0");
        model.renderPart("SM_S400_VarA_01_01_Rockets_4_1");
        bind("textures/models/s400/t_s400_truck_01_01_a.png");
        model.renderPart("SM_S400_VarA_01_01_Truck_1_0");
    }

    private static void renderStrategicTel(int variant, float erection,
            boolean loaded, int launchTicks) {
        StrategicSystemProfile system = StrategicSystemProfile.byOrdinal(variant);
        float vehicleScale = system == StrategicSystemProfile.YARS ? 0.78F
                : system == StrategicSystemProfile.ORESHNIK ? 0.72F : 0.76F;
        float bodyR = system == StrategicSystemProfile.YARS ? 0.16F : 0.21F;
        float bodyG = system == StrategicSystemProfile.ORESHNIK ? 0.24F : 0.28F;
        float bodyB = system == StrategicSystemProfile.ORESHNIK ? 0.14F : 0.17F;
        float progress = Math.max(0.0F, Math.min(1.0F, erection));
        float angle = -90.0F * progress;
        boolean capClosed = launchTicks < 14 || launchTicks > 72;

        GL11.glPushMatrix();
        GL11.glScalef(vehicleScale, vehicleScale, vehicleScale);
        GL11.glTranslatef(2.45F, 0.0F, 0.0F);
        LegacyObjModel tel = model("models/strategic/cc0_transporter_erector.obj");
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        renderColoredPart(tel, "chassis_body", bodyR, bodyG, bodyB);
        renderColoredPart(tel, "chassis_dark", 0.075F, 0.085F, 0.070F);
        renderColoredPart(tel, "chassis_glass", 0.055F, 0.105F, 0.120F);
        renderColoredPart(tel, "wheel_tire", 0.035F, 0.037F, 0.033F);
        renderColoredPart(tel, "wheel_hub", 0.19F, 0.21F, 0.17F);

        GL11.glPushMatrix();
        GL11.glTranslatef(6.60F, 3.05F, 0.0F);
        GL11.glRotatef(angle, 0.0F, 0.0F, 1.0F);
        GL11.glTranslatef(-6.60F, -3.05F, 0.0F);
        renderColoredPart(tel, "erector_body", bodyR * 0.90F,
                bodyG * 0.90F, bodyB * 0.90F);
        renderColoredPart(tel, "erector_dark", 0.065F, 0.070F, 0.060F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        float length = system == StrategicSystemProfile.TOPOL_M ? 16.1F
                : system == StrategicSystemProfile.YARS ? 15.7F : 14.4F;
        float radius = system == StrategicSystemProfile.ORESHNIK ? 0.64F : 0.76F;
        GL11.glPushMatrix();
        GL11.glTranslatef(6.45F, 4.55F, 0.0F);
        GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
        renderCanister(radius, length, capClosed);
        if (loaded && !capClosed && launchTicks < 44) {
            float rise = Math.max(0.0F, launchTicks - 14) / 30.0F * 3.0F;
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.55F + rise, 0.0F);
            GL11.glScalef(0.43F, 1.08F, 0.43F);
            renderStrategicMissileModel(system);
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
        GL11.glPopMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private static void renderColoredPart(LegacyObjModel model, String part,
            float red, float green, float blue) {
        GL11.glColor4f(red, green, blue, 1.0F);
        model.renderPart(part);
    }

    private static void renderCanister(float radius, float length,
            boolean capClosed) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.20F, 0.27F, 0.16F, 1.0F);
        int segments = 32;
        renderCylinderSide(radius, 0.0F, length, segments);

        GL11.glColor4f(0.12F, 0.16F, 0.10F, 1.0F);
        float[] bands = {0.22F, 1.45F, length * 0.50F,
                length - 1.45F, length - 0.22F};
        for (float band : bands) {
            renderCylinderSide(radius * 1.055F, band - 0.08F,
                    band + 0.08F, segments);
        }

        float rail = radius * 0.12F;
        float railZ = radius * 0.985F;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        GL11.glNormal3f(0.0F, 0.0F, 1.0F);
        GL11.glVertex3f(-rail, 0.35F, railZ);
        GL11.glVertex3f(-rail, length - 0.35F, railZ);
        GL11.glVertex3f(rail, 0.35F, railZ);
        GL11.glVertex3f(rail, length - 0.35F, railZ);
        GL11.glEnd();

        renderCanisterDisc(radius, 0.0F, -1.0F, segments);
        if (capClosed) {
            GL11.glColor4f(0.27F, 0.33F, 0.22F, 1.0F);
            renderCanisterDisc(radius * 1.02F, length, 1.0F, segments);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void renderCylinderSide(float radius, float start,
            float end, int segments) {
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int index = 0; index <= segments; ++index) {
            double angle = Math.PI * 2.0D * index / segments;
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            GL11.glNormal3f(x / radius, 0.0F, z / radius);
            GL11.glVertex3f(x, start, z);
            GL11.glVertex3f(x, end, z);
        }
        GL11.glEnd();
    }

    private static void renderCanisterDisc(float radius, float y,
            float normalY, int segments) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glNormal3f(0.0F, normalY, 0.0F);
        GL11.glVertex3f(0.0F, y, 0.0F);
        for (int index = 0; index <= segments; ++index) {
            int winding = normalY > 0.0F ? index : segments - index;
            double angle = Math.PI * 2.0D * winding / segments;
            GL11.glVertex3f((float) Math.cos(angle) * radius, y,
                    (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    private static void renderStrategicFlight(EntityStrategicMissile missile,
            int variant, float yaw, float pitch) {
        StrategicSystemProfile system = StrategicSystemProfile.byOrdinal(variant);
        GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(pitch + 90.0F, 1.0F, 0.0F, 0.0F);
        boolean reentry = missile != null && missile.isReentryVehicle();
        float scale = reentry ? 0.13F
                : system == StrategicSystemProfile.YARS ? 0.61F
                : system == StrategicSystemProfile.ORESHNIK ? 0.52F : 0.58F;
        GL11.glScalef(scale, scale, scale);
        renderStrategicMissileModel(system);
    }

    private static void renderStrategicMissileModel(
            StrategicSystemProfile system) {
        String texture = system == StrategicSystemProfile.YARS
                ? "hbm:textures/models/missiles/missile_huge_bu.png"
                : system == StrategicSystemProfile.ORESHNIK
                ? "hbm:textures/models/missiles/missile_huge_cl.png"
                : "hbm:textures/models/missiles/missile_huge.png";
        renderSingle("hbm:models/missile_huge.obj", texture);
    }

    static void renderBlock(String name, double x, double y, double z,
            TileEntityWarTechMachine tile, EnumFacing decorationFacing,
            float partialTicks) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(LEGACY_ATTRIB_MASK);
        setup();
        if ("geranlauncher".equals(name)) {
            GL11.glTranslated(x + 0.5D, y, z + 0.5D);
            float scale=com.wartec.wartecmod.port.entity.VehicleDimensions.blockScale(name);
            GL11.glScalef(scale,scale,scale);
            renderSingle("models/geran/geran_catapult.obj",
                    "textures/models/geran/geran_catapult.png");
            if (tile != null && !tile.getStackInSlot(0).isEmpty()) {
                GL11.glPushMatrix();
                if(tile.getStackInSlot(0).getItem()==com.wartec.wartecmod.port.content.WarTechContent.GERAN_5_DRONE) {
                    // A low sliding cradle supports the pod and fuselage, with no
                    // changes to the user's mesh and no rail inside its wing.
                    GL11.glRotatef(-12,1,0,0);
                    GL11.glScalef(1/scale,1/scale,1/scale);
                    renderSingle("models/geran/geran5_cradle.obj","textures/models/geran/geran_catapult.png");
                    GL11.glTranslatef(0,1.30064F,.46049F);
                    renderSingle("models/geran/geran5.obj","textures/models/geran/geran5.png");
                } else {
                GL11.glTranslatef(0.0F, 1.22F, 0.45F);
                GL11.glRotatef(-12.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
                GL11.glScalef(0.008F, 0.008F, 0.008F);
                GL11.glTranslatef(0.0F, -0.8F, -25.0F);
                renderSingle("models/geran/geran2_launcher_lod.obj",
                        "textures/models/geran/geran2.png");
                }
                GL11.glPopMatrix();
            }
        } else if ("patriotlauncher".equals(name)) {
            GL11.glTranslated(x + 0.5D, y, z + 0.5D);
            float scale=com.wartec.wartecmod.port.entity.VehicleDimensions.blockScale(name);
            GL11.glScalef(scale,scale,scale);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.60F, 0.60F, 0.60F);
            GL11.glTranslatef(2.42275F, 0.047F, 0.0F);
            renderPatriot();
        } else if ("s400launcher".equals(name)) {
            GL11.glTranslated(x + 0.5D, y, z + 0.5D);
            float scale=com.wartec.wartecmod.port.entity.VehicleDimensions.blockScale(name);
            GL11.glScalef(scale,scale,scale);
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.008F, 0.008F, 0.008F);
            GL11.glTranslatef(0.0F, 1.9531822F, 116.85965F);
            renderS400Launcher();
        } else if ("strategicearlywarningradar".equals(name)) {
            GL11.glTranslated(x + 0.5D, y, z + 0.5D);
            renderSingle("models/network/strategic_radar.obj",
                    "textures/models/network/strategic_radar.png");
        } else if ("launchtube".equals(name)) {
            GL11.glTranslated(x + 0.5D, y, z + 0.5D);
            renderLaunchTube(tile, partialTicks);
        } else if ("vlsexhaust".equals(name)) {
            GL11.glTranslated(x + 0.5D, y, z + 1.0D);
            LegacyObjModel exhaust = model("models/blocks/vls_exhaust.obj");
            bind("textures/models/blocks/vls_exhaust_tex.png");
            exhaust.renderPart("base");
            GL11.glPushMatrix();
            GL11.glTranslated(-0.5D, 11.0D, 0.0D);
            GL11.glRotated(openingAngle(tile, partialTicks),
                    0.0D, 0.0D, 1.0D);
            GL11.glTranslated(0.5D, 0.0D, 0.0D);
            bind("textures/models/blocks/launcher_cover_tex.png");
            exhaust.renderPart("cover");
            GL11.glPopMatrix();
        } else if ("ballisticmissilelauncher".equals(name)) {
            GL11.glTranslated(x + 0.5D, y, z + 0.5D);
            renderSingle("models/blocks/ballistic_missile_launcher.obj",
                    "textures/models/blocks/ballistic_missile_launcher.png");
            renderBallisticPayload(tile);
        } else if (name.startsWith("decoblock")) {
            double offsetY = "decoblocksatellitenuclear".equals(name) ? 0.0D : 0.5D;
            GL11.glTranslated(x + 0.5D, y + offsetY, z + 0.5D);
            GL11.glRotatef(decorationYaw(decorationFacing),
                    0.0F, 1.0F, 0.0F);
            renderDeco(name);
        }
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private static float decorationYaw(EnumFacing facing) {
        if (facing == EnumFacing.WEST) return 0.0F;
        if (facing == EnumFacing.NORTH) return 270.0F;
        if (facing == EnumFacing.EAST) return 180.0F;
        return 90.0F;
    }

    private static void renderBallisticPayload(TileEntityWarTechMachine tile) {
        if (tile == null || tile.getStackInSlot(0).isEmpty()
                || !(tile.getStackInSlot(0).getItem() instanceof MissileItem)) {
            return;
        }
        MissileItem item = (MissileItem) tile.getStackInSlot(0).getItem();
        String id = item.getProfile().getIntentPath();
        GL11.glPushMatrix();
        GL11.glTranslated(0.0D, 1.0D, 0.0D);
        float scale=com.wartec.wartecmod.port.entity.VehicleDimensions.missileScale(id);
        GL11.glScalef(scale,scale,scale);
        // The dev66 launcher intentionally used the neutron skin for loaded ASAT.
        renderLegacyMissile("asat".equals(id) ? "micro_neutron" : id,
                90.0F, 0.0F, 1);
        GL11.glPopMatrix();
    }

    private static void renderLaunchTube(TileEntityWarTechMachine tile,
            float partialTicks) {
        LegacyObjModel tube = model("models/blocks/launch_tube.obj");
        bind("textures/models/blocks/launcher_tex.png");
        tube.renderPart("base");
        GL11.glPushMatrix();
        GL11.glTranslated(-0.5D, 11.0D, 0.0D);
        GL11.glRotated(openingAngle(tile, partialTicks),
                0.0D, 0.0D, 1.0D);
        GL11.glTranslated(0.5D, 0.0D, 0.0D);
        bind("textures/models/blocks/launcher_cover_tex.png");
        tube.renderPart("cover");
        GL11.glPopMatrix();
        if (tile != null && !tile.getStackInSlot(0).isEmpty()
                && tile.getStackInSlot(0).getItem() instanceof MissileItem) {
            GL11.glPushMatrix();
            GL11.glTranslated(0.0D, 1.0D, 0.0D);
            MissileItem item = (MissileItem) tile.getStackInSlot(0).getItem();
            renderLoadedLaunchTubeMissile(item.getProfile());
            GL11.glPopMatrix();
        }
    }

    private static void renderLoadedLaunchTubeMissile(
            MissileProfile profile) {
        String id = profile.getIntentPath();
        float scale=com.wartec.wartecmod.port.entity.VehicleDimensions.missileScale(id);
        GL11.glScalef(scale,scale,scale);
        if (profile.getFlightClass() == MissileProfile.FlightClass.HYPERSONIC) {
            bind(id.contains("nuclear")
                    ? "textures/models/entity_hypersonic_cruise_missile_nuclear_tex.png"
                    : "textures/models/entity_hypersonic_cruise_missile_tex.png");
            model("models/entity_hypersonic_cruise_missile_scramjet.obj")
                    .renderAll();
            model("models/entity_hypersonic_cruise_missile_booster.obj")
                    .renderAll();
            return;
        }
        if (profile.getFlightClass() == MissileProfile.FlightClass.SUPERSONIC) {
            bind("textures/models/entity_supersonic_cruise_missile_ramjet_tex.png");
            model("models/entity_supersonic_cruise_missile_ramjet.obj")
                    .renderAll();
            bind(id.contains("hydrogen")
                    ? "textures/models/entity_supersonic_cruise_missile_fuselage_h_tex.png"
                    : "textures/models/entity_supersonic_cruise_missile_fuselage_tex.png");
            model("models/entity_supersonic_cruise_missile_fuselage.obj")
                    .renderAll();
            bind("textures/models/entity_supersonic_cruise_missile_engine_tex.png");
            model("models/entity_supersonic_cruise_missile_engine.obj")
                    .renderAll();
            bind("textures/models/entity_supersonic_cruise_missile_booster_tex.png");
            model("models/entity_supersonic_cruise_missile_booster.obj")
                    .renderAll();
            bind("textures/models/entity_supersonic_cruise_missile_protection_tex.png");
            model("models/entity_supersonic_cruise_missile_protection.obj")
                    .renderAll();
            return;
        }
        if (profile == MissileProfile.KALIBR) {
            bind("textures/models/entity_kalibr_missile_tex.png");
            model("models/entity_kalibr_missile_base.obj").renderAll();
            model("models/entity_kalibr_missile_booster.obj").renderAll();
            model("models/entity_kalibr_missile_fins_folded.obj").renderAll();
            return;
        }
        if (profile == MissileProfile.ANTI_AIR_TIER_1) {
            renderSingle("models/entity_missile_anti_air_tier1.obj",
                    "textures/models/entity_missile_anti_air_tier1.png");
            return;
        }

        String texture = "textures/models/entity_cruise_missile_tex.png";
        if (id.contains("cluster")) {
            texture = "textures/models/entity_cruise_missile_tex_cluster.png";
        } else if (id.contains("buster")) {
            texture = "textures/models/entity_cruise_missile_tex_buster.png";
        } else if (id.contains("emp")) {
            texture = "textures/models/entity_cruise_missile_tex_emp.png";
        } else if (id.contains("thermobaric")) {
            texture = "textures/models/entity_cruise_missile_tex_fae.png";
        } else if (id.contains("hydrogen")) {
            texture = "textures/models/entity_cruise_missile_tex_h.png";
        } else if (id.contains("nuclear")) {
            texture = "textures/models/entity_cruise_missile_tex_nuclear.png";
        } else if (profile == MissileProfile.TOMAHAWK) {
            texture = "textures/models/entity_tomahawk_missile_tex.png";
        }
        bind(texture);
        model("models/entity_cruise_missile_base.obj").renderAll();
        model("models/entity_cruise_missile_booster.obj").renderAll();
        model("models/entity_cruise_missile_sealing.obj").renderAll();
    }

    private static float openingAngle(TileEntityWarTechMachine tile,
            float partialTicks) {
        if (tile == null) {
            return 0.0F;
        }
        float angle = tile.getOpeningAnimation();
        if (tile.isOpen() && angle < 90.0F) {
            angle += partialTicks * 3.0F;
        } else if (!tile.isOpen() && angle > 0.0F) {
            angle -= partialTicks * 3.0F;
        }
        return Math.max(0.0F, Math.min(90.0F, angle));
    }

    private static void renderDeco(String name) {
        if (name.startsWith("decoblockflag")) {
            String suffix = name.substring("decoblockflag".length());
            bind("textures/models/blocks/block_flag_tex_" + suffix + ".png");
            model("models/blocks/block_flag.obj").renderAll();
        } else if ("decoblocksatellitenuclear".equals(name)) {
            renderSingle("models/blocks/block_sat_nuclear_base.obj",
                    "textures/models/blocks/sat_nuclear_base_tex.png");
            renderSingle("models/blocks/block_sat_nuclear_com.obj",
                    "textures/models/blocks/sat_nuclear_com_tex.png");
            renderSingle("models/blocks/block_sat_nuclear_launcher.obj",
                    "textures/models/blocks/sat_nuclear_launcher_tex.png");
            renderSingle("models/blocks/block_sat_nuclear_missiles.obj",
                    "textures/models/entity_sat_nuclear_missile_tex.png");
        } else if (name.contains("supersonic")) {
            renderSingle("models/blocks/block_supersonic_cruise_missile_ramjet.obj",
                    "textures/models/entity_supersonic_cruise_missile_ramjet_tex.png");
            renderSingle("models/blocks/block_supersonic_cruise_missile_fuselage.obj",
                    name.endsWith("h")
                            ? "textures/models/entity_supersonic_cruise_missile_fuselage_h_tex.png"
                            : "textures/models/entity_supersonic_cruise_missile_fuselage_tex.png");
            renderSingle("models/blocks/block_supersonic_cruise_missile_engine.obj",
                    "textures/models/entity_supersonic_cruise_missile_engine_tex.png");
            renderSingle("models/blocks/block_supersonic_cruise_missile_booster.obj",
                    "textures/models/entity_supersonic_cruise_missile_booster_tex.png");
            renderSingle("models/blocks/block_supersonic_cruise_missile_protection.obj",
                    "textures/models/entity_supersonic_cruise_missile_protection_tex.png");
        } else if (name.contains("hypersonic")) {
            renderSingle("models/blocks/block_hypersonic_cruise_missile.obj",
                    name.contains("nuclear")
                            ? "textures/models/entity_hypersonic_cruise_missile_nuclear_tex.png"
                            : "textures/models/entity_hypersonic_cruise_missile_tex.png");
        } else {
            String texture = "textures/models/entity_cruise_missile_tex.png";
            if (name.contains("cluster")) texture = "textures/models/entity_cruise_missile_tex_cluster.png";
            else if (name.contains("buster")) texture = "textures/models/entity_cruise_missile_tex_buster.png";
            else if (name.contains("emp")) texture = "textures/models/entity_cruise_missile_tex_emp.png";
            else if (name.contains("fae")) texture = "textures/models/entity_cruise_missile_tex_fae.png";
            else if (name.contains("nuclear")) texture = "textures/models/entity_cruise_missile_tex_nuclear.png";
            else if (name.endsWith("h")) texture = "textures/models/entity_cruise_missile_tex_h.png";
            renderSingle("models/blocks/block_cruise_missile.obj", texture);
        }
    }

    private static void tintAntiAir(String name) {
        if (name.endsWith("2")) {
            GL11.glColor4f(0.78F, 0.9F, 1.0F, 1.0F);
        } else if (name.endsWith("3")) {
            GL11.glColor4f(1.0F, 0.82F, 0.68F, 1.0F);
        } else {
            GL11.glColor4f(0.85F, 1.0F, 0.85F, 1.0F);
        }
    }

    private static String resolveVisual(EntityWarTechBase entity) {
        String visual = entity.getVisualId().toLowerCase(Locale.ROOT);
        if (!visual.isEmpty()) {
            return visual;
        }
        switch (entity.getProfile()) {
            case KH_555: return "missile/kh555";
            case AGM_88_HARM: return "missile/anti_radiation";
            case GERAN_2: return "missile/geran_2";
            case MQ_9_REAPER: return "deployment/mq9_reaper";
            case F_16C: return "deployment/f16_tactical_aircraft";
            case SU_27: return "deployment/su27_tactical_aircraft";
            case TU_95: return "deployment/tu95_strategic_bomber";
            case KINETIC_ROD: return "satellite/kinetic_bombardment";
            case FAB_5000:
            case KAB_3000: return "ordnance/strategic_bomb";
            case COMMAND_TRUCK: return "deployment/air_defense_command_truck";
            case RADAR_TRUCK: return "deployment/mobile_radar_truck";
            case S400_RADAR: return "deployment/s400_long_range_radar";
            case MOBILE_AIR_DEFENSE: return "deployment/mobile_air_defense";
            case MOBILE_ARTILLERY: return "deployment/mobile_artillery";
            case ELECTRONIC_WARFARE: return "deployment/electronic_warfare_unit";
            case STRATEGIC_TOPOL_M: return "deployment/strategic_topol_m";
            case STRATEGIC_YARS: return "deployment/strategic_yars";
            case STRATEGIC_ORESHNIK: return "deployment/strategic_oreshnik";
            case STRATEGIC_FLIGHT: return "strategic_flight/boost";
            default: return "missile/storm_shadow";
        }
    }

    private static void renderSingle(String modelPath, String texturePath) {
        bind(texturePath);
        model(modelPath).renderAll();
    }

    private static void renderParts(LegacyObjModel model, String[] parts) {
        for (String part : parts) {
            model.renderPart(part);
        }
    }

    private static LegacyObjModel model(String path) {
        String normalized = path.toLowerCase(Locale.ROOT);
        LegacyObjModel model = MODELS.get(normalized);
        if (model == null) {
            model = new LegacyObjModel(normalized);
            MODELS.put(normalized, model);
        }
        return model;
    }

    private static void bind(String path) {
        if (LegacyObjModel.isMeasuring()) {
            return;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        Minecraft.getMinecraft().getTextureManager().bindTexture(
                normalized.indexOf(':') >= 0
                        ? new ResourceLocation(normalized)
                        : new ResourceLocation(WarTechReforged.MODID, normalized));
    }

    private static void setup() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float interpolate(float previous, float current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    private static float tacticalFlightPitchTrim(EntityWarTechBase entity) {
        int state = entity.getLegacyState();
        return state != 0 && state != 6 ? 3.5F : 0.0F;
    }

    private static float f16RenderPitch(EntityWarTechBase entity,
            float pitch) {
        if (!(entity instanceof EntityWarTechAircraft)
                || entity.getLegacyState() == 0
                || entity.getLegacyState() == 6) {
            return pitch;
        }
        return Math.max(-32.0F, Math.min(24.0F, pitch));
    }

    private static void applyTacticalWreckAttitude(EntityWarTechBase entity) {
        if (entity.getLegacyState() == 6) {
            GL11.glRotatef(14.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-9.0F, 0.0F, 0.0F, 1.0F);
        }
    }

}
