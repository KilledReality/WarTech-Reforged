package com.wartec.wartecmod.compat;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerRadarVehicle extends Container {
    private final IRadarGuiTarget radar;
    private int lastPower = -1;

    public ContainerRadarVehicle(InventoryPlayer playerInventory, IRadarGuiTarget radar) {
        this.radar = radar;
        func_75146_a(new BatterySlot(radar, 0, 222, 105));
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                func_75146_a(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 140 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            func_75146_a(new Slot(playerInventory, column,
                    8 + column * 18, 198));
        }
    }

    public IRadarGuiTarget getRadar() {
        return radar;
    }

    @Override
    public void func_75132_a(ICrafting crafter) {
        super.func_75132_a(crafter);
        sendPower(crafter, radar.wartecGetPower());
    }

    @Override
    public void func_75142_b() {
        super.func_75142_b();
        int power = radar.wartecGetPower();
        if (power != lastPower) {
            for (Object value : (List) field_75149_d) {
                sendPower((ICrafting) value, power);
            }
            lastPower = power;
        }
    }

    private void sendPower(ICrafting crafter, int power) {
        crafter.func_71112_a(this, 0, power & 65535);
        crafter.func_71112_a(this, 1, power >>> 16);
    }

    @Override
    public void func_75137_b(int id, int value) {
        int power = radar.wartecGetPower();
        if (id == 0) {
            radar.wartecSetPower((power & -65536) | (value & 65535));
        } else if (id == 1) {
            radar.wartecSetPower((power & 65535) | ((value & 65535) << 16));
        }
    }

    @Override
    public boolean func_75145_c(EntityPlayer player) {
        Entity entity = radar.wartecGetEntity();
        return entity != null && !entity.field_70128_L
                && player.func_70092_e(entity.field_70165_t,
                        entity.field_70163_u + 1.0D, entity.field_70161_v) <= 256.0D;
    }

    @Override
    public boolean func_75140_a(EntityPlayer player, int action) {
        return action == 0 && radar.wartecToggle(player);
    }

    @Override
    public ItemStack func_82846_b(EntityPlayer player, int index) {
        if (index < 0 || index >= field_75151_b.size()) {
            return null;
        }
        Slot slot = (Slot) field_75151_b.get(index);
        if (slot == null || !slot.func_75216_d()) {
            return null;
        }
        ItemStack stack = slot.func_75211_c();
        ItemStack original = stack.func_77946_l();
        if (index == 0) {
            if (!func_75135_a(stack, 1, 37, true)) return null;
        } else {
            if (!radar.func_94041_b(0, stack)
                    || !func_75135_a(stack, 0, 1, false)) return null;
        }
        if (stack.field_77994_a <= 0) slot.func_75215_d(null);
        else slot.func_75218_e();
        slot.func_75220_a(stack, original);
        return original;
    }

    private static final class BatterySlot extends Slot {
        BatterySlot(IRadarGuiTarget inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Override
        public boolean func_75214_a(ItemStack stack) {
            return VehicleEnergyHelper.isBattery(stack);
        }
    }
}
