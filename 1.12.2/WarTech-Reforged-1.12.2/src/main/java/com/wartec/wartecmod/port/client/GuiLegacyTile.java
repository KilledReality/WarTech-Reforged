package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.gui.ContainerLegacyTile;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public final class GuiLegacyTile {
    private GuiLegacyTile() { }
    public static Object create(InventoryPlayer inventory,TileEntityWarTechMachine tile,int guiId) {
        if(guiId==WarTechGuiHandler.GUI_LAUNCH_TUBE || guiId==WarTechGuiHandler.GUI_BALLISTIC_LAUNCHER)
            return new Launcher(inventory,tile,guiId);
        if(guiId==WarTechGuiHandler.GUI_COMMUNICATION_MAST || guiId==WarTechGuiHandler.GUI_STRATEGIC_RADAR)
            return new Instrument(inventory,tile,guiId);
        return null;
    }
    private static final class Launcher extends GuiContainer {
        private final TileEntityWarTechMachine tile;
        Launcher(InventoryPlayer inventory,TileEntityWarTechMachine tile,int id) {
            super(new ContainerLegacyTile(inventory,tile,id));this.tile=tile;xSize=200;ySize=202;
        }
        @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY) {
            GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
            GuiTheme.panel(guiLeft+7,guiTop+25,186,79);
            GuiTheme.panel(guiLeft+7,guiTop+116,186,82);
            GuiTheme.bar(guiLeft+18,guiTop+91,162,7,tile.getPower()/(double)Math.max(1L,tile.getMaxPower()));
            for(Slot slot:inventorySlots.inventorySlots) GuiTheme.slot(guiLeft+slot.xPos-1,guiTop+slot.yPos-1);
        }
        private void text(String value,int x,int y,int width,int color) { GuiTheme.text(fontRenderer,value,x,y,width,color); }
        @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY) {
            String title=tile.isBallisticLauncher()?"launcher.ballistic":tile.isVlsExhaust()?"launcher.cells":"launcher.tube";
            text(GuiTheme.tr(title),10,8,180,GuiTheme.TEXT);
            text(GuiTheme.tr(tile.isVlsExhaust()?"launcher.cells_label":"launcher.payload_label"),18,27,162,GuiTheme.MUTED);
            text(GuiTheme.tr(tile.isVlsExhaust()?"launcher.auto":"launcher.designator"),18,61,162,GuiTheme.MUTED);
            text(GuiTheme.tr("power",(int)(100.0*tile.getPower()/Math.max(1L,tile.getMaxPower()))),18,78,162,GuiTheme.ACCENT);
            text(I18n.format("container.inventory"),18,108,162,GuiTheme.MUTED);
        }
        @Override public void drawScreen(int x,int y,float partial) {
            drawDefaultBackground();super.drawScreen(x,y,partial);renderHoveredToolTip(x,y);
            if(isPointInRegion(18,76,162,23,x,y))
                drawHoveringText(java.util.Collections.singletonList(tile.getPower()+" / "+tile.getMaxPower()+" HE"),x,y);
            else if(isPointInRegion(16,25,168,47,x,y) && getSlotUnderMouse()==null)
                drawHoveringText(fontRenderer.listFormattedStringToWidth(
                    GuiTheme.tr(tile.isVlsExhaust()?"launcher.cells_hint":"launcher.tube_hint"),230),x,y);
        }
    }
    private static final class Instrument extends GuiContainer {
        private final TileEntityWarTechMachine tile;
        private final boolean relay;
        Instrument(InventoryPlayer inventory,TileEntityWarTechMachine tile,int id) {
            super(new ContainerLegacyTile(inventory,tile,id));this.tile=tile;
            relay=id==WarTechGuiHandler.GUI_COMMUNICATION_MAST;xSize=256;ySize=222;
        }
        private boolean enabled() { return relay?tile.isRelayEnabled():tile.isRadarEnabled(); }
        private boolean online() { return relay?tile.isRelayOnline():tile.isRadarOperational(); }
        @Override public void initGui() { super.initGui();buttonList.add(new WarTechGuiButton(0,guiLeft+130,guiTop+105,84,20,"")); }
        @Override protected void actionPerformed(GuiButton b) throws IOException {
            if(b.enabled && b.id==0) mc.playerController.sendEnchantPacket(inventorySlots.windowId,0);
        }
        @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY) {
            GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
            GuiTheme.panel(guiLeft+7,guiTop+27,114,98);GuiTheme.panel(guiLeft+126,guiTop+27,123,76);
            GuiTheme.panel(guiLeft+5,guiTop+136,246,82);
            GuiLegacyRadar.scope(guiLeft+63,guiTop+76,45,relay?0:tile.getDisplayedRadarContacts(),
                tile::getRadarBlip,relay?2400:6000,online());
            GuiTheme.bar(guiLeft+130,guiTop+100,84,3,tile.getPower()/(double)Math.max(1L,tile.getMaxPower()));
            for(Slot slot:inventorySlots.inventorySlots) GuiTheme.slot(guiLeft+slot.xPos-1,guiTop+slot.yPos-1);
        }
        private void text(String value,int x,int y,int width,int color) { GuiTheme.text(fontRenderer,value,x,y,width,color); }
        @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY) {
            text(GuiTheme.tr(relay?"relay":"radar.strategic"),10,8,236,GuiTheme.TEXT);
            text(GuiTheme.tr(online()?"online":enabled()?"no_power":"standby"),130,30,114,online()?GuiTheme.GOOD:GuiTheme.WARN);
            text(GuiTheme.tr(relay?"links":"contacts",relay?tile.getLinkedRelays():tile.getDisplayedRadarContacts()),130,43,114,GuiTheme.ACCENT);
            text(GuiTheme.tr("range",relay?2400:6000),130,55,114,GuiTheme.TEXT);
            text(GuiTheme.tr(relay?"load":"ceiling",relay?20:4096),130,67,114,GuiTheme.TEXT);
            text(GuiTheme.tr(relay?"relay_channel":"large_only"),130,79,114,GuiTheme.MUTED);
            text(GuiTheme.tr("power",(int)(100.0*tile.getPower()/Math.max(1L,tile.getMaxPower()))),130,91,84,GuiTheme.TEXT);
            text(GuiTheme.tr("battery"),219,94,28,GuiTheme.MUTED);
            text(I18n.format("container.inventory"),8,129,160,GuiTheme.MUTED);
        }
        @Override public void drawScreen(int x,int y,float partial) {
            buttonList.get(0).displayString=GuiTheme.tr(enabled()?"disable":"enable");
            drawDefaultBackground();super.drawScreen(x,y,partial);renderHoveredToolTip(x,y);
        }
    }
}
