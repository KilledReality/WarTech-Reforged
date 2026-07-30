package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class GuiLegacyRadar extends GuiContainer {
    private static final ResourceLocation HBM_RADAR = new ResourceLocation(
            "hbm", "textures/gui/machine/gui_radar_nt.png");
    private final EntityWarTechBase radar;

    public GuiLegacyRadar(InventoryPlayer inventory, EntityWarTechBase radar) {
        super(new ContainerLegacyEntity(inventory, radar,
                ContainerLegacyEntity.Layout.RADAR));
        this.radar = radar;
        xSize = 256;
        ySize = 222;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiButton(0, guiLeft + 142, guiTop + 108, 72, 20, "TOGGLE"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled && button.id == 0) {
            mc.playerController.sendEnchantPacket(inventorySlots.windowId, 0);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int left = guiLeft;
        int top = guiTop;
        Gui.drawRect(left, top, left + 256, top + 222, 0xFF8B8B82);
        Gui.drawRect(left + 4, top + 4, left + 136, top + 136, 0xFF242824);
        Gui.drawRect(left + 140, top + 4, left + 252, top + 136, 0xFF2E352F);
        Gui.drawRect(left + 4, top + 136, left + 166, top + 218, 0xFF77776F);

        mc.getTextureManager().bindTexture(HBM_RADAR);
        GL11.glPushMatrix();
        GL11.glTranslatef(left + 5.0F, top + 5.0F, 0.0F);
        GL11.glScalef(0.62F, 0.62F, 1.0F);
        drawTexturedModalRect(0, 0, 5, 15, 204, 204);
        GL11.glPopMatrix();
        drawRadarPicture(left, top);

        int capacity = Math.max(1, radar.getEnergyCapacity());
        int powerHeight = (int) Math.round(58.0D
                * Math.min(1.0D, radar.getLegacyPower() / (double) capacity));
        Gui.drawRect(left + 222, top + 34, left + 238, top + 94, 0xFF101510);
        Gui.drawRect(left + 224, top + 92 - powerHeight,
                left + 236, top + 92, 0xFF28D75C);
        drawSlot(left + 221, top + 104);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(left + 7 + column * 18, top + 139 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            drawSlot(left + 7 + column * 18, top + 197);
        }
    }

    private void drawRadarPicture(int left, int top) {
        int centerX = left + 68;
        int centerY = top + 68;
        double angle = (System.currentTimeMillis() % 5000L) / 5000.0D
                * Math.PI * 2.0D;
        for (int radius = 5; radius <= 55; radius += 3) {
            int x = centerX + (int) Math.round(Math.sin(angle) * radius);
            int y = centerY - (int) Math.round(Math.cos(angle) * radius);
            Gui.drawRect(x, y, x + 1, y + 1, 0x805CFF78);
        }
        double scale = 54.0D / Math.max(1, range());
        for (int index = 0; index < radar.getLegacyBlipCount(); ++index) {
            int packed = radar.getLegacyBlip(index);
            int relativeX = (short) (packed >>> 16);
            int relativeZ = (short) packed;
            int x = centerX + (int) Math.round(relativeX * scale);
            int y = centerY + (int) Math.round(relativeZ * scale);
            int color = ((System.currentTimeMillis() / 250L + index) & 1L) == 0L
                    ? 0xFFFF5E55 : 0xFFFFC15A;
            Gui.drawRect(x - 2, y - 2, x + 3, y + 3, color);
            Gui.drawRect(x - 3, y, x + 4, y + 1, 0xFFFFFFFF);
        }
    }

    private static void drawSlot(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 18, 0xFF3A3A36);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF9D9D91);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int green = 0x62FF75;
        int white = 0xE6E6DF;
        fontRenderer.drawString(name(), 142, 9, white);
        String status = radar.isLegacyOperational() ? "ONLINE"
                : radar.isLegacyEnabled() ? "NO POWER" : "STANDBY";
        fontRenderer.drawString(status, 142, 23,
                radar.isLegacyOperational() ? green : 0xFFB34F);
        fontRenderer.drawString("CONTACTS  " + radar.getLegacyContacts(), 142, 43, green);
        fontRenderer.drawString("RANGE     " + range(), 142, 55, white);
        fontRenderer.drawString("CEILING   " + ceiling(), 142, 67, white);
        int percent = (int) Math.round(100.0D * radar.getLegacyPower()
                / Math.max(1, radar.getEnergyCapacity()));
        fontRenderer.drawString("POWER " + percent + "%", 142, 83, white);
        if (radar.getProfile() == WarTechEntityProfile.S400_RADAR) {
            fontRenderer.drawString("LARGE ONLY", 142, 95, 0x93D9FF);
        }
        fontRenderer.drawString("BATTERY", 207, 96, white);
        fontRenderer.drawString("INVENTORY", 8, 129, white);
        for (GuiButton button : buttonList) {
            if (button.id == 0) {
                button.displayString = radar.isLegacyEnabled() ? "DISABLE" : "ENABLE";
            }
        }
    }

    private int range() {
        return radar.getProfile() == WarTechEntityProfile.S400_RADAR ? 1200 : 600;
    }

    private int ceiling() {
        return radar.getProfile() == WarTechEntityProfile.S400_RADAR ? 900 : 500;
    }

    private String name() {
        return radar.getProfile() == WarTechEntityProfile.S400_RADAR
                ? "S-400 LONG RANGE RADAR" : "MOBILE RADAR";
    }
}
