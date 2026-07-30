package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

public final class GuiLegacyTu95 extends GuiContainer {
    private final EntityWarTechBase bomber;

    public GuiLegacyTu95(InventoryPlayer inventory, EntityWarTechBase bomber) {
        super(new ContainerLegacyEntity(inventory, bomber,
                ContainerLegacyEntity.Layout.AIRCRAFT));
        this.bomber = bomber;
        xSize = 236;
        ySize = 221;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiButton(0, guiLeft + 12, guiTop + 91, 112, 20, "LAUNCH MISSION"));
        buttonList.add(new GuiButton(1, guiLeft + 128, guiTop + 91, 46, 20, "LAST"));
        buttonList.add(new GuiButton(2, guiLeft + 178, guiTop + 91, 46, 20, "ALL"));
        buttonList.add(new GuiButton(3, guiLeft + 150, guiTop + 25, 74, 18, "REMOTE"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled && button.id >= 0 && button.id <= 3) {
            mc.playerController.sendEnchantPacket(inventorySlots.windowId, button.id);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int left = guiLeft;
        int top = guiTop;
        Gui.drawRect(left, top, left + 236, top + 221, 0xFF747778);
        Gui.drawRect(left + 5, top + 5, left + 231, top + 129, 0xFF20272A);
        Gui.drawRect(left + 5, top + 131, left + 231, top + 216, 0xFF77776F);
        Gui.drawRect(left + 16, top + 31, left + 220, top + 80, 0xFF343E41);
        Gui.drawRect(left + 22, top + 47, left + 214, top + 50, 0xFF98AAAC);
        for (int slot = 0; slot < 6; ++slot) drawSlot(left + 43 + slot * 21, top + 56);
        drawSlot(left + 177, top + 56);
        drawSlot(left + 204, top + 56);
        int powerWidth = (int) Math.round(168.0D * bomber.getLegacyPower()
                / Math.max(1, bomber.getEnergyCapacity()));
        Gui.drawRect(left + 24, top + 118, left + 194, top + 125, 0xFF101719);
        Gui.drawRect(left + 25, top + 119, left + 25 + powerWidth,
                top + 124, 0xFF43D47B);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(left + 23 + column * 18, top + 138 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            drawSlot(left + 23 + column * 18, top + 196);
        }
    }

    private static void drawSlot(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 18, 0xFF393B3A);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF9C9D96);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int white = 0xE7ECEC;
        int cyan = 0x77E3ED;
        fontRenderer.drawString("TU-95 STRATEGIC AVIATION CONTROL", 15, 10, white);
        fontRenderer.drawString("STATUS: " + bomber.getLegacyStateName(), 15, 21,
                bomber.getLegacyState() == 0 ? 0x65F28A : cyan);
        fontRenderer.drawString("MISSION: 8000", 151, 21, cyan);
        fontRenderer.drawString("KH-555 / FAB-5000 / KAB-3000", 15, 31, 0xF1C96B);
        String target = bomber.hasGuidanceTarget()
                ? (int) bomber.getTargetX() + " / " + (int) bomber.getTargetY()
                        + " / " + (int) bomber.getTargetZ()
                : "NOT ASSIGNED";
        EntityWarTechAircraft aircraft =
                bomber instanceof EntityWarTechAircraft
                        ? (EntityWarTechAircraft) bomber : null;
        String queue = bomber.hasGuidanceTarget() && aircraft != null
                ? aircraft.getTargetIndex() + 1 + "/"
                        + aircraft.getTargetCount()
                : "0/6";
        fontRenderer.drawString("TARGET " + queue
                + ": " + target, 15, 81, white);
        fontRenderer.drawString("LTC", 176, 47, 0xF1C96B);
        fontRenderer.drawString("BAT", 203, 47, white);
        fontRenderer.drawString("POWER " + bomber.getLegacyPower() + "/"
                + bomber.getEnergyCapacity(), 24, 113, white);
        fontRenderer.drawString("HP " + Math.round(100.0F * bomber.getHealthValue()
                / bomber.getProfile().getMaxHealth()) + "%", 164, 113, white);
        fontRenderer.drawString("INVENTORY", 24, 130, white);
        for (GuiButton button : buttonList) {
            if (button.id == 0) {
                button.displayString = bomber.getLegacyState() == 0
                        ? "LAUNCH MISSION" : "RETURN TO BASE";
            } else if (button.id == 3) {
                button.displayString = "REMOTE PILOT";
                button.enabled = bomber.getLegacyState() == 0;
            }
        }
    }
}
