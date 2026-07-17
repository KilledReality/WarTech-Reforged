package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import net.minecraft.client.gui.GuiScreen;

public final class GuiCommandNetwork extends GuiScreen {
    private final EntityCommandTruck command;

    public GuiCommandNetwork(EntityCommandTruck command) {
        this.command = command;
    }

    @Override
    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        func_146276_q_();
        int left = field_146294_l / 2 - 112;
        int top = field_146295_m / 2 - 82;
        func_73734_a(left, top, left + 224, top + 164, 0xE0141A18);
        func_73734_a(left + 3, top + 3, left + 221, top + 26, 0xFF26332E);
        func_73732_a(field_146289_q, "AIR DEFENSE COMMAND NETWORK",
                field_146294_l / 2, top + 10, 0xFFD9E7DF);

        int powerColor = command.isNetworkActive() ? 0xFF5DE878 : 0xFFE85D5D;
        drawLine(left, top, 38, "NETWORK", command.isNetworkActive() ? "ONLINE" : "OFFLINE",
                powerColor);
        drawLine(left, top, 57, "LINKED RADARS", Integer.toString(command.getLinkedRadars()),
                command.getLinkedRadars() > 0 ? 0xFF75D9FF : 0xFF9A9A9A);
        drawLine(left, top, 76, "LINKED LAUNCHERS",
                Integer.toString(command.getLinkedLaunchers()), 0xFFE8D475);
        drawLine(left, top, 95, "TRACKED TARGETS", Integer.toString(command.getContacts()),
                command.getContacts() > 0 ? 0xFFFF776E : 0xFF9A9A9A);
        drawLine(left, top, 114, "ACTIVE INTERCEPTS",
                Integer.toString(command.getAssignedTargets()), 0xFFFFB45D);
        int percent = (int) (command.getPower() * 100L
                / EntityCommandTruck.ENERGY_CAPACITY);
        drawLine(left, top, 133, "POWER", percent + "%", powerColor);
        func_73732_a(field_146289_q,
                "Shift + RMB: retract | Battery: recharge",
                field_146294_l / 2, top + 150, 0xFF8FA39A);
        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    private void drawLine(int left, int top, int offset, String label,
            String value, int color) {
        field_146289_q.func_78276_b(label, left + 15, top + offset, 0xFFB6C2BC);
        int width = field_146289_q.func_78256_a(value);
        field_146289_q.func_78276_b(value, left + 209 - width, top + offset, color);
    }

    @Override
    public boolean func_73868_f() {
        return false;
    }
}
