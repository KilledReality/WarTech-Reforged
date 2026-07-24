package com.wartec.wartecmod.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Powered communication mast with a persistent 3x3 chunk-loading sector. */
public final class TileEntityCommunicationRelay extends TileEntity
        implements IInventory {
    public static final int ENERGY_CAPACITY = 500000;
    public static final int LINK_RANGE = 2400;
    private static final int ENERGY_USE_PER_TICK = 20;
    private String ownerTeam = "";
    private int power;
    private boolean enabled = true;
    private boolean online;
    private int linkedRelays;
    private ItemStack battery;

    public long getRelayKey() {
        return MissileTrackingService.communicationRelayKey(
                field_145851_c, field_145848_d, field_145849_e);
    }

    public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
        func_70296_d();
    }

    public int getPower() { return power; }
    public boolean isEnabled() { return enabled; }
    public boolean isOnline() { return online; }
    public String getOwnerTeam() { return ownerTeam; }

    public void setPower(int value) {
        power = Math.max(0, Math.min(ENERGY_CAPACITY, value));
        func_70296_d();
    }

    public void setClientState(boolean nextEnabled, boolean nextOnline,
            int nextLinkedRelays) {
        enabled = nextEnabled;
        online = nextOnline;
        linkedRelays = Math.max(0, nextLinkedRelays);
    }

    public int chargeFromPlayer(EntityPlayer player) {
        int charged = VehicleEnergyHelper.chargeFromHeld(
                player, power, ENERGY_CAPACITY);
        if (charged != power) {
            power = charged;
            func_70296_d();
        }
        return power;
    }

    public void toggleEnabled() {
        enabled = !enabled;
        if (!enabled) shutdown();
        func_70296_d();
    }

    public int getLinkedRelayCount() {
        if (field_145850_b == null || field_145850_b.field_72995_K) {
            return linkedRelays;
        }
        if (!online) return 0;
        linkedRelays = MissileTrackingService.countLinkedCommunicationRelays(
                    field_145850_b, field_145851_c + 0.5D,
                    field_145848_d + 0.5D, field_145849_e + 0.5D,
                    ownerTeam);
        return linkedRelays;
    }

    @Override
    public void func_145845_h() {
        if (field_145850_b == null || field_145850_b.field_72995_K) return;
        int charged = VehicleEnergyHelper.chargeFromStack(
                battery, power, ENERGY_CAPACITY);
        if (charged != power) {
            power = charged;
            func_70296_d();
        }
        boolean nextOnline = enabled && power >= ENERGY_USE_PER_TICK;
        if (nextOnline) {
            power -= ENERGY_USE_PER_TICK;
            MissileChunkLoader.trackCommunicationNode(this);
            if (field_145850_b.func_82737_E() % 10L == 0L) {
                MissileTrackingService.updateCommunicationRelay(field_145850_b,
                        getRelayKey(), field_145851_c + 0.5D,
                        field_145848_d + 0.5D, field_145849_e + 0.5D,
                        ownerTeam);
                linkedRelays = MissileTrackingService
                        .countLinkedCommunicationRelays(field_145850_b,
                                field_145851_c + 0.5D,
                                field_145848_d + 0.5D,
                                field_145849_e + 0.5D, ownerTeam);
                double topY = field_145848_d + 7.15D;
                for (int signal = 0; signal < 8; ++signal) {
                    double angle = signal * Math.PI * 0.25D
                            + field_145850_b.func_82737_E() * 0.06D;
                    field_145850_b.func_72869_a("reddust",
                            field_145851_c + 0.5D + Math.cos(angle) * 0.85D,
                            topY, field_145849_e + 0.5D
                                    + Math.sin(angle) * 0.85D,
                            0.0D, 0.03D, 0.0D);
                }
            }
        } else if (online) {
            shutdown();
        }
        if (online != nextOnline) {
            online = nextOnline;
            func_70296_d();
        }
    }

    public void shutdown() {
        if (field_145850_b != null && !field_145850_b.field_72995_K) {
            MissileTrackingService.removeCommunicationRelay(
                    field_145850_b, getRelayKey());
            MissileChunkLoader.untrackCommunicationNode(this);
        }
        online = false;
    }

    @Override
    public void func_145843_s() {
        shutdown();
        super.func_145843_s();
    }

    @Override
    public void func_145841_b(NBTTagCompound tag) {
        super.func_145841_b(tag);
        tag.func_74778_a("WarTechOwnerTeam", ownerTeam);
        tag.func_74768_a("WarTechRelayPower", power);
        tag.func_74757_a("WarTechRelayEnabled", enabled);
        if (battery != null) {
            tag.func_74782_a("WarTechRelayBattery",
                    battery.func_77955_b(new NBTTagCompound()));
        }
    }

    @Override
    public void func_145839_a(NBTTagCompound tag) {
        super.func_145839_a(tag);
        ownerTeam = tag.func_74779_i("WarTechOwnerTeam");
        power = Math.max(0, Math.min(ENERGY_CAPACITY,
                tag.func_74762_e("WarTechRelayPower")));
        enabled = !tag.func_74764_b("WarTechRelayEnabled")
                || tag.func_74767_n("WarTechRelayEnabled");
        battery = tag.func_74764_b("WarTechRelayBattery")
                ? ItemStack.func_77949_a(
                        tag.func_74775_l("WarTechRelayBattery")) : null;
        online = false;
        linkedRelays = 0;
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
        return "container.wartecCommunicationMast";
    }
    @Override public boolean func_145818_k_() { return false; }
    @Override public int func_70297_j_() { return 1; }
    @Override public boolean func_70300_a(EntityPlayer player) {
        return field_145850_b != null
                && field_145850_b.func_147438_o(
                        field_145851_c, field_145848_d, field_145849_e) == this
                && player.func_70092_e(field_145851_c + 0.5D,
                        field_145848_d + 0.5D,
                        field_145849_e + 0.5D) <= 256.0D;
    }
    @Override public void func_70295_k_() {}
    @Override public void func_70305_f() {}
    @Override public boolean func_94041_b(int slot, ItemStack stack) {
        return slot == 0 && VehicleEnergyHelper.isBattery(stack);
    }
}
