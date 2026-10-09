package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public final class GuiLegacyRadar extends GuiContainer {
    private final EntityWarTechBase radar;
    public GuiLegacyRadar(InventoryPlayer inventory,EntityWarTechBase radar) {
        super(new ContainerLegacyEntity(inventory,radar,ContainerLegacyEntity.Layout.RADAR));
        this.radar=radar;xSize=256;ySize=222;
    }
    @Override public void initGui() {
        super.initGui();buttonList.add(new WarTechGuiButton(0,guiLeft+130,guiTop+105,84,20,""));
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if(button.enabled && button.id==0) mc.playerController.sendEnchantPacket(inventorySlots.windowId,0);
    }
    @Override public void drawScreen(int x,int y,float partial) {
        buttonList.get(0).displayString=GuiTheme.tr(radar.isLegacyEnabled()?"disable":"enable");
        drawDefaultBackground();super.drawScreen(x,y,partial);renderHoveredToolTip(x,y);
        if(isPointInRegion(10,107,108,18,x,y)) drawHoveringText(fontRenderer.listFormattedStringToWidth(GuiTheme.tr("repair_hint"),220),x,y);
        if(isPointInRegion(130,78,114,20,x,y)) drawHoveringText(fontRenderer.listFormattedStringToWidth(
            GuiTheme.tr(radar.getProfile()==WarTechEntityProfile.S400_RADAR?"large_hint":"mobile_hint"),230),x,y);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
        GuiTheme.panel(guiLeft+7,guiTop+27,114,98);
        GuiTheme.panel(guiLeft+126,guiTop+27,123,76);
        GuiTheme.panel(guiLeft+5,guiTop+136,246,82);
        scope(guiLeft+63,guiTop+68,35,radar.getLegacyBlipCount(),radar::getLegacyBlip,range(),radar.isLegacyOperational());
        GuiTheme.healthBar(guiLeft+10,guiTop+119,108,5,radar.getHealthValue()/radar.getHealthCapacity());
        GuiTheme.bar(guiLeft+130,guiTop+100,84,3,radar.getLegacyPower()/(double)Math.max(1,radar.getEnergyCapacity()));
        for(Slot slot:inventorySlots.inventorySlots) GuiTheme.slot(guiLeft+slot.xPos-1,guiTop+slot.yPos-1);
    }
    private void text(String text,int x,int y,int width,int color) { GuiTheme.text(fontRenderer,text,x,y,width,color); }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY) {
        text(GuiTheme.tr(radar.getProfile()==WarTechEntityProfile.S400_RADAR?"radar.s400":"radar.mobile"),10,8,236,GuiTheme.TEXT);
        text(GuiTheme.tr(radar.isLegacyOperational()?"online":radar.isLegacyEnabled()?"no_power":"standby"),130,30,114,
            radar.isLegacyOperational()?GuiTheme.GOOD:GuiTheme.WARN);
        text(GuiTheme.tr("contacts",radar.getLegacyContacts()),130,43,114,GuiTheme.ACCENT);
        text(GuiTheme.tr("range",range()),130,55,114,GuiTheme.TEXT);
        text(GuiTheme.tr("ceiling",radar.getProfile()==WarTechEntityProfile.S400_RADAR?900:500),130,67,114,GuiTheme.TEXT);
        text(GuiTheme.tr(radar.getProfile()==WarTechEntityProfile.S400_RADAR?"large_only":"all_contacts"),130,79,114,GuiTheme.MUTED);
        text(GuiTheme.tr("power",(int)(100L*radar.getLegacyPower()/Math.max(1,radar.getEnergyCapacity()))),130,91,84,GuiTheme.TEXT);
        text(GuiTheme.tr("battery"),219,94,28,GuiTheme.MUTED);
        text(GuiTheme.tr("health_points",Math.round(radar.getHealthValue()),Math.round(radar.getHealthCapacity())),10,108,108,GuiTheme.TEXT);
        text(net.minecraft.client.resources.I18n.format("container.inventory"),8,129,160,GuiTheme.MUTED);
        text(GuiTheme.tr("scope"),177,143,65,GuiTheme.ACCENT);
        int y=157;
        for(String line:fontRenderer.listFormattedStringToWidth(GuiTheme.tr("scope_hint"),65)) {
            if(y>205) break;text(line,177,y,65,GuiTheme.MUTED);y+=10;
        }
    }
    private int range() { return radar.getProfile()==WarTechEntityProfile.S400_RADAR?1200:600; }
    private static final double[] SIN=new double[60],COS=new double[60];
    static { for(int i=0;i<60;i++) { SIN[i]=Math.sin(i*Math.PI/30);COS[i]=Math.cos(i*Math.PI/30); } }
    static void scope(int cx,int cy,int radius,int count,java.util.function.IntUnaryOperator blip,int range,boolean active) {
        for(int ring=1;ring<=3;ring++) for(int i=0;i<60;i++) {
            int x=cx+(int)Math.round(COS[i]*radius*ring/3),y=cy+(int)Math.round(SIN[i]*radius*ring/3);
            Gui.drawRect(x,y,x+1,y+1,0xFF355F59);
        }
        Gui.drawRect(cx-radius,cy,cx+radius+1,cy+1,0xFF355F59);
        Gui.drawRect(cx,cy-radius,cx+1,cy+radius+1,0xFF355F59);
        if(!active) return;
        double angle=(System.currentTimeMillis()%5000)/5000.0*Math.PI*2,dx=Math.sin(angle),dy=-Math.cos(angle);
        for(int r=3;r<radius;r+=2) { int x=cx+(int)Math.round(dx*r),y=cy+(int)Math.round(dy*r);Gui.drawRect(x,y,x+1,y+1,0xFF83D6AE); }
        for(int i=0;i<Math.min(16,count);i++) {
            int packed=blip.applyAsInt(i);double x=(short)(packed>>>16)*(radius-3.0)/Math.max(1,range),z=(short)packed*(radius-3.0)/Math.max(1,range);
            if(x*x+z*z>(radius-2)*(radius-2)) continue;
            int px=cx+(int)Math.round(x),py=cy+(int)Math.round(z);
            Gui.drawRect(px-1,py-1,px+2,py+2,0xFFF0BC7A);
        }
    }
}
