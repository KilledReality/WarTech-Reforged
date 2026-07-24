package com.wartec.wartecmod.compat;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerCommunicationRelay extends Container {
    private final TileEntityCommunicationRelay relay;
    private int lastPower = -1;
    private int lastFlags = -1;
    private int lastLinks = -1;

    public ContainerCommunicationRelay(InventoryPlayer playerInventory,
            TileEntityCommunicationRelay relay) {
        this.relay = relay;
        func_75146_a(new BatterySlot(relay, 0, 222, 105));
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

    @Override
    public void func_75132_a(ICrafting crafter) {
        super.func_75132_a(crafter);
        sendState(crafter);
    }

    @Override
    public void func_75142_b() {
        super.func_75142_b();
        int power = relay.getPower();
        int flags = (relay.isEnabled() ? 1 : 0) | (relay.isOnline() ? 2 : 0);
        int links = relay.getLinkedRelayCount();
        if (power == lastPower && flags == lastFlags && links == lastLinks) return;
        for (Object value : (List) field_75149_d) {
            sendState((ICrafting) value);
        }
        lastPower = power;
        lastFlags = flags;
        lastLinks = links;
    }

    private void sendState(ICrafting crafter) {
        int power = relay.getPower();
        crafter.func_71112_a(this, 0, power & 65535);
        crafter.func_71112_a(this, 1, power >>> 16);
        crafter.func_71112_a(this, 2,
                (relay.isEnabled() ? 1 : 0) | (relay.isOnline() ? 2 : 0));
        crafter.func_71112_a(this, 3, relay.getLinkedRelayCount());
    }

    @Override
    public void func_75137_b(int id, int value) {
        if (id == 0) {
            relay.setPower((relay.getPower() & -65536) | (value & 65535));
        } else if (id == 1) {
            relay.setPower((relay.getPower() & 65535)
                    | ((value & 65535) << 16));
        } else if (id == 2) {
            relay.setClientState((value & 1) != 0, (value & 2) != 0,
                    relay.getLinkedRelayCount());
        } else if (id == 3) {
            relay.setClientState(relay.isEnabled(), relay.isOnline(), value);
        }
    }

    @Override
    public boolean func_75145_c(EntityPlayer player) {
        return relay.func_70300_a(player);
    }

    @Override
    public boolean func_75140_a(EntityPlayer player, int action) {
        if (action != 0) return false;
        relay.toggleEnabled();
        return true;
    }

    @Override
    public ItemStack func_82846_b(EntityPlayer player, int index) {
        if (index < 0 || index >= field_75151_b.size()) return null;
        Slot slot = (Slot) field_75151_b.get(index);
        if (slot == null || !slot.func_75216_d()) return null;
        ItemStack stack = slot.func_75211_c();
        ItemStack original = stack.func_77946_l();
        if (index == 0) {
            if (!func_75135_a(stack, 1, 37, true)) return null;
        } else if (!relay.func_94041_b(0, stack)
                || !func_75135_a(stack, 0, 1, false)) {
            return null;
        }
        if (stack.field_77994_a <= 0) slot.func_75215_d(null);
        else slot.func_75218_e();
        slot.func_75220_a(stack, original);
        return original;
    }

    private static final class BatterySlot extends Slot {
        BatterySlot(TileEntityCommunicationRelay inventory,
                int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Override public boolean func_75214_a(ItemStack stack) {
            return VehicleEnergyHelper.isBattery(stack);
        }
    }
}
