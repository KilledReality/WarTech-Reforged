package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.ContainerMobileAirDefense;
import com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

public final class GuiMobileAirDefense extends GuiContainer {
    private final EntityMobileAirDefense system;

    public GuiMobileAirDefense(InventoryPlayer inventory,
            EntityMobileAirDefense system) {
        super(new ContainerMobileAirDefense(inventory, system));
        this.system = system;
        field_146999_f = 280;
        field_147000_g = 228;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        field_146292_n.add(new GuiButton(0, field_147003_i + 151,
                field_147009_r + 78, 86, 20, "MODE"));
        field_146292_n.add(new GuiButton(1, field_147003_i + 151,
                field_147009_r + 102, 86, 20, "RADAR"));
    }

    @Override
    protected void func_146284_a(GuiButton button) {
        if (!button.field_146124_l) return;
        if (button.field_146127_k == 0 || button.field_146127_k == 1) {
            field_146297_k.field_71442_b.func_78756_a(
                    field_147002_h.field_75152_c, button.field_146127_k);
        }
    }

    @Override
    protected void func_146976_a(float partialTicks, int mouseX, int mouseY) {
        int left = field_147003_i;
        int top = field_147009_r;
        Gui.func_73734_a(left, top, left + 280, top + 228, 0xFF86877F);
        Gui.func_73734_a(left + 4, top + 4, left + 140, top + 138, 0xFF1E2620);
        Gui.func_73734_a(left + 144, top + 4, left + 276, top + 138, 0xFF30352F);
        Gui.func_73734_a(left + 4, top + 140, left + 276, top + 224, 0xFF77776F);
        drawRadar(left, top);

        for (int row = 0; row < 2; ++row) {
            for (int column = 0; column < 6; ++column) {
                int slot = column + row * 6;
                int x = left + 150 + column * 18;
                int y = top + 37 + row * 18;
                drawSlot(x, y);
                if (slot >= system.getMissileCapacity()) {
                    Gui.func_73734_a(x + 2, y + 2, x + 16, y + 16, 0xFF512B2B);
                    Gui.func_73734_a(x + 3, y + 8, x + 15, y + 10, 0xFFD15353);
                }
            }
        }
        drawSlot(left + 243, top + 104);
        int powerHeight = (int) Math.round(58.0D * system.getPower()
                / EntityMobileAirDefense.ENERGY_CAPACITY);
        Gui.func_73734_a(left + 264, top + 65, left + 274, top + 125, 0xFF101510);
        Gui.func_73734_a(left + 266, top + 123 - powerHeight,
                left + 272, top + 123, 0xFF28D75C);

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(left + 7 + column * 18, top + 145 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            drawSlot(left + 7 + column * 18, top + 203);
        }
    }

    private void drawRadar(int left, int top) {
        int centerX = left + 72;
        int centerY = top + 72;
        for (int radius = 20; radius <= 60; radius += 20) {
            drawCircle(centerX, centerY, radius, 0x604DB868);
        }
        Gui.func_73734_a(centerX, top + 11, centerX + 1, top + 133, 0x504DB868);
        Gui.func_73734_a(left + 11, centerY, left + 133, centerY + 1, 0x504DB868);
        double angle = (System.currentTimeMillis() % 3500L) / 3500.0D
                * Math.PI * 2.0D;
        for (int radius = 3; radius <= 60; radius += 2) {
            int x = centerX + (int) Math.round(Math.sin(angle) * radius);
            int y = centerY - (int) Math.round(Math.cos(angle) * radius);
            Gui.func_73734_a(x, y, x + 1, y + 1, 0xA066FF83);
        }
        double scale = 59.0D / Math.max(1, system.getRadarRange());
        for (int i = 0; i < system.getBlipCount(); ++i) {
            int packed = system.getPackedBlip(i);
            int relativeX = (short) (packed >>> 16);
            int relativeZ = (short) packed;
            int x = centerX + (int) Math.round(relativeX * scale);
            int y = centerY + (int) Math.round(relativeZ * scale);
            int color = ((System.currentTimeMillis() / 220L + i) & 1L) == 0L
                    ? 0xFFFF5E55 : 0xFFFFC15A;
            Gui.func_73734_a(x - 2, y - 2, x + 3, y + 3, color);
        }
    }

    private void drawCircle(int cx, int cy, int radius, int color) {
        for (int degrees = 0; degrees < 360; degrees += 6) {
            double radians = Math.toRadians(degrees);
            int x = cx + (int) Math.round(Math.cos(radians) * radius);
            int y = cy + (int) Math.round(Math.sin(radians) * radius);
            Gui.func_73734_a(x, y, x + 1, y + 1, color);
        }
    }

    private void drawSlot(int x, int y) {
        Gui.func_73734_a(x, y, x + 18, y + 18, 0xFF3A3A36);
        Gui.func_73734_a(x + 1, y + 1, x + 17, y + 17, 0xFF9D9D91);
    }

    @Override
    protected void func_146979_b(int mouseX, int mouseY) {
        int white = 0xE6E6DF;
        int green = 0x62FF75;
        field_146289_q.func_78276_b(system.getSystemName(), 150, 9, white);
        String status = system.isOperational() ? "ONLINE"
                : system.isDeployed() ? "NO POWER / RADAR OFF" : "TRAVEL";
        field_146289_q.func_78276_b(status, 150, 21,
                system.isOperational() ? green : 0xFFB34F);
        field_146289_q.func_78276_b("MISSILES " + system.getAmmoCount()
                + "/" + system.getMissileCapacity(), 150, 29, white);
        field_146289_q.func_78276_b("CONTACTS " + system.getContacts(), 8, 7, green);
        field_146289_q.func_78276_b("RANGE " + system.getEngagementRange(), 8, 125, white);
        field_146289_q.func_78276_b("HEALTH " + system.getHealthPercent() + "%",
                73, 125, white);
        field_146289_q.func_78276_b("BAT", 241, 96, white);
        field_146289_q.func_78276_b("INVENTORY", 8, 137, white);
        for (Object value : field_146292_n) {
            if (!(value instanceof GuiButton)) continue;
            GuiButton button = (GuiButton) value;
            if (button.field_146127_k == 0) {
                button.field_146126_j = "FIRE: " + system.getFireModeName();
            } else if (button.field_146127_k == 1) {
                button.field_146126_j = system.isRadarEnabled()
                        ? "RADAR: ON" : "RADAR: OFF";
            }
        }
    }
}
