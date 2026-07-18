package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerMobileAirDefense extends Container {
    private final EntityMobileAirDefense system;
    private int lastPower = -1;

    public ContainerMobileAirDefense(InventoryPlayer playerInventory,
            EntityMobileAirDefense system) {
        this.system = system;
        for (int row = 0; row < 2; ++row) {
            for (int column = 0; column < 6; ++column) {
                int slot = column + row * 6;
                func_75146_a(new MissileSlot(system, slot,
                        151 + column * 18, 38 + row * 18));
            }
        }
        func_75146_a(new BatterySlot(system, EntityMobileAirDefense.BATTERY_SLOT,
                244, 105));
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                func_75146_a(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 146 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            func_75146_a(new Slot(playerInventory, column,
                    8 + column * 18, 204));
        }
    }

    public EntityMobileAirDefense getSystem() {
        return system;
    }

    @Override
    public void func_75132_a(ICrafting crafter) {
        super.func_75132_a(crafter);
        sendPower(crafter, system.getPower());
    }

    @Override
    public void func_75142_b() {
        super.func_75142_b();
        int power = system.getPower();
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
        int power = system.getPower();
        if (id == 0) {
            system.setPower((power & -65536) | (value & 65535));
        } else if (id == 1) {
            system.setPower((power & 65535) | ((value & 65535) << 16));
        }
    }

    @Override
    public boolean func_75145_c(EntityPlayer player) {
        return system.func_70300_a(player);
    }

    @Override
    public boolean func_75140_a(EntityPlayer player, int action) {
        return system.handleGuiAction(action, player);
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
        if (index < 13) {
            if (!func_75135_a(stack, 13, 49, true)) return null;
        } else if (VehicleEnergyHelper.isBattery(stack)) {
            if (!func_75135_a(stack, 12, 13, false)) return null;
        } else {
            boolean moved = false;
            for (int i = 0; i < system.getMissileCapacity(); ++i) {
                if (system.func_94041_b(i, stack)
                        && func_75135_a(stack, i, i + 1, false)) {
                    moved = true;
                    break;
                }
            }
            if (!moved) return null;
        }
        if (stack.field_77994_a <= 0) slot.func_75215_d(null);
        else slot.func_75218_e();
        return original;
    }

    private static final class MissileSlot extends Slot {
        private final EntityMobileAirDefense system;

        MissileSlot(EntityMobileAirDefense system, int slot, int x, int y) {
            super(system, slot, x, y);
            this.system = system;
        }

        @Override
        public boolean func_75214_a(ItemStack stack) {
            return system.func_94041_b(field_75225_a, stack);
        }
    }

    private static final class BatterySlot extends Slot {
        BatterySlot(EntityMobileAirDefense system, int slot, int x, int y) {
            super(system, slot, x, y);
        }

        @Override
        public boolean func_75214_a(ItemStack stack) {
            return VehicleEnergyHelper.isBattery(stack);
        }
    }
}
