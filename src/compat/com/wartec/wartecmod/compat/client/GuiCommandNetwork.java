package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.ContainerCommandVehicle;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

public final class GuiCommandNetwork extends GuiContainer {
    private final EntityCommandTruck command;

    public GuiCommandNetwork(InventoryPlayer inventory, EntityCommandTruck command) {
        super(new ContainerCommandVehicle(inventory, command));
        this.command = command;
        field_146999_f = 256;
        field_147000_g = 222;
    }

    @Override
    protected void func_146976_a(float partialTicks, int mouseX, int mouseY) {
        int left = field_147003_i;
        int top = field_147009_r;
        Gui.func_73734_a(left, top, left + 256, top + 222, 0xFF77776F);
        Gui.func_73734_a(left + 4, top + 4, left + 252, top + 136, 0xFF141A18);
        Gui.func_73734_a(left + 7, top + 7, left + 249, top + 28, 0xFF26332E);
        Gui.func_73734_a(left + 4, top + 136, left + 170, top + 218, 0xFF77776F);
        drawSlot(left + 227, top + 104);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(left + 7 + column * 18, top + 139 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            drawSlot(left + 7 + column * 18, top + 197);
        }
    }

    private void drawSlot(int x, int y) {
        Gui.func_73734_a(x, y, x + 18, y + 18, 0xFF3A3A36);
        Gui.func_73734_a(x + 1, y + 1, x + 17, y + 17, 0xFF9D9D91);
    }

    @Override
    protected void func_146979_b(int mouseX, int mouseY) {
        int green = 0x5DE878;
        int red = 0xE85D5D;
        int white = 0xD9E7DF;
        field_146289_q.func_78276_b("AIR DEFENSE COMMAND NETWORK", 12, 13, white);
        drawLine(12, 38, "NETWORK", command.isNetworkActive() ? "ONLINE" : "OFFLINE",
                command.isNetworkActive() ? green : red);
        drawLine(12, 53, "LINKED RADARS", Integer.toString(command.getLinkedRadars()), white);
        drawLine(12, 68, "LINKED LAUNCHERS", Integer.toString(command.getLinkedLaunchers()), white);
        drawLine(12, 83, "TRACKED TARGETS", Integer.toString(command.getContacts()), 0xFF776E);
        drawLine(12, 98, "HOSTILE EMITTERS", Integer.toString(command.getHostileEmitters()), 0xFF8F70);
        drawLine(12, 113, "ACTIVE INTERCEPTS", Integer.toString(command.getAssignedTargets()), 0xFFB45D);
        int percent = (int) (command.getPower() * 100L / EntityCommandTruck.ENERGY_CAPACITY);
        field_146289_q.func_78276_b("POWER " + percent + "%", 178, 71,
                command.isNetworkActive() ? green : red);
        field_146289_q.func_78276_b("BATTERY", 205, 96, white);
        field_146289_q.func_78276_b("INVENTORY", 8, 129, white);
    }

    private void drawLine(int x, int y, String label, String value, int color) {
        field_146289_q.func_78276_b(label, x, y, 0xB6C2BC);
        field_146289_q.func_78276_b(value, 118, y, color);
    }
}
