package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.content.PantsirAmmoBeltItem;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

public final class GuiLegacyAirDefense extends GuiContainer {
    private final EntityWarTechBase system;

    public GuiLegacyAirDefense(InventoryPlayer inventory, EntityWarTechBase system) {
        super(new ContainerLegacyEntity(inventory, system,
                ContainerLegacyEntity.Layout.AIR_DEFENSE));
        this.system = system;
        xSize = 280;
        ySize = 228;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiButton(0, guiLeft + 151, guiTop + 78, 86, 20, "MODE"));
        buttonList.add(new GuiButton(1, guiLeft + 151, guiTop + 102,
                system.isTor() ? 86 : 42, 20, "RADAR"));
        if (!system.isTor()) {
            buttonList.add(new GuiButton(2, guiLeft + 195, guiTop + 102, 42, 20, "GUNS"));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled && button.id >= 0 && button.id <= 2) {
            mc.playerController.sendEnchantPacket(inventorySlots.windowId, button.id);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int left = guiLeft;
        int top = guiTop;
        Gui.drawRect(left, top, left + 280, top + 228, 0xFF86877F);
        Gui.drawRect(left + 4, top + 4, left + 140, top + 138, 0xFF1E2620);
        Gui.drawRect(left + 144, top + 4, left + 276, top + 138, 0xFF30352F);
        Gui.drawRect(left + 4, top + 140, left + 276, top + 224, 0xFF77776F);
        drawRadar(left, top);
        for (int row = 0; row < 2; ++row) {
            for (int column = 0; column < 6; ++column) {
                int slot = column + row * 6;
                int x = left + 150 + column * 18;
                int y = top + 37 + row * 18;
                drawSlot(x, y);
                if (slot >= missileCapacity()) {
                    Gui.drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF512B2B);
                    Gui.drawRect(x + 3, y + 8, x + 15, y + 10, 0xFFD15353);
                }
            }
        }
        drawSlot(left + 243, top + 104);
        drawSlot(left + 243, top + 79);
        if (system.isTor()) {
            Gui.drawRect(left + 245, top + 81, left + 259, top + 95, 0xFF512B2B);
            Gui.drawRect(left + 246, top + 87, left + 258, top + 90, 0xFFD15353);
        }
        int powerHeight = (int) Math.round(58.0D * system.getLegacyPower()
                / Math.max(1, system.getEnergyCapacity()));
        Gui.drawRect(left + 264, top + 65, left + 274, top + 125, 0xFF101510);
        Gui.drawRect(left + 266, top + 123 - powerHeight,
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
        Gui.drawRect(centerX, top + 11, centerX + 1, top + 133, 0x504DB868);
        Gui.drawRect(left + 11, centerY, left + 133, centerY + 1, 0x504DB868);
        double angle = (System.currentTimeMillis() % 3500L) / 3500.0D * Math.PI * 2.0D;
        for (int radius = 3; radius <= 60; radius += 2) {
            int x = centerX + (int) Math.round(Math.sin(angle) * radius);
            int y = centerY - (int) Math.round(Math.cos(angle) * radius);
            Gui.drawRect(x, y, x + 1, y + 1, 0xA066FF83);
        }
        double scale = 59.0D / Math.max(1, radarRange());
        for (int index = 0; index < system.getLegacyBlipCount(); ++index) {
            int packed = system.getLegacyBlip(index);
            int relativeX = (short) (packed >>> 16);
            int relativeZ = (short) packed;
            int x = centerX + (int) Math.round(relativeX * scale);
            int y = centerY + (int) Math.round(relativeZ * scale);
            int color = ((System.currentTimeMillis() / 220L + index) & 1L) == 0L
                    ? 0xFFFF5E55 : 0xFFFFC15A;
            Gui.drawRect(x - 2, y - 2, x + 3, y + 3, color);
        }
    }

    private static void drawCircle(int cx, int cy, int radius, int color) {
        for (int degrees = 0; degrees < 360; degrees += 6) {
            double radians = Math.toRadians(degrees);
            int x = cx + (int) Math.round(Math.cos(radians) * radius);
            int y = cy + (int) Math.round(Math.sin(radians) * radius);
            Gui.drawRect(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawSlot(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 18, 0xFF3A3A36);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF9D9D91);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int white = 0xE6E6DF;
        int green = 0x62FF75;
        fontRenderer.drawString(system.isTor() ? "TOR-M1" : "PANTSIR-S2", 150, 9, white);
        String status = system.isLegacyOperational() ? "ONLINE"
                : system.isLegacyEnabled() ? "NO POWER / RADAR OFF" : "TRAVEL";
        fontRenderer.drawString(status, 150, 21,
                system.isLegacyOperational() ? green : 0xFFB34F);
        fontRenderer.drawString(system.getRequiredInterceptorName()
                + " ONLY " + ammoCount() + "/" + missileCapacity(), 150, 29, 0xFFD65A);
        fontRenderer.drawString("CONTACTS " + system.getLegacyContacts(), 8, 7, green);
        fontRenderer.drawString("RANGE " + engagementRange(), 8, 125, white);
        fontRenderer.drawString("HEALTH " + Math.round(100.0F
                * system.getHealthValue() / system.getProfile().getMaxHealth()) + "%", 73, 125, white);
        fontRenderer.drawString("BAT", 241, 96, white);
        if (!system.isTor()) {
            int rounds = PantsirAmmoBeltItem.getRounds(system.getStackInSlot(13));
            fontRenderer.drawString("30:" + rounds, 239, 71, 0xFFD65A);
        }
        fontRenderer.drawString("INVENTORY", 8, 137, white);
        for (GuiButton button : buttonList) {
            if (button.id == 0) button.displayString = "FIRE: " + fireModeName();
            else if (button.id == 1) {
                button.displayString = system.isLegacyEnabled() ? "RADAR: ON" : "RADAR: OFF";
            } else if (button.id == 2) {
                button.displayString = system.isGunsEnabled() ? "G:ON" : "G:OFF";
            }
        }
    }

    private int ammoCount() {
        int count = 0;
        for (int slot = 0; slot < missileCapacity(); ++slot) {
            if (!system.getStackInSlot(slot).isEmpty()) ++count;
        }
        return count;
    }
    private int missileCapacity() { return system.isTor() ? 8 : 12; }
    private int radarRange() { return system.isTor() ? 340 : 260; }
    private int engagementRange() { return system.isTor() ? 220 : 100; }
    private String fireModeName() {
        return system.getLegacyFireMode() == 0 ? "HOLD"
                : system.getLegacyFireMode() == 1 ? "AUTO" : "EMERGENCY";
    }
}
