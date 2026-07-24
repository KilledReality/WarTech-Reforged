package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerTu95Bomber extends Container {
    private final EntityTu95Bomber bomber;
    private int lastPower = -1;

    public ContainerTu95Bomber(InventoryPlayer inventory, EntityTu95Bomber bomber) {
        this.bomber = bomber;
        for (int slot = 0; slot < 6; ++slot) {
            func_75146_a(new MissileSlot(bomber, slot, 44 + slot * 21, 57));
        }
        func_75146_a(new FlaresSlot(bomber, EntityTu95Bomber.FLARE_SLOT, 178, 57));
        func_75146_a(new BatterySlot(bomber, EntityTu95Bomber.BATTERY_SLOT, 205, 57));
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                func_75146_a(new Slot(inventory, column + row * 9 + 9,
                        24 + column * 18, 139 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            func_75146_a(new Slot(inventory, column, 24 + column * 18, 197));
        }
    }

    public EntityTu95Bomber getBomber() { return bomber; }

    @Override public void func_75132_a(ICrafting crafter) {
        super.func_75132_a(crafter);
        sendPower(crafter, bomber.getPower());
    }
    @Override public void func_75142_b() {
        super.func_75142_b();
        int power = bomber.getPower();
        if (power != lastPower) {
            for (Object value : (List) field_75149_d) sendPower((ICrafting) value, power);
            lastPower = power;
        }
    }
    private void sendPower(ICrafting crafter, int power) {
        crafter.func_71112_a(this, 0, power & 65535);
        crafter.func_71112_a(this, 1, power >>> 16);
    }
    @Override public void func_75137_b(int id, int value) {
        int power = bomber.getPower();
        if (id == 0) bomber.setPower((power & -65536) | value & 65535);
        else if (id == 1) bomber.setPower((power & 65535) | (value & 65535) << 16);
    }
    @Override public boolean func_75145_c(EntityPlayer player) {
        return bomber.func_70300_a(player);
    }
    @Override public boolean func_75140_a(EntityPlayer player, int action) {
        return bomber.handleGuiAction(action, player);
    }

    @Override
    public ItemStack func_82846_b(EntityPlayer player, int index) {
        if (index < 0 || index >= field_75151_b.size()) return null;
        Slot slot = (Slot) field_75151_b.get(index);
        if (slot == null || !slot.func_75216_d()) return null;
        ItemStack stack = slot.func_75211_c();
        ItemStack original = stack.func_77946_l();
        if (index < 8) {
            if (!func_75135_a(stack, 8, 44, true)) return null;
        } else if (VehicleEnergyHelper.isBattery(stack)) {
            if (!func_75135_a(stack, 6, 7, false)) return null;
        } else if (DroneStrikeContent.isFlares(stack)) {
            if (!func_75135_a(stack, 7, 8, false)) return null;
        } else if (StrategicAviationContent.isStrategicWeapon(stack)) {
            boolean moved = false;
            for (int target = 0; target < 6; ++target) {
                if (func_75135_a(stack, target, target + 1, false)) {
                    moved = true;
                    break;
                }
            }
            if (!moved) return null;
        } else return null;
        if (stack.field_77994_a <= 0) slot.func_75215_d(null);
        else slot.func_75218_e();
        return original;
    }

    private static final class MissileSlot extends Slot {
        MissileSlot(EntityTu95Bomber bomber, int slot, int x, int y) {
            super(bomber, slot, x, y);
        }
        @Override public boolean func_75214_a(ItemStack stack) {
            return StrategicAviationContent.isStrategicWeapon(stack);
        }
        @Override public int func_75219_a() { return 1; }
    }
    private static final class BatterySlot extends Slot {
        BatterySlot(EntityTu95Bomber bomber, int slot, int x, int y) {
            super(bomber, slot, x, y);
        }
        @Override public boolean func_75214_a(ItemStack stack) {
            return VehicleEnergyHelper.isBattery(stack);
        }
        @Override public int func_75219_a() { return 1; }
    }
    private static final class FlaresSlot extends Slot {
        FlaresSlot(EntityTu95Bomber bomber, int slot, int x, int y) {
            super(bomber, slot, x, y);
        }
        @Override public boolean func_75214_a(ItemStack stack) {
            return DroneStrikeContent.isFlares(stack);
        }
        @Override public int func_75219_a() { return 16; }
    }
}
