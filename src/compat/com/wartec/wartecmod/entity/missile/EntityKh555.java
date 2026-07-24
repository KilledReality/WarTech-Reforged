package com.wartec.wartecmod.entity.missile;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.wartec.wartecmod.compat.MissileChunkLoader;
import com.wartec.wartecmod.compat.ITeamOwned;
import com.wartec.wartecmod.compat.StrategicAviationContent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class EntityKh555 extends EntitySupersonicCruiseMissileHE implements ITeamOwned {
    private static final int DW_AIR_LAUNCHED = 10;
    private static final int DW_TARGET_X = 11;
    private static final int DW_TARGET_Y = 12;
    private static final int DW_TARGET_Z = 13;
    private static final double RELEASE_SPEED = 1.15D;
    private static final double CRUISE_SPEED = 2.20D;
    private static final double TERMINAL_SPEED = 2.55D;
    private static final int MAX_AIR_TICKS = 2600;

    private boolean airLaunched;
    private double airStartX;
    private double airStartZ;
    private double airRange;
    private double routeLateral;
    private double routeWave;
    private double routePhase;
    private String ownerTeam = "";

    public EntityKh555(World world) {
        super(world);
    }

    @Override public String getOwnerTeam() { return ownerTeam; }
    @Override public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    public EntityKh555(World world, float x, float y, float z,
            int targetX, int targetZ) {
        super(world, x, y, z, targetX, targetZ);
    }

    @Override
    protected void func_70088_a() {
        super.func_70088_a();
        field_70180_af.func_75682_a(DW_AIR_LAUNCHED, Byte.valueOf((byte) 0));
        field_70180_af.func_75682_a(DW_TARGET_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Y, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Z, Integer.valueOf(0));
    }

    /** Switches the stock ground-launch arc to an independent air profile. */
    public void configureAirLaunch(float yaw) {
        configureAirLaunch(yaw, 0.0D, 0.0D, 0.0D);
    }

    public void configureAirLaunch(float yaw, double carrierMotionX,
            double carrierMotionY, double carrierMotionZ) {
        airLaunched = true;
        startX = floor(field_70165_t);
        startY = floor(field_70163_u);
        startZ = floor(field_70161_v);
        targetY = field_70170_p.func_72976_f(targetX, targetZ);
        airStartX = field_70165_t;
        airStartZ = field_70161_v;
        double targetDx = targetX + 0.5D - airStartX;
        double targetDz = targetZ + 0.5D - airStartZ;
        airRange = Math.max(1.0D, Math.sqrt(targetDx * targetDx + targetDz * targetDz));
        routeLateral = randomSigned(38.0D, Math.min(190.0D, airRange * 0.085D));
        routeWave = (field_70170_p.field_73012_v.nextDouble() - 0.5D) * 0.34D;
        routePhase = field_70170_p.field_73012_v.nextDouble() * Math.PI * 2.0D;

        double angle = Math.toRadians(yaw);
        field_70159_w = -Math.sin(angle) * RELEASE_SPEED + carrierMotionX * 0.35D;
        field_70181_x = Math.min(0.02D, carrierMotionY * 0.10D - 0.015D);
        field_70179_y = Math.cos(angle) * RELEASE_SPEED + carrierMotionZ * 0.35D;
        syncAirLaunch();
        updateAirRotation();
    }

    public boolean isAirLaunched() {
        return airLaunched || field_70180_af.func_75683_a(DW_AIR_LAUNCHED) != 0;
    }

    @Override
    public void func_70071_h_() {
        if (!isAirLaunched()) {
            super.func_70071_h_();
            return;
        }
        tickAirLaunch();
    }

    private void tickAirLaunch() {
        field_70169_q = field_70165_t;
        field_70167_r = field_70163_u;
        field_70166_s = field_70161_v;
        field_70126_B = field_70177_z;
        field_70127_C = field_70125_A;
        field_70173_aa++;

        if (field_70170_p.field_72995_K) {
            airLaunched = true;
            targetX = field_70180_af.func_75679_c(DW_TARGET_X);
            targetY = field_70180_af.func_75679_c(DW_TARGET_Y);
            targetZ = field_70180_af.func_75679_c(DW_TARGET_Z);
            func_70107_b(field_70165_t + field_70159_w,
                    field_70163_u + field_70181_x,
                    field_70161_v + field_70179_y);
            updateAirRotation();
            spawnAirTrail();
            return;
        }

        MissileChunkLoader.track(this);
        double finalX = targetX + 0.5D;
        double finalZ = targetZ + 0.5D;
        double targetDx = finalX - field_70165_t;
        double targetDz = finalZ - field_70161_v;
        double remaining = Math.sqrt(targetDx * targetDx + targetDz * targetDz);
        double targetDy = targetY + 0.8D - field_70163_u;
        int localGround = field_70170_p.func_72976_f(
                floor(field_70165_t), floor(field_70161_v));
        if (shouldDetonateAtTarget(remaining, targetDy, motionLength())
                || field_70173_aa > 10 && field_70163_u <= localGround + 0.75D) {
            detonateAirLaunch();
            return;
        }
        if (field_70173_aa > MAX_AIR_TICKS) {
            func_70106_y();
            return;
        }

        double[] aim = calculateRouteAim(remaining, finalX, finalZ);
        double aimDx = aim[0] - field_70165_t;
        double aimDz = aim[1] - field_70161_v;
        double aimDistance = Math.sqrt(aimDx * aimDx + aimDz * aimDz);
        if (aimDistance < 0.001D) {
            aimDx = targetDx;
            aimDz = targetDz;
            aimDistance = Math.max(0.001D, remaining);
        }
        double desiredY = calculateAirLaunchDesiredY(remaining, localGround, targetY);
        double speed = calculateAirLaunchSpeed(field_70173_aa, remaining);
        double vertical = clamp((desiredY - field_70163_u) * 0.055D,
                -calculateDescentLimit(airRange, remaining), 0.20D);
        double horizontalSpeed = Math.sqrt(Math.max(0.16D,
                speed * speed - vertical * vertical));
        double turn = remaining < 90.0D ? 0.46D : remaining < 240.0D ? 0.26D : 0.15D;
        field_70159_w = blend(field_70159_w,
                aimDx / aimDistance * horizontalSpeed, turn);
        field_70181_x = blend(field_70181_x, vertical, turn);
        field_70179_y = blend(field_70179_y,
                aimDz / aimDistance * horizontalSpeed, turn);
        normalizeMotion(speed);
        func_70107_b(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        updateAirRotation();
    }

    private double[] calculateRouteAim(double remaining, double finalX,
            double finalZ) {
        if (remaining < 145.0D || airRange < 1.0D) {
            return new double[] {finalX, finalZ};
        }
        double progress = clamp(1.0D - remaining / airRange, 0.0D, 1.0D);
        double lookAhead = clamp(progress + Math.max(0.025D, 65.0D / airRange),
                0.0D, 1.0D);
        double routeDx = finalX - airStartX;
        double routeDz = finalZ - airStartZ;
        double routeLength = Math.max(0.001D, Math.sqrt(routeDx * routeDx + routeDz * routeDz));
        double offset = Math.sin(Math.PI * lookAhead) * routeLateral
                * (1.0D + routeWave * (lookAhead * 2.0D - 1.0D));
        return new double[] {
                airStartX + routeDx * lookAhead - routeDz / routeLength * offset,
                airStartZ + routeDz * lookAhead + routeDx / routeLength * offset
        };
    }

    public static double calculateAirLaunchDesiredY(double remaining,
            int localGround, int targetGround) {
        if (remaining > 240.0D) {
            return Math.max(localGround + 25.0D, targetGround + 28.0D);
        }
        if (remaining > 70.0D) {
            double blend = (remaining - 70.0D) / 170.0D;
            double terminal = targetGround + 11.3D + blend * 16.7D;
            return Math.max(localGround + 7.0D, terminal);
        }
        return targetGround + 0.8D + remaining * 0.15D;
    }

    public static double calculateAirLaunchSpeed(int ticks, double remaining) {
        return remaining < 110.0D ? TERMINAL_SPEED
                : Math.min(CRUISE_SPEED, RELEASE_SPEED + ticks * 0.055D);
    }

    public static boolean shouldDetonateAtTarget(double horizontal,
            double vertical, double speed) {
        double radius = Math.max(4.0D, speed * 1.35D);
        return horizontal * horizontal + vertical * vertical <= radius * radius;
    }

    public static double calculateDescentLimit(double launchRange, double remaining) {
        if (launchRange < 420.0D) return remaining < 260.0D ? 0.95D : 0.62D;
        if (remaining < 110.0D) return 0.66D;
        return 0.38D;
    }

    /** Nearby warhead blasts must not cascade through a strategic salvo. */
    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (isAirLaunched() && source != null && source.func_94541_c()) {
            return false;
        }
        return super.func_70097_a(source, amount);
    }

    private void syncAirLaunch() {
        field_70180_af.func_75692_b(DW_AIR_LAUNCHED, Byte.valueOf((byte) 1));
        field_70180_af.func_75692_b(DW_TARGET_X, Integer.valueOf(targetX));
        field_70180_af.func_75692_b(DW_TARGET_Y, Integer.valueOf(targetY));
        field_70180_af.func_75692_b(DW_TARGET_Z, Integer.valueOf(targetZ));
    }

    private void detonateAirLaunch() {
        if (field_70128_L) return;
        onImpact();
        func_70106_y();
    }

    private void updateAirRotation() {
        double horizontal = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        if (horizontal < 0.001D) return;
        field_70177_z = (float) Math.toDegrees(Math.atan2(-field_70159_w,
                field_70179_y));
        field_70125_A = calculateFlightPitch(field_70181_x, horizontal);
    }

    public static float calculateFlightPitch(double vertical,
            double horizontal) {
        return (float) -Math.toDegrees(Math.atan2(vertical,
                Math.max(0.001D, horizontal)));
    }

    private void spawnAirTrail() {
        if ((field_70173_aa & 1) != 0) return;
        field_70170_p.func_72869_a("smoke", field_70165_t, field_70163_u,
                field_70161_v, -field_70159_w * 0.04D,
                -field_70181_x * 0.04D, -field_70179_y * 0.04D);
    }

    private void normalizeMotion(double speed) {
        double actual = Math.sqrt(field_70159_w * field_70159_w
                + field_70181_x * field_70181_x + field_70179_y * field_70179_y);
        if (actual < 0.001D) return;
        double scale = speed / actual;
        field_70159_w *= scale;
        field_70181_x *= scale;
        field_70179_y *= scale;
    }

    private double motionLength() {
        return Math.sqrt(field_70159_w * field_70159_w
                + field_70181_x * field_70181_x
                + field_70179_y * field_70179_y);
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        super.func_70014_b(tag);
        tag.func_74757_a("WarTechAirLaunch", airLaunched);
        tag.func_74780_a("WarTechAirStartX", airStartX);
        tag.func_74780_a("WarTechAirStartZ", airStartZ);
        tag.func_74780_a("WarTechAirRange", airRange);
        tag.func_74780_a("WarTechRouteLateral", routeLateral);
        tag.func_74780_a("WarTechRouteWave", routeWave);
        tag.func_74780_a("WarTechRoutePhase", routePhase);
        tag.func_74778_a("WarTechOwnerTeam", ownerTeam);
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        super.func_70037_a(tag);
        airLaunched = tag.func_74767_n("WarTechAirLaunch");
        airStartX = tag.func_74769_h("WarTechAirStartX");
        airStartZ = tag.func_74769_h("WarTechAirStartZ");
        airRange = tag.func_74769_h("WarTechAirRange");
        routeLateral = tag.func_74769_h("WarTechRouteLateral");
        routeWave = tag.func_74769_h("WarTechRouteWave");
        routePhase = tag.func_74769_h("WarTechRoutePhase");
        ownerTeam = tag.func_74779_i("WarTechOwnerTeam");
        if (airLaunched) syncAirLaunch();
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        return new ItemStack(StrategicAviationContent.kh555Missile);
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_TIER2;
    }

    private double randomSigned(double minimum, double maximum) {
        double magnitude = minimum + field_70170_p.field_73012_v.nextDouble()
                * Math.max(0.0D, maximum - minimum);
        return field_70170_p.field_73012_v.nextBoolean() ? magnitude : -magnitude;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double blend(double current, double target, double amount) {
        return current + (target - current) * amount;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }
}
