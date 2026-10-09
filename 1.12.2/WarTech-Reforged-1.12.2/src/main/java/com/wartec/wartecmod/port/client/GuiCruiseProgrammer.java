package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.EntityCustomCruise;
import com.wartec.wartecmod.port.gui.ContainerCruiseProgrammer;
import com.wartec.wartecmod.port.network.*;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;

public final class GuiCruiseProgrammer extends GuiContainer {
    private final ContainerCruiseProgrammer container;
    private final GuiTextField[] fields=new GuiTextField[3];
    private final int missileSlot,checksum;
    private String error="";
    private int page;
    public GuiCruiseProgrammer(InventoryPlayer inventory,int slot) {
        super(new ContainerCruiseProgrammer(inventory,slot));container=(ContainerCruiseProgrammer)inventorySlots;
        missileSlot=slot;checksum=CruiseBuild.fromStack(inventory.getStackInSlot(slot)).checksum();xSize=320;ySize=254;
    }
    private ItemStack missile() { return mc.player.inventory.getStackInSlot(missileSlot); }
    @Override public void initGui() {
        super.initGui();Keyboard.enableRepeatEvents(true);buttonList.clear();
        for(int i=0;i<3;i++) { fields[i]=new GuiTextField(i,fontRenderer,guiLeft+43+i*86,guiTop+108,80,18);fields[i].setMaxStringLength(16); }
        buttonList.add(new WarTechGuiButton(5,guiLeft+12,guiTop+28,146,20,""));
        buttonList.add(new WarTechGuiButton(8,guiLeft+162,guiTop+28,146,20,""));
        buttonList.add(new WarTechGuiButton(0,guiLeft+12,guiTop+128,100,20,""));
        buttonList.add(new WarTechGuiButton(1,guiLeft+116,guiTop+128,92,20,CruiseText.text("program.remove")));
        buttonList.add(new WarTechGuiButton(2,guiLeft+212,guiTop+128,96,20,CruiseText.text("program.clear")));
        buttonList.add(new WarTechGuiButton(3,guiLeft+12,guiTop+150,140,20,CruiseText.text("program.designator")));
        buttonList.add(new WarTechGuiButton(4,guiLeft+156,guiTop+150,152,20,CruiseText.text("program.update")));
        buttonList.add(new WarTechGuiButton(6,guiLeft+264,guiTop+85,20,16,"<"));
        buttonList.add(new WarTechGuiButton(7,guiLeft+288,guiTop+85,20,16,">"));
    }
    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false);super.onGuiClosed(); }
    @Override public void updateScreen() {
        super.updateScreen();if(!container.canInteractWith(mc.player)) { mc.displayGuiScreen(null);return; }
        for(GuiTextField field:fields) field.updateCursorCounter();
    }
    @Override protected void actionPerformed(GuiButton button) {
        CruiseMission mission=CruiseMission.fromStack(missile());
        if(button.id==6 || button.id==7) { page=button.id==6?0:1;return; }
        if(button.id==4) {
            EntityCustomCruise selected=null;double distance=16000000;
            for(net.minecraft.entity.Entity entity:mc.world.loadedEntityList) {
                if(!(entity instanceof EntityCustomCruise)) continue;
                EntityCustomCruise rocket=(EntityCustomCruise)entity;
                if(rocket.getBuild().checksum()==checksum && mc.player.getUniqueID().equals(rocket.getOwnerUuid()) && rocket.getDistanceSq(mc.player)<distance) { selected=rocket;distance=rocket.getDistanceSq(mc.player); }
            }
            if(selected==null) { error=CruiseText.text("program.no_missile");return; }
            WarTechNetwork.CHANNEL.sendToServer(new CruiseMissionEditMessage(missileSlot,checksum,selected.getEntityId()));return;
        }
        double[] position={0,0,0};error="";
        if(button.id==8) {
            CruiseTargetCategory[] categories=CruiseTargetCategory.values();
            CruisePartDefinition seeker=CruiseBuild.fromStack(missile()).get(CruiseSlot.SEEKER);
            for(int i=1;i<=categories.length;i++) {
                CruiseTargetCategory next=categories[(mission.getCategory().ordinal()+i)%categories.length];
                if(next.supports(seeker)) {
                    WarTechNetwork.CHANNEL.sendToServer(new CruiseMissionEditMessage(missileSlot,checksum,6,next.ordinal(),0,0));return;
                }
            }
            return;
        }
        if(button.id==0) try {
            for(int i=0;i<3;i++) { position[i]=Double.parseDouble(fields[i].getText());if(!Double.isFinite(position[i])) throw new NumberFormatException(); }
        } catch(NumberFormatException ex) { error=CruiseText.text("program.bad_numbers");return; }
        if(button.id==5) position[0]=mission.getMode()==CruiseMission.Mode.COORDINATE?1:0;
        if(button.id==3) {
            position[1]=Double.NaN;
            if(!fields[1].getText().trim().isEmpty()) try {
                position[1]=Double.parseDouble(fields[1].getText());
                if(!Double.isFinite(position[1]) || position[1]<0 || position[1]>255) throw new NumberFormatException();
            } catch(NumberFormatException ex) { error=CruiseText.text("program.bad_numbers");return; }
        }
        WarTechNetwork.CHANNEL.sendToServer(new CruiseMissionEditMessage(missileSlot,checksum,button.id,position[0],position[1],position[2]));
    }
    @Override protected void keyTyped(char character,int key) throws IOException {
        if(key==Keyboard.KEY_TAB) { for(int i=0;i<3;i++) if(fields[i].isFocused()) { fields[i].setFocused(false);fields[(i+1)%3].setFocused(true);return; }fields[0].setFocused(true);return; }
        for(GuiTextField field:fields) if(field.textboxKeyTyped(character,key)) return;
        super.keyTyped(character,key);
    }
    @Override protected void mouseClicked(int x,int y,int button) throws IOException { super.mouseClicked(x,y,button);for(GuiTextField field:fields) field.mouseClicked(x,y,button); }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int x,int y) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
        drawRect(guiLeft+5,guiTop+14,guiLeft+xSize-5,guiTop+26,0xFF26383F);
        for(Slot slot:inventorySlots.inventorySlots) GuiTheme.slot(guiLeft+slot.xPos-1,guiTop+slot.yPos-1);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY) {
        CruiseBuild build=CruiseBuild.fromStack(missile());CruiseMission mission=CruiseMission.fromStack(missile());boolean search=mission.getMode()==CruiseMission.Mode.SEARCH;
        GuiTheme.text(fontRenderer,CruiseText.text("program.title"),12,7,296,GuiTheme.TEXT);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(build.getName(),296),12,18,0x78D5BB);
        if(mission.getTargets().size()<=4) page=0;
        for(int i=page*4;i<Math.min(mission.getTargets().size(),page*4+4);i++) {
            Vec3d goal=mission.getTargets().get(i);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(String.format(Locale.US,"%d  %.1f / %.1f / %.1f",i+1,goal.x,goal.y,goal.z),290),16,52+(i%4)*9,0xCBD9D2);
        }
        fontRenderer.drawString(fontRenderer.trimStringToWidth(error.isEmpty()?CruiseText.text("program.count",mission.getTargets().size(),search?8:1):error,244),12,90,error.isEmpty()?0x78D5BB:0xFF7777);
        fontRenderer.drawString("X",43,99,0xCBD9D2);fontRenderer.drawString("Y",129,99,0xCBD9D2);fontRenderer.drawString("Z",215,99,0xCBD9D2);
        GuiTheme.text(fontRenderer,CruiseText.text("program.designator_short"),14,99,22,0xCBD9D2);
        for(GuiButton button:buttonList) {
            if(button.id==5) { button.displayString=CruiseText.text("mode.short."+(search?"search":"coordinate"));button.enabled=CruiseMission.hasSeeker(build); }
            if(button.id==8) { button.displayString=fontRenderer.trimStringToWidth(CruiseText.text("program.category",CruiseText.text("category."+mission.getCategory().name().toLowerCase(Locale.ROOT))),134);button.enabled=search; }
            if(button.id==0) button.displayString=CruiseText.text(search?"program.add_area":"program.set_target");
            if(button.id==3) button.enabled=!container.getDesignator().isEmpty();
            if(button.id==4) button.enabled=build.get(CruiseSlot.LINK)==CruisePartDefinition.LINK_COMMAND;
            if(button.id==6) button.enabled=page>0;
            if(button.id==7) button.enabled=page==0 && mission.getTargets().size()>4;
        }
    }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground();super.drawScreen(x,y,partial);for(GuiTextField field:fields) field.drawTextBox();renderHoveredToolTip(x,y);
        if(isPointInRegion(12,28,146,20,x,y)) {
            java.util.List<String> info=new java.util.ArrayList<>();info.add(CruiseText.text("program.auto_route"));
            CruisePartDefinition brain=CruiseBuild.fromStack(missile()).get(CruiseSlot.NAVIGATION);
            if(brain!=null) info.addAll(fontRenderer.listFormattedStringToWidth(CruiseText.text("desc."+brain.getId()),260));
            if(!CruiseMission.hasSeeker(CruiseBuild.fromStack(missile()))) info.add(CruiseText.text("program.seeker_required"));
            drawHoveringText(info,x,y);
        }
        if(isPointInRegion(162,28,146,20,x,y)) {
            java.util.List<String> info=new java.util.ArrayList<>();
            info.addAll(fontRenderer.listFormattedStringToWidth(CruiseText.text("program.category_help"),260));
            CruisePartDefinition seeker=CruiseBuild.fromStack(missile()).get(CruiseSlot.SEEKER);
            CruisePartDefinition brain=CruiseBuild.fromStack(missile()).get(CruiseSlot.NAVIGATION);
            info.add(CruiseText.text("program.search_limits",(int)CruiseSeeker.area(brain),CruiseSeeker.searchDuration(brain)/20));
            if(seeker!=null) info.addAll(fontRenderer.listFormattedStringToWidth(CruiseText.text("desc."+seeker.getId()),260));
            for(CruiseTargetCategory category:CruiseTargetCategory.values()) if(category.supports(seeker))
                info.add(CruiseText.text("category."+category.name().toLowerCase(Locale.ROOT)));
            drawHoveringText(info,x,y);
        }
        if(container.getDesignator().isEmpty() && isPointInRegion(16,108,16,16,x,y)) drawHoveringText(java.util.Collections.singletonList(CruiseText.text("program.designator_slot")),x,y);
        if(isPointInRegion(129,108,80,18,x,y)) drawHoveringText(fontRenderer.listFormattedStringToWidth(CruiseText.text("program.designator_y"),260),x,y);
    }
}
