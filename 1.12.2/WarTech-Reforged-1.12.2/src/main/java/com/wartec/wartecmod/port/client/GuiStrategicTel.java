package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityStrategicTel;
import com.wartec.wartecmod.port.entity.StrategicSystemProfile;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

/** Hardened launch-control panel for a deployed strategic TEL. */
public final class GuiStrategicTel extends GuiContainer {
    private final EntityStrategicTel tel;

    public GuiStrategicTel(InventoryPlayer inventory, EntityStrategicTel tel) {
        super(new ContainerLegacyEntity(inventory, tel,
                ContainerLegacyEntity.Layout.STRATEGIC));
        this.tel = tel;
        xSize = 256;
        ySize = 204;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new WarTechGuiButton(1, guiLeft + 16, guiTop + 86,
                104, 20, tel.isDeployed() ? "RETRACT" : "ERECT"));
        buttonList.add(new WarTechGuiButton(0, guiLeft + 136, guiTop + 86,
                104, 20, "AUTHORIZE LAUNCH"));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (buttonList.size() >= 2) {
            buttonList.get(0).displayString = tel.isDeployed()
                    ? "RETRACT" : "ERECT";
            buttonList.get(1).enabled = tel.isDeployed()
                    && tel.getErectionProgress() >= 100
                    && tel.isMissileLoaded() && tel.hasGuidanceTarget()
                    && tel.getLaunchTicks() == 0;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled && (button.id == 0 || button.id == 1)) {
            mc.playerController.sendEnchantPacket(
                    inventorySlots.windowId, button.id);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
            int mouseX, int mouseY) {
        int left = guiLeft;
        int top = guiTop;
        Gui.drawRect(left, top, left + xSize, top + ySize, 0xFF777C78);
        Gui.drawRect(left + 5, top + 5, left + 251, top + 75, 0xFF172125);
        Gui.drawRect(left + 9, top + 9, left + 247, top + 71, 0xFF0C1519);
        int progress = tel.getErectionProgress();
        Gui.drawRect(left + 16, top + 61, left + 240, top + 67, 0xFF263236);
        Gui.drawRect(left + 16, top + 61,
                left + 16 + progress * 224 / 100, top + 67, 0xFF55D98A);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(left + 46 + column * 18,
                        top + 121 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            drawSlot(left + 46 + column * 18, top + 179);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        StrategicSystemProfile system = tel.getStrategicSystem();
        fontRenderer.drawString("STRATEGIC LAUNCH CONTROL // "
                + system.getDisplayName(), 13, 13, 0xFF9CF7C1);
        fontRenderer.drawString("CANISTER: "
                + (tel.isMissileLoaded() ? "LOADED" : "EMPTY"),
                13, 28, tel.isMissileLoaded() ? 0xFF70E69D : 0xFFFF7777);
        String target = tel.hasGuidanceTarget()
                ? (int) tel.getTargetX() + " / " + (int) tel.getTargetY()
                        + " / " + (int) tel.getTargetZ()
                : "NOT PROGRAMMED";
        fontRenderer.drawString("TARGET: " + target, 13, 41,
                tel.hasGuidanceTarget() ? 0xFFE6EBD9 : 0xFFFF7777);
        fontRenderer.drawString("ERECTOR " + tel.getErectionProgress()
                + "%", 13, 51, 0xFFAFBBB6);
        fontRenderer.drawString(system.getReentryVehicles() + " RV // "
                + (system.isNuclear() ? "NUCLEAR" : "KINETIC")
                + " // R " + (int) system.getMaximumRange(),
                13, 111, 0xFF262C29);
    }

    private void drawSlot(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 18, 0xFF333833);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8C9189);
    }
}
