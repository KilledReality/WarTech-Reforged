package com.wartec.wartecmod.port.client;

import com.hbm.inventory.gui.GuiInfoContainer;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.gui.ContainerLegacyTile;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class GuiLegacyTile {
    private GuiLegacyTile() {
    }

    public static Object create(InventoryPlayer inventory,
            TileEntityWarTechMachine tile, int guiId) {
        if (guiId == WarTechGuiHandler.GUI_LAUNCH_TUBE
                || guiId == WarTechGuiHandler.GUI_BALLISTIC_LAUNCHER) {
            return new Launcher(inventory, tile, guiId);
        }
        if (guiId == WarTechGuiHandler.GUI_COMMUNICATION_MAST) {
            return new Relay(inventory, tile, guiId);
        }
        if (guiId == WarTechGuiHandler.GUI_STRATEGIC_RADAR) {
            return new StrategicRadar(inventory, tile, guiId);
        }
        return null;
    }

    private static final class Launcher extends GuiInfoContainer {
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                "wartecmod", "textures/gui/weapon/gui_launch_tube.png");
        private final TileEntityWarTechMachine tile;

        Launcher(InventoryPlayer inventory, TileEntityWarTechMachine tile, int guiId) {
            super(new ContainerLegacyTile(inventory, tile, guiId));
            this.tile = tile;
            xSize = 176;
            ySize = 166;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            super.drawScreen(mouseX, mouseY, partialTicks);
            drawElectricityInfo(this, mouseX, mouseY,
                    guiLeft + 8, guiTop + 53, 160, 16,
                    tile.getPower(), tile.getMaxPower());
            String[] first = tile.isBallisticLauncher()
                    ? new String[]{"First Slot:", "  -Ballistic Missile"}
                    : tile.isVlsExhaust()
                            ? new String[]{"Missile cells:",
                                    "  -Anti-air interceptors"}
                            : new String[]{"First Slot:",
                                    "  -Cruise Missile"};
            drawCustomInfoStat(mouseX, mouseY,
                    guiLeft - 16, guiTop + 36, 16, 16,
                    guiLeft - 8, guiTop + 52, first);
            String[] second = tile.isVlsExhaust()
                    ? new String[]{"Automatic targeting:",
                            "  -No designator required"}
                    : new String[]{"Second Slot:",
                            "  -Target designator for missiles"};
            drawCustomInfoStat(mouseX, mouseY,
                    guiLeft - 16, guiTop + 52, 16, 16,
                    guiLeft - 8, guiTop + 52, second);
        }

        @Override
        protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
            GL11.glColor4f(1, 1, 1, 1);
            mc.getTextureManager().bindTexture(TEXTURE);
            drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
            int width = (int) Math.round(160.0D * tile.getPower()
                    / Math.max(1L, tile.getMaxPower()));
            drawTexturedModalRect(guiLeft + 8, guiTop + 53, 8, 166, width, 16);
            drawInfoPanel(guiLeft - 16, guiTop + 36, 16, 16, 2);
            drawInfoPanel(guiLeft - 16, guiTop + 52, 16, 16, 3);
            if (tile.isVlsExhaust()) {
                mc.getTextureManager().bindTexture(TEXTURE);
                for (int column = 0; column < 9; ++column) {
                    drawTexturedModalRect(guiLeft + 7 + column * 18,
                            guiTop + 16, 7, 83, 18, 18);
                }
            }
        }

        @Override
        protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
            String name = tile.isBallisticLauncher()
                    ? "Ballistic Missile Launcher"
                    : tile.isVlsExhaust() ? "Missile cells" : "Launch Tube";
            fontRenderer.drawString(name,
                    xSize / 2 - fontRenderer.getStringWidth(name) / 2, 6, 0x404040);
            fontRenderer.drawString(I18n.format("container.inventory"),
                    8, ySize - 94, 0x404040);
        }
    }

    private static final class Relay extends GuiContainer {
        private final TileEntityWarTechMachine relay;

        Relay(InventoryPlayer inventory, TileEntityWarTechMachine relay, int guiId) {
            super(new ContainerLegacyTile(inventory, relay, guiId));
            this.relay = relay;
            xSize = 256;
            ySize = 222;
        }

        @Override public void initGui() {
            super.initGui();
            buttonList.add(new GuiButton(0, guiLeft + 142, guiTop + 108, 72, 20, "TOGGLE"));
        }

        @Override protected void actionPerformed(GuiButton button) throws IOException {
            if (button.enabled && button.id == 0) {
                mc.playerController.sendEnchantPacket(inventorySlots.windowId, 0);
            }
        }

        @Override protected void drawGuiContainerBackgroundLayer(float partialTicks,
                int mouseX, int mouseY) {
            int left = guiLeft;
            int top = guiTop;
            Gui.drawRect(left, top, left + 256, top + 222, 0xFF77776F);
            Gui.drawRect(left + 4, top + 4, left + 252, top + 136, 0xFF151B19);
            Gui.drawRect(left + 8, top + 8, left + 136, top + 132, 0xFF23322C);
            Gui.drawRect(left + 140, top + 8, left + 248, top + 104, 0xFF2E352F);
            drawSignal(left + 20, top + 29);
            int powerHeight = (int) Math.round(58.0D * relay.getPower()
                    / Math.max(1L, relay.getMaxPower()));
            Gui.drawRect(left + 222, top + 34, left + 238, top + 94, 0xFF101510);
            Gui.drawRect(left + 224, top + 92 - powerHeight,
                    left + 236, top + 92, 0xFF28D75C);
            drawSlot(left + 221, top + 104);
            drawPlayerSlots(left, top, 7, 139, 197);
        }

        private void drawSignal(int x, int y) {
            int green = relay.isRelayOnline() ? 0xFF55F278 : 0xFF53645A;
            for (int ring = 0; ring < 4; ++ring) {
                int radius = 13 + ring * 12;
                Gui.drawRect(x + 48 - radius, y + 48, x + 49 - radius, y + 50, green);
                Gui.drawRect(x + 48 + radius, y + 48, x + 49 + radius, y + 50, green);
                Gui.drawRect(x + 48, y + 48 - radius, x + 50, y + 49 - radius, green);
                Gui.drawRect(x + 48, y + 48 + radius, x + 50, y + 49 + radius, green);
            }
            Gui.drawRect(x + 46, y + 46, x + 52, y + 52, 0xFFFFFFFF);
            Gui.drawRect(x + 48, y + 22, x + 50, y + 77, green);
        }

        @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
            int green = 0x62FF75;
            int white = 0xE6E6DF;
            fontRenderer.drawString("COMMUNICATION MAST", 142, 12, white);
            String status = relay.isRelayOnline() ? "ONLINE"
                    : relay.isRelayEnabled() ? "NO POWER" : "STANDBY";
            fontRenderer.drawString(status, 142, 26,
                    relay.isRelayOnline() ? green : 0xFFB34F);
            fontRenderer.drawString("RANGE   2400", 142, 46, white);
            fontRenderer.drawString("LINKS   " + relay.getLinkedRelays(), 142, 59, green);
            int percent = (int) Math.round(100.0D * relay.getPower()
                    / Math.max(1L, relay.getMaxPower()));
            fontRenderer.drawString("POWER   " + percent + "%", 142, 72, white);
            fontRenderer.drawString("LOAD    20 HE/t", 142, 85, white);
            fontRenderer.drawString("BATTERY", 207, 96, white);
            fontRenderer.drawString("INVENTORY", 8, 129, white);
            for (GuiButton button : buttonList) {
                if (button.id == 0) {
                    button.displayString = relay.isRelayEnabled() ? "DISABLE" : "ENABLE";
                }
            }
        }
    }

    private static final class StrategicRadar extends GuiContainer {
        private static final ResourceLocation HBM_RADAR =
                new ResourceLocation(
                        "hbm", "textures/gui/machine/gui_radar_nt.png");
        private final TileEntityWarTechMachine radar;

        StrategicRadar(InventoryPlayer inventory, TileEntityWarTechMachine radar, int guiId) {
            super(new ContainerLegacyTile(inventory, radar, guiId));
            this.radar = radar;
            xSize = 256;
            ySize = 222;
        }

        @Override public void initGui() {
            super.initGui();
            buttonList.add(new GuiButton(0, guiLeft + 142, guiTop + 108, 72, 20, "TOGGLE"));
        }

        @Override protected void actionPerformed(GuiButton button) throws IOException {
            if (button.enabled && button.id == 0) {
                mc.playerController.sendEnchantPacket(inventorySlots.windowId, 0);
            }
        }

        @Override protected void drawGuiContainerBackgroundLayer(float partialTicks,
                int mouseX, int mouseY) {
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
            int powerHeight = (int) Math.round(58.0D * radar.getPower()
                    / Math.max(1L, radar.getMaxPower()));
            Gui.drawRect(left + 222, top + 34, left + 238, top + 94, 0xFF101510);
            Gui.drawRect(left + 224, top + 92 - powerHeight,
                    left + 236, top + 92, 0xFF28D75C);
            drawSlot(left + 221, top + 104);
            drawPlayerSlots(left, top, 7, 139, 197);
        }

        private void drawRadarPicture(int left, int top) {
            int cx = left + 68;
            int cy = top + 68;
            double angle = (System.currentTimeMillis() % 5000L) / 5000.0D * Math.PI * 2.0D;
            for (int radius = 5; radius <= 55; radius += 3) {
                int x = cx + (int) Math.round(Math.sin(angle) * radius);
                int y = cy - (int) Math.round(Math.cos(angle) * radius);
                Gui.drawRect(x, y, x + 1, y + 1, 0x805CFF78);
            }
            double scale = 54.0D / 6000.0D;
            for (int index = 0; index < radar.getRadarBlipCount(); ++index) {
                int packed = radar.getRadarBlip(index);
                int relativeX = (short) (packed >>> 16);
                int relativeZ = (short) packed;
                int x = cx + (int) Math.round(relativeX * scale);
                int y = cy + (int) Math.round(relativeZ * scale);
                int color = ((System.currentTimeMillis() / 250L + index) & 1L) == 0L
                        ? 0xFFFF5E55 : 0xFFFFC15A;
                Gui.drawRect(x - 2, y - 2, x + 3, y + 3, color);
                Gui.drawRect(x - 3, y, x + 4, y + 1, 0xFFFFFFFF);
            }
        }

        @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
            int green = 0x62FF75;
            int white = 0xE6E6DF;
            fontRenderer.drawString("STRATEGIC EARLY WARNING RADAR", 142, 9, white);
            String status = radar.isRadarOperational() ? "ONLINE"
                    : !radar.isStructureFormed() ? "STRUCTURE ERROR"
                    : radar.isRadarEnabled() && radar.getPower() >= 2500L
                            ? "WARMUP " + radar.getWarmupPercent() + "%"
                            : radar.isRadarEnabled() ? "NO POWER" : "STANDBY";
            fontRenderer.drawString(status, 142, 23,
                    radar.isRadarOperational() ? green : 0xFFB34F);
            fontRenderer.drawString("CONTACTS  "
                    + radar.getDisplayedRadarContacts(), 142, 43, green);
            fontRenderer.drawString("RANGE     6000", 142, 55, white);
            fontRenderer.drawString("CEILING   4096", 142, 67, white);
            int percent = (int) Math.round(100.0D * radar.getPower()
                    / Math.max(1L, radar.getMaxPower()));
            fontRenderer.drawString("POWER " + percent + "%", 142, 83, white);
            fontRenderer.drawString("LARGE ONLY", 142, 95, 0x93D9FF);
            fontRenderer.drawString("BATTERY", 207, 96, white);
            fontRenderer.drawString("INVENTORY", 8, 129, white);
            for (GuiButton button : buttonList) {
                if (button.id == 0) {
                    button.displayString = radar.isRadarEnabled() ? "DISABLE" : "ENABLE";
                }
            }
        }
    }

    private static void drawSlot(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 18, 0xFF3A3A36);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF9D9D91);
    }

    private static void drawPlayerSlots(int left, int top, int x,
            int inventoryY, int hotbarY) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(left + x + column * 18, top + inventoryY + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            drawSlot(left + x + column * 18, top + hotbarY);
        }
    }

}
