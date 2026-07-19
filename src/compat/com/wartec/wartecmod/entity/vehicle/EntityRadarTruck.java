package com.wartec.wartecmod.entity.vehicle;

import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.IAntiRadiationTarget;
import com.wartec.wartecmod.compat.IRadarGuiTarget;
import com.wartec.wartecmod.compat.RadarGuiHandler;
import com.wartec.wartecmod.compat.VehicleEnergyHelper;
import com.wartec.wartecmod.compat.WarTecBootstrap;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class EntityRadarTruck extends Entity
        implements IAntiRadiationTarget, IRadarGuiTarget {
    public static final double RADAR_RANGE = 600.0D;
    public static final double RADAR_CEILING = 500.0D;
    public static final int ENERGY_CAPACITY = 1000000;
    private static final int ENERGY_USE = 100;
    private static final int DW_ACTIVE = 18;
    private static final int DW_CONTACTS = 19;
    private static final int DW_POWER = 20;
    private static final int DW_BLIP_COUNT = 21;
    private static final int DW_BLIP_BASE = 22;
    private static final int BLIP_LIMIT = 8;
    private static final double MAX_HEALTH = 300.0D;
    private double radarHealth = MAX_HEALTH;
    private String ownerTeam = "";
    private ItemStack battery;

    public EntityRadarTruck(World world) {
        super(world);
        field_70156_m = true;
        func_70105_a(4.2F, 3.0F);
        updateBounds();
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_ACTIVE, Byte.valueOf((byte) 1));
        field_70180_af.func_75682_a(DW_CONTACTS, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_POWER, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_BLIP_COUNT, Integer.valueOf(0));
        for (int i = 0; i < BLIP_LIMIT; ++i) {
            field_70180_af.func_75682_a(DW_BLIP_BASE + i, Integer.valueOf(0));
        }
    }

    public boolean isRadarActive() {
        return field_70180_af.func_75683_a(DW_ACTIVE) != 0;
    }

    public int getRadarContacts() {
        return field_70180_af.func_75679_c(DW_CONTACTS);
    }

    public int getPower() {
        return field_70180_af.func_75679_c(DW_POWER);
    }

    public boolean isRadarOperational() {
        return isRadarActive() && getPower() >= ENERGY_USE;
    }

    public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    @Override
    public void func_70071_h_() {
        super.func_70071_h_();
        field_70159_w = 0.0D;
        field_70181_x = 0.0D;
        field_70179_y = 0.0D;
        updateBounds();
        if (field_70170_p.field_72995_K) {
            return;
        }
        int charged = VehicleEnergyHelper.chargeFromStack(battery,
                getPower(), ENERGY_CAPACITY);
        if (charged != getPower()) {
            field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(charged));
        }
        if (!isRadarOperational()) {
            MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
            if (getRadarContacts() != 0) {
                field_70180_af.func_75692_b(DW_CONTACTS, Integer.valueOf(0));
            }
            clearRadarBlips();
            return;
        }
        field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(getPower() - ENERGY_USE));
        if (field_70173_aa % 10 == Math.abs(func_145782_y()) % 10) {
            int contacts = MissileTrackingService.updateRadarSweep(field_70170_p,
                    func_145782_y(), field_70165_t, field_70163_u + 2.5D, field_70161_v,
                    RADAR_RANGE, RADAR_CEILING, Integer.MAX_VALUE, ownerTeam,
                    com.wartec.wartecmod.compat.ElectronicWarfareService.BAND_S);
            field_70180_af.func_75692_b(DW_CONTACTS, Integer.valueOf(contacts));
            updateRadarBlips();
        }
    }

    private void updateRadarBlips() {
        int[] blips = MissileTrackingService.getRadarBlips(field_70170_p,
                func_145782_y(), field_70165_t, field_70161_v, BLIP_LIMIT);
        field_70180_af.func_75692_b(DW_BLIP_COUNT, Integer.valueOf(blips.length));
        for (int i = 0; i < BLIP_LIMIT; ++i) {
            field_70180_af.func_75692_b(DW_BLIP_BASE + i,
                    Integer.valueOf(i < blips.length ? blips[i] : 0));
        }
    }

    private void clearRadarBlips() {
        if (field_70180_af.func_75679_c(DW_BLIP_COUNT) != 0) {
            field_70180_af.func_75692_b(DW_BLIP_COUNT, Integer.valueOf(0));
        }
    }

    @Override
    public void func_70107_b(double x, double y, double z) {
        super.func_70107_b(x, y, z);
        updateBounds();
    }

    private void updateBounds() {
        double halfWidth = 2.1D;
        field_70121_D.func_72324_b(field_70165_t - halfWidth, field_70163_u,
                field_70161_v - halfWidth, field_70165_t + halfWidth,
                field_70163_u + 3.0D, field_70161_v + halfWidth);
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
    public float func_70111_Y() {
        return 0.6F;
    }

    @Override
    public boolean func_70067_L() {
        return !field_70128_L;
    }

    @Override
    public boolean func_130002_c(EntityPlayer player) {
        if (field_70170_p.field_72995_K) {
            return true;
        }
        ItemStack held = player.func_71045_bC();
        if (VehicleEnergyHelper.isBattery(held)) {
            int power = VehicleEnergyHelper.chargeFromHeld(player,
                    getPower(), ENERGY_CAPACITY);
            field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(power));
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.7F, 1.0F);
            return true;
        }
        if (player.func_70093_af()) {
            wartecToggle(player);
            return true;
        }
        FMLNetworkHandler.openGui(player, WarTecBootstrap.instance,
                RadarGuiHandler.GUI_ID, field_70170_p, func_145782_y(), 0, 0);
        return true;
    }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L || amount <= 0.0F) {
            return true;
        }
        if (source.func_76346_g() instanceof EntityPlayer
                && ((EntityPlayer) source.func_76346_g()).field_71075_bZ.field_75098_d) {
            destroyRadar();
            return true;
        }
        radarHealth -= amount;
        if (radarHealth <= 0.0D) {
            destroyRadar();
        } else {
            field_70170_p.func_72956_a(this, "random.anvil_land", 0.35F, 1.5F);
        }
        return true;
    }

    private void destroyRadar() {
        MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
        dropBattery();
        func_70106_y();
        field_70170_p.func_72885_a(this, field_70165_t, field_70163_u + 1.5D,
                field_70161_v, 3.5F, true, true);
    }

    @Override
    public void wartecDestroyByAntiRadiationMissile() {
        if (!field_70128_L && !field_70170_p.field_72995_K) {
            MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
            dropBattery();
            func_70106_y();
        }
    }

    @Override
    public void func_70106_y() {
        if (field_70170_p != null && !field_70170_p.field_72995_K) {
            MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
        }
        super.func_70106_y();
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        tag.func_74757_a("RadarActive", isRadarActive());
        tag.func_74780_a("RadarHealth", radarHealth);
        tag.func_74778_a("RadarTeam", ownerTeam);
        tag.func_74768_a("RadarPower", getPower());
        if (battery != null) {
            tag.func_74782_a("RadarBattery", battery.func_77955_b(new NBTTagCompound()));
        }
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        boolean active = !tag.func_74764_b("RadarActive") || tag.func_74767_n("RadarActive");
        field_70180_af.func_75692_b(DW_ACTIVE, Byte.valueOf((byte) (active ? 1 : 0)));
        if (tag.func_74764_b("RadarHealth")) {
            radarHealth = Math.max(1.0D, Math.min(MAX_HEALTH, tag.func_74769_h("RadarHealth")));
        }
        ownerTeam = tag.func_74779_i("RadarTeam");
        if (tag.func_74764_b("RadarPower")) {
            field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(Math.max(0,
                    Math.min(ENERGY_CAPACITY, tag.func_74762_e("RadarPower")))));
        }
        battery = tag.func_74764_b("RadarBattery")
                ? ItemStack.func_77949_a(tag.func_74775_l("RadarBattery")) : null;
    }

    private void dropBattery() {
        if (battery != null) {
            func_70099_a(battery, 0.5F);
            battery = null;
        }
    }

    @Override public Entity wartecGetEntity() { return this; }
    @Override public int wartecGetPower() { return getPower(); }
    @Override public void wartecSetPower(int power) {
        field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(Math.max(0,
                Math.min(ENERGY_CAPACITY, power))));
    }
    @Override public int wartecGetCapacity() { return ENERGY_CAPACITY; }
    @Override public int wartecGetContacts() { return getRadarContacts(); }
    @Override public int wartecGetRange() { return (int) RADAR_RANGE; }
    @Override public int wartecGetCeiling() { return (int) RADAR_CEILING; }
    @Override public int wartecGetBlipCount() {
        return Math.max(0, Math.min(BLIP_LIMIT,
                field_70180_af.func_75679_c(DW_BLIP_COUNT)));
    }
    @Override public int wartecGetPackedBlip(int index) {
        return index >= 0 && index < BLIP_LIMIT
                ? field_70180_af.func_75679_c(DW_BLIP_BASE + index) : 0;
    }
    @Override public boolean wartecIsEnabled() { return isRadarActive(); }
    @Override public boolean wartecIsOperational() { return isRadarOperational(); }
    @Override public boolean wartecToggle(EntityPlayer player) {
        boolean active = !isRadarActive();
        field_70180_af.func_75692_b(DW_ACTIVE, Byte.valueOf((byte) (active ? 1 : 0)));
        if (!active) {
            MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
            field_70180_af.func_75692_b(DW_CONTACTS, Integer.valueOf(0));
        }
        field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.8F,
                active ? 1.25F : 0.75F);
        return true;
    }
    @Override public String wartecGetRadarName() { return "TRM RADAR"; }

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
    @Override public String func_145825_b() { return "container.wartecRadar"; }
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
}
