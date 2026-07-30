import com.wartec.wartecmod.compat.client.RemoteControlClient;
import com.wartec.wartecmod.compat.client.RenderAdvancedMissile;
import com.wartec.wartecmod.compat.RemoteControlNetwork;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityMq9Munition;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class SmokeRemoteControlClient {
    public static void main(String[] args) throws Exception {
        Minecraft minecraft = new Minecraft();
        TestWorld world = new TestWorld();
        minecraft.field_71441_e = world;
        minecraft.field_71439_g = new EntityClientPlayerMP(
                minecraft.field_71441_e);
        minecraft.field_71474_y = new GameSettings();
        EntityMq9Drone drone = new EntityMq9Drone(world);
        drone.func_70107_b(32.0D, 64.0D, -16.0D);
        world.entity = drone;

        Method applyPendingState = RemoteControlClient.class
                .getDeclaredMethod("applyPendingState", Minecraft.class);
        applyPendingState.setAccessible(true);

        RemoteControlClient.acceptServerState(drone.func_145782_y(), true,
                RemoteControlNetwork.VEHICLE_MQ9,
                "Remote pilot engaged");
        applyPendingState.invoke(nullSafeInstance(), minecraft);
        RemoteControlClient instance = (RemoteControlClient) nullSafeInstance();
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        require(cameraField(instance) != null,
                "Remote camera state holder was not installed");
        require(minecraft.field_71451_h == cameraField(instance),
                "Remote camera was not installed as the stable render view");
        require(world.spawnedCamera == cameraField(instance),
                "Remote camera was not registered in the client world");

        Field cameraBindingField = RemoteControlClient.class
                .getDeclaredField("CAMERA");
        cameraBindingField.setAccessible(true);
        cameraBindingField.get(null);
        Keyboard.stubSetKeyDown(Keyboard.KEY_C, true);
        instance.onKeyInput(new cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent());
        Keyboard.stubSetKeyDown(Keyboard.KEY_C, false);
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        Field cameraModeField = RemoteControlClient.class
                .getDeclaredField("cameraMode");
        cameraModeField.setAccessible(true);
        require(cameraModeField.getInt(instance) == 1,
                "Raw camera key event did not switch to chase view");

        Keyboard.stubSetKeyDown(Keyboard.KEY_R, true);
        instance.onKeyInput(new cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent());
        Keyboard.stubSetKeyDown(Keyboard.KEY_R, false);
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        Field courseHoldField = RemoteControlClient.class
                .getDeclaredField("courseHold");
        courseHoldField.setAccessible(true);
        require(courseHoldField.getBoolean(instance),
                "Raw R input did not enable course hold");

        Mouse.stubSetButtonDown(0, true);
        instance.onMouseInput(
                new cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent());
        Field pendingFlagsField = RemoteControlClient.class
                .getDeclaredField("pendingRawFlags");
        pendingFlagsField.setAccessible(true);
        require((pendingFlagsField.getInt(instance)
                        & com.wartec.wartecmod.compat.RemoteControlNetwork.FLAG_FIRE)
                        != 0,
                "Raw left mouse input did not queue weapon fire");
        Mouse.stubSetButtonDown(0, false);

        Field throttleField = RemoteControlClient.class
                .getDeclaredField("throttle");
        throttleField.setAccessible(true);
        float throttleBefore = throttleField.getFloat(instance);
        Keyboard.stubSetKeyDown(Keyboard.KEY_W, true);
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        Keyboard.stubSetKeyDown(Keyboard.KEY_W, false);
        require(throttleField.getFloat(instance) > throttleBefore,
                "Raw W input did not increase throttle");

        Field controlYawField = RemoteControlClient.class
                .getDeclaredField("controlYaw");
        controlYawField.setAccessible(true);
        Field flightYawField = RemoteControlClient.class
                .getDeclaredField("flightYaw");
        flightYawField.setAccessible(true);
        Field cameraViewYawField = RemoteControlClient.class
                .getDeclaredField("cameraViewYaw");
        cameraViewYawField.setAccessible(true);
        Entity cameraForLook = (Entity) cameraField(instance);
        float yawBeforeLook = controlYawField.getFloat(instance);
        float flightYawBeforeLook = flightYawField.getFloat(instance);
        cameraForLook.field_70177_z =
                cameraViewYawField.getFloat(instance) + 7.0F;
        instance.onRenderTick(new TickEvent.RenderTickEvent(TickEvent.Phase.END,
                0.5F));
        require(Math.abs(controlYawField.getFloat(instance) - yawBeforeLook)
                        > 1.0F,
                "Render-camera mouse input was not captured");
        require(Math.abs(flightYawField.getFloat(instance)
                        - flightYawBeforeLook) < 0.001F,
                "Free-look mouse input changed the held flight course");

        Keyboard.stubSetKeyDown(Keyboard.KEY_R, true);
        instance.onKeyInput(new cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent());
        Keyboard.stubSetKeyDown(Keyboard.KEY_R, false);
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        require(!courseHoldField.getBoolean(instance),
                "Raw R input did not disable course hold");
        float yawBeforeKeyboard = controlYawField.getFloat(instance);
        Keyboard.stubSetKeyDown(Keyboard.KEY_A, true);
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        Keyboard.stubSetKeyDown(Keyboard.KEY_A, false);
        float yawAfterKeyboard = controlYawField.getFloat(instance);
        require(angleDifference(yawAfterKeyboard, yawBeforeKeyboard) < -1.8F
                        && angleDifference(yawAfterKeyboard,
                        yawBeforeKeyboard) > -2.5F,
                "Raw A input did not apply exactly one controlled yaw step");
        instance.onRenderTick(new TickEvent.RenderTickEvent(TickEvent.Phase.END,
                0.5F));
        require(Math.abs(angleDifference(controlYawField.getFloat(instance),
                        yawAfterKeyboard)) < 0.001F,
                "Keyboard steering fed back through the camera as mouse input");

        RemoteControlClient.acceptTelemetry(drone.func_145782_y(),
                RemoteControlNetwork.VEHICLE_MQ9, 1400.0D,
                92.0D, -1200.0D, 0.42D, 0.0D, -0.38D, 122.0F, -5.0F,
                0.66F, 720000, 800000, 91, 10, true, 0,
                (com.wartec.wartecmod.compat.ItemMq9Payload.HELLFIRE + 1)
                | (com.wartec.wartecmod.compat.ItemMq9Payload.GBU12 + 1) << 4,
                2 | 1 << 5, 1840, 2400, "AGM-114");
        world.entity = null;
        minecraft.field_71462_r = new GuiScreen();
        for (int tick = 0; tick < 120; tick++) {
            instance.onClientTick(new TickEvent.ClientTickEvent(
                    TickEvent.Phase.END));
        }
        require(cameraField(instance) != null,
                "A paused telemetry stream closed the remote camera");
        minecraft.field_71462_r = null;
        double bodyX = minecraft.field_71439_g.field_70165_t;
        instance.onRenderTick(new TickEvent.RenderTickEvent(TickEvent.Phase.START,
                0.5F));
        require(Math.abs(minecraft.field_71439_g.field_70165_t - bodyX) < 0.001D,
                "Remote camera moved the real player body");
        Entity camera = (Entity) cameraField(instance);
        require(Math.abs(camera.field_70165_t - bodyX) > 1.0D,
                "Remote camera did not move to the telemetry position");
        require(camera.field_70142_S == camera.field_70165_t
                        && camera.field_70137_T == camera.field_70163_u
                        && camera.field_70136_U == camera.field_70161_v,
                "Remote camera last-tick position was not synchronized");
        require(camera.field_70169_q == camera.field_70165_t
                        && camera.field_70167_r == camera.field_70163_u
                        && camera.field_70166_s == camera.field_70161_v,
                "Remote camera previous position was not synchronized");
        Field frameYawField = RemoteControlClient.class
                .getDeclaredField("frameYaw");
        frameYawField.setAccessible(true);
        double heading = Math.toRadians(frameYawField.getFloat(instance));
        double forwardX = -Math.sin(heading);
        double forwardZ = Math.cos(heading);
        double behind = (camera.field_70165_t - 1400.0D) * forwardX
                + (camera.field_70161_v + 1200.0D) * forwardZ;
        require(camera.field_70163_u > 99.0D && behind < -15.0D,
                "Chase camera was not placed above and behind the MQ-9: y="
                + camera.field_70163_u + " behind=" + behind);
        require(lookDot(camera, 1400.0D, 92.65D, -1200.0D) > 0.985D,
                "Chase camera was not aimed back at the MQ-9 airframe");
        require(minecraft.field_71451_h == camera,
                "Remote render view was not kept on the camera entity");
        require(cameraField(instance) != null,
                "Remote camera stopped when drone left client tracking range");

        RemoteControlClient.acceptEffect(drone.func_145782_y(),
                com.wartec.wartecmod.compat.RemoteControlNetwork.EFFECT_WEAPON,
                1400.0D, 92.0D, -1200.0D, 1100.0D, 4.0D, -900.0D, 0);
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        require(world.spawnedEffect instanceof EntityMq9Munition,
                "A distant weapon release did not create a client visual mirror");
        RemoteControlClient.acceptEffect(drone.func_145782_y(),
                com.wartec.wartecmod.compat.RemoteControlNetwork.EFFECT_FLARES,
                1400.0D, 92.0D, -1200.0D, 0.0D, 0.0D, 0.0D, 0);
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        require(world.particleCount >= 36,
                "Distant flare effects were not delivered to the remote camera");

        Keyboard.stubSetKeyDown(Keyboard.KEY_X, true);
        instance.onKeyInput(new cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent());
        Keyboard.stubSetKeyDown(Keyboard.KEY_X, false);
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        require(cameraField(instance) != null
                        && minecraft.field_71451_h == cameraField(instance),
                "Client exposed the remote player before server exit confirmation");
        RemoteControlClient.acceptServerState(drone.func_145782_y(), false,
                RemoteControlNetwork.VEHICLE_MQ9,
                "Remote pilot disengaged");
        applyPendingState.invoke(nullSafeInstance(), minecraft);
        require(minecraft.field_71451_h == minecraft.field_71439_g,
                "Player camera was not restored");
        require(camera.field_70128_L,
                "Remote camera entity was not removed after disconnect");

        int geranId = 901;
        RemoteControlClient.acceptServerState(geranId, true,
                RemoteControlNetwork.VEHICLE_GERAN,
                "Geran-2 remote link established");
        RemoteControlClient.acceptTelemetry(geranId,
                RemoteControlNetwork.VEHICLE_GERAN,
                600.0D, 78.0D, 400.0D, 0.5D, 0.02D, 0.1D,
                75.0F, -3.0F, 0.72F, 0, 0, 100, 0, true,
                0, 0, 0, 420, 1000, "WARHEAD");
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        cameraModeField.setInt(instance, 1);
        Field cameraReadyField = RemoteControlClient.class
                .getDeclaredField("cameraPositionReady");
        cameraReadyField.setAccessible(true);
        cameraReadyField.setBoolean(instance, false);
        instance.onRenderTick(new TickEvent.RenderTickEvent(TickEvent.Phase.START,
                0.5F));
        Entity geranCamera = (Entity) cameraField(instance);
        double geranHeading = Math.toRadians(75.0D);
        double geranForwardX = -Math.sin(geranHeading);
        double geranForwardZ = Math.cos(geranHeading);
        double geranBehind = (geranCamera.field_70165_t - 600.0D)
                * geranForwardX + (geranCamera.field_70161_v - 400.0D)
                * geranForwardZ;
        require(geranBehind < -7.0D && geranCamera.field_70163_u > 81.0D,
                "Geran chase camera was not placed above and behind the drone: "
                + "behind=" + geranBehind + " y=" + geranCamera.field_70163_u);
        RemoteControlClient.acceptServerState(geranId, false,
                RemoteControlNetwork.VEHICLE_GERAN, "Geran impact confirmed");
        applyPendingState.invoke(nullSafeInstance(), minecraft);
        int tu95Id = 902;
        RemoteControlClient.acceptServerState(tu95Id, true,
                RemoteControlNetwork.VEHICLE_TU95,
                "Tu-95 remote link established");
        RemoteControlClient.acceptTelemetry(tu95Id,
                RemoteControlNetwork.VEHICLE_TU95,
                -300.0D, 120.0D, 850.0D, 0.4D, 0.0D, 1.1D,
                25.0F, -2.0F, 0.84F, 3600000, 4000000, 96, 14, true,
                2, 1 | 2 << 4 | 3 << 8, 1 | 1 << 5 | 1 << 10,
                3100, 8000, "KAB-3000");
        instance.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        cameraModeField.setInt(instance, 1);
        cameraReadyField.setBoolean(instance, false);
        instance.onRenderTick(new TickEvent.RenderTickEvent(TickEvent.Phase.START,
                0.5F));
        Entity tu95Camera = (Entity) cameraField(instance);
        double tu95Heading = Math.toRadians(25.0D);
        double tu95ForwardX = -Math.sin(tu95Heading);
        double tu95ForwardZ = Math.cos(tu95Heading);
        double tu95Behind = (tu95Camera.field_70165_t + 300.0D)
                * tu95ForwardX + (tu95Camera.field_70161_v - 850.0D)
                * tu95ForwardZ;
        require(tu95Behind < -28.0D && tu95Camera.field_70163_u > 130.0D,
                "Tu-95 chase camera does not frame the complete bomber");
        Method vehicleName = RemoteControlClient.class.getDeclaredMethod(
                "getVehicleName", int.class);
        vehicleName.setAccessible(true);
        require("F-16C".equals(vehicleName.invoke(null,
                        Integer.valueOf(RemoteControlNetwork.VEHICLE_F16)))
                        && "Su-27".equals(vehicleName.invoke(null,
                        Integer.valueOf(RemoteControlNetwork.VEHICLE_SU27)))
                        && "Tu-95".equals(vehicleName.invoke(null,
                        Integer.valueOf(RemoteControlNetwork.VEHICLE_TU95))),
                "Aircraft HUD names are not mapped to telemetry types");
        RemoteControlClient.acceptServerState(tu95Id, false,
                RemoteControlNetwork.VEHICLE_TU95, "");
        applyPendingState.invoke(nullSafeInstance(), minecraft);
        Method pitchRotation = RenderAdvancedMissile.class.getDeclaredMethod(
                "forwardPitchRotation", float.class, boolean.class);
        pitchRotation.setAccessible(true);
        require(Math.abs(((Float) pitchRotation.invoke(null,
                        Float.valueOf(0.0F), Boolean.TRUE)).floatValue())
                        < 0.001F,
                "Level Geran model retained the old vertical 90 degree offset");
        require(Math.abs(((Float) pitchRotation.invoke(null,
                        Float.valueOf(0.0F), Boolean.FALSE)).floatValue()
                        - 90.0F) < 0.001F,
                "Legacy forward-axis missile pitch changed with Geran fix");
        Method yawRotation = RenderAdvancedMissile.class.getDeclaredMethod(
                "forwardYawRotation", float.class, float.class, boolean.class);
        yawRotation.setAccessible(true);
        require(Math.abs(((Float) yawRotation.invoke(null,
                        Float.valueOf(45.0F), Float.valueOf(180.0F),
                        Boolean.TRUE)).floatValue() - 135.0F) < 0.001F,
                "Geran -Z model nose does not follow its forward flight vector");
        require(Math.abs(((Float) yawRotation.invoke(null,
                        Float.valueOf(45.0F), Float.valueOf(0.0F),
                        Boolean.FALSE)).floatValue() - 45.0F) < 0.001F,
                "Legacy missile yaw changed with Geran alignment fix");
        RemoteControlClient.acceptServerState(geranId, true,
                RemoteControlNetwork.VEHICLE_GERAN, "");
        applyPendingState.invoke(nullSafeInstance(), minecraft);
        RenderPlayerEvent.Pre playerRender =
                new RenderPlayerEvent.Pre(minecraft.field_71439_g);
        instance.onRenderPlayer(playerRender);
        require(playerRender.canceled,
                "Remote Pilot did not hide the operator silhouette and name");
        RemoteControlClient.acceptServerState(geranId, false,
                RemoteControlNetwork.VEHICLE_GERAN, "");
        applyPendingState.invoke(nullSafeInstance(), minecraft);
        System.out.println("Remote Pilot client state smoke test passed");
    }

    private static float angleDifference(float value, float origin) {
        float difference = value - origin;
        while (difference <= -180.0F) difference += 360.0F;
        while (difference > 180.0F) difference -= 360.0F;
        return difference;
    }

    private static double lookDot(Entity camera, double x, double y, double z) {
        double yaw = Math.toRadians(camera.field_70177_z);
        double pitch = Math.toRadians(camera.field_70125_A);
        double horizontal = Math.cos(pitch);
        double lookX = -Math.sin(yaw) * horizontal;
        double lookY = -Math.sin(pitch);
        double lookZ = Math.cos(yaw) * horizontal;
        double targetX = x - camera.field_70165_t;
        double targetY = y - camera.field_70163_u;
        double targetZ = z - camera.field_70161_v;
        double length = Math.sqrt(targetX * targetX + targetY * targetY
                + targetZ * targetZ);
        return (lookX * targetX + lookY * targetY + lookZ * targetZ) / length;
    }

    private static Object nullSafeInstance() throws Exception {
        java.lang.reflect.Field field = RemoteControlClient.class
                .getDeclaredField("INSTANCE");
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object cameraField(RemoteControlClient instance)
            throws Exception {
        java.lang.reflect.Field field = RemoteControlClient.class
                .getDeclaredField("camera");
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class TestWorld extends WorldClient {
        private Entity entity;
        private Entity spawnedCamera;
        private Entity spawnedEffect;
        private int particleCount;

        @Override
        public boolean func_72838_d(Entity value) {
            if (value instanceof EntityMq9Munition) spawnedEffect = value;
            else spawnedCamera = value;
            return true;
        }

        @Override
        public void func_72869_a(String name, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            particleCount++;
        }

        @Override
        public Entity func_73045_a(int id) {
            return entity != null && entity.func_145782_y() == id
                    ? entity : null;
        }
    }
}
