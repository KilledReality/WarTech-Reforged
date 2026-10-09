package com.wartec.wartecmod.port.client;

import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import com.wartec.wartecmod.port.entity.EntityCustomUav;
import com.wartec.wartecmod.port.integration.AviationOrdnance;
import com.wartec.wartecmod.port.network.RemoteControlNetwork;
import com.wartec.wartecmod.port.network.RemoteControlTelemetryMessage;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import net.minecraftforge.event.world.WorldEvent;
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
    private static final double TELEMETRY_BUFFER_TICKS = 4.0D;
    private static final double MAX_TELEMETRY_EXTRAPOLATION_TICKS = 2.5D;
    private static final int MAX_TELEMETRY_FRAMES = 32;

    private static volatile PendingState pendingState;
    private static final ConcurrentLinkedQueue<RemoteFrame>
            PENDING_TELEMETRY = new ConcurrentLinkedQueue<RemoteFrame>();
    private static final Set<Integer> HIDDEN_REMOTE_OPERATORS =
            Collections.newSetFromMap(
                    new ConcurrentHashMap<Integer, Boolean>());

    private int entityId = -1;
    private int vehicleType;
    private RemoteCameraEntity camera;
    private float throttle;
    private float controlYaw;
    private float controlPitch;
    private float flightYaw;
    private float flightPitch;
    private final RemoteLookInput mouseLook = new RemoteLookInput();
    private RemoteCameraEntity mouseFrameCamera;
    private int cameraMode;
    private int savedPerspective = -1;
    private final RemoteCameraTransition cameraTransition = new RemoteCameraTransition();
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
    private final Deque<RemoteFrame> telemetryFrames =
            new ArrayDeque<RemoteFrame>();
    private double telemetryRenderTick;
    private boolean telemetryTimelineReady;
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
            PENDING_TELEMETRY.offer(new RemoteFrame(message));
        }
    }

    public static void acceptOperatorVisibility(int playerEntityId,
            boolean hidden) {
        if (hidden) {
            HIDDEN_REMOTE_OPERATORS.add(playerEntityId);
        } else {
            HIDDEN_REMOTE_OPERATORS.remove(playerEntityId);
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
        // InputEvent and KeyBinding describe the same press. Always drain the
        // binding, even when the event flag is already set (no short-circuit).
        if (pollCameraToggle(minecraft)) {
            switchCameraMode();
            tell(minecraft, vehicleName(vehicleType)
                    + (cameraMode == 0
                            ? " camera: NOSE" : " camera: CHASE"));
        }
        boolean courseHoldPressed=false;
        while(COURSE_HOLD.isPressed()) courseHoldPressed=true;
        if (courseHoldToggleRequested || courseHoldPressed) {
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
        mouseLook.reset();
        applyPendingTelemetry();
        Entity controlled = getAircraft(minecraft);
        updateRemoteFrame(controlled,
                Math.max(0.0D, Math.min(1.0D, event.renderTickTime)));
        updateCamera();
        minecraft.setRenderViewEntity(camera);
        captureMouseLookBaseline(minecraft);
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
        if (HIDDEN_REMOTE_OPERATORS.contains(
                    event.getEntityPlayer().getEntityId())
                || entityId >= 0
                    && event.getEntityPlayer() == minecraft.player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            HIDDEN_REMOTE_OPERATORS.clear();
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
        camera = null;
        throttle = initialThrottle(vehicleType);
        if(savedPerspective<0) savedPerspective=minecraft.gameSettings.thirdPersonView;
        minecraft.gameSettings.thirdPersonView=0;
        cameraTransition.reset();
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
        telemetryFrames.clear();
        // State and initial telemetry may arrive before the same client tick.
        // Keep queued packets; applyPendingTelemetry filters the new entity ID.
        telemetryTimelineReady = false;
        telemetryRenderTick = 0.0D;
        lastRenderNanos = 0L;
        mouseLook.reset();
        minecraft.displayGuiScreen(null);
    }

    private void applyPendingTelemetry() {
        RemoteFrame frame;
        while ((frame = PENDING_TELEMETRY.poll()) != null) {
            if (frame.entityId != entityId
                    || latestTelemetry != null
                        && frame.serverTick <= latestTelemetry.serverTick) {
                continue;
            }
            telemetryFrames.addLast(frame);
            while (telemetryFrames.size() > MAX_TELEMETRY_FRAMES) {
                telemetryFrames.removeFirst();
            }
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
        updateCamera();
        minecraft.setRenderViewEntity(camera);
    }

    private void captureMouseLookBaseline(Minecraft minecraft) {
        mouseFrameCamera=camera;
        mouseLook.begin(minecraft.player,minecraft.player.rotationYaw,minecraft.player.rotationPitch,
                minecraft.currentScreen==null && minecraft.inGameHasFocus && !exitAwaitingServer);
    }

    private void applyMouseLook(Minecraft minecraft) {
        // Scheduled network packets run before RenderTick.START. Vanilla's
        // EntityRenderer calls mc.player.turn between START and END. Comparing
        // with a session-old anchor misread SPacketPlayerPosLook as mouse input.
        if(camera!=mouseFrameCamera || !mouseLook.consume(minecraft.player,
                minecraft.player.rotationYaw,minecraft.player.rotationPitch)) return;
        float yawDelta=mouseLook.yawDelta(),pitchDelta=mouseLook.pitchDelta();
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
        minecraft.player.rotationYaw = mouseLook.baselineYaw();
        minecraft.player.rotationPitch = mouseLook.baselinePitch();
        camera.prevRotationYaw = camera.rotationYaw = cameraViewYaw;
        camera.prevRotationPitch = camera.rotationPitch = cameraViewPitch;
    }

    private void updateRemoteFrame(Entity entity, double partialTick) {
        if (!telemetryFrames.isEmpty()) {
            updateRemoteFrameFromTelemetry();
            return;
        }
        if (entity != null) {
            updateRemoteFrameFromEntity(entity, partialTick);
            return;
        }
        if (latestTelemetry != null) seedRemoteFrame(latestTelemetry);
    }

    private void updateRemoteFrameFromTelemetry() {
        long now = System.nanoTime();
        double delta = lastRenderNanos == 0L ? 0.0D
                : clamp((now - lastRenderNanos) / 50000000.0D,
                        0.0D, 1.25D);
        renderDeltaTicks = delta;
        lastRenderNanos = now;
        RemoteFrame oldest = telemetryFrames.peekFirst();
        RemoteFrame newest = telemetryFrames.peekLast();
        if (!telemetryTimelineReady) {
            telemetryRenderTick = oldest.serverTick;
            if (newest.serverTick - oldest.serverTick
                    >= TELEMETRY_BUFFER_TICKS) {
                telemetryTimelineReady = true;
            }
        } else {
            double forwardLimit = newest.serverTick
                    + MAX_TELEMETRY_EXTRAPOLATION_TICKS;
            telemetryRenderTick = Math.min(forwardLimit,
                    telemetryRenderTick + delta);
        }
        while (telemetryFrames.size() > 2) {
            RemoteFrame second = secondTelemetryFrame();
            if (second == null || second.serverTick > telemetryRenderTick) {
                break;
            }
            telemetryFrames.removeFirst();
        }
        RemoteFrame from = telemetryFrames.peekFirst();
        RemoteFrame to = secondTelemetryFrame();
        if (to == null) {
            applyTelemetryTarget(from.x, from.y, from.z,
                    from.motionX, from.motionY, from.motionZ,
                    from.yaw, from.pitch, delta);
        } else if (telemetryRenderTick <= to.serverTick) {
            double span = Math.max(1.0D, to.serverTick - from.serverTick);
            double progress = clamp(
                    (telemetryRenderTick - from.serverTick) / span,
                    0.0D, 1.0D);
            applyTelemetryTarget(
                    interpolate(from.x, to.x, progress),
                    interpolate(from.y, to.y, progress),
                    interpolate(from.z, to.z, progress),
                    interpolate(from.motionX, to.motionX, progress),
                    interpolate(from.motionY, to.motionY, progress),
                    interpolate(from.motionZ, to.motionZ, progress),
                    normalizeAngle(from.yaw + normalizeAngle(to.yaw - from.yaw)
                            * (float) progress),
                    (float) interpolate(from.pitch, to.pitch, progress), delta);
        } else {
            double ahead = Math.min(MAX_TELEMETRY_EXTRAPOLATION_TICKS,
                    telemetryRenderTick - to.serverTick);
            double span = Math.max(1.0D, to.serverTick - from.serverTick);
            float yawRate = normalizeAngle(to.yaw - from.yaw) / (float) span;
            float pitchRate = (to.pitch - from.pitch) / (float) span;
            applyTelemetryTarget(to.x + to.motionX * ahead,
                    to.y + to.motionY * ahead, to.z + to.motionZ * ahead,
                    to.motionX, to.motionY, to.motionZ,
                    normalizeAngle(to.yaw + yawRate * (float) ahead),
                    to.pitch + pitchRate * (float) ahead, delta);
        }
        remoteFrameReady = true;
    }

    private RemoteFrame secondTelemetryFrame() {
        java.util.Iterator<RemoteFrame> iterator = telemetryFrames.iterator();
        if (!iterator.hasNext()) return null;
        iterator.next();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private void applyTelemetryTarget(double targetX, double targetY,
            double targetZ, double targetMotionX, double targetMotionY,
            double targetMotionZ, float targetYaw, float targetPitch,
            double delta) {
        // Continue at render rate and converge on the ordered timeline. Short
        // packet gaps therefore do not freeze the camera, and the monotonic
        // render tick prevents a late packet from pulling it backwards.
        frameX += frameMotionX * delta;
        frameY += frameMotionY * delta;
        frameZ += frameMotionZ * delta;
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
            double correction = 1.0D - Math.exp(-delta * 0.82D);
            frameX += errorX * correction;
            frameY += errorY * correction;
            frameZ += errorZ * correction;
        }
        double motionBlend = 1.0D - Math.exp(-delta * 0.55D);
        frameMotionX += (targetMotionX - frameMotionX) * motionBlend;
        frameMotionY += (targetMotionY - frameMotionY) * motionBlend;
        frameMotionZ += (targetMotionZ - frameMotionZ) * motionBlend;
        float headingBlend = (float) (1.0D - Math.exp(-delta * 0.72D));
        frameYaw = normalizeAngle(frameYaw
                + normalizeAngle(targetYaw - frameYaw) * headingBlend);
        framePitch += (targetPitch - framePitch)
                * (float) Math.min(1.0D, delta * 0.72D);
    }

    private void updateRemoteFrameFromEntity(Entity entity,
            double partialTick) {
        double tick = clamp(partialTick, 0.0D, 1.0D);
        frameX = interpolate(entity.lastTickPosX, entity.posX, tick);
        frameY = interpolate(entity.lastTickPosY, entity.posY, tick);
        frameZ = interpolate(entity.lastTickPosZ, entity.posZ, tick);
        frameMotionX = entity.motionX;
        frameMotionY = entity.motionY;
        frameMotionZ = entity.motionZ;
        frameYaw = normalizeAngle(entity.prevRotationYaw
                + normalizeAngle(entity.rotationYaw - entity.prevRotationYaw)
                        * (float) tick);
        framePitch = entity.prevRotationPitch
                + (entity.rotationPitch - entity.prevRotationPitch)
                        * (float) tick;
        long now = System.nanoTime();
        renderDeltaTicks = lastRenderNanos == 0L ? 0.0D
                : clamp((now - lastRenderNanos) / 50000000.0D,
                        0.0D, 1.25D);
        lastRenderNanos = now;
        remoteFrameReady = true;
    }

    private void seedRemoteFrame(Entity entity) {
        RemoteFrame frame = !telemetryFrames.isEmpty()
                ? telemetryFrames.peekFirst() : latestTelemetry != null
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

    private boolean pollCameraToggle(Minecraft minecraft) {
        boolean pressed=false;
        while(CAMERA.isPressed()) pressed=true;
        boolean vanillaPerspective=minecraft.gameSettings.thirdPersonView!=0;
        minecraft.gameSettings.thirdPersonView=0;
        boolean toggle=cameraToggleRequested || pressed || vanillaPerspective;
        cameraToggleRequested=false;
        return toggle;
    }

    private void switchCameraMode() {
        cameraTransition.start(cameraViewYaw,cameraViewPitch);
        cameraMode=(cameraMode+1)%2;
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
        double size=com.wartec.wartecmod.port.entity.VehicleDimensions.remoteScale(vehicleType);
        distance*=size;height*=size;
        double x = frameX + forwardX * distance;
        double y = frameY + height;
        double z = frameZ + forwardZ * distance;
        if ((cameraMode == 1 || cameraTransition.active()) && cameraPositionReady && visualReady) {
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
            // Focus belongs to the same smoothed rig, not the unsmoothed
            // telemetry position: a chunk/packet catch-up must not pitch it skyward.
            double focusX = x + forwardX * (chaseFocusDistance()*size-distance);
            double focusY = y + chaseFocusHeight()*size-height;
            double focusZ = z + forwardZ * (chaseFocusDistance()*size-distance);
            double dx = focusX - x;
            double dy = focusY - y;
            double dz = focusZ - z;
            cameraViewYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            cameraViewPitch = (float) -Math.toDegrees(
                    Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        }
        cameraViewYaw=cameraTransition.yaw(cameraViewYaw);
        cameraViewPitch=cameraTransition.pitch(cameraViewPitch);
        cameraTransition.advance(renderDeltaTicks);
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
        mouseLook.reset();mouseFrameCamera=null;
        cameraTransition.reset();
        if(minecraft!=null && savedPerspective>=0) minecraft.gameSettings.thirdPersonView=savedPerspective;
        savedPerspective=-1;
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
        telemetryFrames.clear();
        PENDING_TELEMETRY.clear();
        telemetryTimelineReady = false;
        telemetryRenderTick = 0.0D;
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

    static RemoteRenderPose resolveControlledRenderPose(Entity entity,
            double x, double y, double z, float partialTicks) {
        RemoteControlClient client = INSTANCE;
        if (entity == null || client.entityId != entity.getEntityId()
                || client.cameraMode != 1 || client.camera == null
                || !client.remoteFrameReady) {
            return new RemoteRenderPose(x, y, z, Float.NaN, Float.NaN);
        }
        double entityX = interpolate(entity.lastTickPosX,
                entity.posX, partialTicks);
        double entityY = interpolate(entity.lastTickPosY,
                entity.posY, partialTicks);
        double entityZ = interpolate(entity.lastTickPosZ,
                entity.posZ, partialTicks);
        return new RemoteRenderPose(
                x + client.frameX - entityX,
                y + client.frameY - entityY,
                z + client.frameZ - entityZ,
                client.frameYaw, client.framePitch);
    }

    private Entity getAircraft(Minecraft minecraft) {
        if (minecraft.world == null || entityId < 0) {
            return null;
        }
        Entity entity = minecraft.world.getEntityByID(entityId);
        return (entity instanceof EntityWarTechAircraft
                || entity instanceof EntityCustomUav
                || entity instanceof EntityWarTechMissile) && !entity.isDead
                ? entity : null;
    }

    private RemoteFrame snapshotFromEntity(Entity entity) {
        if (entity instanceof EntityWarTechAircraft) {
            return new RemoteFrame((EntityWarTechAircraft) entity, throttle);
        }
        if (entity instanceof EntityCustomUav) {
            return new RemoteFrame((EntityCustomUav) entity, throttle);
        }
        return null;
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
                + com.wartec.wartecmod.port.uav.UavText.ui(" REMOTE OPTICAL LINK"), 8, 6, green);
        drawRight(font, cameraMode == 0
                ? com.wartec.wartecmod.port.uav.UavText.ui("CAM 1: NOSE") : com.wartec.wartecmod.port.uav.UavText.ui("CAM 2: CHASE"),
                width - 8, 6, amber);
        String range = String.format(Locale.ROOT, com.wartec.wartecmod.port.uav.UavText.ui("RANGE %04d / %04d"),
                frame.distance, frame.maxRange);
        font.drawString(range, centerX - font.getStringWidth(range) / 2, 6,
                frame.distance >= frame.maxRange * 9 / 10
                        ? 0xFFFFCC58 : amber);
        boolean geran = frame.vehicleType == 1 || frame.vehicleType == 5 || frame.vehicleType == 8;
        if (geran) {
            font.drawString(com.wartec.wartecmod.port.uav.UavText.ui("IMPACT FUSE ARMED"),
                    8, height - 44, 0xFFFFCC58);
        } else {
            drawLoadout(font, width, height - 44,
                    frame, amber, 0xFFFFCC58);
        }
        font.drawString(String.format(Locale.ROOT,
                com.wartec.wartecmod.port.uav.UavText.ui("THR %03d%%   SPD %4.2f   ALT %04d"),
                Math.round(frame.throttle * 100.0F), speed, (int) frame.y),
                8, height - 30, amber);
        font.drawString((geran ? "" : com.wartec.wartecmod.port.uav.UavText.ui("PWR ") + frame.power + "/"
                + frame.maxPower + "   ")
                + com.wartec.wartecmod.port.uav.UavText.ui("AIRFRAME ") + frame.healthPercent + "%",
                8, height - 18, green);
        String heading = String.format(Locale.ROOT, com.wartec.wartecmod.port.uav.UavText.ui("HDG %03d"),
                (int) ((flightYaw + 360.0F) % 360.0F));
        font.drawString(heading,
                centerX - font.getStringWidth(heading) / 2,
                height - 30, amber);
        String link = (telemetryAge <= 6
                ? com.wartec.wartecmod.port.uav.UavText.ui("LINK STABLE") : com.wartec.wartecmod.port.uav.UavText.ui("LINK BUFFERING"))
                + (courseHold ? com.wartec.wartecmod.port.uav.UavText.ui("  |  COURSE HOLD") : "")
                + (geran ? com.wartec.wartecmod.port.uav.UavText.ui("  |  WARHEAD LIVE")
                        : com.wartec.wartecmod.port.uav.UavText.ui("  |  LTC ") + frame.flares);
        font.drawString(link, centerX - font.getStringWidth(link) / 2,
                height - 18, green);
        if (geran) {
            drawRight(font, com.wartec.wartecmod.port.uav.UavText.ui("WARHEAD: CONTACT"),
                    width - 8, height - 30, 0xFFFFCC58);
            drawRight(font,
                    com.wartec.wartecmod.port.uav.UavText.ui("A/D TURN  SPACE/SHIFT PITCH  C CAM  R HOLD  X EXIT"),
                    width - 8, height - 18, amber);
        } else {
            int selectedCount = payloadCount(
                    frame.payloadCounts, frame.selectedHardpoint);
            drawRight(font, com.wartec.wartecmod.port.uav.UavText.ui("HP ") + (frame.selectedHardpoint + 1)
                    + ": " + com.wartec.wartecmod.port.uav.UavText.ui(frame.weapon) + " x" + selectedCount,
                    width - 8, height - 30, 0xFFFFCC58);
            drawRight(font,
                    com.wartec.wartecmod.port.uav.UavText.ui("LMB FIRE  Z SELECT  F LTC  C CAM  R HOLD  X EXIT"),
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
        if(code>=13) return com.wartec.wartecmod.port.uav.UavText.ui("CUSTOM CRUISE");
        if (vehicleType == 4) {
            if (code == 10) return "KH-555";
            if (code == 11) return "FAB-5000";
            if (code == 12) return "KAB-3000";
            return com.wartec.wartecmod.port.uav.UavText.ui("EMPTY");
        }
        return code >= 1 && code <= 9
                ? AviationOrdnance.getName(code - 1) : com.wartec.wartecmod.port.uav.UavText.ui("EMPTY");
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
            minecraft.player.sendMessage(message.startsWith("uav.message.")
                    ? new net.minecraft.util.text.TextComponentTranslation(message)
                    : new TextComponentString(message));
        }
    }

    private static String vehicleName(int type) {
        if (type == 1) return "Geran-2";
        if (type == 8) return "Geran-5";
        if (type == 2) return "F-16C";
        if (type == 3) return "Su-27";
        if (type == 4) return "Tu-95";
        if (type == 5) return com.wartec.wartecmod.port.uav.UavText.ui("Custom one-way UAV");
        if (type == 6) return com.wartec.wartecmod.port.uav.UavText.ui("Custom reconnaissance UAV");
        if (type == 7) return com.wartec.wartecmod.port.uav.UavText.ui("Custom strike UAV");
        return "MQ-9";
    }

    private static float initialThrottle(int type) {
        if (type == 1 || type == 8) return 0.72F;
        if (type == 5) return 0.28F;
        if (type == 2 || type == 3) return 0.18F;
        if (type == 4) return 0.16F;
        return 0.28F;
    }

    private float keyboardTurnStep() {
        if (vehicleType == 1 || vehicleType == 5 || vehicleType == 8) return 3.6F;
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
        if(vehicleType==8) return 2.45D;
        if (vehicleType == 1 || vehicleType == 5 || vehicleType == 8) return 1.25D;
        if (vehicleType == 6) return 3.20D;
        if (vehicleType == 7) return 5.85D;
        if (vehicleType == 2) return 2.8D;
        if (vehicleType == 3) return 3.4D;
        if (vehicleType == 4) return 7.2D;
        return 2.15D;
    }

    private double noseCameraHeight() {
        if(vehicleType==8) return .30D;
        if (vehicleType == 1 || vehicleType == 5 || vehicleType == 8) return 0.35D;
        if (vehicleType == 6) return 0.82D;
        if (vehicleType == 7) return 1.32D;
        if (vehicleType == 2) return 1.0D;
        if (vehicleType == 3) return 1.25D;
        if (vehicleType == 4) return 2.4D;
        return 1.18D;
    }

    private double chaseCameraDistance() {
        if (vehicleType == 1 || vehicleType == 5 || vehicleType == 8) return -7.5D;
        if (vehicleType == 6) return -17.0D;
        if (vehicleType == 7) return -28.0D;
        if (vehicleType == 2) return -13.0D;
        if (vehicleType == 3) return -15.0D;
        if (vehicleType == 4) return -29.0D;
        return -16.5D;
    }

    private double chaseCameraHeight() {
        if (vehicleType == 1 || vehicleType == 5 || vehicleType == 8) return 3.7D;
        if (vehicleType == 6) return 8.0D;
        if (vehicleType == 7) return 12.0D;
        if (vehicleType == 2) return 5.0D;
        if (vehicleType == 3) return 5.8D;
        if (vehicleType == 4) return 11.5D;
        return 8.0D;
    }

    private double chaseFocusDistance() {
        if (vehicleType == 6) return 3.6D;
        if (vehicleType == 7) return 6.2D;
        if (vehicleType == 2) return 3.0D;
        if (vehicleType == 3) return 3.7D;
        if (vehicleType == 4) return 8.0D;
        return 2.4D;
    }

    private double chaseFocusHeight() {
        if (vehicleType == 6) return 0.92D;
        if (vehicleType == 7) return 1.45D;
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

    private static double interpolate(double start, double end,
            double progress) {
        return start + (end - start) * progress;
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
        private final int serverTick;
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
            this(message.entityId, message.vehicleType, message.serverTick,
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
                    aircraft.ticksExisted,
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

        private RemoteFrame(EntityCustomUav uav, float clientThrottle) {
            this(uav.getEntityId(), uav.getRemoteVehicleType(),
                    uav.ticksExisted,
                    uav.posX, uav.posY, uav.posZ,
                    uav.motionX, uav.motionY, uav.motionZ,
                    uav.rotationYaw, uav.rotationPitch,
                    clientThrottle, uav.getLegacyPower(),
                    uav.getEnergyCapacity(), uav.getHealthPercent(),
                    uav.getFlareCount(), uav.isRemoteAirborne(),
                    uav.getLegacySelectedHardpoint(),
                    uav.getLegacyPayloadMask(),
                    uav.getPackedPayloadCounts(),
                    uav.getDistanceFromLaunch(), uav.getLinkRange(),
                    uav.getSelectedHardpointName());
        }

        private RemoteFrame(int entityId, int vehicleType, int serverTick,
                double x, double y, double z,
                double motionX, double motionY, double motionZ,
                float yaw, float pitch, float throttle,
                int power, int maxPower, int healthPercent, int flares,
                boolean airborne, int selectedHardpoint, int payloadMask,
                int payloadCounts, int distance, int maxRange,
                String weapon) {
            this.entityId = entityId;
            this.vehicleType = vehicleType;
            this.serverTick = serverTick;
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
                    ? com.wartec.wartecmod.port.uav.UavText.ui("EMPTY") : weapon;
        }
    }

    static final class RemoteRenderPose {
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float pitch;

        private RemoteRenderPose(double x, double y, double z,
                float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
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
