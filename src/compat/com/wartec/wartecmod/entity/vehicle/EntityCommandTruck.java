package com.wartec.wartecmod.entity.vehicle;

import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.MissileTrackingService.CommandSnapshot;
import com.wartec.wartecmod.compat.VehicleEnergyHelper;
import com.wartec.wartecmod.compat.ElectronicWarfareService;
import com.wartec.wartecmod.compat.IAntiRadiationTarget;
import com.wartec.wartecmod.compat.RadarGuiHandler;
import com.wartec.wartecmod.compat.WarTecBootstrap;
import com.wartec.wartecmod.compat.HeavyVehicleDynamics;
import com.wartec.wartecmod.compat.HeavyVehicleDynamics.Motion;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class EntityCommandTruck extends Entity
        implements IAntiRadiationTarget, IInventory {
    public static final int ENERGY_CAPACITY = 2000000;
    private static final int ENERGY_USE = 40;
    private static final int DW_DEPLOYED = 18;
    private static final int DW_POWER = 19;
    private static final int DW_RADARS = 20;
    private static final int DW_LAUNCHERS = 21;
    private static final int DW_CONTACTS = 22;
    private static final int DW_ASSIGNED = 23;
    private static final int DW_EMITTERS = 24;
    private static final int DW_JAMMERS = 25;
    private static final double MAX_HEALTH = 450.0D;
    private double vehicleHealth = MAX_HEALTH;
    private double driveSpeed;
    private double steeringState;
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;
    private String ownerTeam = "";
    private ItemStack battery;

    public EntityCommandTruck(World world) {
        super(world);
        field_70156_m = true;
        field_70138_W = 1.1F;
        func_70105_a(2.7F, 2.5F);
        updateBounds();
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_DEPLOYED, Byte.valueOf((byte) 0));
        field_70180_af.func_75682_a(DW_POWER, Integer.valueOf(500000));
        field_70180_af.func_75682_a(DW_RADARS, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_LAUNCHERS, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_CONTACTS, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_ASSIGNED, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_EMITTERS, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_JAMMERS, Integer.valueOf(0));
    }

    public boolean isDeployed() {
        return field_70180_af.func_75683_a(DW_DEPLOYED) != 0;
    }

    public boolean isNetworkActive() {
        return isDeployed() && getPower() >= ENERGY_USE;
    }

    public int getPower() {
        return field_70180_af.func_75679_c(DW_POWER);
    }

    public int getLinkedRadars() {
        return field_70180_af.func_75679_c(DW_RADARS);
    }

    public int getLinkedLaunchers() {
        return field_70180_af.func_75679_c(DW_LAUNCHERS);
    }

    public int getContacts() {
        return field_70180_af.func_75679_c(DW_CONTACTS);
    }

    public int getAssignedTargets() {
        return field_70180_af.func_75679_c(DW_ASSIGNED);
    }

    public int getHostileEmitters() {
        return field_70180_af.func_75679_c(DW_EMITTERS);
    }

    public int getActiveJammers() {
        return field_70180_af.func_75679_c(DW_JAMMERS);
    }

    public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    @Override
    public void func_70071_h_() {
        super.func_70071_h_();
        updateBounds();
        if (field_70170_p.field_72995_K) {
            updateClientInterpolation();
            return;
        }
        int charged = VehicleEnergyHelper.chargeFromStack(battery,
                getPower(), ENERGY_CAPACITY);
        if (charged != getPower()) setPower(charged);
        if (isDeployed()) {
            driveSpeed = 0.0D;
            field_70159_w = 0.0D;
            field_70179_y = 0.0D;
            int power = getPower();
            if (power >= ENERGY_USE) {
                field_70180_af.func_75692_b(DW_POWER,
                        Integer.valueOf(power - ENERGY_USE));
                ElectronicWarfareService.updateEmitter(field_70170_p, func_145782_y(),
                        field_70165_t, field_70163_u + 2.0D, field_70161_v,
                        ElectronicWarfareService.EMITTER_COMMAND,
                        ElectronicWarfareService.BAND_X, ownerTeam);
                if (field_70173_aa % 10 == Math.abs(func_145782_y()) % 10) {
                    applySnapshot(MissileTrackingService.updateCommandPost(field_70170_p,
                            func_145782_y(), field_70165_t, field_70163_u + 2.0D,
                            field_70161_v, ownerTeam));
                }
            } else {
                disconnectNetwork();
            }
        } else {
            disconnectNetwork();
            updateDriving();
        }
        if (!field_70122_E) {
            field_70181_x -= 0.08D;
        } else if (field_70181_x < 0.0D) {
            field_70181_x = 0.0D;
        }
        if (field_70123_F && field_70122_E && Math.abs(driveSpeed) > 0.04D) {
            field_70181_x = Math.max(field_70181_x, 0.20D);
        }
        double oldY = field_70163_u;
        func_70091_d(field_70159_w, field_70181_x, field_70179_y);
        field_70125_A = HeavyVehicleDynamics.suspensionPitch(
                field_70125_A, field_70163_u - oldY);
        updateBounds();
        field_70159_w *= 0.72D;
        field_70179_y *= 0.72D;
        field_70181_x *= 0.98D;
    }

    private void applySnapshot(CommandSnapshot snapshot) {
        setWatcher(DW_RADARS, snapshot.linkedRadars);
        setWatcher(DW_LAUNCHERS, snapshot.linkedLaunchers);
        setWatcher(DW_CONTACTS, snapshot.contacts);
        setWatcher(DW_ASSIGNED, snapshot.assignedTargets);
        setWatcher(DW_EMITTERS, snapshot.hostileEmitters);
        setWatcher(DW_JAMMERS, snapshot.activeJammers);
    }

    private void disconnectNetwork() {
        MissileTrackingService.removeCommandPost(field_70170_p, func_145782_y());
        ElectronicWarfareService.removeNode(field_70170_p, func_145782_y());
        setWatcher(DW_RADARS, 0);
        setWatcher(DW_LAUNCHERS, 0);
        setWatcher(DW_CONTACTS, 0);
        setWatcher(DW_ASSIGNED, 0);
        setWatcher(DW_EMITTERS, 0);
        setWatcher(DW_JAMMERS, 0);
    }

    private void setWatcher(int id, int value) {
        if (field_70180_af.func_75679_c(id) != value) {
            field_70180_af.func_75692_b(id, Integer.valueOf(value));
        }
    }

    private void updateDriving() {
        float forward = 0.0F;
        float steering = 0.0F;
        if (field_70153_n instanceof EntityPlayer) {
            EntityPlayer driver = (EntityPlayer) field_70153_n;
            forward = driver.field_70701_bs;
            steering = driver.field_70702_br;
        }
        Motion motion = HeavyVehicleDynamics.step(driveSpeed, steeringState,
                field_70177_z, forward, steering, 0.36D, 0.17D,
                field_70122_E, field_70123_F);
        driveSpeed = motion.speed;
        steeringState = motion.steering;
        field_70177_z = motion.yaw;
        field_70159_w = motion.motionX;
        field_70179_y = motion.motionZ;
        if (field_70153_n != null && Math.abs(driveSpeed) > 0.04D
                && field_70173_aa % 24 == 0) {
            field_70170_p.func_72956_a(this, "minecart.base", 0.28F,
                    (float) (0.82D + Math.min(0.35D, Math.abs(driveSpeed))));
        }
    }

    private void updateClientInterpolation() {
        if (clientInterpolationTicks <= 0) {
            return;
        }
        double fraction = 1.0D / clientInterpolationTicks;
        double x = field_70165_t + (clientTargetX - field_70165_t) * fraction;
        double y = field_70163_u + (clientTargetY - field_70163_u) * fraction;
        double z = field_70161_v + (clientTargetZ - field_70161_v) * fraction;
        double yawDelta = clientTargetYaw - field_70177_z;
        while (yawDelta < -180.0D) yawDelta += 360.0D;
        while (yawDelta >= 180.0D) yawDelta -= 360.0D;
        field_70177_z = (float) (field_70177_z + yawDelta * fraction);
        field_70125_A = (float) (field_70125_A
                + (clientTargetPitch - field_70125_A) * fraction);
        func_70107_b(x, y, z);
        clientInterpolationTicks--;
    }

    @Override
    public void func_70056_a(double x, double y, double z,
            float yaw, float pitch, int increments) {
        if (!field_70170_p.field_72995_K) {
            func_70012_b(x, y, z, yaw, pitch);
            return;
        }
        clientTargetX = x;
        clientTargetY = y;
        clientTargetZ = z;
        clientTargetYaw = yaw;
        clientTargetPitch = pitch;
        clientInterpolationTicks = Math.max(3, increments);
    }

    @Override
    public boolean func_130002_c(EntityPlayer player) {
        ItemStack held = player.func_71045_bC();
        if (field_70170_p.field_72995_K) {
            return true;
        }
        if (VehicleEnergyHelper.isBattery(held)) {
            int power = VehicleEnergyHelper.chargeFromHeld(player,
                    getPower(), ENERGY_CAPACITY);
            field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(power));
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.7F, 1.05F);
            tell(player, "Command post power: " + power + "/" + ENERGY_CAPACITY + " HE");
            return true;
        }
        if (player.func_70093_af()) {
            boolean deployed = !isDeployed();
            field_70180_af.func_75692_b(DW_DEPLOYED,
                    Byte.valueOf((byte) (deployed ? 1 : 0)));
            driveSpeed = 0.0D;
            if (!deployed) {
                disconnectNetwork();
            }
            field_70170_p.func_72956_a(this, "random.anvil_use", 0.75F,
                    deployed ? 0.78F : 1.12F);
            tell(player, deployed ? "Command post deployed." : "Command post retracted.");
            return true;
        }
        if (isDeployed()) {
            FMLNetworkHandler.openGui(player, WarTecBootstrap.instance,
                    RadarGuiHandler.GUI_ID_COMMAND, field_70170_p,
                    func_145782_y(), 0, 0);
            return true;
        }
        if (field_70153_n == null || field_70153_n == player) {
            player.func_70078_a(this);
            return true;
        }
        tell(player, "The driver's seat is occupied.");
        return true;
    }

    @Override
    public void func_70043_V() {
        if (field_70153_n != null) {
            double yaw = Math.toRadians(field_70177_z);
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double rightX = Math.cos(yaw);
            double rightZ = Math.sin(yaw);
            field_70153_n.func_70107_b(
                    field_70165_t + forwardX * 2.15D - rightX * 0.48D,
                    field_70163_u + 0.98D + field_70153_n.func_70033_W(),
                    field_70161_v + forwardZ * 2.15D - rightZ * 0.48D);
        }
    }

    @Override
    public double func_70042_X() {
        return 1.3D;
    }

    @Override
    public void func_70107_b(double x, double y, double z) {
        super.func_70107_b(x, y, z);
        updateBounds();
    }

    private void updateBounds() {
        double halfWidth = 1.3D;
        field_70121_D.func_72324_b(field_70165_t - halfWidth, field_70163_u,
                field_70161_v - halfWidth, field_70165_t + halfWidth,
                field_70163_u + 2.5D, field_70161_v + halfWidth);
    }

    @Override
    public AxisAlignedBB func_70046_E() {
        return field_70121_D;
    }

    @Override
    public AxisAlignedBB func_70114_g(Entity entity) {
        return entity.field_70121_D;
    }

    @Override
    public boolean func_70104_M() {
        return true;
    }

    @Override
    public boolean func_70067_L() {
        return !field_70128_L;
    }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L || amount <= 0.0F) {
            return true;
        }
        if (source.func_76346_g() instanceof EntityPlayer
                && ((EntityPlayer) source.func_76346_g()).field_71075_bZ.field_75098_d) {
            destroyVehicle();
            return true;
        }
        vehicleHealth -= amount;
        if (vehicleHealth <= 0.0D) {
            destroyVehicle();
        }
        return true;
    }

    private void destroyVehicle() {
        disconnectNetwork();
        dropBattery();
        func_70106_y();
        field_70170_p.func_72885_a(this, field_70165_t, field_70163_u + 1.0D,
                field_70161_v, 4.5F, true, true);
    }

    @Override
    public void wartecDestroyByAntiRadiationMissile() {
        if (!field_70128_L && !field_70170_p.field_72995_K) {
            disconnectNetwork();
            dropBattery();
            func_70106_y();
        }
    }

    @Override
    public void func_70106_y() {
        if (field_70170_p != null && !field_70170_p.field_72995_K) {
            MissileTrackingService.removeCommandPost(field_70170_p, func_145782_y());
            ElectronicWarfareService.removeNode(field_70170_p, func_145782_y());
        }
        super.func_70106_y();
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        tag.func_74757_a("Deployed", isDeployed());
        tag.func_74768_a("Power", getPower());
        tag.func_74780_a("VehicleHealth", vehicleHealth);
        tag.func_74778_a("CommandTeam", ownerTeam);
        if (battery != null) {
            tag.func_74782_a("CommandBattery", battery.func_77955_b(new NBTTagCompound()));
        }
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        field_70180_af.func_75692_b(DW_DEPLOYED,
                Byte.valueOf((byte) (tag.func_74767_n("Deployed") ? 1 : 0)));
        field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(Math.max(0,
                Math.min(ENERGY_CAPACITY, tag.func_74762_e("Power")))));
        if (tag.func_74764_b("VehicleHealth")) {
            vehicleHealth = Math.max(1.0D,
                    Math.min(MAX_HEALTH, tag.func_74769_h("VehicleHealth")));
        }
        ownerTeam = tag.func_74779_i("CommandTeam");
        battery = tag.func_74764_b("CommandBattery")
                ? ItemStack.func_77949_a(tag.func_74775_l("CommandBattery")) : null;
    }

    public void setPower(int power) {
        field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(Math.max(0,
                Math.min(ENERGY_CAPACITY, power))));
    }

    private void dropBattery() {
        if (battery != null) {
            func_70099_a(battery, 0.5F);
            battery = null;
        }
    }

    @Override public int func_70302_i_() { return 1; }
    @Override public ItemStack func_70301_a(int slot) { return slot == 0 ? battery : null; }
    @Override public ItemStack func_70298_a(int slot, int amount) {
        if (slot != 0 || battery == null) return null;
        if (battery.field_77994_a <= amount) {
            ItemStack result = battery;
            battery = null;
            func_70296_d();
            return result;
        }
        ItemStack result = battery.func_77979_a(amount);
        func_70296_d();
        return result;
    }
    @Override public ItemStack func_70304_b(int slot) {
        if (slot != 0) return null;
        ItemStack result = battery;
        battery = null;
        func_70296_d();
        return result;
    }
    @Override public void func_70299_a(int slot, ItemStack stack) {
        if (slot == 0) {
            battery = stack;
            if (battery != null && battery.field_77994_a > 1) battery.field_77994_a = 1;
            func_70296_d();
        }
    }
    @Override public String func_145825_b() { return "container.wartecCommand"; }
    @Override public boolean func_145818_k_() { return false; }
    @Override public int func_70297_j_() { return 1; }
    @Override public void func_70296_d() {}
    @Override public boolean func_70300_a(EntityPlayer player) {
        return !field_70128_L && player.func_70092_e(field_70165_t,
                field_70163_u, field_70161_v) <= 256.0D;
    }
    @Override public void func_70295_k_() {}
    @Override public void func_70305_f() {}
    @Override public boolean func_94041_b(int slot, ItemStack stack) {
        return slot == 0 && VehicleEnergyHelper.isBattery(stack);
    }

    private static void tell(EntityPlayer player, String text) {
        player.func_145747_a(new ChatComponentText(text));
    }
}
