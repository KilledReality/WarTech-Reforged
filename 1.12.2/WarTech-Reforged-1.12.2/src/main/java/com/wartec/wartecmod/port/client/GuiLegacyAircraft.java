package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import com.wartec.wartecmod.port.uav.UavText;
import com.wartec.wartecmod.port.cruise.*;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

/** Non-overlapping rows shared by UAVs, fighters and the Tu-95. */
public class GuiLegacyAircraft extends GuiContainer {
    private final EntityWarTechBase aircraft;
    public GuiLegacyAircraft(InventoryPlayer inventory,EntityWarTechBase aircraft) {
        super(new ContainerLegacyEntity(inventory,aircraft,ContainerLegacyEntity.Layout.AIRCRAFT));
        this.aircraft=aircraft;xSize=300;ySize=258;
    }
    private EntityCustomUav uav() { return aircraft instanceof EntityCustomUav?(EntityCustomUav)aircraft:null; }
    private EntityWarTechAircraft plane() { return aircraft instanceof EntityWarTechAircraft?(EntityWarTechAircraft)aircraft:null; }
    private boolean tu95() { return aircraft.getProfile()==WarTechEntityProfile.TU_95; }
    private boolean tactical() { return aircraft.getProfile()==WarTechEntityProfile.SU_27 || aircraft.getProfile()==WarTechEntityProfile.F_16C; }
    private int cruises() { return plane()==null?0:plane().getCustomCruiseCount(); }
    private void button(int id,int x,int y,int w,String label) { buttonList.add(new WarTechGuiButton(id,guiLeft+x,guiTop+y,w,18,label)); }
    @Override public void initGui() {
        super.initGui();button(0,12,122,96,UavText.ui("LAUNCH"));
        if(uav()!=null || plane()!=null) button(8,15,82,56,I18n.format("cruise.carrier.import"));
        if(tu95()) {
            button(1,110,122,78,UavText.ui("LAST"));button(2,190,122,98,UavText.ui("ALL"));
            button(3,230,24,58,UavText.ui("REMOTE"));
        } else {
            button(1,110,122,78,UavText.ui("WEAPON"));
            if(uav()!=null) {
                button(6,190,122,98,UavText.ui("GET DATA"));
                button(7,164,24,64,UavText.ui("REPAIR"));button(4,230,24,58,UavText.ui("REMOTE"));
            } else {
                button(2,190,122,46,UavText.ui("LAST"));button(3,238,122,50,UavText.ui("ALL"));
                if(tactical()) button(4,164,24,64,UavText.ui("MODE"));
                button(tactical()?5:4,230,24,58,UavText.ui("REMOTE"));
            }
        }
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        if(b.enabled) mc.playerController.sendEnchantPacket(inventorySlots.windowId,b.id);
    }
    private void text(String label,int x,int y,int width,int colour) {
        fontRenderer.drawString(fontRenderer.trimStringToWidth(label,width),x,y,colour);
    }
    private String name() {
        if(uav()!=null) return aircraft.getName();
        return I18n.format("cruise.aircraft.name."+(tu95()?"tu95":aircraft.getProfile()==WarTechEntityProfile.SU_27?"su27":
            aircraft.getProfile()==WarTechEntityProfile.F_16C?"f16":"mq9"));
    }
    private String weapon() {
        if(cruises()>0 || uav()!=null && uav().hasCruiseStore()) return UavText.ui("CUSTOM CRUISE");
        if(uav()!=null) return uav().getSelectedHardpointName();
        String[] labels={"AGM-114","GBU-12","MK 82","HJ-10","AGM-65","KH-29","KAB-500L","JDAM","WT-AAM"};
        return labels[Math.max(0,Math.min(8,aircraft.getLegacySelectedPayload()))];
    }
    private String payload() {
        if(cruises()>0) return I18n.format(cruises()==1?"cruise.aircraft.single":"cruise.aircraft.multiple",cruises());
        if(uav()!=null) return uav().getSelectedHardpointName();
        return tu95()?"KH-555 / FAB-5000 / KAB-3000":weapon()+UavText.ui(" SELECTED");
    }
    private String envelope() {
        if(uav()!=null && uav().hasCruiseStore()) return I18n.format("cruise.uav.envelope",(int)uav().getUavStats().getMass(),
            (int)uav().getUavStats().getMaximumMass(),(int)uav().getCruiseReleaseRange());
        if(cruises()>0) return I18n.format(tu95()?"cruise.aircraft.tu_capacity":"cruise.aircraft.fighter_capacity");
        return UavText.ui("HARDPOINTS ")+aircraft.getHardpointCount()+UavText.ui(" | RANGE ")+aircraft.getMissionRange();
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
        GuiTheme.panel(guiLeft+5,guiTop+24,290,142);
        GuiTheme.panel(guiLeft+5,guiTop+168,290,85);
        Gui.drawRect(guiLeft+15,guiTop+45,guiLeft+285,guiTop+103,0xFF343F43);
        for(int slot=0;slot<6;slot++) {
            drawSlot(75+slot*21,82);
            boolean available=aircraft.isPayloadSlotAvailable(slot);
            if(cruises()>0 && !tu95()) {
                CruiseBuild build=plane().getCruiseStoreBuild(aircraft.getLegacySelectedHardpoint());
                available=slot<CruiseAircraftLoadout.capacity(aircraft.getProfile(),build.getAirframe());
            }
            if(!available) Gui.drawRect(guiLeft+76+slot*21,guiTop+83,guiLeft+92+slot*21,guiTop+99,0xD0181B1C);
        }
        drawSlot(209,82);drawSlot(236,82);
        int width=(int)Math.round(238.0*aircraft.getLegacyPower()/Math.max(1,aircraft.getEnergyCapacity()));
        Gui.drawRect(guiLeft+24,guiTop+156,guiLeft+264,guiTop+164,0xFF101719);
        Gui.drawRect(guiLeft+25,guiTop+157,guiLeft+25+Math.max(0,Math.min(238,width)),guiTop+163,0xFF43D47B);
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) drawSlot(67+col*18,175+row*18);
        for(int col=0;col<9;col++) drawSlot(67+col*18,233);
    }
    private void drawSlot(int x,int y) {
        GuiTheme.slot(guiLeft+x,guiTop+y);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY) {
        text(name()+UavText.ui(" MISSION CONTROL"),15,10,270,0xE7ECEC);
        text(UavText.ui("STATUS: ")+UavText.ui(aircraft.getLegacyStateName()),15,28,143,aircraft.getLegacyState()==0?0x65F28A:0x77E3ED);
        text(payload(),15,48,270,0xF1C96B);text(envelope(),15,60,270,0xB8C6C8);
        String target=aircraft.hasGuidanceTarget()?(int)aircraft.getTargetX()+" / "+(int)aircraft.getTargetY()+" / "+(int)aircraft.getTargetZ():UavText.ui("NOT ASSIGNED");
        String queue=uav()!=null && !uav().getMission().isEmpty()?uav().getMissionIndex()+1+"/"+uav().getMission().size():
            uav()!=null && aircraft.hasGuidanceTarget()?"1/1":
            plane()!=null && aircraft.hasGuidanceTarget()?plane().getTargetIndex()+1+"/"+plane().getTargetCount():"0/"+aircraft.getMaximumTargets();
        text(UavText.ui("TARGET ")+queue+": "+target,15,108,270,0xE7ECEC);
        text(UavText.ui("LTC ")+(uav()==null?"":uav().getFlareCount()),204,73,30,0xF1C96B);
        text(UavText.ui("BAT"),236,73,48,0xE7ECEC);
        text(UavText.ui("POWER ")+aircraft.getLegacyPower()+"/"+aircraft.getEnergyCapacity(),24,146,191,0xE7ECEC);
        int health=uav()==null?Math.round(100*aircraft.getHealthValue()/aircraft.getProfile().getMaxHealth()):uav().getHealthPercent();
        text(UavText.ui("HP ")+health+"%",222,146,64,0xE7ECEC);text(UavText.ui("INVENTORY"),68,167,164,0xE7ECEC);
    }
    /** Set bounded labels BEFORE GuiScreen draws the buttons (foreground is too late). */
    @Override public void drawScreen(int mouseX,int mouseY,float partial) {
        for(GuiButton b:buttonList) {
            if(b.id==8) { b.displayString=I18n.format("cruise.carrier.import");b.enabled=aircraft.getLegacyState()==0 && (cruises()>0 || uav()!=null && uav().hasCruiseStore()); }
            else if(b.id==0) b.displayString=UavText.ui(aircraft.getLegacyState()==0?tactical() && aircraft.isInterceptorMode()?"AUTO ARMED":"LAUNCH":"RETURN TO BASE");
            else if(tu95() && b.id==3 || !tu95() && (b.id==5 || b.id==4 && !tactical())) {
                b.displayString=UavText.ui("REMOTE");b.enabled=aircraft.getLegacyState()==0;
            }
            else if(!tu95() && b.id==1) { b.displayString=weapon();b.enabled=uav()==null || aircraft.getHardpointCount()>0; }
            else if(b.id==6) b.displayString=UavText.ui("GET DATA");
            else if(b.id==7) { b.displayString=UavText.ui("REPAIR");b.enabled=aircraft.getLegacyState()==0 && uav().getHealthPercent()<100; }
            else if(tactical() && b.id==4) { b.displayString=UavText.ui(aircraft.isInterceptorMode()?"INTERCEPT":"STRIKE");b.enabled=aircraft.getLegacyState()==0 && cruises()==0; }
            b.displayString=fontRenderer.trimStringToWidth(b.displayString,b.width-8);
        }
        drawDefaultBackground();super.drawScreen(mouseX,mouseY,partial);renderHoveredToolTip(mouseX,mouseY);
        ItemStack cursor=mc.player.inventory.getItemStack();
        if(cursor.getItem()==com.wartec.wartecmod.port.content.WarTechContent.ASSEMBLED_CRUISE && isPointInRegion(75,82,126,18,mouseX,mouseY)) {
            CruiseBuild build=CruiseBuild.fromStack(cursor);String error=null;Object[] args=new Object[0];
            if(uav()!=null) { error=com.wartec.wartecmod.port.uav.UavCruiseCarriage.error(uav().getBuild(),build);args=com.wartec.wartecmod.port.uav.UavCruiseCarriage.errorArguments(uav().getBuild(),build,error); }
            else if(plane()!=null) error=plane().getCruiseLoadError(Math.max(0,Math.min(5,(mouseX-guiLeft-75)/21)),cursor);
            java.util.List<String> info=new java.util.ArrayList<>();
            if(error!=null) info.addAll(fontRenderer.listFormattedStringToWidth(I18n.format(error,args),280));
            info.addAll(fontRenderer.listFormattedStringToWidth(I18n.format(uav()!=null?"cruise.uav.rack_slot":tu95()?"cruise.aircraft.tu_capacity":"cruise.aircraft.fighter_capacity"),280));
            drawHoveringText(info,mouseX,mouseY);
        } else if(isPointInRegion(15,82,56,18,mouseX,mouseY)) {
            drawHoveringText(fontRenderer.listFormattedStringToWidth(I18n.format("cruise.carrier.import_hint"),280),mouseX,mouseY);
        } else if(uav()!=null && isPointInRegion(75,82,18,18,mouseX,mouseY) && cursor.isEmpty()) {
            java.util.List<String> info=new java.util.ArrayList<>();
            com.wartec.wartecmod.port.uav.UavStats dry=uav().getBuild().calculateStats();
            info.add(I18n.format("cruise.uav.available_mass",(int)Math.floor(dry.getMaximumMass()-dry.getMass())));
            info.addAll(fontRenderer.listFormattedStringToWidth(I18n.format("cruise.uav.hint"),280));drawHoveringText(info,mouseX,mouseY);
        } else if(isPointInRegion(15,45,270,25,mouseX,mouseY)) {
            java.util.List<String> info=new java.util.ArrayList<>();info.addAll(fontRenderer.listFormattedStringToWidth(payload(),280));
            info.addAll(fontRenderer.listFormattedStringToWidth(envelope(),280));drawHoveringText(info,mouseX,mouseY);
        }
    }
}
