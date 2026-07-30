package com.wartec.wartecmod.compat.client;

import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.compat.AviationOrdnance;
import com.wartec.wartecmod.compat.RemoteControlNetwork;
import com.wartec.wartecmod.entity.missile.EntityGeran;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityMq9Munition;
import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Client camera, controls and compact flight HUD for remotely piloted MQ-9s. */
public final class RemoteControlClient {
    private static final RemoteControlClient INSTANCE = new RemoteControlClient();
    private static final String CATEGORY = "WarTech Remote Flight";
    private static final GameProfile CAMERA_PROFILE = new GameProfile(
            UUID.fromString("97e9b947-e66e-4b24-9490-42ef7f7027b5"),
            "WarTech_MQ9_Camera");

    private static final KeyBinding CAMERA = new KeyBinding(
            "Camera mode", Keyboard.KEY_C, CATEGORY);
    private static final KeyBinding FLARES = new KeyBinding(
            "Deploy flares", Keyboard.KEY_F, CATEGORY);
    private static final KeyBinding CYCLE = new KeyBinding(
            "Cycle weapon", Keyboard.KEY_Z, CATEGORY);
    private static final KeyBinding EXIT = new KeyBinding(
            "Exit remote control", Keyboard.KEY_X, CATEGORY);
    private static final KeyBinding COURSE_HOLD = new KeyBinding(
            "Course hold / free look", Keyboard.KEY_R, CATEGORY);

    private static volatile int pendingEntityId = -1;
    private static volatile boolean pendingActive;
    private static volatile int pendingVehicleType =
            RemoteControlNetwork.VEHICLE_MQ9;
    private static volatile String pendingMessage = "";
    private static volatile RemoteFrame pendingTelemetry;
    private static volatile RemoteEffect pendingEffect;

    private int entityId = -1;
    private int vehicleType = RemoteControlNetwork.VEHICLE_MQ9;
    private RemoteCameraEntity camera;
    private float throttle;
    private float controlYaw;
    private float controlPitch;
    private float flightYaw;
    private float flightPitch;
    private float playerAnchorYaw;
    private float playerAnchorPitch;
    private int cameraMode;
    private int inputTicks;
    private boolean cameraKeyDown;
    private boolean flareKeyDown;
    private boolean cycleKeyDown;
    private boolean exitKeyDown;
    private boolean holdKeyDown;
    private boolean fireMouseDown;
    private boolean cameraToggleRequested;
    private boolean courseHoldToggleRequested;
    private boolean courseHold;
    private boolean exitAwaitingServer;
    private int pendingRawFlags;
    private boolean cameraPositionReady;
    private RemoteFrame latestTelemetry;
    private int telemetryAge;
    private boolean remoteFrameReady;
    private double frameX;
    private double frameY;
    private double frameZ;
    private double frameMotionX;
    private double frameMotionY;
    private double frameMotionZ;
    private float frameYaw;
    private float framePitch;
    private double visualX;
    private double visualY;
    private double visualZ;
    private boolean visualReady;
    private float cameraViewYaw;
    private float cameraViewPitch;
    private float visualControlYaw;
    private float visualControlPitch;
    private double renderDeltaTicks;
    private long lastRenderNanos;

    private RemoteControlClient() {
    }

    public static void register() {
        ClientRegistry.registerKeyBinding(CAMERA);
        ClientRegistry.registerKeyBinding(FLARES);
        ClientRegistry.registerKeyBinding(CYCLE);
        ClientRegistry.registerKeyBinding(EXIT);
        ClientRegistry.registerKeyBinding(COURSE_HOLD);
        FMLCommonHandler.instance().bus().register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static void acceptServerState(int entityId, boolean active,
            int vehicleType, String message) {
        pendingEntityId = entityId;
        pendingActive = active;
        pendingVehicleType = vehicleType;
        pendingMessage = message == null ? "" : message;
    }

    public static void acceptTelemetry(int entityId, int vehicleType,
            double x, double y, double z,
            double motionX, double motionY, double motionZ, float yaw, float pitch,
            float throttle, int power, int maxPower, int healthPercent, int flares,
            boolean airborne, int selectedHardpoint, int payloadMask,
            int payloadCounts, int distance, int maxRange, String weapon) {
        pendingTelemetry = new RemoteFrame(entityId, vehicleType,
                x, y, z, motionX, motionY,
                motionZ, yaw, pitch, throttle, power, maxPower, healthPercent,
                flares, airborne, selectedHardpoint, payloadMask, payloadCounts,
                distance, maxRange, weapon);
    }

    public static void acceptEffect(int entityId, int effect, double x,
            double y, double z, double targetX, double targetY, double targetZ,
            int payload) {
        pendingEffect = new RemoteEffect(entityId, effect, x, y, z, targetX,
                targetY, targetZ, payload);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (entityId < 0 || !Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_C) cameraToggleRequested = true;
        if (key == Keyboard.KEY_F) pendingRawFlags |= RemoteControlNetwork.FLAG_FLARES;
        if (key == Keyboard.KEY_Z) {
            pendingRawFlags |= RemoteControlNetwork.FLAG_CYCLE_WEAPON;
        }
        if (key == Keyboard.KEY_X) pendingRawFlags |= RemoteControlNetwork.FLAG_EXIT;
        if (key == Keyboard.KEY_R) courseHoldToggleRequested = true;
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (entityId >= 0 && Mouse.getEventButton() == 0
                && Mouse.getEventButtonState()) {
            pendingRawFlags |= RemoteControlNetwork.FLAG_FIRE;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.func_71410_x();
        if (minecraft == null) return;
        applyPendingState(minecraft);
        applyPendingTelemetry();
        applyPendingEffect(minecraft);
        if (entityId < 0) return;
        if (minecraft.field_71441_e == null || minecraft.field_71439_g == null) {
            stopLocal(minecraft);
            return;
        }
        Entity value = minecraft.field_71441_e.func_73045_a(entityId);
        Entity remoteEntity = isSupportedRemoteEntity(value) ? value : null;
        if (latestTelemetry == null && remoteEntity == null) {
            stopLocal(minecraft);
            return;
        }
        if (latestTelemetry != null && telemetryAge < 1000000) telemetryAge++;
        if (!remoteFrameReady) seedRemoteFrame(remoteEntity);
        if (camera == null) {
            camera = new RemoteCameraEntity(minecraft.field_71441_e);
            controlYaw = frameYaw;
            controlPitch = clamp(framePitch, getViewPitchMinimum(),
                    getViewPitchMaximum());
            flightYaw = controlYaw;
            flightPitch = controlPitch;
            visualControlYaw = controlYaw;
            visualControlPitch = controlPitch;
            playerAnchorYaw = minecraft.field_71439_g.field_70177_z;
            playerAnchorPitch = minecraft.field_71439_g.field_70125_A;
            cameraPositionReady = false;
            updateCamera();
            minecraft.field_71441_e.func_72838_d(camera);
            minecraft.field_71451_h = camera;
        }
        if (minecraft.field_71462_r != null) {
            pendingRawFlags = 0;
            cameraToggleRequested = false;
            courseHoldToggleRequested = false;
            cameraKeyDown = Keyboard.isKeyDown(Keyboard.KEY_C);
            flareKeyDown = Keyboard.isKeyDown(Keyboard.KEY_F);
            cycleKeyDown = Keyboard.isKeyDown(Keyboard.KEY_Z);
            exitKeyDown = Keyboard.isKeyDown(Keyboard.KEY_X);
            holdKeyDown = Keyboard.isKeyDown(Keyboard.KEY_R);
            fireMouseDown = Mouse.isButtonDown(0);
            return;
        }
        if (exitAwaitingServer) return;

        int flags = pendingRawFlags;
        pendingRawFlags = 0;
        boolean firePressed =
                minecraft.field_71474_y.field_74312_F.func_151468_f();
        boolean fireDown = minecraft.field_71474_y.field_74312_F.func_151470_d()
                || Mouse.isButtonDown(0);
        if (firePressed || fireDown && !fireMouseDown) {
            flags |= RemoteControlNetwork.FLAG_FIRE;
        }
        fireMouseDown = fireDown;

        boolean flarePressed = FLARES.func_151468_f();
        boolean flareDown = FLARES.func_151470_d()
                || Keyboard.isKeyDown(Keyboard.KEY_F);
        if (flarePressed || flareDown && !flareKeyDown) {
            flags |= RemoteControlNetwork.FLAG_FLARES;
        }
        flareKeyDown = flareDown;

        boolean cyclePressed = CYCLE.func_151468_f();
        boolean cycleDown = CYCLE.func_151470_d()
                || Keyboard.isKeyDown(Keyboard.KEY_Z);
        if (cyclePressed || cycleDown && !cycleKeyDown) {
            flags |= RemoteControlNetwork.FLAG_CYCLE_WEAPON;
        }
        cycleKeyDown = cycleDown;

        boolean exitPressed = EXIT.func_151468_f();
        boolean exitDown = EXIT.func_151470_d()
                || Keyboard.isKeyDown(Keyboard.KEY_X);
        if (exitPressed || exitDown && !exitKeyDown) {
            flags |= RemoteControlNetwork.FLAG_EXIT;
        }
        exitKeyDown = exitDown;

        boolean cameraPressed = CAMERA.func_151468_f();
        boolean cameraDown = CAMERA.func_151470_d()
                || Keyboard.isKeyDown(Keyboard.KEY_C);
        if (cameraToggleRequested || cameraPressed
                || cameraDown && !cameraKeyDown) {
            cameraMode = (cameraMode + 1) % 2;
            cameraPositionReady = false;
            cameraToggleRequested = false;
            minecraft.field_71439_g.func_145747_a(new ChatComponentText(
                    getVehicleName(vehicleType) + (cameraMode == 0
                            ? " camera: NOSE" : " camera: CHASE")));
        }
        cameraKeyDown = cameraDown;

        boolean holdPressed = COURSE_HOLD.func_151468_f();
        boolean holdDown = COURSE_HOLD.func_151470_d()
                || Keyboard.isKeyDown(Keyboard.KEY_R);
        if (courseHoldToggleRequested || holdPressed
                || holdDown && !holdKeyDown) {
            courseHold = !courseHold;
            courseHoldToggleRequested = false;
            if (courseHold) {
                flightYaw = frameYaw;
                flightPitch = clamp(framePitch, getFlightPitchMinimum(),
                        getFlightPitchMaximum());
            } else {
                flightYaw = controlYaw;
                flightPitch = controlPitch;
            }
            minecraft.field_71439_g.func_145747_a(new ChatComponentText(
                    getVehicleName(vehicleType) + (courseHold
                            ? " course hold: FREE LOOK"
                            : " course hold: OFF")));
        }
        holdKeyDown = holdDown;

        if (minecraft.field_71474_y.field_74351_w.func_151470_d()
                || Keyboard.isKeyDown(Keyboard.KEY_W)) {
            throttle = Math.min(1.0F, throttle + 0.018F);
        }
        if (minecraft.field_71474_y.field_74368_y.func_151470_d()
                || Keyboard.isKeyDown(Keyboard.KEY_S)) {
            throttle = Math.max(0.0F, throttle - 0.018F);
        }
        boolean turnLeft = minecraft.field_71474_y.field_74370_x.func_151470_d()
                || Keyboard.isKeyDown(Keyboard.KEY_A);
        boolean turnRight = minecraft.field_71474_y.field_74366_z.func_151470_d()
                || Keyboard.isKeyDown(Keyboard.KEY_D);
        if (turnLeft != turnRight) {
            float turnStep = turnLeft
                    ? -getKeyboardTurnStep() : getKeyboardTurnStep();
            flags |= turnLeft ? RemoteControlNetwork.FLAG_TURN_LEFT
                    : RemoteControlNetwork.FLAG_TURN_RIGHT;
            flightYaw += turnStep;
            if (!courseHold) controlYaw += turnStep;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            if (courseHold) flightPitch -= getFlightPitchStep();
            else controlPitch -= getViewPitchStep();
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            if (courseHold) flightPitch += getFlightPitchStep();
            else controlPitch += getViewPitchStep();
        }
        controlYaw = normalizeAngle(controlYaw);
        controlPitch = clamp(controlPitch, getViewPitchMinimum(),
                getViewPitchMaximum());
        if (!courseHold) {
            flightYaw = controlYaw;
            flightPitch = controlPitch;
        }
        flightYaw = normalizeAngle(flightYaw);
        flightPitch = clamp(flightPitch, getFlightPitchMinimum(),
                getFlightPitchMaximum());

        if (++inputTicks >= 1 || flags != 0) {
            inputTicks = 0;
            RemoteControlNetwork.sendInput(entityId, flightYaw, flightPitch,
                    controlYaw, controlPitch, throttle, flags);
        }
        if ((flags & RemoteControlNetwork.FLAG_EXIT) != 0) {
            exitAwaitingServer = true;
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft minecraft = Minecraft.func_71410_x();
        if (event.phase == TickEvent.Phase.END) {
            if (entityId >= 0 && minecraft != null
                    && minecraft.field_71439_g != null && camera != null) {
                applyMouseLook(minecraft);
                minecraft.field_71451_h = camera;
            }
            return;
        }
        if (event.phase != TickEvent.Phase.START || entityId < 0) return;
        if (minecraft == null || minecraft.field_71441_e == null
                || minecraft.field_71439_g == null || camera == null) return;
        applyPendingTelemetry();
        Entity value = minecraft.field_71441_e.func_73045_a(entityId);
        Entity remoteEntity = isSupportedRemoteEntity(value) ? value : null;
        updateRemoteFrame(remoteEntity, Math.max(0.25D,
                Math.min(1.0D, event.renderTickTime)));
        updateCamera();
        minecraft.field_71451_h = camera;
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (entityId < 0) return;
        switch (event.type) {
            case CROSSHAIRS:
            case HOTBAR:
            case HEALTH:
            case ARMOR:
            case FOOD:
            case AIR:
            case EXPERIENCE:
            case HEALTHMOUNT:
            case JUMPBAR:
                event.setCanceled(true);
                break;
            default:
                break;
        }
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (entityId >= 0) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (entityId < 0 || event == null) return;
        Minecraft minecraft = Minecraft.func_71410_x();
        if (minecraft != null && event.entityPlayer == minecraft.field_71439_g) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post event) {
        if (entityId < 0 || event.type != RenderGameOverlayEvent.ElementType.ALL
                || event.resolution == null) return;
        if (entityId < 0) return;
        Minecraft minecraft = Minecraft.func_71410_x();
        if (minecraft == null || minecraft.field_71441_e == null
                || minecraft.field_71466_p == null) return;
        Entity value = minecraft.field_71441_e.func_73045_a(entityId);
        RemoteFrame frame = getHudFrame(isSupportedRemoteEntity(value)
                ? value : null);
        if (frame == null) return;
        int width = event.resolution.func_78326_a();
        int height = event.resolution.func_78328_b();
        FontRenderer font = minecraft.field_71466_p;
        double speed = Math.sqrt(frame.motionX * frame.motionX
                + frame.motionY * frame.motionY + frame.motionZ * frame.motionZ);
        drawOperatorHud(font, width, height, frame, speed);
    }

    private void applyPendingState(Minecraft minecraft) {
        int requested = pendingEntityId;
        if (requested < 0) return;
        boolean active = pendingActive;
        int requestedVehicleType = pendingVehicleType;
        String message = pendingMessage;
        pendingEntityId = -1;
        pendingMessage = "";
        if (message.length() > 0 && minecraft.field_71439_g != null) {
            minecraft.field_71439_g.func_145747_a(new ChatComponentText(message));
        }
        if (!active) {
            stopLocal(minecraft);
            return;
        }
        entityId = requested;
        vehicleType = requestedVehicleType;
        throttle = getInitialThrottle(vehicleType);
        cameraMode = 0;
        inputTicks = 0;
        cameraKeyDown = false;
        flareKeyDown = false;
        cycleKeyDown = false;
        exitKeyDown = false;
        holdKeyDown = false;
        fireMouseDown = false;
        cameraToggleRequested = false;
        courseHoldToggleRequested = false;
        courseHold = false;
        exitAwaitingServer = false;
        pendingRawFlags = 0;
        cameraPositionReady = false;
        latestTelemetry = null;
        telemetryAge = 0;
        remoteFrameReady = false;
        visualReady = false;
        lastRenderNanos = 0L;
        if (minecraft.field_71439_g != null) {
            playerAnchorYaw = minecraft.field_71439_g.field_70177_z;
            playerAnchorPitch = minecraft.field_71439_g.field_70125_A;
        }
        minecraft.func_147108_a(null);
    }

    private void applyPendingTelemetry() {
        RemoteFrame frame = pendingTelemetry;
        if (frame == null) return;
        pendingTelemetry = null;
        if (entityId == frame.entityId) {
            latestTelemetry = frame;
            vehicleType = frame.vehicleType;
            telemetryAge = 0;
        }
    }

    private void applyPendingEffect(Minecraft minecraft) {
        RemoteEffect effect = pendingEffect;
        if (effect == null) return;
        pendingEffect = null;
        if (effect.entityId != entityId || minecraft.field_71441_e == null
                || minecraft.field_71439_g == null) return;
        double dx = minecraft.field_71439_g.field_70165_t - effect.x;
        double dy = minecraft.field_71439_g.field_70163_u - effect.y;
        double dz = minecraft.field_71439_g.field_70161_v - effect.z;
        double bodyDistanceSquared = dx * dx + dy * dy + dz * dz;
        if (effect.effect == RemoteControlNetwork.EFFECT_FLARES
                && bodyDistanceSquared > 65536.0D) {
            for (int index = 0; index < 36; index++) {
                double angle = index * Math.PI * 2.0D / 36.0D;
                double spread = 0.08D + index % 5 * 0.018D;
                minecraft.field_71441_e.func_72869_a(
                        index % 3 == 0 ? "flame" : "fireworksSpark",
                        effect.x, effect.y, effect.z,
                        Math.cos(angle) * spread,
                        -0.035D - index % 4 * 0.012D,
                        Math.sin(angle) * spread);
            }
            return;
        }
        if (effect.effect == RemoteControlNetwork.EFFECT_WEAPON
                && bodyDistanceSquared > 1048576.0D) {
            RemoteVisualMunition visual = new RemoteVisualMunition(
                    minecraft.field_71441_e, effect);
            minecraft.field_71441_e.func_72838_d(visual);
            for (int index = 0; index < 8; index++) {
                minecraft.field_71441_e.func_72869_a(
                        index % 2 == 0 ? "flame" : "smoke",
                        effect.x, effect.y, effect.z,
                        (index - 3.5D) * 0.018D, -0.03D,
                        (3.5D - index) * 0.018D);
            }
        }
    }

    private void applyMouseLook(Minecraft minecraft) {
        float playerYaw = minecraft.field_71439_g.field_70177_z;
        float playerPitch = minecraft.field_71439_g.field_70125_A;
        float cameraYawDelta = normalizeAngle(
                camera.field_70177_z - cameraViewYaw);
        float cameraPitchDelta = camera.field_70125_A - cameraViewPitch;
        float playerYawDelta = normalizeAngle(playerYaw - playerAnchorYaw);
        float playerPitchDelta = playerPitch - playerAnchorPitch;
        float cameraMovement = Math.abs(cameraYawDelta)
                + Math.abs(cameraPitchDelta);
        float playerMovement = Math.abs(playerYawDelta)
                + Math.abs(playerPitchDelta);
        if (cameraMovement >= playerMovement && cameraMovement > 0.0001F) {
            controlYaw = normalizeAngle(controlYaw + cameraYawDelta);
            controlPitch += cameraPitchDelta;
            visualControlYaw = normalizeAngle(
                    visualControlYaw + cameraYawDelta);
            visualControlPitch += cameraPitchDelta;
        } else if (playerMovement > 0.0001F) {
            controlYaw = normalizeAngle(controlYaw + playerYawDelta);
            controlPitch += playerPitchDelta;
            visualControlYaw = normalizeAngle(
                    visualControlYaw + playerYawDelta);
            visualControlPitch += playerPitchDelta;
        }
        controlPitch = clamp(controlPitch, getViewPitchMinimum(),
                getViewPitchMaximum());
        visualControlPitch = clamp(visualControlPitch, getViewPitchMinimum(),
                getViewPitchMaximum());
        if (!courseHold) {
            flightYaw = controlYaw;
            flightPitch = controlPitch;
        }
        minecraft.field_71439_g.field_70177_z = playerAnchorYaw;
        minecraft.field_71439_g.field_70125_A = playerAnchorPitch;
        camera.field_70126_B = camera.field_70177_z = cameraViewYaw;
        camera.field_70127_C = camera.field_70125_A = cameraViewPitch;
    }

    private void updateRemoteFrame(Entity remoteEntity, double tickFraction) {
        RemoteFrame source = latestTelemetry != null
                ? latestTelemetry : snapshotFromEntity(remoteEntity);
        if (source == null) return;
        if (!remoteFrameReady) {
            seedRemoteFrame(source);
            return;
        }
        long now = System.nanoTime();
        double elapsedTicks = lastRenderNanos == 0L ? 0.0D
                : clamp((now - lastRenderNanos) / 50000000.0D, 0.0D, 1.25D);
        renderDeltaTicks = elapsedTicks;
        lastRenderNanos = now;
        frameX += frameMotionX * elapsedTicks;
        frameY += frameMotionY * elapsedTicks;
        frameZ += frameMotionZ * elapsedTicks;

        double predictionTicks = Math.min(4.0D, Math.max(0.0D,
                telemetryAge - 1.0D + tickFraction));
        double targetX = source.x + source.motionX * predictionTicks;
        double targetY = source.y + source.motionY * predictionTicks;
        double targetZ = source.z + source.motionZ * predictionTicks;
        double errorX = targetX - frameX;
        double errorY = targetY - frameY;
        double errorZ = targetZ - frameZ;
        double errorSquared = errorX * errorX + errorY * errorY
                + errorZ * errorZ;
        if (errorSquared > 576.0D) {
            frameX = targetX;
            frameY = targetY;
            frameZ = targetZ;
        } else {
            double correction = 1.0D - Math.exp(-elapsedTicks * 0.34D);
            frameX += errorX * correction;
            frameY += errorY * correction;
            frameZ += errorZ * correction;
        }
        double velocityBlend = 1.0D - Math.exp(-elapsedTicks * 0.55D);
        frameMotionX += (source.motionX - frameMotionX) * velocityBlend;
        frameMotionY += (source.motionY - frameMotionY) * velocityBlend;
        frameMotionZ += (source.motionZ - frameMotionZ) * velocityBlend;
        float headingBlend = (float) (1.0D
                - Math.exp(-elapsedTicks * 0.30D));
        frameYaw = normalizeAngle(frameYaw
                + normalizeAngle(source.yaw - frameYaw) * headingBlend);
        framePitch += (source.pitch - framePitch)
                * (float) Math.min(1.0D, elapsedTicks * 0.4D);
    }

    private void seedRemoteFrame(Entity remoteEntity) {
        RemoteFrame source = latestTelemetry != null
                ? latestTelemetry : snapshotFromEntity(remoteEntity);
        if (source != null) seedRemoteFrame(source);
    }

    private void seedRemoteFrame(RemoteFrame source) {
        frameX = source.x;
        frameY = source.y;
        frameZ = source.z;
        frameMotionX = source.motionX;
        frameMotionY = source.motionY;
        frameMotionZ = source.motionZ;
        frameYaw = source.yaw;
        framePitch = source.pitch;
        remoteFrameReady = true;
        lastRenderNanos = System.nanoTime();
    }

    private void updateCamera() {
        double viewBlend = 1.0D
                - Math.exp(-Math.max(0.02D, renderDeltaTicks) * 0.72D);
        visualControlYaw = normalizeAngle(visualControlYaw
                + normalizeAngle(controlYaw - visualControlYaw)
                * (float) viewBlend);
        visualControlPitch += (controlPitch - visualControlPitch)
                * (float) viewBlend;
        double yaw = Math.toRadians(frameYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double distance = cameraMode == 0
                ? getNoseCameraDistance() : getChaseCameraDistance();
        double height = cameraMode == 0
                ? getNoseCameraHeight() : getChaseCameraHeight();
        double targetX = frameX + forwardX * distance;
        double targetY = frameY + height;
        double targetZ = frameZ + forwardZ * distance;
        if (cameraMode == 1 && cameraPositionReady && visualReady) {
            double cameraBlend = 1.0D
                    - Math.exp(-Math.max(0.02D, renderDeltaTicks) * 0.82D);
            visualX += (targetX - visualX) * cameraBlend;
            visualY += (targetY - visualY) * cameraBlend;
            visualZ += (targetZ - visualZ) * cameraBlend;
            targetX = visualX;
            targetY = visualY;
            targetZ = visualZ;
        } else {
            visualX = targetX;
            visualY = targetY;
            visualZ = targetZ;
        }
        visualReady = true;
        cameraPositionReady = true;
        cameraViewYaw = visualControlYaw;
        cameraViewPitch = visualControlPitch;
        if (cameraMode == 1) {
            double focusX = frameX + forwardX * getChaseFocusDistance();
            double focusY = frameY + getChaseFocusHeight();
            double focusZ = frameZ + forwardZ * getChaseFocusDistance();
            double viewX = focusX - targetX;
            double viewY = focusY - targetY;
            double viewZ = focusZ - targetZ;
            cameraViewYaw = (float) Math.toDegrees(Math.atan2(-viewX, viewZ));
            cameraViewPitch = (float) -Math.toDegrees(Math.atan2(viewY,
                    Math.sqrt(viewX * viewX + viewZ * viewZ)));
        }
        placeCamera(targetX, targetY, targetZ,
                cameraViewYaw, cameraViewPitch);
    }

    private void placeCamera(double x, double y, double z,
            float viewYaw, float viewPitch) {
        camera.field_70142_S = x;
        camera.field_70137_T = y;
        camera.field_70136_U = z;
        camera.field_70169_q = x;
        camera.field_70167_r = y;
        camera.field_70166_s = z;
        camera.func_70107_b(x, y, z);
        camera.field_70177_z = viewYaw;
        camera.field_70126_B = viewYaw;
        camera.field_70125_A = viewPitch;
        camera.field_70127_C = viewPitch;
    }

    private void stopLocal(Minecraft minecraft) {
        entityId = -1;
        if (camera != null) camera.func_70106_y();
        camera = null;
        cameraPositionReady = false;
        cameraKeyDown = false;
        flareKeyDown = false;
        cycleKeyDown = false;
        exitKeyDown = false;
        holdKeyDown = false;
        fireMouseDown = false;
        cameraToggleRequested = false;
        courseHoldToggleRequested = false;
        courseHold = false;
        vehicleType = RemoteControlNetwork.VEHICLE_MQ9;
        exitAwaitingServer = false;
        pendingRawFlags = 0;
        latestTelemetry = null;
        telemetryAge = 0;
        remoteFrameReady = false;
        visualReady = false;
        lastRenderNanos = 0L;
        if (minecraft != null && minecraft.field_71439_g != null) {
            minecraft.field_71451_h = minecraft.field_71439_g;
        }
    }

    private void drawOperatorHud(FontRenderer font, int width, int height,
            RemoteFrame frame, double speed) {
        final int green = 0xFF79F2B0;
        final int pale = 0xFFD6FFE8;
        final int amber = 0xFFFFCC58;
        final int shade = 0x98040D0A;
        int centerX = width / 2;
        int centerY = height / 2;

        Gui.func_73734_a(0, 0, width, 18, shade);
        Gui.func_73734_a(0, height - 50, width, height, shade);
        Gui.func_73734_a(0, 0, 2, height, 0xB02B8A5D);
        Gui.func_73734_a(width - 2, 0, width, height, 0xB02B8A5D);
        Gui.func_73734_a(0, 0, width, 2, 0xB02B8A5D);
        Gui.func_73734_a(0, height - 2, width, height, 0xB02B8A5D);

        drawReticle(centerX, centerY, green);
        boolean geran = frame.vehicleType == RemoteControlNetwork.VEHICLE_GERAN;
        font.func_78276_b("WARTECH // " + getVehicleName(frame.vehicleType)
                .toUpperCase(Locale.ROOT) + " REMOTE OPTICAL LINK",
                8, 6, green);
        drawRight(font, cameraMode == 0 ? "CAM 1: NOSE" : "CAM 2: CHASE",
                width - 8, 6, pale);
        String range = String.format(Locale.ROOT, "RANGE %04d / %04d",
                Integer.valueOf(frame.distance), Integer.valueOf(frame.maxRange));
        font.func_78276_b(range,
                centerX - font.func_78256_a(range) / 2, 6,
                frame.distance >= frame.maxRange * 9 / 10 ? amber : pale);
        if (geran) {
            font.func_78276_b("IMPACT FUSE ARMED", 8, height - 44, amber);
        } else {
            drawLoadout(font, width, height - 44, frame, pale, amber);
        }

        font.func_78276_b(String.format(Locale.ROOT,
                "THR %03d%%   SPD %4.2f   ALT %04d",
                Integer.valueOf(Math.round(frame.throttle * 100.0F)),
                Double.valueOf(speed), Integer.valueOf((int) frame.y)),
                8, height - 30, pale);
        font.func_78276_b((geran ? "" : "PWR " + frame.power + "/"
                + frame.maxPower + "   ") + "AIRFRAME "
                + frame.healthPercent + "%", 8, height - 18, green);

        String heading = String.format(Locale.ROOT, "HDG %03d",
                Integer.valueOf((int) ((flightYaw + 360.0F) % 360.0F)));
        font.func_78276_b(heading,
                centerX - font.func_78256_a(heading) / 2,
                height - 30, pale);
        String status = (telemetryAge <= 6 ? "LINK STABLE" : "LINK BUFFERING")
                + (courseHold ? "  |  COURSE HOLD" : "")
                + (geran ? "  |  WARHEAD LIVE" : "  |  LTC " + frame.flares);
        font.func_78276_b(status,
                centerX - font.func_78256_a(status) / 2, height - 18, green);

        if (geran) {
            drawRight(font, "WARHEAD: CONTACT", width - 8, height - 30, amber);
            drawRight(font, "A/D TURN  SPACE/SHIFT PITCH  C CAM  R HOLD  X EXIT",
                    width - 8, height - 18, pale);
        } else {
            int selectedCount = payloadCount(frame.payloadCounts,
                    frame.selectedHardpoint);
            drawRight(font, "HP " + (frame.selectedHardpoint + 1) + ": "
                    + frame.weapon + " x" + selectedCount,
                    width - 8, height - 30, amber);
            drawRight(font, "LMB FIRE  Z SELECT  F LTC  C CAM  R HOLD  X EXIT",
                    width - 8, height - 18, pale);
        }
    }

    private static void drawLoadout(FontRenderer font, int width, int y,
            RemoteFrame frame, int normalColor, int selectedColor) {
        int x = 8;
        for (int slot = 0; slot < 6; ++slot) {
            int rawPayload = frame.payloadMask >>> (slot * 4) & 15;
            int count = payloadCount(frame.payloadCounts, slot);
            String text = (slot == frame.selectedHardpoint ? "[" : "")
                    + (slot + 1) + " "
                    + getPayloadName(frame.vehicleType, rawPayload)
                    + " x" + count
                    + (slot == frame.selectedHardpoint ? "]" : "")
                    + (slot < 5 ? "  " : "");
            int textWidth = font.func_78256_a(text);
            if (x + textWidth > width - 8) break;
            font.func_78276_b(text, x, y,
                    slot == frame.selectedHardpoint
                            ? selectedColor : normalColor);
            x += textWidth;
        }
    }

    private static int payloadCount(int packed, int slot) {
        return packed >>> (slot * 5) & 31;
    }

    private static void drawReticle(int x, int y, int color) {
        Gui.func_73734_a(x - 34, y, x - 10, y + 1, color);
        Gui.func_73734_a(x + 11, y, x + 35, y + 1, color);
        Gui.func_73734_a(x, y - 25, x + 1, y - 9, color);
        Gui.func_73734_a(x, y + 10, x + 1, y + 26, color);
        Gui.func_73734_a(x - 5, y - 5, x + 6, y - 4, color);
        Gui.func_73734_a(x - 5, y + 5, x + 6, y + 6, color);
        Gui.func_73734_a(x - 5, y - 5, x - 4, y + 6, color);
        Gui.func_73734_a(x + 5, y - 5, x + 6, y + 6, color);
        Gui.func_73734_a(x - 1, y - 1, x + 2, y + 2, 0xFFFFFFFF);
    }

    private static void drawRight(FontRenderer font, String text, int right,
            int y, int color) {
        font.func_78276_b(text, right - font.func_78256_a(text), y, color);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }

    private static float normalizeAngle(float angle) {
        while (angle <= -180.0F) angle += 360.0F;
        while (angle > 180.0F) angle -= 360.0F;
        return angle;
    }

    private static float approachAngle(float current, float target, float maximum) {
        float delta = normalizeAngle(target - current);
        if (delta > maximum) delta = maximum;
        if (delta < -maximum) delta = -maximum;
        return normalizeAngle(current + delta);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }

    private RemoteFrame getHudFrame(Entity remoteEntity) {
        if (latestTelemetry != null) return latestTelemetry;
        return snapshotFromEntity(remoteEntity);
    }

    private RemoteFrame snapshotFromEntity(Entity remoteEntity) {
        if (remoteEntity instanceof EntityMq9Drone) {
            EntityMq9Drone drone = (EntityMq9Drone) remoteEntity;
            return new RemoteFrame(drone.func_145782_y(),
                drone.getRemoteVehicleType(), drone.field_70165_t,
                drone.field_70163_u, drone.field_70161_v, drone.field_70159_w,
                drone.field_70181_x, drone.field_70179_y, drone.field_70177_z,
                drone.field_70125_A, throttle, drone.getPower(),
                drone.getEnergyCapacity(), drone.getHealthPercent(),
                drone.getFlareCount(), drone.isRemoteAirborne(),
                drone.getSelectedHardpoint(), drone.getPayloadMask(),
                drone.getPackedPayloadCounts(), drone.getDistanceFromLaunch(),
                drone.getMissionRange(), drone.getSelectedHardpointName());
        }
        if (remoteEntity instanceof EntityGeran) {
            EntityGeran drone = (EntityGeran) remoteEntity;
            return new RemoteFrame(drone.func_145782_y(),
                    RemoteControlNetwork.VEHICLE_GERAN,
                    drone.field_70165_t, drone.field_70163_u,
                    drone.field_70161_v, drone.field_70159_w,
                    drone.field_70181_x, drone.field_70179_y,
                    drone.field_70177_z, drone.field_70125_A, throttle,
                    0, 0, Math.max(0, Math.min(100, drone.health * 100 / 6)),
                    0, true, 0, 0, 0, drone.getDistanceFromLaunch(),
                    drone.getRemoteControlRange(), "WARHEAD");
        }
        if (remoteEntity instanceof EntityTu95Bomber) {
            EntityTu95Bomber bomber = (EntityTu95Bomber) remoteEntity;
            return new RemoteFrame(bomber.func_145782_y(),
                    RemoteControlNetwork.VEHICLE_TU95,
                    bomber.field_70165_t, bomber.field_70163_u,
                    bomber.field_70161_v, bomber.field_70159_w,
                    bomber.field_70181_x, bomber.field_70179_y,
                    bomber.field_70177_z, bomber.field_70125_A,
                    bomber.getRemoteThrottle(), bomber.getPower(),
                    bomber.getEnergyCapacity(), bomber.getHealthPercent(),
                    bomber.getFlareCount(), bomber.isRemoteAirborne(),
                    bomber.getSelectedHardpoint(),
                    bomber.getRemotePayloadMask(),
                    bomber.getPackedPayloadCounts(),
                    bomber.getDistanceFromLaunch(),
                    bomber.getRemoteControlRange(),
                    bomber.getSelectedHardpointName());
        }
        return null;
    }

    private static boolean isSupportedRemoteEntity(Entity entity) {
        return entity != null && !entity.field_70128_L
                && (entity instanceof EntityMq9Drone
                || entity instanceof EntityGeran
                || entity instanceof EntityTu95Bomber);
    }

    private static String getVehicleName(int type) {
        switch (type) {
            case RemoteControlNetwork.VEHICLE_GERAN: return "Geran-2";
            case RemoteControlNetwork.VEHICLE_F16: return "F-16C";
            case RemoteControlNetwork.VEHICLE_SU27: return "Su-27";
            case RemoteControlNetwork.VEHICLE_TU95: return "Tu-95";
            default: return "MQ-9";
        }
    }

    private static String getPayloadName(int type, int rawPayload) {
        if (type == RemoteControlNetwork.VEHICLE_TU95) {
            return EntityTu95Bomber.getStrategicWeaponName(rawPayload);
        }
        int payload = rawPayload == 0 ? -1 : rawPayload - 1;
        return EntityMq9Drone.getPayloadName(payload);
    }

    private static float getInitialThrottle(int type) {
        if (type == RemoteControlNetwork.VEHICLE_GERAN) return 0.72F;
        if (type == RemoteControlNetwork.VEHICLE_F16
                || type == RemoteControlNetwork.VEHICLE_SU27) return 0.18F;
        if (type == RemoteControlNetwork.VEHICLE_TU95) return 0.16F;
        return 0.28F;
    }

    private float getKeyboardTurnStep() {
        switch (vehicleType) {
            case RemoteControlNetwork.VEHICLE_F16: return 4.20F;
            case RemoteControlNetwork.VEHICLE_SU27: return 3.65F;
            case RemoteControlNetwork.VEHICLE_TU95: return 0.82F;
            default: return 2.15F;
        }
    }

    private float getFlightPitchMinimum() {
        switch (vehicleType) {
            case RemoteControlNetwork.VEHICLE_F16: return -42.0F;
            case RemoteControlNetwork.VEHICLE_SU27: return -38.0F;
            case RemoteControlNetwork.VEHICLE_TU95: return -14.0F;
            default: return -24.0F;
        }
    }

    private float getFlightPitchMaximum() {
        switch (vehicleType) {
            case RemoteControlNetwork.VEHICLE_F16: return 34.0F;
            case RemoteControlNetwork.VEHICLE_SU27: return 31.0F;
            case RemoteControlNetwork.VEHICLE_TU95: return 11.0F;
            default: return 20.0F;
        }
    }

    private float getViewPitchMinimum() {
        return vehicleType == RemoteControlNetwork.VEHICLE_TU95
                ? -60.0F : -70.0F;
    }

    private float getViewPitchMaximum() {
        return vehicleType == RemoteControlNetwork.VEHICLE_TU95
                ? 40.0F : 55.0F;
    }

    private float getFlightPitchStep() {
        if (vehicleType == RemoteControlNetwork.VEHICLE_TU95) return 0.28F;
        if (vehicleType == RemoteControlNetwork.VEHICLE_F16) return 0.95F;
        if (vehicleType == RemoteControlNetwork.VEHICLE_SU27) return 0.85F;
        return 0.65F;
    }

    private float getViewPitchStep() {
        if (vehicleType == RemoteControlNetwork.VEHICLE_TU95) return 0.38F;
        if (vehicleType == RemoteControlNetwork.VEHICLE_F16) return 1.15F;
        if (vehicleType == RemoteControlNetwork.VEHICLE_SU27) return 1.05F;
        return 0.90F;
    }

    private double getNoseCameraDistance() {
        switch (vehicleType) {
            case RemoteControlNetwork.VEHICLE_GERAN: return 1.25D;
            case RemoteControlNetwork.VEHICLE_F16: return 2.8D;
            case RemoteControlNetwork.VEHICLE_SU27: return 3.4D;
            case RemoteControlNetwork.VEHICLE_TU95: return 7.2D;
            default: return 2.15D;
        }
    }

    private double getNoseCameraHeight() {
        switch (vehicleType) {
            case RemoteControlNetwork.VEHICLE_GERAN: return 0.35D;
            case RemoteControlNetwork.VEHICLE_F16: return 1.0D;
            case RemoteControlNetwork.VEHICLE_SU27: return 1.25D;
            case RemoteControlNetwork.VEHICLE_TU95: return 2.4D;
            default: return 1.18D;
        }
    }

    private double getChaseCameraDistance() {
        switch (vehicleType) {
            case RemoteControlNetwork.VEHICLE_GERAN: return -7.5D;
            case RemoteControlNetwork.VEHICLE_F16: return -13.0D;
            case RemoteControlNetwork.VEHICLE_SU27: return -15.0D;
            case RemoteControlNetwork.VEHICLE_TU95: return -29.0D;
            default: return -16.5D;
        }
    }

    private double getChaseCameraHeight() {
        switch (vehicleType) {
            case RemoteControlNetwork.VEHICLE_GERAN: return 3.7D;
            case RemoteControlNetwork.VEHICLE_F16: return 5.0D;
            case RemoteControlNetwork.VEHICLE_SU27: return 5.8D;
            case RemoteControlNetwork.VEHICLE_TU95: return 11.5D;
            default: return 8.0D;
        }
    }

    private double getChaseFocusDistance() {
        switch (vehicleType) {
            case RemoteControlNetwork.VEHICLE_F16: return 3.0D;
            case RemoteControlNetwork.VEHICLE_SU27: return 3.7D;
            case RemoteControlNetwork.VEHICLE_TU95: return 8.0D;
            default: return 2.4D;
        }
    }

    private double getChaseFocusHeight() {
        switch (vehicleType) {
            case RemoteControlNetwork.VEHICLE_F16: return 0.9D;
            case RemoteControlNetwork.VEHICLE_SU27: return 1.1D;
            case RemoteControlNetwork.VEHICLE_TU95: return 2.2D;
            default: return 0.65D;
        }
    }

    private static final class RemoteEffect {
        final int entityId;
        final int effect;
        final double x;
        final double y;
        final double z;
        final double targetX;
        final double targetY;
        final double targetZ;
        final int payload;

        RemoteEffect(int entityId, int effect, double x, double y, double z,
                double targetX, double targetY, double targetZ, int payload) {
            this.entityId = entityId;
            this.effect = effect;
            this.x = x;
            this.y = y;
            this.z = z;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.payload = payload;
        }
    }

    /**
     * Visual mirror used only when vanilla entity tracking cannot reach the
     * operator body. Damage and impact remain server-authoritative.
     */
    private static final class RemoteVisualMunition extends EntityMq9Munition {
        private final double visualTargetX;
        private final double visualTargetY;
        private final double visualTargetZ;

        RemoteVisualMunition(net.minecraft.world.World world,
                RemoteEffect effect) {
            super(world, effect.payload, (int) Math.floor(effect.targetX),
                    (int) Math.floor(effect.targetY),
                    (int) Math.floor(effect.targetZ));
            visualTargetX = effect.targetX;
            visualTargetY = effect.targetY;
            visualTargetZ = effect.targetZ;
            func_70107_b(effect.x, effect.y, effect.z);
            field_70169_q = field_70142_S = effect.x;
            field_70167_r = field_70137_T = effect.y;
            field_70166_s = field_70136_U = effect.z;
            field_70145_X = true;
        }

        @Override
        public void func_70071_h_() {
            tickEntityBase();
            if (field_70128_L || field_70170_p == null) return;
            double dx = visualTargetX - field_70165_t;
            double dy = visualTargetY - field_70163_u;
            double dz = visualTargetZ - field_70161_v;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double speed = AviationOrdnance.isPowered(getType())
                    ? AviationOrdnance.getFlightSpeed(getType())
                    : Math.max(0.82D, AviationOrdnance.getFlightSpeed(getType()));
            if (distance <= speed * 1.4D || field_70173_aa > 900) {
                field_70170_p.func_72869_a("hugeexplosion", field_70165_t,
                        field_70163_u, field_70161_v, 0.0D, 0.0D, 0.0D);
                func_70106_y();
                return;
            }
            field_70169_q = field_70165_t;
            field_70167_r = field_70163_u;
            field_70166_s = field_70161_v;
            field_70159_w = dx / distance * speed;
            field_70181_x = dy / distance * speed;
            field_70179_y = dz / distance * speed;
            func_70107_b(field_70165_t + field_70159_w,
                    field_70163_u + field_70181_x,
                    field_70161_v + field_70179_y);
            double horizontal = Math.sqrt(field_70159_w * field_70159_w
                    + field_70179_y * field_70179_y);
            field_70177_z = (float) Math.toDegrees(Math.atan2(
                    -field_70159_w, field_70179_y));
            field_70125_A = (float) -Math.toDegrees(Math.atan2(
                    field_70181_x, horizontal));
            spawnTrail();
        }
    }

    private static final class RemoteFrame {
        final int entityId;
        final int vehicleType;
        final double x;
        final double y;
        final double z;
        final double motionX;
        final double motionY;
        final double motionZ;
        final float yaw;
        final float pitch;
        final float throttle;
        final int power;
        final int maxPower;
        final int healthPercent;
        final int flares;
        final boolean airborne;
        final int selectedHardpoint;
        final int payloadMask;
        final int payloadCounts;
        final int distance;
        final int maxRange;
        final String weapon;

        RemoteFrame(int entityId, int vehicleType, double x, double y,
                double z, double motionX, double motionY, double motionZ,
                float yaw, float pitch,
                float throttle, int power, int maxPower, int healthPercent,
                int flares, boolean airborne, int selectedHardpoint,
                int payloadMask, int payloadCounts, int distance, int maxRange,
                String weapon) {
            this.entityId = entityId;
            this.vehicleType = vehicleType;
            this.x = x;
            this.y = y;
            this.z = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.yaw = yaw;
            this.pitch = pitch;
            this.throttle = throttle;
            this.power = power;
            this.maxPower = maxPower;
            this.healthPercent = healthPercent;
            this.flares = flares;
            this.airborne = airborne;
            this.selectedHardpoint = selectedHardpoint;
            this.payloadMask = payloadMask;
            this.payloadCounts = payloadCounts;
            this.distance = distance;
            this.maxRange = maxRange;
            this.weapon = weapon == null || weapon.length() == 0 ? "NONE" : weapon;
        }
    }

    private static final class RemoteCameraEntity extends EntityOtherPlayerMP {
        private RemoteCameraEntity(net.minecraft.world.World world) {
            super(world, CAMERA_PROFILE);
            func_70105_a(0.0F, 0.0F);
            field_70145_X = true;
            field_70129_M = 0.0F;
        }

        @Override
        public void func_70071_h_() {
            // Camera placement is driven once per render frame by telemetry.
        }

        public float func_70047_e() { return 0.0F; }
    }
}
