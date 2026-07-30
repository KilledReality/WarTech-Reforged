package com.wartec.wartecmod.port.client;

import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import com.wartec.wartecmod.port.integration.AviationOrdnance;
import com.wartec.wartecmod.port.network.RemoteControlNetwork;
import com.wartec.wartecmod.port.network.RemoteControlTelemetryMessage;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * 1.12.2 adaptation of dev66's remote optical-link client. Control rates,
 * camera positions, keys and HUD layout intentionally match the 1.7.10 code.
 */
public final class RemoteControlClient {
    private static final RemoteControlClient INSTANCE =
            new RemoteControlClient();
    private static final String CATEGORY = "WarTech Remote Flight";
    private static final GameProfile CAMERA_PROFILE = new GameProfile(
            UUID.fromString("97e9b947-e66e-4b24-9490-42ef7f7027b5"),
            "WarTech_Remote_Camera");
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

    private static volatile PendingState pendingState;
    private static volatile RemoteFrame pendingTelemetry;

    private int entityId = -1;
    private int vehicleType;
    private RemoteCameraEntity camera;
    private float throttle;
    private float controlYaw;
    private float controlPitch;
    private float flightYaw;
    private float flightPitch;
    private float playerAnchorYaw;
    private float playerAnchorPitch;
    private int cameraMode;
    private int pendingFlags;
    private int telemetryAge;
    private int missingEntityTicks;
    private boolean courseHold;
    private boolean exitAwaitingServer;
    private boolean cameraToggleRequested;
    private boolean courseHoldToggleRequested;
    private boolean cameraPositionReady;
    private boolean remoteFrameReady;
    private boolean visualReady;
    private RemoteFrame latestTelemetry;
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
        pendingState = new PendingState(entityId, active, vehicleType,
                message == null ? "" : message);
    }

    public static void acceptTelemetry(
            RemoteControlTelemetryMessage message) {
        if (message != null) {
            pendingTelemetry = new RemoteFrame(message);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (entityId < 0 || !Keyboard.getEventKeyState()) {
            return;
        }
        switch (Keyboard.getEventKey()) {
            case Keyboard.KEY_C:
                cameraToggleRequested = true;
                break;
            case Keyboard.KEY_F:
                pendingFlags |= 0x08;
                break;
            case Keyboard.KEY_Z:
                pendingFlags |= 0x10;
                break;
            case Keyboard.KEY_X:
                pendingFlags |= 0x02;
                break;
            case Keyboard.KEY_R:
                courseHoldToggleRequested = true;
                break;
            default:
                break;
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (entityId >= 0 && Mouse.getEventButton() == 0
                && Mouse.getEventButtonState()) {
            pendingFlags |= 0x04;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        applyPendingState(minecraft);
        applyPendingTelemetry();
        if (entityId < 0) {
            return;
        }
        if (minecraft.world == null || minecraft.player == null) {
            stopLocal(minecraft);
            return;
        }

        Entity aircraft = getAircraft(minecraft);
        if (aircraft == null && latestTelemetry == null) {
            if (++missingEntityTicks > 40) {
                stopLocal(minecraft);
            }
            return;
        }
        missingEntityTicks = 0;
        if (latestTelemetry != null && telemetryAge < Integer.MAX_VALUE) {
            ++telemetryAge;
        }
        if (!remoteFrameReady) {
            seedRemoteFrame(aircraft);
        }
        if (camera == null && remoteFrameReady) {
            createCamera(minecraft);
        }
        if (camera == null || exitAwaitingServer) {
            return;
        }
        if (minecraft.currentScreen != null) {
            pendingFlags = 0;
            cameraToggleRequested = false;
            courseHoldToggleRequested = false;
            return;
        }

        int flags = pendingFlags;
        pendingFlags = 0;
        if (minecraft.gameSettings.keyBindAttack.isPressed()) {
            flags |= 0x04;
        }
        if (FLARES.isPressed()) {
            flags |= 0x08;
        }
        if (CYCLE.isPressed()) {
            flags |= 0x10;
        }
        if (EXIT.isPressed()) {
            flags |= 0x02;
        }
        if (cameraToggleRequested || CAMERA.isPressed()) {
            cameraMode = (cameraMode + 1) % 2;
            cameraPositionReady = false;
            cameraToggleRequested = false;
            tell(minecraft, vehicleName(vehicleType)
                    + (cameraMode == 0
                            ? " camera: NOSE" : " camera: CHASE"));
        }
        if (courseHoldToggleRequested || COURSE_HOLD.isPressed()) {
            courseHold = !courseHold;
            courseHoldToggleRequested = false;
            if (courseHold) {
                flightYaw = frameYaw;
                flightPitch = clamp(framePitch,
                        flightPitchMinimum(), flightPitchMaximum());
            } else {
                flightYaw = controlYaw;
                flightPitch = controlPitch;
            }
            tell(minecraft, vehicleName(vehicleType)
                    + (courseHold
                            ? " course hold: FREE LOOK"
                            : " course hold: OFF"));
        }

        if (minecraft.gameSettings.keyBindForward.isKeyDown()
                || Keyboard.isKeyDown(Keyboard.KEY_W)) {
            throttle = Math.min(1.0F, throttle + 0.018F);
        }
        if (minecraft.gameSettings.keyBindBack.isKeyDown()
                || Keyboard.isKeyDown(Keyboard.KEY_S)) {
            throttle = Math.max(0.0F, throttle - 0.018F);
        }
        boolean left = minecraft.gameSettings.keyBindLeft.isKeyDown()
                || Keyboard.isKeyDown(Keyboard.KEY_A);
        boolean right = minecraft.gameSettings.keyBindRight.isKeyDown()
                || Keyboard.isKeyDown(Keyboard.KEY_D);
        if (left != right) {
            float turn = left ? -keyboardTurnStep() : keyboardTurnStep();
            flags |= left ? 0x20 : 0x40;
            flightYaw += turn;
            if (!courseHold) {
                controlYaw += turn;
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            if (courseHold) {
                flightPitch -= flightPitchStep();
            } else {
                controlPitch -= viewPitchStep();
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            if (courseHold) {
                flightPitch += flightPitchStep();
            } else {
                controlPitch += viewPitchStep();
            }
        }

        controlYaw = normalizeAngle(controlYaw);
        controlPitch = clamp(controlPitch,
                viewPitchMinimum(), viewPitchMaximum());
        if (!courseHold) {
            flightYaw = controlYaw;
            flightPitch = controlPitch;
        }
        flightYaw = normalizeAngle(flightYaw);
        flightPitch = clamp(flightPitch,
                flightPitchMinimum(), flightPitchMaximum());
        RemoteControlNetwork.sendInput(entityId, flightYaw, flightPitch,
                controlYaw, controlPitch, throttle, flags);
        if ((flags & 0x02) != 0) {
            exitAwaitingServer = true;
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.END) {
            if (entityId >= 0 && minecraft.player != null
                    && camera != null) {
                applyMouseLook(minecraft);
                minecraft.setRenderViewEntity(camera);
            }
            return;
        }
        if (entityId < 0 || minecraft.world == null
                || minecraft.player == null || camera == null) {
            return;
        }
        applyPendingTelemetry();
        Entity controlled = getAircraft(minecraft);
        updateRemoteFrame(controlled,
                Math.max(0.0D, Math.min(1.0D, event.renderTickTime)));
        updateCamera();
        minecraft.setRenderViewEntity(camera);
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (entityId < 0) {
            return;
        }
        switch (event.getType()) {
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
        if (entityId >= 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (entityId >= 0 && event.getEntityPlayer() == minecraft.player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post event) {
        if (entityId < 0
                || event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null || minecraft.fontRenderer == null) {
            return;
        }
        RemoteFrame frame = latestTelemetry != null
                ? latestTelemetry : snapshotFromEntity(getAircraft(minecraft));
        if (frame == null) {
            return;
        }
        double speed = Math.sqrt(frame.motionX * frame.motionX
                + frame.motionY * frame.motionY
                + frame.motionZ * frame.motionZ);
        drawOperatorHud(minecraft.fontRenderer,
                event.getResolution().getScaledWidth(),
                event.getResolution().getScaledHeight(), frame, speed);
    }

    private void applyPendingState(Minecraft minecraft) {
        PendingState state = pendingState;
        if (state == null) {
            return;
        }
        pendingState = null;
        if (!state.message.isEmpty()) {
            tell(minecraft, state.message);
        }
        if (!state.active) {
            stopLocal(minecraft);
            return;
        }
        entityId = state.entityId;
        vehicleType = state.vehicleType;
        throttle = initialThrottle(vehicleType);
        cameraMode = 0;
        pendingFlags = 0;
        telemetryAge = 0;
        missingEntityTicks = 0;
        courseHold = false;
        exitAwaitingServer = false;
        cameraToggleRequested = false;
        courseHoldToggleRequested = false;
        cameraPositionReady = false;
        remoteFrameReady = false;
        visualReady = false;
        latestTelemetry = null;
        lastRenderNanos = 0L;
        if (minecraft.player != null) {
            playerAnchorYaw = minecraft.player.rotationYaw;
            playerAnchorPitch = minecraft.player.rotationPitch;
        }
        minecraft.displayGuiScreen(null);
    }

    private void applyPendingTelemetry() {
        RemoteFrame frame = pendingTelemetry;
        if (frame == null) {
            return;
        }
        pendingTelemetry = null;
        if (frame.entityId == entityId) {
            latestTelemetry = frame;
            vehicleType = frame.vehicleType;
            telemetryAge = 0;
        }
    }

    private void createCamera(Minecraft minecraft) {
        camera = new RemoteCameraEntity(minecraft.world);
        controlYaw = frameYaw;
        controlPitch = clamp(framePitch,
                viewPitchMinimum(), viewPitchMaximum());
        flightYaw = controlYaw;
        flightPitch = clamp(controlPitch,
                flightPitchMinimum(), flightPitchMaximum());
        visualControlYaw = controlYaw;
        visualControlPitch = controlPitch;
        playerAnchorYaw = minecraft.player.rotationYaw;
        playerAnchorPitch = minecraft.player.rotationPitch;
        updateCamera();
        minecraft.setRenderViewEntity(camera);
    }

    private void applyMouseLook(Minecraft minecraft) {
        float cameraYawDelta =
                normalizeAngle(camera.rotationYaw - cameraViewYaw);
        float cameraPitchDelta = camera.rotationPitch - cameraViewPitch;
        float playerYawDelta = normalizeAngle(
                minecraft.player.rotationYaw - playerAnchorYaw);
        float playerPitchDelta =
                minecraft.player.rotationPitch - playerAnchorPitch;
        float cameraDelta = Math.abs(cameraYawDelta)
                + Math.abs(cameraPitchDelta);
        float playerDelta = Math.abs(playerYawDelta)
                + Math.abs(playerPitchDelta);
        float yawDelta;
        float pitchDelta;
        if (cameraDelta >= playerDelta && cameraDelta > 0.0001F) {
            yawDelta = cameraYawDelta;
            pitchDelta = cameraPitchDelta;
        } else if (playerDelta > 0.0001F) {
            yawDelta = playerYawDelta;
            pitchDelta = playerPitchDelta;
        } else {
            yawDelta = 0.0F;
            pitchDelta = 0.0F;
        }
        if (Math.abs(yawDelta) + Math.abs(pitchDelta) > 0.0001F) {
            controlYaw = normalizeAngle(controlYaw + yawDelta);
            controlPitch = clamp(controlPitch + pitchDelta,
                    viewPitchMinimum(), viewPitchMaximum());
            visualControlYaw = normalizeAngle(
                    visualControlYaw + yawDelta);
            visualControlPitch = clamp(visualControlPitch + pitchDelta,
                    viewPitchMinimum(), viewPitchMaximum());
            if (!courseHold) {
                flightYaw = controlYaw;
                flightPitch = clamp(controlPitch,
                        flightPitchMinimum(), flightPitchMaximum());
            }
        }
        minecraft.player.rotationYaw = playerAnchorYaw;
        minecraft.player.rotationPitch = playerAnchorPitch;
        camera.prevRotationYaw = camera.rotationYaw = cameraViewYaw;
        camera.prevRotationPitch = camera.rotationPitch = cameraViewPitch;
    }

    private void updateRemoteFrame(Entity entity, double partialTick) {
        RemoteFrame frame = latestTelemetry != null
                ? latestTelemetry : snapshotFromEntity(entity);
        if (frame == null) {
            return;
        }
        if (!remoteFrameReady) {
            seedRemoteFrame(frame);
            return;
        }
        long now = System.nanoTime();
        double delta = lastRenderNanos == 0L ? 0.0D
                : clamp((now - lastRenderNanos) / 50000000.0D,
                        0.0D, 1.25D);
        renderDeltaTicks = delta;
        lastRenderNanos = now;
        frameX += frameMotionX * delta;
        frameY += frameMotionY * delta;
        frameZ += frameMotionZ * delta;
        double prediction = Math.min(4.0D,
                Math.max(0.0D, telemetryAge - 1.0D + partialTick));
        double targetX = frame.x + frame.motionX * prediction;
        double targetY = frame.y + frame.motionY * prediction;
        double targetZ = frame.z + frame.motionZ * prediction;
        double dx = targetX - frameX;
        double dy = targetY - frameY;
        double dz = targetZ - frameZ;
        if (dx * dx + dy * dy + dz * dz > 576.0D) {
            frameX = targetX;
            frameY = targetY;
            frameZ = targetZ;
        } else {
            double positionBlend = 1.0D - Math.exp(-delta * 0.34D);
            frameX += dx * positionBlend;
            frameY += dy * positionBlend;
            frameZ += dz * positionBlend;
        }
        double motionBlend = 1.0D - Math.exp(-delta * 0.55D);
        frameMotionX += (frame.motionX - frameMotionX) * motionBlend;
        frameMotionY += (frame.motionY - frameMotionY) * motionBlend;
        frameMotionZ += (frame.motionZ - frameMotionZ) * motionBlend;
        float yawBlend = (float) (1.0D - Math.exp(-delta * 0.3D));
        frameYaw = normalizeAngle(frameYaw
                + normalizeAngle(frame.yaw - frameYaw) * yawBlend);
        framePitch += (frame.pitch - framePitch)
                * (float) Math.min(1.0D, delta * 0.4D);
    }

    private void seedRemoteFrame(Entity entity) {
        RemoteFrame frame = latestTelemetry != null
                ? latestTelemetry : snapshotFromEntity(entity);
        if (frame != null) {
            seedRemoteFrame(frame);
        }
    }

    private void seedRemoteFrame(RemoteFrame frame) {
        frameX = frame.x;
        frameY = frame.y;
        frameZ = frame.z;
        frameMotionX = frame.motionX;
        frameMotionY = frame.motionY;
        frameMotionZ = frame.motionZ;
        frameYaw = frame.yaw;
        framePitch = frame.pitch;
        remoteFrameReady = true;
        lastRenderNanos = System.nanoTime();
    }

    private void updateCamera() {
        double blend = 1.0D - Math.exp(
                -Math.max(0.02D, renderDeltaTicks) * 0.72D);
        visualControlYaw = normalizeAngle(visualControlYaw
                + normalizeAngle(controlYaw - visualControlYaw)
                        * (float) blend);
        visualControlPitch += (controlPitch - visualControlPitch)
                * (float) blend;
        double yaw = Math.toRadians(frameYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double distance = cameraMode == 0
                ? noseCameraDistance() : chaseCameraDistance();
        double height = cameraMode == 0
                ? noseCameraHeight() : chaseCameraHeight();
        double x = frameX + forwardX * distance;
        double y = frameY + height;
        double z = frameZ + forwardZ * distance;
        if (cameraMode == 1 && cameraPositionReady && visualReady) {
            double positionBlend = 1.0D - Math.exp(
                    -Math.max(0.02D, renderDeltaTicks) * 0.82D);
            visualX += (x - visualX) * positionBlend;
            visualY += (y - visualY) * positionBlend;
            visualZ += (z - visualZ) * positionBlend;
            x = visualX;
            y = visualY;
            z = visualZ;
        } else {
            visualX = x;
            visualY = y;
            visualZ = z;
        }
        visualReady = true;
        cameraPositionReady = true;
        cameraViewYaw = visualControlYaw;
        cameraViewPitch = visualControlPitch;
        if (cameraMode == 1) {
            double focusX = frameX + forwardX * chaseFocusDistance();
            double focusY = frameY + chaseFocusHeight();
            double focusZ = frameZ + forwardZ * chaseFocusDistance();
            double dx = focusX - x;
            double dy = focusY - y;
            double dz = focusZ - z;
            cameraViewYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            cameraViewPitch = (float) -Math.toDegrees(
                    Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        }
        placeCamera(x, y, z, cameraViewYaw, cameraViewPitch);
    }

    private void placeCamera(double x, double y, double z,
            float yaw, float pitch) {
        camera.prevPosX = x;
        camera.prevPosY = y;
        camera.prevPosZ = z;
        camera.lastTickPosX = x;
        camera.lastTickPosY = y;
        camera.lastTickPosZ = z;
        camera.setPosition(x, y, z);
        camera.prevRotationYaw = camera.rotationYaw = yaw;
        camera.prevRotationPitch = camera.rotationPitch = pitch;
        camera.rotationYawHead = yaw;
    }

    private void stopLocal(Minecraft minecraft) {
        entityId = -1;
        if (camera != null) {
            camera.setDead();
        }
        camera = null;
        cameraPositionReady = false;
        pendingFlags = 0;
        telemetryAge = 0;
        missingEntityTicks = 0;
        courseHold = false;
        exitAwaitingServer = false;
        cameraToggleRequested = false;
        courseHoldToggleRequested = false;
        vehicleType = 0;
        latestTelemetry = null;
        remoteFrameReady = false;
        visualReady = false;
        lastRenderNanos = 0L;
        if (minecraft != null && minecraft.player != null) {
            minecraft.setRenderViewEntity(minecraft.player);
        }
    }

    static boolean shouldHideControlledEntity(Entity entity) {
        RemoteControlClient client = INSTANCE;
        return entity != null && client.entityId == entity.getEntityId()
                && client.cameraMode == 0 && client.camera != null;
    }

    private Entity getAircraft(Minecraft minecraft) {
        if (minecraft.world == null || entityId < 0) {
            return null;
        }
        Entity entity = minecraft.world.getEntityByID(entityId);
        return (entity instanceof EntityWarTechAircraft
                || entity instanceof EntityWarTechMissile) && !entity.isDead
                ? entity : null;
    }

    private RemoteFrame snapshotFromEntity(Entity entity) {
        if (!(entity instanceof EntityWarTechAircraft)) {
            return null;
        }
        EntityWarTechAircraft aircraft = (EntityWarTechAircraft) entity;
        return new RemoteFrame(aircraft, throttle);
    }

    private void drawOperatorHud(FontRenderer font, int width, int height,
            RemoteFrame frame, double speed) {
        int centerX = width / 2;
        int centerY = height / 2;
        int green = 0xFF79E8B0;
        int amber = 0xFFFFCC58;
        Gui.drawRect(0, 0, width, 18, 0x980A131A);
        Gui.drawRect(0, height - 50, width, height, 0x980A131A);
        Gui.drawRect(0, 0, 2, height, 0xB02C5865);
        Gui.drawRect(width - 2, 0, width, height, 0xB02C5865);
        Gui.drawRect(0, 0, width, 2, 0xB02C5865);
        Gui.drawRect(0, height - 2, width, height, 0xB02C5865);
        drawReticle(centerX, centerY, green);
        font.drawString("WARTECH // "
                + vehicleName(frame.vehicleType).toUpperCase(Locale.ROOT)
                + " REMOTE OPTICAL LINK", 8, 6, green);
        drawRight(font, cameraMode == 0
                ? "CAM 1: NOSE" : "CAM 2: CHASE",
                width - 8, 6, amber);
        String range = String.format(Locale.ROOT, "RANGE %04d / %04d",
                frame.distance, frame.maxRange);
        font.drawString(range, centerX - font.getStringWidth(range) / 2, 6,
                frame.distance >= frame.maxRange * 9 / 10
                        ? 0xFFFFCC58 : amber);
        boolean geran = frame.vehicleType == 1;
        if (geran) {
            font.drawString("IMPACT FUSE ARMED",
                    8, height - 44, 0xFFFFCC58);
        } else {
            drawLoadout(font, width, height - 44,
                    frame, amber, 0xFFFFCC58);
        }
        font.drawString(String.format(Locale.ROOT,
                "THR %03d%%   SPD %4.2f   ALT %04d",
                Math.round(frame.throttle * 100.0F), speed, (int) frame.y),
                8, height - 30, amber);
        font.drawString((geran ? "" : "PWR " + frame.power + "/"
                + frame.maxPower + "   ")
                + "AIRFRAME " + frame.healthPercent + "%",
                8, height - 18, green);
        String heading = String.format(Locale.ROOT, "HDG %03d",
                (int) ((flightYaw + 360.0F) % 360.0F));
        font.drawString(heading,
                centerX - font.getStringWidth(heading) / 2,
                height - 30, amber);
        String link = (telemetryAge <= 6
                ? "LINK STABLE" : "LINK BUFFERING")
                + (courseHold ? "  |  COURSE HOLD" : "")
                + (geran ? "  |  WARHEAD LIVE"
                        : "  |  LTC " + frame.flares);
        font.drawString(link, centerX - font.getStringWidth(link) / 2,
                height - 18, green);
        if (geran) {
            drawRight(font, "WARHEAD: CONTACT",
                    width - 8, height - 30, 0xFFFFCC58);
            drawRight(font,
                    "A/D TURN  SPACE/SHIFT PITCH  C CAM  R HOLD  X EXIT",
                    width - 8, height - 18, amber);
        } else {
            int selectedCount = payloadCount(
                    frame.payloadCounts, frame.selectedHardpoint);
            drawRight(font, "HP " + (frame.selectedHardpoint + 1)
                    + ": " + frame.weapon + " x" + selectedCount,
                    width - 8, height - 30, 0xFFFFCC58);
            drawRight(font,
                    "LMB FIRE  Z SELECT  F LTC  C CAM  R HOLD  X EXIT",
                    width - 8, height - 18, amber);
        }
    }

    private static void drawLoadout(FontRenderer font, int width, int y,
            RemoteFrame frame, int normalColor, int selectedColor) {
        int x = 8;
        for (int slot = 0; slot < 6; ++slot) {
            int code = frame.payloadMask >>> (slot * 4) & 0x0F;
            int count = payloadCount(frame.payloadCounts, slot);
            String text = (slot == frame.selectedHardpoint ? "[" : "")
                    + (slot + 1) + " "
                    + payloadName(frame.vehicleType, code)
                    + " x" + count
                    + (slot == frame.selectedHardpoint ? "]" : "")
                    + (slot < 5 ? "  " : "");
            int textWidth = font.getStringWidth(text);
            if (x + textWidth > width - 8) {
                break;
            }
            font.drawString(text, x, y,
                    slot == frame.selectedHardpoint
                            ? selectedColor : normalColor);
            x += textWidth;
        }
    }

    private static int payloadCount(int packed, int slot) {
        return packed >>> (Math.max(0, slot) * 5) & 0x1F;
    }

    private static String payloadName(int vehicleType, int code) {
        if (vehicleType == 4) {
            if (code == 10) return "KH-555";
            if (code == 11) return "FAB-5000";
            if (code == 12) return "KAB-3000";
            return "EMPTY";
        }
        return code >= 1 && code <= 9
                ? AviationOrdnance.getName(code - 1) : "EMPTY";
    }

    private static void drawReticle(int x, int y, int color) {
        Gui.drawRect(x - 34, y, x - 10, y + 1, color);
        Gui.drawRect(x + 11, y, x + 35, y + 1, color);
        Gui.drawRect(x, y - 25, x + 1, y - 9, color);
        Gui.drawRect(x, y + 10, x + 1, y + 26, color);
        Gui.drawRect(x - 5, y - 5, x + 6, y - 4, color);
        Gui.drawRect(x - 5, y + 5, x + 6, y + 6, color);
        Gui.drawRect(x - 5, y - 5, x - 4, y + 6, color);
        Gui.drawRect(x + 5, y - 5, x + 6, y + 6, color);
        Gui.drawRect(x - 1, y - 1, x + 2, y + 2, 0xFFFFFFFF);
    }

    private static void drawRight(FontRenderer font, String text,
            int right, int y, int color) {
        font.drawString(text, right - font.getStringWidth(text), y, color);
    }

    private static void tell(Minecraft minecraft, String message) {
        if (minecraft != null && minecraft.player != null
                && message != null && !message.isEmpty()) {
            minecraft.player.sendMessage(new TextComponentString(message));
        }
    }

    private static String vehicleName(int type) {
        if (type == 1) return "Geran-2";
        if (type == 2) return "F-16C";
        if (type == 3) return "Su-27";
        if (type == 4) return "Tu-95";
        return "MQ-9";
    }

    private static float initialThrottle(int type) {
        if (type == 1) return 0.72F;
        if (type == 2 || type == 3) return 0.18F;
        if (type == 4) return 0.16F;
        return 0.28F;
    }

    private float keyboardTurnStep() {
        if (vehicleType == 2) return 4.2F;
        if (vehicleType == 3) return 3.65F;
        if (vehicleType == 4) return 0.82F;
        return 2.15F;
    }

    private float flightPitchMinimum() {
        if (vehicleType == 2) return -42.0F;
        if (vehicleType == 3) return -38.0F;
        if (vehicleType == 4) return -14.0F;
        return -24.0F;
    }

    private float flightPitchMaximum() {
        if (vehicleType == 2) return 34.0F;
        if (vehicleType == 3) return 31.0F;
        if (vehicleType == 4) return 11.0F;
        return 20.0F;
    }

    private float viewPitchMinimum() {
        return vehicleType == 4 ? -60.0F : -70.0F;
    }

    private float viewPitchMaximum() {
        return vehicleType == 4 ? 40.0F : 55.0F;
    }

    private float flightPitchStep() {
        if (vehicleType == 2) return 0.95F;
        if (vehicleType == 3) return 0.85F;
        if (vehicleType == 4) return 0.28F;
        return 0.65F;
    }

    private float viewPitchStep() {
        if (vehicleType == 2) return 1.15F;
        if (vehicleType == 3) return 1.05F;
        if (vehicleType == 4) return 0.38F;
        return 0.9F;
    }

    private double noseCameraDistance() {
        if (vehicleType == 1) return 1.25D;
        if (vehicleType == 2) return 2.8D;
        if (vehicleType == 3) return 3.4D;
        if (vehicleType == 4) return 7.2D;
        return 2.15D;
    }

    private double noseCameraHeight() {
        if (vehicleType == 1) return 0.35D;
        if (vehicleType == 2) return 1.0D;
        if (vehicleType == 3) return 1.25D;
        if (vehicleType == 4) return 2.4D;
        return 1.18D;
    }

    private double chaseCameraDistance() {
        if (vehicleType == 1) return -7.5D;
        if (vehicleType == 2) return -13.0D;
        if (vehicleType == 3) return -15.0D;
        if (vehicleType == 4) return -29.0D;
        return -16.5D;
    }

    private double chaseCameraHeight() {
        if (vehicleType == 1) return 3.7D;
        if (vehicleType == 2) return 5.0D;
        if (vehicleType == 3) return 5.8D;
        if (vehicleType == 4) return 11.5D;
        return 8.0D;
    }

    private double chaseFocusDistance() {
        if (vehicleType == 2) return 3.0D;
        if (vehicleType == 3) return 3.7D;
        if (vehicleType == 4) return 8.0D;
        return 2.4D;
    }

    private double chaseFocusHeight() {
        if (vehicleType == 2) return 0.9D;
        if (vehicleType == 3) return 1.1D;
        if (vehicleType == 4) return 2.2D;
        return 0.65D;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return value < minimum ? minimum
                : Math.min(value, maximum);
    }

    private static double clamp(double value,
            double minimum, double maximum) {
        return value < minimum ? minimum
                : Math.min(value, maximum);
    }

    private static float normalizeAngle(float angle) {
        while (angle <= -180.0F) angle += 360.0F;
        while (angle > 180.0F) angle -= 360.0F;
        return angle;
    }

    private static final class PendingState {
        private final int entityId;
        private final boolean active;
        private final int vehicleType;
        private final String message;

        private PendingState(int entityId, boolean active,
                int vehicleType, String message) {
            this.entityId = entityId;
            this.active = active;
            this.vehicleType = vehicleType;
            this.message = message;
        }
    }

    private static final class RemoteFrame {
        private final int entityId;
        private final int vehicleType;
        private final double x;
        private final double y;
        private final double z;
        private final double motionX;
        private final double motionY;
        private final double motionZ;
        private final float yaw;
        private final float pitch;
        private final float throttle;
        private final int power;
        private final int maxPower;
        private final int healthPercent;
        private final int flares;
        private final boolean airborne;
        private final int selectedHardpoint;
        private final int payloadMask;
        private final int payloadCounts;
        private final int distance;
        private final int maxRange;
        private final String weapon;

        private RemoteFrame(RemoteControlTelemetryMessage message) {
            this(message.entityId, message.vehicleType,
                    message.x, message.y, message.z,
                    message.motionX, message.motionY, message.motionZ,
                    message.yaw, message.pitch, message.throttle,
                    message.power, message.maxPower, message.healthPercent,
                    message.flares, message.airborne,
                    message.selectedHardpoint, message.payloadMask,
                    message.payloadCounts, message.distance,
                    message.maxRange, message.weapon);
        }

        private RemoteFrame(EntityWarTechAircraft aircraft,
                float clientThrottle) {
            this(aircraft.getEntityId(), aircraft.getRemoteVehicleType(),
                    aircraft.posX, aircraft.posY, aircraft.posZ,
                    aircraft.motionX, aircraft.motionY, aircraft.motionZ,
                    aircraft.rotationYaw, aircraft.rotationPitch,
                    clientThrottle, aircraft.getLegacyPower(),
                    aircraft.getEnergyCapacity(), aircraft.getHealthPercent(),
                    aircraft.getFlareCount(), aircraft.isRemoteAirborne(),
                    aircraft.getLegacySelectedHardpoint(),
                    aircraft.getLegacyPayloadMask(),
                    aircraft.getPackedPayloadCounts(),
                    aircraft.getDistanceFromLaunch(),
                    aircraft.getMissionRange(),
                    aircraft.getSelectedHardpointName());
        }

        private RemoteFrame(int entityId, int vehicleType,
                double x, double y, double z,
                double motionX, double motionY, double motionZ,
                float yaw, float pitch, float throttle,
                int power, int maxPower, int healthPercent, int flares,
                boolean airborne, int selectedHardpoint, int payloadMask,
                int payloadCounts, int distance, int maxRange,
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
            this.selectedHardpoint = Math.max(0, selectedHardpoint);
            this.payloadMask = payloadMask;
            this.payloadCounts = payloadCounts;
            this.distance = distance;
            this.maxRange = maxRange;
            this.weapon = weapon == null || weapon.isEmpty()
                    ? "EMPTY" : weapon;
        }
    }

    private static final class RemoteCameraEntity
            extends EntityOtherPlayerMP {
        private RemoteCameraEntity(World world) {
            super(world, CAMERA_PROFILE);
            setSize(0.0F, 0.0F);
            noClip = true;
            setInvisible(true);
        }

        @Override
        public void onUpdate() {
        }

        @Override
        public float getEyeHeight() {
            return 0.0F;
        }
    }
}
