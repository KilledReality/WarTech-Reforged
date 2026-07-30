package com.wartec.wartecmod.compat;

import api.hbm.energy.IEnergyUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

/** Powered fixed strategic radar. It ignores small UAV signatures by design. */
public final class TileEntityStrategicRadar extends TileEntity
        implements IInventory, IEnergyUser, IRadarGuiTarget, ITeamOwned {
    public static final int ENERGY_CAPACITY = 50000000;
    public static final int ENERGY_USE_PER_TICK = 2500;
    public static final int RADAR_RANGE = 6000;
    public static final int RADAR_CEILING = 4096;
    public static final int CONTACT_LIMIT = 64;
    private static final int WARMUP_TICKS = 200;
    private static final int BLIP_LIMIT = 16;

    private final int[] blips = new int[BLIP_LIMIT];
    private String ownerTeam = "";
    private long power;
    private boolean enabled = true;
    private boolean operational;
    private boolean structureFormed;
    private int warmup;
    private int contacts;
    private int blipCount;
    private ItemStack battery;

    @Override
    public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
        func_70296_d();
    }

    @Override
    public String getOwnerTeam() {
        return ownerTeam;
    }

    public void setStructureFormed(boolean formed) {
        structureFormed = formed;
        func_70296_d();
    }

    public boolean isStructureFormed() {
        return structureFormed;
    }

    public int getWarmupPercent() {
        return Math.min(100, warmup * 100 / WARMUP_TICKS);
    }

    public void setClientWarmupPercent(int percent) {
        warmup = Math.max(0, Math.min(WARMUP_TICKS,
                percent * WARMUP_TICKS / 100));
    }

    public int getRadarId() {
        int hash = field_145851_c * 73428767
                ^ field_145848_d * 19349663
                ^ field_145849_e * 912931;
        return 0x40000000 | hash & 0x3FFFFFFF;
    }

    @Override
    public void func_145845_h() {
        if (field_145850_b == null || field_145850_b.field_72995_K) return;
        HbmTilePowerLink.subscribeNearby(this, this,
                StrategicRadarStructure.BASE_RADIUS + 2,
                StrategicRadarStructure.HEIGHT + 2);
        int charged = VehicleEnergyHelper.chargeFromStack(
                battery, (int) power, ENERGY_CAPACITY);
        if (charged != power) {
            power = charged;
            func_70296_d();
        }

        boolean hasPower = enabled && structureFormed
                && power >= ENERGY_USE_PER_TICK;
        if (hasPower) {
            power -= ENERGY_USE_PER_TICK;
            if (warmup < WARMUP_TICKS) ++warmup;
        } else {
            warmup = Math.max(0, warmup - 4);
        }
        boolean nextOperational = hasPower && warmup >= WARMUP_TICKS;
        if (nextOperational && field_145850_b.func_82737_E() % 20L == 0L) {
            contacts = MissileTrackingService.updateStrategicRadarSweep(
                    field_145850_b, getRadarId(),
                    field_145851_c + 0.5D, field_145848_d + 12.0D,
                    field_145849_e + 0.5D,
                    RADAR_RANGE, RADAR_CEILING, CONTACT_LIMIT, ownerTeam,
                    ElectronicWarfareService.BAND_S);
            int[] next = MissileTrackingService.getRadarBlips(
                    field_145850_b, getRadarId(),
                    field_145851_c + 0.5D, field_145849_e + 0.5D,
                    BLIP_LIMIT);
            blipCount = next.length;
            for (int i = 0; i < BLIP_LIMIT; ++i) {
                blips[i] = i < next.length ? next[i] : 0;
            }
        } else if (!nextOperational && operational) {
            removeRadarContact();
        }
        if (operational != nextOperational) {
            operational = nextOperational;
            field_145850_b.func_147465_d(field_145851_c, field_145848_d,
                    field_145849_e, RadarNetworkContent.strategicRadar,
                    operational ? 1 : 0, 2);
            func_70296_d();
        }
    }

    public void shutdown() {
        removeRadarContact();
        operational = false;
        warmup = 0;
    }

    private void removeRadarContact() {
        if (field_145850_b != null && !field_145850_b.field_72995_K) {
            MissileTrackingService.removeRadar(field_145850_b, getRadarId());
        }
        contacts = 0;
        blipCount = 0;
        for (int i = 0; i < BLIP_LIMIT; ++i) blips[i] = 0;
    }

    public void setClientContacts(int value) {
        contacts = Math.max(0, value);
    }

    public void setClientFlags(int flags) {
        enabled = (flags & 1) != 0;
        operational = (flags & 2) != 0;
        structureFormed = (flags & 4) != 0;
    }

    public int getClientFlags() {
        return (enabled ? 1 : 0) | (operational ? 2 : 0)
                | (structureFormed ? 4 : 0);
    }

    public void setClientBlipCount(int count) {
        blipCount = Math.max(0, Math.min(BLIP_LIMIT, count));
    }

    public void setClientBlipHalf(int index, boolean high, int value) {
        if (index < 0 || index >= BLIP_LIMIT) return;
        if (high) blips[index] = blips[index] & 65535 | (value & 65535) << 16;
        else blips[index] = blips[index] & -65536 | value & 65535;
    }

    @Override
    public void func_145843_s() {
        shutdown();
        super.func_145843_s();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.func_72330_a(
                field_145851_c - 18.0D, field_145848_d - 1.0D,
                field_145849_e - 18.0D,
                field_145851_c + 19.0D, field_145848_d + 27.0D,
                field_145849_e + 19.0D);
    }

    @Override public double func_145833_n() { return 262144.0D; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long value) {
        power = Math.max(0L, Math.min(ENERGY_CAPACITY, value));
        func_70296_d();
    }
    @Override public long getMaxPower() { return ENERGY_CAPACITY; }
    @Override public boolean canConnect(ForgeDirection direction) {
        return direction != ForgeDirection.UNKNOWN;
    }
    @Override public boolean isLoaded() {
        return field_145850_b != null && field_145850_b.func_147438_o(
                field_145851_c, field_145848_d, field_145849_e) == this;
    }

    @Override
    public void func_145841_b(NBTTagCompound tag) {
        super.func_145841_b(tag);
        tag.func_74778_a("WarTechOwnerTeam", ownerTeam);
        tag.func_74772_a("WarTechStrategicRadarPower", power);
        tag.func_74757_a("WarTechStrategicRadarEnabled", enabled);
        tag.func_74757_a("WarTechStrategicRadarFormed", structureFormed);
        tag.func_74768_a("WarTechStrategicRadarWarmup", warmup);
        if (battery != null) {
            tag.func_74782_a("WarTechStrategicRadarBattery",
                    battery.func_77955_b(new NBTTagCompound()));
        }
    }

    @Override
    public void func_145839_a(NBTTagCompound tag) {
        super.func_145839_a(tag);
        ownerTeam = tag.func_74779_i("WarTechOwnerTeam");
        power = Math.max(0L, Math.min(ENERGY_CAPACITY,
                tag.func_74763_f("WarTechStrategicRadarPower")));
        enabled = !tag.func_74764_b("WarTechStrategicRadarEnabled")
                || tag.func_74767_n("WarTechStrategicRadarEnabled");
        structureFormed = tag.func_74767_n("WarTechStrategicRadarFormed");
        warmup = Math.max(0, Math.min(WARMUP_TICKS,
                tag.func_74762_e("WarTechStrategicRadarWarmup")));
        battery = tag.func_74764_b("WarTechStrategicRadarBattery")
                ? ItemStack.func_77949_a(
                        tag.func_74775_l("WarTechStrategicRadarBattery")) : null;
        operational = false;
        contacts = 0;
        blipCount = 0;
    }

    @Override public Entity wartecGetEntity() { return null; }
    @Override public int wartecGetPower() { return (int) power; }
    @Override public void wartecSetPower(int value) { setPower(value & 0xFFFFFFFFL); }
    @Override public int wartecGetCapacity() { return ENERGY_CAPACITY; }
    @Override public int wartecGetContacts() { return contacts; }
    @Override public int wartecGetRange() { return RADAR_RANGE; }
    @Override public int wartecGetCeiling() { return RADAR_CEILING; }
    @Override public int wartecGetBlipCount() { return blipCount; }
    @Override public int wartecGetPackedBlip(int index) {
        return index >= 0 && index < BLIP_LIMIT ? blips[index] : 0;
    }
    @Override public boolean wartecIsEnabled() { return enabled; }
    @Override public boolean wartecIsOperational() { return operational; }
    @Override public boolean wartecToggle(EntityPlayer player) {
        enabled = !enabled;
        if (!enabled) shutdown();
        func_70296_d();
        return true;
    }
    @Override public String wartecGetRadarName() {
        return "STRATEGIC EWR";
    }

    @Override public int func_70302_i_() { return 1; }
    @Override public ItemStack func_70301_a(int slot) {
        return slot == 0 ? battery : null;
    }
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
        if (slot != 0) return;
        battery = stack;
        if (battery != null && battery.field_77994_a > 1) {
            battery.field_77994_a = 1;
        }
        func_70296_d();
    }
    @Override public String func_145825_b() {
        return "container.wartecStrategicRadar";
    }
    @Override public boolean func_145818_k_() { return false; }
    @Override public int func_70297_j_() { return 1; }
    @Override public boolean func_70300_a(EntityPlayer player) {
        return field_145850_b != null
                && field_145850_b.func_147438_o(field_145851_c,
                        field_145848_d, field_145849_e) == this
                && player.func_70092_e(field_145851_c + 0.5D,
                        field_145848_d + 1.0D,
                        field_145849_e + 0.5D) <= 1024.0D;
    }
    @Override public void func_70295_k_() {}
    @Override public void func_70305_f() {}
    @Override public boolean func_94041_b(int slot, ItemStack stack) {
        return slot == 0 && VehicleEnergyHelper.isBattery(stack);
    }
}
