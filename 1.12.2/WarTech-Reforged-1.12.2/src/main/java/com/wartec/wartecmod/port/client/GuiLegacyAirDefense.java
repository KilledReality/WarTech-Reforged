package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.content.PantsirAmmoBeltItem;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public final class GuiLegacyAirDefense extends GuiContainer {
    private final EntityWarTechBase system;
    public GuiLegacyAirDefense(InventoryPlayer inventory,EntityWarTechBase system) {
        super(new ContainerLegacyEntity(inventory,system,ContainerLegacyEntity.Layout.AIR_DEFENSE));
        this.system=system;xSize=280;ySize=228;
    }
    @Override public void initGui() {
        super.initGui();
        buttonList.add(new WarTechGuiButton(0,guiLeft+151,guiTop+78,86,20,""));
        buttonList.add(new WarTechGuiButton(1,guiLeft+151,guiTop+102,system.isTor()?86:42,20,""));
        if(!system.isTor()) buttonList.add(new WarTechGuiButton(2,guiLeft+195,guiTop+102,42,20,""));
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        if(b.enabled && b.id>=0 && b.id<=2) mc.playerController.sendEnchantPacket(inventorySlots.windowId,b.id);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int x,int y) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
        GuiTheme.panel(guiLeft+5,guiTop+26,135,109);
        GuiTheme.panel(guiLeft+144,guiTop+26,132,109);
        GuiTheme.panel(guiLeft+5,guiTop+144,270,80);
        GuiLegacyRadar.scope(guiLeft+71,guiTop+80,28,system.getLegacyBlipCount(),system::getLegacyBlip,
            system.isTor()?340:260,system.isLegacyOperational());
        for(Slot slot:inventorySlots.inventorySlots) GuiTheme.slot(guiLeft+slot.xPos-1,guiTop+slot.yPos-1);
        for(int slot=missileCapacity();slot<12;slot++) {
            int sx=guiLeft+151+(slot%6)*18,sy=guiTop+38+(slot/6)*18;
            Gui.drawRect(sx,sy,sx+16,sy+16,0xFF30383C);Gui.drawRect(sx+4,sy+7,sx+12,sy+9,0xFF956D65);
        }
        if(system.isTor()) Gui.drawRect(guiLeft+244,guiTop+80,guiLeft+260,guiTop+96,0xFF30383C);
        GuiTheme.bar(guiLeft+264,guiTop+80,10,43,system.getLegacyPower()/(double)Math.max(1,system.getEnergyCapacity()));
        GuiTheme.healthBar(guiLeft+10,guiTop+123,126,7,system.getHealthValue()/system.getHealthCapacity());
    }
    private void text(String value,int x,int y,int width,int color) { GuiTheme.text(fontRenderer,value,x,y,width,color); }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY) {
        text(GuiTheme.tr(system.isTor()?"defense.tor":"defense.pantsir"),10,8,260,GuiTheme.TEXT);
        text(GuiTheme.tr("contacts",system.getLegacyContacts()),10,29,126,GuiTheme.ACCENT);
        text(GuiTheme.tr(!system.isDeployed()?"transport":system.isLegacyOperational()?"online":system.getHealthValue()<=system.getHealthCapacity()*.2?"damaged_offline":system.isLegacyEnabled()?"no_power":"standby"),10,39,126,
            system.isLegacyOperational()?GuiTheme.GOOD:GuiTheme.WARN);
        text(system.getRequiredInterceptorName()+" "+ammoCount()+"/"+missileCapacity(),150,27,122,GuiTheme.WARN);
        text(GuiTheme.tr("range_short",system.getDefenseEngagementRange()),150,125,122,GuiTheme.MUTED);
        text(GuiTheme.tr("health_points",Math.round(system.getHealthValue()),Math.round(system.getHealthCapacity())),10,112,126,GuiTheme.TEXT);
        text(GuiTheme.tr("battery"),242,96,30,GuiTheme.MUTED);
        if(!system.isTor()) text("30: "+PantsirAmmoBeltItem.getRounds(system.getStackInSlot(13)),240,70,32,GuiTheme.WARN);
        text(net.minecraft.client.resources.I18n.format("container.inventory"),8,137,160,GuiTheme.MUTED);
    }
    @Override public void drawScreen(int x,int y,float partial) {
        for(GuiButton b:buttonList) {
            if(b.id==0) b.displayString=GuiTheme.tr("fire."+Math.max(0,Math.min(2,system.getLegacyFireMode())));
            if(b.id==1) b.displayString=GuiTheme.tr(!system.isDeployed()?"deploy":system.isLegacyEnabled()?"radar_on":"radar_off");
            if(b.id==2) b.displayString=GuiTheme.tr(system.isGunsEnabled()?"guns_on":"guns_off");
        }
        drawDefaultBackground();super.drawScreen(x,y,partial);renderHoveredToolTip(x,y);
        if(isPointInRegion(10,111,126,20,x,y)) drawHoveringText(fontRenderer.listFormattedStringToWidth(GuiTheme.tr("repair_hint"),220),x,y);
        if(isPointInRegion(150,27,122,9,x,y)) drawHoveringText(java.util.Collections.singletonList(
            system.getRequiredInterceptorName()+" "+ammoCount()+"/"+missileCapacity()),x,y);
    }
    private int missileCapacity() { return system.isTor()?8:12; }
    private int ammoCount() {
        int count=0;for(int i=0;i<missileCapacity();i++) if(!system.getStackInSlot(i).isEmpty()) count++;return count;
    }
}
