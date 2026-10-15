package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.gui.ContainerLegacyTile;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import java.io.IOException;

/** External radar launchers use the defense theme without a local radar scope. */
public final class GuiNetworkAirDefense extends GuiContainer {
    private final TileEntityWarTechMachine tile;
    public GuiNetworkAirDefense(InventoryPlayer inventory,TileEntityWarTechMachine tile,int id) {
        super(new ContainerLegacyTile(inventory,tile,id));this.tile=tile;xSize=320;ySize=228;
    }
    @Override public void initGui() {
        super.initGui();buttonList.add(new WarTechGuiButton(0,guiLeft+151,guiTop+82,126,20,""));
        buttonList.add(new WarTechGuiButton(1,guiLeft+151,guiTop+106,126,20,""));
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException { if(b.enabled) mc.playerController.sendEnchantPacket(inventorySlots.windowId,b.id); }
    @Override public void drawScreen(int x,int y,float partial) {
        for(GuiButton b:buttonList) b.displayString=GuiTheme.tr(b.id==0?(tile.isSamEnabled()?"sam.stow":"sam.deploy"):(tile.isSamAutoFire()?"sam.auto_on":"sam.auto_off"));
        drawDefaultBackground();super.drawScreen(x,y,partial);renderHoveredToolTip(x,y);
        if(isPointInRegion(8,27,134,100,x,y)) drawHoveringText(fontRenderer.listFormattedStringToWidth(GuiTheme.tr("sam.network_hint"),250),x,y);
        if(isPointInRegion(150,26,126,51,x,y)) drawHoveringText(fontRenderer.listFormattedStringToWidth(GuiTheme.tr("sam.ammo_hint"),250),x,y);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int x,int y) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);GuiTheme.panel(guiLeft+5,guiTop+26,135,109);
        GuiTheme.panel(guiLeft+144,guiTop+26,172,109);GuiTheme.panel(guiLeft+5,guiTop+144,310,80);
        for(Slot slot:inventorySlots.inventorySlots) GuiTheme.slot(guiLeft+slot.xPos-1,guiTop+slot.yPos-1);
        GuiTheme.bar(guiLeft+290,guiTop+106,16,24,tile.getPower()/(double)Math.max(1,tile.getMaxPower()));
    }
    private void text(String value,int x,int y,int w,int color) {GuiTheme.text(fontRenderer,value,x,y,w,color);}
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        text(GuiTheme.tr(tile.getLegacyMachineName().contains("s400")?"sam.s400":"sam.patriot"),10,8,300,GuiTheme.TEXT);
        String status=!tile.isSamEnabled()?"transport":tile.getSamDeployment(1)<1?"sam.preparing":!tile.isSamAutoFire()?"standby":tile.getPower()<50000?"no_power":tile.getSamAmmoCount()==0?"sam.empty":tile.getSamNetworkRadars()==0?"sam.no_radar":"sam.ready";
        text(GuiTheme.tr(status),10,30,126,GuiTheme.WARN);
        text(GuiTheme.tr("sam.network",tile.getSamNetworkRadars()),10,47,126,GuiTheme.ACCENT);
        text(GuiTheme.tr("sam.progress",Math.round(tile.getSamDeployment(1)*100)),10,63,126,GuiTheme.TEXT);
        int tier=tile.getLegacyMachineName().contains("s400")?3:2;
        text(GuiTheme.tr("range_short",Math.round(com.wartec.wartecmod.port.integration.WeaponBalance.interceptorRange(tier))),10,79,126,GuiTheme.MUTED);
        text(GuiTheme.tr("power",100L*tile.getPower()/Math.max(1,tile.getMaxPower())),10,95,126,GuiTheme.ACCENT);
        text(GuiTheme.tr("sam.external"),10,114,126,GuiTheme.MUTED);
        text(GuiTheme.tr("sam.ammo",tile.getSamAmmoCount()),151,27,126,GuiTheme.TEXT);
        text(GuiTheme.tr("battery"),282,72,31,GuiTheme.MUTED);
        text(net.minecraft.client.resources.I18n.format("container.inventory"),8,137,300,GuiTheme.MUTED);
    }
}
