package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

public final class GuiLegacyAircraft extends GuiContainer {
    private final EntityWarTechBase aircraft;

    public GuiLegacyAircraft(InventoryPlayer inventory, EntityWarTechBase aircraft) {
        super(new ContainerLegacyEntity(inventory, aircraft,
                ContainerLegacyEntity.Layout.AIRCRAFT));
        this.aircraft = aircraft;
        xSize = 236;
        ySize = 221;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiButton(0, guiLeft + 12, guiTop + 91, 76, 20, "LAUNCH"));
        buttonList.add(new GuiButton(1, guiLeft + 90, guiTop + 91, 60, 20, "WEAPON"));
        buttonList.add(new GuiButton(2, guiLeft + 152, guiTop + 91, 38, 20, "LAST"));
        buttonList.add(new GuiButton(3, guiLeft + 192, guiTop + 91, 32, 20, "ALL"));
        if (isTactical()) {
            buttonList.add(new GuiButton(4, guiLeft + 158, guiTop + 6, 66, 18, "MODE"));
            buttonList.add(new GuiButton(5, guiLeft + 158, guiTop + 25, 66, 18, "REMOTE"));
        } else {
            buttonList.add(new GuiButton(4, guiLeft + 158, guiTop + 6, 66, 18, "REMOTE"));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled && button.id >= 0 && button.id <= 5) {
            mc.playerController.sendEnchantPacket(inventorySlots.windowId, button.id);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int left = guiLeft;
        int top = guiTop;
        Gui.drawRect(left, top, left + 236, top + 221, 0xFF777B7C);
        Gui.drawRect(left + 5, top + 5, left + 231, top + 129, 0xFF20282C);
        Gui.drawRect(left + 5, top + 131, left + 231, top + 216, 0xFF77776F);
        Gui.drawRect(left + 16, top + 31, left + 220, top + 80, 0xFF343F43);
        Gui.drawRect(left + 22, top + 47, left + 214, top + 50, 0xFF92A7A9);
        for (int slot = 0; slot < 6; ++slot) {
            drawSlot(left + 43 + slot * 21, top + 56);
            if (!aircraft.isPayloadSlotAvailable(slot)) {
                Gui.drawRect(left + 44 + slot * 21, top + 57,
                        left + 60 + slot * 21, top + 73, 0xD0181B1C);
            }
            Gui.drawRect(left + 51 + slot * 21, top + 39,
                    left + 54 + slot * 21, top + 57, 0xFF69787B);
        }
        drawSlot(left + 177, top + 56);
        drawSlot(left + 204, top + 56);
        int powerWidth = (int) Math.round(168.0D * aircraft.getLegacyPower()
                / Math.max(1, aircraft.getEnergyCapacity()));
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
        fontRenderer.drawString(aircraftName() + " MISSION CONTROL", 15, 10, white);
        fontRenderer.drawString("STATUS: " + aircraft.getLegacyStateName(), 15, 21,
                aircraft.getLegacyState() == 0 ? 0x65F28A : cyan);
        fontRenderer.drawString(payloadStatus(), 15, 31, 0xF1C96B);
        fontRenderer.drawString("HARDPOINTS " + aircraft.getHardpointCount()
                + " | RANGE " + aircraft.getMissionRange(), 15, 41, 0xB8C6C8);
        String target = aircraft.hasGuidanceTarget()
                ? (int) aircraft.getTargetX() + " / " + (int) aircraft.getTargetY()
                        + " / " + (int) aircraft.getTargetZ()
                : "NOT ASSIGNED";
        EntityWarTechAircraft missionAircraft =
                aircraft instanceof EntityWarTechAircraft
                        ? (EntityWarTechAircraft) aircraft : null;
        String queue = aircraft.hasGuidanceTarget()
                && missionAircraft != null
                ? missionAircraft.getTargetIndex() + 1 + "/"
                        + missionAircraft.getTargetCount()
                : "0/" + aircraft.getMaximumTargets();
        fontRenderer.drawString("TARGET " + queue + ": " + target, 15, 81, white);
        fontRenderer.drawString("LTC", 176, 47, 0xF1C96B);
        fontRenderer.drawString("BAT", 203, 47, white);
        fontRenderer.drawString("POWER " + aircraft.getLegacyPower() + "/"
                + aircraft.getEnergyCapacity(), 24, 113, white);
        fontRenderer.drawString("HP " + Math.round(100.0F * aircraft.getHealthValue()
                / aircraft.getProfile().getMaxHealth()) + "%", 164, 113, white);
        fontRenderer.drawString("INVENTORY", 24, 130, white);
        for (GuiButton button : buttonList) {
            if (button.id == 0) {
                button.displayString = aircraft.getLegacyState() == 0
                        ? (isTactical() && aircraft.isInterceptorMode()
                                ? "AUTO ARMED" : "LAUNCH MISSION")
                        : "RETURN TO BASE";
            } else if (button.id == 1) {
                button.displayString = shortPayloadName();
            } else if (button.id == 4 && isTactical()) {
                button.displayString = aircraft.isInterceptorMode() ? "INTERCEPT" : "STRIKE";
                button.enabled = aircraft.getLegacyState() == 0;
            } else if (button.id == 5 && isTactical()) {
                button.displayString = "REMOTE PILOT";
                button.enabled = aircraft.getLegacyState() == 0;
            } else if (button.id == 4) {
                button.displayString = "REMOTE PILOT";
                button.enabled = aircraft.getLegacyState() == 0;
            }
        }
    }

    private boolean isTactical() {
        return aircraft.getProfile() == WarTechEntityProfile.F_16C
                || aircraft.getProfile() == WarTechEntityProfile.SU_27;
    }

    private String aircraftName() {
        if (aircraft.getProfile() == WarTechEntityProfile.F_16C) return "F-16C";
        if (aircraft.getProfile() == WarTechEntityProfile.SU_27) return "SU-27";
        return "MQ-9";
    }

    private String payloadStatus() {
        return shortPayloadName() + " SELECTED";
    }

    private String shortPayloadName() {
        switch (aircraft.getLegacySelectedPayload()) {
            case 1: return "GBU-12";
            case 2: return "MK 82";
            case 3: return "HJ-10";
            case 4: return "AGM-65";
            case 5: return "KH-29";
            case 6: return "KAB-500L";
            case 7: return "JDAM";
            case 8: return "WT-AAM";
            default: return "AGM-114";
        }
    }
}
